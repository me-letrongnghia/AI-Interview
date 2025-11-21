import Https from "../access/Https";

export const CloudinaryApi = {
  Upload_Image: (formData) =>
    Https.post("/api/cloudinary/upload", formData, {
      headers: {
        "Content-Type": "multipart/form-data",
      },
    }),
};
