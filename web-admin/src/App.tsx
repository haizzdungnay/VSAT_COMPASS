import React, { useState } from 'react';
import { Sidebar } from './components/layout/Sidebar';
import { Header } from './components/layout/Header';
import { ExamList } from './pages/ExamList';
import { CreateExamWizard } from './pages/ExamWizard';
import { QuestionBank } from './pages/QuestionBank';
import { ReviewQueue } from './pages/ReviewQueue';
import { ImportQuestions } from './pages/ImportQuestions';
import { BlueprintsPage } from './pages/Blueprints';
import { SchedulesPage } from './pages/Schedules';
import { UsersPage } from './pages/Users';
import { TicketsPage } from './pages/Tickets';
import { StudentExamLobby } from './pages/StudentCBT/StudentExamLobby';
import { StudentCBTWorkspace } from './pages/StudentCBT/StudentCBTWorkspace';
import { StudentScorecard } from './pages/StudentCBT/StudentScorecard';
import type { BlueprintItem } from './types/exam';
import type { StudentSessionInfo } from './types/student-cbt';

export const App: React.FC = () => {
  const [currentTab, setCurrentTab] = useState<string>('exams');
  const [selectedBlueprintForWizard, setSelectedBlueprintForWizard] = useState<BlueprintItem | null>(null);
  const [activeCbtSession, setActiveCbtSession] = useState<StudentSessionInfo | null>(null);
  const [cbtExamTitle, setCbtExamTitle] = useState<string>('Đề thi V-SAT 2026');
  const [cbtDurationMinutes, setCbtDurationMinutes] = useState<number>(90);

  const handleSelectTab = (tab: string) => {
    if (tab !== 'create-exam') {
      setSelectedBlueprintForWizard(null);
    }
    setCurrentTab(tab);
  };

  const tabTitles: Record<string, string> = {
    dashboard: 'Dashboard Tổng quan',
    exams: 'Quản lý đề thi V-SAT 2026',
    'create-exam': 'Tạo đề thi mới (Wizard 4 bước)',
    blueprints: 'Ma trận đề thi V-SAT',
    schedules: 'Ca thi & Lịch thi',
    'question-bank': 'Ngân hàng câu hỏi V-SAT 2026',
    'review-queue': 'Hàng đợi duyệt câu hỏi V-SAT',
    'import-questions': 'Import câu hỏi',
    users: 'Quản lý người dùng',
    tickets: 'Báo lỗi & Phản hồi (Ticket)',
    settings: 'Cấu hình hệ thống',
    'student-lobby': 'Cổng thi trực tuyến (CBT)',
    'student-workspace': 'Phòng thi trực tuyến (CBT)',
    'student-scorecard': 'Bảng điểm & Rà soát kết quả'
  };

  return (
    <div className="min-h-screen bg-surface font-body-md text-on-surface antialiased">
      {/* 260px Left Sidebar */}
      <Sidebar currentTab={currentTab} onSelectTab={handleSelectTab} />

      {/* Main Content Area */}
      <div className="pl-[260px]">
        {/* Fixed Header */}
        <Header currentTabName={tabTitles[currentTab] || 'Quản lý đề thi'} />

        {/* Dynamic Page Views */}
        <main className="relative pt-16 w-full min-h-screen bg-background">
          {currentTab === 'exams' && (
            <ExamList onCreateClick={() => { setSelectedBlueprintForWizard(null); setCurrentTab('create-exam'); }} />
          )}

          {currentTab === 'create-exam' && (
            <CreateExamWizard
              initialBlueprint={selectedBlueprintForWizard}
              onBackToList={() => { setSelectedBlueprintForWizard(null); setCurrentTab('exams'); }}
            />
          )}

          {currentTab === 'question-bank' && (
            <QuestionBank />
          )}

          {currentTab === 'review-queue' && (
            <ReviewQueue />
          )}

          {currentTab === 'import-questions' && (
            <ImportQuestions />
          )}

          {currentTab === 'blueprints' && (
            <BlueprintsPage onCreateExamClick={(bp) => {
              setSelectedBlueprintForWizard(bp || null);
              setCurrentTab('create-exam');
            }} />
          )}

          {currentTab === 'schedules' && (
            <SchedulesPage />
          )}

          {currentTab === 'users' && (
            <UsersPage />
          )}

          {currentTab === 'tickets' && (
            <TicketsPage />
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

          {currentTab === 'student-lobby' && (
            <StudentExamLobby
              onStartExam={(sessionInfo, examTitle, durationMinutes) => {
                setActiveCbtSession(sessionInfo);
                setCbtExamTitle(examTitle);
                setCbtDurationMinutes(durationMinutes);
                setCurrentTab('student-workspace');
              }}
            />
          )}

          {currentTab === 'student-workspace' && activeCbtSession && (
            <StudentCBTWorkspace
              session={activeCbtSession}
              examTitle={cbtExamTitle}
              durationMinutes={cbtDurationMinutes}
              onFinishSession={(submittedSession) => {
                setActiveCbtSession(submittedSession);
                setCurrentTab('student-scorecard');
              }}
              onExitLobby={() => setCurrentTab('student-lobby')}
            />
          )}

          {currentTab === 'student-scorecard' && activeCbtSession && (
            <StudentScorecard
              session={activeCbtSession}
              examTitle={cbtExamTitle}
              onReturnToLobby={() => {
                setActiveCbtSession(null);
                setCurrentTab('student-lobby');
              }}
            />
          )}
        </main>
      </div>
    </div>
  );
};

export default App;
