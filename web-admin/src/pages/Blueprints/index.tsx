import React, { useState, useEffect } from 'react';
import type { BlueprintItem } from '../../types/exam';
import { fetchBlueprints, fetchSubjects } from '../../services/api';

interface BlueprintsPageProps {
  onCreateExamClick?: (blueprint?: BlueprintItem) => void;
}

export const BlueprintsPage: React.FC<BlueprintsPageProps> = ({ onCreateExamClick }) => {
  const [subjects, setSubjects] = useState<Array<{ id: number; code: string; name: string }>>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number>(1);
  const [blueprints, setBlueprints] = useState<BlueprintItem[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [copySuccess, setCopySuccess] = useState<boolean>(false);

  useEffect(() => {
    fetchSubjects().then(subs => {
      setSubjects(subs);
      if (subs.length > 0) {
        setSelectedSubjectId(subs[0].id);
      }
    });
  }, []);

  useEffect(() => {
    if (!selectedSubjectId) return;
    let active = true;
    fetchBlueprints(selectedSubjectId)
      .then(bps => {
        if (!active) return;
        setBlueprints(bps);
        setLoading(false);
      });
    return () => { active = false; };
  }, [selectedSubjectId]);

  const currentBlueprint: BlueprintItem | undefined = blueprints.find(b => b.subjectId === selectedSubjectId) || blueprints[0];

  const handleCopyJson = () => {
    if (!currentBlueprint) return;
    navigator.clipboard.writeText(JSON.stringify(currentBlueprint, null, 2));
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  const getCognitiveLabel = (level?: string) => {
    switch (level) {
      case 'RECOGNITION': return { label: 'Nhận biết (NB)', color: 'bg-emerald-500/10 text-emerald-800 border-emerald-500/20' };
      case 'UNDERSTANDING': return { label: 'Thông hiểu (TH)', color: 'bg-blue-500/10 text-blue-800 border-blue-500/20' };
      case 'APPLICATION': return { label: 'Vận dụng (VD)', color: 'bg-amber-500/10 text-amber-800 border-amber-500/20' };
      case 'HIGH_APPLICATION': return { label: 'Vận dụng cao (VDC)', color: 'bg-rose-500/10 text-rose-800 border-rose-500/20' };
      default: return { label: level || 'Thông hiểu', color: 'bg-surface-container-high text-on-surface' };
    }
  };

  const getQuestionTypeLabel = (type: string) => {
    switch (type) {
      case 'TRUE_FALSE': return { name: 'Đúng / Sai', detail: '4 ý độc lập (lũy tiến 1-2-3-6đ)' };
      case 'SINGLE_CHOICE': return { name: 'Trắc nghiệm 4 lựa chọn (MCQ)', detail: '1 đáp án đúng (6đ/câu)' };
      case 'MATCHING': return { name: 'Ghép hợp (Matching)', detail: '4 cặp tương ứng (6đ/câu)' };
      case 'SHORT_ANSWER': return { name: 'Trả lời ngắn / Điền số', detail: 'Điền số thực hoặc chuỗi (6đ/câu)' };
      default: return { name: type, detail: '' };
    }
  };

  return (
    <div className="p-margin max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/30 shadow-sm">
        <div>
          <h1 className="font-headline-lg text-on-surface flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[32px]">grid_view</span>
            Khung Ma trận Khảo thí V-SAT 2026
          </h1>
          <p className="text-body-md text-outline mt-1">
            Quy chuẩn cấu trúc 25 câu hỏi × Thang điểm 150 thô theo chương trình GDPT 2018 Bộ GD&ĐT.
          </p>
        </div>

        <div className="flex gap-2">
          <button
            onClick={handleCopyJson}
            disabled={!currentBlueprint}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-surface-container-high hover:bg-surface-container-highest text-on-surface rounded-xl font-label-md transition border border-outline-variant/20"
          >
            <span className="material-symbols-outlined text-[18px]">
              {copySuccess ? 'check' : 'content_copy'}
            </span>
            {copySuccess ? 'Đã sao chép' : 'Sao chép JSON'}
          </button>

          {onCreateExamClick && (
            <button
              onClick={() => onCreateExamClick(currentBlueprint)}
              className="flex items-center gap-2 px-5 py-2 bg-primary text-on-primary hover:bg-primary/90 rounded-xl font-label-md font-semibold transition shadow-sm"
            >
              <span className="material-symbols-outlined text-[18px]">add_circle</span>
              Soạn đề mới theo ma trận này
            </button>
          )}
        </div>
      </div>

      {/* Subject Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {subjects.map(sub => {
          const isSelected = sub.id === selectedSubjectId;
          return (
            <button
              key={sub.id}
              onClick={() => setSelectedSubjectId(sub.id)}
              className={`px-4 py-2.5 rounded-xl font-label-md font-medium whitespace-nowrap transition-all border ${
                isSelected
                  ? 'bg-primary text-on-primary border-primary shadow-sm'
                  : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-high border-outline-variant/30'
              }`}
            >
              {sub.name} ({sub.code})
            </button>
          );
        })}
      </div>

      {loading ? (
        <div className="p-12 text-center text-outline bg-surface-container-lowest rounded-2xl border border-outline-variant/30">
          <span className="material-symbols-outlined text-[36px] animate-spin text-primary">progress_activity</span>
          <div className="mt-2 text-body-md">Đang tải ma trận môn thi...</div>
        </div>
      ) : currentBlueprint ? (
        <div className="space-y-6">
          {/* Top KPI Summary Grid */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30 shadow-sm">
              <span className="font-label-sm uppercase text-outline">Tổng số câu hỏi</span>
              <div className="font-headline-lg font-bold text-on-surface mt-1 flex items-baseline gap-1">
                {currentBlueprint.totalQuestions}
                <span className="text-body-sm font-normal text-outline">câu</span>
              </div>
            </div>

            <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30 shadow-sm">
              <span className="font-label-sm uppercase text-outline">Thang điểm tối đa</span>
              <div className="font-headline-lg font-bold text-primary mt-1 flex items-baseline gap-1">
                {currentBlueprint.maxRawScore}
                <span className="text-body-sm font-normal text-outline">điểm thô</span>
              </div>
            </div>

            <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30 shadow-sm">
              <span className="font-label-sm uppercase text-outline">Thời gian làm bài</span>
              <div className="font-headline-lg font-bold text-emerald-700 mt-1 flex items-baseline gap-1">
                {currentBlueprint.subjectCode === 'MATH' ? '90' : '60'}
                <span className="text-body-sm font-normal text-outline">phút</span>
              </div>
            </div>

            <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30 shadow-sm">
              <span className="font-label-sm uppercase text-outline">Mã định danh khung</span>
              <div className="font-mono text-[13px] font-bold text-on-surface mt-2 truncate">
                {currentBlueprint.code}
              </div>
            </div>
          </div>

          {/* Visual Percentage Distribution Bar */}
          <div className="bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/30 space-y-4 shadow-sm">
            <div className="flex justify-between items-center">
              <h2 className="font-headline-sm text-on-surface">Tỷ trọng điểm theo Dạng thức (4 Phần cấu trúc)</h2>
              <span className="text-label-sm text-outline">Tổng: 100% (150 điểm)</span>
            </div>

            {/* Segmented Bar */}
            <div className="h-6 w-full rounded-xl overflow-hidden flex shadow-inner border border-outline-variant/20">
              <div className="bg-sky-500 h-full flex items-center justify-center text-white font-mono text-[11px] font-bold" style={{ width: '36%' }}>
                Phần I (36%)
              </div>
              <div className="bg-emerald-500 h-full flex items-center justify-center text-white font-mono text-[11px] font-bold" style={{ width: '24%' }}>
                Phần II (24%)
              </div>
              <div className="bg-amber-500 h-full flex items-center justify-center text-white font-mono text-[11px] font-bold" style={{ width: '20%' }}>
                Phần III (20%)
              </div>
              <div className="bg-purple-500 h-full flex items-center justify-center text-white font-mono text-[11px] font-bold" style={{ width: '20%' }}>
                Phần IV (20%)
              </div>
            </div>

            {/* Legend */}
            <div className="grid grid-cols-2 md:grid-cols-4 gap-3 text-body-sm pt-1">
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded bg-sky-500 flex-shrink-0"></span>
                <span className="text-on-surface-variant font-medium">Phần I (Đúng/Sai): 9 câu (54đ)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded bg-emerald-500 flex-shrink-0"></span>
                <span className="text-on-surface-variant font-medium">Phần II (MCQ): 6 câu (36đ)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded bg-amber-500 flex-shrink-0"></span>
                <span className="text-on-surface-variant font-medium">Phần III (Ghép): 5 câu (30đ)</span>
              </div>
              <div className="flex items-center gap-2">
                <span className="w-3.5 h-3.5 rounded bg-purple-500 flex-shrink-0"></span>
                <span className="text-on-surface-variant font-medium">Phần IV (Điền): 5 câu (30đ)</span>
              </div>
            </div>
          </div>

          {/* Matrix Detail Table */}
          <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 overflow-hidden shadow-sm">
            <div className="p-4 border-b border-outline-variant/20 bg-surface-container-low/40 flex justify-between items-center">
              <div>
                <h3 className="font-headline-sm text-on-surface">Chi tiết Ma trận 3 chiều V-SAT</h3>
                <p className="text-body-sm text-outline">Phân bổ 25 câu hỏi theo Dạng thức × Mức độ nhận thức × Điểm số</p>
              </div>
              <span className="font-mono text-[12px] px-2.5 py-1 rounded-full bg-primary-container text-on-primary-container font-semibold">
                Phiên bản {currentBlueprint.version}.0
              </span>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-body-sm border-collapse">
                <thead>
                  <tr className="border-b border-outline-variant/20 bg-surface-container-low font-label-md text-on-surface-variant">
                    <th className="p-3.5 w-12 text-center">STT</th>
                    <th className="p-3.5 min-w-[220px]">Phần thi & Tên cấu trúc</th>
                    <th className="p-3.5 w-36">Dạng thức</th>
                    <th className="p-3.5 w-44">Mức nhận thức</th>
                    <th className="p-3.5 w-28 text-center">Độ khó</th>
                    <th className="p-3.5 w-28 text-center">Số câu</th>
                    <th className="p-3.5 w-28 text-center">Điểm tối đa</th>
                    <th className="p-3.5 w-28 text-center">Tỷ trọng</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/10">
                  {currentBlueprint.cells.map((cell, idx) => {
                    const cog = getCognitiveLabel(cell.cognitiveLevel);
                    const qType = getQuestionTypeLabel(cell.questionType);
                    const maxScore = idx === 0 ? 54 : (idx === 1 ? 36 : 30);
                    const weightPct = ((maxScore / 150) * 100).toFixed(1);

                    return (
                      <tr key={cell.id || idx} className="hover:bg-surface-container-low/40 transition-colors">
                        <td className="p-3.5 text-center font-mono text-outline text-[12px]">{idx + 1}</td>
                        <td className="p-3.5">
                          <div className="font-semibold text-on-surface">{cell.topicName}</div>
                          <div className="text-[12px] text-outline mt-0.5">{qType.detail}</div>
                        </td>
                        <td className="p-3.5">
                          <span className="font-mono text-[11px] px-2 py-0.5 rounded bg-surface-container-high text-on-surface font-medium">
                            {cell.questionType}
                          </span>
                        </td>
                        <td className="p-3.5">
                          <span className={`text-[11px] font-bold px-2.5 py-1 rounded-full border ${cog.color}`}>
                            {cog.label}
                          </span>
                        </td>
                        <td className="p-3.5 text-center">
                          <span className="font-mono text-[11px] text-outline">
                            {cell.difficulty || 'MEDIUM'}
                          </span>
                        </td>
                        <td className="p-3.5 text-center font-bold text-on-surface">
                          {cell.questionCount}
                        </td>
                        <td className="p-3.5 text-center font-bold text-primary">
                          {maxScore}.0
                        </td>
                        <td className="p-3.5 text-center font-mono text-outline">
                          {weightPct}%
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
                <tfoot>
                  <tr className="bg-surface-container-low/80 font-bold border-t border-outline-variant/30 text-on-surface">
                    <td colSpan={5} className="p-3.5 text-right uppercase font-label-md text-outline">
                      Tổng cộng quy chuẩn V-SAT:
                    </td>
                    <td className="p-3.5 text-center text-primary text-[15px]">25 câu</td>
                    <td className="p-3.5 text-center text-primary text-[15px]">150.0 đ</td>
                    <td className="p-3.5 text-center text-primary text-[15px]">100%</td>
                  </tr>
                </tfoot>
              </table>
            </div>
          </div>
        </div>
      ) : (
        <div className="p-12 text-center text-outline bg-surface-container-lowest rounded-2xl border border-outline-variant/30">
          Chưa có ma trận cho môn học này.
        </div>
      )}
    </div>
  );
};
