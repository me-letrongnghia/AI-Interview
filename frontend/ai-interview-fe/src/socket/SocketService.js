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
        isConnected = false;
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
