import React, { useState, useEffect, useCallback } from 'react';
import type { AdmissionPredictionItem, AdmissionPredictionResponse, AdmissionTier } from '../../types/student-cbt';
import { predictUniversityAdmission } from '../../services/api';

interface AdmissionPredictorWidgetProps {
  initialScore?: number;
  initialCombo?: string;
  title?: string;
  subtitle?: string;
  compact?: boolean;
}

const COMBOS = [
  { key: 'ALL', label: 'Tất cả tổ hợp' },
  { key: 'A00', label: 'A00 (Toán, Lí, Hóa)' },
  { key: 'A01', label: 'A01 (Toán, Lí, Anh)' },
  { key: 'D01', label: 'D01 (Toán, Văn, Anh)' },
  { key: 'B00', label: 'B00 (Toán, Hóa, Sinh)' },
  { key: 'C00', label: 'C00 (Văn, Sử, Địa)' }
];

const UNIVERSITIES = [
  { code: 'ALL', name: 'Tất cả trường ĐH' },
  { code: 'HUB', name: 'HUB - ĐH Ngân hàng TP.HCM' },
  { code: 'UEL', name: 'UEL - ĐH Kinh tế - Luật (ĐHQG)' },
  { code: 'CTU', name: 'CTU - ĐH Cần Thơ' },
  { code: 'UFM', name: 'UFM - ĐH Tài chính - Marketing' },
  { code: 'HCMUS', name: 'HCMUS - ĐH Khoa học Tự nhiên' },
  { code: 'SGU', name: 'SGU - ĐH Sài Gòn' }
];

export const AdmissionPredictorWidget: React.FC<AdmissionPredictorWidgetProps> = ({
  initialScore = 315,
  initialCombo = 'ALL',
  title = 'Dự đoán Trúng tuyển Đại học (Admission Matching Engine)',
  subtitle = 'Phân tích xác suất trúng tuyển theo chuẩn kỳ thi V-SAT 2026 của HUB, UEL, CTU, UFM, HCMUS, SGU',
  compact = false
}) => {
  const [totalScore, setTotalScore] = useState<number>(initialScore);
  const [selectedCombo, setSelectedCombo] = useState<string>(initialCombo);
  const [selectedUni, setSelectedUni] = useState<string>('ALL');
  const [activeTierTab, setActiveTierTab] = useState<'ALL' | AdmissionTier>('ALL');
  const [loading, setLoading] = useState<boolean>(false);
  const [data, setData] = useState<AdmissionPredictionResponse | null>(null);

  const runPrediction = useCallback(async (scoreToPredict: number, comboToPredict: string, uniToPredict: string) => {
    setLoading(true);
    try {
      const res = await predictUniversityAdmission({
        totalScore: scoreToPredict,
        subjectCombo: comboToPredict !== 'ALL' ? comboToPredict : undefined,
        universityCode: uniToPredict !== 'ALL' ? uniToPredict : undefined
      });
      setData(res);
    } catch (err: unknown) {
      console.warn('Prediction error:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    let isCancelled = false;
    predictUniversityAdmission({
      totalScore,
      subjectCombo: selectedCombo !== 'ALL' ? selectedCombo : undefined,
      universityCode: selectedUni !== 'ALL' ? selectedUni : undefined
    }).then(res => {
      if (!isCancelled) {
        setData(res);
      }
    }).catch(err => {
      if (!isCancelled) {
        console.warn('Prediction error:', err);
      }
    });
    return () => {
      isCancelled = true;
    };
  }, [totalScore, selectedCombo, selectedUni]);

  const handleScoreChange = (val: number) => {
    const clamped = Math.max(0, Math.min(450, val));
    setTotalScore(clamped);
  };

  const handleApplyPredict = () => {
    runPrediction(totalScore, selectedCombo, selectedUni);
  };

  const filteredItems = (data?.items || []).filter(item => {
    if (activeTierTab === 'ALL') return true;
    return item.tier === activeTierTab;
  });

  return (
    <div className={`rounded-2xl border border-outline-variant/40 bg-surface-container-lowest shadow-sm space-y-5 ${compact ? 'p-4' : 'p-6'}`}>
      {/* Widget Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-outline-variant/20 pb-4">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full bg-primary/10 text-primary text-[11px] font-bold">
            <span className="material-symbols-outlined text-[16px]">school</span>
            <span>HỆ THỐNG DỰ ĐOÁN XÉT TUYỂN V-SAT</span>
          </div>
          <h2 className="font-headline-sm font-bold text-lg md:text-xl text-on-surface">
            {title}
          </h2>
          {subtitle && (
            <p className="text-xs text-on-surface-variant max-w-2xl leading-relaxed">
              {subtitle}
            </p>
          )}
        </div>

        <div className="text-right flex sm:flex-col items-center sm:items-end justify-between gap-1">
          <span className="text-[11px] text-outline font-medium">Tổng điểm xét tuyển</span>
          <span className="font-mono text-xl md:text-2xl font-black text-primary">
            {totalScore.toFixed(1)} <span className="text-xs text-outline font-normal">/ 450</span>
          </span>
        </div>
      </div>

      {/* Input Form & Filters */}
      <div className="grid grid-cols-1 md:grid-cols-12 gap-3 p-4 rounded-xl bg-surface-container-low border border-outline-variant/20">
        {/* Score Input */}
        <div className="md:col-span-4 space-y-1.5">
          <label className="text-xs font-bold text-on-surface flex items-center justify-between">
            <span>Điểm V-SAT 3 môn:</span>
            <span className="text-[10px] text-outline">Thang 450 (150đ × 3)</span>
          </label>
          <div className="relative">
            <input
              type="number"
              min={0}
              max={450}
              step={0.5}
              value={totalScore}
              onChange={(e) => handleScoreChange(parseFloat(e.target.value) || 0)}
              className="w-full px-3.5 py-2 rounded-lg border border-outline-variant/50 bg-surface text-on-surface font-mono font-bold text-sm focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
            />
            <span className="absolute right-3 top-2 text-xs text-outline font-mono">điểm</span>
          </div>
          <div className="flex gap-1.5 pt-1">
            {[270, 300, 330, 360].map(s => (
              <button
                key={s}
                type="button"
                onClick={() => { setTotalScore(s); runPrediction(s, selectedCombo, selectedUni); }}
                className="px-2 py-0.5 rounded text-[10px] font-mono font-semibold bg-surface border border-outline-variant/30 text-on-surface-variant hover:bg-surface-container-high"
              >
                {s}đ
              </button>
            ))}
          </div>
        </div>

        {/* Combo Select */}
        <div className="md:col-span-3 space-y-1.5">
          <label className="text-xs font-bold text-on-surface">Tổ hợp môn:</label>
          <select
            value={selectedCombo}
            onChange={(e) => setSelectedCombo(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-outline-variant/50 bg-surface text-on-surface text-xs font-semibold focus:border-primary outline-none transition-all"
          >
            {COMBOS.map(c => (
              <option key={c.key} value={c.key}>{c.label}</option>
            ))}
          </select>
        </div>

        {/* University Select */}
        <div className="md:col-span-3 space-y-1.5">
          <label className="text-xs font-bold text-on-surface">Trường Đại học:</label>
          <select
            value={selectedUni}
            onChange={(e) => setSelectedUni(e.target.value)}
            className="w-full px-3 py-2 rounded-lg border border-outline-variant/50 bg-surface text-on-surface text-xs font-semibold focus:border-primary outline-none transition-all"
          >
            {UNIVERSITIES.map(u => (
              <option key={u.code} value={u.code}>{u.name}</option>
            ))}
          </select>
        </div>

        {/* Run Button */}
        <div className="md:col-span-2 flex items-end">
          <button
            type="button"
            onClick={handleApplyPredict}
            disabled={loading}
            className="w-full py-2 px-3 rounded-lg bg-primary text-on-primary text-xs font-bold hover:bg-primary/90 transition-all flex items-center justify-center gap-1 shadow-sm disabled:opacity-50"
          >
            {loading ? (
              <span className="material-symbols-outlined text-[16px] animate-spin">sync</span>
            ) : (
              <span className="material-symbols-outlined text-[16px]">insights</span>
            )}
            <span>Phân tích</span>
          </button>
        </div>
      </div>

      {/* 3 Color Tier Summary Tabs */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
        <button
          type="button"
          onClick={() => setActiveTierTab('ALL')}
          className={`p-3 rounded-xl border text-left transition-all ${
            activeTierTab === 'ALL'
              ? 'border-primary bg-primary/10 ring-1 ring-primary'
              : 'border-outline-variant/30 bg-surface hover:bg-surface-container-high'
          }`}
        >
          <div className="text-[11px] text-outline font-semibold">Tất cả ngành</div>
          <div className="flex items-baseline gap-1 mt-0.5">
            <span className="text-lg font-bold text-on-surface">{data?.items.length || 0}</span>
            <span className="text-[10px] text-outline">ngành</span>
          </div>
          <div className="text-[10px] text-outline mt-0.5 truncate">Toàn bộ danh mục</div>
        </button>

        {/* Tab 1: SAFE (Green / Emerald) */}
        <button
          type="button"
          onClick={() => setActiveTierTab('SAFE')}
          className={`p-3 rounded-xl border text-left transition-all ${
            activeTierTab === 'SAFE'
              ? 'border-emerald-500 bg-emerald-50 ring-1 ring-emerald-500 shadow-xs'
              : 'border-emerald-200 bg-emerald-50/50 hover:bg-emerald-50 text-emerald-950'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[11px] text-emerald-800 font-bold flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-emerald-500"></span>
              An toàn (SAFE)
            </span>
            <span className="material-symbols-outlined text-emerald-600 text-[18px]">verified</span>
          </div>
          <div className="flex items-baseline gap-1 mt-0.5">
            <span className="text-lg font-black text-emerald-700">{data?.safeCount || 0}</span>
            <span className="text-[10px] text-emerald-700">ngành</span>
          </div>
          <div className="text-[10px] text-emerald-800 font-medium mt-0.5">
            Điểm ≥ Chuẩn + 15đ
          </div>
        </button>

        {/* Tab 2: FIT (Amber / Yellow) */}
        <button
          type="button"
          onClick={() => setActiveTierTab('FIT')}
          className={`p-3 rounded-xl border text-left transition-all ${
            activeTierTab === 'FIT'
              ? 'border-amber-500 bg-amber-50 ring-1 ring-amber-500 shadow-xs'
              : 'border-amber-200 bg-amber-50/50 hover:bg-amber-50 text-amber-950'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[11px] text-amber-800 font-bold flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-amber-500"></span>
              Phù hợp (FIT)
            </span>
            <span className="material-symbols-outlined text-amber-600 text-[18px]">balance</span>
          </div>
          <div className="flex items-baseline gap-1 mt-0.5">
            <span className="text-lg font-black text-amber-700">{data?.fitCount || 0}</span>
            <span className="text-[10px] text-amber-700">ngành</span>
          </div>
          <div className="text-[10px] text-amber-800 font-medium mt-0.5">
            Chuẩn - 10đ đến + 15đ
          </div>
        </button>

        {/* Tab 3: RISK (Rose / Red) */}
        <button
          type="button"
          onClick={() => setActiveTierTab('RISK')}
          className={`p-3 rounded-xl border text-left transition-all ${
            activeTierTab === 'RISK'
              ? 'border-rose-500 bg-rose-50 ring-1 ring-rose-500 shadow-xs'
              : 'border-rose-200 bg-rose-50/50 hover:bg-rose-50 text-rose-950'
          }`}
        >
          <div className="flex items-center justify-between">
            <span className="text-[11px] text-rose-800 font-bold flex items-center gap-1">
              <span className="w-2 h-2 rounded-full bg-rose-500"></span>
              Rủi ro (RISK)
            </span>
            <span className="material-symbols-outlined text-rose-600 text-[18px]">warning</span>
          </div>
          <div className="flex items-baseline gap-1 mt-0.5">
            <span className="text-lg font-black text-rose-700">{data?.riskCount || 0}</span>
            <span className="text-[10px] text-rose-700">ngành</span>
          </div>
          <div className="text-[10px] text-rose-800 font-medium mt-0.5">
            Điểm &lt; Chuẩn - 10đ
          </div>
        </button>
      </div>

      {/* Results List */}
      <div className="space-y-3">
        <div className="flex items-center justify-between text-xs text-on-surface-variant font-medium">
          <span>
            Danh sách ngành xét tuyển ({filteredItems.length} kết quả):
          </span>
          <span className="text-[11px] text-outline">
            Dữ liệu tham chiếu điểm chuẩn tuyển sinh ĐH 2025
          </span>
        </div>

        {loading ? (
          <div className="p-8 text-center text-outline space-y-2">
            <span className="material-symbols-outlined text-2xl animate-spin text-primary">sync</span>
            <p className="text-xs">Đang đối chiếu điểm chuẩn với các trường Đại học...</p>
          </div>
        ) : filteredItems.length === 0 ? (
          <div className="p-8 rounded-xl border border-outline-variant/30 text-center text-outline text-xs">
            Không tìm thấy ngành nào phù hợp với bộ lọc đã chọn.
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-3 max-h-[460px] overflow-y-auto pr-1">
            {filteredItems.map((item: AdmissionPredictionItem) => {
              const isSafe = item.tier === 'SAFE';
              const isFit = item.tier === 'FIT';
              const deltaPrefix = item.delta >= 0 ? '+' : '';

              return (
                <div
                  key={`${item.universityCode}-${item.majorCode}-${item.subjectCombo}`}
                  className={`p-4 rounded-xl border transition-all flex flex-col justify-between space-y-2.5 ${
                    isSafe
                      ? 'border-emerald-200 bg-emerald-50/30 hover:border-emerald-400'
                      : isFit
                      ? 'border-amber-200 bg-amber-50/30 hover:border-amber-400'
                      : 'border-rose-200 bg-rose-50/30 hover:border-rose-400'
                  }`}
                >
                  <div className="space-y-1.5">
                    {/* Header: Uni Code & Tier Badge */}
                    <div className="flex items-center justify-between gap-2">
                      <div className="flex items-center gap-1.5">
                        <span className="font-mono text-xs font-black px-2 py-0.5 rounded bg-surface border border-outline-variant/40 text-on-surface">
                          {item.universityCode}
                        </span>
                        <span className="text-xs text-on-surface-variant font-semibold truncate max-w-[180px]">
                          {item.universityName}
                        </span>
                      </div>

                      <span
                        className={`text-[10px] font-bold px-2 py-0.5 rounded-full flex items-center gap-1 ${
                          isSafe
                            ? 'bg-emerald-100 text-emerald-800'
                            : isFit
                            ? 'bg-amber-100 text-amber-800'
                            : 'bg-rose-100 text-rose-800'
                        }`}
                      >
                        <span className={`w-1.5 h-1.5 rounded-full ${isSafe ? 'bg-emerald-600' : isFit ? 'bg-amber-600' : 'bg-rose-600'}`}></span>
                        {isSafe ? 'An toàn' : isFit ? 'Phù hợp' : 'Rủi ro'}
                      </span>
                    </div>

                    {/* Major Name */}
                    <h3 className="text-sm font-bold text-on-surface leading-snug line-clamp-1">
                      {item.majorName}
                    </h3>
                    <div className="text-[11px] text-outline flex items-center gap-2">
                      <span>Mã: <code className="font-mono">{item.majorCode}</code></span>
                      <span>•</span>
                      <span className="font-semibold text-primary">Tổ hợp {item.subjectCombo}</span>
                      <span>•</span>
                      <span>Năm {item.year}</span>
                    </div>
                  </div>

                  {/* Cutoff & Delta row */}
                  <div className="pt-2 border-t border-outline-variant/20 flex items-center justify-between text-xs">
                    <div>
                      <span className="text-[11px] text-outline">Điểm chuẩn: </span>
                      <span className="font-mono font-bold text-on-surface">{item.cutoffScore.toFixed(1)}</span>
                    </div>

                    <div className="flex items-center gap-1 font-mono font-bold text-xs">
                      <span className="text-[11px] text-outline font-normal">Chênh lệch:</span>
                      <span
                        className={
                          item.delta >= 15
                            ? 'text-emerald-700'
                            : item.delta >= 0
                            ? 'text-amber-700'
                            : 'text-rose-600'
                        }
                      >
                        {deltaPrefix}{item.delta.toFixed(1)} đ
                      </span>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
};
