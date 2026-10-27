import React from 'react';

interface SidebarProps {
  currentTab: string;
  onSelectTab: (tab: string) => void;
}

export const Sidebar: React.FC<SidebarProps> = ({ currentTab, onSelectTab }) => {
  return (
    <aside className="fixed left-0 top-0 h-full w-[260px] bg-surface-container-lowest z-50 flex flex-col justify-between border-r border-outline-variant/30 select-none">
      <div className="flex flex-col flex-1 min-h-0">
        {/* Brand header */}
        <div className="h-16 px-space-lg flex items-center gap-space-md border-b border-outline-variant/20 flex-shrink-0">
          <div className="w-9 h-9 rounded-xl bg-primary flex items-center justify-center text-on-primary shadow-sm flex-shrink-0">
            <span className="material-symbols-outlined text-[20px]">explore</span>
          </div>
          <div className="flex flex-col min-w-0">
            <span className="font-headline-sm text-[15px] font-bold text-primary tracking-tight leading-snug truncate">
              V-SAT COMPASS
            </span>
            <span className="font-label-sm text-[10px] text-on-surface-variant font-medium tracking-normal truncate">
              Hệ thống Quản trị Học thuật
            </span>
          </div>
        </div>

        {/* Navigation Sections */}
        <div className="flex-1 overflow-y-auto px-space-md py-space-md space-y-space-md">
          {/* Tổng quan */}
          <div className="space-y-space-xs">
            <div className="px-space-sm font-label-sm text-[10px] uppercase text-outline font-bold tracking-wider">
              Tổng quan
            </div>
            <nav className="space-y-0.5">
              <button
                onClick={() => onSelectTab('dashboard')}
                className={`w-full flex items-center gap-space-md px-space-sm py-2 rounded-lg font-label-md transition-colors ${
                  currentTab === 'dashboard'
                    ? 'bg-primary-container text-on-primary font-medium shadow-sm'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">dashboard</span>
                <span>Dashboard</span>
              </button>
            </nav>
          </div>

          {/* Đề thi */}
          <div className="space-y-space-xs">
            <div className="px-space-sm font-label-sm text-[10px] uppercase text-outline font-bold tracking-wider">
              Đề thi V-SAT 2026
            </div>
            <nav className="space-y-0.5">
              <button
                onClick={() => onSelectTab('exams')}
                className={`w-full flex items-center gap-space-md px-space-sm py-2 rounded-lg font-label-md transition-colors ${
                  currentTab === 'exams'
                    ? 'bg-primary-container text-on-primary font-medium shadow-sm'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">menu_book</span>
                <span>Danh sách đề thi</span>
              </button>
              <button
                onClick={() => onSelectTab('create-exam')}
                className={`w-full flex items-center gap-space-md px-space-sm py-2 rounded-lg font-label-md transition-colors ${
                  currentTab === 'create-exam'
                    ? 'bg-primary-container text-on-primary font-medium shadow-sm'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">add_circle</span>
                <span>Tạo đề thi mới (Wizard)</span>
              </button>
              <button
                onClick={() => onSelectTab('blueprints')}
                className={`w-full flex items-center gap-space-md px-space-sm py-2 rounded-lg font-label-md transition-colors ${
                  currentTab === 'blueprints'
                    ? 'bg-primary-container text-on-primary font-medium shadow-sm'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">grid_view</span>
                <span>Ma trận đề thi</span>
              </button>
              <button
                onClick={() => onSelectTab('schedules')}
                className={`w-full flex items-center gap-space-md px-space-sm py-2 rounded-lg font-label-md transition-colors ${
                  currentTab === 'schedules'
                    ? 'bg-primary-container text-on-primary font-medium shadow-sm'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">pending_actions</span>
                <span>Ca thi & Lịch thi</span>
              </button>
            </nav>
          </div>

          {/* Nội dung */}
          <div className="space-y-space-xs">
            <div className="px-space-sm font-label-sm text-[10px] uppercase text-outline font-bold tracking-wider">
              Ngân hàng câu hỏi
            </div>
            <nav className="space-y-0.5">
              <button
                onClick={() => onSelectTab('question-bank')}
                className={`w-full flex items-center gap-space-md px-space-sm py-2 rounded-lg font-label-md transition-colors ${
                  currentTab === 'question-bank'
                    ? 'bg-primary-container text-on-primary font-medium shadow-sm'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">database</span>
                <span>Ngân hàng câu hỏi</span>
              </button>
              <button
                onClick={() => onSelectTab('import-questions')}
                className={`w-full flex items-center gap-space-md px-space-sm py-2 rounded-lg font-label-md transition-colors ${
                  currentTab === 'import-questions'
                    ? 'bg-primary-container text-on-primary font-medium shadow-sm'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">cloud_upload</span>
                <span>Import Excel / Word</span>
              </button>
            </nav>
          </div>

          {/* Quản trị */}
          <div className="space-y-space-xs">
            <div className="px-space-sm font-label-sm text-[10px] uppercase text-outline font-bold tracking-wider">
              Quản trị hệ thống
            </div>
            <nav className="space-y-0.5">
              <button
                onClick={() => onSelectTab('settings')}
                className={`w-full flex items-center gap-space-md px-space-sm py-2 rounded-lg font-label-md transition-colors ${
                  currentTab === 'settings'
                    ? 'bg-primary-container text-on-primary font-medium shadow-sm'
                    : 'text-on-surface-variant hover:bg-surface-container-high hover:text-on-surface'
                }`}
              >
                <span className="material-symbols-outlined text-[18px]">settings</span>
                <span>Cấu hình & Quy chuẩn</span>
              </button>
            </nav>
          </div>
        </div>
      </div>

      {/* Role Footer */}
      <div className="p-space-md border-t border-outline-variant/30 bg-surface-container-low/50">
        <div className="bg-surface-container-lowest p-space-sm rounded-lg border border-outline-variant/40 space-y-1">
          <div className="flex items-center justify-between">
            <span className="font-label-sm text-[10px] text-on-surface-variant font-bold uppercase tracking-wider">
              Vai trò
            </span>
            <span className="font-label-sm text-[10px] px-1.5 py-0.5 rounded bg-primary-fixed text-on-primary-fixed font-bold leading-none">
              CONTENT_ADMIN
            </span>
          </div>
          <div className="flex items-center gap-1.5 pt-0.5 text-outline text-[11px] font-body-sm">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600 flex-shrink-0 animate-pulse"></span>
            <span className="truncate font-mono text-[10px]">Spring Boot :18080 • UP</span>
          </div>
        </div>
      </div>
    </aside>
  );
};
