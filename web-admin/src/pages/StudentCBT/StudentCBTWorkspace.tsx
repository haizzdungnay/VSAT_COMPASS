import React, { useEffect, useState, useRef, useCallback, useMemo } from 'react';
import type {
  StudentSessionInfo,
  StudentQuestionContent,
  SaveSessionAnswerPayload
} from '../../types/student-cbt';
import {
  fetchSessionQuestion,
  saveSessionAnswer,
  submitStudentSession,
  fallbackSessionQuestions
} from '../../services/api';
import { MathView } from '../../components/MathView';

interface StudentCBTWorkspaceProps {
  session: StudentSessionInfo;
  examTitle: string;
  durationMinutes: number;
  onFinishSession: (submittedSession: StudentSessionInfo) => void;
  onExitLobby: () => void;
}

interface LocalAnswerState {
  selectedOptionId?: number;
  answerPayload?: Record<string, unknown>;
  bookmarked: boolean;
  isDirty: boolean;
}

export const StudentCBTWorkspace: React.FC<StudentCBTWorkspaceProps> = ({
  session,
  examTitle,
  durationMinutes,
  onFinishSession,
  onExitLobby
}) => {
  const questionIds = useMemo(() => (
    session.orderedQuestionIds && session.orderedQuestionIds.length > 0
      ? session.orderedQuestionIds
      : fallbackSessionQuestions.map(q => q.id)
  ), [session.orderedQuestionIds]);

  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [questionCache, setQuestionCache] = useState<Record<number, StudentQuestionContent>>({});
  const [answers, setAnswers] = useState<Record<number, LocalAnswerState>>({});
  const [remainingSeconds, setRemainingSeconds] = useState<number>(durationMinutes * 60);
  const [lastSavedTime, setLastSavedTime] = useState<string | null>(null);
  const [isSaving, setIsSaving] = useState<boolean>(false);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [showSubmitModal, setShowSubmitModal] = useState<boolean>(false);

  const workerRef = useRef<Worker | null>(null);
  const answersRef = useRef<Record<number, LocalAnswerState>>({});
  useEffect(() => {
    answersRef.current = answers;
  }, [answers]);

  const currentQId = questionIds[currentIndex];
  const currentQuestion = questionCache[currentQId] || null;
  const currentAnswer = answers[currentQId] || { bookmarked: false, isDirty: false };
  const loadingQuestion = !questionCache[currentQId];

  // 1. Web Worker Timer
  const handleSubmitExamRef = useRef<() => void>(() => {});

  useEffect(() => {
    const workerBlobCode = `
      let timer = null;
      self.onmessage = function(e) {
        if (e.data === 'start') {
          if (timer) clearInterval(timer);
          timer = setInterval(function() {
            self.postMessage('tick');
          }, 1000);
        } else if (e.data === 'stop') {
          if (timer) clearInterval(timer);
          timer = null;
        }
      };
    `;
    const blob = new Blob([workerBlobCode], { type: 'application/javascript' });
    const workerUrl = URL.createObjectURL(blob);
    const worker = new Worker(workerUrl);
    workerRef.current = worker;

    worker.onmessage = (e) => {
      if (e.data === 'tick') {
        setRemainingSeconds(prev => {
          if (prev <= 1) {
            worker.postMessage('stop');
            setTimeout(() => {
              handleSubmitExamRef.current();
            }, 0);
            return 0;
          }
          return prev - 1;
        });
      }
    };

    worker.postMessage('start');

    return () => {
      worker.postMessage('stop');
      worker.terminate();
      URL.revokeObjectURL(workerUrl);
      workerRef.current = null;
    };
  }, []);

  // Format countdown mm:ss or hh:mm:ss
  const formatTime = (secs: number) => {
    const hours = Math.floor(secs / 3600);
    const m = Math.floor((secs % 3600) / 60);
    const s = secs % 60;
    if (hours > 0) {
      return `${String(hours).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
    }
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`;
  };

  // 2. Load Question Content
  useEffect(() => {
    if (!currentQId) return;
    if (questionCache[currentQId]) return;

    let isMounted = true;
    fetchSessionQuestion(session.id, currentQId)
      .then(q => {
        if (isMounted && q) {
          setQuestionCache(prev => ({ ...prev, [currentQId]: q }));
        }
      })
      .catch((err: unknown) => {
        console.warn('Load question failed:', err);
      });

    return () => {
      isMounted = false;
    };
  }, [currentQId, session.id, questionCache]);

  // 3. Save Question Answer Function
  const persistAnswer = useCallback(async (qId: number, state?: LocalAnswerState) => {
    const targetState = state || answersRef.current[qId];
    if (!targetState) return;

    const payload: SaveSessionAnswerPayload = {
      questionId: qId,
      selectedOptionId: targetState.selectedOptionId,
      answerPayload: targetState.answerPayload,
      bookmarked: targetState.bookmarked,
      timeSpentSeconds: 15
    };

    setIsSaving(true);
    try {
      await saveSessionAnswer(session.id, payload);
      const now = new Date();
      const timeStr = `${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(2, '0')}:${String(now.getSeconds()).padStart(2, '0')}`;
      setLastSavedTime(timeStr);
      setAnswers(prev => {
        if (!prev[qId]) return prev;
        return {
          ...prev,
          [qId]: { ...prev[qId], isDirty: false }
        };
      });
    } catch (err: unknown) {
      console.warn('Persist answer error:', err);
    } finally {
      setIsSaving(false);
    }
  }, [session.id]);

  // 4. Auto-save every 15 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      if (currentQId && answersRef.current[currentQId]?.isDirty) {
        persistAnswer(currentQId, answersRef.current[currentQId]);
      }
    }, 15000);

    return () => clearInterval(interval);
  }, [currentQId, persistAnswer]);

  // Auto-submit on time expiration (00:00)
  const handleSubmitExam = useCallback(async () => {
    if (isSubmitting) return;
    setIsSubmitting(true);
    try {
      // Save current if dirty
      if (currentQId && answersRef.current[currentQId]?.isDirty) {
        await persistAnswer(currentQId, answersRef.current[currentQId]);
      }
      const submitted = await submitStudentSession(session.id);
      onFinishSession(submitted);
    } catch (err: unknown) {
      console.warn('Submit exam error:', err);
    } finally {
      setIsSubmitting(false);
    }
  }, [isSubmitting, currentQId, persistAnswer, session.id, onFinishSession]);

  // 5. Handlers for Question Types
  const handleSelectOption = (optId: number) => {
    const updated: LocalAnswerState = {
      ...currentAnswer,
      selectedOptionId: optId,
      isDirty: true
    };
    setAnswers(prev => ({ ...prev, [currentQId]: updated }));
    persistAnswer(currentQId, updated);
  };

  const handleToggleTrueFalse = (itemKey: string, val: boolean) => {
    const existingPayload = (currentAnswer.answerPayload as Record<string, boolean>) || {};
    const updatedPayload = {
      ...existingPayload,
      [itemKey]: val
    };
    const updated: LocalAnswerState = {
      ...currentAnswer,
      answerPayload: updatedPayload,
      isDirty: true
    };
    setAnswers(prev => ({ ...prev, [currentQId]: updated }));
  };

  const handleShortAnswerChange = (text: string) => {
    const updated: LocalAnswerState = {
      ...currentAnswer,
      answerPayload: { value: text },
      isDirty: true
    };
    setAnswers(prev => ({ ...prev, [currentQId]: updated }));
  };

  const handleToggleBookmark = () => {
    const updated: LocalAnswerState = {
      ...currentAnswer,
      bookmarked: !currentAnswer.bookmarked,
      isDirty: true
    };
    setAnswers(prev => ({ ...prev, [currentQId]: updated }));
    persistAnswer(currentQId, updated);
  };

  // Switch questions
  const goToQuestion = (index: number) => {
    if (index < 0 || index >= questionIds.length) return;
    if (currentQId && answersRef.current[currentQId]?.isDirty) {
      persistAnswer(currentQId, answersRef.current[currentQId]);
    }
    setCurrentIndex(index);
  };

  // 6. Keyboard Shortcuts using stable refs
  const currentIndexRef = useRef(currentIndex);
  const currentQuestionRef = useRef(currentQuestion);
  const goToQuestionRef = useRef(goToQuestion);
  const handleToggleBookmarkRef = useRef(handleToggleBookmark);
  const handleSelectOptionRef = useRef(handleSelectOption);

  useEffect(() => {
    handleSubmitExamRef.current = handleSubmitExam;
    currentIndexRef.current = currentIndex;
    currentQuestionRef.current = currentQuestion;
    goToQuestionRef.current = goToQuestion;
    handleToggleBookmarkRef.current = handleToggleBookmark;
    handleSelectOptionRef.current = handleSelectOption;
  });

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const activeEl = document.activeElement;
      const isInputFocused = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA');

      if (e.key === 'ArrowLeft') {
        e.preventDefault();
        goToQuestionRef.current(currentIndexRef.current - 1);
        return;
      }

      if (e.key === 'ArrowRight') {
        e.preventDefault();
        goToQuestionRef.current(currentIndexRef.current + 1);
        return;
      }

      if (!isInputFocused) {
        if (e.key === 'f' || e.key === 'F') {
          e.preventDefault();
          handleToggleBookmarkRef.current();
          return;
        }

        const q = currentQuestionRef.current;
        if (q && q.questionType === 'SINGLE_CHOICE' && q.options) {
          if (e.key === '1' && q.options[0]) {
            e.preventDefault();
            handleSelectOptionRef.current(q.options[0].id);
          } else if (e.key === '2' && q.options[1]) {
            e.preventDefault();
            handleSelectOptionRef.current(q.options[1].id);
          } else if (e.key === '3' && q.options[2]) {
            e.preventDefault();
            handleSelectOptionRef.current(q.options[2].id);
          } else if (e.key === '4' && q.options[3]) {
            e.preventDefault();
            handleSelectOptionRef.current(q.options[3].id);
          }
        }
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, []);

  // Check whether question is answered
  const isQuestionAnswered = (qId: number) => {
    const ans = answers[qId];
    if (!ans) return false;
    if (ans.selectedOptionId !== undefined) return true;
    if (ans.answerPayload) {
      if (typeof ans.answerPayload.value === 'string') {
        return ans.answerPayload.value.trim().length > 0;
      }
      return Object.keys(ans.answerPayload).length > 0;
    }
    return false;
  };

  const answeredCount = questionIds.filter(id => isQuestionAnswered(id)).length;
  const bookmarkedCount = questionIds.filter(id => answers[id]?.bookmarked).length;

  return (
    <div className="flex flex-col h-[calc(100vh-64px)] overflow-hidden bg-background">
      {/* Top Exam Header Bar */}
      <div className="h-14 px-6 bg-surface-container-lowest border-b border-outline-variant/30 flex items-center justify-between flex-shrink-0 z-20">
        <div className="flex items-center gap-3 min-w-0">
          <button
            onClick={onExitLobby}
            className="p-1.5 rounded-lg text-outline hover:text-on-surface hover:bg-surface-container-high transition-colors"
            title="Thoát về sảnh thi"
          >
            <span className="material-symbols-outlined text-[20px]">arrow_back</span>
          </button>
          <div className="flex flex-col min-w-0">
            <h1 className="font-headline-sm text-sm font-bold text-on-surface truncate">
              {examTitle}
            </h1>
            <div className="flex items-center gap-2 text-[11px] text-outline">
              <span>Phiên #{session.id}</span>
              <span>•</span>
              <span className="font-medium text-emerald-700">Chế độ thi CBT</span>
            </div>
          </div>
        </div>

        {/* Center: Timer & Auto-save Status */}
        <div className="flex items-center gap-4">
          <div className={`flex items-center gap-2 px-3 py-1 rounded-xl border font-mono text-sm font-bold shadow-xs ${
            remainingSeconds <= 300
              ? 'bg-rose-50 text-rose-700 border-rose-300 animate-pulse'
              : 'bg-surface-container-high text-primary border-outline-variant/40'
          }`}>
            <span className="material-symbols-outlined text-[18px]">alarm</span>
            <span>{formatTime(remainingSeconds)}</span>
          </div>

          <div className="hidden md:flex items-center gap-1.5 text-xs text-on-surface-variant font-medium">
            {isSaving ? (
              <>
                <span className="material-symbols-outlined text-[15px] animate-spin text-primary">sync</span>
                <span>Đang lưu...</span>
              </>
            ) : lastSavedTime ? (
              <>
                <span className="material-symbols-outlined text-[15px] text-emerald-600">cloud_done</span>
                <span>Đã lưu lúc {lastSavedTime}</span>
              </>
            ) : (
              <span>Tự động lưu sau 15s</span>
            )}
          </div>
        </div>

        {/* Right: Submit Button */}
        <div className="flex items-center gap-3">
          <button
            onClick={handleToggleBookmark}
            className={`flex items-center gap-1 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors ${
              currentAnswer.bookmarked
                ? 'bg-amber-100 text-amber-900 border-amber-300'
                : 'bg-surface text-outline border-outline-variant/40 hover:bg-surface-container-high'
            }`}
          >
            <span className="material-symbols-outlined text-[16px]">
              {currentAnswer.bookmarked ? 'bookmark' : 'bookmark_border'}
            </span>
            <span className="hidden sm:inline">Đánh dấu (F)</span>
          </button>

          <button
            onClick={() => setShowSubmitModal(true)}
            className="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-label-md text-xs font-bold transition-all shadow-sm flex items-center gap-1.5"
          >
            <span className="material-symbols-outlined text-[16px]">send</span>
            <span>Nộp bài</span>
          </button>
        </div>
      </div>

      {/* Main Workspace Layout: Question Area + Palette Sidebar */}
      <div className="flex-1 flex overflow-hidden">
        {/* Left/Center Workspace */}
        <div className="flex-1 overflow-y-auto p-6 flex flex-col justify-between">
          {loadingQuestion || !currentQuestion ? (
            <div className="flex-1 flex flex-col items-center justify-center text-outline space-y-2">
              <span className="material-symbols-outlined text-4xl animate-spin text-primary">sync</span>
              <p className="text-sm">Đang tải nội dung câu hỏi #{currentIndex + 1}...</p>
            </div>
          ) : (
            <div className="space-y-6 max-w-5xl mx-auto w-full">
              {/* Question Header & Shortcuts guide */}
              <div className="flex items-center justify-between pb-3 border-b border-outline-variant/30">
                <div className="flex items-center gap-3">
                  <span className="px-3 py-1 rounded-lg bg-primary text-on-primary font-bold text-sm shadow-xs">
                    Câu {currentIndex + 1} / {questionIds.length}
                  </span>
                  <span className="text-xs font-mono px-2 py-0.5 rounded bg-surface-container-high text-outline">
                    {currentQuestion.questionCode}
                  </span>
                  <span className="text-xs font-semibold px-2 py-0.5 rounded bg-primary-container text-on-primary">
                    {currentQuestion.questionType === 'SINGLE_CHOICE'
                      ? 'Trắc nghiệm 4 lựa chọn'
                      : currentQuestion.questionType === 'TRUE_FALSE'
                      ? 'Đúng / Sai 4 ý'
                      : 'Trả lời ngắn'}
                  </span>
                </div>

                <div className="hidden lg:flex items-center gap-2 text-[11px] text-outline">
                  <span>Phím tắt:</span>
                  <kbd className="px-1.5 py-0.5 rounded bg-surface-container-high border text-on-surface">←</kbd>
                  <kbd className="px-1.5 py-0.5 rounded bg-surface-container-high border text-on-surface">→</kbd>
                  <span>Chuyển câu</span>
                  <kbd className="px-1.5 py-0.5 rounded bg-surface-container-high border text-on-surface">1..4</kbd>
                  <span>Chọn A-D</span>
                  <kbd className="px-1.5 py-0.5 rounded bg-surface-container-high border text-on-surface">F</kbd>
                  <span>Cờ</span>
                </div>
              </div>

              {/* Split-pane Reading Layout if Question Group is present */}
              {currentQuestion.questionGroup ? (
                <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
                  {/* Left Column: Passage / Group Content */}
                  <div className="lg:col-span-6 p-5 rounded-2xl bg-surface-container-lowest border border-outline-variant/40 space-y-3 max-h-[62vh] overflow-y-auto">
                    <div className="flex items-center gap-2 text-primary font-bold text-xs uppercase tracking-wide">
                      <span className="material-symbols-outlined text-[18px]">menu_book</span>
                      <span>{currentQuestion.questionGroup.title || 'Ngữ liệu chung'}</span>
                    </div>
                    <div className="text-sm leading-relaxed text-on-surface">
                      <MathView content={currentQuestion.questionGroup.passageText || ''} />
                    </div>
                  </div>

                  {/* Right Column: Question Content & Answering Area */}
                  <div className="lg:col-span-6 p-5 rounded-2xl bg-surface-container-lowest border border-outline-variant/40 space-y-5">
                    <div className="text-base font-medium text-on-surface leading-relaxed">
                      <MathView content={currentQuestion.content} />
                    </div>

                    {renderAnswerWidget()}
                  </div>
                </div>
              ) : (
                /* Standard Single Column Layout */
                <div className="p-6 rounded-2xl bg-surface-container-lowest border border-outline-variant/40 space-y-6">
                  <div className="text-base md:text-lg font-medium text-on-surface leading-relaxed">
                    <MathView content={currentQuestion.content} />
                  </div>

                  {renderAnswerWidget()}
                </div>
              )}
            </div>
          )}

          {/* Bottom Navigation Buttons */}
          <div className="max-w-5xl mx-auto w-full pt-4 mt-6 border-t border-outline-variant/30 flex items-center justify-between">
            <button
              onClick={() => goToQuestion(currentIndex - 1)}
              disabled={currentIndex === 0}
              className="px-4 py-2 rounded-xl border border-outline-variant/40 font-semibold text-xs text-on-surface hover:bg-surface-container-high transition-colors disabled:opacity-40 flex items-center gap-1.5"
            >
              <span className="material-symbols-outlined text-[16px]">arrow_back</span>
              <span>Câu trước</span>
            </button>

            <div className="text-xs text-outline font-medium">
              Câu {currentIndex + 1} của {questionIds.length}
            </div>

            <button
              onClick={() => goToQuestion(currentIndex + 1)}
              disabled={currentIndex === questionIds.length - 1}
              className="px-4 py-2 rounded-xl bg-primary text-on-primary font-semibold text-xs hover:bg-primary/90 transition-colors disabled:opacity-40 flex items-center gap-1.5"
            >
              <span>Câu tiếp theo</span>
              <span className="material-symbols-outlined text-[16px]">arrow_forward</span>
            </button>
          </div>
        </div>

        {/* Right Sidebar: Question Palette */}
        <aside className="w-72 bg-surface-container-lowest border-l border-outline-variant/30 p-4 flex flex-col justify-between flex-shrink-0">
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="font-headline-sm font-bold text-sm text-on-surface">
                Bảng câu hỏi ({questionIds.length})
              </h3>
              <span className="text-xs font-semibold text-primary">
                Đã làm: {answeredCount}/{questionIds.length}
              </span>
            </div>

            {/* Status Legend */}
            <div className="grid grid-cols-2 gap-2 text-[11px] text-on-surface-variant pt-1 border-t border-outline-variant/20">
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded bg-primary"></span>
                <span>Đã trả lời</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded bg-surface-container-high border"></span>
                <span>Chưa làm</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded border-2 border-primary bg-primary/20"></span>
                <span>Đang xem</span>
              </div>
              <div className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded bg-amber-400"></span>
                <span>Đánh dấu cờ</span>
              </div>
            </div>

            {/* Questions Grid */}
            <div className="grid grid-cols-5 gap-2 max-h-[50vh] overflow-y-auto p-1">
              {questionIds.map((qId, idx) => {
                const isCurrent = idx === currentIndex;
                const answered = isQuestionAnswered(qId);
                const bookmarked = answers[qId]?.bookmarked;

                return (
                  <button
                    key={qId}
                    onClick={() => goToQuestion(idx)}
                    className={`relative h-10 rounded-xl font-bold text-xs transition-all flex items-center justify-center ${
                      isCurrent
                        ? 'ring-2 ring-primary ring-offset-2 border-primary font-black bg-primary/15 text-primary'
                        : answered
                        ? 'bg-primary text-on-primary shadow-xs'
                        : 'bg-surface-container-high text-on-surface-variant hover:bg-surface-container-highest'
                    }`}
                  >
                    <span>{idx + 1}</span>
                    {bookmarked && (
                      <span className="absolute -top-1 -right-1 w-2.5 h-2.5 rounded-full bg-amber-500 ring-2 ring-white"></span>
                    )}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Bottom Palette Actions */}
          <div className="space-y-3 pt-4 border-t border-outline-variant/30">
            <div className="text-xs text-on-surface-variant space-y-1">
              <div className="flex justify-between">
                <span>Số câu hoàn thành:</span>
                <span className="font-bold text-emerald-700">{answeredCount}</span>
              </div>
              <div className="flex justify-between">
                <span>Số câu đánh dấu:</span>
                <span className="font-bold text-amber-700">{bookmarkedCount}</span>
              </div>
              <div className="flex justify-between">
                <span>Số câu còn lại:</span>
                <span className="font-bold text-outline">{questionIds.length - answeredCount}</span>
              </div>
            </div>

            <button
              onClick={() => setShowSubmitModal(true)}
              disabled={isSubmitting}
              className="w-full py-2.5 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-label-md text-sm font-bold transition-all shadow-sm flex items-center justify-center gap-1.5 disabled:opacity-50"
            >
              <span className="material-symbols-outlined text-[18px]">verified</span>
              <span>Nộp bài thi</span>
            </button>
          </div>
        </aside>
      </div>

      {/* Confirmation Submit Modal */}
      {showSubmitModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface-container-lowest rounded-2xl p-6 max-w-md w-full border border-outline-variant/40 shadow-xl space-y-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 rounded-xl bg-amber-100 text-amber-800 flex items-center justify-center">
                <span className="material-symbols-outlined text-[24px]">help</span>
              </div>
              <div>
                <h3 className="font-headline-sm font-bold text-base text-on-surface">
                  Xác nhận nộp bài thi
                </h3>
                <p className="text-xs text-on-surface-variant">
                  Bạn có chắc chắn muốn kết thúc bài làm và gửi kết quả chấm điểm?
                </p>
              </div>
            </div>

            <div className="p-3.5 rounded-xl bg-surface-container-low space-y-1.5 text-xs">
              <div className="flex justify-between">
                <span className="text-outline">Tổng số câu hỏi:</span>
                <span className="font-bold text-on-surface">{questionIds.length} câu</span>
              </div>
              <div className="flex justify-between">
                <span className="text-outline">Đã trả lời:</span>
                <span className="font-bold text-emerald-700">{answeredCount} câu</span>
              </div>
              <div className="flex justify-between">
                <span className="text-outline">Chưa trả lời:</span>
                <span className="font-bold text-rose-600">{questionIds.length - answeredCount} câu</span>
              </div>
              <div className="flex justify-between">
                <span className="text-outline">Thời gian còn lại:</span>
                <span className="font-bold text-primary">{formatTime(remainingSeconds)}</span>
              </div>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                onClick={() => setShowSubmitModal(false)}
                disabled={isSubmitting}
                className="px-4 py-2 rounded-xl text-xs font-semibold text-outline hover:bg-surface-container-high transition-colors"
              >
                Tiếp tục làm bài
              </button>
              <button
                onClick={handleSubmitExam}
                disabled={isSubmitting}
                className="px-5 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white text-xs font-bold transition-all shadow-sm flex items-center gap-1.5 disabled:opacity-50"
              >
                {isSubmitting ? (
                  <>
                    <span className="material-symbols-outlined text-[16px] animate-spin">sync</span>
                    <span>Đang nộp...</span>
                  </>
                ) : (
                  <>
                    <span className="material-symbols-outlined text-[16px]">check_circle</span>
                    <span>Xác nhận nộp bài</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );

  // Sub-renderer for answer options
  function renderAnswerWidget() {
    if (!currentQuestion) return null;

    // 1. SINGLE_CHOICE / MCQ
    if (currentQuestion.questionType === 'SINGLE_CHOICE') {
      const options = currentQuestion.options || [];
      return (
        <div className="space-y-3 pt-2">
          {options.map((opt, optIndex) => {
            const isSelected = currentAnswer.selectedOptionId === opt.id;
            const shortcutKey = optIndex + 1;

            return (
              <button
                key={opt.id}
                onClick={() => handleSelectOption(opt.id)}
                className={`w-full p-4 rounded-xl border text-left transition-all flex items-start gap-3.5 ${
                  isSelected
                    ? 'border-primary bg-primary/10 ring-1 ring-primary text-on-surface shadow-xs'
                    : 'border-outline-variant/40 bg-surface-container-lowest hover:bg-surface-container-high text-on-surface'
                }`}
              >
                <div
                  className={`w-7 h-7 rounded-lg flex items-center justify-center font-bold text-xs flex-shrink-0 transition-colors ${
                    isSelected
                      ? 'bg-primary text-on-primary'
                      : 'bg-surface-container-high text-outline'
                  }`}
                >
                  {opt.optionLabel || String.fromCharCode(65 + optIndex)}
                </div>
                <div className="flex-1 text-sm md:text-base leading-relaxed pt-0.5">
                  <MathView content={opt.optionText} />
                </div>
                <span className="hidden sm:inline-block text-[10px] font-mono text-outline px-1.5 py-0.5 rounded bg-surface-container-low border border-outline-variant/30">
                  Phím {shortcutKey}
                </span>
              </button>
            );
          })}
        </div>
      );
    }

    // 2. TRUE_FALSE (4 items a, b, c, d)
    if (currentQuestion.questionType === 'TRUE_FALSE') {
      const items = currentQuestion.trueFalseItems || [
        { key: 'a', text: 'Mệnh đề a) của bài toán' },
        { key: 'b', text: 'Mệnh đề b) của bài toán' },
        { key: 'c', text: 'Mệnh đề c) của bài toán' },
        { key: 'd', text: 'Mệnh đề d) của bài toán' }
      ];

      const tfPayload = (currentAnswer.answerPayload as Record<string, boolean>) || {};

      return (
        <div className="space-y-3 pt-2">
          <div className="text-xs text-outline italic">
            Quy chuẩn chấm lũy tiến: Đúng 1 ý (0.1đ), 2 ý (0.25đ), 3 ý (0.5đ), 4 ý (1.0đ)
          </div>

          <div className="divide-y divide-outline-variant/30 border border-outline-variant/40 rounded-xl overflow-hidden bg-surface-container-lowest">
            {items.map(item => {
              const currentVal = tfPayload[item.key];

              return (
                <div key={item.key} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
                  <div className="flex items-start gap-3 flex-1 min-w-0">
                    <span className="w-6 h-6 rounded-full bg-surface-container-high text-outline text-xs font-bold flex items-center justify-center flex-shrink-0 mt-0.5">
                      {item.key}
                    </span>
                    <div className="text-sm text-on-surface leading-relaxed">
                      <MathView content={item.text} />
                    </div>
                  </div>

                  <div className="flex items-center gap-2 self-end sm:self-center flex-shrink-0">
                    <button
                      onClick={() => handleToggleTrueFalse(item.key, true)}
                      className={`px-4 py-1.5 rounded-lg text-xs font-bold border transition-all ${
                        currentVal === true
                          ? 'bg-emerald-600 text-white border-emerald-600 shadow-xs'
                          : 'bg-surface-container-low text-outline border-outline-variant/40 hover:bg-emerald-50 hover:text-emerald-700'
                      }`}
                    >
                      Đúng
                    </button>
                    <button
                      onClick={() => handleToggleTrueFalse(item.key, false)}
                      className={`px-4 py-1.5 rounded-lg text-xs font-bold border transition-all ${
                        currentVal === false
                          ? 'bg-rose-600 text-white border-rose-600 shadow-xs'
                          : 'bg-surface-container-low text-outline border-outline-variant/40 hover:bg-rose-50 hover:text-rose-700'
                      }`}
                    >
                      Sai
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      );
    }

    // 3. SHORT_ANSWER
    const shortVal = (currentAnswer.answerPayload?.value as string) || '';

    return (
      <div className="space-y-3 pt-2">
        <label className="block text-xs font-bold text-outline uppercase tracking-wide">
          Nhập câu trả lời ngắn (Đáp số)
        </label>
        <div className="relative max-w-md">
          <input
            type="text"
            value={shortVal}
            onChange={(e) => handleShortAnswerChange(e.target.value)}
            placeholder="Ví dụ: 12 hoặc 3.5 hoặc -4..."
            className="w-full px-4 py-3 rounded-xl border border-outline-variant/50 bg-surface-container-lowest text-on-surface font-mono text-base focus:border-primary focus:ring-2 focus:ring-primary/20 outline-none transition-all shadow-xs"
          />
        </div>
        <p className="text-xs text-on-surface-variant">
          Ghi chú: Thí sinh điền số nguyên hoặc số thập phân theo yêu cầu câu hỏi. Nhấn phím bất kỳ ra ngoài để tự động lưu.
        </p>
      </div>
    );
  }
};
