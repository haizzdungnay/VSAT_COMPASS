import React, { useState } from 'react';
import type { ExamDetail } from '../../types/exam';
import { createExam, updateExam } from '../../services/api';
import { Step1General } from './Step1General';
import { Step2Composition } from './Step2Composition';
import { Step3Preview } from './Step3Preview';
import { Step4VariantsPublish } from './Step4VariantsPublish';

interface WizardProps {
  onBackToList: () => void;
}

export const CreateExamWizard: React.FC<WizardProps> = ({ onBackToList }) => {
  const [currentStep, setCurrentStep] = useState<number>(1);
  const [isSaving, setIsSaving] = useState(false);
  const [exam, setExam] = useState<ExamDetail>({
    id: 0,
    examCode: 'VSAT_2026_TOAN_01',
    title: 'Đề thi thử V-SAT Toán học - Khóa 2026 số 01',
    subjectId: 1,
    subjectName: 'Toán học',
    questionCount: 25,
    durationMinutes: 90,
    difficulty: 'HARD',
    pricingType: 'FREE',
    status: 'DRAFT',
    scoringProfileCode: 'VSAT_2026_RAW_150',
    deliveryMode: 'PRACTICE'
  });

  const handleUpdate = (updated: Partial<ExamDetail>) => {
    setExam(prev => ({ ...prev, ...updated }));
  };

  const handleSaveDraft = async () => {
    setIsSaving(true);
    try {
      if (exam.id === 0) {
        const created = await createExam(exam);
        setExam(created);
      } else {
        await updateExam(exam.id, exam);
      }
    } finally {
      setIsSaving(false);
    }
  };

  const handleNextStep = async () => {
    if (currentStep === 1) {
      setIsSaving(true);
      try {
        if (exam.id === 0) {
          const created = await createExam(exam);
          setExam(created);
        } else {
          await updateExam(exam.id, exam);
        }
      } finally {
        setIsSaving(false);
      }
    }
    setCurrentStep(prev => Math.min(4, prev + 1));
  };

  const steps = [
    { num: 1, title: 'Thông tin chung & Ca thi' },
    { num: 2, title: 'Cấu trúc & Soạn đề (25 câu)' },
    { num: 3, title: 'Xem trước & Kiểm định' },
    { num: 4, title: 'Sinh mã đề & Xuất bản' }
  ];

  return (
    <div className="p-margin space-y-space-lg">
      {/* Wizard Header */}
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-space-md">
        <div className="space-y-1">
          <div className="flex items-center gap-space-sm">
            <button
              onClick={onBackToList}
              className="p-1 rounded text-outline hover:text-on-surface hover:bg-surface-container"
            >
              <span className="material-symbols-outlined text-[20px]">arrow_back</span>
            </button>
            <h1 className="font-headline-lg text-headline-lg text-on-surface tracking-tight">
              Tạo đề thi chuẩn V-SAT 2026
            </h1>
            <span className="px-2 py-0.5 rounded-full font-label-sm text-label-sm bg-primary/10 text-primary font-bold">
              Bước {currentStep}/4
            </span>
            {exam.id > 0 && (
              <span className="px-2 py-0.5 rounded text-[11px] font-mono bg-surface-container-high text-on-surface-variant">
                ID: #{exam.id}
              </span>
            )}
          </div>
          <p className="font-body-md text-body-md text-on-surface-variant">
            Quy trình 4 bước biên soạn, kiểm tra toàn vẹn 25 câu 150 điểm và sinh biến thể hoán vị
          </p>
        </div>

        <div className="flex items-center gap-space-sm">
          <button
            onClick={onBackToList}
            className="h-9 px-space-md rounded-lg bg-surface-container hover:bg-surface-container-high text-on-surface font-label-md text-label-md transition-colors"
          >
            Hủy bỏ
          </button>
          <button
            onClick={handleSaveDraft}
            disabled={isSaving}
            className="h-9 px-space-md rounded-lg bg-surface-container-high hover:bg-surface-variant text-primary font-label-md text-label-md flex items-center gap-1.5 transition-colors disabled:opacity-50"
          >
            <span className="material-symbols-outlined text-[18px]">bookmark</span>
            <span>{isSaving ? 'Đang lưu...' : 'Lưu nháp'}</span>
          </button>
        </div>
      </div>

      {/* Stepper Navigation */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-space-md">
        {steps.map((st) => {
          const isActive = currentStep === st.num;
          const isDone = currentStep > st.num;
          return (
            <div
              key={st.num}
              onClick={() => setCurrentStep(st.num)}
              className={`flex items-center gap-space-sm p-space-sm rounded-xl cursor-pointer transition-all border ${
                isActive
                  ? 'bg-primary/10 border-primary'
                  : isDone
                  ? 'bg-surface-container-lowest border-emerald-300'
                  : 'bg-surface-container-low border-outline-variant/30 opacity-80'
              }`}
            >
              <div
                className={`w-8 h-8 rounded-full flex items-center justify-center font-label-md font-bold shadow-sm flex-shrink-0 ${
                  isActive
                    ? 'bg-primary text-on-primary ring-4 ring-primary/20'
                    : isDone
                    ? 'bg-emerald-600 text-white'
                    : 'bg-surface-container-highest text-on-surface-variant'
                }`}
              >
                {isDone ? '✓' : st.num}
              </div>
              <div className="flex flex-col min-w-0">
                <span
                  className={`font-label-sm uppercase tracking-wider text-[10px] font-bold ${
                    isActive ? 'text-primary' : isDone ? 'text-emerald-700' : 'text-outline'
                  }`}
                >
                  Bước {st.num} {isActive && '· Hiện tại'}
                </span>
                <span className="font-label-md text-on-surface font-bold truncate text-[13px]">
                  {st.title}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {/* Step Body */}
      <div>
        {currentStep === 1 && <Step1General exam={exam} onChange={handleUpdate} />}
        {currentStep === 2 && <Step2Composition exam={exam} onChange={handleUpdate} />}
        {currentStep === 3 && <Step3Preview exam={exam} />}
        {currentStep === 4 && <Step4VariantsPublish exam={exam} onPublishSuccess={onBackToList} />}
      </div>

      {/* Footer Navigation */}
      <div className="flex items-center justify-between bg-surface-container-lowest p-space-md rounded-xl border border-outline-variant/30">
        <button
          onClick={() => setCurrentStep(prev => Math.max(1, prev - 1))}
          disabled={currentStep === 1}
          className="h-10 px-space-lg rounded-lg border border-outline-variant/40 bg-surface-container-low text-on-surface font-label-md disabled:opacity-40 flex items-center gap-2"
        >
          <span className="material-symbols-outlined text-[18px]">arrow_back</span>
          <span>Quay lại</span>
        </button>

        <div className="text-body-sm text-outline font-medium">
          Bước {currentStep} của 4
        </div>

        {currentStep < 4 ? (
          <button
            onClick={handleNextStep}
            disabled={isSaving}
            className="h-10 px-space-lg rounded-lg bg-primary hover:bg-tertiary text-on-primary font-label-md font-bold flex items-center gap-2 shadow-sm transition-all disabled:opacity-50"
          >
            <span>{isSaving ? 'Đang xử lý...' : 'Tiếp tục'}</span>
            <span className="material-symbols-outlined text-[18px]">arrow_forward</span>
          </button>
        ) : (
          <button
            onClick={onBackToList}
            className="h-10 px-space-lg rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-label-md font-bold flex items-center gap-2 shadow-sm transition-all"
          >
            <span>Hoàn tất & Quay lại</span>
          </button>
        )}
      </div>
    </div>
  );
};
