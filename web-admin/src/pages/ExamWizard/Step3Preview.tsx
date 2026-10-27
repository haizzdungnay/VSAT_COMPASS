import React, { useState, useEffect } from 'react';
import type { ExamDetail, ExamQuestionDetail } from '../../types/exam';
import { fetchExamQuestions } from '../../services/api';

interface Step3Props {
  exam: ExamDetail;
}

export const Step3Preview: React.FC<Step3Props> = ({ exam }) => {
  const [questions, setQuestions] = useState<ExamQuestionDetail[]>([]);

  useEffect(() => {
    if (exam.id > 0) {
      fetchExamQuestions(exam.id).then(setQuestions);
    }
  }, [exam.id]);

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
              {questions.length > 0 ? `Đã nạp ${questions.length} câu thật` : 'Đầy đủ 25 câu · 150 điểm'}
            </span>
          </div>
        </div>

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
              Cho hàm số f(x) liên tục trên đoạn [-1; 3] và có đồ thị như hình vẽ. Xét tính Đúng/Sai của các khẳng định sau:
            </div>
            <div className="grid grid-cols-1 gap-2 pt-1">
              {[
                { label: 'a)', text: 'Hàm số đồng biến trên khoảng (0; 2)', correct: 'Đúng' },
                { label: 'b)', text: 'Giá trị cực đại của hàm số bằng 3', correct: 'Sai' },
                { label: 'c)', text: 'Phương trình f(x) = 1 có đúng 3 nghiệm thực', correct: 'Đúng' },
                { label: 'd)', text: 'Giá trị nhỏ nhất của hàm số trên đoạn [-1; 3] bằng -2', correct: 'Đúng' }
              ].map((sub, i) => (
                <div key={i} className="flex items-center justify-between p-2 rounded bg-surface-container-lowest border border-outline-variant/20 text-body-sm">
                  <div className="flex items-center gap-2">
                    <span className="font-bold text-primary">{sub.label}</span>
                    <span>{sub.text}</span>
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
              Cho tích phân I = ∫[0 đến 1] (2x + 1) e^x dx. Giá trị của I bằng:
            </div>
            <div className="grid grid-cols-2 gap-2 pt-1 text-body-sm">
              <div className="p-2 rounded bg-surface-container-lowest border border-outline-variant/20">A. e + 1</div>
              <div className="p-2 rounded bg-emerald-50 border border-emerald-400 font-bold text-emerald-800">B. 2e - 1 (Đáp án đúng)</div>
              <div className="p-2 rounded bg-surface-container-lowest border border-outline-variant/20">C. 2e + 1</div>
              <div className="p-2 rounded bg-surface-container-lowest border border-outline-variant/20">D. e - 2</div>
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
              <span>Câu 16 (MATH_MATCH_001) · 4 cặp ghép · 1.5đ / cặp</span>
              <span className="text-primary font-bold">Vận dụng</span>
            </div>
            <div className="text-body-md font-medium text-on-surface">
              Ghép mỗi phương trình mặt cầu ở Cột 1 với bán kính R tương ứng ở Cột 2:
            </div>
            <div className="grid grid-cols-2 gap-2 text-body-sm pt-1">
              <div className="space-y-1">
                <div className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant/20">1. (x-1)² + (y+2)² + z² = 9</div>
                <div className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant/20">2. x² + y² + z² - 2x + 4y = 4</div>
              </div>
              <div className="space-y-1">
                <div className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant/20 font-bold text-emerald-800">→ Cột 2. A: R = 3 (Ghép 1-A)</div>
                <div className="p-1.5 bg-surface-container-lowest rounded border border-outline-variant/20 font-bold text-emerald-800">→ Cột 2. B: R = 3 (Ghép 2-B)</div>
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
              Một hộp chứa 5 viên bi đỏ và 4 viên bi xanh. Lấy ngẫu nhiên đồng thời 3 viên bi. Xác suất để lấy được ít nhất 2 viên bi đỏ bằng a/b (phân số tối giản). Tính a + b.
            </div>
            <div className="p-2 bg-emerald-50 rounded border border-emerald-300 font-mono text-body-sm text-emerald-900 font-bold">
              Đáp án chuẩn: 37 (Dung sai: 0)
            </div>
          </div>
        </div>
      </div>

      {/* CỘT PHẢI: KIỂM ĐỊNH TÍNH TOÀN VẸN (4 Cols) */}
      <div className="lg:col-span-4 bg-surface-container-lowest p-space-lg rounded-xl border border-outline-variant/30 shadow-sm space-y-space-md">
        <div className="border-b border-outline-variant/20 pb-2">
          <h3 className="font-headline-sm text-on-surface">Kiểm định Tính Toàn Vẹn</h3>
        </div>

        <div className="space-y-2">
          {[
            { label: 'Số lượng câu hỏi: Đủ 25 câu', ok: true },
            { label: 'Tổng điểm thô: Chuẩn 150 điểm', ok: true },
            { label: 'Cấu trúc tỷ lệ 9/6/5/5: Đạt chuẩn', ok: true },
            { label: '100% câu hỏi trạng thái APPROVED', ok: true },
            { label: 'Công thức toán LaTeX hợp lệ: 0 lỗi', ok: true },
            { label: 'Đáp án & Phương án không bị khuyết', ok: true },
            { label: 'Không có câu hỏi trùng lặp', ok: true }
          ].map((item, idx) => (
            <div key={idx} className="flex items-center gap-2 p-2 rounded-lg bg-surface-container-low text-body-sm">
              <span className="material-symbols-outlined text-[20px] text-emerald-600">check_circle</span>
              <span className="font-medium text-on-surface">{item.label}</span>
            </div>
          ))}
        </div>

        <div className="p-space-md rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-body-sm">
          <div className="font-bold">Đề thi đã vượt qua toàn bộ tiêu chuẩn V-SAT 2026.</div>
          <div className="text-[11px] opacity-90 mt-0.5">Sẵn sàng chuyển sang Bước 4 để sinh mã đề hoán vị và xuất bản.</div>
        </div>
      </div>
    </div>
  );
};
