import React, { useState } from 'react';
import type { FullQuestionItem, QuestionType, CognitiveLevel, DifficultyLevel } from '../../types/exam';
import { createBankQuestion } from '../../services/api';
import { MathView } from '../../components/MathView';

interface ModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (newQuestion: FullQuestionItem) => void;
}

export const QuestionCreateModal: React.FC<ModalProps> = ({ isOpen, onClose, onSuccess }) => {
  const [subjectId, setSubjectId] = useState<number>(1);
  const [questionType, setQuestionType] = useState<QuestionType>('TRUE_FALSE');
  const [cognitiveLevel, setCognitiveLevel] = useState<CognitiveLevel>('UNDERSTANDING');
  const [difficulty, setDifficulty] = useState<DifficultyLevel>('MEDIUM');
  const [questionText, setQuestionText] = useState('');
  const [explanation, setExplanation] = useState('');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Dạng 1: Đúng/Sai (4 ý a, b, c, d)
  const [tfItems, setTfItems] = useState([
    { key: 'a', text: '', isCorrect: true },
    { key: 'b', text: '', isCorrect: false },
    { key: 'c', text: '', isCorrect: true },
    { key: 'd', text: '', isCorrect: false }
  ]);

  // Dạng 2: MCQ 4 lựa chọn (A, B, C, D)
  const [mcqOptions, setMcqOptions] = useState([
    { label: 'A', text: '', isCorrect: true },
    { label: 'B', text: '', isCorrect: false },
    { label: 'C', text: '', isCorrect: false },
    { label: 'D', text: '', isCorrect: false }
  ]);

  // Dạng 3: Ghép hợp (Matching)
  const [matchingPairs, setMatchingPairs] = useState([
    { left: '1. ', right: 'A. ' },
    { left: '2. ', right: 'B. ' },
    { left: '3. ', right: 'C. ' },
    { left: '4. ', right: 'D. ' }
  ]);

  // Dạng 4: Điền số / Trả lời ngắn
  const [saKey, setSaKey] = useState({
    acceptedAnswer: '',
    tolerance: 0,
    unit: ''
  });

  if (!isOpen) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!questionText.trim()) {
      alert('Vui lòng nhập nội dung câu hỏi!');
      return;
    }

    setIsSubmitting(true);
    try {
      const subjectNameMap: Record<number, string> = {
        1: 'Toán học', 2: 'Tiếng Anh', 3: 'Vật lí', 4: 'Hóa học', 5: 'Sinh học', 6: 'Lịch sử', 7: 'Địa lí'
      };

      const newQ: Partial<FullQuestionItem> = {
        subjectId,
        subjectName: subjectNameMap[subjectId] || 'Toán học',
        questionType,
        cognitiveLevel,
        difficulty,
        questionText,
        explanation,
        status: 'APPROVED',
        rawPoints: 6.0,
        trueFalseItems: questionType === 'TRUE_FALSE' ? tfItems : undefined,
        options: questionType === 'SINGLE_CHOICE' ? mcqOptions : undefined,
        matchingPairs: questionType === 'MATCHING' ? matchingPairs : undefined,
        shortAnswerKey: questionType === 'SHORT_ANSWER' ? saKey : undefined
      };

      const saved = await createBankQuestion(newQ);
      onSuccess(saved);
      onClose();
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
            <span className="material-symbols-outlined text-primary text-[24px]">post_add</span>
            <h2 className="font-headline-sm text-on-surface">Nhập câu hỏi mới chuẩn V-SAT 2026</h2>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded text-outline hover:text-on-surface hover:bg-surface-container"
          >
            <span className="material-symbols-outlined">close</span>
          </button>
        </div>

        {/* Modal Body */}
        <form onSubmit={handleSubmit} className="p-6 space-y-6 overflow-y-auto flex-1">
          {/* Metadata selectors */}
          <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
            <div className="space-y-1">
              <label className="font-label-md text-on-surface font-semibold">Môn thi</label>
              <select
                value={subjectId}
                onChange={(e) => setSubjectId(Number(e.target.value))}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/40"
              >
                <option value={1}>Toán học</option>
                <option value={2}>Tiếng Anh</option>
                <option value={3}>Vật lí</option>
                <option value={4}>Hóa học</option>
                <option value={5}>Sinh học</option>
                <option value={6}>Lịch sử</option>
                <option value={7}>Địa lí</option>
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
                        const copy = [...tfItems];
                        copy[idx].text = e.target.value;
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
                            const copy = [...tfItems];
                            copy[idx].isCorrect = true;
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
                            const copy = [...tfItems];
                            copy[idx].isCorrect = false;
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
                <span className="text-[11px] text-indigo-800">Mỗi câu đúng 6.0 điểm</span>
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
                        const copy = [...mcqOptions];
                        copy[idx].text = e.target.value;
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
                <span className="text-[11px] text-amber-800">1.5 điểm / cặp ghép chuẩn</span>
              </div>

              <div className="space-y-2">
                {matchingPairs.map((pair, idx) => (
                  <div key={idx} className="grid grid-cols-1 md:grid-cols-2 gap-2 p-2 bg-surface-container-lowest rounded-lg border border-amber-100">
                    <input
                      type="text"
                      value={pair.left}
                      onChange={(e) => {
                        const copy = [...matchingPairs];
                        copy[idx].left = e.target.value;
                        setMatchingPairs(copy);
                      }}
                      placeholder={`Vế ${idx + 1} ở Cột 1...`}
                      className="h-8 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/30"
                    />
                    <input
                      type="text"
                      value={pair.right}
                      onChange={(e) => {
                        const copy = [...matchingPairs];
                        copy[idx].right = e.target.value;
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
                <span className="text-[11px] text-emerald-800">Đúng hoàn toàn = 6.0 điểm</span>
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
                    value={saKey.tolerance}
                    onChange={(e) => setSaKey(prev => ({ ...prev, tolerance: Number(e.target.value) }))}
                    className="w-full h-8 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono"
                  />
                </div>

                <div className="space-y-1">
                  <label className="font-label-sm text-outline">Đơn vị (Unit)</label>
                  <input
                    type="text"
                    value={saKey.unit}
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
              {isSubmitting ? 'Đang lưu...' : 'Lưu câu hỏi vào ngân hàng'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
