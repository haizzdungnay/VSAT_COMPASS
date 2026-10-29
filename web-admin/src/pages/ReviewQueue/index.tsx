import React, { useState, useEffect, useCallback } from 'react';
import type { FullQuestionItem, QuestionStatus } from '../../types/exam';
import {
  fetchReviewQueue,
  fetchQuestionDetail,
  approveBankQuestion,
  rejectBankQuestion,
  requestRevisionBankQuestion
} from '../../services/api';
import { MathView } from '../../components/MathView';

export const ReviewQueue: React.FC = () => {
  const [questions, setQuestions] = useState<FullQuestionItem[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('PENDING_REVIEW');
  const [page, setPage] = useState<number>(0);
  const [subjectFilter, setSubjectFilter] = useState<string>('ALL');
  const [typeFilter, setTypeFilter] = useState<string>('ALL');
  const [search, setSearch] = useState<string>('');
  const [loading, setLoading] = useState<boolean>(false);
  const [fetchError, setFetchError] = useState<string | null>(null);

  // Review modal state
  const [selectedQuestion, setSelectedQuestion] = useState<FullQuestionItem | null>(null);
  const [loadingDetailId, setLoadingDetailId] = useState<number | null>(null);
  const [comment, setComment] = useState<string>('');
  const [submittingAction, setSubmittingAction] = useState<boolean>(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const loadData = useCallback(async () => {
    setLoading(true);
    setFetchError(null);
    try {
      const data = await fetchReviewQueue({
        status: statusFilter,
        page,
        size: 20
      });
      setQuestions(data);
    } catch (err: any) {
      setFetchError(err?.message || 'Không thể tải danh sách duyệt từ máy chủ.');
      setQuestions([]);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, page]);

  useEffect(() => {
    let ignore = false;
    const run = async () => {
      setLoading(true);
      setFetchError(null);
      try {
        const data = await fetchReviewQueue({
          status: statusFilter,
          page,
          size: 20
        });
        if (!ignore) setQuestions(data);
      } catch (err: any) {
        if (!ignore) {
          setFetchError(err?.message || 'Không thể tải danh sách duyệt từ máy chủ.');
          setQuestions([]);
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    };
    run();
    return () => {
      ignore = true;
    };
  }, [statusFilter, page]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && selectedQuestion) {
        setSelectedQuestion(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [selectedQuestion]);

  const handleOpenDetail = async (id: number) => {
    setLoadingDetailId(id);
    setErrorMessage(null);
    setComment('');
    try {
      const detail = await fetchQuestionDetail(id);
      if (detail) {
        setSelectedQuestion(detail);
      } else {
        setErrorMessage('Không thể tải chi tiết câu hỏi từ máy chủ.');
      }
    } catch {
      setErrorMessage('Không thể tải chi tiết câu hỏi.');
    } finally {
      setLoadingDetailId(null);
    }
  };

  const handleApprove = async (id: number) => {
    setSubmittingAction(true);
    setErrorMessage(null);
    try {
      const res = await approveBankQuestion(id, comment.trim());
      if (res.ok) {
        setSelectedQuestion(null);
        await loadData();
      } else {
        setErrorMessage(res.error || 'Duyệt thất bại.');
      }
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleRequestRevision = async (id: number) => {
    if (!comment.trim()) {
      setErrorMessage('Vui lòng nhập nhận xét/yêu cầu sửa đổi (bắt buộc).');
      return;
    }
    if (comment.length > 2000) {
      setErrorMessage('Nhận xét không được vượt quá 2000 ký tự.');
      return;
    }
    setSubmittingAction(true);
    setErrorMessage(null);
    try {
      const res = await requestRevisionBankQuestion(id, comment.trim());
      if (res.ok) {
        setSelectedQuestion(null);
        await loadData();
      } else {
        setErrorMessage(res.error || 'Yêu cầu sửa thất bại.');
      }
    } finally {
      setSubmittingAction(false);
    }
  };

  const handleReject = async (id: number) => {
    if (!comment.trim()) {
      setErrorMessage('Vui lòng nhập lý do từ chối (bắt buộc).');
      return;
    }
    if (comment.length > 2000) {
      setErrorMessage('Nhận xét không được vượt quá 2000 ký tự.');
      return;
    }
    setSubmittingAction(true);
    setErrorMessage(null);
    try {
      const res = await rejectBankQuestion(id, comment.trim());
      if (res.ok) {
        setSelectedQuestion(null);
        await loadData();
      } else {
        setErrorMessage(res.error || 'Từ chối thất bại.');
      }
    } finally {
      setSubmittingAction(false);
    }
  };

  const filtered = questions.filter(q => {
    const matchesSearch = !search ||
      q.questionCode.toLowerCase().includes(search.toLowerCase()) ||
      q.questionText.toLowerCase().includes(search.toLowerCase());
    const matchesSubject = subjectFilter === 'ALL' ||
      (subjectFilter === 'MATH' && q.subjectId === 1) ||
      (subjectFilter === 'ENG' && q.subjectId === 2) ||
      (subjectFilter === 'PHY' && q.subjectId === 3);
    const matchesType = typeFilter === 'ALL' || q.questionType === typeFilter;
    return matchesSearch && matchesSubject && matchesType;
  });

  const getStatusBadge = (status: QuestionStatus) => {
    switch (status) {
      case 'PENDING_REVIEW':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-label-sm text-label-sm bg-amber-100 text-amber-800 border border-amber-200">
            <span className="w-1.5 h-1.5 rounded-full bg-amber-600 animate-pulse"></span>
            Chờ duyệt
          </span>
        );
      case 'APPROVED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-label-sm text-label-sm bg-emerald-100 text-emerald-800 border border-emerald-200">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-600"></span>
            Đã duyệt
          </span>
        );
      case 'NEEDS_REVISION':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-label-sm text-label-sm bg-orange-100 text-orange-800 border border-orange-200">
            <span className="w-1.5 h-1.5 rounded-full bg-orange-600"></span>
            Yêu cầu sửa
          </span>
        );
      case 'ARCHIVED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-label-sm text-label-sm bg-rose-100 text-rose-800 border border-rose-200">
            <span className="w-1.5 h-1.5 rounded-full bg-rose-600"></span>
            Từ chối
          </span>
        );
      case 'PUBLISHED':
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-label-sm text-label-sm bg-blue-100 text-blue-800 border border-blue-200">
            <span className="w-1.5 h-1.5 rounded-full bg-blue-600"></span>
            Đã phát hành
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full font-label-sm text-label-sm bg-slate-100 text-slate-700">
            {status}
          </span>
        );
    }
  };

  const getTypeBadge = (type: string) => {
    const map: Record<string, { label: string; color: string }> = {
      TRUE_FALSE: { label: 'Đúng / Sai', color: 'bg-indigo-50 text-indigo-700 border-indigo-200' },
      SINGLE_CHOICE: { label: 'Trắc nghiệm đơn', color: 'bg-blue-50 text-blue-700 border-blue-200' },
      MULTIPLE_CHOICE: { label: 'Nhiều lựa chọn', color: 'bg-cyan-50 text-cyan-700 border-cyan-200' },
      FILL_IN_BLANK: { label: 'Điền khuyết', color: 'bg-emerald-50 text-emerald-700 border-emerald-200' },
      MATCHING: { label: 'Ghép hợp', color: 'bg-purple-50 text-purple-700 border-purple-200' },
      SHORT_ANSWER: { label: 'Điền số', color: 'bg-teal-50 text-teal-700 border-teal-200' }
    };
    const info = map[type] || { label: type, color: 'bg-slate-50 text-slate-700 border-slate-200' };
    return (
      <span className={`px-2 py-0.5 rounded-md font-label-sm text-[11px] border ${info.color}`}>
        {info.label}
      </span>
    );
  };

  return (
    <div className="p-margin space-y-space-lg">
      {/* Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md">
        <div className="space-y-1">
          <div className="flex items-center gap-space-sm">
            <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight">
              Hàng đợi duyệt câu hỏi V-SAT
            </h1>
            <span className="px-2 py-0.5 rounded-full font-label-sm text-label-sm bg-primary-fixed text-on-primary-fixed font-medium">
              Q-ADM Workflow
            </span>
          </div>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Phê duyệt, yêu cầu sửa đổi hoặc từ chối câu hỏi do Cộng tác viên gửi lên ngân hàng câu hỏi
          </p>
        </div>

        <div className="flex items-center gap-space-sm">
          <button
            onClick={loadData}
            disabled={loading}
            className="h-9 px-space-md rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-label-md flex items-center gap-1.5 shadow-sm transition-colors disabled:opacity-50"
          >
            <span className={`material-symbols-outlined text-[18px] text-outline ${loading ? 'animate-spin' : ''}`}>
              refresh
            </span>
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* Error state alert */}
      {fetchError && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-200 text-rose-800 flex items-center justify-between gap-3 font-body-sm">
          <div className="flex items-center gap-2">
            <span className="material-symbols-outlined text-[20px] text-rose-600">error</span>
            <span>{fetchError}</span>
          </div>
          <button
            onClick={loadData}
            className="px-3 py-1 rounded-lg bg-rose-600 text-white font-label-sm font-medium hover:bg-rose-700"
          >
            Thử lại
          </button>
        </div>
      )}

      {/* Status Pill Tabs */}
      <div className="flex items-center gap-2 border-b border-outline-variant/30 pb-3 overflow-x-auto">
        {[
          { key: 'PENDING_REVIEW', label: 'Chờ duyệt', icon: 'pending' },
          { key: 'APPROVED', label: 'Đã duyệt', icon: 'check_circle' },
          { key: 'NEEDS_REVISION', label: 'Yêu cầu sửa', icon: 'edit_note' },
          { key: 'ARCHIVED', label: 'Đã từ chối', icon: 'cancel' },
          { key: 'PUBLISHED', label: 'Đã phát hành', icon: 'public' },
          { key: 'ALL', label: 'Tất cả trạng thái', icon: 'list' }
        ].map(tab => (
          <button
            key={tab.key}
            onClick={() => { setStatusFilter(tab.key); setPage(0); }}
            className={`px-3.5 py-1.5 rounded-lg font-label-md text-label-md flex items-center gap-1.5 transition-colors whitespace-nowrap ${
              statusFilter === tab.key
                ? 'bg-primary text-on-primary font-medium shadow-sm'
                : 'bg-surface-container-low hover:bg-surface-container text-on-surface-variant'
            }`}
          >
            <span className="material-symbols-outlined text-[16px]">{tab.icon}</span>
            <span>{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col md:flex-row gap-space-sm items-stretch md:items-center justify-between">
        <div className="flex-1 relative max-w-md">
          <span className="material-symbols-outlined absolute left-3 top-2.5 text-[18px] text-outline">
            search
          </span>
          <input
            type="text"
            value={search}
            onChange={e => setSearch(e.target.value)}
            placeholder="Tìm theo mã câu hỏi hoặc từ khóa..."
            className="w-full h-10 pl-9 pr-3 rounded-lg border border-outline-variant/40 bg-surface-container-lowest font-body-sm text-on-surface placeholder:text-outline focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition-colors"
          />
        </div>

        <div className="flex items-center gap-2 flex-wrap">
          <select
            value={subjectFilter}
            onChange={e => setSubjectFilter(e.target.value)}
            className="h-10 px-3 rounded-lg border border-outline-variant/40 bg-surface-container-lowest font-body-sm text-on-surface focus:outline-none focus:border-primary"
          >
            <option value="ALL">Tất cả môn học</option>
            <option value="MATH">Toán học</option>
            <option value="ENG">Tiếng Anh</option>
            <option value="PHY">Vật lí</option>
          </select>

          <select
            value={typeFilter}
            onChange={e => setTypeFilter(e.target.value)}
            className="h-10 px-3 rounded-lg border border-outline-variant/40 bg-surface-container-lowest font-body-sm text-on-surface focus:outline-none focus:border-primary"
          >
            <option value="ALL">Tất cả dạng câu</option>
            <option value="TRUE_FALSE">Đúng / Sai</option>
            <option value="SINGLE_CHOICE">Trắc nghiệm 4 lựa chọn</option>
            <option value="MULTIPLE_CHOICE">Nhiều lựa chọn</option>
            <option value="FILL_IN_BLANK">Điền khuyết</option>
            <option value="MATCHING">Ghép hợp</option>
            <option value="SHORT_ANSWER">Điền số</option>
          </select>
        </div>
      </div>

      {/* Question Table / List */}
      <div className="rounded-xl border border-outline-variant/30 bg-surface-container-lowest overflow-hidden shadow-sm">
        {loading ? (
          <div className="p-12 text-center text-outline flex flex-col items-center gap-2">
            <span className="material-symbols-outlined text-[32px] animate-spin text-primary">sync</span>
            <span>Đang tải hàng đợi duyệt...</span>
          </div>
        ) : filtered.length === 0 ? (
          <div className="p-12 text-center text-outline flex flex-col items-center gap-2">
            <span className="material-symbols-outlined text-[48px] text-outline/50">inbox</span>
            <p className="font-headline-sm text-on-surface">Không có câu hỏi nào trong hàng đợi</p>
            <p className="text-body-sm text-outline max-w-sm">
              Hiện tại không có câu hỏi nào khớp với bộ lọc trạng thái {statusFilter === 'ALL' ? 'đã chọn' : statusFilter}.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="border-b border-outline-variant/20 bg-surface-container-low/40 text-outline font-label-sm text-[11px] uppercase tracking-wider">
                  <th className="py-3 px-4">Mã câu hỏi</th>
                  <th className="py-3 px-4">Môn & Dạng thức</th>
                  <th className="py-3 px-4">Nội dung tóm tắt</th>
                  <th className="py-3 px-4">Độ khó</th>
                  <th className="py-3 px-4">Trạng thái</th>
                  <th className="py-3 px-4 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/20 font-body-sm">
                {filtered.map(q => (
                  <tr key={q.id} className="hover:bg-surface-container-low/50 transition-colors">
                    <td className="py-3 px-4 font-mono font-bold text-primary whitespace-nowrap">
                      {q.questionCode}
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <div className="flex flex-col gap-1">
                        <span className="font-medium text-on-surface">{q.subjectName || 'Toán học'}</span>
                        <div>{getTypeBadge(q.questionType)}</div>
                      </div>
                    </td>
                    <td className="py-3 px-4 max-w-md">
                      <div className="line-clamp-2 text-on-surface">
                        <MathView content={q.questionText} />
                      </div>
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      <span className="px-2 py-0.5 rounded bg-surface-container font-label-sm text-[11px] text-on-surface-variant font-medium">
                        {q.difficulty}
                      </span>
                    </td>
                    <td className="py-3 px-4 whitespace-nowrap">
                      {getStatusBadge(q.status)}
                    </td>
                    <td className="py-3 px-4 text-right whitespace-nowrap">
                      <div className="flex items-center justify-end gap-1.5">
                        <button
                          onClick={() => handleOpenDetail(q.id)}
                          disabled={loadingDetailId === q.id}
                          className="px-3 py-1.5 rounded-lg bg-primary-container text-on-primary font-label-sm font-medium hover:opacity-90 flex items-center gap-1 shadow-sm transition-opacity disabled:opacity-50"
                        >
                          <span className={`material-symbols-outlined text-[16px] ${loadingDetailId === q.id ? 'animate-spin' : ''}`}>
                            {loadingDetailId === q.id ? 'sync' : 'visibility'}
                          </span>
                          <span>{loadingDetailId === q.id ? 'Đang mở...' : 'Xem & Duyệt'}</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <div className="p-space-md border-t border-outline-variant/20 flex items-center justify-between text-body-sm text-outline">
              <span>Hiển thị {filtered.length} câu hỏi (Trang {page + 1})</span>
              <div className="flex items-center gap-2">
                <button
                  onClick={() => setPage(p => Math.max(0, p - 1))}
                  disabled={page === 0 || loading}
                  className="px-3 py-1.5 rounded-lg border border-outline-variant/30 hover:bg-surface-container font-label-sm text-on-surface disabled:opacity-40"
                >
                  Trang trước
                </button>
                <button
                  onClick={() => setPage(p => p + 1)}
                  disabled={questions.length < 20 || loading}
                  className="px-3 py-1.5 rounded-lg border border-outline-variant/30 hover:bg-surface-container font-label-sm text-on-surface disabled:opacity-40"
                >
                  Trang sau
                </button>
              </div>
            </div>
          </div>
        )}
      </div>

      {/* Review & Detail Modal */}
      {selectedQuestion && (
        <div
          className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4 overflow-y-auto"
          onClick={e => {
            if (e.target === e.currentTarget) setSelectedQuestion(null);
          }}
        >
          <div className="bg-surface-container-lowest rounded-2xl max-w-3xl w-full border border-outline-variant/40 shadow-2xl overflow-hidden my-8 animate-in fade-in zoom-in-95 duration-150">
            {/* Modal Header */}
            <div className="p-space-lg border-b border-outline-variant/20 flex items-center justify-between bg-surface-container-low/40">
              <div className="flex items-center gap-3">
                <span className="font-mono text-headline-sm font-bold text-primary">
                  {selectedQuestion.questionCode}
                </span>
                {getTypeBadge(selectedQuestion.questionType)}
                {getStatusBadge(selectedQuestion.status)}
              </div>
              <button
                onClick={() => setSelectedQuestion(null)}
                className="w-8 h-8 rounded-lg flex items-center justify-center text-outline hover:bg-surface-container hover:text-on-surface transition-colors"
              >
                <span className="material-symbols-outlined text-[20px]">close</span>
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-space-lg space-y-space-md max-h-[70vh] overflow-y-auto">
              {/* Question content with MathView */}
              <div className="space-y-2">
                <span className="font-label-sm uppercase text-[11px] text-outline font-bold tracking-wider">
                  Nội dung câu hỏi:
                </span>
                <div className="p-4 rounded-xl bg-surface border border-outline-variant/30 text-body-md text-on-surface leading-relaxed">
                  <MathView content={selectedQuestion.questionText || '(Trống)'} />
                </div>
              </div>

              {/* Options / Answer choices */}
              {selectedQuestion.options && selectedQuestion.options.length > 0 && (
                <div className="space-y-2">
                  <span className="font-label-sm uppercase text-[11px] text-outline font-bold tracking-wider">
                    Các lựa chọn:
                  </span>
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
                    {selectedQuestion.options.map((opt, i) => (
                      <div
                        key={i}
                        className={`p-3 rounded-lg border flex items-start gap-2.5 ${
                          opt.isCorrect
                            ? 'bg-emerald-50 border-emerald-300 text-emerald-900'
                            : 'bg-surface-container-low border-outline-variant/30 text-on-surface'
                        }`}
                      >
                        <span className={`w-6 h-6 rounded flex items-center justify-center font-bold text-[12px] flex-shrink-0 ${
                          opt.isCorrect ? 'bg-emerald-600 text-white' : 'bg-surface-container-high text-outline'
                        }`}>
                          {opt.label || String.fromCharCode(65 + i)}
                        </span>
                        <div className="flex-1 text-body-sm">
                          <MathView content={opt.text} />
                        </div>
                        {opt.isCorrect && (
                          <span className="material-symbols-outlined text-emerald-600 text-[18px]">
                            check_circle
                          </span>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* True/False Items */}
              {selectedQuestion.trueFalseItems && selectedQuestion.trueFalseItems.length > 0 && (
                <div className="space-y-2">
                  <span className="font-label-sm uppercase text-[11px] text-outline font-bold tracking-wider">
                    Các mệnh đề (Đúng / Sai):
                  </span>
                  <div className="space-y-2">
                    {selectedQuestion.trueFalseItems.map((item, idx) => (
                      <div
                        key={idx}
                        className="p-3 rounded-lg border border-outline-variant/30 bg-surface-container-low flex items-center justify-between gap-3"
                      >
                        <div className="flex items-start gap-2.5 flex-1">
                          <span className="w-6 h-6 rounded bg-surface-container-high text-on-surface-variant font-bold text-[12px] flex items-center justify-center flex-shrink-0">
                            {item.key || String.fromCharCode(97 + idx)}
                          </span>
                          <div className="text-body-sm text-on-surface">
                            <MathView content={item.text} />
                          </div>
                        </div>
                        <span className={`px-2.5 py-1 rounded-md text-[12px] font-bold whitespace-nowrap ${
                          item.isCorrect
                            ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                            : 'bg-rose-100 text-rose-800 border border-rose-300'
                        }`}>
                          {item.isCorrect ? 'ĐÚNG' : 'SAI'}
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Matching Pairs */}
              {selectedQuestion.matchingPairs && selectedQuestion.matchingPairs.length > 0 && (
                <div className="space-y-2">
                  <span className="font-label-sm uppercase text-[11px] text-outline font-bold tracking-wider">
                    Các cặp ghép nối:
                  </span>
                  <div className="space-y-2">
                    {selectedQuestion.matchingPairs.map((pair, idx) => (
                      <div
                        key={idx}
                        className="p-3 rounded-lg border border-outline-variant/30 bg-surface-container-low grid grid-cols-1 md:grid-cols-2 gap-3 items-center"
                      >
                        <div className="p-2 rounded bg-surface border border-outline-variant/20 text-body-sm text-on-surface">
                          <span className="text-[11px] text-outline font-bold block mb-1">Vế A ({idx + 1}):</span>
                          <MathView content={pair.left} />
                        </div>
                        <div className="p-2 rounded bg-emerald-50 border border-emerald-200 text-body-sm text-emerald-950">
                          <span className="text-[11px] text-emerald-700 font-bold block mb-1">Ghép với Vế B:</span>
                          <MathView content={pair.right} />
                        </div>
                      </div>
                    ))}
                  </div>
                </div>
              )}

              {/* Short Answer Key */}
              {selectedQuestion.shortAnswerKey && (
                <div className="space-y-2">
                  <span className="font-label-sm uppercase text-[11px] text-outline font-bold tracking-wider">
                    Đáp án điền số / điền khuyết:
                  </span>
                  <div className="p-3.5 rounded-lg border border-emerald-300 bg-emerald-50 text-emerald-950 flex items-center gap-4 flex-wrap">
                    <div>
                      <span className="text-[11px] text-emerald-700 font-bold block">Đáp án chuẩn:</span>
                      <span className="font-mono font-bold text-base">{selectedQuestion.shortAnswerKey.acceptedAnswer}</span>
                    </div>
                    {selectedQuestion.shortAnswerKey.tolerance !== undefined && (
                      <div>
                        <span className="text-[11px] text-emerald-700 font-bold block">Sai số cho phép:</span>
                        <span className="font-mono text-sm">±{selectedQuestion.shortAnswerKey.tolerance}</span>
                      </div>
                    )}
                    {selectedQuestion.shortAnswerKey.unit && (
                      <div>
                        <span className="text-[11px] text-emerald-700 font-bold block">Đơn vị:</span>
                        <span className="font-mono text-sm">{selectedQuestion.shortAnswerKey.unit}</span>
                      </div>
                    )}
                  </div>
                </div>
              )}

              {/* Explanation with MathView */}
              {selectedQuestion.explanation && (
                <div className="space-y-2">
                  <span className="font-label-sm uppercase text-[11px] text-outline font-bold tracking-wider flex items-center gap-1">
                    <span className="material-symbols-outlined text-[14px] text-primary">lightbulb</span>
                    Lời giải chi tiết:
                  </span>
                  <div className="p-3.5 rounded-xl bg-blue-50/60 border border-blue-200/60 text-body-sm text-blue-950 leading-relaxed">
                    <MathView content={selectedQuestion.explanation} />
                  </div>
                </div>
              )}

              {/* Review Action Form only when PENDING_REVIEW */}
              {selectedQuestion.status === 'PENDING_REVIEW' ? (
                <div className="pt-2 border-t border-outline-variant/20 space-y-2">
                  <div className="flex items-center justify-between">
                    <label className="block font-label-sm font-medium text-on-surface">
                      Nhận xét / Lý do duyệt / Yêu cầu sửa đổi:
                    </label>
                    <span className={`text-[11px] font-mono ${comment.length > 2000 ? 'text-rose-600 font-bold' : 'text-outline'}`}>
                      {comment.length}/2000
                    </span>
                  </div>
                  <textarea
                    value={comment}
                    maxLength={2000}
                    onChange={e => setComment(e.target.value)}
                    placeholder="Bắt buộc khi yêu cầu sửa hoặc từ chối; không bắt buộc khi phê duyệt..."
                    rows={3}
                    className="w-full p-3 rounded-xl border border-outline-variant/40 bg-surface-container-lowest font-body-sm text-on-surface placeholder:text-outline focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary"
                  />
                  {errorMessage && (
                    <p className="font-label-sm text-rose-600 flex items-center gap-1">
                      <span className="material-symbols-outlined text-[16px]">error</span>
                      {errorMessage}
                    </p>
                  )}
                </div>
              ) : (
                <div className="p-3 rounded-xl bg-surface-container border border-outline-variant/30 text-outline font-body-sm flex items-center gap-2">
                  <span className="material-symbols-outlined text-[18px]">info</span>
                  <span>Câu hỏi này đang ở trạng thái <strong>{selectedQuestion.status}</strong>, chỉ có câu hỏi ở trạng thái <strong>PENDING_REVIEW</strong> mới có thể thao tác duyệt.</span>
                </div>
              )}
            </div>

            {/* Modal Actions Footer */}
            <div className="p-space-lg border-t border-outline-variant/20 bg-surface-container-low/40 flex flex-col sm:flex-row items-center justify-between gap-space-sm">
              <button
                type="button"
                onClick={() => setSelectedQuestion(null)}
                className="w-full sm:w-auto px-4 py-2 rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md transition-colors"
              >
                Đóng
              </button>

              {selectedQuestion.status === 'PENDING_REVIEW' && (
                <div className="w-full sm:w-auto flex items-center gap-2 flex-wrap justify-end">
                  <button
                    type="button"
                    disabled={submittingAction}
                    onClick={() => handleReject(selectedQuestion.id)}
                    className="px-3.5 py-2 rounded-lg bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200 font-label-md flex items-center gap-1.5 transition-colors disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-[18px]">cancel</span>
                    <span>Từ chối</span>
                  </button>

                  <button
                    type="button"
                    disabled={submittingAction}
                    onClick={() => handleRequestRevision(selectedQuestion.id)}
                    className="px-3.5 py-2 rounded-lg bg-amber-50 text-amber-800 hover:bg-amber-100 border border-amber-200 font-label-md flex items-center gap-1.5 transition-colors disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-[18px]">edit_note</span>
                    <span>Yêu cầu sửa</span>
                  </button>

                  <button
                    type="button"
                    disabled={submittingAction}
                    onClick={() => handleApprove(selectedQuestion.id)}
                    className="px-4 py-2 rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 font-label-md font-medium flex items-center gap-1.5 shadow-sm transition-colors disabled:opacity-50"
                  >
                    <span className="material-symbols-outlined text-[18px]">check_circle</span>
                    <span>Phê duyệt</span>
                  </button>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
