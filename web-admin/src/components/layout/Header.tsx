import React from 'react';

interface HeaderProps {
  currentTabName: string;
  onSearchChange?: (val: string) => void;
}

export const Header: React.FC<HeaderProps> = ({ currentTabName, onSearchChange }) => {
  return (
    <header className="fixed top-0 left-[260px] right-0 h-16 bg-surface-container-lowest/90 backdrop-blur-md z-40 border-b border-outline-variant/30 flex items-center justify-between px-margin">
      <div className="flex items-center gap-space-lg min-w-0">
        <div className="flex items-center gap-1.5 text-on-surface-variant font-label-md text-body-sm whitespace-nowrap">
          <span className="hover:text-primary cursor-pointer transition-colors">Đề thi</span>
          <span className="material-symbols-outlined text-[14px] text-outline">chevron_right</span>
          <span className="text-on-surface font-semibold">{currentTabName}</span>
        </div>
        <div className="relative hidden xl:block w-72">
          <span className="material-symbols-outlined absolute left-2.5 top-1/2 -translate-y-1/2 text-outline text-[18px]">
            search
          </span>
          <input
            onChange={(e) => onSearchChange?.(e.target.value)}
            className="w-full h-9 pl-9 pr-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40 text-on-surface placeholder:text-outline focus:outline-none focus:border-primary-container focus:ring-1 focus:ring-primary-container transition-all"
            placeholder="Tìm kiếm mã đề, môn thi..."
            type="text"
          />
        </div>
      </div>

      <div className="flex items-center gap-space-md flex-shrink-0">
        <div className="hidden lg:flex items-center gap-2 px-space-sm py-1 rounded-lg bg-surface-container border border-outline-variant/30 text-on-surface-variant font-label-sm">
          <span className="w-2 h-2 rounded-full bg-emerald-600 animate-pulse flex-shrink-0"></span>
          <span className="truncate font-medium">Hệ thống Quản trị V-SAT 2026</span>
          <span className="text-outline">|</span>
          <span className="text-[10px] text-outline font-normal font-mono">Backend: Online</span>
        </div>

        <div className="flex items-center gap-1 bg-secondary-fixed/50 px-2 py-1 rounded-lg">
          <span className="material-symbols-outlined text-on-secondary-fixed-variant text-[16px]">
            admin_panel_settings
          </span>
          <span className="font-label-sm text-[10px] font-bold text-on-secondary-fixed-variant uppercase tracking-wider">
            CONTENT_ADMIN
          </span>
        </div>

        <button
          className="relative w-9 h-9 flex items-center justify-center rounded-lg text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface transition-colors"
          type="button"
        >
          <span className="material-symbols-outlined text-[20px]">notifications</span>
          <span className="absolute top-1.5 right-1.5 w-2 h-2 rounded-full bg-error ring-2 ring-surface-container-lowest"></span>
        </button>

        <div className="h-6 w-px bg-outline-variant/40 mx-0.5"></div>

        <div className="flex items-center gap-space-sm pl-1 cursor-pointer group">
          <div className="w-8 h-8 rounded-full bg-primary flex items-center justify-center flex-shrink-0">
            <span className="material-symbols-outlined text-on-primary text-[18px]">person</span>
          </div>
          <div className="hidden md:flex flex-col text-left">
            <span className="font-label-md text-on-surface leading-tight truncate group-hover:text-primary transition-colors">
              Ths. Nguyễn Văn An
            </span>
            <span className="font-body-sm text-[11px] text-on-surface-variant leading-tight truncate">
              Quản trị viên nội dung
            </span>
          </div>
          <span className="material-symbols-outlined text-outline text-[16px] group-hover:text-on-surface transition-colors">
            expand_more
          </span>
        </div>
      </div>
    </header>
  );
};
