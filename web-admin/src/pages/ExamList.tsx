import React, { useState, useEffect } from 'react';
import type { ExamItem } from '../types/exam';
import { fetchExams, discardDraftExam } from '../services/api';

interface ExamListProps {
  onCreateClick: () => void;
}

export const ExamList: React.FC<ExamListProps> = ({ onCreateClick }) => {
  const [exams, setExams] = useState<ExamItem[]>([]);
  const [search, setSearch] = useState('');
  const [subjectFilter, setSubjectFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');

  const loadExams = async () => {
    const subjectId = subjectFilter === 'MATH' ? 1 : (subjectFilter === 'ENG' ? 2 : (subjectFilter === 'PHY' ? 3 : undefined));
    const status = statusFilter !== 'ALL' ? statusFilter : undefined;
    const data = await fetchExams({ status, subjectId });
    setExams(data);
  };

  useEffect(() => {
    let ignore = false;
    const load = async () => {
      const subjectId = subjectFilter === 'MATH' ? 1 : (subjectFilter === 'ENG' ? 2 : (subjectFilter === 'PHY' ? 3 : undefined));
      const status = statusFilter !== 'ALL' ? statusFilter : undefined;
      const data = await fetchExams({ status, subjectId });
      if (!ignore) setExams(data);
    };
    load();
    return () => {
      ignore = true;
    };
  }, [subjectFilter, statusFilter]);

  const handleDelete = async (id: number) => {
    if (confirm('Bạn có chắc muốn xóa bản nháp đề thi này?')) {
      await discardDraftExam(id);
      loadExams();
    }
  };

  const filteredExams = exams.filter(ex => {
    const matchesSearch = !search || ex.title.toLowerCase().includes(search.toLowerCase()) ||
                          ex.examCode.toLowerCase().includes(search.toLowerCase());
    return matchesSearch;
  });

  return (
    <div className="p-margin space-y-space-lg">
      {/* PAGE HEADER */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md">
        <div className="space-y-1">
          <div className="flex items-center gap-space-sm">
            <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight">
              Quản lý đề thi
            </h1>
            <span className="px-2 py-0.5 rounded-full font-label-sm text-label-sm bg-secondary-fixed text-on-secondary-fixed-variant">
              Quy chuẩn V-SAT 2026
            </span>
          </div>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Quản lý, thẩm định ma trận cấu trúc 25 câu (150 điểm) và xuất bản đề thi V-SAT cho 7 môn học
          </p>
        </div>

        {/* Action buttons */}
        <div className="flex items-center gap-space-sm flex-wrap">
          <button
            onClick={loadExams}
            className="h-9 px-space-md rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-colors"
            type="button"
          >
            <span className="material-symbols-outlined text-[18px] text-outline">refresh</span>
            <span>Làm mới</span>
          </button>
          <button
            onClick={onCreateClick}
            className="h-9 px-space-md rounded-lg bg-primary hover:bg-tertiary text-on-primary font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-colors"
            type="button"
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            <span>Tạo đề thi mới</span>
          </button>
        </div>
      </div>

      {/* FILTER BAR */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-space-md bg-surface-container-lowest p-space-md rounded-xl border border-outline-variant/30">
        <div className="flex items-center gap-space-sm w-full md:w-auto">
          <div className="relative flex-1 md:w-80">
            <span className="material-symbols-outlined absolute left-3 top-2.5 text-[18px] text-outline">
              search
            </span>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo mã đề, tên đề..."
              className="w-full h-9 pl-9 pr-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
            />
          </div>

          <select
            value={subjectFilter}
            onChange={(e) => setSubjectFilter(e.target.value)}
            className="h-9 px-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
          >
            <option value="ALL">Tất cả môn học</option>
            <option value="MATH">Toán học</option>
            <option value="ENG">Tiếng Anh</option>
            <option value="PHY">Vật lí</option>
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-9 px-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="DRAFT">Bản nháp (Draft)</option>
            <option value="PENDING_REVIEW">Chờ duyệt (Review)</option>
            <option value="PUBLISHED">Đã xuất bản (Published)</option>
            <option value="HIDDEN">Đang ẩn (Hidden)</option>
          </select>
        </div>

        <div className="text-body-sm text-outline font-medium self-end md:self-center">
          Tổng cộng: {filteredExams.length} đề thi
        </div>
      </div>

      {/* EXAMS GRID / LIST */}
      <div className="grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-space-md">
        {filteredExams.map((ex) => (
          <div
            key={ex.id}
            className="bg-surface-container-lowest p-space-md rounded-xl border border-outline-variant/30 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between gap-space-md"
          >
            <div className="space-y-space-sm">
              <div className="flex items-center justify-between">
                <span className="font-mono text-[11px] font-bold text-outline">
                  {ex.examCode}
                </span>
                <span
                  className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                    ex.status === 'PUBLISHED'
                      ? 'bg-emerald-100 text-emerald-800'
                      : ex.status === 'DRAFT'
                      ? 'bg-amber-100 text-amber-800'
                      : ex.status === 'PENDING_REVIEW'
                      ? 'bg-indigo-100 text-indigo-800'
                      : 'bg-surface-container-high text-on-surface-variant'
                  }`}
                >
                  {ex.status}
                </span>
              </div>

              <h3 className="font-headline-sm text-on-surface line-clamp-2">
                {ex.title}
              </h3>

              <div className="flex items-center gap-space-sm text-[12px] text-outline">
                <span className="font-semibold text-primary">{ex.subjectName}</span>
                <span>•</span>
                <span>{ex.questionCount || 25} câu</span>
                <span>•</span>
                <span>{ex.durationMinutes} phút</span>
                <span>•</span>
                <span>Độ khó: {ex.difficulty}</span>
              </div>
            </div>

            <div className="flex items-center justify-between pt-space-sm border-t border-outline-variant/20">
              <span className="text-[11px] text-outline">
                Cập nhật: {ex.updatedAt || '06/10/2026'}
              </span>

              <div className="flex items-center gap-1">
                {ex.status === 'DRAFT' && (
                  <button
                    onClick={() => handleDelete(ex.id)}
                    className="p-1 rounded text-red-600 hover:bg-red-50 text-[11px] font-semibold"
                    title="Hủy bản nháp"
                  >
                    Xóa nháp
                  </button>
                )}
                <button
                  onClick={onCreateClick}
                  className="px-2.5 py-1 rounded bg-surface-container hover:bg-surface-container-high text-primary text-[11px] font-bold transition-colors"
                >
                  Chi tiết
                </button>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};
