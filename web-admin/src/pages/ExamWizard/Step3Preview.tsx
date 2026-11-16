import React, { useState, useEffect } from 'react';
import type { ExamDetail, ExamQuestionDetail } from '../../types/exam';
import {
  VSAT_PARTS,
  VSAT_PART_COUNTS,
  VSAT_TOTAL_QUESTIONS,
  VSAT_TOTAL_RAW_SCORE,
  getExpectedVsatDuration
} from '../../types/exam';
import { fetchExamQuestions } from '../../services/api';
import { MathView } from '../../components/MathView';

interface Step3Props {
  exam: ExamDetail;
  onPublish?: () => void;
}

export const Step3Preview: React.FC<Step3Props> = ({ exam, onPublish }) => {
  const [questions, setQuestions] = useState<ExamQuestionDetail[]>([]);

  useEffect(() => {
    if (exam.id > 0) {
      fetchExamQuestions(exam.id).then(setQuestions);
    }
  }, [exam.id]);

  const hasRealQuestions = questions.length > 0;

  // Compute V-SAT criteria dynamically from real question list
  const isCountValid = questions.length === VSAT_TOTAL_QUESTIONS;
  const part1Count = questions.filter(q => q.questionType === 'TRUE_FALSE').length;
  const part2Count = questions.filter(q => q.questionType === 'SINGLE_CHOICE').length;
  const part3Count = questions.filter(q => q.questionType === 'MATCHING').length;
  const part4Count = questions.filter(q => q.questionType === 'SHORT_ANSWER').length;

  const isStructureValid = isCountValid &&
    part1Count === VSAT_PART_COUNTS.TRUE_FALSE &&
    part2Count === VSAT_PART_COUNTS.SINGLE_CHOICE &&
    part3Count === VSAT_PART_COUNTS.MATCHING &&
    part4Count === VSAT_PART_COUNTS.SHORT_ANSWER;

  const partIndices = questions.map(q => VSAT_PARTS.indexOf(q.questionType as any));
  const isOrderValid = isCountValid &&
    partIndices.every((idx, i) => idx >= 0 && (i === 0 || idx >= partIndices[i - 1]));

  const expectedDuration = getExpectedVsatDuration(exam.subjectId, exam.subjectName, exam.examCode);
  const isDurationValid = exam.durationMinutes === expectedDuration;

  const totalRawScore = questions.reduce((sum, q) => sum + (Number(q.maxRawScore) || 6.0), 0);
  const isScoreValid = isCountValid && Math.abs(totalRawScore - VSAT_TOTAL_RAW_SCORE) < 0.1;

  const isStatusValid = isCountValid &&
    questions.every(q => !q.status || q.status === 'APPROVED' || q.status === 'PUBLISHED');

  const uniqueIds = new Set(questions.map(q => q.questionId || q.id));
  const isUnique = isCountValid && uniqueIds.size === VSAT_TOTAL_QUESTIONS;

  const criteria = [
    {
      id: 'count',
      label: `Số lượng câu hỏi: ${isCountValid ? 'Đủ 25 câu' : `${questions.length}/25 câu`}`,
      ok: isCountValid,
      reason: `Chưa đủ 25 câu (hiện có ${questions.length}/25 câu)`
    },
    {
      id: 'structure',
      label: `Cấu trúc tỷ lệ 9/6/5/5 (9 Đ/S, 6 Trắc nghiệm, 5 Ghép hợp, 5 Điền số): ${isStructureValid ? 'Đạt chuẩn' : `Chưa đạt (${part1Count} Đ/S, ${part2Count} TN, ${part3Count} Ghép, ${part4Count} Điền)`}`,
      ok: isStructureValid,
      reason: `Cấu trúc chưa đạt chuẩn 9/6/5/5 (hiện có ${part1Count}/${part2Count}/${part3Count}/${part4Count})`
    },
    {
      id: 'order',
      label: `Thứ tự các phần (Phần I Đ/S → Phần II Trắc nghiệm → Phần III Ghép hợp → Phần IV Điền số): ${isOrderValid ? 'Đúng thứ tự' : 'Sai thứ tự phần'}`,
      ok: isOrderValid,
      reason: 'Thứ tự các phần chưa đúng chuẩn (Phần I Đ/S → Phần II TN → Phần III Ghép → Phần IV Điền)'
    },
    {
      id: 'duration',
      label: `Thời lượng làm bài: ${exam.durationMinutes} phút (${isDurationValid ? 'Đúng chuẩn' : `Chưa đúng, môn này chuẩn ${expectedDuration} phút`})`,
      ok: isDurationValid,
      reason: `Thời lượng ${exam.durationMinutes} phút chưa đúng chuẩn (${expectedDuration} phút)`
    },
    {
      id: 'score',
      label: `Tổng điểm thô: ${isScoreValid ? 'Chuẩn 150 điểm' : `${totalRawScore.toFixed(1)} / 150 điểm`}`,
      ok: isScoreValid,
      reason: `Tổng điểm thô ${totalRawScore.toFixed(1)}đ chưa đạt 150 điểm`
    },
    {
      id: 'status',
      label: `100% câu hỏi trạng thái APPROVED: ${isStatusValid ? 'Đạt chuẩn' : 'Có câu chưa duyệt hoặc thiếu câu'}`,
      ok: isStatusValid,
      reason: 'Có câu hỏi chưa ở trạng thái duyệt APPROVED'
    },
    {
      id: 'unique',
      label: `Không có câu hỏi trùng lặp: ${isUnique ? 'Đạt chuẩn' : 'Có câu trùng lặp hoặc thiếu câu'}`,
      ok: isUnique,
      reason: 'Có câu hỏi bị trùng lặp'
    }
  ];

  const allPassed = criteria.every(c => c.ok);
  const failedCriteria = criteria.filter(c => !c.ok);

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-space-lg">
      {/* CỘT TRÁI: ĐỀ THI XEM TRƯỚC (8 Cols) */}
      <div className="lg:col-span-8 bg-surface-container-lowest p-space-lg rounded-xl border border-outline-variant/30 shadow-sm space-y-space-lg">
        <div className="border-b border-outline-variant/20 pb-space-sm flex items-center justify-between">
          <div className="flex items-center gap-space-sm">
            <span className="material-symbols-outlined text-primary text-[22px]">visibility</span>
            <h2 className="font-headline-sm text-on-surface">Xem trước đề thi (WYSIWYG Preview)</h2>
          </div>
          <div className="flex items-center gap-2">
            <span className="px-2.5 py-0.5 rounded text-[11px] font-bold bg-emerald-100 text-emerald-800">
              {hasRealQuestions ? `Đã nạp ${questions.length} câu thật` : 'Đầy đủ 25 câu · 150 điểm'}
            </span>
          </div>
        </div>

        {hasRealQuestions ? (
          /* Render Real Questions */
          <div className="space-y-4">
            {questions.map((q, idx) => (
              <div key={q.id || idx} className="p-4 rounded-xl bg-surface-container-low border border-outline-variant/30 space-y-2">
                <div className="flex items-center justify-between text-[11px] text-outline font-mono">
                  <span>Câu {idx + 1} ({q.questionCode}) · {q.questionType} · Điểm tối đa: {q.maxRawScore || 6.0}đ</span>
                  <span className="text-primary font-bold">{q.difficulty}</span>
                </div>
                <div className="text-body-md font-medium text-on-surface">
                  <MathView content={q.questionText} />
                </div>
                {q.options && q.options.length > 0 && (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1 text-body-sm">
                    {q.options.map((opt, optIdx) => (
                      <div
                        key={optIdx}
                        className={`p-2 rounded border text-body-sm flex items-center gap-2 ${
                          opt.isCorrect
                            ? 'bg-emerald-50 border-emerald-400 font-bold text-emerald-800'
                            : 'bg-surface-container-lowest border-outline-variant/20 text-on-surface'
                        }`}
                      >
                        <span className="font-bold">{opt.optionLabel}.</span>
                        <MathView content={opt.optionText} />
                        {opt.isCorrect && <span className="ml-auto text-[10px] text-emerald-700 font-semibold">(Đúng)</span>}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}
          </div>
        ) : (
          /* Default 4 Parts Demo */
          <>
            {/* Section 1: Đúng Sai Demo */}
            <div className="space-y-3">
              <div className="p-2.5 rounded-lg bg-cyan-50 border border-cyan-200 flex justify-between items-center text-cyan-900 font-bold text-body-sm">
                <span>PHẦN I. CÂU HỎI ĐÚNG / SAI (Gồm 9 câu hỏi từ Câu 1 đến Câu 9)</span>
                <span>54 ĐIỂM</span>
              </div>

              <div className="p-space-md rounded-lg bg-surface-container-low border border-outline-variant/30 space-y-2">
                <div className="flex items-center justify-between text-[11px] text-outline font-mono">
                  <span>Câu 1 (MATH_TF_001) · Điểm tối đa: 6.0đ</span>
                  <span className="text-primary font-bold">Vận dụng</span>
                </div>
                <div className="text-body-md font-medium text-on-surface">
                  <MathView content="Cho hàm số $f(x)$ liên tục trên đoạn $[-1; 3]$ và có đồ thị như hình vẽ. Xét tính Đúng/Sai của các khẳng định sau:" />
                </div>
                <div className="grid grid-cols-1 gap-2 pt-1">
                  {[
                    { label: 'a)', text: 'Hàm số đồng biến trên khoảng $(0; 2)$', correct: 'Đúng' },
                    { label: 'b)', text: 'Giá trị cực đại của hàm số bằng $3$', correct: 'Sai' },
                    { label: 'c)', text: 'Phương trình $f(x) = 1$ có đúng 3 nghiệm thực', correct: 'Đúng' },
                    { label: 'd)', text: 'Giá trị nhỏ nhất của hàm số trên đoạn $[-1; 3]$ bằng $-2$', correct: 'Đúng' }
                  ].map((sub, i) => (
                    <div key={i} className="flex items-center justify-between p-2 rounded bg-surface-container-lowest border border-outline-variant/20 text-body-sm">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-primary">{sub.label}</span>
                        <MathView content={sub.text} />
                      </div>
                      <span className={`px-2 py-0.5 rounded text-[11px] font-bold ${sub.correct === 'Đúng' ? 'bg-emerald-100 text-emerald-800' : 'bg-red-100 text-red-800'}`}>
                        {sub.correct}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Section 2: MCQ Demo */}
            <div className="space-y-3">
              <div className="p-2.5 rounded-lg bg-indigo-50 border border-indigo-200 flex justify-between items-center text-indigo-900 font-bold text-body-sm">
                <span>PHẦN II. TRẮC NGHIỆM 4 LỰA CHỌN (Gồm 6 câu hỏi từ Câu 10 đến Câu 15)</span>
                <span>36 ĐIỂM</span>
              </div>

              <div className="p-space-md rounded-lg bg-surface-container-low border border-outline-variant/30 space-y-2">
                <div className="flex items-center justify-between text-[11px] text-outline font-mono">
                  <span>Câu 10 (MATH_MCQ_001) · Điểm tối đa: 6.0đ</span>
                  <span className="text-primary font-bold">Thông hiểu</span>
                </div>
                <div className="text-body-md font-medium text-on-surface">
                  <MathView content="Cho tích phân $I = \int_{0}^{1} (2x + 1) e^x dx$. Giá trị của $I$ bằng:" />
                </div>
                <div className="grid grid-cols-2 gap-2 pt-1 text-body-sm">
                  <div className="p-2 rounded bg-surface-container-lowest border border-outline-variant/20">
                    <MathView content="A. $e + 1$" />
                  </div>
                  <div className="p-2 rounded bg-emerald-50 border border-emerald-400 font-bold text-emerald-800">
                    <MathView content="B. $2e - 1$ (Đáp án đúng)" />
                  </div>
                  <div className="p-2 rounded bg-surface-container-lowest border border-outline-variant/20">
                    <MathView content="C. $2e + 1$" />
                  </div>
                  <div className="p-2 rounded bg-surface-container-lowest border border-outline-variant/20">
                    <MathView content="D. $e - 2$" />
                  </div>
                </div>
              </div>
            </div>

            {/* Section 3: Matching Demo */}
            <div className="space-y-3">
              <div className="p-2.5 rounded-lg bg-amber-50 border border-amber-200 flex justify-between items-center text-amber-900 font-bold text-body-sm">
                <span>PHẦN III. GHÉP HỢP (Gồm 5 câu hỏi từ Câu 16 đến Câu 20)</span>
                <span>30 ĐIỂM</span>
              </div>

              <div className="p-space-md rounded-lg bg-surface-container-low border border-outline-variant/30 space-y-2">
                <div className="flex items-center justify-between text-[11px] text-outline font-mono">
                  <span>Câu 16 (MATH_MATCH_001) · Điểm tối đa: 6.0đ</span>
                  <span className="text-primary font-bold">Vận dụng</span>
                </div>
                <div className="text-body-md font-medium text-on-surface">
                  <MathView content="Ghép mỗi phương trình mặt cầu ở Cột 1 với bán kính $R$ tương ứng ở Cột 2:" />
                </div>
                <div className="grid grid-cols-2 gap-2 text-body-sm pt-1">
                  <div className="space-y-1">
                    <div className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant/20">
                      <MathView content="1. $(x-1)^2 + (y+2)^2 + z^2 = 9$" />
                    </div>
                    <div className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant/20">
                      <MathView content="2. $x^2 + y^2 + z^2 - 2x + 4y = 4$" />
                    </div>
                  </div>
                  <div className="space-y-1">
                    <div className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant/20 font-bold text-emerald-800">
                      <MathView content="→ Cột 2. A: $R = 3$ (Ghép 1-A)" />
                    </div>
                    <div className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant/20 font-bold text-emerald-800">
                      <MathView content="→ Cột 2. B: $R = 3$ (Ghép 2-B)" />
                    </div>
                  </div>
                </div>
              </div>
            </div>

            {/* Section 4: Short Answer Demo */}
            <div className="space-y-3">
              <div className="p-2.5 rounded-lg bg-emerald-50 border border-emerald-200 flex justify-between items-center text-emerald-900 font-bold text-body-sm">
                <span>PHẦN IV. ĐIỀN SỐ / TRẢ LỜI NGẮN (Gồm 5 câu hỏi từ Câu 21 đến Câu 25)</span>
                <span>30 ĐIỂM</span>
              </div>

              <div className="p-space-md rounded-lg bg-surface-container-low border border-outline-variant/30 space-y-2">
                <div className="flex items-center justify-between text-[11px] text-outline font-mono">
                  <span>Câu 21 (MATH_SA_001) · Điểm tối đa: 6.0đ</span>
                  <span className="text-primary font-bold">Vận dụng cao</span>
                </div>
                <div className="text-body-md font-medium text-on-surface">
                  <MathView content="Một hộp chứa 5 viên bi đỏ và 4 viên bi xanh. Lấy ngẫu nhiên đồng thời 3 viên bi. Xác suất để lấy được ít nhất 2 viên bi đỏ bằng $\frac{a}{b}$ (phân số tối giản). Tính $a + b$." />
                </div>
                <div className="p-2 bg-emerald-50 rounded border border-emerald-300 font-mono text-body-sm text-emerald-900 font-bold">
                  Đáp án chuẩn: 37 (Dung sai: 0)
                </div>
              </div>
            </div>
          </>
        )}
      </div>

      {/* CỘT PHẢI: KIỂM ĐỊNH TÍNH TOÀN VẸN (4 Cols) */}
      <div className="lg:col-span-4 bg-surface-container-lowest p-space-lg rounded-xl border border-outline-variant/30 shadow-sm space-y-space-md">
        <div className="border-b border-outline-variant/20 pb-2">
          <h3 className="font-headline-sm text-on-surface">Kiểm định Tính Toàn Vẹn</h3>
        </div>

        <div className="space-y-2">
          {criteria.map((item, idx) => (
            <div key={idx} className="flex items-center gap-2 p-2 rounded-lg bg-surface-container-low text-body-sm">
              <span className={`material-symbols-outlined text-[20px] ${item.ok ? 'text-emerald-600' : 'text-amber-600'}`}>
                {item.ok ? 'check_circle' : 'warning'}
              </span>
              <span className="font-medium text-on-surface">{item.label}</span>
            </div>
          ))}
        </div>

        <div className={`p-space-md rounded-lg border text-body-sm ${
          allPassed
            ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
            : 'bg-amber-50 border-amber-200 text-amber-800'
        }`}>
          <div className="font-bold">
            {allPassed
              ? 'Đề thi đã vượt qua toàn bộ tiêu chuẩn V-SAT 2026.'
              : 'Đề thi cần hoàn thiện thêm một số tiêu chí ma trận.'}
          </div>
          <div className="text-[11px] opacity-90 mt-0.5">
            {allPassed
              ? 'Sẵn sàng chuyển sang Bước 4 để sinh mã đề hoán vị và xuất bản.'
              : 'Vui lòng kiểm tra lại cấu trúc các phần thi.'}
          </div>
        </div>

        {/* Nút xuất bản (bị khóa kèm lý do khi chưa đạt kiểm định) */}
        <div className="pt-2 border-t border-outline-variant/20 space-y-2">
          <button
            type="button"
            disabled={!allPassed}
            aria-describedby={!allPassed ? 'step3-publish-reason' : undefined}
            onClick={onPublish}
            className="w-full h-11 rounded-lg bg-primary hover:bg-tertiary text-on-primary font-label-md font-bold flex items-center justify-center gap-2 shadow-sm transition-all disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <span className="material-symbols-outlined text-[20px]">publish</span>
            <span>XUẤT BẢN ĐỀ THI V-SAT</span>
          </button>
          {!allPassed && (
            <div id="step3-publish-reason" className="text-[12px] text-amber-800 bg-amber-50 border border-amber-200 p-2.5 rounded-lg space-y-1">
              <div className="font-semibold">Nút xuất bản bị khóa do chưa đạt các tiêu chí:</div>
              <ul className="list-disc list-inside space-y-0.5 text-[11px]">
                {failedCriteria.map(c => (
                  <li key={c.id}>{c.reason}</li>
                ))}
              </ul>
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
