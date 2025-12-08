import SockJS from "sockjs-client";
import Stomp from "stompjs";
// SocketService.js
let stompClient = null;
let isConnected = false;
let isConnecting = false;

/**
 * Kết nối WebSocket
 * onMessageReceived callback nhận message từ server
 */
export const connectSocket = (sessionId, onMessageReceived) => {
  return new Promise((resolve, reject) => {
    // If already connected with a valid client, just resolve
    if (stompClient && stompClient.connected && isConnected) {
      console.log("✅ Socket already connected, reusing connection");
      resolve();
      return;
    }

    // Prevent multiple concurrent connections
    if (isConnecting) {
      console.warn("⚠️ Socket is already connecting, please wait");
      resolve();
      return;
    }

    // Clean up any stale client
    if (stompClient && !stompClient.connected) {
      console.log("🧹 Cleaning up stale socket client");
      stompClient = null;
      isConnected = false;
    }

    isConnecting = true;
    console.log("🔌 Connecting to WebSocket for session:", sessionId);

    // Lấy token từ localStorage
    const token = localStorage.getItem("accessToken");

    const socket = new SockJS("http://localhost:8080/ws/interview");
    stompClient = Stomp.over(socket);

    // tắt debug spam
    stompClient.debug = null;

    // Headers với JWT token - gửi qua STOMP header và native header
    const headers = {};
    if (token) {
      headers.Authorization = `Bearer ${token}`;
      headers.token = token; // Fallback for SockJS
    }

    // Store reference to current client for callback validation
    const currentClient = stompClient;

    currentClient.connect(
      headers,
      () => {
        isConnecting = false;

        // ⭐ CRITICAL: Check if stompClient is still the same (not cleaned up by unmount)
        if (!stompClient || stompClient !== currentClient) {
          console.warn(
            "⚠️ Socket was cleaned up during connection, aborting subscription"
          );
          // Try to disconnect this orphaned connection
          try {
            currentClient.disconnect();
          } catch (e) {
            // Ignore disconnect errors
          }
          return;
        }

        isConnected = true;

        console.log(
          "✅ WebSocket connected successfully for session:",
          sessionId
        );

        // Subscribe kênh nhận message
        try {
          stompClient.subscribe(`/topic/interview/${sessionId}`, (message) => {
            console.log("📥 Received WebSocket message:", message);
            if (message.body) {
              const body = JSON.parse(message.body);
              console.log("📨 Parsed message body:", body);
              onMessageReceived(body);
            }
          });

          console.log("✅ Subscribed to /topic/interview/" + sessionId);
          resolve();
        } catch (subscribeError) {
          console.error("❌ Failed to subscribe:", subscribeError);
          isConnected = false;
          reject(subscribeError);
        }
      },
      (error) => {
        isConnecting = false;
        isConnected = false;
        stompClient = null; // ⭐ Reset client khi lỗi
        console.error("❌ WebSocket connection failed:", error);
        reject(error);
      }
    );
  });
};

/**
 * Gửi answer lên server
 */
export const sendAnswer = (sessionId, answerMessage) => {
  console.log("🔌 WebSocket send attempt:", {
    sessionId,
    isConnected,
    hasClient: !!stompClient,
    clientConnected: stompClient?.connected,
    message: answerMessage,
  });

  // ⭐ Validation nghiêm ngặt: phải có cả client VÀ connected
  if (!stompClient) {
    console.error("❌ stompClient is null!");
    isConnected = false; // Reset state
    return false;
  }

  if (!stompClient.connected) {
    console.error("❌ stompClient exists but not connected!");
    isConnected = false; // Reset state
    return false;
  }

  // Nếu đến đây thì client tồn tại VÀ connected
  console.log("✅ Sending message via WebSocket...");
  try {
    stompClient.send(
      `/app/interview/${sessionId}/answer`,
      {},
      JSON.stringify(answerMessage)
    );
    console.log("✅ Message sent successfully");
    return true;
  } catch (error) {
    console.error("❌ Error sending message:", error);
    // Reset connection state if send fails
    isConnected = false;
    stompClient = null;
    return false;
  }
};

let isDisconnecting = false; // ⭐ Flag để tránh disconnect nhiều lần

export const disconnectSocket = () => {
  // Nếu đang disconnect thì skip
  if (isDisconnecting) {
    console.log("⏳ Disconnect already in progress");
    return;
  }

  // Nếu không có client VÀ đã disconnected thì skip
  if (!stompClient && !isConnected && !isConnecting) {
    console.log("⚠️ Socket already disconnected");
    return;
  }

  // Đánh dấu đang disconnect
  isDisconnecting = true;

  // ⭐ IMPORTANT: Cancel any pending connection immediately
  if (isConnecting) {
    console.log("🛑 Cancelling pending connection");
    isConnecting = false;
  }

  try {
    if (stompClient) {
      const clientToDisconnect = stompClient;
      stompClient = null; // ⭐ Set null FIRST to prevent callbacks from using it
      isConnected = false;

      if (clientToDisconnect.connected) {
        console.log("📤 Sending disconnect to server...");
        try {
          clientToDisconnect.disconnect(() => {
            console.log("✅ Socket disconnected successfully");
            isDisconnecting = false;
          });
        } catch (disconnectError) {
          console.warn("⚠️ Error during disconnect:", disconnectError);
          isDisconnecting = false;
        }
      } else {
        console.log("🧹 Cleaning up inactive socket");
        isDisconnecting = false;
      }
    } else {
      // Client đã null nhưng flag chưa reset
      console.log("🔄 Resetting connection flags");
      isConnected = false;
      isConnecting = false; // ⭐ Reset cả isConnecting
      isDisconnecting = false;
    }
  } catch (error) {
    console.warn("⚠️ Error during disconnect:", error);
    stompClient = null;
    isConnected = false;
    isConnecting = false; // ⭐ Reset cả isConnecting
    isDisconnecting = false;
  }
};

/**
 * Get connection status (useful for debugging)
 */
export const getConnectionStatus = () => ({
  isConnected,
  isConnecting,
  hasClient: !!stompClient,
  clientConnected: stompClient?.connected,
});

/**
 * Force reconnect if needed (useful for recovery)
 */
export const ensureConnected = async (sessionId, onMessageReceived) => {
  const status = getConnectionStatus();
  console.log("🔍 Checking connection status:", status);

  // If client exists and is connected, we're good
  if (stompClient && stompClient.connected) {
    console.log("✅ Connection is healthy");
    return Promise.resolve();
  }

  // If state is out of sync or client is dead, reconnect
  console.log("🔄 Connection needs repair, reconnecting...");
  disconnectSocket(); // Clean up first

  // Wait a bit for cleanup
  await new Promise((resolve) => setTimeout(resolve, 100));

  return connectSocket(sessionId, onMessageReceived);
};
/**
 * Notify server that user is leaving via HTTP (more reliable than WebSocket)
 * Uses sendBeacon API which works even during page unload
 */
export const notifyUserLeaving = (
  sessionId,
  reason = "User leaving",
  elapsedSeconds = 0
) => {
  console.log("📤 Notifying server user is leaving via HTTP:", {
    sessionId,
    reason,
    elapsedSeconds,
  });

  try {
    const url = `http://localhost:8080/api/interviews/${sessionId}/leave`;

    // 🔐 Get access token from localStorage
    const token = localStorage.getItem("access_token");

    const data = JSON.stringify({
      sessionId,
      reason,
      elapsedSeconds,
      timestamp: new Date().toISOString(),
    });

    // Prepare headers with Authorization Bearer token
    const headers = {
      "Content-Type": "application/json",
    };

    // 🔐 Add Authorization header if token exists
    if (token) {
      headers.Authorization = `Bearer ${token}`;
      console.log("🔐 Including Authorization Bearer token");
    }

    // Use fetch with keepalive (works during page unload and supports headers)
    fetch(url, {
      method: "POST",
      headers: headers,
      body: data,
      keepalive: true, // Important: allows request to continue after page unload
    })
      .then((response) => {
        if (response.ok) {
          console.log("✅ Leave notification sent successfully");
          console.log(
            `   Elapsed time: ${elapsedSeconds}s (${Math.floor(
              elapsedSeconds / 60
            )}m)`
          );
        } else {
          console.warn(`⚠️ Server returned status: ${response.status}`);
        }
      })
      .catch((err) => {
        console.warn("⚠️ Fetch failed:", err);
      });

    return true;
  } catch (error) {
    console.error("❌ Error sending leave notification:", error);
    return false;
  }
};

export const notifyUserInactive = () => {
  if (stompClient && stompClient.connected) {
    stompClient.send(
      "/app/user-inactive",
      {},
      JSON.stringify({
        message: "User switched tab or route",
        timestamp: Date.now(),
      })
    );
  }
};
