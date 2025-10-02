import SockJS from "sockjs-client";
import Stomp from "stompjs";

let stompClient = null;
let isConnected = false;

export const connectSocket = (sessionId, onMessageReceived) => {
  return new Promise((resolve, reject) => {
    const socket = new SockJS("http://localhost:8080/ws/interview");
    stompClient = Stomp.over(socket);

    stompClient.connect(
      {},
      () => {
        isConnected = true;
        console.log("✅ WebSocket Connected");

        stompClient.subscribe(`/topic/interview/${sessionId}`, (message) => {
          const body = JSON.parse(message.body);
          onMessageReceived(body);
        });

        resolve(); // báo là kết nối thành công
      },
      (error) => {
        console.error("❌ WebSocket Connection Error:", error);
        reject(error);
      }
    );
  });
};

export const sendAnswer = (sessionId, answerMessage) => {
  if (stompClient && isConnected) {
    stompClient.send(
      `/app/interview/${sessionId}/answer`,
      {},
      JSON.stringify(answerMessage)
    );
  } else {
    console.error("⚠️ Cannot send, socket not connected yet!");
  }
};

export const disconnectSocket = () => {
  if (stompClient && isConnected) {
    stompClient.disconnect(() => {
      console.log("❎ Disconnected");
      isConnected = false;
    });
  }
};
