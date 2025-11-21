import React, { useState, useRef, useEffect } from "react";
import { Mail, Edit2, Save, X, Camera, Upload, BookOpen } from "lucide-react";
import { UserApi } from "../api/UserApi";
import { CloudinaryApi } from "../api/CloudinaryApi";
import { toast } from "react-toastify";
import Header from "../components/Header";
import { UseAppContext } from "../context/AppContext";
import { Link } from "react-router-dom";

export default function ProfilePage() {
  const { setUserProfile, userProfile } = UseAppContext();
  const [isEditing, setIsEditing] = useState(false);
  const [loading, setLoading] = useState(true);
  const [profile, setProfile] = useState({
    email: "",
    fullName: "",
    picture: null,
    countSession: 0,
    totalDuration: 0,
    totalQuestion: 0,
  });
  const [editedProfile, setEditedProfile] = useState({
    email: "",
    fullName: "",
    picture: null,
    countSession: 0,
  });
  const [showAvatarMenu, setShowAvatarMenu] = useState(false);
  const [imageFile, setImageFile] = useState(null);
  const fileInputRef = useRef(null);

  useEffect(() => {
    fetchProfile();
  }, []);

  const fetchProfile = async () => {
    try {
      setLoading(true);
      const response = await UserApi.Get_Profile();
      console.log("Profile response:", response);
      const profileData = {
        email: response.data.email || "",
        fullName: response.data.fullName || "",
        picture: userProfile.picture || null,
        countSession: response.data.countSession || 0,
        totalDuration: response.data.totalDuration || 0,
        totalQuestion: response.data.totalQuestion || 0,
      };
      console.log("Fetched profile data:", profileData);
      setProfile(profileData);
      setEditedProfile(profileData);
    } catch (error) {
      toast.error("Không thể tải thông tin người dùng");
      console.error("Error fetching profile:", error);
    } finally {
      setLoading(false);
    }
  };
  console.log("totalDrution", profile.totalDuration);
  const handleEdit = () => {
    setIsEditing(true);
    setEditedProfile(profile);
  };

  const handleSave = async () => {
    try {
      // Validate fullName
      const trimmedFullName = editedProfile.fullName.trim();
      if (!trimmedFullName) {
        toast.error("Họ và tên không được để trống!");
        return;
      }

      if (trimmedFullName.length < 2) {
        toast.error("Họ và tên phải có ít nhất 2 ký tự!");
        return;
      }

      let pictureUrl = editedProfile.picture;

      // Nếu có file ảnh mới, upload lên Cloudinary trước
      if (imageFile) {
        const formData = new FormData();
        formData.append("file", imageFile);

        const uploadResponse = await CloudinaryApi.Upload_Image(formData);
        pictureUrl = uploadResponse.data.imageUrl;
      }

      // Sau đó cập nhật thông tin profile
      const response = await UserApi.Update_Picture({
        fullName: trimmedFullName,
        pictureUrl: pictureUrl,
      });
      const updatedProfileData = {
        email: response.data.email,
        fullName: response.data.fullName,
        picture: response.data.picture,
        countSession: response.data.countSession || profile.countSession,
        totalDuration: response.data.totalDuration || profile.totalDuration,
        totalQuestion: response.data.totalQuestion || profile.totalQuestion,
      };

      setProfile(updatedProfileData);
      setEditedProfile(updatedProfileData);
      setIsEditing(false);
      setShowAvatarMenu(false);
      setImageFile(null);
      setUserProfile(updatedProfileData);
      localStorage.setItem("user", JSON.stringify(updatedProfileData));
      toast.success("Cập nhật thông tin thành công!");
    } catch (error) {
      toast.error("Không thể cập nhật thông tin");
      console.error("Error updating profile:", error);
    }
  };

  const handleCancel = () => {
    setEditedProfile(profile);
    setIsEditing(false);
    setShowAvatarMenu(false);
  };

  const handleChange = (field, value) => {
    if (field === "fullName") {
      // Không cho phép nhập toàn khoảng trắng
      if (value && !value.trim() && value.length > 0) {
        return;
      }
    }
    setEditedProfile((prev) => ({ ...prev, [field]: value }));
  };

  const handleAvatarClick = () => {
    if (isEditing) {
      setShowAvatarMenu(!showAvatarMenu);
    }
  };

  const handleFileChange = (e) => {
    const file = e.target.files[0];
    if (file) {
      // Kiểm tra định dạng file
      const allowedTypes = ["image/jpeg", "image/png", "image/jpg"];
      if (!allowedTypes.includes(file.type)) {
        toast.error("Chỉ chấp nhận file ảnh định dạng JPG, JPEG hoặc PNG!");
        e.target.value = null; // Reset input
        return;
      }

      // Kiểm tra kích thước file (tùy chọn, ví dụ: giới hạn 5MB)
      const maxSize = 5 * 1024 * 1024; // 5MB
      if (file.size > maxSize) {
        toast.error("Kích thước ảnh không được vượt quá 5MB!");
        e.target.value = null; // Reset input
        return;
      }

      // Lưu file gốc để upload
      setImageFile(file);

      // Hiển thị preview
      const reader = new FileReader();
      reader.onloadend = () => {
        setEditedProfile((prev) => ({ ...prev, picture: reader.result }));
        setShowAvatarMenu(false);
      };
      reader.readAsDataURL(file);
    }
  };

  const handleRemoveAvatar = () => {
    setEditedProfile((prev) => ({ ...prev, picture: null }));
    setImageFile(null);
    setShowAvatarMenu(false);
  };

  const triggerFileInput = () => {
    fileInputRef.current?.click();
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 via-white to-blue-50 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-16 w-16 border-b-2 border-green-500 mx-auto"></div>
          <p className="mt-4 text-gray-600 font-medium">Đang tải...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 via-white to-blue-50">
      {/* Header/Navigation */}
      <Header />

      {/* Main Content */}
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        {/* Profile Card */}
        <div className="bg-white rounded-2xl shadow-xl p-8">
          {/* Header */}
          <div className="flex justify-between items-center mb-8">
            <h1 className="text-3xl font-bold text-gray-900">My Profile</h1>
            <div className="flex space-x-3">
              {isEditing ? (
                <>
                  <button
                    onClick={handleSave}
                    className="flex items-center space-x-2 px-5 py-2.5 bg-green-500 text-white rounded-lg hover:bg-green-600 transition shadow-md"
                  >
                    <Save className="w-4 h-4" />
                    <span>Save</span>
                  </button>
                  <button
                    onClick={handleCancel}
                    className="flex items-center space-x-2 px-5 py-2.5 bg-gray-200 text-gray-700 rounded-lg hover:bg-gray-300 transition"
                  >
                    <X className="w-4 h-4" />
                    <span>Cancel</span>
                  </button>
                </>
              ) : (
                <button
                  onClick={handleEdit}
                  className="flex items-center space-x-2 px-5 py-2.5 bg-green-500 text-white rounded-lg hover:bg-green-600 transition shadow-md"
                >
                  <Edit2 className="w-4 h-4" />
                  <span>Edit Profile</span>
                </button>
              )}
            </div>
          </div>

          {/* Avatar Section */}
          <div className="flex justify-center mb-8">
            <div className="relative">
              {editedProfile.picture ? (
                <img
                  src={editedProfile.picture}
                  alt="Profile"
                  className="w-32 h-32 rounded-full object-cover border-4 border-green-200 shadow-lg"
                />
              ) : (
                <div className="w-32 h-32 bg-gradient-to-br from-green-400 to-green-600 rounded-full flex items-center justify-center text-white text-5xl font-bold border-4 border-green-200 shadow-lg">
                  {editedProfile.fullName.charAt(0).toUpperCase()}
                </div>
              )}

              {isEditing && (
                <>
                  <button
                    onClick={handleAvatarClick}
                    className="absolute bottom-0 right-0 w-10 h-10 bg-green-500 rounded-full flex items-center justify-center text-white hover:bg-green-600 transition shadow-lg border-2 border-white"
                  >
                    <Camera className="w-5 h-5" />
                  </button>

                  {showAvatarMenu && (
                    <div className="absolute top-full right-0 mt-3 bg-white rounded-lg shadow-2xl border border-gray-200 py-2 z-10 w-52">
                      <button
                        onClick={triggerFileInput}
                        className="w-full px-4 py-2.5 text-left hover:bg-gray-50 flex items-center space-x-3 text-gray-700"
                      >
                        <Upload className="w-4 h-4" />
                        <span>Upload Photo</span>
                      </button>
                      {editedProfile.picture && (
                        <button
                          onClick={handleRemoveAvatar}
                          className="w-full px-4 py-2.5 text-left hover:bg-gray-50 flex items-center space-x-3 text-red-600"
                        >
                          <X className="w-4 h-4" />
                          <span>Remove Photo</span>
                        </button>
                      )}
                    </div>
                  )}

                  <input
                    ref={fileInputRef}
                    type="file"
                    accept="image/*"
                    onChange={handleFileChange}
                    className="hidden"
                  />
                </>
              )}
            </div>
          </div>

          {/* Profile Information */}
          <div className="space-y-6">
            {/* Full Name */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Full Name
              </label>
              {isEditing ? (
                <input
                  type="text"
                  value={editedProfile.fullName}
                  onChange={(e) => handleChange("fullName", e.target.value)}
                  className="w-full px-4 py-3 border-2 border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-green-500 focus:border-transparent transition"
                  placeholder="Enter your full name"
                />
              ) : (
                <div className="px-4 py-3 bg-gray-50 border-2 border-gray-200 rounded-lg text-gray-900 font-medium">
                  {profile.fullName}
                </div>
              )}
            </div>

            {/* Email (Read-only) */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Email
              </label>
              <div className="flex items-center space-x-3 px-4 py-3 bg-gray-100 border-2 border-gray-200 rounded-lg">
                <Mail className="w-5 h-5 text-gray-500" />
                <span className="text-gray-700 font-medium">
                  {profile.email}
                </span>
              </div>
              <p className="text-xs text-gray-500 mt-1 ml-1">
                Email cannot be changed
              </p>
            </div>

            {/* Count Session (Read-only) */}
            <div>
              <label className="block text-sm font-semibold text-gray-700 mb-2">
                Interview Sessions Completed
              </label>
              <div className="flex items-center space-x-3 px-4 py-3 bg-green-50 border-2 border-green-200 rounded-lg">
                <BookOpen className="w-5 h-5 text-green-600" />
                <span className="text-2xl font-bold text-green-600">
                  {profile.countSession}
                </span>
                <span className="text-gray-600 font-medium">sessions</span>
              </div>
            </div>
          </div>

          {!isEditing && (
            <div className="mt-8 pt-6 border-t border-gray-200">
              <div className="grid grid-cols-2 gap-4 text-center">
                <div className="p-4 bg-blue-50 rounded-lg">
                  <p className="text-sm text-gray-600 mb-1">Total Question</p>
                  <p className="text-2xl font-bold text-blue-600">
                    {profile.totalQuestion}
                  </p>
                </div>
                <div className="p-4 bg-purple-50 rounded-lg">
                  <p className="text-sm text-gray-600 mb-1">Total Minutes</p>
                  <p className="text-2xl font-bold text-purple-600">
                    {profile.totalDuration}m
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Action Button */}
        {!isEditing && (
          <div className="mt-6">
            <button className="w-full py-4 bg-gradient-to-r from-green-500 to-green-600 text-white rounded-xl font-semibold text-lg hover:from-green-600 hover:to-green-700 transition shadow-lg">
              <Link to="/options">Start New Mock Interview</Link>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
