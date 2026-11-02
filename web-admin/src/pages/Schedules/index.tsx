import React, { useState, useEffect } from 'react';
import type { ExamScheduleItem, ExamItem, ResultReleasePolicy, DeliveryMode } from '../../types/exam';
import { fetchSchedules, createSchedule, deleteSchedule, updateScheduleStatus, fetchExams } from '../../services/api';

export const SchedulesPage: React.FC = () => {
  const [schedules, setSchedules] = useState<ExamScheduleItem[]>([]);
  const [availableExams, setAvailableExams] = useState<ExamItem[]>([]);
  const [filterStatus, setFilterStatus] = useState<string>('ALL');
  const [showModal, setShowModal] = useState<boolean>(false);
  const [loading, setLoading] = useState<boolean>(true);
  const [submitting, setSubmitting] = useState<boolean>(false);

  // Form states
  const [selectedExamId, setSelectedExamId] = useState<number>(1);
  const [windowCode, setWindowCode] = useState<string>('');
  const [name, setName] = useState<string>('');
  const [mode, setMode] = useState<DeliveryMode>('SCHEDULED_MOCK');
  const [startAt, setStartAt] = useState<string>('2026-11-05T08:00');
  const [endAt, setEndAt] = useState<string>('2026-11-05T10:00');
  const [gracePeriodMinutes, setGracePeriodMinutes] = useState<number>(15);
  const [maxAttempts, setMaxAttempts] = useState<number>(1);
  const [resultPolicy, setResultPolicy] = useState<ResultReleasePolicy>('AFTER_WINDOW_FULL');

  const refreshSchedules = async () => {
    try {
      const data = await fetchSchedules();
      setSchedules(data);
    } catch (err) {
      console.error('Failed to load schedules', err);
    }
  };

  useEffect(() => {
    fetchSchedules().then(data => {
      setSchedules(data);
      setLoading(false);
    }).catch(() => setLoading(false));
    fetchExams().then(exams => {
      setAvailableExams(exams);
      if (exams.length > 0) {
        setSelectedExamId(exams[0].id);
        setWindowCode(`CA_VSAT_${exams[0].subjectId === 1 ? 'TOAN' : 'ENG'}_${Math.floor(100 + Math.random() * 900)}`);
        setName(`Ca thi Khảo sát ${exams[0].title}`);
      }
    });
  }, []);

  const handleExamChange = (examId: number) => {
    setSelectedExamId(examId);
    const ex = availableExams.find(e => e.id === examId);
    if (ex) {
      setWindowCode(`CA_VSAT_${ex.subjectId === 1 ? 'TOAN' : 'ENG'}_${Math.floor(100 + Math.random() * 900)}`);
      setName(`Ca thi Khảo sát ${ex.title}`);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    try {
      await createSchedule({
        examId: selectedExamId,
        windowCode: windowCode.trim(),
        title: name.trim(),
        mode,
        startAt: new Date(startAt).toISOString(),
        endAt: new Date(endAt).toISOString(),
        gracePeriodMinutes,
        maxAttempts,
        resultPolicy,
        hardStopAtEnd: true,
        allowOffline: false
      });
      setShowModal(false);
      await refreshSchedules();
    } catch (err: any) {
      alert(err?.message || 'Có lỗi xảy ra khi tạo ca thi');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (confirm('Bạn có chắc chắn muốn xóa ca thi này không?')) {
      try {
        await deleteSchedule(id);
        await refreshSchedules();
      } catch (err: any) {
        alert(err?.message || 'Không thể xóa ca thi');
      }
    }
  };

  const handleStatusChange = async (id: number, newStatus: 'UPCOMING' | 'RUNNING' | 'CLOSED') => {
    try {
      await updateScheduleStatus(id, newStatus);
      await refreshSchedules();
    } catch (err: any) {
      alert(err?.message || 'Không thể đổi trạng thái ca thi');
    }
  };

  const filtered = schedules.filter(s => {
    if (filterStatus === 'ALL') return true;
    return s.status === filterStatus;
  });

  const runningCount = schedules.filter(s => s.status === 'RUNNING').length;
  const totalCandidates = schedules.reduce((acc, s) => acc + (s.activeCandidates || 0), 0);
  const totalSubmissions = schedules.reduce((acc, s) => acc + (s.totalSubmissions || 0), 0);

  const formatDateTime = (iso: string) => {
    try {
      const d = new Date(iso);
      return d.toLocaleString('vi-VN', {
        hour: '2-digit',
        minute: '2-digit',
        day: '2-digit',
        month: '2-digit',
        year: 'numeric'
      });
    } catch {
      return iso;
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'RUNNING':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-[12px] font-bold bg-emerald-500/10 text-emerald-700 border border-emerald-500/30">
            <span className="w-2 h-2 rounded-full bg-emerald-600 animate-pulse"></span>
            Đang diễn ra
          </span>
        );
      case 'UPCOMING':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-[12px] font-bold bg-sky-500/10 text-sky-700 border border-sky-500/30">
            <span className="material-symbols-outlined text-[14px]">schedule</span>
            Sắp diễn ra
          </span>
        );
      case 'CLOSED':
        return (
          <span className="inline-flex items-center gap-1 px-3 py-1 rounded-full text-[12px] font-bold bg-surface-container-high text-on-surface-variant border border-outline-variant/30">
            <span className="material-symbols-outlined text-[14px]">lock</span>
            Đã kết thúc
          </span>
        );
      default:
        return null;
    }
  };

  const getPolicyLabel = (policy: ResultReleasePolicy) => {
    switch (policy) {
      case 'IMMEDIATE':
      case 'IMMEDIATE_FULL':
        return 'Công bố ngay khi nộp';
      case 'IMMEDIATE_SCORE':
        return 'Công bố ngay (Chỉ điểm)';
      case 'AFTER_WINDOW_CLOSED':
      case 'AFTER_WINDOW_FULL':
        return 'Sau khi đóng ca thi';
      case 'AFTER_WINDOW_SCORE':
        return 'Sau khi đóng ca (Chỉ điểm)';
      case 'MANUAL':
        return 'Duyệt công bố thủ công';
      default:
        return policy;
    }
  };

  return (
    <div className="p-margin max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/30 shadow-sm">
        <div>
          <h1 className="font-headline-lg text-on-surface flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[32px]">pending_actions</span>
            Ca thi & Lịch thi (Exam Windows)
          </h1>
          <p className="text-body-md text-outline mt-1">
            Thiết lập phòng thi trực tuyến, quản lý giờ mở/đóng ca và chính sách công bố điểm số V-SAT.
          </p>
        </div>

        <button
          onClick={() => setShowModal(true)}
          className="flex items-center gap-2 px-5 py-2.5 bg-primary text-on-primary hover:bg-primary/90 rounded-xl font-label-md font-semibold transition shadow-sm"
        >
          <span className="material-symbols-outlined text-[20px]">add_circle</span>
          Tạo ca thi mới
        </button>
      </div>

      {/* KPI Cards */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30 shadow-sm">
          <span className="font-label-sm uppercase text-outline">Tổng số ca thi</span>
          <div className="font-headline-lg font-bold text-on-surface mt-1">{schedules.length}</div>
        </div>

        <div className="bg-surface-container-lowest p-5 rounded-2xl border border-emerald-500/20 bg-emerald-500/5 shadow-sm">
          <span className="font-label-sm uppercase text-emerald-800">Đang diễn ra (Active)</span>
          <div className="font-headline-lg font-bold text-emerald-700 mt-1 flex items-center gap-2">
            {runningCount}
            <span className="w-2.5 h-2.5 rounded-full bg-emerald-600 animate-pulse"></span>
          </div>
        </div>

        <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30 shadow-sm">
          <span className="font-label-sm uppercase text-outline">Thí sinh đang thi</span>
          <div className="font-headline-lg font-bold text-primary mt-1">{totalCandidates}</div>
        </div>

        <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30 shadow-sm">
          <span className="font-label-sm uppercase text-outline">Lượt nộp bài</span>
          <div className="font-headline-lg font-bold text-on-surface mt-1">{totalSubmissions}</div>
        </div>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-2">
        {[
          { key: 'ALL', label: 'Tất cả ca thi' },
          { key: 'RUNNING', label: 'Đang mở (Active)' },
          { key: 'UPCOMING', label: 'Sắp diễn ra' },
          { key: 'CLOSED', label: 'Đã đóng' }
        ].map(tab => (
          <button
            key={tab.key}
            onClick={() => setFilterStatus(tab.key)}
            className={`px-4 py-2 rounded-xl font-label-md font-medium transition-colors border ${
              filterStatus === tab.key
                ? 'bg-primary-container text-on-primary-container border-primary font-semibold'
                : 'bg-surface-container-lowest text-on-surface-variant hover:bg-surface-container-high border-outline-variant/30'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* Schedule Table / Cards */}
      <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 overflow-hidden shadow-sm">
        <div className="p-4 border-b border-outline-variant/20 bg-surface-container-low/40 flex justify-between items-center">
          <h2 className="font-headline-sm text-on-surface">Danh sách Phòng thi & Ca thi ({filtered.length})</h2>
          <span className="text-body-sm text-outline">Đồng bộ tự động với máy chủ V-SAT API</span>
        </div>

        {loading ? (
          <div className="p-12 text-center text-outline flex items-center justify-center gap-2">
            <span className="w-4 h-4 border-2 border-primary border-t-transparent rounded-full animate-spin"></span>
            Đang tải dữ liệu ca thi từ máy chủ...
          </div>
        ) : filtered.length === 0 ? (
          <div className="p-12 text-center text-outline">
            Chưa có ca thi nào phù hợp với bộ lọc.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-body-sm border-collapse">
              <thead>
                <tr className="border-b border-outline-variant/20 bg-surface-container-low font-label-md text-on-surface-variant">
                  <th className="p-3.5 w-32">Trạng thái</th>
                  <th className="p-3.5 min-w-[240px]">Tên ca & Đề thi</th>
                  <th className="p-3.5 w-36">Chế độ</th>
                  <th className="p-3.5 min-w-[220px]">Khung giờ mở ca</th>
                  <th className="p-3.5 w-36">Trả điểm</th>
                  <th className="p-3.5 w-28 text-center">Thí sinh</th>
                  <th className="p-3.5 w-44 text-right">Thao tác</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-outline-variant/10">
                {filtered.map(item => (
                  <tr key={item.id} className="hover:bg-surface-container-low/30 transition-colors">
                    <td className="p-3.5">
                      {getStatusBadge(item.status)}
                    </td>
                    <td className="p-3.5 space-y-1">
                      <div className="font-semibold text-on-surface">{item.name || item.windowCode}</div>
                      <div className="text-[12px] text-outline flex items-center gap-2">
                        <span className="font-mono text-primary font-medium">[{item.windowCode}]</span>
                        <span>•</span>
                        <span>{item.examTitle || ('Đề #' + item.examId)}</span>
                      </div>
                    </td>
                    <td className="p-3.5">
                      <span className="px-2.5 py-1 rounded-full text-[11px] font-medium bg-surface-container-high text-on-surface">
                        {item.mode === 'PRACTICE' ? 'Luyện tập tự do' : 'Thi thử định kỳ'}
                      </span>
                    </td>
                    <td className="p-3.5 space-y-0.5 text-[12px]">
                      <div className="text-on-surface font-medium">
                        Từ: {formatDateTime(item.startAt)}
                      </div>
                      <div className="text-outline">
                        Đến: {formatDateTime(item.endAt)}
                      </div>
                      {item.gracePeriodMinutes > 0 && (
                        <div className="text-amber-700 font-mono text-[11px]">
                          Trễ tối đa: +{item.gracePeriodMinutes}p
                        </div>
                      )}
                    </td>
                    <td className="p-3.5 text-[12px]">
                      <span className="font-medium text-on-surface-variant">
                        {getPolicyLabel(item.resultPolicy)}
                      </span>
                    </td>
                    <td className="p-3.5 text-center">
                      <div className="font-bold text-primary">{item.activeCandidates || 0}</div>
                      <div className="text-[11px] text-outline">{item.totalSubmissions || 0} đã nộp</div>
                    </td>
                    <td className="p-3.5 text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {item.status === 'UPCOMING' && (
                          <button
                            onClick={() => handleStatusChange(item.id, 'RUNNING')}
                            className="px-2.5 py-1 bg-emerald-600 text-white rounded-lg text-[12px] font-semibold hover:bg-emerald-700 transition"
                            title="Mở ca thi ngay bây giờ"
                          >
                            Mở ca
                          </button>
                        )}
                        {item.status === 'RUNNING' && (
                          <button
                            onClick={() => handleStatusChange(item.id, 'CLOSED')}
                            className="px-2.5 py-1 bg-amber-600 text-white rounded-lg text-[12px] font-semibold hover:bg-amber-700 transition"
                            title="Đóng ca thi ngay"
                          >
                            Đóng ca
                          </button>
                        )}
                        {item.status === 'CLOSED' && (
                          <button
                            onClick={() => handleStatusChange(item.id, 'RUNNING')}
                            className="px-2.5 py-1 bg-surface-container-high text-on-surface rounded-lg text-[12px] font-semibold hover:bg-surface-container-highest transition"
                            title="Mở lại ca thi"
                          >
                            Mở lại
                          </button>
                        )}
                        <button
                          onClick={() => handleDelete(item.id)}
                          className="p-1 text-outline hover:text-error transition rounded-lg hover:bg-error-container/20"
                          title="Xóa ca thi"
                        >
                          <span className="material-symbols-outlined text-[18px]">delete</span>
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Modal Tạo Ca Thi */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm p-4">
          <div className="bg-surface-container-lowest w-full max-w-xl rounded-2xl border border-outline-variant/30 shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="p-5 border-b border-outline-variant/20 flex justify-between items-center bg-surface-container-low/40">
              <h2 className="font-headline-sm text-on-surface flex items-center gap-2">
                <span className="material-symbols-outlined text-primary">add_circle</span>
                Tạo Ca thi / Phòng thi mới
              </h2>
              <button
                onClick={() => setShowModal(false)}
                className="text-outline hover:text-on-surface transition"
              >
                <span className="material-symbols-outlined">close</span>
              </button>
            </div>

            <form onSubmit={handleCreate} className="p-6 space-y-4">
              <div>
                <label className="font-label-md text-on-surface block mb-1">Đề thi áp dụng *</label>
                <select
                  value={selectedExamId}
                  onChange={(e) => handleExamChange(Number(e.target.value))}
                  className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-body-sm text-on-surface"
                >
                  {availableExams.map(ex => (
                    <option key={ex.id} value={ex.id}>
                      {ex.title} ({ex.examCode})
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-label-md text-on-surface block mb-1">Mã ca thi *</label>
                  <input
                    type="text"
                    required
                    value={windowCode}
                    onChange={(e) => setWindowCode(e.target.value)}
                    className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-mono text-[13px] text-on-surface"
                  />
                </div>
                <div>
                  <label className="font-label-md text-on-surface block mb-1">Chế độ thi *</label>
                  <select
                    value={mode}
                    onChange={(e) => setMode(e.target.value as DeliveryMode)}
                    className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-body-sm text-on-surface"
                  >
                    <option value="SCHEDULED_MOCK">Thi thử theo lịch (SCHEDULED)</option>
                    <option value="PRACTICE">Luyện tập tự do (PRACTICE)</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="font-label-md text-on-surface block mb-1">Tên hiển thị phòng thi *</label>
                <input
                  type="text"
                  required
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-body-sm text-on-surface"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-label-md text-on-surface block mb-1">Thời gian mở ca *</label>
                  <input
                    type="datetime-local"
                    required
                    value={startAt}
                    onChange={(e) => setStartAt(e.target.value)}
                    className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-body-sm text-on-surface"
                  />
                </div>
                <div>
                  <label className="font-label-md text-on-surface block mb-1">Thời gian đóng ca *</label>
                  <input
                    type="datetime-local"
                    required
                    value={endAt}
                    onChange={(e) => setEndAt(e.target.value)}
                    className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-body-sm text-on-surface"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-label-md text-on-surface block mb-1">Bù giờ trễ tối đa (phút)</label>
                  <input
                    type="number"
                    min={0}
                    max={60}
                    value={gracePeriodMinutes}
                    onChange={(e) => setGracePeriodMinutes(Number(e.target.value))}
                    className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-body-sm text-on-surface"
                  />
                </div>
                <div>
                  <label className="font-label-md text-on-surface block mb-1">Số lượt thi tối đa</label>
                  <input
                    type="number"
                    min={1}
                    max={10}
                    value={maxAttempts}
                    onChange={(e) => setMaxAttempts(Number(e.target.value))}
                    className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-body-sm text-on-surface"
                  />
                </div>
              </div>

              <div>
                <label className="font-label-md text-on-surface block mb-1">Chính sách công bố kết quả</label>
                <select
                  value={resultPolicy}
                  onChange={(e) => setResultPolicy(e.target.value as ResultReleasePolicy)}
                  className="w-full p-2.5 bg-surface-container-low border border-outline-variant/40 rounded-xl font-body-sm text-on-surface"
                >
                  <option value="AFTER_WINDOW_FULL">Sau khi đóng ca thi (Đầy đủ đáp án & giải thích)</option>
                  <option value="IMMEDIATE_FULL">Công bố ngay khi nộp bài (Đầy đủ)</option>
                  <option value="AFTER_WINDOW_SCORE">Sau khi đóng ca thi (Chỉ hiển thị điểm số)</option>
                  <option value="MANUAL">Chờ quản trị viên duyệt công bố thủ công</option>
                </select>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-outline-variant/20">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 text-outline hover:text-on-surface font-label-md transition"
                >
                  Hủy bỏ
                </button>
                <button
                  type="submit"
                  disabled={submitting}
                  className="px-6 py-2 bg-primary text-on-primary hover:bg-primary/90 disabled:opacity-50 rounded-xl font-label-md font-semibold transition shadow-sm"
                >
                  {submitting ? 'Đang lưu...' : 'Lưu ca thi'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
