import { toast } from "react-toastify";

export const confirmToast = (message) => {
  return new Promise((resolve) => {
    toast(
      ({ closeToast }) => (
        <div className="flex flex-col items-center text-center gap-4 py-2">
          {/* Nội dung */}
          <p className="text-base font-medium">{message}</p>

          {/* Hai nút */}
          <div className="flex gap-4 mt-2">
            {/* Cancel - đỏ nhạt */}
            <button
              className="px-4 py-2 rounded-lg bg-red-200 text-red-700 font-semibold hover:bg-red-300 transition"
              onClick={() => {
                resolve(false);
                closeToast();
              }}
            >
              Cancel
            </button>

            {/* OK - xanh lá */}
            <button
              className="px-4 py-2 rounded-lg bg-green-500 text-white font-semibold hover:bg-green-600 transition"
              onClick={() => {
                resolve(true);
                closeToast();
              }}
            >
              OK
            </button>
          </div>
        </div>
      ),
      {
        autoClose: false,
        closeOnClick: false,
        draggable: false,
        position: "top-center"
      }
    );
  });
};
