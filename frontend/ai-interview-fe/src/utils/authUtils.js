/**
 * Utility functions for authentication and session management
 */

/**
 * Decode JWT token to get payload
 * @param {string} token - JWT token to decode
 * @returns {object|null} - Decoded payload or null if invalid
 */
export const decodeJWT = (token) => {
  try {
    if (!token) return null;
    
    const parts = token.split('.');
    if (parts.length !== 3) return null;
    
    const payload = parts[1];
    const decoded = JSON.parse(atob(payload));
    return decoded;
  } catch (error) {
    console.error("Error decoding JWT:", error);
    return null;
  }
};

/**
 * Check if JWT token is expired
 * @param {string} token - JWT token to check
 * @returns {boolean} - true if expired, false if still valid
 */
export const isTokenExpired = (token) => {
  const decoded = decodeJWT(token);
  if (!decoded || !decoded.exp) return true;
  
  // exp is in seconds, Date.now() is in milliseconds
  const expirationTime = decoded.exp * 1000;
  const currentTime = Date.now();
  
  return currentTime >= expirationTime;
};

/**
 * Get time until token expires in milliseconds
 * @param {string} token - JWT token
 * @returns {number} - milliseconds until expiration, or 0 if expired
 */
export const getTimeUntilExpiration = (token) => {
  const decoded = decodeJWT(token);
  if (!decoded || !decoded.exp) return 0;
  
  const expirationTime = decoded.exp * 1000;
  const currentTime = Date.now();
  const timeRemaining = expirationTime - currentTime;
  
  return timeRemaining > 0 ? timeRemaining : 0;
};

/**
 * Clear all Firebase authentication data from localStorage
 * This includes the Firebase auth user object which persists even after signOut
 */
export const clearFirebaseAuth = () => {
  // Find and remove all Firebase auth keys
  const firebaseKeys = Object.keys(localStorage).filter(key => 
    key.startsWith('firebase:authUser:') || 
    key.startsWith('firebase:host:')
  );
  
  firebaseKeys.forEach(key => {
    localStorage.removeItem(key);
  });
};

/**
 * Clear all application auth data
 * This clears both app-specific auth data and Firebase persistence
 */
export const clearAllAuthData = () => {
  // Clear app auth data
  localStorage.removeItem("user");
  localStorage.removeItem("access_token");
  localStorage.removeItem("isLogin");
  
  // Clear Firebase auth data
  clearFirebaseAuth();
};

/**
 * Check if user session is valid
 * Returns true if user has valid token and user data
 */
export const isSessionValid = () => {
  const user = localStorage.getItem("user");
  const token = localStorage.getItem("access_token");
  const isLogin = localStorage.getItem("isLogin");
  
  return !!(user && token && isLogin === "true");
};
