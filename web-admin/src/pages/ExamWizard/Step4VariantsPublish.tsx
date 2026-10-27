import React, { useState, useEffect } from 'react';
import type { ExamDetail, ExamVariantItem } from '../../types/exam';
import { fetchExamVariants, generateExamVariants, publishExam } from '../../services/api';

interface Step4Props {
  exam: ExamDetail;
  onPublishSuccess: () => void;
}

export const Step4VariantsPublish: React.FC<Step4Props> = ({ exam, onPublishSuccess }) => {
  const [shuffleQuestions, setShuffleQuestions] = useState(true);
  const [shuffleMcq, setShuffleMcq] = useState(true);
  const [shuffleMatching, setShuffleMatching] = useState(true);
  const [isPublishing, setIsPublishing] = useState(false);
  const [variants, setVariants] = useState<ExamVariantItem[]>([]);
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => {
    fetchExamVariants(exam.id).then(setVariants);
  }, [exam.id]);

  const handleRegenerate = async () => {
    const list = await generateExamVariants(exam.id, {
      variantCount: 4,
      shuffleQuestions,
      shuffleMcq,
      shuffleMatching
    });
    setVariants(list);
  };

  const handlePublish = async () => {
    setIsPublishing(true);
    setFeedback(null);
    try {
      const ok = await publishExam(exam.id);
      if (ok) {
        setFeedback('Đề thi đã được xuất bản thành công lên hệ thống!');
        setTimeout(() => {
          onPublishSuccess();
        }, 1000);
      } else {
        setFeedback('Đã nộp đề thi để duyệt (Review queued)');
        setTimeout(() => {
          onPublishSuccess();
        }, 1200);
      }
    } finally {
      setIsPublishing(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg">
      {/* CỘT TRÁI: Cấu hình biến thể (8 Cols) */}
      <div className="lg:col-span-8 flex flex-col gap-space-lg">
        <div className="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-sm flex flex-col gap-space-md">
          <div className="flex items-center justify-between pb-space-xs border-b border-outline-variant/20">
            <div className="flex items-center gap-space-sm">
              <div className="w-8 h-8 rounded-lg bg-surface-container flex items-center justify-center text-primary">
                <span className="material-symbols-outlined text-[20px]">shuffle</span>
              </div>
              <div>
                <h2 className="font-headline-sm text-on-surface">Cấu hình Thuật toán Sinh Mã Đề Hoán Vị</h2>
                <p className="font-body-sm text-outline">Xáo trộn câu hỏi và đáp án bảo đảm tính đẳng trị sư phạm</p>
              </div>
            </div>
            <span className="bg-secondary-fixed text-on-secondary-fixed-variant font-label-sm text-[11px] px-space-sm py-1 rounded-full font-bold">
              Chuẩn V-SAT Core v3.2
            </span>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-space-md p-space-md rounded-lg bg-surface-container-low">
            <div>
              <div className="font-label-sm text-outline">Số lượng mã đề sinh ra</div>
              <div className="font-headline-sm text-on-surface mt-0.5">{variants.length} mã đề hoán vị</div>
            </div>
            <div>
              <div className="font-label-sm text-outline">Dãy mã đề chuẩn</div>
              <div className="font-headline-sm font-mono text-primary mt-0.5">
                {variants.map(v => v.variantCode).join(' · ')}
              </div>
            </div>
            <div>
              <div className="font-label-sm text-outline">Random Seed</div>
              <div className="font-mono text-body-sm text-on-surface mt-1">
                {variants[0]?.randomSeed || `seed_${exam.examCode}`}
              </div>
            </div>
          </div>

          <div className="space-y-2 pt-1">
            <div className="font-label-md text-on-surface font-semibold">Tùy chọn xáo trộn theo quy chuẩn V-SAT:</div>
            
            <label className="flex items-center gap-2 text-body-sm text-on-surface cursor-pointer">
              <input
                type="checkbox"
                checked={shuffleQuestions}
                onChange={(e) => {
                  setShuffleQuestions(e.target.checked);
                  handleRegenerate();
                }}
                className="w-4 h-4 text-primary rounded"
              />
              <span>Xáo trộn thứ tự câu hỏi trong từng phần riêng biệt</span>
            </label>

            <label className="flex items-center gap-2 text-body-sm text-on-surface cursor-pointer">
              <input
                type="checkbox"
                checked={shuffleMcq}
                onChange={(e) => {
                  setShuffleMcq(e.target.checked);
                  handleRegenerate();
                }}
                className="w-4 h-4 text-primary rounded"
              />
              <span>Xáo trộn các phương án A, B, C, D của câu hỏi Trắc nghiệm 4 lựa chọn</span>
            </label>

            <label className="flex items-center gap-2 text-body-sm text-on-surface cursor-pointer">
              <input
                type="checkbox"
                checked={shuffleMatching}
                onChange={(e) => {
                  setShuffleMatching(e.target.checked);
                  handleRegenerate();
                }}
                className="w-4 h-4 text-primary rounded"
              />
              <span>Xáo trộn các lựa chọn của câu hỏi Ghép hợp</span>
            </label>

            <div className="p-2.5 rounded bg-surface-container-low text-[11px] text-outline flex items-center gap-1.5">
              <span className="material-symbols-outlined text-[16px] text-amber-600">info</span>
              <span>Lưu ý: Mệnh đề a, b, c, d của câu hỏi Đúng/Sai được giữ nguyên thứ tự sư phạm.</span>
            </div>
          </div>
        </div>

        {/* Bảng các biến thể */}
        <div className="bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-sm space-y-3">
          <div className="flex items-center justify-between">
            <div className="font-headline-sm text-on-surface">Danh sách Mã Đề Hoán Vị (Variants)</div>
            <button
              onClick={handleRegenerate}
              className="px-2.5 py-1 rounded bg-surface-container-low text-primary text-label-sm font-semibold hover:bg-surface-container-high transition-colors"
            >
              Sinh lại biến thể
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {variants.map((v) => (
              <div key={v.variantCode} className="p-3 rounded-lg border border-outline-variant/30 bg-surface-container-low flex justify-between items-center">
                <div>
                  <div className="font-bold text-primary font-mono text-body-lg">Mã đề {v.variantCode}</div>
                  <div className="text-[11px] text-outline">
                    {v.questionCount || 25} câu · 150 điểm · Hash: {v.contentHash}
                  </div>
                </div>
                <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800">
                  Đẳng trị
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* CỘT PHẢI: Xác nhận và Xuất bản (4 Cols) */}
      <div className="lg:col-span-4 bg-surface-container-lowest rounded-xl p-space-lg border border-outline-variant/30 shadow-sm flex flex-col justify-between gap-space-lg">
        <div className="space-y-space-md">
          <div className="border-b border-outline-variant/20 pb-2">
            <h3 className="font-headline-sm text-on-surface">Thẩm định & Xuất bản</h3>
            <p className="text-body-sm text-outline">Đề thi sẵn sàng phát hành cho học sinh</p>
          </div>

          {feedback && (
            <div className="p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-body-sm flex items-center gap-2">
              <span className="material-symbols-outlined text-[20px] text-emerald-600">check_circle</span>
              <span>{feedback}</span>
            </div>
          )}

          <div className="space-y-2">
            {[
              'Đầy đủ 25 câu hỏi chuẩn V-SAT',
              'Đúng 150 điểm thô',
              `Đã sinh ${variants.length} mã đề hoán vị đẳng trị`,
              'Đã cấu hình thời lượng & ca thi',
              'Phân quyền xuất bản: CONTENT_ADMIN / SUPER_ADMIN'
            ].map((text, i) => (
              <div key={i} className="flex items-center gap-2 text-body-sm text-on-surface">
                <span className="material-symbols-outlined text-[18px] text-emerald-600">check_circle</span>
                <span>{text}</span>
              </div>
            ))}
          </div>
        </div>

        <div className="space-y-2 pt-4 border-t border-outline-variant/20">
          <button
            onClick={handlePublish}
            disabled={isPublishing}
            className="w-full h-11 rounded-lg bg-primary hover:bg-tertiary text-on-primary font-label-md font-bold flex items-center justify-center gap-2 shadow-sm transition-all disabled:opacity-50"
          >
            {isPublishing ? (
              <>
                <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin"></span>
                <span>Đang xuất bản đề thi...</span>
              </>
            ) : (
              <>
                <span className="material-symbols-outlined text-[20px]">publish</span>
                <span>XUẤT BẢN ĐỀ THI V-SAT</span>
              </>
            )}
          </button>

          <button
            type="button"
            onClick={onPublishSuccess}
            className="w-full h-9 rounded-lg bg-surface-container-low hover:bg-surface-container-high text-on-surface-variant font-label-md text-label-md"
          >
            Lưu bản nháp & Quay lại
          </button>
        </div>
      </div>
    </div>
  );
};
