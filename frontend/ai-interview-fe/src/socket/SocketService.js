import SockJS from "sockjs-client";
import Stomp from "stompjs";

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

    stompClient.connect(
      headers,
      () => {
        isConnecting = false;
        isConnected = true;

        console.log(
          "✅ WebSocket connected successfully for session:",
          sessionId
        );

        // Subscribe kênh nhận message
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
      },
      (error) => {
        isConnecting = false;
        isConnected = false;
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

  // Check both our flag AND the actual stomp connection status
  if (stompClient && stompClient.connected) {
    console.log("✅ Sending message via WebSocket...");
    try {
      stompClient.send(
        `/app/interview/${sessionId}/answer`, // map với @MessageMapping server
        {},
        JSON.stringify(answerMessage)
      );
      console.log("✅ Message sent successfully");
      return true;
    } catch (error) {
      console.error("❌ Error sending message:", error);
      // Reset connection state if send fails
      isConnected = false;
      return false;
    }
  } else {
    console.error("❌ Cannot send, socket not connected!", {
      hasClient: !!stompClient,
      isConnected,
      clientConnected: stompClient?.connected,
    });

    // Fix state if out of sync
    if (isConnected && (!stompClient || !stompClient.connected)) {
      console.warn("🔧 Fixing out-of-sync connection state");
      isConnected = false;
    }

    return false;
  }
};

/**
 * Ngắt kết nối WebSocket
 */
export const disconnectSocket = () => {
  console.log("🔌 Disconnecting socket...", {
    hasClient: !!stompClient,
    isConnected,
    isConnecting,
  });

  // Always reset flags first
  isConnected = false;
  isConnecting = false;

  if (stompClient) {
    try {
      // Try to disconnect gracefully if connected
      if (stompClient.connected) {
        stompClient.disconnect(() => {
          console.log("✅ Socket disconnected successfully");
          stompClient = null;
        });
      } else {
        // Just clean up if not connected
        console.log("🧹 Cleaning up disconnected socket");
        stompClient = null;
      }
    } catch (error) {
      console.warn("⚠️ Error during socket disconnect:", error);
      stompClient = null;
    }
  } else {
    console.log("⚠️ No socket client to disconnect");
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