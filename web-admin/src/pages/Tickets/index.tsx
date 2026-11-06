import React, { useState, useEffect, useCallback } from 'react';
import type { TicketItem, TicketStatus } from '../../services/api';
import type { FullQuestionItem } from '../../types/exam';
import { fetchTickets, updateTicket, fetchQuestionDetail } from '../../services/api';
import { MathView } from '../../components/MathView';

const STATUS_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'OPEN', label: 'Mới tạo (OPEN)' },
  { value: 'IN_PROGRESS', label: 'Đang xử lý (IN_PROGRESS)' },
  { value: 'CLOSED', label: 'Đã giải quyết (CLOSED)' }
];

function getTicketStatusBadge(status: TicketStatus) {
  switch (status) {
    case 'OPEN':
      return {
        label: 'Mới tạo (OPEN)',
        className: 'bg-rose-50 text-rose-700 border-rose-300 dark:bg-rose-950 dark:text-rose-300'
      };
    case 'IN_PROGRESS':
      return {
        label: 'Đang xử lý',
        className: 'bg-amber-50 text-amber-700 border-amber-300 dark:bg-amber-950 dark:text-amber-300'
      };
    case 'CLOSED':
    default:
      return {
        label: 'Đã đóng',
        className: 'bg-emerald-50 text-emerald-700 border-emerald-300 dark:bg-emerald-950 dark:text-emerald-300'
      };
  }
}

function formatDate(isoString: string): string {
  try {
    const d = new Date(isoString);
    return d.toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch {
    return isoString;
  }
}

export const TicketsPage: React.FC = () => {
  const [tickets, setTickets] = useState<TicketItem[]>([]);
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);

  // Selected ticket & Question detail
  const [selectedTicket, setSelectedTicket] = useState<TicketItem | null>(null);
  const [questionDetail, setQuestionDetail] = useState<FullQuestionItem | null>(null);
  const [loadingQuestion, setLoadingQuestion] = useState<boolean>(false);

  // Form states for resolving
  const [targetStatus, setTargetStatus] = useState<TicketStatus>('IN_PROGRESS');
  const [resolverNote, setResolverNote] = useState<string>('');
  const [submittingAction, setSubmittingAction] = useState<boolean>(false);

  // Feedback messages
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const loadTickets = useCallback(async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const res = await fetchTickets({
        status: statusFilter,
        page,
        size: 20
      });
      setTickets(res.content);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements);
    } catch (err: any) {
      setErrorMessage(err?.message || 'Không thể tải danh sách ticket từ máy chủ.');
      setTickets([]);
    } finally {
      setLoading(false);
    }
  }, [statusFilter, page]);

  useEffect(() => {
    let ignore = false;
    const run = async () => {
      setLoading(true);
      setErrorMessage(null);
      try {
        const res = await fetchTickets({
          status: statusFilter,
          page,
          size: 20
        });
        if (!ignore) {
          setTickets(res.content);
          setTotalPages(res.totalPages || 1);
          setTotalElements(res.totalElements);
        }
      } catch (err: any) {
        if (!ignore) {
          setErrorMessage(err?.message || 'Không thể tải danh sách ticket từ máy chủ.');
          setTickets([]);
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

  // When questionId of selected ticket changes, fetch question detail
  useEffect(() => {
    const qId = selectedTicket?.questionId;
    if (!qId) {
      return;
    }

    let ignore = false;
    fetchQuestionDetail(qId)
      .then((detail) => {
        if (!ignore) {
          setQuestionDetail(detail);
        }
      })
      .catch(() => {
        if (!ignore) {
          setQuestionDetail(null);
        }
      })
      .finally(() => {
        if (!ignore) {
          setLoadingQuestion(false);
        }
      });
    return () => {
      ignore = true;
    };
  }, [selectedTicket?.questionId]);

  const handleSelectTicket = (t: TicketItem) => {
    setSelectedTicket(t);
    setResolverNote(t.resolverNote || '');
    if (t.questionId) {
      setQuestionDetail(null);
      setLoadingQuestion(true);
    } else {
      setQuestionDetail(null);
      setLoadingQuestion(false);
    }
    if (t.status === 'OPEN') {
      setTargetStatus('IN_PROGRESS');
    } else if (t.status === 'IN_PROGRESS') {
      setTargetStatus('CLOSED');
    } else {
      setTargetStatus('CLOSED');
    }
    setErrorMessage(null);
    setSuccessMessage(null);
  };

  const handleUpdateTicket = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedTicket) return;

    if (resolverNote.length > 5000) {
      setErrorMessage('Nội dung phản hồi không được vượt quá 5000 ký tự.');
      return;
    }

    setSubmittingAction(true);
    setErrorMessage(null);
    setSuccessMessage(null);

    const res = await updateTicket(selectedTicket.id, {
      status: targetStatus,
      resolverNote: resolverNote.trim() || undefined
    });

    setSubmittingAction(false);

    if (res.ok && res.data) {
      setSuccessMessage('Đã cập nhật trạng thái ticket và lưu ghi chú thành công.');
      setSelectedTicket(res.data);
      // Update item in local list immediately
      setTickets((prev) => prev.map((item) => (item.id === res.data!.id ? res.data! : item)));
    } else {
      setErrorMessage(res.message || 'Không thể cập nhật ticket.');
    }
  };

  // Determine allowed transitions
  const isClosed = selectedTicket?.status === 'CLOSED';
  const allowedStatuses: TicketStatus[] = selectedTicket?.status === 'OPEN'
    ? ['OPEN', 'IN_PROGRESS', 'CLOSED']
    : selectedTicket?.status === 'IN_PROGRESS'
    ? ['IN_PROGRESS', 'CLOSED']
    : ['CLOSED'];

  return (
    <div className="p-margin space-y-6">
      {/* Title & Actions */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="font-headline-lg text-on-surface">Báo lỗi học thuật & Ticket</h1>
          <p className="font-body-md text-outline mt-1">
            Xử lý khiếu nại đề thi, báo lỗi công thức toán KaTeX và phản hồi cho thí sinh.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              setPage(0);
              loadTickets();
            }}
            disabled={loading}
            className="flex items-center gap-1.5 px-3 py-2 rounded-lg border border-outline-variant/60 bg-surface-container-lowest text-on-surface hover:bg-surface-container font-label-md transition-colors"
          >
            <span className={`material-symbols-outlined text-[18px] ${loading ? 'animate-spin' : ''}`}>
              refresh
            </span>
            <span>Làm mới</span>
          </button>
        </div>
      </div>

      {/* Global Alerts */}
      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-300 text-rose-800 dark:bg-rose-950/40 dark:border-rose-800 dark:text-rose-200 flex items-start gap-3">
          <span className="material-symbols-outlined text-[22px] flex-shrink-0 text-rose-600">
            error
          </span>
          <div className="flex-1 text-body-md">
            <span className="font-bold">Lỗi: </span>
            {errorMessage}
          </div>
          <button
            onClick={() => setErrorMessage(null)}
            className="text-rose-600 hover:text-rose-800 text-body-sm font-semibold"
          >
            Đóng
          </button>
        </div>
      )}

      {successMessage && (
        <div className="p-4 rounded-xl bg-emerald-50 border border-emerald-300 text-emerald-800 dark:bg-emerald-950/40 dark:border-emerald-800 dark:text-emerald-200 flex items-start gap-3">
          <span className="material-symbols-outlined text-[22px] flex-shrink-0 text-emerald-600">
            check_circle
          </span>
          <div className="flex-1 text-body-md">
            <span className="font-bold">Thành công: </span>
            {successMessage}
          </div>
          <button
            onClick={() => setSuccessMessage(null)}
            className="text-emerald-600 hover:text-emerald-800 text-body-sm font-semibold"
          >
            Đóng
          </button>
        </div>
      )}

      {/* Filter Bar */}
      <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 flex flex-wrap items-center justify-between gap-4">
        <div className="flex items-center gap-3">
          <span className="text-[11px] font-bold text-outline uppercase tracking-wider">
            Lọc theo trạng thái:
          </span>
          <select
            value={statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            className="px-3 py-2 rounded-lg border border-outline-variant/60 bg-surface font-body-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20"
          >
            {STATUS_OPTIONS.map((opt) => (
              <option key={opt.value} value={opt.value}>
                {opt.label}
              </option>
            ))}
          </select>
        </div>
        <div className="text-body-sm text-outline">
          Tổng cộng: <span className="font-semibold text-on-surface">{totalElements}</span> tickets
        </div>
      </div>

      {/* Main Split Layout: Left List (5 cols), Right Detail/Resolve (7 cols) */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* Ticket List Panel */}
        <div className="lg:col-span-5 space-y-3">
          <div className="rounded-xl bg-surface-container-lowest border border-outline-variant/30 overflow-hidden shadow-sm flex flex-col">
            <div className="p-3.5 border-b border-outline-variant/30 bg-surface-container-low/50 flex items-center justify-between">
              <span className="text-[11px] font-bold text-outline uppercase tracking-wider">
                Danh sách Ticket
              </span>
              <span className="text-[12px] text-outline">Trang {page + 1}/{totalPages}</span>
            </div>

            <div className="divide-y divide-outline-variant/20 max-h-[680px] overflow-y-auto">
              {loading && tickets.length === 0 ? (
                <div className="p-8 text-center text-outline">
                  <span className="material-symbols-outlined text-[32px] animate-spin text-primary block mx-auto mb-2">
                    progress_activity
                  </span>
                  Đang tải tickets...
                </div>
              ) : tickets.length === 0 ? (
                <div className="p-8 text-center text-outline">
                  <span className="material-symbols-outlined text-[36px] text-outline/50 block mx-auto mb-2">
                    inbox
                  </span>
                  Không có ticket nào trong trạng thái này.
                </div>
              ) : (
                tickets.map((t) => {
                  const badge = getTicketStatusBadge(t.status);
                  const isSelected = selectedTicket?.id === t.id;

                  return (
                    <div
                      key={t.id}
                      onClick={() => handleSelectTicket(t)}
                      className={`p-4 cursor-pointer transition-colors ${
                        isSelected
                          ? 'bg-primary/5 border-l-4 border-l-primary'
                          : 'hover:bg-surface-container-low/60 border-l-4 border-l-transparent'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2 mb-1.5">
                        <span className="font-bold text-sm text-on-surface">Ticket #{t.id}</span>
                        <span
                          className={`px-2 py-0.5 rounded-full text-[10px] font-bold border ${badge.className}`}
                        >
                          {badge.label}
                        </span>
                      </div>

                      <div className="text-body-sm text-on-surface line-clamp-2 mb-2">
                        {t.message}
                      </div>

                      <div className="flex items-center justify-between text-[11px] text-outline">
                        <span>
                          {t.questionId ? (
                            <span className="text-primary font-semibold">Câu hỏi #{t.questionId}</span>
                          ) : (
                            <span className="italic">Lỗi chung</span>
                          )}
                          {' • '}Thí sinh #{t.reporterUserId}
                        </span>
                        <span>{formatDate(t.createdAt)}</span>
                      </div>
                    </div>
                  );
                })
              )}
            </div>

            {/* Pagination Controls */}
            {totalPages > 1 && (
              <div className="p-3 border-t border-outline-variant/30 flex items-center justify-between bg-surface-container-low/30">
                <button
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0 || loading}
                  className="px-2.5 py-1 text-[12px] rounded border border-outline-variant/60 bg-surface font-label-md hover:bg-surface-container disabled:opacity-40"
                >
                  Trước
                </button>
                <span className="text-[12px] text-outline">
                  {page + 1} / {totalPages}
                </span>
                <button
                  onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
                  disabled={page >= totalPages - 1 || loading}
                  className="px-2.5 py-1 text-[12px] rounded border border-outline-variant/60 bg-surface font-label-md hover:bg-surface-container disabled:opacity-40"
                >
                  Sau
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Ticket Details & Resolution Panel */}
        <div className="lg:col-span-7">
          {selectedTicket ? (
            <div className="space-y-5 rounded-xl bg-surface-container-lowest border border-outline-variant/30 p-6 shadow-sm">
              {/* Header */}
              <div className="flex flex-wrap items-center justify-between gap-3 border-b border-outline-variant/30 pb-4">
                <div>
                  <div className="flex items-center gap-3">
                    <h2 className="font-headline-sm text-on-surface">Ticket #{selectedTicket.id}</h2>
                    <span
                      className={`px-2.5 py-0.5 rounded-full text-[11px] font-bold border ${
                        getTicketStatusBadge(selectedTicket.status).className
                      }`}
                    >
                      {getTicketStatusBadge(selectedTicket.status).label}
                    </span>
                  </div>
                  <div className="text-[12px] text-outline mt-1">
                    Gửi bởi Thí sinh #{selectedTicket.reporterUserId} • Nhận lúc: {formatDate(selectedTicket.createdAt)}
                  </div>
                </div>
              </div>

              {/* Student Error Message with MathView */}
              <div className="space-y-2">
                <label className="text-[11px] font-bold text-outline uppercase tracking-wider block">
                  Nội dung báo lỗi của thí sinh:
                </label>
                <div className="p-4 rounded-xl bg-surface-container-low/70 border border-outline-variant/30 text-body-md text-on-surface leading-relaxed">
                  <MathView content={selectedTicket.message} />
                </div>
              </div>

              {/* Question Context (if questionId is present) */}
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <label className="text-[11px] font-bold text-outline uppercase tracking-wider">
                    {selectedTicket.questionId
                      ? `Thông tin câu hỏi liên kết (#${selectedTicket.questionId})`
                      : 'Câu hỏi liên kết'}
                  </label>
                </div>

                {selectedTicket.questionId ? (
                  loadingQuestion ? (
                    <div className="p-6 rounded-xl border border-outline-variant/30 text-center text-outline">
                      <span className="material-symbols-outlined text-[24px] animate-spin text-primary block mx-auto mb-1">
                        progress_activity
                      </span>
                      Đang tải nội dung câu hỏi #{selectedTicket.questionId}...
                    </div>
                  ) : questionDetail ? (
                    <div className="p-4 rounded-xl border border-primary/20 bg-primary/2 space-y-3">
                      <div className="flex flex-wrap items-center justify-between gap-2 text-[12px]">
                        <span className="font-semibold text-primary">
                          Mã: {questionDetail.questionCode} • {questionDetail.subjectName || 'Toán học'}
                        </span>
                        <span className="px-2 py-0.5 rounded bg-surface border border-outline-variant/40 text-outline text-[11px]">
                          Dạng: {questionDetail.questionType} • Độ khó: {questionDetail.difficulty}
                        </span>
                      </div>

                      {/* Question Text */}
                      <div className="p-3 bg-surface rounded-lg border border-outline-variant/30 text-body-sm text-on-surface">
                        <div className="font-bold text-[11px] text-outline uppercase tracking-wider mb-1">
                          Đề bài:
                        </div>
                        <MathView content={questionDetail.questionText} />
                      </div>

                      {/* Explanation */}
                      {questionDetail.explanation && (
                        <div className="p-3 bg-surface rounded-lg border border-outline-variant/30 text-body-sm text-on-surface">
                          <div className="font-bold text-[11px] text-outline uppercase tracking-wider mb-1 text-emerald-700">
                            Lời giải / Hướng dẫn giải chi tiết:
                          </div>
                          <MathView content={questionDetail.explanation} />
                        </div>
                      )}
                    </div>
                  ) : (
                    <div className="p-4 rounded-xl border border-outline-variant/30 bg-surface-container-low/40 text-body-sm text-outline">
                      Không tìm thấy dữ liệu chi tiết cho câu hỏi #{selectedTicket.questionId} trên hệ thống.
                    </div>
                  )
                ) : (
                  <div className="p-4 rounded-xl border border-dashed border-outline-variant/50 bg-surface-container-low/30 text-body-sm text-outline flex items-center gap-2">
                    <span className="material-symbols-outlined text-[20px] text-outline">info</span>
                    <span>Đây là báo lỗi chung về hệ thống hoặc phòng thi (không gắn với mã câu hỏi cụ thể).</span>
                  </div>
                )}
              </div>

              {/* Admin Resolution Form */}
              <form onSubmit={handleUpdateTicket} className="space-y-4 pt-3 border-t border-outline-variant/30">
                <div className="flex items-center justify-between">
                  <label className="text-[11px] font-bold text-outline uppercase tracking-wider">
                    Phản hồi & Cập nhật trạng thái
                  </label>
                  {isClosed && (
                    <span className="text-[11px] text-emerald-600 font-semibold flex items-center gap-1">
                      <span className="material-symbols-outlined text-[14px]">lock</span>
                      Ticket đã đóng
                    </span>
                  )}
                </div>

                {/* Transition Select */}
                <div>
                  <label className="block text-[12px] font-medium text-on-surface mb-1">
                    Chuyển sang trạng thái:
                  </label>
                  <select
                    value={targetStatus}
                    onChange={(e) => setTargetStatus(e.target.value as TicketStatus)}
                    disabled={isClosed}
                    className="w-full px-3 py-2 rounded-lg border border-outline-variant/60 bg-surface font-body-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20 disabled:opacity-50 disabled:bg-surface-container"
                  >
                    {allowedStatuses.map((st) => (
                      <option key={st} value={st}>
                        {st === 'OPEN'
                          ? 'OPEN (Mới tạo)'
                          : st === 'IN_PROGRESS'
                          ? 'IN_PROGRESS (Đang xử lý / Tiếp nhận)'
                          : 'CLOSED (Đã giải quyết / Đóng ticket)'}
                      </option>
                    ))}
                  </select>
                  {selectedTicket.status === 'OPEN' && (
                    <p className="text-[11px] text-outline mt-1">
                      Gợi ý: Chọn "IN_PROGRESS" khi bắt đầu rà soát câu hỏi, hoặc "CLOSED" nếu giải quyết xong ngay.
                    </p>
                  )}
                </div>

                {/* Resolver Note */}
                <div>
                  <div className="flex items-center justify-between mb-1">
                    <label className="text-[12px] font-medium text-on-surface">
                      Ghi chú xử lý của Quản trị viên (resolverNote):
                    </label>
                    <span
                      className={`text-[11px] ${
                        resolverNote.length > 5000 ? 'text-rose-600 font-bold' : 'text-outline'
                      }`}
                    >
                      {resolverNote.length} / 5000 ký tự
                    </span>
                  </div>
                  <textarea
                    rows={4}
                    value={resolverNote}
                    onChange={(e) => setResolverNote(e.target.value)}
                    placeholder="Nhập nội dung giải trình, kết quả kiểm tra hoặc hướng xử lý cho thí sinh..."
                    className="w-full px-3 py-2 rounded-lg border border-outline-variant/60 bg-surface font-body-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20"
                  />
                </div>

                {/* Submit Action */}
                <div className="flex justify-end pt-2">
                  <button
                    type="submit"
                    disabled={submittingAction}
                    className="px-5 py-2.5 rounded-lg bg-primary text-on-primary font-label-md hover:bg-primary-container transition-colors disabled:opacity-50 flex items-center gap-2"
                  >
                    {submittingAction && (
                      <span className="material-symbols-outlined text-[18px] animate-spin">
                        progress_activity
                      </span>
                    )}
                    <span>{submittingAction ? 'Đang lưu...' : 'Lưu kết quả & Cập nhật'}</span>
                  </button>
                </div>
              </form>
            </div>
          ) : (
            <div className="rounded-xl bg-surface-container-lowest border border-outline-variant/30 p-12 text-center text-outline">
              <span className="material-symbols-outlined text-[48px] text-outline/40 block mx-auto mb-3">
                contact_support
              </span>
              <h3 className="font-headline-sm text-on-surface mb-1">Chưa chọn ticket nào</h3>
              <p className="font-body-sm max-w-sm mx-auto">
                Vui lòng chọn một ticket từ danh sách bên trái để xem nội dung báo lỗi, công thức toán và xử lý phản hồi.
              </p>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};

export default TicketsPage;
