import SockJS from "sockjs-client";
import Stomp from "stompjs";

let stompClient = null;
let isConnected = false;

/**
 * Kết nối WebSocket
 * onMessageReceived callback nhận message từ server
 */
export const connectSocket = (sessionId, onMessageReceived) => {
  return new Promise((resolve, reject) => {
    const socket = new SockJS("http://localhost:8080/ws/interview"); // match server endpoint
    stompClient = Stomp.over(socket);

    // tắt debug spam
    stompClient.debug = null;

    stompClient.connect(
      {},
      () => {
        isConnected = true;
        // Subscribe kênh nhận message
        stompClient.subscribe(`/topic/interview/${sessionId}`, (message) => {
          if (message.body) {
            const body = JSON.parse(message.body);
            onMessageReceived(body);
          }
        });

        resolve();
      },
      (error) => {
        reject(error);
      }
    );
  });
};

/**
 * Gửi answer lên server
 */
export const sendAnswer = (sessionId, answerMessage) => {
  if (stompClient && isConnected) {
    stompClient.send(
      `/app/interview/${sessionId}/answer`, // map với @MessageMapping server
      {},
      JSON.stringify(answerMessage)
    );
  } else {
    console.error("Cannot send, socket not connected yet!");
  }
};

/**
 * Ngắt kết nối WebSocket
 */
export const disconnectSocket = () => {
  if (stompClient && isConnected) {
    stompClient.disconnect(() => {
      isConnected = false;
    });
  }
};
