import React, { useState, useEffect } from 'react';
import type { FullQuestionItem } from '../../types/exam';
import {
  fetchBankQuestions,
  approveBankQuestion,
  rejectBankQuestion
} from '../../services/api';
import { MathView } from '../../components/MathView';
import { QuestionCreateModal } from './QuestionCreateModal';

export const QuestionBank: React.FC = () => {
  const [questions, setQuestions] = useState<FullQuestionItem[]>([]);
  const [search, setSearch] = useState('');
  const [subjectFilter, setSubjectFilter] = useState('ALL');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [statusFilter, setStatusFilter] = useState('ALL');
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(false);

  const loadData = async () => {
    setLoading(true);
    try {
      const subjectId = subjectFilter === 'MATH' ? 1 : (subjectFilter === 'ENG' ? 2 : (subjectFilter === 'PHY' ? 3 : (subjectFilter === 'CHEM' ? 4 : undefined)));
      const data = await fetchBankQuestions({
        status: statusFilter,
        subjectId,
        questionType: typeFilter,
        q: search
      });
      setQuestions(data);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    let ignore = false;
    const load = async () => {
      const subjectId = subjectFilter === 'MATH' ? 1 : (subjectFilter === 'ENG' ? 2 : (subjectFilter === 'PHY' ? 3 : (subjectFilter === 'CHEM' ? 4 : undefined)));
      const data = await fetchBankQuestions({
        status: statusFilter,
        subjectId,
        questionType: typeFilter,
        q: search
      });
      if (!ignore) setQuestions(data);
    };
    load();
    return () => {
      ignore = true;
    };
  }, [subjectFilter, typeFilter, statusFilter, search]);

  const handleApprove = async (id: number) => {
    await approveBankQuestion(id);
    setQuestions(prev => prev.map(q => q.id === id ? { ...q, status: 'APPROVED' } : q));
  };

  const handleReject = async (id: number) => {
    await rejectBankQuestion(id);
    setQuestions(prev => prev.map(q => q.id === id ? { ...q, status: 'ARCHIVED' } : q));
  };

  const handleCreated = (newQ: FullQuestionItem) => {
    setQuestions(prev => [newQ, ...prev]);
  };

  const filtered = questions.filter(q => {
    const matchesSearch = !search ||
      q.questionCode.toLowerCase().includes(search.toLowerCase()) ||
      q.questionText.toLowerCase().includes(search.toLowerCase());
    const matchesType = typeFilter === 'ALL' || q.questionType === typeFilter;
    const matchesStatus = statusFilter === 'ALL' || q.status === statusFilter;
    return matchesSearch && matchesType && matchesStatus;
  });

  return (
    <div className="p-margin space-y-space-lg">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md">
        <div className="space-y-1">
          <div className="flex items-center gap-space-sm">
            <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight">
              Ngân hàng câu hỏi V-SAT 2026
            </h1>
            <span className="px-2 py-0.5 rounded-full font-label-sm text-label-sm bg-secondary-fixed text-on-secondary-fixed-variant">
              Tích hợp KaTeX Math Engine
            </span>
          </div>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Quản trị kho câu hỏi 4 dạng thức chuẩn khảo thí V-SAT: Đúng/Sai, Trắc nghiệm 4 lựa chọn, Ghép hợp, Điền số
          </p>
        </div>

        <div className="flex items-center gap-space-sm">
          <button
            onClick={loadData}
            className="h-9 px-space-md rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-colors"
          >
            <span className="material-symbols-outlined text-[18px] text-outline">refresh</span>
            <span>Làm mới</span>
          </button>
          <button
            onClick={() => setIsModalOpen(true)}
            className="h-9 px-space-md rounded-lg bg-primary hover:bg-tertiary text-on-primary font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-colors"
          >
            <span className="material-symbols-outlined text-[18px]">add</span>
            <span>Thêm câu hỏi mới</span>
          </button>
        </div>
      </div>

      {/* Filter Bar */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-space-md bg-surface-container-lowest p-space-md rounded-xl border border-outline-variant/30">
        <div className="flex items-center gap-space-sm w-full md:w-auto flex-wrap">
          <div className="relative flex-1 md:w-72">
            <span className="material-symbols-outlined absolute left-3 top-2.5 text-[18px] text-outline">
              search
            </span>
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Tìm theo mã câu, nội dung..."
              className="w-full h-9 pl-9 pr-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
            />
          </div>

          <select
            value={subjectFilter}
            onChange={(e) => setSubjectFilter(e.target.value)}
            className="h-9 px-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40"
          >
            <option value="ALL">Tất cả môn học</option>
            <option value="MATH">Toán học</option>
            <option value="PHY">Vật lí</option>
            <option value="CHEM">Hóa học</option>
            <option value="ENG">Tiếng Anh</option>
          </select>

          <select
            value={typeFilter}
            onChange={(e) => setTypeFilter(e.target.value)}
            className="h-9 px-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40"
          >
            <option value="ALL">Tất cả dạng câu hỏi</option>
            <option value="TRUE_FALSE">Phần 1: Đúng / Sai</option>
            <option value="SINGLE_CHOICE">Phần 2: MCQ 4 lựa chọn</option>
            <option value="MATCHING">Phần 3: Ghép hợp</option>
            <option value="SHORT_ANSWER">Phần 4: Điền số / Trả lời ngắn</option>
          </select>

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="h-9 px-3 text-body-sm bg-surface-container-low rounded-lg border border-outline-variant/40"
          >
            <option value="ALL">Tất cả trạng thái</option>
            <option value="APPROVED">Đã duyệt (APPROVED)</option>
            <option value="PENDING_REVIEW">Chờ duyệt (PENDING)</option>
            <option value="DRAFT">Bản nháp (DRAFT)</option>
          </select>
        </div>

        <div className="text-body-sm text-outline font-medium self-end md:self-center">
          {loading ? 'Đang tải...' : `Hiển thị: ${filtered.length} câu hỏi`}
        </div>
      </div>

      {/* Questions list */}
      <div className="space-y-4">
        {filtered.map((q) => {
          const typeBadgeColor =
            q.questionType === 'TRUE_FALSE' ? 'bg-cyan-100 text-cyan-800' :
            q.questionType === 'SINGLE_CHOICE' ? 'bg-indigo-100 text-indigo-800' :
            q.questionType === 'MATCHING' ? 'bg-amber-100 text-amber-800' :
            'bg-emerald-100 text-emerald-800';

          const typeName =
            q.questionType === 'TRUE_FALSE' ? 'Phần 1: Đúng / Sai' :
            q.questionType === 'SINGLE_CHOICE' ? 'Phần 2: MCQ 4 lựa chọn' :
            q.questionType === 'MATCHING' ? 'Phần 3: Ghép hợp' :
            'Phần 4: Điền số / Trả lời ngắn';

          return (
            <div
              key={q.id}
              className="bg-surface-container-lowest rounded-xl p-space-md border border-outline-variant/30 shadow-sm space-y-3 hover:border-primary/50 transition-all"
            >
              {/* Question card header */}
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-outline-variant/20 pb-2">
                <div className="flex items-center gap-2 flex-wrap">
                  <span className="font-mono text-[12px] font-bold text-primary">
                    {q.questionCode}
                  </span>
                  <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${typeBadgeColor}`}>
                    {typeName}
                  </span>
                  <span className="text-[12px] text-outline">
                    {q.subjectName || 'Toán học'} • {q.topicName || 'Chung'}
                  </span>
                  <span className="px-1.5 py-0.2 rounded bg-surface-container text-on-surface-variant text-[11px]">
                    {q.difficulty}
                  </span>
                  <span className="px-1.5 py-0.2 rounded bg-surface-container text-on-surface-variant text-[11px]">
                    {q.cognitiveLevel}
                  </span>
                </div>

                <div className="flex items-center gap-2">
                  <span className="font-mono text-[11px] font-bold text-emerald-700 bg-emerald-50 px-2 py-0.5 rounded">
                    Điểm: 6.0đ
                  </span>
                  <span
                    className={`px-2 py-0.5 rounded-full text-[10px] font-bold ${
                      q.status === 'APPROVED' ? 'bg-emerald-100 text-emerald-800' :
                      q.status === 'PENDING_REVIEW' ? 'bg-amber-100 text-amber-800' :
                      'bg-surface-container-high text-on-surface-variant'
                    }`}
                  >
                    {q.status}
                  </span>
                </div>
              </div>

              {/* Question stem (LaTeX rendered via MathView) */}
              <div className="text-body-md font-medium text-on-surface leading-relaxed">
                <MathView content={q.questionText} />
              </div>

              {/* Subitems rendered by format */}
              {q.questionType === 'TRUE_FALSE' && q.trueFalseItems && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2 pt-1">
                  {q.trueFalseItems.map((sub) => (
                    <div
                      key={sub.key}
                      className="p-2 rounded bg-surface-container-low border border-outline-variant/20 flex items-center justify-between text-body-sm gap-2"
                    >
                      <div className="flex items-center gap-1.5 min-w-0">
                        <span className="font-bold text-primary font-mono">{sub.key})</span>
                        <MathView content={sub.text} className="truncate" />
                      </div>
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold shrink-0 ${
                          sub.isCorrect ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'
                        }`}
                      >
                        {sub.isCorrect ? 'Đúng' : 'Sai'}
                      </span>
                    </div>
                  ))}
                </div>
              )}

              {q.questionType === 'SINGLE_CHOICE' && q.options && (
                <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-4 gap-2 pt-1 text-body-sm">
                  {q.options.map((opt) => (
                    <div
                      key={opt.label}
                      className={`p-2 rounded border ${
                        opt.isCorrect
                          ? 'bg-emerald-50 border-emerald-300 font-semibold text-emerald-900'
                          : 'bg-surface-container-low border-outline-variant/20'
                      }`}
                    >
                      <span className="font-mono font-bold mr-1">{opt.label}.</span>
                      <MathView content={opt.text} />
                    </div>
                  ))}
                </div>
              )}

              {q.questionType === 'MATCHING' && q.matchingPairs && (
                <div className="grid grid-cols-1 md:grid-cols-2 gap-2 pt-1 text-body-sm">
                  {q.matchingPairs.map((pair, idx) => (
                    <div
                      key={idx}
                      className="p-2 rounded bg-surface-container-low border border-outline-variant/20 flex items-center justify-between"
                    >
                      <MathView content={pair.left} />
                      <span className="text-outline mx-2">⇄</span>
                      <MathView content={pair.right} className="font-semibold text-emerald-800" />
                    </div>
                  ))}
                </div>
              )}

              {q.questionType === 'SHORT_ANSWER' && q.shortAnswerKey && (
                <div className="p-2 rounded bg-emerald-50/60 border border-emerald-200 text-body-sm font-mono flex items-center gap-3">
                  <span className="font-bold text-emerald-900">
                    Đáp án chuẩn: {q.shortAnswerKey.acceptedAnswer} {q.shortAnswerKey.unit}
                  </span>
                  {q.shortAnswerKey.tolerance !== undefined && (
                    <span className="text-[11px] text-emerald-700">
                      (Dung sai: ±{q.shortAnswerKey.tolerance})
                    </span>
                  )}
                </div>
              )}

              {/* Explanation (if any) */}
              {q.explanation && (
                <div className="p-2.5 rounded-lg bg-surface-container text-body-sm border border-outline-variant/20 text-on-surface-variant">
                  <span className="font-bold text-primary block text-[11px] uppercase tracking-wider mb-0.5">
                    Hướng dẫn giải & Lập luận:
                  </span>
                  <MathView content={q.explanation} />
                </div>
              )}

              {/* Actions footer */}
              {q.status === 'PENDING_REVIEW' && (
                <div className="flex items-center justify-end gap-2 pt-2 border-t border-outline-variant/20">
                  <button
                    onClick={() => handleReject(q.id)}
                    className="h-8 px-3 rounded bg-surface-container hover:bg-surface-container-high text-red-600 text-label-sm font-semibold transition-colors"
                  >
                    Từ chối
                  </button>
                  <button
                    onClick={() => handleApprove(q.id)}
                    className="h-8 px-3 rounded bg-emerald-600 hover:bg-emerald-700 text-white text-label-sm font-bold shadow-sm transition-colors"
                  >
                    Duyệt câu hỏi
                  </button>
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Question Create Modal */}
      <QuestionCreateModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        onSuccess={handleCreated}
      />
    </div>
  );
};
