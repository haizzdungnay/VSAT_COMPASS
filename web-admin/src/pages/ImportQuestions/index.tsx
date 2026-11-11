import React, { useState, useRef } from 'react';
import type { QuestionImportResult } from '../../types/exam';
import { uploadQuestionFile, downloadImportTemplate, triggerBlobDownload } from '../../services/api';
import { MathView } from '../../components/MathView';

export const ImportQuestions: React.FC = () => {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [loading, setLoading] = useState(false);
  const [downloadingTemplate, setDownloadingTemplate] = useState(false);
  const [previewResult, setPreviewResult] = useState<QuestionImportResult | null>(null);
  const [commitResult, setCommitResult] = useState<QuestionImportResult | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [showWordGuide, setShowWordGuide] = useState<boolean>(false);
  const [isDragging, setIsDragging] = useState<boolean>(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const file = e.target.files[0];
      setSelectedFile(file);
      setPreviewResult(null);
      setCommitResult(null);
      setErrorMsg(null);
    }
  };

  const handleDrop = (e: React.DragEvent<HTMLDivElement>) => {
    e.preventDefault();
    setIsDragging(false);
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const file = e.dataTransfer.files[0];
      setSelectedFile(file);
      setPreviewResult(null);
      setCommitResult(null);
      setErrorMsg(null);
    }
  };

  const handleDownloadTemplate = async () => {
    try {
      setDownloadingTemplate(true);
      const blob = await downloadImportTemplate();
      triggerBlobDownload(blob, 'vsat_question_import_template.xlsx');
    } catch (err: unknown) {
      setErrorMsg(err instanceof Error ? err.message : 'Không thể tải file mẫu');
    } finally {
      setDownloadingTemplate(false);
    }
  };

  const handleDownloadJsonTemplate = () => {
    const sampleJson = [
      {
        sheetName: "PHAN_I_DUNG_SAI",
        rowNumber: 2,
        groupCode: "GRP_ENG_L1",
        groupTitle: "Bài nghe hội thoại 1",
        passageText: "Ngữ liệu bài nghe tiếng Anh V-SAT...",
        subjectCode: "ENG",
        questionType: "TRUE_FALSE",
        difficulty: "MEDIUM",
        questionText: "Mệnh đề câu hỏi dựa trên bài nghe...",
        options: ["Ý a đúng", "Ý b sai", "Ý c đúng", "Ý d sai"],
        correctAnswer: "T,F,T,F",
        explanation: "Giải thích chi tiết theo bài nghe",
        audioUrl: "https://storage.vsatcompass.edu.vn/audio/eng_sample_01.mp3",
        maxPlays: 2
      }
    ];
    const blob = new Blob([JSON.stringify(sampleJson, null, 2)], { type: 'application/json' });
    triggerBlobDownload(blob, 'vsat_question_import_template.json');
  };

  const handlePreview = async () => {
    if (!selectedFile) return;
    try {
      setLoading(true);
      setErrorMsg(null);
      const result = await uploadQuestionFile(selectedFile, true);
      setPreviewResult(result);
    } catch (err: any) {
      setErrorMsg(err.message || 'Lỗi khi kiểm tra file Excel');
    } finally {
      setLoading(false);
    }
  };

  const handleCommit = async () => {
    if (!selectedFile) return;
    try {
      setLoading(true);
      setErrorMsg(null);
      const result = await uploadQuestionFile(selectedFile, false);
      setCommitResult(result);
    } catch (err: any) {
      setErrorMsg(err.message || 'Lỗi khi import câu hỏi vào cơ sở dữ liệu');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = () => {
    setSelectedFile(null);
    setPreviewResult(null);
    setCommitResult(null);
    setErrorMsg(null);
    if (fileInputRef.current) fileInputRef.current.value = '';
  };

  return (
    <div className="p-margin max-w-7xl mx-auto space-y-6">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/30 shadow-sm">
        <div>
          <h1 className="font-headline-lg text-on-surface flex items-center gap-2">
            <span className="material-symbols-outlined text-primary text-[32px]">cloud_upload</span>
            Import câu hỏi từ file Excel (.xlsx)
          </h1>
          <p className="text-body-md text-outline mt-1 flex items-center gap-2 flex-wrap">
            <span>Hỗ trợ nhập hàng loạt câu hỏi đơn và chùm câu hỏi đọc hiểu / ngữ liệu chung kèm KaTeX.</span>
            <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full bg-primary/10 text-primary text-xs font-bold font-mono">
              Hỗ trợ: .xlsx, .json, .docx, .txt
            </span>
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            onClick={handleDownloadTemplate}
            disabled={downloadingTemplate}
            className="flex items-center gap-2 px-4 py-2.5 bg-secondary-container text-on-secondary-container hover:bg-secondary-container/80 rounded-xl font-label-md font-medium transition-colors border border-outline-variant/20 shadow-sm"
          >
            <span className="material-symbols-outlined text-[20px]">download</span>
            {downloadingTemplate ? 'Đang tải...' : 'Tải file Excel mẫu'}
          </button>

          <button
            onClick={handleDownloadJsonTemplate}
            className="flex items-center gap-1.5 px-3.5 py-2.5 bg-surface-container-high hover:bg-surface-container-highest text-on-surface rounded-xl font-label-md font-medium transition-colors border border-outline-variant/20 shadow-sm"
          >
            <span className="material-symbols-outlined text-[18px]">data_object</span>
            Tải mẫu JSON
          </button>

          <button
            onClick={() => setShowWordGuide(!showWordGuide)}
            className="flex items-center gap-1.5 px-3.5 py-2.5 bg-surface-container-high hover:bg-surface-container-highest text-on-surface rounded-xl font-label-md font-medium transition-colors border border-outline-variant/20 shadow-sm"
          >
            <span className="material-symbols-outlined text-[18px]">help_outline</span>
            {showWordGuide ? 'Ẩn hướng dẫn Word' : 'Hướng dẫn Word / TXT'}
          </button>
        </div>
      </div>

      {/* Word / TXT Format Guide */}
      {showWordGuide && (
        <div className="p-6 bg-surface-container-lowest border border-outline-variant/30 rounded-2xl space-y-3">
          <div className="flex items-center justify-between border-b border-outline-variant/20 pb-3">
            <h3 className="font-headline-sm text-on-surface flex items-center gap-2">
              <span className="material-symbols-outlined text-primary text-[22px]">description</span>
              Quy chuẩn định dạng file Word (.docx) & Plain Text (.txt)
            </h3>
            <button
              onClick={() => setShowWordGuide(false)}
              className="text-outline hover:text-on-surface text-sm"
            >
              Đóng
            </button>
          </div>
          <p className="text-body-sm text-outline">
            Hệ thống hỗ trợ parse tự động tài liệu Word hoặc Text định dạng theo cặp thẻ chuẩn V-SAT:
          </p>
          <pre className="p-4 rounded-xl bg-surface-container-low text-on-surface font-mono text-xs overflow-x-auto border border-outline-variant/30 leading-relaxed">
{`[Q]
[SUBJECT: ENG]
[TOPIC: Listening Comprehension]
[TYPE: SINGLE_CHOICE]
[DIFFICULTY: MEDIUM]
[AUDIO: https://storage.vsatcompass.edu.vn/audio/sample_01.mp3]
[MAX_PLAYS: 2]
[STEM: Listen to the conversation and answer the question: What is the main topic?]
[OPTIONS]
A. Environmental protection in modern cities
B. Economic growth and industry development
C. Scientific discovery in astronomy
D. Healthcare system improvements
[/OPTIONS]
[ANSWER: A]
[EXPLANATION: In the first passage, the speaker emphasizes urban green spaces.]
[/Q]`}
          </pre>
        </div>
      )}

      {/* Error Alert */}
      {errorMsg && (
        <div className="p-4 bg-error-container/20 border border-error/40 rounded-xl text-error flex items-start gap-3">
          <span className="material-symbols-outlined text-[22px] flex-shrink-0 mt-0.5">error</span>
          <div>
            <div className="font-bold text-body-md">Đã xảy ra lỗi</div>
            <div className="text-body-sm opacity-90">{errorMsg}</div>
          </div>
        </div>
      )}

      {/* Success Alert after commit */}
      {commitResult && (
        <div className="p-6 bg-emerald-500/10 border border-emerald-500/30 rounded-2xl text-emerald-800 space-y-3">
          <div className="flex items-center gap-3">
            <span className="material-symbols-outlined text-emerald-600 text-[32px]">check_circle</span>
            <div>
              <h3 className="font-headline-sm text-emerald-900 font-bold">Import câu hỏi thành công!</h3>
              <p className="text-body-md text-emerald-700">
                Đã thêm thành công {commitResult.importedQuestions} câu hỏi
                {commitResult.createdGroups > 0 ? ` và tạo ${commitResult.createdGroups} chùm bài đọc` : ''} vào ngân hàng câu hỏi.
              </p>
            </div>
          </div>
          <div className="flex gap-3 pt-2">
            <button
              onClick={handleReset}
              className="px-4 py-2 bg-emerald-600 text-white rounded-lg font-label-md font-semibold hover:bg-emerald-700 transition"
            >
              Tiếp tục import file khác
            </button>
          </div>
        </div>
      )}

      {/* File Upload Box (shown when not yet committed) */}
      {!commitResult && (
        <div className="bg-surface-container-lowest p-6 rounded-2xl border border-outline-variant/30 space-y-4">
          <h2 className="font-headline-sm text-on-surface">1. Chọn file đề thi (.xlsx, .json, .docx, .txt)</h2>

          <div
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => { e.preventDefault(); setIsDragging(true); }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={handleDrop}
            className={`border-2 border-dashed rounded-2xl p-8 text-center cursor-pointer transition-colors flex flex-col items-center justify-center gap-2 ${
              isDragging
                ? 'border-primary bg-primary/5'
                : 'border-outline-variant/60 hover:border-primary/60 bg-surface-container-low/30 hover:bg-surface-container-low/60'
            }`}
          >
            <input
              ref={fileInputRef}
              type="file"
              accept=".xlsx,.json,.docx,.txt"
              onChange={handleFileChange}
              className="hidden"
            />
            <span className="material-symbols-outlined text-primary text-[48px]">upload_file</span>
            <div className="font-title-md text-on-surface">
              {selectedFile ? selectedFile.name : 'Nhấp hoặc kéo thả file (.xlsx, .json, .docx, .txt) vào đây'}
            </div>
            <div className="text-body-sm text-outline">
              {selectedFile
                ? `Kích thước: ${(selectedFile.size / 1024).toFixed(1)} KB`
                : 'Chấp nhận định dạng .xlsx, .json, .docx, hoặc .txt theo cấu trúc mẫu V-SAT'}
            </div>
          </div>

          <div className="flex justify-end gap-3 pt-2">
            {selectedFile && (
              <button
                onClick={handleReset}
                disabled={loading}
                className="px-4 py-2 text-outline hover:text-on-surface font-label-md transition"
              >
                Hủy chọn
              </button>
            )}
            <button
              onClick={handlePreview}
              disabled={!selectedFile || loading}
              className="flex items-center gap-2 px-6 py-2.5 bg-primary text-on-primary hover:bg-primary/90 disabled:opacity-50 rounded-xl font-label-md font-semibold transition shadow-sm"
            >
              <span className="material-symbols-outlined text-[20px]">
                {loading ? 'progress_activity' : 'fact_check'}
              </span>
              {loading ? 'Đang phân tích...' : 'Kiểm tra trước dữ liệu (Preview)'}
            </button>
          </div>
        </div>
      )}

      {/* Preview Section */}
      {previewResult && !commitResult && (
        <div className="space-y-6">
          {/* Summary KPI Cards */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30">
              <span className="font-label-sm uppercase text-outline">Tổng số dòng</span>
              <div className="font-headline-lg font-bold text-on-surface mt-1">{previewResult.totalRows}</div>
            </div>
            <div className="bg-surface-container-lowest p-5 rounded-2xl border border-emerald-500/20 bg-emerald-500/5">
              <span className="font-label-sm uppercase text-emerald-700">Hợp lệ</span>
              <div className="font-headline-lg font-bold text-emerald-700 mt-1">{previewResult.validCount}</div>
            </div>
            <div className="bg-surface-container-lowest p-5 rounded-2xl border border-rose-500/20 bg-rose-500/5">
              <span className="font-label-sm uppercase text-rose-700">Dòng có lỗi</span>
              <div className="font-headline-lg font-bold text-rose-700 mt-1">{previewResult.errorCount}</div>
            </div>
            <div className="bg-surface-container-lowest p-5 rounded-2xl border border-outline-variant/30">
              <span className="font-label-sm uppercase text-outline">Chế độ</span>
              <div className="font-headline-sm font-semibold text-primary mt-1">Preview (Dry-run)</div>
            </div>
          </div>

          {/* Error messages box if any */}
          {previewResult.errors && previewResult.errors.length > 0 && (
            <div className="p-5 bg-amber-500/10 border border-amber-500/30 rounded-2xl space-y-2">
              <div className="font-bold text-amber-900 flex items-center gap-2">
                <span className="material-symbols-outlined text-[20px]">warning</span>
                Chi tiết các lỗi cần khắc phục ({previewResult.errors.length}):
              </div>
              <ul className="list-disc pl-5 space-y-1 text-body-sm text-amber-800">
                {previewResult.errors.map((err, i) => (
                  <li key={i}>{err}</li>
                ))}
              </ul>
            </div>
          )}

          {/* Preview Table */}
          <div className="bg-surface-container-lowest rounded-2xl border border-outline-variant/30 overflow-hidden shadow-sm">
            <div className="p-4 border-b border-outline-variant/20 flex justify-between items-center bg-surface-container-low/40">
              <h3 className="font-headline-sm text-on-surface">2. Bảng xem trước nội dung ({previewResult.items.length} câu)</h3>
              <div className="text-body-sm text-outline">Kiểm tra hiển thị công thức toán KaTeX và đáp án đúng</div>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left text-body-sm border-collapse">
                <thead>
                  <tr className="border-b border-outline-variant/20 bg-surface-container-low font-label-md text-on-surface-variant">
                    <th className="p-3 w-16">Hàng</th>
                    <th className="p-3 w-24">Trạng thái</th>
                    <th className="p-3 w-32">Sheet / Nhóm</th>
                    <th className="p-3 w-28">Độ khó</th>
                    <th className="p-3 min-w-[280px]">Nội dung câu hỏi & Lựa chọn</th>
                    <th className="p-3 w-24">Đáp án</th>
                    <th className="p-3 min-w-[200px]">Lời giải / Ghi chú</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-outline-variant/10">
                  {previewResult.items.map((item, idx) => (
                    <tr
                      key={idx}
                      className={item.valid ? 'hover:bg-surface-container-low/30' : 'bg-rose-500/5 hover:bg-rose-500/10'}
                    >
                      <td className="p-3 text-outline font-mono text-[12px]">{item.rowNumber}</td>
                      <td className="p-3">
                        {item.valid ? (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-emerald-500/10 text-emerald-700">
                            <span className="material-symbols-outlined text-[14px]">check</span>
                            Hợp lệ
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-[11px] font-bold bg-rose-500/10 text-rose-700">
                            <span className="material-symbols-outlined text-[14px]">close</span>
                            Lỗi
                          </span>
                        )}
                      </td>
                      <td className="p-3">
                        <div className="font-medium text-on-surface">{item.sheetName}</div>
                        {item.groupCode && (
                          <div className="text-[11px] text-primary font-mono mt-0.5">[{item.groupCode}]</div>
                        )}
                      </td>
                      <td className="p-3">
                        <span className="px-2 py-0.5 rounded bg-surface-container-high text-on-surface-variant font-mono text-[11px]">
                          {item.difficulty || 'MEDIUM'}
                        </span>
                      </td>
                      <td className="p-3 space-y-2">
                        {item.passageText && (
                          <div className="p-2.5 rounded-lg bg-surface-container-low text-[12px] border border-outline-variant/20 italic text-outline max-h-24 overflow-y-auto">
                            <strong className="not-italic text-on-surface">Ngữ liệu chung: </strong>
                            <MathView content={item.passageText} />
                          </div>
                        )}
                        {item.audioUrl && (
                          <div className="flex items-center gap-2 p-2 rounded-lg bg-sky-500/10 border border-sky-500/30 text-sky-900 text-xs">
                            <span className="material-symbols-outlined text-[16px] text-sky-600">volume_up</span>
                            <span className="font-mono truncate max-w-[220px]" title={item.audioUrl}>
                              {item.audioUrl}
                            </span>
                            <span className="ml-auto font-bold px-1.5 py-0.5 rounded bg-sky-200/60 text-sky-800 text-[10px]">
                              Tối đa {item.maxPlays || 2} lượt nghe
                            </span>
                          </div>
                        )}
                        <div className="font-medium text-on-surface">
                          <MathView content={item.questionText} />
                        </div>
                        {item.options && (
                          <div className="grid grid-cols-2 gap-1 text-[12px] pt-1">
                            {item.options.map((opt, oIdx) => {
                              const label = String.fromCharCode(65 + oIdx);
                              const isCorrect = item.correctAnswer?.toUpperCase().includes(label);
                              return (
                                <div
                                  key={oIdx}
                                  className={`px-2 py-1 rounded ${
                                    isCorrect
                                      ? 'bg-emerald-500/15 text-emerald-800 font-semibold border border-emerald-500/30'
                                      : 'bg-surface-container-low text-on-surface-variant'
                                  }`}
                                >
                                  <span className="font-bold mr-1">{label}.</span>
                                  <MathView content={opt} />
                                </div>
                              );
                            })}
                          </div>
                        )}
                        {item.error && (
                          <div className="text-[12px] text-rose-600 font-semibold flex items-center gap-1 pt-1">
                            <span className="material-symbols-outlined text-[14px]">error</span>
                            {item.error}
                          </div>
                        )}
                      </td>
                      <td className="p-3">
                        <span className="font-mono font-bold text-primary px-2 py-1 rounded bg-primary-container/20">
                          {item.correctAnswer}
                        </span>
                      </td>
                      <td className="p-3 text-[12px] text-outline">
                        <MathView content={item.explanation || '-'} />
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>

            {/* Bottom Commit Action Bar */}
            <div className="p-5 bg-surface-container-low/40 border-t border-outline-variant/20 flex flex-col sm:flex-row justify-between items-center gap-4">
              <div className="text-body-sm text-outline">
                {previewResult.errorCount > 0
                  ? `Có ${previewResult.errorCount} dòng lỗi. Vui lòng chỉnh sửa file Excel để khắc phục toàn bộ lỗi trước khi nạp vào CSDL.`
                  : previewResult.validCount > 0
                    ? `Sẵn sàng nạp ${previewResult.validCount} câu hỏi hợp lệ vào cơ sở dữ liệu.`
                    : 'Không có câu hỏi hợp lệ nào để nạp.'}
              </div>

              <div className="flex gap-3">
                <button
                  onClick={handleReset}
                  disabled={loading}
                  className="px-4 py-2 text-outline hover:text-on-surface font-label-md transition"
                >
                  Hủy bỏ
                </button>
                <button
                  onClick={handleCommit}
                  disabled={loading || previewResult.validCount === 0 || previewResult.errorCount > 0}
                  className="flex items-center gap-2 px-6 py-2.5 bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 rounded-xl font-label-md font-semibold transition shadow-sm"
                >
                  <span className="material-symbols-outlined text-[20px]">
                    {loading ? 'progress_activity' : 'cloud_done'}
                  </span>
                  {loading ? 'Đang nạp vào DB...' : `Xác nhận nạp (${previewResult.validCount} câu)`}
                </button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
