import React, { useEffect, useState } from 'react';
import type { PublicExamSummary, PublicExamWindow, StudentSessionInfo, StudentExamMode } from '../../types/student-cbt';
import { fetchPublicExams, fetchPublicExamWindows, startStudentSession } from '../../services/api';
import { AdmissionPredictorWidget } from './AdmissionPredictorWidget';

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
  const [isProctoringEnabled, setIsProctoringEnabled] = useState<boolean>(false);

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
      const effectiveMode: StudentExamMode = (mode === 'MOCK_EXAM' && isProctoringEnabled) ? 'SCHEDULED_MOCK' : mode;
      const session = await startStudentSession({
        examId: exam.id,
        mode: effectiveMode,
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
            Môi trường thi chuẩn hóa theo quy định tuyển sinh đại học của Bộ Giáo dục & Đào tạo. Đồng hồ Web Worker độc lập, tự động lưu câu trả lời mỗi 15 giây và chấm điểm phân hóa 4 phần chuẩn V-SAT 2026.
          </p>
        </div>
      </div>

      {/* V-SAT Standard Structure Overview (4 Parts - 25 Questions - 150 Raw Score) */}
      <div className="space-y-3">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
          <div className="text-xs font-bold uppercase tracking-wider text-primary flex items-center gap-1.5">
            <span className="material-symbols-outlined text-[18px]">verified</span>
            <span>Cấu trúc đề thi chuẩn V-SAT 2026 (4 phần — 25 câu — 150 điểm thô)</span>
          </div>
          <span className="text-xs text-outline font-medium">
            Thời gian làm bài chuẩn: Toán 90 phút; Vật lí, Hóa học, Sinh học, Lịch sử, Địa lí, Tiếng Anh 60 phút
          </span>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Phần I: Đúng / Sai */}
          <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 space-y-2 shadow-sm flex flex-col justify-between">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-primary font-bold text-sm">
                <span className="material-symbols-outlined text-[20px]">check_circle</span>
                <span>Phần I: Đúng / Sai</span>
              </div>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                9 câu (36 ý a, b, c, d). Đánh giá tính đúng/sai từng mệnh đề độc lập. Điểm lũy tiến: đúng 1 ý (1đ), 2 ý (2đ), 3 ý (3đ), 4 ý (6đ).
              </p>
            </div>
            <div className="text-[11px] font-semibold text-primary-fixed-dim pt-2 border-t border-outline-variant/20">
              9 câu • 36 ý • Tối đa 54 điểm
            </div>
          </div>

          {/* Phần II: Trắc nghiệm 4 lựa chọn */}
          <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 space-y-2 shadow-sm flex flex-col justify-between">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-primary font-bold text-sm">
                <span className="material-symbols-outlined text-[20px]">format_list_bulleted</span>
                <span>Phần II: Trắc nghiệm 4 lựa chọn</span>
              </div>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                6 câu hỏi trắc nghiệm (A, B, C, D). Chọn duy nhất 1 phương án chính xác. Mỗi câu trả lời đúng nhận trọn vẹn 6 điểm.
              </p>
            </div>
            <div className="text-[11px] font-semibold text-primary-fixed-dim pt-2 border-t border-outline-variant/20">
              6 câu • 6 điểm/câu • Tối đa 36 điểm
            </div>
          </div>

          {/* Phần III: Ghép hợp Cột A - B */}
          <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 space-y-2 shadow-sm flex flex-col justify-between">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-primary font-bold text-sm">
                <span className="material-symbols-outlined text-[20px]">sync_alt</span>
                <span>Phần III: Ghép hợp (Cột A - B)</span>
              </div>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                5 câu ghép nối. Mỗi câu gồm 4 mệnh đề ở Cột A ghép với 6 lựa chọn A–F ở Cột B (có 2 phương án nhiễu). Nhận 1.5 điểm/cặp đúng.
              </p>
            </div>
            <div className="text-[11px] font-semibold text-primary-fixed-dim pt-2 border-t border-outline-variant/20">
              5 câu • Tối đa 6đ/câu • Tối đa 30 điểm
            </div>
          </div>

          {/* Phần IV: Trả lời ngắn */}
          <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 space-y-2 shadow-sm flex flex-col justify-between">
            <div className="space-y-2">
              <div className="flex items-center gap-2 text-primary font-bold text-sm">
                <span className="material-symbols-outlined text-[20px]">edit_note</span>
                <span>Phần IV: Trả lời ngắn / Điền đáp án</span>
              </div>
              <p className="text-xs text-on-surface-variant leading-relaxed">
                5 câu điền kết quả. Thí sinh tự tính toán và nhập đáp số dưới dạng số nguyên hoặc số thập phân. Nhận 6 điểm cho mỗi câu chính xác.
              </p>
            </div>
            <div className="text-[11px] font-semibold text-primary-fixed-dim pt-2 border-t border-outline-variant/20">
              5 câu • 6 điểm/câu • Tối đa 30 điểm
            </div>
          </div>
        </div>
      </div>

      {/* P4: Proctoring Mode Setting Bar */}
      <div className={`p-4 rounded-xl border transition-all flex flex-col sm:flex-row sm:items-center justify-between gap-3 ${
        isProctoringEnabled
          ? 'bg-rose-50/80 border-rose-300 text-rose-950 shadow-xs'
          : 'bg-surface-container-lowest border-outline-variant/30 text-on-surface'
      }`}>
        <div className="flex items-center gap-3">
          <div className={`w-10 h-10 rounded-xl flex items-center justify-center flex-shrink-0 ${
            isProctoringEnabled ? 'bg-rose-600 text-white' : 'bg-surface-container-high text-outline'
          }`}>
            <span className="material-symbols-outlined text-[22px]">
              {isProctoringEnabled ? 'security' : 'shield'}
            </span>
          </div>
          <div className="space-y-0.5">
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm">Chế độ Giám sát Thi Chống Gian lận (Proctoring Mode)</span>
              {isProctoringEnabled && (
                <span className="text-[10px] font-bold px-2 py-0.5 rounded-full bg-rose-200 text-rose-800">
                  ĐANG BẬT
                </span>
              )}
            </div>
            <p className="text-xs text-on-surface-variant">
              Bắt buộc toàn màn hình, ghi nhận vi phạm khi chuyển tab hoặc mất tiêu điểm, tự động nộp bài khi vi phạm 3 lần (quy chuẩn SCHEDULED_MOCK).
            </p>
          </div>
        </div>

        <button
          type="button"
          onClick={() => setIsProctoringEnabled(prev => !prev)}
          className={`px-4 py-2 rounded-xl text-xs font-bold transition-all flex items-center gap-2 flex-shrink-0 ${
            isProctoringEnabled
              ? 'bg-rose-600 text-white hover:bg-rose-700 shadow-sm'
              : 'bg-surface-container-high text-on-surface hover:bg-surface-container-highest border border-outline-variant/40'
          }`}
        >
          <span className="material-symbols-outlined text-[18px]">
            {isProctoringEnabled ? 'check_circle' : 'toggle_off'}
          </span>
          <span>{isProctoringEnabled ? 'Giám sát: BẬT' : 'Bật chế độ Giám sát'}</span>
        </button>
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
                    {exam.description || 'Đề thi khảo sát năng lực V-SAT bám sát cấu trúc 4 phần với câu hỏi thực tế và bài toán liên môn.'}
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

      {/* P2: Admission Predictor Section */}
      <AdmissionPredictorWidget
        initialScore={320}
        title="Dự đoán Trúng tuyển Đại học theo Điểm V-SAT (Admission Engine)"
        subtitle="Học sinh có thể thử nhập điểm V-SAT ước tính để kiểm tra cơ hội trúng tuyển vào các trường ĐH Ngân hàng (HUB), Kinh tế - Luật (UEL), Cần Thơ (CTU), Tài chính - Marketing (UFM), KHTN (HCMUS), Sài Gòn (SGU)."
      />
    </div>
  );
};
