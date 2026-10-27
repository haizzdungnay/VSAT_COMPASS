import React, { useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';
import { ExamList } from './pages/ExamList';
import { CreateExamWizard } from './pages/ExamWizard';
import { QuestionBank } from './pages/QuestionBank';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<string>('exams');

  const tabTitles: Record<string, string> = {
    dashboard: 'Dashboard Tổng quan',
    exams: 'Quản lý đề thi V-SAT 2026',
    'create-exam': 'Tạo đề thi mới (Wizard 4 bước)',
    blueprints: 'Ma trận đề thi V-SAT',
    schedules: 'Ca thi & Lịch thi',
    'question-bank': 'Ngân hàng câu hỏi V-SAT 2026',
    'import-questions': 'Import câu hỏi',
    settings: 'Cấu hình hệ thống'
  };

  return (
    <div className="min-h-screen bg-surface font-body-md text-on-surface antialiased">
      {/* 260px Left Sidebar */}
      <Sidebar currentTab={currentTab} onSelectTab={setCurrentTab} />

      {/* Main Content Area */}
      <div className="pl-[260px]">
        {/* Fixed Header */}
        <Header currentTabName={tabTitles[currentTab] || 'Quản lý đề thi'} />

        {/* Dynamic Page Views */}
        <main className="relative pt-16 w-full min-h-screen bg-background">
          {currentTab === 'exams' && (
            <ExamList onCreateClick={() => setCurrentTab('create-exam')} />
          )}

          {currentTab === 'create-exam' && (
            <CreateExamWizard onBackToList={() => setCurrentTab('exams')} />
          )}

          {currentTab === 'question-bank' && (
            <QuestionBank />
          )}

          {currentTab === 'blueprints' && (
            <div className="p-margin space-y-4">
              <div className="flex justify-between items-center">
                <h1 className="font-headline-lg text-on-surface">Ma trận đề thi V-SAT</h1>
              </div>
              <div className="p-6 rounded-xl bg-surface-container-lowest border border-outline-variant/30 text-center space-y-2">
                <span className="material-symbols-outlined text-[48px] text-primary">grid_view</span>
                <h3 className="font-headline-sm text-on-surface">Khung Ma trận Khảo thí 3 chiều</h3>
                <p className="text-body-sm text-outline max-w-md mx-auto">
                  Quản lý các bản blueprint phân bổ 25 câu hỏi theo chủ đề kiến thức x mức độ nhận thức (NB/TH/VD/VDC) x 4 dạng câu hỏi.
                </p>
              </div>
            </div>
          )}

          {currentTab === 'schedules' && (
            <div className="p-margin space-y-4">
              <h1 className="font-headline-lg text-on-surface">Ca thi & Lịch thi</h1>
              <div className="p-6 rounded-xl bg-surface-container-lowest border border-outline-variant/30 text-center space-y-2">
                <span className="material-symbols-outlined text-[48px] text-primary">pending_actions</span>
                <h3 className="font-headline-sm text-on-surface">Cấu hình Phòng thi & Ca thi (Exam Windows)</h3>
                <p className="text-body-sm text-outline max-w-md mx-auto">
                  Thiết lập thời gian bắt đầu, thời gian đóng ca, trễ giờ tối đa và chính sách công bố điểm tự động.
                </p>
              </div>
            </div>
          )}

          {currentTab === 'dashboard' && (
            <div className="p-margin space-y-4">
              <h1 className="font-headline-lg text-on-surface">Dashboard Tổng quan</h1>
              <div className="p-6 rounded-xl bg-surface-container-lowest border border-outline-variant/30 text-center space-y-2">
                <span className="material-symbols-outlined text-[48px] text-primary">dashboard</span>
                <h3 className="font-headline-sm text-on-surface">Trung tâm Điều hành V-SAT Compass</h3>
                <p className="text-body-sm text-outline max-w-md mx-auto">
                  Theo dõi số lượng ca thi trực tuyến, tổng số học sinh đang thi và tỷ lệ phân hóa điểm số.
                </p>
              </div>
            </div>
          )}

          {currentTab === 'settings' && (
            <div className="p-margin space-y-4">
              <h1 className="font-headline-lg text-on-surface">Cấu hình & Quy chuẩn</h1>
              <div className="p-6 rounded-xl bg-surface-container-lowest border border-outline-variant/30 text-center space-y-2">
                <span className="material-symbols-outlined text-[48px] text-primary">settings</span>
                <h3 className="font-headline-sm text-on-surface">Quy chuẩn Điểm số & Chấm thi</h3>
                <p className="text-body-sm text-outline max-w-md mx-auto">
                  Cấu hình thang điểm 150 điểm thô, quy tắc tính điểm lũy tiến Đúng/Sai và kết nối API Spring Boot.
                </p>
              </div>
            </div>
          )}
        </main>
      </div>
    </div>
  );
};

export default App;
