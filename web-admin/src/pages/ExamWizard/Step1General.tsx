import React from 'react';
import type { ExamDetail } from '../../types/exam';

interface Step1Props {
  exam: ExamDetail;
  onChange: (updated: Partial<ExamDetail>) => void;
}

export const Step1General: React.FC<Step1Props> = ({ exam, onChange }) => {
  return (
    <div className="space-y-space-lg">
      <div className="bg-surface-container-lowest p-space-lg rounded-xl border border-outline-variant/30 shadow-sm space-y-space-md">
        <div className="border-b border-outline-variant/20 pb-space-sm flex items-center justify-between">
          <div className="flex items-center gap-space-sm">
            <span className="material-symbols-outlined text-primary text-[22px]">tune</span>
            <h2 className="font-headline-sm text-on-surface">1. Thông tin chung & Quy chuẩn đề thi</h2>
          </div>
          <span className="px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-primary-fixed text-on-primary-fixed">
            Quy chuẩn V-SAT 2026
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-space-md">
          <div className="space-y-1">
            <label className="font-label-md text-on-surface">Tên đề thi *</label>
            <input
              type="text"
              value={exam.title}
              onChange={(e) => onChange({ title: e.target.value })}
              placeholder="VD: Đề thi thử V-SAT Toán học - Khóa 2026 số 01"
              className="w-full h-10 px-3 text-body-md bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
            />
          </div>

          <div className="space-y-1">
            <label className="font-label-md text-on-surface">Mã đề (Exam Code) *</label>
            <input
              type="text"
              value={exam.examCode}
              onChange={(e) => onChange({ examCode: e.target.value })}
              placeholder="VD: VSAT_2026_TOAN_MOCK_01"
              className="w-full h-10 px-3 text-body-md font-mono bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
            />
          </div>

          <div className="space-y-1">
            <label className="font-label-md text-on-surface">Môn thi *</label>
            <select
              value={exam.subjectId}
              onChange={(e) => {
                const subId = Number(e.target.value);
                const isMath = subId === 1;
                onChange({
                  subjectId: subId,
                  durationMinutes: isMath ? 90 : 60,
                  subjectName: isMath ? 'Toán học' : (subId === 2 ? 'Tiếng Anh' : 'Vật lí')
                });
              }}
              className="w-full h-10 px-3 text-body-md bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
            >
              <option value={1}>Toán học (Chuẩn 90 phút, 25 câu)</option>
              <option value={2}>Tiếng Anh (Chuẩn 60 phút, 25 câu)</option>
              <option value={3}>Vật lí (Chuẩn 60 phút, 25 câu)</option>
              <option value={4}>Hóa học (Chuẩn 60 phút, 25 câu)</option>
              <option value={5}>Sinh học (Chuẩn 60 phút, 25 câu)</option>
              <option value={6}>Lịch sử (Chuẩn 60 phút, 25 câu)</option>
              <option value={7}>Địa lí (Chuẩn 60 phút, 25 câu)</option>
            </select>
          </div>

          <div className="space-y-1">
            <label className="font-label-md text-on-surface">Quy chuẩn chấm điểm (Scoring Profile)</label>
            <select
              value={exam.scoringProfileCode || 'VSAT_2026_RAW_150'}
              onChange={(e) => onChange({ scoringProfileCode: e.target.value })}
              className="w-full h-10 px-3 text-body-md bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
            >
              <option value="VSAT_2026_RAW_150">V-SAT 2026: Điểm thô tối đa 150 (Đ/S lũy tiến 0-1-2-3-6đ)</option>
            </select>
          </div>
        </div>

        <div className="space-y-1">
          <label className="font-label-md text-on-surface">Mô tả đề thi</label>
          <textarea
            rows={2}
            value={exam.description || ''}
            onChange={(e) => onChange({ description: e.target.value })}
            placeholder="Mô tả mục tiêu kiểm tra, phạm vi kiến thức chuyên sâu..."
            className="w-full p-3 text-body-md bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
          />
        </div>
      </div>

      {/* Cấu hình thời gian & Ca thi */}
      <div className="bg-surface-container-lowest p-space-lg rounded-xl border border-outline-variant/30 shadow-sm space-y-space-md">
        <div className="border-b border-outline-variant/20 pb-space-sm flex items-center justify-between">
          <div className="flex items-center gap-space-sm">
            <span className="material-symbols-outlined text-primary text-[22px]">schedule</span>
            <h2 className="font-headline-sm text-on-surface">2. Thời lượng & Lịch trình ca thi</h2>
          </div>
          <span className="font-mono text-body-sm text-outline">
            Thời lượng: {exam.durationMinutes} phút
          </span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-space-md">
          <div className="space-y-2">
            <label className="font-label-md text-on-surface">Chế độ tổ chức thi *</label>
            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => onChange({ deliveryMode: 'PRACTICE' })}
                className={`p-3 rounded-lg border text-left flex flex-col gap-1 transition-all ${
                  exam.deliveryMode !== 'SCHEDULED_MOCK'
                    ? 'border-primary bg-primary/5 text-primary font-bold'
                    : 'border-outline-variant/40 bg-surface-container-low text-on-surface-variant'
                }`}
              >
                <span className="font-label-md">Luyện tập tự do</span>
                <span className="text-[11px] font-normal opacity-80">Học sinh làm bài bất kỳ lúc nào, xem giải thích ngay</span>
              </button>

              <button
                type="button"
                onClick={() => onChange({ deliveryMode: 'SCHEDULED_MOCK' })}
                className={`p-3 rounded-lg border text-left flex flex-col gap-1 transition-all ${
                  exam.deliveryMode === 'SCHEDULED_MOCK'
                    ? 'border-primary bg-primary/5 text-primary font-bold'
                    : 'border-outline-variant/40 bg-surface-container-low text-on-surface-variant'
                }`}
              >
                <span className="font-label-md">Thi thử theo ca</span>
                <span className="text-[11px] font-normal opacity-80">Mở phòng theo khung giờ cố định, đồng bộ nộp bài</span>
              </button>
            </div>
          </div>

          <div className="space-y-1">
            <label className="font-label-md text-on-surface">Thời gian làm bài (phút)</label>
            <input
              type="number"
              value={exam.durationMinutes}
              onChange={(e) => onChange({ durationMinutes: Number(e.target.value) })}
              className="w-full h-10 px-3 text-body-md font-mono bg-surface-container-low rounded-lg border border-outline-variant/40 focus:outline-none focus:border-primary"
            />
          </div>
        </div>

        {exam.deliveryMode === 'SCHEDULED_MOCK' && (
          <div className="p-space-md rounded-lg bg-surface-container-low border border-outline-variant/30 grid grid-cols-1 md:grid-cols-3 gap-space-md animate-fadeIn">
            <div className="space-y-1">
              <label className="font-label-sm text-outline">Thời gian mở ca thi (Start)</label>
              <input
                type="datetime-local"
                defaultValue="2026-10-20T08:00"
                className="w-full h-9 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40"
              />
            </div>
            <div className="space-y-1">
              <label className="font-label-sm text-outline">Thời gian đóng ca thi (End)</label>
              <input
                type="datetime-local"
                defaultValue="2026-10-20T09:30"
                className="w-full h-9 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40"
              />
            </div>
            <div className="space-y-1">
              <label className="font-label-sm text-outline">Trễ giờ tối đa (Grace period)</label>
              <input
                type="number"
                defaultValue={15}
                className="w-full h-9 px-2 text-body-sm bg-surface-container-lowest rounded border border-outline-variant/40"
              />
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

