import React, { useState, useEffect, useCallback } from 'react';
import type { UserSummaryItem, UserRole, UserStatus } from '../../services/api';
import { fetchUsers, lockUser, unlockUser, updateUserRole } from '../../services/api';

const ROLE_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'ALL', label: 'Tất cả vai trò' },
  { value: 'STUDENT', label: 'Học sinh (STUDENT)' },
  { value: 'COLLABORATOR', label: 'Cộng tác viên (COLLABORATOR)' },
  { value: 'CONTENT_ADMIN', label: 'Quản trị nội dung (CONTENT_ADMIN)' },
  { value: 'SUPER_ADMIN', label: 'Quản trị tối cao (SUPER_ADMIN)' }
];

const STATUS_OPTIONS: Array<{ value: string; label: string }> = [
  { value: 'ALL', label: 'Tất cả trạng thái' },
  { value: 'ACTIVE', label: 'Đang hoạt động (ACTIVE)' },
  { value: 'LOCKED', label: 'Đã khóa (LOCKED)' },
  { value: 'SUSPENDED', label: 'Tạm ngưng (SUSPENDED)' },
  { value: 'DEACTIVATED', label: 'Vô hiệu hóa (DEACTIVATED)' }
];

function getRoleBadge(role: UserRole) {
  switch (role) {
    case 'SUPER_ADMIN':
      return {
        label: 'SUPER ADMIN',
        className: 'bg-amber-100 text-amber-900 border-amber-300 dark:bg-amber-950 dark:text-amber-200'
      };
    case 'CONTENT_ADMIN':
      return {
        label: 'CONTENT ADMIN',
        className: 'bg-purple-100 text-purple-900 border-purple-300 dark:bg-purple-950 dark:text-purple-200'
      };
    case 'COLLABORATOR':
      return {
        label: 'CỘNG TÁC VIÊN',
        className: 'bg-cyan-100 text-cyan-900 border-cyan-300 dark:bg-cyan-950 dark:text-cyan-200'
      };
    case 'STUDENT':
    default:
      return {
        label: 'HỌC SINH',
        className: 'bg-slate-100 text-slate-800 border-slate-300 dark:bg-slate-800 dark:text-slate-200'
      };
  }
}

function getStatusBadge(status: UserStatus) {
  switch (status) {
    case 'ACTIVE':
      return {
        label: 'Đang hoạt động',
        className: 'bg-emerald-50 text-emerald-700 border-emerald-300 dark:bg-emerald-950 dark:text-emerald-300'
      };
    case 'LOCKED':
      return {
        label: 'Đã khóa',
        className: 'bg-rose-50 text-rose-700 border-rose-300 dark:bg-rose-950 dark:text-rose-300'
      };
    case 'SUSPENDED':
      return {
        label: 'Tạm ngưng',
        className: 'bg-amber-50 text-amber-700 border-amber-300 dark:bg-amber-950 dark:text-amber-300'
      };
    case 'DEACTIVATED':
    default:
      return {
        label: 'Vô hiệu hóa',
        className: 'bg-gray-100 text-gray-600 border-gray-300 dark:bg-gray-800 dark:text-gray-400'
      };
  }
}

function formatDate(isoString: string | null): string {
  if (!isoString) return 'Chưa đăng nhập';
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

export const UsersPage: React.FC = () => {
  const [users, setUsers] = useState<UserSummaryItem[]>([]);
  const [roleFilter, setRoleFilter] = useState<string>('ALL');
  const [statusFilter, setStatusFilter] = useState<string>('ALL');
  const [keyword, setKeyword] = useState<string>('');
  const [appliedKeyword, setAppliedKeyword] = useState<string>('');
  const [page, setPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

  // Error & Notification States
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Role Edit Modal
  const [editingUser, setEditingUser] = useState<UserSummaryItem | null>(null);
  const [selectedNewRole, setSelectedNewRole] = useState<UserRole>('STUDENT');

  const loadUsers = useCallback(async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const res = await fetchUsers({
        role: roleFilter,
        status: statusFilter,
        keyword: appliedKeyword.trim() || undefined,
        page,
        size: 20
      });
      setUsers(res.content);
      setTotalPages(res.totalPages || 1);
      setTotalElements(res.totalElements);
    } catch (err: any) {
      setErrorMessage(err?.message || 'Không thể tải danh sách người dùng từ máy chủ.');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  }, [roleFilter, statusFilter, appliedKeyword, page]);

  useEffect(() => {
    let ignore = false;
    const run = async () => {
      setLoading(true);
      setErrorMessage(null);
      try {
        const res = await fetchUsers({
          role: roleFilter,
          status: statusFilter,
          keyword: appliedKeyword.trim() || undefined,
          page,
          size: 20
        });
        if (!ignore) {
          setUsers(res.content);
          setTotalPages(res.totalPages || 1);
          setTotalElements(res.totalElements);
        }
      } catch (err: any) {
        if (!ignore) {
          setErrorMessage(err?.message || 'Không thể tải danh sách người dùng từ máy chủ.');
          setUsers([]);
        }
      } finally {
        if (!ignore) setLoading(false);
      }
    };
    run();
    return () => {
      ignore = true;
    };
  }, [roleFilter, statusFilter, appliedKeyword, page]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setAppliedKeyword(keyword);
  };

  const handleLockToggle = async (user: UserSummaryItem) => {
    const isLocking = user.status !== 'LOCKED';
    const actionLabel = isLocking ? 'khóa' : 'mở khóa';
    if (!window.confirm(`Bạn có chắc chắn muốn ${actionLabel} tài khoản "${user.full_name}" (${user.email})?`)) {
      return;
    }

    setActionLoadingId(user.id);
    setErrorMessage(null);
    setSuccessMessage(null);

    const res = isLocking ? await lockUser(user.id) : await unlockUser(user.id);
    setActionLoadingId(null);

    if (res.ok) {
      setSuccessMessage(res.message || `Đã ${actionLabel} tài khoản thành công.`);
      await loadUsers();
    } else {
      setErrorMessage(res.message || `Không thể ${actionLabel} tài khoản.`);
    }
  };

  const handleOpenRoleModal = (user: UserSummaryItem) => {
    setEditingUser(user);
    setSelectedNewRole(user.role);
    setErrorMessage(null);
    setSuccessMessage(null);
  };

  const handleSaveRole = async () => {
    if (!editingUser) return;
    if (editingUser.role === selectedNewRole) {
      setEditingUser(null);
      return;
    }

    setActionLoadingId(editingUser.id);
    setErrorMessage(null);
    setSuccessMessage(null);

    const res = await updateUserRole(editingUser.id, selectedNewRole);
    setActionLoadingId(null);

    if (res.ok) {
      setSuccessMessage(`Đã cập nhật vai trò của ${editingUser.full_name} thành ${selectedNewRole}.`);
      setEditingUser(null);
      await loadUsers();
    } else {
      setErrorMessage(res.message || 'Cập nhật vai trò thất bại.');
    }
  };

  return (
    <div className="p-margin space-y-6">
      {/* Page Title & Stats */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="font-headline-lg text-on-surface">Quản lý người dùng</h1>
          <p className="font-body-md text-outline mt-1">
            Phân quyền tài khoản (Super Admin), kiểm soát trạng thái đăng nhập và truy cập khảo thí.
          </p>
        </div>
        <div className="flex items-center gap-2">
          <button
            onClick={() => {
              setPage(0);
              loadUsers();
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

      {/* Error Banner */}
      {errorMessage && (
        <div className="p-4 rounded-xl bg-rose-50 border border-rose-300 text-rose-800 dark:bg-rose-950/40 dark:border-rose-800 dark:text-rose-200 flex items-start gap-3">
          <span className="material-symbols-outlined text-[22px] flex-shrink-0 text-rose-600">
            error
          </span>
          <div className="flex-1 text-body-md">
            <span className="font-bold">Lỗi thao tác: </span>
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

      {/* Success Banner */}
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

      {/* Filters Bar */}
      <div className="p-4 rounded-xl bg-surface-container-lowest border border-outline-variant/30 space-y-4">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 md:grid-cols-12 gap-3 items-center">
          {/* Keyword Search */}
          <div className="md:col-span-5 relative">
            <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-outline text-[18px]">
              search
            </span>
            <input
              type="text"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              placeholder="Tìm theo tên, email, số điện thoại..."
              className="w-full pl-9 pr-3 py-2 rounded-lg border border-outline-variant/60 bg-surface font-body-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20"
            />
          </div>

          {/* Role Filter */}
          <div className="md:col-span-3">
            <select
              value={roleFilter}
              onChange={(e) => {
                setRoleFilter(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg border border-outline-variant/60 bg-surface font-body-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              {ROLE_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Status Filter */}
          <div className="md:col-span-3">
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(0);
              }}
              className="w-full px-3 py-2 rounded-lg border border-outline-variant/60 bg-surface font-body-sm text-on-surface focus:outline-none focus:ring-2 focus:ring-primary/20"
            >
              {STATUS_OPTIONS.map((opt) => (
                <option key={opt.value} value={opt.value}>
                  {opt.label}
                </option>
              ))}
            </select>
          </div>

          {/* Submit Search */}
          <div className="md:col-span-1">
            <button
              type="submit"
              className="w-full py-2 bg-primary text-on-primary rounded-lg font-label-md hover:bg-primary-container transition-colors"
            >
              Lọc
            </button>
          </div>
        </form>
      </div>

      {/* Users Table */}
      <div className="rounded-xl bg-surface-container-lowest border border-outline-variant/30 overflow-hidden shadow-sm">
        <div className="overflow-x-auto">
          <table className="w-full text-left border-collapse">
            <thead>
              <tr className="border-b border-outline-variant/30 bg-surface-container-low/50 text-[11px] font-bold text-outline uppercase tracking-wider">
                <th className="py-3 px-4">Người dùng</th>
                <th className="py-3 px-4">Liên hệ</th>
                <th className="py-3 px-4">Vai trò</th>
                <th className="py-3 px-4">Trạng thái</th>
                <th className="py-3 px-4">Ngày tạo / Đăng nhập cuối</th>
                <th className="py-3 px-4 text-right">Thao tác</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/20 font-body-sm text-on-surface">
              {loading && users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-outline">
                    <span className="material-symbols-outlined text-[32px] animate-spin text-primary block mx-auto mb-2">
                      progress_activity
                    </span>
                    Đang tải dữ liệu người dùng...
                  </td>
                </tr>
              ) : users.length === 0 ? (
                <tr>
                  <td colSpan={6} className="py-12 text-center text-outline">
                    <span className="material-symbols-outlined text-[36px] text-outline/50 block mx-auto mb-2">
                      person_off
                    </span>
                    Không tìm thấy người dùng phù hợp với bộ lọc.
                  </td>
                </tr>
              ) : (
                users.map((user) => {
                  const roleBadge = getRoleBadge(user.role);
                  const statusBadge = getStatusBadge(user.status);
                  const isOperating = actionLoadingId === user.id;

                  return (
                    <tr key={user.id} className="hover:bg-surface-container-low/40 transition-colors">
                      {/* Name & Avatar */}
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-full bg-primary/10 text-primary flex items-center justify-center font-bold text-sm uppercase flex-shrink-0">
                            {user.avatar_url ? (
                              <img
                                src={user.avatar_url}
                                alt={user.full_name}
                                className="w-full h-full rounded-full object-cover"
                              />
                            ) : (
                              (user.full_name || 'U').charAt(0)
                            )}
                          </div>
                          <div>
                            <div className="font-semibold text-on-surface">{user.full_name}</div>
                            <div className="text-[11px] text-outline font-mono">ID: #{user.id}</div>
                          </div>
                        </div>
                      </td>

                      {/* Contact */}
                      <td className="py-3 px-4">
                        <div className="text-on-surface">{user.email}</div>
                        <div className="text-[12px] text-outline">{user.phone || '—'}</div>
                      </td>

                      {/* Role */}
                      <td className="py-3 px-4">
                        <span
                          className={`inline-block px-2.5 py-0.5 rounded-full text-[10px] font-bold tracking-wider border ${roleBadge.className}`}
                        >
                          {roleBadge.label}
                        </span>
                      </td>

                      {/* Status */}
                      <td className="py-3 px-4">
                        <span
                          className={`inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-medium border ${statusBadge.className}`}
                        >
                          <span
                            className={`w-1.5 h-1.5 rounded-full ${
                              user.status === 'ACTIVE'
                                ? 'bg-emerald-500'
                                : user.status === 'LOCKED'
                                ? 'bg-rose-500'
                                : 'bg-amber-500'
                            }`}
                          />
                          {statusBadge.label}
                        </span>
                      </td>

                      {/* Dates */}
                      <td className="py-3 px-4 text-[12px]">
                        <div>Tạo: {formatDate(user.created_at)}</div>
                        <div className="text-outline">Đăng nhập: {formatDate(user.last_login_at)}</div>
                      </td>

                      {/* Actions */}
                      <td className="py-3 px-4 text-right">
                        <div className="inline-flex items-center gap-2">
                          {/* Role Change */}
                          <button
                            onClick={() => handleOpenRoleModal(user)}
                            disabled={isOperating}
                            title="Đổi vai trò"
                            className="px-2.5 py-1 text-[12px] rounded border border-outline-variant/60 bg-surface hover:bg-surface-container font-medium text-on-surface transition-colors"
                          >
                            Đổi Role
                          </button>

                          {/* Lock / Unlock */}
                          <button
                            onClick={() => handleLockToggle(user)}
                            disabled={isOperating}
                            title={user.status === 'LOCKED' ? 'Mở khóa tài khoản' : 'Khóa tài khoản'}
                            className={`px-2.5 py-1 text-[12px] rounded border font-medium transition-colors ${
                              user.status === 'LOCKED'
                                ? 'border-emerald-300 bg-emerald-50 text-emerald-700 hover:bg-emerald-100'
                                : 'border-rose-300 bg-rose-50 text-rose-700 hover:bg-rose-100'
                            }`}
                          >
                            {isOperating
                              ? 'Đang xử lý...'
                              : user.status === 'LOCKED'
                              ? 'Mở khóa'
                              : 'Khóa'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>

        {/* Pagination Bar */}
        <div className="p-4 border-t border-outline-variant/30 flex flex-col sm:flex-row items-center justify-between gap-3 text-body-sm text-outline">
          <div>
            Hiển thị trang <span className="font-semibold text-on-surface">{page + 1}</span> / {totalPages} (Tổng cộng{' '}
            <span className="font-semibold text-on-surface">{totalElements}</span> người dùng)
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => setPage((prev) => Math.max(0, prev - 1))}
              disabled={page === 0 || loading}
              className="px-3 py-1.5 rounded border border-outline-variant/60 bg-surface font-label-md text-on-surface hover:bg-surface-container disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Trang trước
            </button>
            <button
              onClick={() => setPage((prev) => Math.min(totalPages - 1, prev + 1))}
              disabled={page >= totalPages - 1 || loading}
              className="px-3 py-1.5 rounded border border-outline-variant/60 bg-surface font-label-md text-on-surface hover:bg-surface-container disabled:opacity-40 disabled:cursor-not-allowed"
            >
              Trang sau
            </button>
          </div>
        </div>
      </div>

      {/* Role Change Modal */}
      {editingUser && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 backdrop-blur-xs p-4">
          <div className="w-full max-w-md bg-surface-container-lowest rounded-xl border border-outline-variant/40 shadow-xl overflow-hidden animate-in fade-in zoom-in-95 duration-150">
            <div className="p-5 border-b border-outline-variant/30">
              <h3 className="font-headline-sm text-on-surface">Cập nhật vai trò người dùng</h3>
              <p className="text-body-sm text-outline mt-1">
                Người dùng: <span className="font-semibold text-on-surface">{editingUser.full_name}</span> (ID: #{editingUser.id})
              </p>
            </div>
            <div className="p-5 space-y-4">
              <div>
                <label className="block text-[11px] font-bold text-outline uppercase tracking-wider mb-2">
                  Chọn vai trò mới
                </label>
                <div className="space-y-2">
                  {(['STUDENT', 'COLLABORATOR', 'CONTENT_ADMIN', 'SUPER_ADMIN'] as UserRole[]).map((r) => (
                    <label
                      key={r}
                      className={`flex items-center justify-between p-3 rounded-lg border cursor-pointer transition-colors ${
                        selectedNewRole === r
                          ? 'border-primary bg-primary/5 text-primary font-semibold'
                          : 'border-outline-variant/40 hover:bg-surface-container-low text-on-surface'
                      }`}
                    >
                      <div className="flex items-center gap-2">
                        <input
                          type="radio"
                          name="roleSelection"
                          value={r}
                          checked={selectedNewRole === r}
                          onChange={() => setSelectedNewRole(r)}
                          className="text-primary focus:ring-primary"
                        />
                        <span>{r}</span>
                      </div>
                      <span className="text-[11px] text-outline">
                        {r === 'SUPER_ADMIN'
                          ? 'Toàn quyền hệ thống'
                          : r === 'CONTENT_ADMIN'
                          ? 'Biên soạn & duyệt đề'
                          : r === 'COLLABORATOR'
                          ? 'Đóng góp câu hỏi'
                          : 'Luyện tập & thi thử'}
                      </span>
                    </label>
                  ))}
                </div>
              </div>

              <div className="p-3 rounded-lg bg-amber-50 border border-amber-200 text-amber-900 text-[12px] dark:bg-amber-950/30 dark:border-amber-800 dark:text-amber-200">
                <span className="font-bold">Lưu ý:</span> Không thể hạ quyền tài khoản nếu đây là Super Admin duy nhất còn lại trên hệ thống.
              </div>
            </div>
            <div className="p-4 bg-surface-container-low/50 border-t border-outline-variant/30 flex justify-end gap-2">
              <button
                type="button"
                onClick={() => setEditingUser(null)}
                className="px-4 py-2 rounded-lg border border-outline-variant/60 bg-surface font-label-md text-on-surface hover:bg-surface-container transition-colors"
              >
                Hủy bỏ
              </button>
              <button
                type="button"
                onClick={handleSaveRole}
                disabled={actionLoadingId === editingUser.id}
                className="px-4 py-2 rounded-lg bg-primary text-on-primary font-label-md hover:bg-primary-container transition-colors disabled:opacity-50"
              >
                {actionLoadingId === editingUser.id ? 'Đang lưu...' : 'Xác nhận cập nhật'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default UsersPage;
