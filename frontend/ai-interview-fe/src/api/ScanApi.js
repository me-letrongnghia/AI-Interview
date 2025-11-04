import Https from "../access/Https";
import axios from "axios";

// Create a separate instance for file uploads and scan operations without credentials
const scanInstance = axios.create({
  baseURL: "http://localhost:8080",
  timeout: 120000, // 2 minutes for AI processing
  withCredentials: false, // Disable credentials to avoid CORS issues
});

// Add auth token to scan requests if needed
scanInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("access_token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Add response interceptor for better error handling
scanInstance.interceptors.response.use(
  (response) => {
    console.log("Scan API response:", response);
    return response;
  },
  (error) => {
    console.error("Scan API error:", error);
    return Promise.reject(error);
  }
);

export const ScanApi = {
  // Scan and extract data from CV file
  scanCV: (file) => {
    const formData = new FormData();
    formData.append("file", file);

    console.log("Uploading CV file:", file.name, "Size:", file.size);

    return scanInstance.post("/api/cv/scan", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    });
  },

  scanJD: (jdText) => {
    console.log("Scanning JD text:", jdText);

    return scanInstance.post("/api/jd/scan", jdText, {
      headers: {
        "Content-Type": "text/plain",
      },
    });
  },

  scanJDWithParam: (jdText) => {
    return scanInstance.post("/api/jd/scan", null, {
      params: { jdText },
    });
  },

  // Scrape JD text only from URL - NO AI analysis, just get text
  scrapeJDUrl: (url) => {
    console.log("Scraping JD text from URL:", url);

    return scanInstance.post("/api/jd/scrape-url", { url }, {
      headers: {
        "Content-Type": "application/json",
      },
    });
  },

  // Scan JD from URL - scrape + AI analysis (optional, if needed)
  scanJDUrl: (url) => {
    console.log("Scanning JD from URL with AI:", url);

    return scanInstance.post("/api/jd/scan-url", { url }, {
      headers: {
        "Content-Type": "application/json",
      },
    });
  },
};

export default ScanApi;
