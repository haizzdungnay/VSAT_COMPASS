import React, { useEffect, useState } from 'react';
import type { PublicExamSummary, PublicExamWindow, StudentSessionInfo, StudentExamMode } from '../../types/student-cbt';
import { fetchPublicExams, fetchPublicExamWindows, startStudentSession } from '../../services/api';

interface StudentExamLobbyProps {
  onStartExam: (sessionInfo: StudentSessionInfo, examTitle: string, durationMinutes: number) => void;
}

const SUBJECT_FILTERS = [
  'Tất cả',
  'Toán học',
  'Vật lí',
  'Hóa học',
  'Sinh học',
  'Tiếng Anh',
  'Lịch sử',
  'Địa lí'
];

export const StudentExamLobby: React.FC<StudentExamLobbyProps> = ({ onStartExam }) => {
  const [exams, setExams] = useState<PublicExamSummary[]>([]);
  const [windows, setWindows] = useState<PublicExamWindow[]>([]);
  const [selectedSubject, setSelectedSubject] = useState<string>('Tất cả');
  const [loading, setLoading] = useState<boolean>(true);
  const [startingId, setStartingId] = useState<number | null>(null);

  useEffect(() => {
    let isMounted = true;
    const loadData = async () => {
      try {
        const [examsRes, windowsRes] = await Promise.all([
          fetchPublicExams(0, 50),
          fetchPublicExamWindows()
        ]);
        if (isMounted) {
          setExams(examsRes.content || []);
          setWindows(windowsRes || []);
        }
      } catch (err: unknown) {
        console.warn('Load lobby data error:', err);
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };
    loadData();
    return () => {
      isMounted = false;
    };
  }, []);

  const handleStartExam = async (exam: PublicExamSummary, mode: StudentExamMode = 'MOCK_EXAM', examWindowId?: number) => {
    try {
      setStartingId(exam.id);
      const session = await startStudentSession({
        examId: exam.id,
        mode,
        totalQuestions: exam.questionCount,
        examWindowId
      });
      onStartExam(session, exam.title, exam.durationMinutes);
    } catch (err: unknown) {
      console.warn('Start session error:', err);
    } finally {
      setStartingId(null);
    }
  };

  const filteredExams = exams.filter(e => {
    if (selectedSubject === 'Tất cả') return true;
    return e.subjectName.toLowerCase().includes(selectedSubject.toLowerCase());
  });

  return (
    <div className="p-margin max-w-7xl mx-auto space-y-6">
      {/* Header Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-primary to-primary-container p-6 text-on-primary shadow-sm">
        <div className="relative z-10 max-w-3xl space-y-2">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-white/20 backdrop-blur-sm text-xs font-semibold tracking-wide">
            <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse"></span>
            HỆ THỐNG THI TRỰC TUYẾN COMPUTER-BASED TESTING (CBT)
          </div>
          <h1 className="font-headline-lg font-bold text-2xl md:text-3xl">
            Cổng Thi & Khảo Sát Năng Lực V-SAT 2026
          </h1>
          <p className="text-on-primary/90 text-sm md:text-base leading-relaxed">
            Môi trường thi chuẩn hóa theo quy định tuyển sinh đại học của Bộ Giáo dục & Đào tạo. Đồng hồ Web Worker độc lập, tự động lưu câu trả lời mỗi 15 giây và chấm điểm phân hóa 3 phần.
          </p>
        </div>
      </div>

      {/* V-SAT Standard Structure Overview */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 space-y-2 shadow-sm">
          <div className="flex items-center gap-2 text-primary font-bold text-sm">
            <span className="material-symbols-outlined text-[20px]">format_list_bulleted</span>
            <span>Phần 1: Trắc nghiệm 4 lựa chọn</span>
          </div>
          <p className="text-xs text-on-surface-variant">
            Chọn 1 đáp án chính xác trong số 4 phương án A, B, C, D. Nhận 0.25 điểm cho mỗi câu trả lời đúng.
          </p>
          <div className="text-[11px] font-semibold text-primary-fixed-dim">
            Chuẩn hóa 12 câu • Tối đa 3.0 điểm thô
          </div>
        </div>

        <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 space-y-2 shadow-sm">
          <div className="flex items-center gap-2 text-primary font-bold text-sm">
            <span className="material-symbols-outlined text-[20px]">check_circle</span>
            <span>Phần 2: Đúng / Sai 4 ý (a-b-c-d)</span>
          </div>
          <p className="text-xs text-on-surface-variant">
            Đánh giá tính đúng/sai của 4 mệnh đề độc lập. Điểm số lũy tiến theo số ý đúng: 1 ý (0.1đ), 2 ý (0.25đ), 3 ý (0.5đ), 4 ý (1.0đ).
          </p>
          <div className="text-[11px] font-semibold text-primary-fixed-dim">
            Chuẩn hóa 4 câu • Tối đa 4.0 điểm thô
          </div>
        </div>

        <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 space-y-2 shadow-sm">
          <div className="flex items-center gap-2 text-primary font-bold text-sm">
            <span className="material-symbols-outlined text-[20px]">edit_note</span>
            <span>Phần 3: Trả lời ngắn</span>
          </div>
          <p className="text-xs text-on-surface-variant">
            Thí sinh tự tính toán và nhập đáp số dưới dạng số nguyên hoặc số thập phân. Nhận 0.5 điểm cho mỗi câu trả lời chính xác.
          </p>
          <div className="text-[11px] font-semibold text-primary-fixed-dim">
            Chuẩn hóa 6-9 câu • Tối đa 3.0-4.5 điểm thô
          </div>
        </div>
      </div>

      {/* Ca thi sắp & đang diễn ra */}
      {windows.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-headline-sm font-bold text-lg text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[22px]">timer</span>
              Ca thi trực tuyến sắp & đang diễn ra
            </h2>
            <span className="text-xs text-on-surface-variant font-medium">
              {windows.length} ca thi đang mở
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {windows.map(win => (
              <div
                key={win.id}
                className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/40 hover:border-primary/50 transition-all space-y-3 flex flex-col justify-between"
              >
                <div className="space-y-1.5">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[11px] font-mono px-2 py-0.5 rounded bg-surface-container-high text-on-surface font-semibold">
                      {win.windowCode}
                    </span>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                        win.status === 'RUNNING'
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-amber-100 text-amber-800'
                      }`}
                    >
                      <span className={`w-1.5 h-1.5 rounded-full ${win.status === 'RUNNING' ? 'bg-emerald-600 animate-pulse' : 'bg-amber-600'}`}></span>
                      {win.status === 'RUNNING' ? 'Đang diễn ra' : 'Sắp diễn ra'}
                    </span>
                  </div>
                  <h3 className="font-headline-sm font-semibold text-sm text-on-surface line-clamp-2">
                    {win.name}
                  </h3>
                  <div className="text-xs text-on-surface-variant flex items-center gap-2">
                    <span>Môn: {win.subjectName || 'V-SAT'}</span>
                    <span>•</span>
                    <span>{win.activeCandidates ?? 0} thí sinh</span>
                  </div>
                </div>

                <button
                  onClick={() => {
                    const matchedExam = exams.find(e => e.id === win.examId) || {
                      id: win.examId,
                      examCode: win.examCode,
                      title: win.examTitle,
                      subjectId: 1,
                      subjectName: win.subjectName || 'Toán học',
                      questionCount: 25,
                      durationMinutes: 90,
                      difficulty: 'HARD' as const
                    };
                    handleStartExam(matchedExam, win.mode, win.id);
                  }}
                  disabled={startingId !== null}
                  className="w-full py-2 px-3 rounded-lg bg-primary text-on-primary font-label-md text-xs font-semibold hover:bg-primary/90 transition-colors flex items-center justify-center gap-1.5 shadow-sm disabled:opacity-50"
                >
                  <span className="material-symbols-outlined text-[16px]">play_circle</span>
                  <span>Vào thi ca này</span>
                </button>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Danh sách Đề thi công khai */}
      <div className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-outline-variant/30 pb-3">
          <div>
            <h2 className="font-headline-sm font-bold text-lg text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[22px]">assignment</span>
              Danh sách đề thi V-SAT công khai
            </h2>
            <p className="text-xs text-on-surface-variant">
              Chọn đề thi để luyện tập hoặc kiểm tra năng lực độc lập với bảng bấm giờ CBT
            </p>
          </div>

          {/* Subject Filter Pills */}
          <div className="flex items-center gap-1.5 overflow-x-auto pb-1 max-w-full">
            {SUBJECT_FILTERS.map(sub => (
              <button
                key={sub}
                onClick={() => setSelectedSubject(sub)}
                className={`px-3 py-1 rounded-full text-xs font-semibold whitespace-nowrap transition-colors ${
                  selectedSubject === sub
                    ? 'bg-primary text-on-primary shadow-sm'
                    : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'
                }`}
              >
                {sub}
              </button>
            ))}
          </div>
        </div>

        {loading ? (
          <div className="p-12 text-center text-on-surface-variant space-y-2">
            <span className="material-symbols-outlined text-3xl animate-spin text-primary">sync</span>
            <p className="text-sm">Đang tải danh sách đề thi V-SAT...</p>
          </div>
        ) : filteredExams.length === 0 ? (
          <div className="p-12 rounded-xl bg-surface-container-lowest border border-outline-variant/30 text-center text-on-surface-variant">
            <span className="material-symbols-outlined text-4xl text-outline mb-2">find_in_page</span>
            <p className="text-sm">Không tìm thấy đề thi phù hợp với bộ lọc môn học.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
            {filteredExams.map(exam => (
              <div
                key={exam.id}
                className="p-5 rounded-2xl bg-surface-container-lowest border border-outline-variant/30 hover:border-primary/50 hover:shadow-md transition-all flex flex-col justify-between space-y-4"
              >
                <div className="space-y-2.5">
                  <div className="flex items-center justify-between gap-2">
                    <span className="text-[11px] font-bold px-2.5 py-0.5 rounded-full bg-primary-container text-on-primary">
                      {exam.subjectName}
                    </span>
                    <span className="text-xs font-mono text-outline">
                      {exam.examCode}
                    </span>
                  </div>

                  <h3 className="font-headline-sm font-bold text-base text-on-surface line-clamp-2">
                    {exam.title}
                  </h3>

                  <p className="text-xs text-on-surface-variant line-clamp-2">
                    {exam.description || 'Đề thi khảo sát năng lực V-SAT bám sát cấu trúc 3 phần với câu hỏi thực tế và bài toán liên môn.'}
                  </p>

                  {/* Badges metadata */}
                  <div className="grid grid-cols-3 gap-2 pt-1 border-t border-outline-variant/20 text-center">
                    <div className="p-1.5 rounded bg-surface-container-low">
                      <div className="text-[10px] text-outline">Thời gian</div>
                      <div className="font-bold text-xs text-on-surface">{exam.durationMinutes} phút</div>
                    </div>
                    <div className="p-1.5 rounded bg-surface-container-low">
                      <div className="text-[10px] text-outline">Số câu</div>
                      <div className="font-bold text-xs text-on-surface">{exam.questionCount} câu</div>
                    </div>
                    <div className="p-1.5 rounded bg-surface-container-low">
                      <div className="text-[10px] text-outline">Độ khó</div>
                      <div className="font-bold text-xs text-amber-700">{exam.difficulty}</div>
                    </div>
                  </div>
                </div>

                <button
                  onClick={() => handleStartExam(exam, 'MOCK_EXAM')}
                  disabled={startingId !== null}
                  className="w-full py-2.5 px-4 rounded-xl bg-primary text-on-primary font-label-md text-sm font-bold hover:bg-primary/90 transition-all flex items-center justify-center gap-2 shadow-sm disabled:opacity-50"
                >
                  {startingId === exam.id ? (
                    <>
                      <span className="material-symbols-outlined text-[18px] animate-spin">sync</span>
                      <span>Đang chuẩn bị đề thi...</span>
                    </>
                  ) : (
                    <>
                      <span className="material-symbols-outlined text-[18px]">play_circle</span>
                      <span>Bắt đầu làm bài (CBT)</span>
                    </>
                  )}
                </button>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
