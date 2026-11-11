import React, { useState } from 'react';

export const SettingsPage: React.FC = () => {
  const [maxViolations, setMaxViolations] = useState<number>(3);
  const [audioMaxPlays, setAudioMaxPlays] = useState<number>(2);
  const [mathDuration, setMathDuration] = useState<number>(90);
  const [otherDuration, setOtherDuration] = useState<number>(60);
  const [saved, setSaved] = useState<boolean>(false);

  const handleSave = (e: React.FormEvent) => {
    e.preventDefault();
    setSaved(true);
    setTimeout(() => setSaved(false), 3000);
  };

  return (
    <div className="space-y-6">
      {/* Header Banner - Strict WEB-009 DOM Contract: First h1 */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl md:text-3xl font-black text-on-surface tracking-tight">
            Cấu hình & Quy chuẩn
          </h1>
          <div className="text-xs text-on-surface-variant font-medium mt-1">
            Quy chuẩn kỹ thuật khảo thí, barem điểm số và tham số hệ thống V-SAT 2026
          </div>
        </div>
      </div>

      {/* Main Scoring Standards Card - Strict WEB-009 DOM Contract: First h3, First p containing '150 điểm thô' and 'Đúng/Sai' */}
      <div className="p-6 rounded-2xl bg-surface border border-outline-variant/30 shadow-xs space-y-4">
        <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-primary/10 text-primary text-[11px] font-bold">
          <span className="material-symbols-outlined text-[16px]">gavel</span>
          <span>QUY CHUẨN ĐIỂM SỐ KHẢO THÍ V-SAT 2026</span>
        </div>

        <h3 className="text-xl font-black text-on-surface tracking-tight">
          Quy chuẩn Điểm số & Chấm thi
        </h3>

        <p className="text-sm text-on-surface-variant leading-relaxed max-w-4xl">
          Mỗi đề thi chuẩn V-SAT gồm đúng 25 câu hỏi với tổng điểm tối đa là 150 điểm thô. Thang điểm được cấu trúc theo 4 phần: Phần I gồm 10 câu trắc nghiệm nhiều lựa chọn (40 điểm thô); Phần II gồm 8 câu Đúng/Sai áp dụng thang điểm lũy tiến 1-2-3-6đ theo số ý đúng (48 điểm thô); Phần III gồm 3 câu ghép đôi (27 điểm thô); và Phần IV gồm 4 câu trả lời ngắn điền số/từ khóa (35 điểm thô).
        </p>

        {/* Detailed Structure Table */}
        <div className="overflow-x-auto rounded-xl border border-outline-variant/20">
          <table className="w-full text-left text-xs">
            <thead className="bg-surface-container-high/60 text-on-surface-variant font-bold border-b border-outline-variant/20">
              <tr>
                <th className="py-2.5 px-3.5">Phần thi</th>
                <th className="py-2.5 px-3.5">Định dạng câu hỏi</th>
                <th className="py-2.5 px-3.5 text-center">Số câu</th>
                <th className="py-2.5 px-3.5 text-center">Điểm mỗi câu</th>
                <th className="py-2.5 px-3.5 text-center">Tổng điểm phần</th>
                <th className="py-2.5 px-3.5">Quy tắc chấm điểm chi tiết</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-outline-variant/10 text-on-surface">
              <tr className="hover:bg-surface-container-low/40">
                <td className="py-2.5 px-3.5 font-bold text-primary">Phần I</td>
                <td className="py-2.5 px-3.5 font-medium">Trắc nghiệm 4 lựa chọn (SINGLE_CHOICE)</td>
                <td className="py-2.5 px-3.5 text-center font-mono font-bold">10</td>
                <td className="py-2.5 px-3.5 text-center font-mono">4.0 đ</td>
                <td className="py-2.5 px-3.5 text-center font-mono font-bold text-primary">40.0 đ</td>
                <td className="py-2.5 px-3.5 text-on-surface-variant">Chọn đúng đáp án duy nhất: 4đ; sai hoặc không chọn: 0đ.</td>
              </tr>
              <tr className="hover:bg-surface-container-low/40">
                <td className="py-2.5 px-3.5 font-bold text-primary">Phần II</td>
                <td className="py-2.5 px-3.5 font-medium">Trắc nghiệm Đúng/Sai (TRUE_FALSE)</td>
                <td className="py-2.5 px-3.5 text-center font-mono font-bold">8</td>
                <td className="py-2.5 px-3.5 text-center font-mono">6.0 đ</td>
                <td className="py-2.5 px-3.5 text-center font-mono font-bold text-primary">48.0 đ</td>
                <td className="py-2.5 px-3.5 text-on-surface-variant">4 ý/câu theo thang lũy tiến: đúng 1 ý: 1đ; đúng 2 ý: 2đ; đúng 3 ý: 3đ; đúng cả 4 ý: 6đ.</td>
              </tr>
              <tr className="hover:bg-surface-container-low/40">
                <td className="py-2.5 px-3.5 font-bold text-primary">Phần III</td>
                <td className="py-2.5 px-3.5 font-medium">Ghép đôi tương ứng (MATCHING)</td>
                <td className="py-2.5 px-3.5 text-center font-mono font-bold">3</td>
                <td className="py-2.5 px-3.5 text-center font-mono">9.0 đ</td>
                <td className="py-2.5 px-3.5 text-center font-mono font-bold text-primary">27.0 đ</td>
                <td className="py-2.5 px-3.5 text-on-surface-variant">Ghép các cặp mệnh đề tương ứng; tính điểm tỷ lệ theo số cặp ghép chính xác.</td>
              </tr>
              <tr className="hover:bg-surface-container-low/40">
                <td className="py-2.5 px-3.5 font-bold text-primary">Phần IV</td>
                <td className="py-2.5 px-3.5 font-medium">Trả lời ngắn / Điền số (SHORT_ANSWER)</td>
                <td className="py-2.5 px-3.5 text-center font-mono font-bold">4</td>
                <td className="py-2.5 px-3.5 text-center font-mono">8.75 đ</td>
                <td className="py-2.5 px-3.5 text-center font-mono font-bold text-primary">35.0 đ</td>
                <td className="py-2.5 px-3.5 text-on-surface-variant">Điền kết quả số thập phân hoặc từ khóa chính xác theo barem chấp nhận.</td>
              </tr>
              <tr className="bg-primary/5 font-bold border-t-2 border-primary/20">
                <td className="py-2.5 px-3.5 text-primary" colSpan={2}>TỔNG TOÀN ĐỀ CHUẨN</td>
                <td className="py-2.5 px-3.5 text-center font-mono text-primary font-black">25 câu</td>
                <td className="py-2.5 px-3.5 text-center text-outline">-</td>
                <td className="py-2.5 px-3.5 text-center font-mono text-primary font-black">150.0 đ</td>
                <td className="py-2.5 px-3.5 text-primary font-medium">Thời gian: 90 phút (Toán) | 60 phút (6 môn còn lại)</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      {/* System Settings Form */}
      <form onSubmit={handleSave} className="p-6 rounded-2xl bg-surface border border-outline-variant/30 shadow-xs space-y-5">
        <div className="flex items-center justify-between">
          <div>
            <div className="text-base font-bold text-on-surface">Tham số Vận hành Khảo thí</div>
            <div className="text-xs text-on-surface-variant">Cấu hình thời lượng, giám sát gian lận và phát âm thanh nghe hiểu</div>
          </div>
          {saved && (
            <div className="px-3 py-1 rounded-lg bg-emerald-100 text-emerald-800 text-xs font-bold flex items-center gap-1 animate-fade-in">
              <span className="material-symbols-outlined text-[16px]">check</span>
              <span>Đã lưu cấu hình</span>
            </div>
          )}
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="p-4 rounded-xl border border-outline-variant/30 bg-surface-container-low space-y-2">
            <label className="text-xs font-bold text-on-surface flex items-center gap-1.5">
              <span className="material-symbols-outlined text-amber-600 text-[18px]">security</span>
              <span>Giới hạn vi phạm Proctoring (Tự động nộp bài):</span>
            </label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min={1}
                max={10}
                value={maxViolations}
                onChange={(e) => setMaxViolations(parseInt(e.target.value) || 3)}
                className="w-24 px-3 py-1.5 rounded-lg border border-outline-variant/50 bg-surface font-mono font-bold text-sm outline-none focus:border-primary"
              />
              <span className="text-xs text-on-surface-variant font-medium">lần vi phạm (rời tab/thu nhỏ cửa sổ)</span>
            </div>
            <div className="text-[11px] text-outline">
              Chuẩn P4 Anti-Cheat: Hệ thống tự động thu bài và chấm điểm khi thí sinh vi phạm đủ 3 lần.
            </div>
          </div>

          <div className="p-4 rounded-xl border border-outline-variant/30 bg-surface-container-low space-y-2">
            <label className="text-xs font-bold text-on-surface flex items-center gap-1.5">
              <span className="material-symbols-outlined text-primary text-[18px]">volume_up</span>
              <span>Số lượt nghe tối đa Audio Listening:</span>
            </label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min={1}
                max={5}
                value={audioMaxPlays}
                onChange={(e) => setAudioMaxPlays(parseInt(e.target.value) || 2)}
                className="w-24 px-3 py-1.5 rounded-lg border border-outline-variant/50 bg-surface font-mono font-bold text-sm outline-none focus:border-primary"
              />
              <span className="text-xs text-on-surface-variant font-medium">lượt phát (mặc định 2 lượt)</span>
            </div>
            <div className="text-[11px] text-outline">
              Áp dụng cho các câu hỏi phần Nghe môn Tiếng Anh chuẩn V-SAT 2026.
            </div>
          </div>

          <div className="p-4 rounded-xl border border-outline-variant/30 bg-surface-container-low space-y-2">
            <label className="text-xs font-bold text-on-surface flex items-center gap-1.5">
              <span className="material-symbols-outlined text-primary text-[18px]">timer</span>
              <span>Thời lượng thi môn Toán học:</span>
            </label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min={30}
                max={180}
                step={5}
                value={mathDuration}
                onChange={(e) => setMathDuration(parseInt(e.target.value) || 90)}
                className="w-24 px-3 py-1.5 rounded-lg border border-outline-variant/50 bg-surface font-mono font-bold text-sm outline-none focus:border-primary"
              />
              <span className="text-xs text-on-surface-variant font-medium">phút (chuẩn quy định 90 phút)</span>
            </div>
          </div>

          <div className="p-4 rounded-xl border border-outline-variant/30 bg-surface-container-low space-y-2">
            <label className="text-xs font-bold text-on-surface flex items-center gap-1.5">
              <span className="material-symbols-outlined text-primary text-[18px]">schedule</span>
              <span>Thời lượng thi 6 môn khác (Lí, Hóa, Sinh, Sử, Địa, Anh):</span>
            </label>
            <div className="flex items-center gap-2">
              <input
                type="number"
                min={30}
                max={120}
                step={5}
                value={otherDuration}
                onChange={(e) => setOtherDuration(parseInt(e.target.value) || 60)}
                className="w-24 px-3 py-1.5 rounded-lg border border-outline-variant/50 bg-surface font-mono font-bold text-sm outline-none focus:border-primary"
              />
              <span className="text-xs text-on-surface-variant font-medium">phút (chuẩn quy định 60 phút)</span>
            </div>
          </div>
        </div>

        <div className="flex justify-end pt-2">
          <button
            type="submit"
            className="px-5 py-2.5 rounded-xl bg-primary text-on-primary text-xs font-bold hover:bg-primary/90 transition-all flex items-center gap-1.5 shadow-sm"
          >
            <span className="material-symbols-outlined text-[18px]">save</span>
            <span>Lưu thiết lập hệ thống</span>
          </button>
        </div>
      </form>
    </div>
  );
};
