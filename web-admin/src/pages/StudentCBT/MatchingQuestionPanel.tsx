import React from 'react';
import type { StudentQuestionContent } from '../../types/student-cbt';
import { MathView } from '../../components/MathView';

interface MatchingQuestionPanelProps {
  question: StudentQuestionContent;
  matchingPayload: Record<string, string>;
  onChangeMatching: (nextPayload: Record<string, string>) => void;
}

export const MatchingQuestionPanel: React.FC<MatchingQuestionPanelProps> = ({
  question,
  matchingPayload,
  onChangeMatching
}) => {
  const premises = question.options || [];
  const choices = question.matchingChoices || [];

  const handleSelectPair = (premiseKey: string, choiceKey: string) => {
    const currentChoice = matchingPayload[premiseKey];
    if (currentChoice === choiceKey) {
      const next = { ...matchingPayload };
      delete next[premiseKey];
      onChangeMatching(next);
      return;
    }
    onChangeMatching({
      ...matchingPayload,
      [premiseKey]: choiceKey
    });
  };

  const handleClearPair = (premiseKey: string) => {
    if (!(premiseKey in matchingPayload)) return;
    const next = { ...matchingPayload };
    delete next[premiseKey];
    onChangeMatching(next);
  };

  const handleClearAll = () => {
    if (Object.keys(matchingPayload).length === 0) return;
    onChangeMatching({});
  };

  const pairedCount = premises.filter((opt, idx) => {
    const key = opt.optionLabel || String(idx + 1);
    return Boolean(matchingPayload[key]);
  }).length;

  return (
    <div className="space-y-4 pt-2">
      <div className="flex flex-wrap items-center justify-between gap-2 text-xs text-outline">
        <span className="italic">
          Ghép từng mệnh đề ở Cột A với 1 lựa chọn tương ứng ở Cột B (1.5đ/cặp đúng, tối đa 6đ)
        </span>
        <div className="flex items-center gap-2">
          <span className="font-semibold text-primary">
            Đã ghép: {pairedCount}/{premises.length}
          </span>
          {pairedCount > 0 && (
            <button
              type="button"
              onClick={handleClearAll}
              className="px-2 py-0.5 rounded bg-surface-container-high hover:bg-rose-50 hover:text-rose-700 text-outline text-[11px] font-semibold transition-colors"
            >
              Xóa tất cả
            </button>
          )}
        </div>
      </div>

      {/* Cột B: Danh sách lựa chọn */}
      <div className="p-4 rounded-xl bg-surface-container-low border border-outline-variant/40 space-y-2.5">
        <div className="flex items-center gap-1.5 text-xs font-bold text-primary uppercase tracking-wide">
          <span className="material-symbols-outlined text-[16px]">view_list</span>
          <span>Cột B — Các phương án lựa chọn ({choices.length} lựa chọn)</span>
        </div>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2">
          {choices.map(choice => {
            const usedByKeys = premises
              .map((opt, idx) => opt.optionLabel || String(idx + 1))
              .filter(pKey => matchingPayload[pKey] === choice.key);
            const isUsed = usedByKeys.length > 0;

            return (
              <div
                key={choice.key}
                className={`p-2.5 rounded-lg border text-xs flex items-start gap-2 transition-colors ${
                  isUsed
                    ? 'bg-primary/5 border-primary/40 text-on-surface'
                    : 'bg-surface-container-lowest border-outline-variant/30 text-on-surface'
                }`}
              >
                <span
                  className={`w-6 h-6 rounded-md font-bold flex items-center justify-center flex-shrink-0 ${
                    isUsed
                      ? 'bg-primary text-on-primary'
                      : 'bg-surface-container-high text-outline'
                  }`}
                >
                  {choice.key}
                </span>
                <div className="flex-1 pt-0.5 leading-relaxed">
                  <MathView content={choice.text} />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Cột A: Các mệnh đề cần ghép */}
      <div className="space-y-2.5">
        <div className="flex items-center gap-1.5 text-xs font-bold text-primary uppercase tracking-wide">
          <span className="material-symbols-outlined text-[16px]">sync_alt</span>
          <span>Cột A — Ghép cặp với Cột B</span>
        </div>

        <div className="divide-y divide-outline-variant/30 border border-outline-variant/40 rounded-xl overflow-hidden bg-surface-container-lowest">
          {premises.map((opt, idx) => {
            const premiseKey = opt.optionLabel || String(idx + 1);
            const selectedChoice = matchingPayload[premiseKey] || '';

            return (
              <div
                key={opt.id || premiseKey}
                className="p-4 flex flex-col gap-3 transition-colors"
              >
                <div className="flex items-start justify-between gap-3">
                  <div className="flex items-start gap-3 flex-1 min-w-0">
                    <span className="w-7 h-7 rounded-lg bg-surface-container-high text-on-surface text-xs font-bold flex items-center justify-center flex-shrink-0 mt-0.5">
                      {premiseKey}
                    </span>
                    <div className="text-sm text-on-surface leading-relaxed pt-0.5">
                      <MathView content={opt.optionText} />
                    </div>
                  </div>

                  {selectedChoice && (
                    <div className="flex items-center gap-1.5 flex-shrink-0">
                      <span className="px-2.5 py-1 rounded-lg bg-primary/10 border border-primary/30 text-primary text-xs font-bold">
                        {premiseKey} → {selectedChoice}
                      </span>
                      <button
                        type="button"
                        onClick={() => handleClearPair(premiseKey)}
                        className="p-1 rounded-lg text-outline hover:text-rose-600 hover:bg-rose-50 transition-colors"
                        title={`Xóa lựa chọn của ý ${premiseKey}`}
                      >
                        <span className="material-symbols-outlined text-[16px]">close</span>
                      </button>
                    </div>
                  )}
                </div>

                {/* Button group chọn A..F */}
                <div className="flex flex-wrap items-center gap-1.5 pl-10">
                  <span className="text-[11px] text-outline mr-1">Chọn ghép:</span>
                  {choices.map(choice => {
                    const isSelected = selectedChoice === choice.key;
                    return (
                      <button
                        key={choice.key}
                        type="button"
                        onClick={() => handleSelectPair(premiseKey, choice.key)}
                        title={`Ghép ${premiseKey} với ${choice.key}`}
                        className={`min-w-[36px] h-8 px-2.5 rounded-lg text-xs font-bold border transition-all ${
                          isSelected
                            ? 'bg-primary text-on-primary border-primary shadow-xs ring-2 ring-primary/20'
                            : 'bg-surface-container-low text-on-surface-variant border-outline-variant/40 hover:bg-primary/10 hover:text-primary hover:border-primary/40'
                        }`}
                      >
                        {choice.key}
                      </button>
                    );
                  })}
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
};
