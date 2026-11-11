import React, { useState, useEffect } from 'react';
import type { BlueprintItem, BlueprintCellItem, QuestionType, CognitiveLevel } from '../../types/exam';
import { fetchBlueprints, fetchSubjects, createBlueprint, updateBlueprint, deleteBlueprint } from '../../services/api';

interface BlueprintsPageProps {
  onCreateExamClick?: (blueprint?: BlueprintItem) => void;
}

export const BlueprintsPage: React.FC<BlueprintsPageProps> = ({ onCreateExamClick }) => {
  const [subjects, setSubjects] = useState<Array<{ id: number; code: string; name: string }>>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number>(1);
  const [blueprints, setBlueprints] = useState<BlueprintItem[]>([]);
  const [selectedBlueprintId, setSelectedBlueprintId] = useState<number | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [copySuccess, setCopySuccess] = useState<boolean>(false);

  // Modal Create/Edit State
  const [isModalOpen, setIsModalOpen] = useState<boolean>(false);
  const [modalMode, setModalMode] = useState<'create' | 'edit'>('create');
  const [editingBlueprint, setEditingBlueprint] = useState<Partial<BlueprintItem>>({});
  const [savingModal, setSavingModal] = useState<boolean>(false);
  const [modalError, setModalError] = useState<string>('');

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
        setSelectedBlueprintId(bps[0]?.id ?? null);
        setLoading(false);
      });
    return () => { active = false; };
  }, [selectedSubjectId]);

  const currentBlueprint: BlueprintItem | undefined =
    blueprints.find(b => b.id === selectedBlueprintId) ||
    blueprints.find(b => b.subjectId === selectedSubjectId) ||
    blueprints[0];

  const handleCopyJson = () => {
    if (!currentBlueprint) return;
    navigator.clipboard.writeText(JSON.stringify(currentBlueprint, null, 2));
    setCopySuccess(true);
    setTimeout(() => setCopySuccess(false), 2000);
  };

  const handleOpenCreateModal = () => {
    setModalMode('create');
    setModalError('');
    const currentSub = subjects.find(s => s.id === selectedSubjectId);
    const defaultCells: BlueprintCellItem[] = currentBlueprint?.cells
      ? JSON.parse(JSON.stringify(currentBlueprint.cells))
      : [
          { id: 1, topicId: 1, topicName: 'Phần I: Câu hỏi Đúng / Sai', questionType: 'TRUE_FALSE', cognitiveLevel: 'UNDERSTANDING', difficulty: 'MEDIUM', questionCount: 9 },
          { id: 2, topicId: 2, topicName: 'Phần II: Trắc nghiệm 4 lựa chọn (MCQ)', questionType: 'SINGLE_CHOICE', cognitiveLevel: 'APPLICATION', difficulty: 'MEDIUM', questionCount: 6 },
          { id: 3, topicId: 3, topicName: 'Phần III: Ghép hợp (Matching)', questionType: 'MATCHING', cognitiveLevel: 'APPLICATION', difficulty: 'HARD', questionCount: 5 },
          { id: 4, topicId: 4, topicName: 'Phần IV: Trả lời ngắn / Điền số', questionType: 'SHORT_ANSWER', cognitiveLevel: 'HIGH_APPLICATION', difficulty: 'HARD', questionCount: 5 }
        ];
    setEditingBlueprint({
      code: `BP_${currentSub?.code || 'SUB'}_${Math.floor(100 + Math.random() * 900)}`,
      name: `Ma trận ${currentSub?.name || 'V-SAT'} tùy chỉnh`,
      subjectId: selectedSubjectId,
      subjectCode: currentSub?.code,
      subjectName: currentSub?.name,
      totalQuestions: defaultCells.reduce((s, c) => s + c.questionCount, 0),
      maxRawScore: 150,
      version: 1,
      cells: defaultCells
    });
    setIsModalOpen(true);
  };

  const handleOpenEditModal = () => {
    if (!currentBlueprint) return;
    setModalMode('edit');
    setModalError('');
    setEditingBlueprint(JSON.parse(JSON.stringify(currentBlueprint)));
    setIsModalOpen(true);
  };

  const handleCellChange = (idx: number, field: keyof BlueprintCellItem, value: string | number) => {
    setEditingBlueprint(prev => {
      const cells = [...(prev.cells || [])];
      if (!cells[idx]) return prev;
      cells[idx] = { ...cells[idx], [field]: value };
      const totalQ = cells.reduce((sum, c) => sum + (Number(c.questionCount) || 0), 0);
      return { ...prev, cells, totalQuestions: totalQ };
    });
  };

  const handleSaveModal = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingBlueprint.code?.trim() || !editingBlueprint.name?.trim()) {
      setModalError('Vui lòng nhập đầy đủ Mã ma trận và Tên ma trận.');
      return;
    }
    setSavingModal(true);
    setModalError('');
    try {
      const cells = editingBlueprint.cells || [];
      const totalQ = cells.reduce((sum, c) => sum + (Number(c.questionCount) || 0), 0);
      const selectedSub = subjects.find(s => s.id === editingBlueprint.subjectId);
      const payload: Partial<BlueprintItem> = {
        ...editingBlueprint,
        subjectCode: selectedSub?.code || editingBlueprint.subjectCode,
        subjectName: selectedSub?.name || editingBlueprint.subjectName,
        totalQuestions: totalQ,
        maxRawScore: Number(editingBlueprint.maxRawScore) || 150
      };

      if (modalMode === 'create') {
        const created = await createBlueprint(payload);
        setBlueprints(prev => [...prev, created]);
        if (created.subjectId === selectedSubjectId) {
          setSelectedBlueprintId(created.id);
        } else if (created.subjectId) {
          setSelectedSubjectId(created.subjectId);
          setSelectedBlueprintId(created.id);
        }
      } else if (editingBlueprint.id) {
        const updated = await updateBlueprint(editingBlueprint.id, payload);
        setBlueprints(prev => prev.map(b => (b.id === updated.id ? updated : b)));
        setSelectedBlueprintId(updated.id);
      }
      setIsModalOpen(false);
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Lỗi khi lưu ma trận đề thi');
    } finally {
      setSavingModal(false);
    }
  };

  const handleDeleteBlueprint = async () => {
    if (!editingBlueprint.id) return;
    setSavingModal(true);
    setModalError('');
    try {
      await deleteBlueprint(editingBlueprint.id);
      setBlueprints(prev => {
        const remaining = prev.filter(b => b.id !== editingBlueprint.id);
        setSelectedBlueprintId(remaining[0]?.id ?? null);
        return remaining;
      });
      setIsModalOpen(false);
    } catch (err: unknown) {
      setModalError(err instanceof Error ? err.message : 'Không thể xóa ma trận đề thi');
    } finally {
      setSavingModal(false);
    }
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

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={handleCopyJson}
            disabled={!currentBlueprint}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-surface-container-high hover:bg-surface-container-highest text-on-surface rounded-xl font-label-md transition border border-outline-variant/20 disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-[18px]">
              {copySuccess ? 'check' : 'content_copy'}
            </span>
            {copySuccess ? 'Đã sao chép' : 'Sao chép JSON'}
          </button>

          <button
            onClick={() => onCreateExamClick?.(currentBlueprint)}
            disabled={!currentBlueprint}
            className="flex items-center gap-2 px-4 py-2 bg-primary text-on-primary hover:bg-primary/90 rounded-xl font-label-md font-semibold transition shadow-sm disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-[18px]">add_circle</span>
            Soạn đề mới theo ma trận này
          </button>

          <button
            onClick={handleOpenCreateModal}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-label-md font-semibold transition shadow-sm"
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            Tạo ma trận mới
          </button>

          <button
            onClick={handleOpenEditModal}
            disabled={!currentBlueprint}
            className="flex items-center gap-1.5 px-3.5 py-2 bg-surface-container-high hover:bg-surface-container-highest text-on-surface rounded-xl font-label-md font-semibold transition border border-outline-variant/20 disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-[18px]">edit</span>
            Chỉnh sửa ma trận
          </button>
        </div>
      </div>

      {/* Subject Filter Tabs */}
      <div className="flex items-center gap-2 overflow-x-auto pb-1">
        {subjects.map(sub => {
          const isSelected = sub.id === selectedSubjectId;
          return (
            <button
              key={sub.id}
              onClick={() => {
                setSelectedSubjectId(sub.id);
                const subBps = blueprints.filter(b => b.subjectId === sub.id);
                if (subBps.length > 0) {
                  setSelectedBlueprintId(subBps[0].id);
                }
              }}
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

      {/* Blueprint Variant Switcher if multiple blueprints exist for this subject */}
      {blueprints.filter(b => b.subjectId === selectedSubjectId).length > 1 && (
        <div className="flex items-center gap-2 overflow-x-auto bg-surface-container-lowest p-3 rounded-xl border border-outline-variant/30">
          <span className="text-xs font-bold text-outline uppercase tracking-wider pl-1">Phiên bản ma trận:</span>
          {blueprints
            .filter(b => b.subjectId === selectedSubjectId)
            .map(bp => (
              <button
                key={bp.id}
                onClick={() => setSelectedBlueprintId(bp.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-semibold border transition-all ${
                  bp.id === currentBlueprint?.id
                    ? 'bg-primary/10 border-primary text-primary font-bold'
                    : 'bg-surface-container-lowest border-outline-variant/40 text-outline hover:bg-surface-container-high'
                }`}
              >
                {bp.name} ({bp.code})
              </button>
            ))}
        </div>
      )}

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

      {/* Blueprint Create / Edit Modal (rendered only when isModalOpen === true) */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4">
          <div className="bg-surface rounded-2xl border border-outline-variant/30 shadow-2xl max-w-3xl w-full max-h-[90vh] flex flex-col overflow-hidden animate-fade-in">
            {/* Modal Header */}
            <div className="p-5 border-b border-outline-variant/20 flex items-center justify-between bg-surface-container-low/40">
              <h2 className="text-xl font-bold text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary text-[24px]">
                  {modalMode === 'create' ? 'add_box' : 'edit_note'}
                </span>
                {modalMode === 'create' ? 'Tạo Ma trận đề thi mới' : 'Chỉnh sửa Ma trận đề thi'}
              </h2>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="w-8 h-8 rounded-lg flex items-center justify-center hover:bg-surface-container-high text-outline transition-colors"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            {/* Modal Form */}
            <form onSubmit={handleSaveModal} className="flex-1 overflow-y-auto p-6 space-y-5">
              {modalError && (
                <div className="p-3.5 rounded-xl bg-error/10 border border-error/30 text-error text-xs font-semibold">
                  {modalError}
                </div>
              )}

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-on-surface">Mã ma trận (Code)</label>
                  <input
                    type="text"
                    value={editingBlueprint.code || ''}
                    onChange={e => setEditingBlueprint(prev => ({ ...prev, code: e.target.value }))}
                    placeholder="VD: BP_MATH_2026"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-outline-variant/50 bg-surface-container-lowest text-on-surface text-sm focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all font-mono"
                    required
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-on-surface">Môn học</label>
                  <select
                    value={editingBlueprint.subjectId || selectedSubjectId}
                    onChange={e => {
                      const newSubId = Number(e.target.value);
                      const sub = subjects.find(s => s.id === newSubId);
                      setEditingBlueprint(prev => ({
                        ...prev,
                        subjectId: newSubId,
                        subjectCode: sub?.code,
                        subjectName: sub?.name
                      }));
                    }}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-outline-variant/50 bg-surface-container-lowest text-on-surface text-sm focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                  >
                    {subjects.map(sub => (
                      <option key={sub.id} value={sub.id}>
                        {sub.name} ({sub.code})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div className="space-y-1.5">
                <label className="text-xs font-bold text-on-surface">Tên ma trận đề thi</label>
                <input
                  type="text"
                  value={editingBlueprint.name || ''}
                  onChange={e => setEditingBlueprint(prev => ({ ...prev, name: e.target.value }))}
                  placeholder="VD: Ma trận khảo thí V-SAT Toán học chuẩn GDPT 2018"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-outline-variant/50 bg-surface-container-lowest text-on-surface text-sm focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                  required
                />
              </div>

              <div className="grid grid-cols-2 sm:grid-cols-3 gap-4">
                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-on-surface">Thang điểm tối đa</label>
                  <input
                    type="number"
                    value={editingBlueprint.maxRawScore ?? 150}
                    onChange={e => setEditingBlueprint(prev => ({ ...prev, maxRawScore: Number(e.target.value) }))}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-outline-variant/50 bg-surface-container-lowest text-on-surface text-sm focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                  />
                </div>

                <div className="space-y-1.5">
                  <label className="text-xs font-bold text-on-surface">Tổng số câu hỏi</label>
                  <input
                    type="number"
                    disabled
                    value={editingBlueprint.cells?.reduce((s, c) => s + (Number(c.questionCount) || 0), 0) || 0}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-outline-variant/30 bg-surface-container-low text-outline text-sm font-bold"
                  />
                </div>

                <div className="space-y-1.5 col-span-2 sm:col-span-1">
                  <label className="text-xs font-bold text-on-surface">Phiên bản</label>
                  <input
                    type="number"
                    value={editingBlueprint.version ?? 1}
                    onChange={e => setEditingBlueprint(prev => ({ ...prev, version: Number(e.target.value) }))}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-outline-variant/50 bg-surface-container-lowest text-on-surface text-sm focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                  />
                </div>
              </div>

              {/* Matrix Cells Configuration */}
              <div className="space-y-3 pt-2">
                <div className="flex items-center justify-between">
                  <label className="text-xs font-bold text-on-surface uppercase tracking-wide">
                    Cấu trúc các phần thi (Cells ma trận)
                  </label>
                  <span className="text-[11px] text-outline">Quy chuẩn 4 phần theo GDPT 2018</span>
                </div>

                <div className="space-y-3">
                  {(editingBlueprint.cells || []).map((cell, idx) => (
                    <div
                      key={cell.id || idx}
                      className="p-3.5 rounded-xl border border-outline-variant/30 bg-surface-container-lowest space-y-2.5"
                    >
                      <div className="grid grid-cols-1 sm:grid-cols-12 gap-2.5 items-center">
                        <div className="sm:col-span-4">
                          <input
                            type="text"
                            value={cell.topicName}
                            onChange={e => handleCellChange(idx, 'topicName', e.target.value)}
                            placeholder="Tên phần thi / chủ đề"
                            className="w-full px-2.5 py-1.5 rounded-lg border border-outline-variant/40 text-xs font-semibold text-on-surface bg-surface-container-low"
                          />
                        </div>

                        <div className="sm:col-span-3">
                          <select
                            value={cell.questionType}
                            onChange={e => handleCellChange(idx, 'questionType', e.target.value as QuestionType)}
                            className="w-full px-2 py-1.5 rounded-lg border border-outline-variant/40 text-xs text-on-surface bg-surface-container-low font-mono"
                          >
                            <option value="TRUE_FALSE">TRUE_FALSE (Đ/S)</option>
                            <option value="SINGLE_CHOICE">SINGLE_CHOICE (MCQ)</option>
                            <option value="MATCHING">MATCHING (Ghép)</option>
                            <option value="SHORT_ANSWER">SHORT_ANSWER (Điền)</option>
                          </select>
                        </div>

                        <div className="sm:col-span-3">
                          <select
                            value={cell.cognitiveLevel}
                            onChange={e => handleCellChange(idx, 'cognitiveLevel', e.target.value as CognitiveLevel)}
                            className="w-full px-2 py-1.5 rounded-lg border border-outline-variant/40 text-xs text-on-surface bg-surface-container-low"
                          >
                            <option value="RECOGNITION">Nhận biết</option>
                            <option value="UNDERSTANDING">Thông hiểu</option>
                            <option value="APPLICATION">Vận dụng</option>
                            <option value="HIGH_APPLICATION">Vận dụng cao</option>
                          </select>
                        </div>

                        <div className="sm:col-span-2 flex items-center gap-1">
                          <input
                            type="number"
                            min="0"
                            max="50"
                            value={cell.questionCount}
                            onChange={e => handleCellChange(idx, 'questionCount', Number(e.target.value))}
                            className="w-full px-2 py-1.5 rounded-lg border border-outline-variant/40 text-xs text-center font-bold text-primary bg-surface-container-low"
                          />
                          <span className="text-[11px] text-outline">câu</span>
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              {/* Modal Footer Controls */}
              <div className="pt-4 border-t border-outline-variant/20 flex flex-col sm:flex-row items-center justify-between gap-3">
                {modalMode === 'edit' && editingBlueprint.id ? (
                  <button
                    type="button"
                    onClick={handleDeleteBlueprint}
                    disabled={savingModal}
                    className="w-full sm:w-auto px-4 py-2 rounded-xl border border-error/40 text-error hover:bg-error/10 text-xs font-bold transition-all flex items-center justify-center gap-1.5 disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-[16px]">delete</span>
                    Xóa ma trận này
                  </button>
                ) : (
                  <div />
                )}

                <div className="flex items-center gap-2.5 w-full sm:w-auto justify-end">
                  <button
                    type="button"
                    onClick={() => setIsModalOpen(false)}
                    disabled={savingModal}
                    className="px-4 py-2 rounded-xl border border-outline-variant/40 text-outline hover:bg-surface-container-high text-xs font-bold transition-all"
                  >
                    Hủy bỏ
                  </button>
                  <button
                    type="submit"
                    disabled={savingModal}
                    className="px-5 py-2 rounded-xl bg-primary text-on-primary hover:bg-primary/90 text-xs font-bold transition-all shadow-sm flex items-center gap-1.5 disabled:opacity-50"
                  >
                    {savingModal ? (
                      <span className="material-symbols-outlined animate-spin text-[16px]">sync</span>
                    ) : (
                      <span className="material-symbols-outlined text-[16px]">save</span>
                    )}
                    <span>{modalMode === 'create' ? 'Tạo ma trận' : 'Lưu thay đổi'}</span>
                  </button>
                </div>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
