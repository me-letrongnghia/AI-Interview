import SockJS from "sockjs-client";
import Stomp from "stompjs";

let stompClient = null;
let isConnected = false;

/**
 * Phát audio từ Base64 string
 */
export const playAudioFromBase64 = (base64Audio) => {
  try {
    // Convert Base64 to Blob
    const byteCharacters = atob(base64Audio);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
      byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);
    const blob = new Blob([byteArray], { type: 'audio/wav' });
    
    // Tạo URL và phát audio
    const audioUrl = URL.createObjectURL(blob);
    const audio = new Audio(audioUrl);
    
    audio.play()
      .then(() => console.log('Playing question audio...'))
      .catch(err => console.error('Error playing audio:', err));
    
    // Clean up URL sau khi phát xong
    audio.onended = () => {
      URL.revokeObjectURL(audioUrl);
      console.log('Audio finished playing');
    };
  } catch (error) {
    console.error('Error playing audio from Base64:', error);
  }
};

/**
 * Kết nối WebSocket
 * onMessageReceived callback nhận message từ server
 */
export const connectSocket = (sessionId, onMessageReceived) => {
  return new Promise((resolve, reject) => {
    const socket = new SockJS("http://localhost:8080/ws/interview");
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
            
            // TẠO VÀ PHÁT AUDIO TẠI ĐÂY NẾU CÓ
            if (body.type === 'question' && body.audioData) {
              console.log('Received question with audio');
              playAudioFromBase64(body.audioData);
            }
            
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
      `/app/interview/${sessionId}/answer`,
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