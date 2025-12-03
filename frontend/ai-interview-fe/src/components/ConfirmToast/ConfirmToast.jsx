import { toast } from "react-toastify";

export const confirmToast = (message, options = {}) => {
  const {
    type = "default", // "default", "danger", "warning"
    title = "Confirm Action",
    confirmText = "OK",
    cancelText = "Cancel",
  } = options;

  return new Promise((resolve) => {
    toast(
      ({ closeToast }) => (
        <div
          className='bg-white rounded-xl shadow-xl'
          style={{ width: "420px", maxWidth: "90vw" }}
        >
          {/* Modal Header */}
          <div className='p-5 border-b border-gray-200'>
            <div className='flex items-start gap-3'>
              <div
                className={`w-10 h-10 rounded-lg flex items-center justify-center flex-shrink-0 ${
                  type === "danger"
                    ? "bg-red-100"
                    : type === "warning"
                    ? "bg-yellow-100"
                    : "bg-blue-100"
                }`}
              >
                {type === "danger" ? (
                  <svg
                    className='w-5 h-5 text-red-600'
                    fill='currentColor'
                    viewBox='0 0 20 20'
                  >
                    <path
                      fillRule='evenodd'
                      d='M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z'
                      clipRule='evenodd'
                    />
                  </svg>
                ) : type === "warning" ? (
                  <svg
                    className='w-5 h-5 text-yellow-600'
                    fill='currentColor'
                    viewBox='0 0 20 20'
                  >
                    <path
                      fillRule='evenodd'
                      d='M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z'
                      clipRule='evenodd'
                    />
                  </svg>
                ) : (
                  <svg
                    className='w-5 h-5 text-blue-600'
                    fill='currentColor'
                    viewBox='0 0 20 20'
                  >
                    <path
                      fillRule='evenodd'
                      d='M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z'
                      clipRule='evenodd'
                    />
                  </svg>
                )}
              </div>
              <div className='flex-1'>
                <h3 className='text-lg font-bold text-gray-900'>{title}</h3>
                <p className='text-sm text-gray-600 mt-1 leading-relaxed'>
                  {message}
                </p>
              </div>
            </div>
          </div>

          {/* Modal Footer */}
          <div className='flex items-center justify-end gap-3 px-5 py-4 bg-gray-50 rounded-b-xl'>
            <button
              className='px-4 py-2 rounded-lg bg-gray-100 text-gray-700 font-medium hover:bg-gray-200 transition-colors'
              onClick={() => {
                resolve(false);
                closeToast();
              }}
            >
              {cancelText}
            </button>
            <button
              className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                type === "danger"
                  ? "bg-red-500 text-white hover:bg-red-600"
                  : type === "warning"
                  ? "bg-orange-500 text-white hover:bg-orange-600"
                  : "bg-blue-500 text-white hover:bg-blue-600"
              }`}
              onClick={() => {
                resolve(true);
                closeToast();
              }}
            >
              {confirmText}
            </button>
          </div>
        </div>
      ),
      {
        autoClose: false,
        closeOnClick: false,
        draggable: false,
        closeButton: false,
        position: "top-center",
        style: {
          background: "transparent",
          boxShadow: "none",
          padding: 0,
          width: "420px",
          maxWidth: "90vw",
        },
      }
    );
  });
};

export const confirmToastWithOptions = () => {
  return new Promise((resolve) => {
    toast(
      ({ closeToast }) => (
        <div
          className='bg-white rounded-2xl shadow-2xl'
          style={{ width: "560px", maxWidth: "90vw" }}
        >
          {/* Modal Header */}
          <div className='flex items-center justify-between p-6 border-b border-gray-200'>
            <div className='flex items-center gap-3'>
              <div className='w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center'>
                <svg
                  className='w-6 h-6 text-green-600'
                  fill='currentColor'
                  viewBox='0 0 20 20'
                >
                  <path
                    fillRule='evenodd'
                    d='M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z'
                    clipRule='evenodd'
                  />
                </svg>
              </div>
              <div>
                <h3 className='text-xl font-bold text-gray-900'>
                  Choose Your Action
                </h3>
                <p className='text-sm text-gray-500'>
                  What would you like to do with your interview session?
                </p>
              </div>
            </div>
          </div>

          {/* Modal Body */}
          <div className='p-6 space-y-4'>
            {/* Option 1: End Interview & Get Feedback */}
            <button
              onClick={() => {
                resolve("feedback");
                closeToast();
              }}
              className='w-full group bg-gradient-to-br from-green-50 to-white border-2 border-green-200 hover:border-green-400 rounded-xl p-6 text-left transition-all hover:shadow-lg'
            >
              <div className='flex items-start gap-4'>
                <div className='w-12 h-12 bg-green-100 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-green-200 transition-colors'>
                  <svg
                    className='w-6 h-6 text-green-600'
                    fill='currentColor'
                    viewBox='0 0 20 20'
                  >
                    <path
                      fillRule='evenodd'
                      d='M6 2a1 1 0 00-1 1v1H4a2 2 0 00-2 2v10a2 2 0 002 2h12a2 2 0 002-2V6a2 2 0 00-2-2h-1V3a1 1 0 10-2 0v1H7V3a1 1 0 00-1-1zm0 5a1 1 0 000 2h8a1 1 0 100-2H6z'
                      clipRule='evenodd'
                    />
                  </svg>
                </div>
                <div className='flex-1'>
                  <h4 className='text-lg font-bold text-gray-900 mb-2 group-hover:text-green-600 transition-colors'>
                    End Interview & Get Feedback
                  </h4>
                  <p className='text-sm text-gray-600 mb-3'>
                    Finish your interview session and receive detailed feedback
                    on your performance.
                  </p>
                </div>
                <svg
                  className='w-5 h-5 text-gray-400 group-hover:text-green-600 group-hover:translate-x-1 transition-all'
                  fill='none'
                  stroke='currentColor'
                  viewBox='0 0 24 24'
                >
                  <path
                    strokeLinecap='round'
                    strokeLinejoin='round'
                    strokeWidth={2}
                    d='M9 5l7 7-7 7'
                  />
                </svg>
              </div>
            </button>

            {/* Option 2: Continue Interview */}
            <button
              onClick={() => {
                resolve("stop");
                closeToast();
              }}
              className='w-full group bg-gradient-to-br from-blue-50 to-white border-2 border-blue-200 hover:border-blue-400 rounded-xl p-6 text-left transition-all hover:shadow-lg'
            >
              <div className='flex items-start gap-4'>
                <div className='w-12 h-12 bg-blue-100 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-blue-200 transition-colors'>
                  <svg
                    className='w-6 h-6 text-blue-600'
                    fill='currentColor'
                    viewBox='0 0 20 20'
                  >
                    <path
                      fillRule='evenodd'
                      d='M10 18a8 8 0 100-16 8 8 0 000 16zM9.555 7.168A1 1 0 008 8v4a1 1 0 001.555.832l3-2a1 1 0 000-1.664l-3-2z'
                      clipRule='evenodd'
                    />
                  </svg>
                </div>
                <div className='flex-1'>
                  <h4 className='text-lg font-bold text-gray-900 mb-2 group-hover:text-blue-600 transition-colors'>
                    Continue Interview
                  </h4>
                  <p className='text-sm text-gray-600 mb-3'>
                    Keep going with your interview session. Your progress is
                    saved automatically.
                  </p>
                </div>
                <svg
                  className='w-5 h-5 text-gray-400 group-hover:text-blue-600 group-hover:translate-x-1 transition-all'
                  fill='none'
                  stroke='currentColor'
                  viewBox='0 0 24 24'
                >
                  <path
                    strokeLinecap='round'
                    strokeLinejoin='round'
                    strokeWidth={2}
                    d='M9 5l7 7-7 7'
                  />
                </svg>
              </div>
            </button>

            {/* Option 3: Exit to Home */}
            <button
              onClick={() => {
                resolve("home");
                closeToast();
              }}
              className='w-full group bg-gradient-to-br from-red-50 to-white border-2 border-red-200 hover:border-red-400 rounded-xl p-6 text-left transition-all hover:shadow-lg'
            >
              <div className='flex items-start gap-4'>
                <div className='w-12 h-12 bg-red-100 rounded-xl flex items-center justify-center flex-shrink-0 group-hover:bg-red-200 transition-colors'>
                  <svg
                    className='w-6 h-6 text-red-600'
                    fill='currentColor'
                    viewBox='0 0 20 20'
                  >
                    <path d='M10.707 2.293a1 1 0 00-1.414 0l-7 7a1 1 0 001.414 1.414L4 10.414V17a1 1 0 001 1h2a1 1 0 001-1v-2a1 1 0 011-1h2a1 1 0 011 1v2a1 1 0 001 1h2a1 1 0 001-1v-6.586l.293.293a1 1 0 001.414-1.414l-7-7z' />
                  </svg>
                </div>
                <div className='flex-1'>
                  <h4 className='text-lg font-bold text-gray-900 mb-2 group-hover:text-red-600 transition-colors'>
                    Stop Interview & Return Home
                  </h4>
                  <p className='text-sm text-gray-600 mb-3'>
                    Exit the interview session and return to the home page
                    without saving.
                  </p>
                </div>
                <svg
                  className='w-5 h-5 text-gray-400 group-hover:text-red-600 group-hover:translate-x-1 transition-all'
                  fill='none'
                  stroke='currentColor'
                  viewBox='0 0 24 24'
                >
                  <path
                    strokeLinecap='round'
                    strokeLinejoin='round'
                    strokeWidth={2}
                    d='M9 5l7 7-7 7'
                  />
                </svg>
              </div>
            </button>
          </div>

          {/* Modal Footer */}
          <div className='px-6 py-4 bg-gray-50 rounded-b-2xl'>
            <p className='text-xs text-gray-500 text-center'>
              💡 Tip: You can always come back and continue your interview later
            </p>
          </div>
        </div>
      ),
      {
        autoClose: false,
        closeOnClick: false,
        draggable: false,
        closeButton: false,
        position: "top-center",
        style: {
          background: "transparent",
          boxShadow: "none",
          padding: 0,
          width: "560px",
          maxWidth: "90vw",
        },
      }
    );
  });
};
