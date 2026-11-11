import React, { useState, useEffect } from 'react';
import type { FullQuestionItem, QuestionType, CognitiveLevel, DifficultyLevel } from '../../types/exam';
import { createBankQuestion, updateBankQuestion, SUBJECTS, getSubjectNameById } from '../../services/api';
import { MathView } from '../../components/MathView';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (savedQuestion: FullQuestionItem) => void;
  initialData?: FullQuestionItem | null;
}

const DEFAULT_TF_ITEMS = [
  { key: 'a', text: '', isCorrect: true },
  { key: 'b', text: '', isCorrect: false },
  { key: 'c', text: '', isCorrect: true },
  { key: 'd', text: '', isCorrect: false }
];

const DEFAULT_MCQ_OPTIONS = [
  { label: 'A', text: '', isCorrect: true },
  { label: 'B', text: '', isCorrect: false },
  { label: 'C', text: '', isCorrect: false },
  { label: 'D', text: '', isCorrect: false }
];

const DEFAULT_MATCHING_PAIRS = [
  { left: '1. ', right: 'A. ' },
  { left: '2. ', right: 'B. ' },
  { left: '3. ', right: 'C. ' },
  { left: '4. ', right: 'D. ' }
];

export const QuestionCreateModal: React.FC<ModalProps> = ({ isOpen, onClose, onSuccess, initialData }) => {
  const isEditing = Boolean(initialData && initialData.id > 0);

  const [subjectId, setSubjectId] = useState<number>(initialData?.subjectId || 1);
  const [questionType, setQuestionType] = useState<QuestionType>(initialData?.questionType || 'TRUE_FALSE');
  const [cognitiveLevel, setCognitiveLevel] = useState<CognitiveLevel>(initialData?.cognitiveLevel || 'UNDERSTANDING');
  const [difficulty, setDifficulty] = useState<DifficultyLevel>(initialData?.difficulty || 'MEDIUM');
  const [questionText, setQuestionText] = useState(initialData?.questionText || '');
  const [explanation, setExplanation] = useState(initialData?.explanation || '');
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [conflictError, setConflictError] = useState<string | null>(null);

  // Nhóm ngữ liệu / bài nghe Audio (Question Group)
  const initialAudio = (initialData as any)?.audioUrl || initialData?.questionGroup?.audioUrl || '';
  const initialMaxPlays = (initialData as any)?.maxPlays || initialData?.questionGroup?.maxPlays || 2;
  const [hasGroup, setHasGroup] = useState(Boolean(initialData?.questionGroup || initialAudio));
  const [groupTitle, setGroupTitle] = useState(initialData?.questionGroup?.title || '');
  const [passageText, setPassageText] = useState(initialData?.questionGroup?.passageText || '');
  const [audioUrl, setAudioUrl] = useState(initialAudio);
  const [maxPlays, setMaxPlays] = useState<number>(initialMaxPlays);

  // Dạng 1: Đúng/Sai (4 ý a, b, c, d)
  const [tfItems, setTfItems] = useState(
    initialData?.trueFalseItems && initialData.trueFalseItems.length > 0
      ? initialData.trueFalseItems
      : DEFAULT_TF_ITEMS
  );

  // Dạng 2: MCQ 4 lựa chọn (A, B, C, D)
  const [mcqOptions, setMcqOptions] = useState(
    initialData?.options && initialData.options.length > 0
      ? initialData.options
      : DEFAULT_MCQ_OPTIONS
  );

  // Dạng 3: Ghép hợp (Matching)
  const [matchingPairs, setMatchingPairs] = useState(
    initialData?.matchingPairs && initialData.matchingPairs.length > 0
      ? initialData.matchingPairs
      : DEFAULT_MATCHING_PAIRS
  );

  // Dạng 4: Điền số / Trả lời ngắn
  const [saKey, setSaKey] = useState(
    initialData?.shortAnswerKey || {
      acceptedAnswer: '',
      tolerance: 0,
      unit: ''
    }
  );

  useEffect(() => {
    if (!isOpen) return;
    setConflictError(null);
    if (initialData) {
      setSubjectId(initialData.subjectId || 1);
      setQuestionType(initialData.questionType || 'TRUE_FALSE');
      setCognitiveLevel(initialData.cognitiveLevel || 'UNDERSTANDING');
      setDifficulty(initialData.difficulty || 'MEDIUM');
      setQuestionText(initialData.questionText || '');
      setExplanation(initialData.explanation || '');
      const aUrl = (initialData as any).audioUrl || initialData.questionGroup?.audioUrl || '';
      const mPlays = (initialData as any).maxPlays || initialData.questionGroup?.maxPlays || 2;
      setHasGroup(Boolean(initialData.questionGroup || aUrl));
      setGroupTitle(initialData.questionGroup?.title || '');
      setPassageText(initialData.questionGroup?.passageText || '');
      setAudioUrl(aUrl);
      setMaxPlays(mPlays);
      setTfItems(initialData.trueFalseItems && initialData.trueFalseItems.length > 0 ? initialData.trueFalseItems : DEFAULT_TF_ITEMS);
      setMcqOptions(initialData.options && initialData.options.length > 0 ? initialData.options : DEFAULT_MCQ_OPTIONS);
      setMatchingPairs(initialData.matchingPairs && initialData.matchingPairs.length > 0 ? initialData.matchingPairs : DEFAULT_MATCHING_PAIRS);
      setSaKey(initialData.shortAnswerKey || { acceptedAnswer: '', tolerance: 0, unit: '' });
    } else {
      setSubjectId(1);
      setQuestionType('TRUE_FALSE');
      setCognitiveLevel('UNDERSTANDING');
      setDifficulty('MEDIUM');
      setQuestionText('');
      setExplanation('');
      setHasGroup(false);
      setGroupTitle('');
      setPassageText('');
      setAudioUrl('');
      setMaxPlays(2);
      setTfItems(DEFAULT_TF_ITEMS);
      setMcqOptions(DEFAULT_MCQ_OPTIONS);
      setMatchingPairs(DEFAULT_MATCHING_PAIRS);
      setSaKey({ acceptedAnswer: '', tolerance: 0, unit: '' });
    }
  }, [initialData, isOpen]);

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setConflictError(null);
    if (!questionText.trim()) {
      alert('Vui lòng nhập nội dung câu hỏi!');
      return;
    }

    setIsSubmitting(true);
    try {
      const cleanAudioUrl = audioUrl.trim() || undefined;
      const effectiveMaxPlays = maxPlays > 0 ? maxPlays : 2;

      const questionGroup = hasGroup ? {
        id: initialData?.questionGroup?.id || (Date.now() % 100000),
        title: groupTitle || undefined,
        passageText: passageText || undefined,
        audioUrl: cleanAudioUrl,
        maxPlays: effectiveMaxPlays
      } : undefined;

      const payload: Partial<FullQuestionItem> & { audioUrl?: string; maxPlays?: number } = {
        subjectId,
        subjectName: getSubjectNameById(subjectId),
        questionType,
        cognitiveLevel,
        difficulty,
        questionText,
        explanation,
        status: initialData?.status || 'APPROVED',
        rawPoints: 6.0,
        questionGroup,
        audioUrl: cleanAudioUrl,
        maxPlays: effectiveMaxPlays,
        lockVersion: initialData?.lockVersion,
        trueFalseItems: questionType === 'TRUE_FALSE' ? tfItems : undefined,
        options: questionType === 'SINGLE_CHOICE' ? mcqOptions : undefined,
        matchingPairs: questionType === 'MATCHING' ? matchingPairs : undefined,
        shortAnswerKey: questionType === 'SHORT_ANSWER' ? saKey : undefined
      };

      if (isEditing && initialData) {
        const updated = await updateBankQuestion(initialData.id, payload);
        if (updated) {
          onSuccess({
            ...initialData,
            ...payload,
            ...updated
          } as FullQuestionItem);
          onClose();
        }
      } else {
        const saved = await createBankQuestion(payload);
        onSuccess(saved);
        onClose();
      }
    } catch (err: any) {
      if (err?.status === 409 || err?.code === 'CONCURRENT_MODIFICATION') {
        setConflictError(err.message || 'Xung đột phiên bản chỉnh sửa đồng thời (HTTP 409 CONCURRENT_MODIFICATION). Dữ liệu câu hỏi đã bị người khác cập nhật, vui lòng tải lại trước khi lưu.');
      } else {
        setConflictError(err?.message || 'Đã xảy ra lỗi khi lưu câu hỏi.');
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 p-4 backdrop-blur-sm overflow-y-auto">
      <div className="bg-surface-container-lowest w-full max-w-4xl rounded-2xl shadow-2xl border border-outline-variant/30 flex flex-col max-h-[90vh]">
        {/* Modal Header */}
        <div className="px-6 py-4 border-b border-outline-variant/20 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[24px]">
              {isEditing ? 'edit_note' : 'post_add'}
            </span>
            <h2 className="font-headline-sm text-on-surface">
              {isEditing
                ? `Chỉnh sửa câu hỏi ${initialData?.questionCode || `#${initialData?.id}`} (lockVersion v${initialData?.lockVersion ?? 0})`
                : 'Nhập câu hỏi mới chuẩn V-SAT 2026'}
            </h2>
          </div>
          <button
            onClick={onClose}
            type="button"
            className="p-1 rounded text-outline hover:text-on-surface hover:bg-surface-container"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Modal Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6 overflow-y-auto flex-1">
          {conflictError && (
            <div className="p-4 rounded-xl bg-red-50 border border-red-300 text-red-900 text-body-sm flex items-start gap-2.5 shadow-xs">
              <span className="material-symbols-outlined text-red-600 text-[22px] shrink-0">gpp_maybe</span>
              <div className="space-y-0.5">
                <div className="font-bold">Cảnh báo Xung đột Phiên bản (Optimistic Locking 409)</div>
                <div className="text-xs">{conflictError}</div>
              </div>
            </div>
          )}

          {/* Metadata selectors */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="space-y-1">
              <label className="font-label-md text-on-surface font-semibold">Môn thi</label>
              <select
                value={subjectId}
                onChange={(e) => setSubjectId(Number(e.target.value))}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/40"
              >
                {SUBJECTS.map((s) => (
                  <option key={s.id} value={s.id}>{s.name}</option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <label className="font-label-md text-on-surface font-semibold">Dạng câu hỏi V-SAT</label>
              <select
                value={questionType}
                onChange={(e) => setQuestionType(e.target.value as QuestionType)}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/40 font-bold text-primary"
              >
                <option value="TRUE_FALSE">Phần 1: Đúng / Sai</option>
                <option value="SINGLE_CHOICE">Phần 2: MCQ 4 lựa chọn</option>
                <option value="MATCHING">Phần 3: Ghép hợp (Matching)</option>
                <option value="SHORT_ANSWER">Phần 4: Điền số / Trả lời ngắn</option>
              </select>
            </div>

            <div className="space-y-1">
              <label className="font-label-md text-on-surface font-semibold">Mức độ nhận thức</label>
              <select
                value={cognitiveLevel}
                onChange={(e) => setCognitiveLevel(e.target.value as CognitiveLevel)}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/40"
              >
                <option value="RECOGNITION">Nhận biết (NB)</option>
                <option value="UNDERSTANDING">Thông hiểu (TH)</option>
                <option value="APPLICATION">Vận dụng (VD)</option>
                <option value="HIGH_APPLICATION">Vận dụng cao (VDC)</option>
              </select>
            </div>

            <div className="space-y-1">
              <label className="font-label-md text-on-surface font-semibold">Độ khó</label>
              <select
                value={difficulty}
                onChange={(e) => setDifficulty(e.target.value as DifficultyLevel)}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/40"
              >
                <option value="EASY">Dễ (EASY)</option>
                <option value="MEDIUM">Trung bình (MEDIUM)</option>
                <option value="HARD">Khó (HARD)</option>
                <option value="VERY_HARD">Rất khó (VERY_HARD)</option>
              </select>
            </div>
          </div>

          {/* Question Group / Stimulus / Audio Optional Section */}
          <div className="p-4 rounded-xl bg-surface-container-low border border-outline-variant/30 space-y-3">
            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 cursor-pointer font-label-md text-on-surface font-semibold">
                <input
                  type="checkbox"
                  checked={hasGroup}
                  onChange={(e) => setHasGroup(e.target.checked)}
                  className="rounded border-outline-variant/40 text-primary focus:ring-primary h-4 w-4"
                />
                <span>Câu hỏi có ngữ liệu nhóm / bài đọc / file nghe Tiếng Anh (Question Group)</span>
              </label>
              {hasGroup && (
                <span className="text-[11px] text-primary font-medium">Hỗ trợ Audio Listening V-SAT</span>
              )}
            </div>

            {hasGroup && (
              <div className="space-y-3 pt-2 border-t border-outline-variant/20">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <label className="font-label-sm text-outline">Tiêu đề ngữ liệu chung</label>
                    <input
                      type="text"
                      value={groupTitle}
                      onChange={(e) => setGroupTitle(e.target.value)}
                      placeholder="VD: Part 1 - Short Conversations"
                      className="w-full h-8 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40"
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="font-label-sm text-outline">URL file nghe Tiếng Anh (Audio URL)</label>
                    <input
                      type="url"
                      value={audioUrl}
                      onChange={(e) => setAudioUrl(e.target.value)}
                      placeholder="https://example.com/audio/listening-part1.mp3"
                      className="w-full h-8 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono text-[12px]"
                    />
                  </div>
                </div>

                {audioUrl.trim() && (
                  <div className="p-2.5 rounded-lg bg-surface-container-lowest border border-outline-variant/30 space-y-1">
                    <div className="text-[11px] font-bold text-primary flex items-center gap-1">
                      <span className="material-symbols-outlined text-[16px]">headphones</span>
                      <span>Nghe thử Audio Listening:</span>
                    </div>
                    <audio controls preload="none" src={audioUrl.trim()} className="w-full h-8" />
                  </div>
                )}

                <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div className="space-y-1">
                    <label className="font-label-sm text-outline">Số lượt nghe tối đa (maxPlays, mặc định: 2)</label>
                    <input
                      type="number"
                      min={1}
                      max={10}
                      value={maxPlays}
                      onChange={(e) => setMaxPlays(Math.max(1, parseInt(e.target.value) || 2))}
                      className="w-full h-8 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono"
                    />
                  </div>

                  <div className="space-y-1">
                    <label className="font-label-sm text-outline">Đoạn văn đọc hiểu chung (Passage Text, tùy chọn)</label>
                    <textarea
                      rows={2}
                      value={passageText}
                      onChange={(e) => setPassageText(e.target.value)}
                      placeholder="Nội dung bài đọc chung..."
                      className="w-full p-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40"
                    />
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Question Text with Live KaTeX Preview */}
          <div className="space-y-2">
            <div className="flex items-center justify-between">
              <label className="font-label-md text-on-surface font-semibold">
                Nội dung câu hỏi (Hỗ trợ LaTeX kẹp trong $ hoặc $$) *
              </label>
              <span className="text-[11px] text-outline font-mono">Ví dụ: $f(x) = \int_0^1 x^2 dx$</span>
            </div>

            <textarea
              rows={3}
              value={questionText}
              onChange={(e) => setQuestionText(e.target.value)}
              placeholder="Nhập đề bài câu hỏi..."
              className="w-full p-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
              required
            />

            {questionText && (
              <div className="p-3 rounded-lg bg-surface-container border border-outline-variant/30 space-y-1">
                <span className="font-label-sm text-outline text-[11px] uppercase tracking-wider block">
                  Live KaTeX Preview:
                </span>
                <div className="text-body-md text-on-surface font-medium">
                  <MathView content={questionText} />
                </div>
              </div>
            )}
          </div>

          {/* DẠNG THỨC 1: ĐÚNG / SAI */}
          {questionType === 'TRUE_FALSE' && (
            <div className="p-4 rounded-xl bg-cyan-50/40 border border-cyan-200 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-label-md text-cyan-900 font-bold">4 Mệnh đề Đúng / Sai (a, b, c, d)</span>
                <span className="text-[11px] text-cyan-800">Quy chuẩn thang điểm 0-1-2-3-6đ</span>
              </div>

              <div className="space-y-2">
                {tfItems.map((item, idx) => (
                  <div key={item.key} className="flex items-center gap-3 p-2 bg-surface-container-lowest rounded-lg border border-cyan-100">
                    <span className="font-bold text-primary font-mono w-6">{item.key})</span>
                    <input
                      type="text"
                      value={item.text}
                      onChange={(e) => {
                        const copy = tfItems.map((t, i) => i === idx ? { ...t, text: e.target.value } : t);
                        setTfItems(copy);
                      }}
                      placeholder={`Nội dung khẳng định ${item.key} (hỗ trợ LaTeX)... `}
                      className="flex-1 h-8 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/30"
                    />

                    <div className="flex items-center gap-2">
                      <label className="flex items-center gap-1 text-[12px] font-semibold text-emerald-800 cursor-pointer">
                        <input
                          type="radio"
                          name={`tf_${item.key}`}
                          checked={item.isCorrect}
                          onChange={() => {
                            const copy = tfItems.map((t, i) => i === idx ? { ...t, isCorrect: true } : t);
                            setTfItems(copy);
                          }}
                        />
                        <span>Đúng</span>
                      </label>

                      <label className="flex items-center gap-1 text-[12px] font-semibold text-red-800 cursor-pointer">
                        <input
                          type="radio"
                          name={`tf_${item.key}`}
                          checked={!item.isCorrect}
                          onChange={() => {
                            const copy = tfItems.map((t, i) => i === idx ? { ...t, isCorrect: false } : t);
                            setTfItems(copy);
                          }}
                        />
                        <span>Sai</span>
                      </label>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* DẠNG THỨC 2: MCQ 4 LỰA CHỌN */}
          {questionType === 'SINGLE_CHOICE' && (
            <div className="p-4 rounded-xl bg-indigo-50/40 border border-indigo-200 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-label-md text-indigo-900 font-bold">4 Phương án trắc nghiệm (Chọn 1 đáp án đúng)</span>
                <span className="text-[11px] text-indigo-800">Mỗi câu đúng 4.0 điểm</span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
                {mcqOptions.map((opt, idx) => (
                  <div key={opt.label} className="p-2.5 bg-surface-container-lowest rounded-lg border border-indigo-100 space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-primary font-mono">Phương án {opt.label}</span>
                      <label className="flex items-center gap-1 text-[11px] font-bold text-indigo-800 cursor-pointer">
                        <input
                          type="radio"
                          name="mcq_correct"
                          checked={opt.isCorrect}
                          onChange={() => {
                            const copy = mcqOptions.map((o, i) => ({ ...o, isCorrect: i === idx }));
                            setMcqOptions(copy);
                          }}
                        />
                        <span>Đáp án đúng</span>
                      </label>
                    </div>

                    <input
                      type="text"
                      value={opt.text}
                      onChange={(e) => {
                        const copy = mcqOptions.map((o, i) => i === idx ? { ...o, text: e.target.value } : o);
                        setMcqOptions(copy);
                      }}
                      placeholder="Nội dung phương án (hỗ trợ LaTeX)..."
                      className="w-full h-8 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/30"
                    />

                    {opt.text && (
                      <div className="text-[12px] text-on-surface">
                        <MathView content={opt.text} />
                      </div>
                    )}
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* DẠNG THỨC 3: GHÉP HỢP (MATCHING) */}
          {questionType === 'MATCHING' && (
            <div className="p-4 rounded-xl bg-amber-50/40 border border-amber-200 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-label-md text-amber-900 font-bold">4 Cặp ghép hợp (Cột 1 ghép với Cột 2)</span>
                <span className="text-[11px] text-amber-800">9.0 điểm / câu chuẩn</span>
              </div>

              <div className="space-y-2">
                {matchingPairs.map((pair, idx) => (
                  <div key={idx} className="grid grid-cols-1 md:grid-cols-2 gap-2 p-2 bg-surface-container-lowest rounded-lg border border-amber-100">
                    <input
                      type="text"
                      value={pair.left}
                      onChange={(e) => {
                        const copy = matchingPairs.map((p, i) => i === idx ? { ...p, left: e.target.value } : p);
                        setMatchingPairs(copy);
                      }}
                      placeholder={`Vế ${idx + 1} ở Cột 1...`}
                      className="h-8 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/30"
                    />
                    <input
                      type="text"
                      value={pair.right}
                      onChange={(e) => {
                        const copy = matchingPairs.map((p, i) => i === idx ? { ...p, right: e.target.value } : p);
                        setMatchingPairs(copy);
                      }}
                      placeholder={`Vế tương ứng ở Cột 2...`}
                      className="h-8 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/30"
                    />
                  </div>
                ))}
              </div>
            </div>
          )}

          {/* DẠNG THỨC 4: ĐIỀN SỐ / TRẢ LỜI NGẮN */}
          {questionType === 'SHORT_ANSWER' && (
            <div className="p-4 rounded-xl bg-emerald-50/40 border border-emerald-200 space-y-3">
              <div className="flex items-center justify-between">
                <span className="font-label-md text-emerald-900 font-bold">Khóa đáp án Điền số / Trả lời ngắn</span>
                <span className="text-[11px] text-emerald-800">Đúng hoàn toàn = 8.75 điểm</span>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
                <div className="space-y-1">
                  <label className="font-label-sm text-outline">Đáp án chuẩn (Số hoặc chuỗi) *</label>
                  <input
                    type="text"
                    value={saKey.acceptedAnswer}
                    onChange={(e) => setSaKey(prev => ({ ...prev, acceptedAnswer: e.target.value }))}
                    placeholder="VD: 37 hoặc 7.44"
                    className="w-full h-8 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono font-bold"
                    required
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-label-sm text-outline">Dung sai cho phép (Tolerance)</label>
                  <input
                    type="number"
                    step="0.01"
                    value={saKey.tolerance ?? 0}
                    onChange={(e) => setSaKey(prev => ({ ...prev, tolerance: Number(e.target.value) }))}
                    className="w-full h-8 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-label-sm text-outline">Đơn vị (Unit)</label>
                  <input
                    type="text"
                    value={saKey.unit ?? ''}
                    onChange={(e) => setSaKey(prev => ({ ...prev, unit: e.target.value }))}
                    placeholder="VD: cm, m/s, lít..."
                    className="w-full h-8 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40"
                  />
                </div>
              </div>
            </div>
          )}

          {/* Explanation */}
          <div className="space-y-2">
            <label className="font-label-md text-on-surface font-semibold">
              Lời giải chi tiết & Hướng dẫn chấm (Hỗ trợ LaTeX)
            </label>
            <textarea
              rows={2}
              value={explanation}
              onChange={(e) => setExplanation(e.target.value)}
              placeholder="Giải thích từng bước, công thức áp dụng..."
              className="w-full p-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
            />
          </div>

          {/* Footer buttons */}
          <div className="flex items-center justify-end gap-3 pt-3 border-t border-outline-variant/20">
            <button
              type="button"
              onClick={onClose}
              className="h-9 px-4 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md transition-colors"
            >
              Hủy bỏ
            </button>
            <button
              type="submit"
              disabled={isSubmitting}
              className="h-9 px-5 rounded-lg bg-primary hover:bg-tertiary text-on-primary font-label-md font-bold shadow-sm transition-colors disabled:opacity-50 flex items-center gap-2"
            >
              {isSubmitting ? 'Đang lưu...' : (isEditing ? 'Cập nhật câu hỏi' : 'Lưu câu hỏi vào ngân hàng')}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
