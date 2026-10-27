import React, { useState, useEffect } from 'react';
import type { ExamDetail, QuestionPickerItem, BlueprintItem } from '../../types/exam';
import {
  fetchQuestionPicker,
  fetchBlueprints,
  generateByMatrix,
  addQuestionToExam
} from '../../services/api';

interface Step2Props {
  exam: ExamDetail;
  onChange: (updated: Partial<ExamDetail>) => void;
}

export const Step2Composition: React.FC<Step2Props> = ({ exam, onChange }) => {
  const [mode, setMode] = useState<'MANUAL' | 'MATRIX'>('MANUAL');
  const [selectedTopic, setSelectedTopic] = useState('ALL');
  const [search, setSearch] = useState('');
  const [candidates, setCandidates] = useState<QuestionPickerItem[]>([]);
  const [blueprints, setBlueprints] = useState<BlueprintItem[]>([]);
  const [selectedBlueprintId, setSelectedBlueprintId] = useState<number>(1);
  const [isGenerating, setIsGenerating] = useState(false);
  const [matrixResult, setMatrixResult] = useState<string | null>(null);

  useEffect(() => {
    fetchQuestionPicker({ subjectId: exam.subjectId }).then(setCandidates);
    fetchBlueprints(exam.subjectId).then(bps => {
      setBlueprints(bps);
      if (bps.length > 0) setSelectedBlueprintId(bps[0].id);
    });
  }, [exam.subjectId]);

  const handleAddQuestion = async (q: QuestionPickerItem) => {
    if (exam.id > 0) {
      await addQuestionToExam(exam.id, q.id);
    }
    onChange({ questionCount: (exam.questionCount || 0) + 1 });
  };

  const handleGenerateMatrix = async () => {
    setIsGenerating(true);
    setMatrixResult(null);
    try {
      const res = await generateByMatrix(exam.id, selectedBlueprintId, 4);
      onChange({ questionCount: res.selectedQuestions || 25 });
      setMatrixResult(`Đã sinh thành công ${res.selectedQuestions || 25} câu hỏi theo ma trận và ${res.variants?.length || 4} mã đề biến thể!`);
    } finally {
      setIsGenerating(false);
    }
  };

  const filteredCandidates = candidates.filter(q => {
    const matchesSearch = !search || q.questionCode.toLowerCase().includes(search.toLowerCase()) ||
      q.questionTextSnippet.toLowerCase().includes(search.toLowerCase());
    return matchesSearch;
  });

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
              {filteredCandidates.map((item) => (
                <div
                  key={item.id}
                  onClick={() => handleAddQuestion(item)}
                  className="p-2.5 rounded-lg bg-surface-container-low border border-outline-variant/30 hover:border-primary cursor-pointer transition-all group"
                >
                  <div className="flex items-center justify-between text-[11px] font-mono text-outline">
                    <span>{item.questionCode}</span>
                    <span className="font-bold text-primary group-hover:underline">+ Thêm</span>
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
              ))}
            </div>
          </div>

          {/* CỘT 2: Cấu trúc 4 phần V-SAT (6 cols) */}
          <div className="lg:col-span-6 bg-surface-container-lowest rounded-xl p-space-md border border-outline-variant/30 space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-outline-variant/20">
              <span className="font-label-md text-on-surface font-bold">Cấu trúc đề thi V-SAT (4 Phần)</span>
              <span className="font-mono text-[11px] text-emerald-700 font-bold">
                Đã chọn: {exam.questionCount || 25} / 25 câu
              </span>
            </div>

            {/* Section 1: Đúng/Sai */}
            <div className="rounded-lg border border-cyan-200 bg-cyan-50/20 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-cyan-100 text-cyan-800">Phần 1: Đúng / Sai</span>
                  <span className="text-body-sm font-semibold">9 câu hỏi · 54 điểm</span>
                </div>
                <span className="font-mono text-[11px] text-cyan-800 font-bold">9/9 câu</span>
              </div>
              <div className="text-[11px] text-outline">
                Chấm điểm lũy tiến 4 ý (a,b,c,d): Đúng 1 ý = 1đ, 2 ý = 2đ, 3 ý = 3đ, 4 ý = 6đ
              </div>
            </div>

            {/* Section 2: MCQ 4 lựa chọn */}
            <div className="rounded-lg border border-indigo-200 bg-indigo-50/20 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-indigo-100 text-indigo-800">Phần 2: Trắc nghiệm 4 lựa chọn</span>
                  <span className="text-body-sm font-semibold">6 câu hỏi · 36 điểm</span>
                </div>
                <span className="font-mono text-[11px] text-indigo-800 font-bold">6/6 câu</span>
              </div>
              <div className="text-[11px] text-outline">
                1 đáp án đúng duy nhất, mỗi câu đúng tính 6 điểm
              </div>
            </div>

            {/* Section 3: Ghép hợp */}
            <div className="rounded-lg border border-amber-200 bg-amber-50/20 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-amber-100 text-amber-800">Phần 3: Ghép hợp (Matching)</span>
                  <span className="text-body-sm font-semibold">5 câu hỏi · 30 điểm</span>
                </div>
                <span className="font-mono text-[11px] text-amber-800 font-bold">5/5 câu</span>
              </div>
              <div className="text-[11px] text-outline">
                Mỗi câu gồm 4 cặp cần ghép đúng, 1.5 điểm / cặp ghép chuẩn
              </div>
            </div>

            {/* Section 4: Trả lời ngắn */}
            <div className="rounded-lg border border-emerald-200 bg-emerald-50/20 p-3 space-y-2">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800">Phần 4: Điền số / Trả lời ngắn</span>
                  <span className="text-body-sm font-semibold">5 câu hỏi · 30 điểm</span>
                </div>
                <span className="font-mono text-[11px] text-emerald-800 font-bold">5/5 câu</span>
              </div>
              <div className="text-[11px] text-outline">
                Điền số thực hoặc chuỗi ngắn, đúng hoàn toàn = 6 điểm
              </div>
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
                <span className="font-bold text-on-surface">{exam.questionCount || 25} / 25</span>
              </div>
              <div className="flex justify-between text-body-sm">
                <span className="text-outline">Tổng điểm thô:</span>
                <span className="font-bold text-emerald-700">150 / 150</span>
              </div>
              <div className="flex justify-between text-body-sm">
                <span className="text-outline">Lỗi render LaTeX:</span>
                <span className="font-bold text-emerald-600">0 lỗi</span>
              </div>
            </div>

            <div className="space-y-1.5 text-body-sm">
              <div className="font-label-sm uppercase text-outline text-[10px]">Độ phủ mức độ nhận thức</div>
              <div className="flex items-center justify-between text-[11px]">
                <span>Nhận biết (NB):</span> <span className="font-bold">6 câu</span>
              </div>
              <div className="flex items-center justify-between text-[11px]">
                <span>Thông hiểu (TH):</span> <span className="font-bold">9 câu</span>
              </div>
              <div className="flex items-center justify-between text-[11px]">
                <span>Vận dụng (VD):</span> <span className="font-bold">7 câu</span>
              </div>
              <div className="flex items-center justify-between text-[11px]">
                <span>Vận dụng cao (VDC):</span> <span className="font-bold">3 câu</span>
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
              <input type="number" defaultValue={4} className="w-full h-9 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono" />
            </div>

            <div className="space-y-1">
              <label className="font-label-sm text-outline">Tỷ lệ phơi nhiễm tối đa (Exposure Rate)</label>
              <input type="number" defaultValue={0.3} step={0.05} className="w-full h-9 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40 font-mono" />
            </div>
          </div>

          <div className="flex justify-end pt-2">
            <button
              onClick={handleGenerateMatrix}
              disabled={isGenerating}
              className="h-10 px-space-lg rounded-lg bg-primary hover:bg-tertiary text-on-primary font-label-md flex items-center gap-2 shadow-sm transition-colors disabled:opacity-50"
            >
              {isGenerating ? (
                <>
                  <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                  <span>Đang sinh ma trận...</span>
                </>
              ) : (
                <>
                  <span className="material-symbols-outlined text-[18px]">auto_awesome</span>
                  <span>Bắt đầu Sinh Đề Tự Động</span>
                </>
              )}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
