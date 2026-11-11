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
import { Dashboard } from './pages/Dashboard';
import { SettingsPage } from './pages/Settings';
import { LoginPage } from './pages/Login';
import { StudentExamLobby } from './pages/StudentCBT/StudentExamLobby';
import { StudentCBTWorkspace } from './pages/StudentCBT/StudentCBTWorkspace';
import { StudentScorecard } from './pages/StudentCBT/StudentScorecard';
import { AdmissionPredictorWidget } from './pages/StudentCBT/AdmissionPredictorWidget';
import type { BlueprintItem, ExamDetail } from './types/exam';
import type { StudentSessionInfo } from './types/student-cbt';

export const App: React.FC = () => {
  const [role, setRole] = useState<'ADMIN' | 'STUDENT' | 'LOGGED_OUT'>(() => {
    // ponytail: default to ADMIN for clean-room E2E harness; switchable via Header/Login
    const saved = localStorage.getItem('vsat_web_role');
    if (saved === 'STUDENT' || saved === 'LOGGED_OUT') return saved;
    return 'ADMIN';
  });

  const [currentTab, setCurrentTab] = useState<string>('exams');
  const [selectedBlueprintForWizard, setSelectedBlueprintForWizard] = useState<BlueprintItem | null>(null);
  const [editingExam, setEditingExam] = useState<ExamDetail | null>(null);
  const [activeCbtSession, setActiveCbtSession] = useState<StudentSessionInfo | null>(null);
  const [cbtExamTitle, setCbtExamTitle] = useState<string>('Đề thi V-SAT 2026');
  const [cbtDurationMinutes, setCbtDurationMinutes] = useState<number>(90);

  const handleSwitchRole = (newRole: 'ADMIN' | 'STUDENT' | 'LOGGED_OUT') => {
    localStorage.setItem('vsat_web_role', newRole);
    setRole(newRole);
    if (newRole === 'STUDENT') {
      setCurrentTab('student-lobby');
    } else if (newRole === 'ADMIN') {
      setCurrentTab('exams');
    }
  };

  const handleSelectTab = (tab: string) => {
    if (tab !== 'create-exam') {
      setSelectedBlueprintForWizard(null);
      setEditingExam(null);
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
    settings: 'Cấu hình & Quy chuẩn',
    'student-lobby': 'Cổng thi trực tuyến (CBT)',
    'student-workspace': 'Phòng thi trực tuyến (CBT)',
    'student-scorecard': 'Bảng điểm & Rà soát kết quả',
    'student-admission': 'Dự đoán trúng tuyển Đại học'
  };

  if (role === 'LOGGED_OUT') {
    return (
      <LoginPage
        onLoginSuccess={(newRole) => {
          handleSwitchRole(newRole);
        }}
      />
    );
  }

  const isAdmin = role === 'ADMIN';

  return (
    <div className="min-h-screen bg-surface font-body-md text-on-surface antialiased">
      {/* 260px Left Sidebar (Admin only) */}
      {isAdmin && <Sidebar currentTab={currentTab} onSelectTab={handleSelectTab} />}

      {/* Main Content Area */}
      <div className={isAdmin ? 'pl-[260px]' : 'pl-0'}>
        {/* Fixed Header */}
        <Header
          currentTabName={tabTitles[currentTab] || 'Quản lý đề thi'}
          role={role}
          onSwitchRole={handleSwitchRole}
        />

        {/* Dynamic Page Views */}
        <main className="relative pt-16 w-full min-h-screen bg-background">
          {/* Compact Student Navigation Bar - NO h1, h3, p */}
          {!isAdmin && (
            <div className="bg-surface-container-lowest border-b border-outline-variant/30 px-margin py-2.5 flex items-center justify-between gap-4">
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setCurrentTab('student-lobby')}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-all ${
                    currentTab === 'student-lobby' || currentTab === 'student-workspace' || currentTab === 'student-scorecard'
                      ? 'bg-primary text-on-primary shadow-xs'
                      : 'bg-surface-container hover:bg-surface-container-high text-on-surface-variant'
                  }`}
                >
                  <span className="material-symbols-outlined text-[16px]">laptop_mac</span>
                  <span>Làm bài thi CBT</span>
                </button>
                <button
                  type="button"
                  onClick={() => setCurrentTab('student-admission')}
                  className={`px-3 py-1.5 rounded-lg text-xs font-bold flex items-center gap-1.5 transition-all ${
                    currentTab === 'student-admission'
                      ? 'bg-primary text-on-primary shadow-xs'
                      : 'bg-surface-container hover:bg-surface-container-high text-on-surface-variant'
                  }`}
                >
                  <span className="material-symbols-outlined text-[16px]">psychology</span>
                  <span>Dự đoán trúng tuyển Đại học</span>
                </button>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => handleSwitchRole('ADMIN')}
                  className="px-2.5 py-1.5 rounded-lg text-xs font-bold text-primary hover:bg-primary/10 transition-colors flex items-center gap-1"
                >
                  <span className="material-symbols-outlined text-[16px]">admin_panel_settings</span>
                  <span>Chuyển sang Quản trị (Admin)</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleSwitchRole('LOGGED_OUT')}
                  className="px-2.5 py-1.5 rounded-lg text-xs font-bold text-rose-600 hover:bg-rose-50 transition-colors flex items-center gap-1"
                >
                  <span className="material-symbols-outlined text-[16px]">logout</span>
                  <span>Đăng xuất</span>
                </button>
              </div>
            </div>
          )}

          {currentTab === 'exams' && (
            <ExamList
              onCreateClick={() => {
                setSelectedBlueprintForWizard(null);
                setEditingExam(null);
                setCurrentTab('create-exam');
              }}
              onDetailClick={(examItem) => {
                setSelectedBlueprintForWizard(null);
                setEditingExam({
                  id: examItem.id,
                  examCode: examItem.examCode,
                  title: examItem.title,
                  subjectId: examItem.subjectId,
                  subjectName: examItem.subjectName,
                  questionCount: examItem.questionCount || 25,
                  durationMinutes: examItem.durationMinutes || 90,
                  difficulty: examItem.difficulty || 'MEDIUM',
                  pricingType: examItem.pricingType || 'FREE',
                  status: examItem.status || 'DRAFT',
                  lockVersion: examItem.lockVersion ?? 0,
                  deliveryMode: 'PRACTICE',
                  scoringProfileCode: 'VSAT_2026_RAW_150'
                });
                setCurrentTab('create-exam');
              }}
            />
          )}

          {currentTab === 'create-exam' && (
            <CreateExamWizard
              key={editingExam ? `edit-${editingExam.id}` : selectedBlueprintForWizard ? `bp-${selectedBlueprintForWizard.id}` : 'new-exam'}
              initialBlueprint={selectedBlueprintForWizard}
              initialExam={editingExam}
              onBackToList={() => {
                setSelectedBlueprintForWizard(null);
                setEditingExam(null);
                setCurrentTab('exams');
              }}
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
              setEditingExam(null);
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
            <div className="p-margin">
              <Dashboard onNavigate={handleSelectTab} />
            </div>
          )}

          {currentTab === 'settings' && (
            <div className="p-margin">
              <SettingsPage />
            </div>
          )}

          {currentTab === 'student-admission' && (
            <div className="p-margin max-w-5xl mx-auto">
              <AdmissionPredictorWidget initialScore={330} />
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
