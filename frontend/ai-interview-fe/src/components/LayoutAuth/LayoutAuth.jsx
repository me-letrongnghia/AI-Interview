import React from "react";
import LinhVat from "../../assets/main.png";
import { Outlet } from "react-router-dom";

export const LayoutAuth = () => {
  return (
    <div className="h-screen bg-white overflow-hidden flex flex-col">
      <div className="flex-1 overflow-auto">
        <div className="mx-auto px-4 py-8 max-w-[1080px] h-full">
          <div className="flex items-center h-full gap-x-10">
            <img
              src={LinhVat}
              alt="Panda corner"
              className="fixed bottom-0 left-0 w-[700px] h-[700px] object-contain translate-x-[-30%] translate-y-[50%] opacity-20 pointer-events-none select-none hidden md:block"
            />
            {/* ==== LOGIN ==== */}
            <Outlet />
            {/* ==== PANDA ==== */}
            <div className="hidden lg:flex flex-1 items-center justify-center relative translate-x-[20%]">
              <img
                src={LinhVat}
                alt="Panda Mascot"
                className="scale-x-[-1] max-h-[600px] object-contain"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
