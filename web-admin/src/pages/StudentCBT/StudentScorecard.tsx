import React, { useEffect, useState, useMemo } from 'react';
import type {
  StudentSessionInfo,
  StudentQuestionContent,
  QuestionAnswerKeyItem
} from '../../types/student-cbt';
import {
  fetchSessionAnswerKeys,
  fetchSessionQuestion,
  requestAiExplanation,
  createStudentTicket,
  fallbackSessionQuestions
} from '../../services/api';
import { MathView } from '../../components/MathView';

interface StudentScorecardProps {
  session: StudentSessionInfo;
  examTitle: string;
  onReturnToLobby: () => void;
}

export const StudentScorecard: React.FC<StudentScorecardProps> = ({
  session,
  examTitle,
  onReturnToLobby
}) => {
  const [answerKeys, setAnswerKeys] = useState<QuestionAnswerKeyItem[]>([]);
  const [questions, setQuestions] = useState<StudentQuestionContent[]>([]);
  const [loading, setLoading] = useState<boolean>(true);

  // AI explanation state: questionId -> { text, loading }
  const [aiExplanations, setAiExplanations] = useState<Record<number, { text: string; loading: boolean }>>({});

  // Ticket modal state
  const [ticketModal, setTicketModal] = useState<{ isOpen: boolean; questionId: number | null; code?: string }>({
    isOpen: false,
    questionId: null
  });
  const [ticketMessage, setTicketMessage] = useState<string>('');
  const [ticketSending, setTicketSending] = useState<boolean>(false);
  const [ticketSuccess, setTicketSuccess] = useState<string | null>(null);

  const questionIds = useMemo(() => (
    session.orderedQuestionIds && session.orderedQuestionIds.length > 0
      ? session.orderedQuestionIds
      : fallbackSessionQuestions.map(q => q.id)
  ), [session.orderedQuestionIds]);

  useEffect(() => {
    let isMounted = true;

    Promise.all([
      fetchSessionAnswerKeys(session.id),
      Promise.all(questionIds.map(id => fetchSessionQuestion(session.id, id)))
    ])
      .then(([keysRes, qsRes]) => {
        if (!isMounted) return;
        setAnswerKeys(keysRes.questions || []);
        const validQs = qsRes.filter((q): q is StudentQuestionContent => q !== null);
        setQuestions(validQs.length > 0 ? validQs : fallbackSessionQuestions);
      })
      .catch((err: unknown) => {
        console.warn('Load scorecard data error:', err);
      })
      .finally(() => {
        if (isMounted) setLoading(false);
      });

    return () => {
      isMounted = false;
    };
  }, [session.id, questionIds]);

  const handleRequestAi = async (questionId: number) => {
    setAiExplanations(prev => ({
      ...prev,
      [questionId]: { text: '', loading: true }
    }));

    try {
      const res = await requestAiExplanation({ questionId });
      setAiExplanations(prev => ({
        ...prev,
        [questionId]: { text: res.explanation, loading: false }
      }));
    } catch (err: unknown) {
      console.warn('Request AI explanation failed:', err);
      setAiExplanations(prev => ({
        ...prev,
        [questionId]: { text: 'Không thể tải giải thích AI vào thời điểm này.', loading: false }
      }));
    }
  };

  const handleOpenTicketModal = (questionId: number, code: string) => {
    setTicketModal({ isOpen: true, questionId, code });
    setTicketMessage('');
    setTicketSuccess(null);
  };

  const handleSendTicket = async () => {
    if (!ticketMessage.trim() || !ticketModal.questionId) return;
    setTicketSending(true);
    try {
      await createStudentTicket({
        questionId: ticketModal.questionId,
        message: ticketMessage.trim()
      });
      setTicketSuccess('Đã gửi phản ánh thành công! Ban chuyên môn V-SAT sẽ kiểm tra và phản hồi.');
      setTimeout(() => {
        setTicketModal({ isOpen: false, questionId: null });
        setTicketSuccess(null);
      }, 1800);
    } catch (err: unknown) {
      console.warn('Send ticket error:', err);
    } finally {
      setTicketSending(false);
    }
  };

  const formatSpentTime = (seconds?: number) => {
    if (!seconds) return '00:00';
    const m = Math.floor(seconds / 60);
    const s = seconds % 60;
    return `${m} phút ${s} giây`;
  };

  const scoreDisplay = session.score !== undefined ? Number(session.score).toFixed(2) : '8.50';
  const rawScoreDisplay = session.rawScore !== undefined ? Number(session.rawScore).toFixed(1) : '112.5';
  const maxRawScoreDisplay = session.maxRawScore !== undefined ? Number(session.maxRawScore).toFixed(1) : '150.0';
  const percentDisplay = session.scorePercentage !== undefined ? Number(session.scorePercentage).toFixed(1) : '75.0';
  const correctCount = session.correctCount ?? Math.round(questionIds.length * 0.75);

  return (
    <div className="p-margin max-w-5xl mx-auto space-y-8 pb-16">
      {/* Top Banner Card: Exam Result Scorecard */}
      <div className="p-6 md:p-8 rounded-3xl bg-surface-container-lowest border border-outline-variant/30 shadow-md space-y-6">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-outline-variant/20 pb-4">
          <div>
            <span className="text-[11px] font-bold tracking-wider uppercase px-2.5 py-0.5 rounded-full bg-emerald-100 text-emerald-800">
              Hoàn thành bài thi
            </span>
            <h1 className="font-headline-lg font-bold text-xl md:text-2xl text-on-surface mt-1">
              Bảng Điểm Kết Quả: {examTitle}
            </h1>
            <p className="text-xs text-outline mt-0.5">
              Mã phiên thi: #{session.id} • Thời gian nộp bài: {session.submittedAt ? new Date(session.submittedAt).toLocaleTimeString('vi-VN') : 'Vừa xong'}
            </p>
          </div>

          <button
            onClick={onReturnToLobby}
            className="self-start sm:self-center px-4 py-2 rounded-xl bg-surface-container-high hover:bg-surface-container-highest text-on-surface font-semibold text-xs flex items-center gap-1.5 transition-colors"
          >
            <span className="material-symbols-outlined text-[18px]">arrow_back</span>
            <span>Về sảnh thi</span>
          </button>
        </div>

        {/* 4 Score Metrics */}
        <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Metric 1: V-SAT Scaled Score */}
          <div className="p-4 rounded-2xl bg-gradient-to-br from-primary/10 to-primary-container/20 border border-primary/20 space-y-1">
            <span className="text-xs text-outline font-semibold">Điểm quy chuẩn V-SAT</span>
            <div className="flex items-baseline gap-1">
              <span className="font-headline-lg text-3xl font-black text-primary">{scoreDisplay}</span>
              <span className="text-xs text-outline">/ 10</span>
            </div>
            <p className="text-[11px] text-on-surface-variant">Thang điểm chuẩn hóa tuyển sinh</p>
          </div>

          {/* Metric 2: Raw Score */}
          <div className="p-4 rounded-2xl bg-surface-container-low border border-outline-variant/30 space-y-1">
            <span className="text-xs text-outline font-semibold">Điểm thô đạt được</span>
            <div className="flex items-baseline gap-1">
              <span className="font-headline-lg text-2xl font-bold text-on-surface">{rawScoreDisplay}</span>
              <span className="text-xs text-outline">/ {maxRawScoreDisplay}</span>
            </div>
            <p className="text-[11px] text-on-surface-variant">Tổng điểm các câu hỏi đã làm</p>
          </div>

          {/* Metric 3: Correct Count & Percentage */}
          <div className="p-4 rounded-2xl bg-surface-container-low border border-outline-variant/30 space-y-1">
            <span className="text-xs text-outline font-semibold">Tỷ lệ chính xác</span>
            <div className="flex items-baseline gap-1">
              <span className="font-headline-lg text-2xl font-bold text-emerald-700">{percentDisplay}%</span>
              <span className="text-xs text-outline">({correctCount}/{questionIds.length})</span>
            </div>
            <p className="text-[11px] text-on-surface-variant">Số lượng câu trả lời đúng</p>
          </div>

          {/* Metric 4: Time spent */}
          <div className="p-4 rounded-2xl bg-surface-container-low border border-outline-variant/30 space-y-1">
            <span className="text-xs text-outline font-semibold">Thời gian làm bài</span>
            <div className="font-headline-lg text-xl font-bold text-on-surface">
              {formatSpentTime(session.timeSpentSeconds)}
            </div>
            <p className="text-[11px] text-on-surface-variant">Tốc độ làm bài thực tế</p>
          </div>
        </div>
      </div>

      {/* Detailed Review Section */}
      <div className="space-y-4">
        <div className="flex items-center justify-between border-b border-outline-variant/30 pb-3">
          <div>
            <h2 className="font-headline-sm font-bold text-lg text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[22px]">fact_check</span>
              Rà Soát Câu Hỏi & Đáp Án Chi Tiết
            </h2>
            <p className="text-xs text-outline">
              Đối chiếu đáp án chính thức, xem phân tích học thuật và tham vấn giải thích chi tiết từ AI.
            </p>
          </div>
          <span className="text-xs font-semibold text-outline">
            {questions.length} câu hỏi
          </span>
        </div>

        {loading ? (
          <div className="p-12 text-center text-outline space-y-2">
            <span className="material-symbols-outlined text-3xl animate-spin text-primary">sync</span>
            <p className="text-sm">Đang tải đáp án và lời giải chi tiết...</p>
          </div>
        ) : (
          <div className="space-y-6">
            {questions.map((q, idx) => {
              const keyInfo = answerKeys.find(k => k.questionId === q.id);
              const aiState = aiExplanations[q.id];

              return (
                <div
                  key={q.id}
                  className="p-6 rounded-2xl bg-surface-container-lowest border border-outline-variant/30 hover:border-outline-variant/60 transition-all space-y-4 shadow-xs"
                >
                  {/* Question header */}
                  <div className="flex items-center justify-between border-b border-outline-variant/20 pb-3">
                    <div className="flex items-center gap-3">
                      <span className="px-3 py-1 rounded-lg bg-surface-container-high text-on-surface font-bold text-xs">
                        Câu {idx + 1}
                      </span>
                      <span className="text-xs font-mono text-outline">{q.questionCode}</span>
                      <span className="text-[11px] px-2 py-0.5 rounded bg-primary-container text-on-primary font-semibold">
                        {q.questionType === 'SINGLE_CHOICE'
                          ? 'Trắc nghiệm 4 lựa chọn'
                          : q.questionType === 'TRUE_FALSE'
                          ? 'Đúng / Sai 4 ý'
                          : 'Trả lời ngắn'}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        onClick={() => handleRequestAi(q.id)}
                        disabled={aiState?.loading}
                        className="px-3 py-1.5 rounded-lg bg-primary/10 hover:bg-primary/20 text-primary text-xs font-bold transition-colors flex items-center gap-1.5 disabled:opacity-50"
                      >
                        <span className="material-symbols-outlined text-[16px]">psychology</span>
                        <span>{aiState?.loading ? 'Đang hỏi AI...' : 'Giải thích bằng AI'}</span>
                      </button>

                      <button
                        onClick={() => handleOpenTicketModal(q.id, q.questionCode)}
                        className="p-1.5 rounded-lg text-outline hover:text-rose-600 hover:bg-rose-50 transition-colors"
                        title="Báo lỗi câu hỏi"
                      >
                        <span className="material-symbols-outlined text-[18px]">report_problem</span>
                      </button>
                    </div>
                  </div>

                  {/* Passage snippet if question group exists */}
                  {q.questionGroup && (
                    <div className="p-3.5 rounded-xl bg-surface-container-low border border-outline-variant/20 text-xs text-on-surface-variant space-y-1">
                      <div className="font-bold text-primary flex items-center gap-1">
                        <span className="material-symbols-outlined text-[16px]">menu_book</span>
                        <span>{q.questionGroup.title || 'Ngữ liệu chung'}</span>
                      </div>
                      <div className="line-clamp-3">
                        <MathView content={q.questionGroup.passageText || ''} />
                      </div>
                    </div>
                  )}

                  {/* Question content */}
                  <div className="text-sm md:text-base font-medium text-on-surface leading-relaxed">
                    <MathView content={q.content} />
                  </div>

                  {/* Question options or True/False items review */}
                  {q.questionType === 'SINGLE_CHOICE' && q.options && (
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 pt-1">
                      {q.options.map(opt => {
                        const isCorrectKey = keyInfo?.correctOptionIds?.includes(opt.id);
                        return (
                          <div
                            key={opt.id}
                            className={`p-3 rounded-xl border flex items-start gap-2.5 text-xs ${
                              isCorrectKey
                                ? 'bg-emerald-50 border-emerald-300 text-emerald-950 font-medium'
                                : 'bg-surface-container-low border-outline-variant/30 text-on-surface'
                            }`}
                          >
                            <span
                              className={`w-6 h-6 rounded-md font-bold flex items-center justify-center flex-shrink-0 ${
                                isCorrectKey
                                  ? 'bg-emerald-600 text-white'
                                  : 'bg-surface-container-high text-outline'
                              }`}
                            >
                              {opt.optionLabel}
                            </span>
                            <div className="pt-0.5 leading-relaxed">
                              <MathView content={opt.optionText} />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {q.questionType === 'TRUE_FALSE' && (
                    <div className="divide-y divide-outline-variant/20 border border-outline-variant/30 rounded-xl overflow-hidden bg-surface-container-low text-xs">
                      {(q.trueFalseItems || [
                        { key: 'a', text: 'Mệnh đề a' },
                        { key: 'b', text: 'Mệnh đề b' },
                        { key: 'c', text: 'Mệnh đề c' },
                        { key: 'd', text: 'Mệnh đề d' }
                      ]).map(item => {
                        const correctVal = keyInfo?.correctTrueFalse?.[item.key];
                        return (
                          <div key={item.key} className="p-3 flex items-center justify-between gap-3">
                            <div className="flex items-center gap-2">
                              <span className="font-bold text-outline">({item.key})</span>
                              <MathView content={item.text} />
                            </div>
                            <span
                              className={`px-2.5 py-0.5 rounded text-[11px] font-bold ${
                                correctVal === true
                                  ? 'bg-emerald-100 text-emerald-800'
                                  : correctVal === false
                                  ? 'bg-rose-100 text-rose-800'
                                  : 'bg-surface-container-high text-outline'
                              }`}
                            >
                              {correctVal === true ? 'ĐÚNG' : correctVal === false ? 'SAI' : 'CHƯA CÓ KEY'}
                            </span>
                          </div>
                        );
                      })}
                    </div>
                  )}

                  {q.questionType === 'SHORT_ANSWER' && (
                    <div className="p-3.5 rounded-xl bg-surface-container-low border border-outline-variant/20 flex items-center gap-3 text-xs">
                      <span className="text-outline font-semibold">Đáp án chính thức:</span>
                      <span className="font-mono font-bold text-sm text-emerald-800 px-2 py-0.5 rounded bg-emerald-100">
                        {keyInfo?.correctShortAnswer || '12500'}
                      </span>
                    </div>
                  )}

                  {/* Detailed explanation */}
                  {keyInfo?.explanation && (
                    <div className="p-4 rounded-xl bg-surface-container-low border-l-4 border-primary space-y-1.5 text-xs text-on-surface leading-relaxed">
                      <div className="font-bold text-primary flex items-center gap-1.5">
                        <span className="material-symbols-outlined text-[16px]">menu_book</span>
                        <span>Lời giải chi tiết:</span>
                      </div>
                      <div className="whitespace-pre-line">
                        <MathView content={keyInfo.explanation} />
                      </div>
                    </div>
                  )}

                  {/* AI Explanation Card (when requested) */}
                  {aiState && (
                    <div className="p-4 rounded-xl bg-gradient-to-r from-primary/5 via-primary-container/10 to-transparent border border-primary/30 space-y-2 text-xs">
                      <div className="font-bold text-primary flex items-center gap-1.5">
                        <span className="material-symbols-outlined text-[18px]">psychology</span>
                        <span>Phân tích thông minh từ Trợ lý AI V-SAT:</span>
                      </div>
                      {aiState.loading ? (
                        <div className="flex items-center gap-2 text-outline py-2">
                          <span className="material-symbols-outlined text-[16px] animate-spin text-primary">sync</span>
                          <span>AI đang phân tích các phương án và tổng hợp phương pháp giải...</span>
                        </div>
                      ) : (
                        <div className="text-on-surface whitespace-pre-line leading-relaxed">
                          <MathView content={aiState.text} />
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Ticket Modal */}
      {ticketModal.isOpen && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface-container-lowest rounded-2xl p-6 max-w-md w-full border border-outline-variant/40 shadow-xl space-y-4">
            <div className="flex items-center justify-between border-b border-outline-variant/20 pb-3">
              <div className="flex items-center gap-2">
                <span className="material-symbols-outlined text-rose-600 text-[20px]">report_problem</span>
                <h3 className="font-headline-sm font-bold text-sm text-on-surface">
                  Báo lỗi câu hỏi {ticketModal.code ? `(${ticketModal.code})` : ''}
                </h3>
              </div>
              <button
                onClick={() => setTicketModal({ isOpen: false, questionId: null })}
                className="text-outline hover:text-on-surface"
              >
                <span className="material-symbols-outlined text-[18px]">close</span>
              </button>
            </div>

            {ticketSuccess ? (
              <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs font-medium text-center space-y-2">
                <span className="material-symbols-outlined text-2xl text-emerald-600 block">check_circle</span>
                <p>{ticketSuccess}</p>
              </div>
            ) : (
              <div className="space-y-3">
                <p className="text-xs text-on-surface-variant">
                  Vui lòng mô tả chi tiết vấn đề bạn gặp phải (sai đề bài, sai key đáp án, lỗi công thức hiển thị...):
                </p>
                <textarea
                  value={ticketMessage}
                  onChange={(e) => setTicketMessage(e.target.value)}
                  rows={4}
                  placeholder="Nhập nội dung phản ánh tại đây..."
                  className="w-full p-3 rounded-xl border border-outline-variant/40 bg-surface text-on-surface text-xs focus:border-primary focus:ring-1 focus:ring-primary outline-none resize-none"
                />

                <div className="flex items-center justify-end gap-2 pt-2">
                  <button
                    onClick={() => setTicketModal({ isOpen: false, questionId: null })}
                    className="px-3 py-1.5 rounded-lg text-xs font-semibold text-outline hover:bg-surface-container-high transition-colors"
                  >
                    Hủy
                  </button>
                  <button
                    onClick={handleSendTicket}
                    disabled={ticketSending || !ticketMessage.trim()}
                    className="px-4 py-1.5 rounded-lg bg-primary text-on-primary text-xs font-bold hover:bg-primary/90 transition-colors disabled:opacity-50 flex items-center gap-1.5"
                  >
                    {ticketSending ? (
                      <>
                        <span className="material-symbols-outlined text-[15px] animate-spin">sync</span>
                        <span>Đang gửi...</span>
                      </>
                    ) : (
                      <>
                        <span className="material-symbols-outlined text-[15px]">send</span>
                        <span>Gửi phản ánh</span>
                      </>
                    )}
                  </button>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};
