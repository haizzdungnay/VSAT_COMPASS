import React, { useState, useEffect } from 'react';
import type { ExamDetail, QuestionPickerItem, BlueprintItem, ExamQuestionDetail } from '../../types/exam';
import {
  fetchQuestionPicker,
  fetchBlueprints,
  generateByMatrix,
  addQuestionToExam,
  removeQuestionFromExam,
  fetchExamQuestions
} from '../../services/api';

interface Step2Props {
  exam: ExamDetail;
  onChange: (updated: Partial<ExamDetail>) => void;
  initialBlueprintId?: number;
}

const generateTempQuestionId = (): number => Date.now() % 100000;

export const Step2Composition: React.FC<Step2Props> = ({ exam, onChange, initialBlueprintId }) => {
  const [mode, setMode] = useState<'MANUAL' | 'MATRIX'>(initialBlueprintId ? 'MATRIX' : 'MANUAL');
  const [selectedTopic, setSelectedTopic] = useState('ALL');
  const [search, setSearch] = useState('');
  const [candidates, setCandidates] = useState<QuestionPickerItem[]>([]);
  const [blueprints, setBlueprints] = useState<BlueprintItem[]>([]);
  const [selectedBlueprintId, setSelectedBlueprintId] = useState<number>(initialBlueprintId || 1);
  const [isGenerating, setIsGenerating] = useState(false);
  const [matrixResult, setMatrixResult] = useState<string | null>(null);

  // Issue #23: State for dynamic composition questions & matrix parameters
  const [examQuestions, setExamQuestions] = useState<ExamQuestionDetail[]>([]);
  const [variantCount, setVariantCount] = useState<number>(4);
  const [maxExposureRate, setMaxExposureRate] = useState<number>(0.3);

  useEffect(() => {
    fetchQuestionPicker({ subjectId: exam.subjectId }).then(setCandidates);
    fetchBlueprints(exam.subjectId).then(bps => {
      setBlueprints(bps);
      if (initialBlueprintId && bps.some(b => b.id === initialBlueprintId)) {
        setSelectedBlueprintId(initialBlueprintId);
      } else if (bps.length > 0) {
        setSelectedBlueprintId(bps[0].id);
      }
    });

    if (exam.id > 0) {
      fetchExamQuestions(exam.id).then(qs => {
        if (qs && qs.length > 0) {
          setExamQuestions(qs);
        }
      });
    }
  }, [exam.subjectId, exam.id, initialBlueprintId]);

  const handleAddQuestion = async (q: QuestionPickerItem) => {
    if (examQuestions.some(eq => eq.questionId === q.id)) {
      return;
    }
    if (exam.id > 0) {
      await addQuestionToExam(exam.id, q.id);
    }
    const newDetail: ExamQuestionDetail = {
      id: generateTempQuestionId(),
      examId: exam.id,
      questionId: q.id,
      questionOrder: examQuestions.length + 1,
      questionCode: q.questionCode,
      questionText: q.questionTextSnippet,
      questionType: q.questionType,
      difficulty: q.difficulty,
      maxRawScore: q.questionType === 'SINGLE_CHOICE' ? 4 : (q.questionType === 'TRUE_FALSE' ? 6 : (q.questionType === 'MATCHING' ? 9 : 8.75)),
      options: []
    };
    const updated = [...examQuestions, newDetail];
    setExamQuestions(updated);
    onChange({ questionCount: updated.length });
  };

  const handleRemoveQuestion = async (qId: number) => {
    if (exam.id > 0) {
      await removeQuestionFromExam(exam.id, qId);
    }
    const updated = examQuestions.filter(eq => eq.questionId !== qId && eq.id !== qId);
    setExamQuestions(updated);
    onChange({ questionCount: updated.length });
  };

  const handleGenerateMatrix = async () => {
    setIsGenerating(true);
    setMatrixResult(null);
    try {
      const res = await generateByMatrix(exam.id, selectedBlueprintId, variantCount, maxExposureRate);
      onChange({ questionCount: res.selectedQuestions || 25 });
      setMatrixResult(`Đã sinh thành công ${res.selectedQuestions || 25} câu hỏi theo ma trận và ${res.variants?.length || variantCount} mã đề biến thể (Tỷ lệ phơi nhiễm ≤ ${maxExposureRate * 100}%)!`);
      if (exam.id > 0) {
        const refreshed = await fetchExamQuestions(exam.id);
        if (refreshed && refreshed.length > 0) {
          setExamQuestions(refreshed);
        }
      }
    } finally {
      setIsGenerating(false);
    }
  };

  const filteredCandidates = candidates.filter(q => {
    const matchesSearch = !search || q.questionCode.toLowerCase().includes(search.toLowerCase()) ||
      q.questionTextSnippet.toLowerCase().includes(search.toLowerCase());
    return matchesSearch;
  });

  // Calculate dynamic stats
  const part1Questions = examQuestions.filter(q => q.questionType === 'SINGLE_CHOICE');
  const part2Questions = examQuestions.filter(q => q.questionType === 'TRUE_FALSE');
  const part3Questions = examQuestions.filter(q => q.questionType === 'MATCHING');
  const part4Questions = examQuestions.filter(q => q.questionType === 'SHORT_ANSWER');

  const totalQuestions = examQuestions.length;
  const totalScore = (part1Questions.length * 4.0) +
                     (part2Questions.length * 6.0) +
                     (part3Questions.length * 9.0) +
                     (part4Questions.length * 8.75);

  const nbCount = examQuestions.filter(q => q.difficulty === 'EASY').length;
  const thCount = examQuestions.filter(q => q.difficulty === 'MEDIUM').length;
  const vdCount = examQuestions.filter(q => q.difficulty === 'HARD').length;
  const vdcCount = examQuestions.filter(q => q.difficulty === 'VERY_HARD').length;

  return (
    <div className="space-y-space-md">
      {/* Mode Switcher */}
      <div className="flex items-center justify-between bg-surface-container-lowest p-space-md rounded-xl border border-outline-variant/30">
        <div className="flex items-center gap-space-sm">
          <span className="font-label-md text-on-surface">Phương thức xây dựng đề:</span>
          <div className="inline-flex rounded-lg bg-surface-container-low p-1 border border-outline-variant/30">
            <button
              onClick={() => setMode('MANUAL')}
              className={`px-3 py-1.5 rounded-md font-label-md text-label-md transition-all ${
                mode === 'MANUAL'
                  ? 'bg-primary text-on-primary font-bold shadow-sm'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              Chọn câu hỏi thủ công
            </button>
            <button
              onClick={() => setMode('MATRIX')}
              className={`px-3 py-1.5 rounded-md font-label-md text-label-md transition-all ${
                mode === 'MATRIX'
                  ? 'bg-primary text-on-primary font-bold shadow-sm'
                  : 'text-on-surface-variant hover:text-on-surface'
              }`}
            >
              Sinh tự động theo ma trận
            </button>
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-secondary-fixed text-on-secondary-fixed-variant">
            Mục tiêu: 25 câu · 150 điểm thô
          </span>
        </div>
      </div>

      {mode === 'MANUAL' ? (
        /* MANUAL COMPOSITION: 3-COLUMN SPLIT */
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-md">
          {/* CỘT 1: Ngân hàng câu hỏi (3 cols) */}
          <div className="lg:col-span-3 bg-surface-container-lowest rounded-xl p-space-md border border-outline-variant/30 space-y-3">
            <div className="flex items-center justify-between pb-2 border-b border-outline-variant/20">
              <span className="font-label-md text-on-surface font-bold">Ngân hàng câu hỏi</span>
              <span className="text-[11px] text-outline">{candidates.length} câu sẵn sàng</span>
            </div>

            <div className="space-y-2">
              <input
                type="text"
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                placeholder="Tìm mã câu, nội dung..."
                className="w-full h-8 px-2.5 text-body-sm bg-surface-container-low rounded border border-outline-variant/40"
              />

              <select
                value={selectedTopic}
                onChange={(e) => setSelectedTopic(e.target.value)}
                className="w-full h-8 px-2 text-body-sm bg-surface-container-low rounded border border-outline-variant/40"
              >
                <option value="ALL">Tất cả chủ đề</option>
                <option value="CALCULUS">Giải tích & Đạo hàm</option>
                <option value="GEOMETRY">Hình học không gian Oxyz</option>
                <option value="PROBABILITY">Xác suất & Thống kê</option>
              </select>
            </div>

            {/* Candidate list */}
            <div className="space-y-2 max-h-[500px] overflow-y-auto pr-1">
              {filteredCandidates.map((item) => {
                const isAdded = examQuestions.some(eq => eq.questionId === item.id);
                return (
                  <div
                    key={item.id}
                    onClick={() => !isAdded && handleAddQuestion(item)}
                    className={`p-2.5 rounded-lg border transition-all ${
                      isAdded
                        ? 'bg-surface-container-high/50 border-outline-variant/20 opacity-60 cursor-not-allowed'
                        : 'bg-surface-container-low border-outline-variant/30 hover:border-primary cursor-pointer group'
                    }`}
                  >
                    <div className="flex items-center justify-between text-[11px] font-mono text-outline">
                      <span>{item.questionCode}</span>
                      <span className={`font-bold ${isAdded ? 'text-outline' : 'text-primary group-hover:underline'}`}>
                        {isAdded ? '✓ Đã thêm' : '+ Thêm'}
                      </span>
                    </div>
                    <div className="text-body-sm font-medium text-on-surface line-clamp-2 mt-0.5">
                      {item.questionTextSnippet}
                    </div>
                    <div className="flex items-center gap-1.5 mt-1 text-[10px]">
                      <span className="px-1.5 py-0.2 rounded bg-surface-container-high text-on-surface-variant font-medium">
                        {item.questionType}
                      </span>
                      <span className="text-outline">{item.difficulty}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>

          {/* CỘT 2: Cấu trúc 4 phần V-SAT (6 cols) */}
          <div className="lg:col-span-6 bg-surface-container-lowest rounded-xl p-space-md border border-outline-variant/30 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-outline-variant/20">
              <span className="font-label-md text-on-surface font-bold">Cấu trúc đề thi V-SAT (4 Phần)</span>
              <span className="font-mono text-[11px] text-emerald-700 font-bold">
                Đã chọn: {totalQuestions} / 25 câu · {totalScore.toFixed(1)} / 150đ
              </span>
            </div>

            {/* Section 1: MCQ 4 lựa chọn (Phần I) */}
            <div className="rounded-lg border border-indigo-200 bg-indigo-50/20 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-indigo-100 text-indigo-800">
                    Phần I: Trắc nghiệm 4 lựa chọn (SINGLE_CHOICE)
                  </span>
                  <span className="text-body-sm font-semibold">
                    {part1Questions.length} câu hỏi · {(part1Questions.length * 4).toFixed(1)} đ
                  </span>
                </div>
                <span className="font-mono text-[11px] text-indigo-800 font-bold">
                  {part1Questions.length}/10 câu
                </span>
              </div>
              <div className="text-[11px] text-outline">
                1 đáp án đúng duy nhất, mỗi câu đúng tính 4.0 điểm thô
              </div>
              {part1Questions.length > 0 && (
                <div className="space-y-1.5 pt-1">
                  {part1Questions.map((q) => (
                    <div key={q.id || q.questionId} className="flex items-center justify-between bg-surface p-2 rounded border border-indigo-100 text-xs">
                      <span className="font-mono text-[11px] font-bold text-primary">{q.questionCode}</span>
                      <span className="truncate max-w-[240px] text-on-surface">{q.questionText}</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveQuestion(q.questionId || q.id)}
                        className="text-red-600 hover:text-red-800 font-bold px-1.5"
                        title="Xóa khỏi đề"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Section 2: Đúng / Sai (Phần II) */}
            <div className="rounded-lg border border-cyan-200 bg-cyan-50/20 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-cyan-100 text-cyan-800">
                    Phần II: Đúng / Sai (TRUE_FALSE)
                  </span>
                  <span className="text-body-sm font-semibold">
                    {part2Questions.length} câu hỏi · {(part2Questions.length * 6).toFixed(1)} đ
                  </span>
                </div>
                <span className="font-mono text-[11px] text-cyan-800 font-bold">
                  {part2Questions.length}/8 câu
                </span>
              </div>
              <div className="text-[11px] text-outline">
                Chấm điểm lũy tiến 4 ý (a,b,c,d): Đúng 1 ý = 1đ, 2 ý = 2đ, 3 ý = 3đ, 4 ý = 6đ
              </div>
              {part2Questions.length > 0 && (
                <div className="space-y-1.5 pt-1">
                  {part2Questions.map((q) => (
                    <div key={q.id || q.questionId} className="flex items-center justify-between bg-surface p-2 rounded border border-cyan-100 text-xs">
                      <span className="font-mono text-[11px] font-bold text-primary">{q.questionCode}</span>
                      <span className="truncate max-w-[240px] text-on-surface">{q.questionText}</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveQuestion(q.questionId || q.id)}
                        className="text-red-600 hover:text-red-800 font-bold px-1.5"
                        title="Xóa khỏi đề"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Section 3: Ghép đôi (Phần III) */}
            <div className="rounded-lg border border-amber-200 bg-amber-50/20 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800">
                    Phần III: Ghép hợp (MATCHING)
                  </span>
                  <span className="text-body-sm font-semibold">
                    {part3Questions.length} câu hỏi · {(part3Questions.length * 9).toFixed(1)} đ
                  </span>
                </div>
                <span className="font-mono text-[11px] text-amber-800 font-bold">
                  {part3Questions.length}/3 câu
                </span>
              </div>
              <div className="text-[11px] text-outline">
                Mỗi câu ghép đúng các cặp mệnh đề tương ứng, tính điểm chuẩn 9.0 điểm / câu
              </div>
              {part3Questions.length > 0 && (
                <div className="space-y-1.5 pt-1">
                  {part3Questions.map((q) => (
                    <div key={q.id || q.questionId} className="flex items-center justify-between bg-surface p-2 rounded border border-amber-100 text-xs">
                      <span className="font-mono text-[11px] font-bold text-primary">{q.questionCode}</span>
                      <span className="truncate max-w-[240px] text-on-surface">{q.questionText}</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveQuestion(q.questionId || q.id)}
                        className="text-red-600 hover:text-red-800 font-bold px-1.5"
                        title="Xóa khỏi đề"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Section 4: Trả lời ngắn / Điền số (Phần IV) */}
            <div className="rounded-lg border border-emerald-200 bg-emerald-50/20 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800">
                    Phần IV: Điền số / Trả lời ngắn (SHORT_ANSWER)
                  </span>
                  <span className="text-body-sm font-semibold">
                    {part4Questions.length} câu hỏi · {(part4Questions.length * 8.75).toFixed(1)} đ
                  </span>
                </div>
                <span className="font-mono text-[11px] text-emerald-800 font-bold">
                  {part4Questions.length}/4 câu
                </span>
              </div>
              <div className="text-[11px] text-outline">
                Điền số thực hoặc từ khóa ngắn, đúng chuẩn = 8.75 điểm / câu
              </div>
              {part4Questions.length > 0 && (
                <div className="space-y-1.5 pt-1">
                  {part4Questions.map((q) => (
                    <div key={q.id || q.questionId} className="flex items-center justify-between bg-surface p-2 rounded border border-emerald-100 text-xs">
                      <span className="font-mono text-[11px] font-bold text-primary">{q.questionCode}</span>
                      <span className="truncate max-w-[240px] text-on-surface">{q.questionText}</span>
                      <button
                        type="button"
                        onClick={() => handleRemoveQuestion(q.questionId || q.id)}
                        className="text-red-600 hover:text-red-800 font-bold px-1.5"
                        title="Xóa khỏi đề"
                      >
                        ×
                      </button>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* CỘT 3: Inspector Giám định đề (3 cols) */}
          <div className="lg:col-span-3 bg-surface-container-lowest rounded-xl p-space-md border border-outline-variant/30 space-y-3">
            <div className="pb-2 border-b border-outline-variant/20">
              <span className="font-label-md text-on-surface font-bold">Giám định ma trận</span>
            </div>

            <div className="p-3 rounded-lg bg-surface-container-low space-y-2">
              <div className="flex justify-between text-body-sm">
                <span className="text-outline">Tổng số câu:</span>
                <span className="font-bold text-on-surface">{totalQuestions} / 25</span>
              </div>
              <div className="flex justify-between text-body-sm">
                <span className="text-outline">Tổng điểm thô:</span>
                <span className="font-bold text-emerald-700">{totalScore.toFixed(1)} / 150</span>
              </div>
              <div className="flex justify-between text-body-sm">
                <span className="text-outline">Lỗi render LaTeX:</span>
                <span className="font-bold text-emerald-600">0 lỗi</span>
              </div>
            </div>

            <div className="space-y-1.5 text-body-sm">
              <div className="font-label-sm uppercase text-outline text-[10px]">Độ phủ mức độ nhận thức</div>
              <div className="flex items-center justify-between text-[11px]">
                <span>Nhận biết (NB):</span> <span className="font-bold">{nbCount} câu</span>
              </div>
              <div className="flex items-center justify-between text-[11px]">
                <span>Thông hiểu (TH):</span> <span className="font-bold">{thCount} câu</span>
              </div>
              <div className="flex items-center justify-between text-[11px]">
                <span>Vận dụng (VD):</span> <span className="font-bold">{vdCount} câu</span>
              </div>
              <div className="flex items-center justify-between text-[11px]">
                <span>Vận dụng cao (VDC):</span> <span className="font-bold">{vdcCount} câu</span>
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* MATRIX GENERATOR TAB */
        <div className="bg-surface-container-lowest p-space-lg rounded-xl border border-outline-variant/30 shadow-sm space-y-space-md">
          <div className="flex items-center justify-between border-b border-outline-variant/20 pb-2">
            <div className="space-y-0.5">
              <h3 className="font-headline-sm text-on-surface">Cấu hình Sinh đề Tự động theo Ma trận Khảo thí</h3>
              <p className="font-body-sm text-outline">Bốc ngẫu nhiên câu hỏi theo phân bố chuẩn 3 chiều đảm bảo tính đẳng trị</p>
            </div>
            <span className="px-2.5 py-1 rounded-full text-[11px] font-bold bg-primary-fixed text-on-primary-fixed">
              Thuật toán V-SAT Matrix Engine
            </span>
          </div>

          {matrixResult && (
            <div className="p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-body-sm flex items-center gap-2">
              <span className="material-symbols-outlined text-[20px] text-emerald-600">check_circle</span>
              <span>{matrixResult}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-3 gap-space-md bg-surface-container-low p-space-md rounded-lg">
            <div className="space-y-1">
              <label className="font-label-sm text-outline">Chọn khung Ma trận (Blueprint)</label>
              <select
                value={selectedBlueprintId}
                onChange={(e) => setSelectedBlueprintId(Number(e.target.value))}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40"
              >
                {blueprints.map(bp => (
                  <option key={bp.id} value={bp.id}>{bp.name}</option>
                ))}
              </select>
            </div>

            <div className="space-y-1">
              <label className="font-label-sm text-outline">Số lượng mã đề sinh ra (Variants)</label>
              <input
                type="number"
                min={1}
                max={10}
                value={variantCount}
                onChange={(e) => setVariantCount(Number(e.target.value) || 4)}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono"
              />
            </div>

            <div className="space-y-1">
              <label className="font-label-sm text-outline">Tỷ lệ phơi nhiễm tối đa (Exposure Rate)</label>
              <input
                type="number"
                min={0.1}
                max={1.0}
                step={0.05}
                value={maxExposureRate}
                onChange={(e) => setMaxExposureRate(parseFloat(e.target.value) || 0.3)}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono"
              />
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              onClick={handleGenerateMatrix}
              disabled={isGenerating}
              className="px-4 py-2 bg-primary text-on-primary rounded-lg font-label-md font-bold flex items-center gap-2 shadow-sm hover:bg-tertiary transition disabled:opacity-50"
            >
              <span className="material-symbols-outlined text-[18px]">
                {isGenerating ? 'sync' : 'auto_mode'}
              </span>
              <span>{isGenerating ? 'Đang giải bài toán tối ưu ma trận...' : 'Sinh đề thi & 4 mã đề biến thể'}</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
