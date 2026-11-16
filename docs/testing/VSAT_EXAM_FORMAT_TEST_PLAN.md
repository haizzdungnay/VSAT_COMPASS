# Kế hoạch kiểm thử: Thêm đề / Cập nhật đề có đúng định dạng V-SAT

Ngày lập: 2026-11-12 · Phạm vi: backend `VSAT/vsat-compass-api`, web admin `web-admin`, bundle tải đề (Android/offline).
Môi trường: chỉ local / `acceptance-harness/docker-compose.clean.yml`. Cấm trỏ vào DB/API production.

---

## 1. Chuẩn V-SAT (oracle để đối chiếu)

Nguồn chính thức đã đọc trực tiếp:
- Báo Chính phủ: https://baochinhphu.vn/bo-gddt-cong-bo-de-minh-hoa-bai-thi-v-sat-de-tuyen-sinh-dai-hoc-102241112092343702.htm
- Thanh Niên: https://thanhnien.vn/de-minh-hoa-ky-thi-v-sat-nam-2025-185241110153411905.htm

Nguồn tham khảo, chưa đọc trực tiếp:
- Đề minh họa ĐH Cần Thơ: https://vsat.ctu.edu.vn/de-thi-minh-hoa

| Quy tắc | Giá trị chuẩn | Mức chắc chắn |
|---|---|---|
| Môn thi | 8 môn: Toán, Lý, Hóa, Sinh, Sử, Địa, Tiếng Anh, Ngữ văn | Chính thức (Thanh Niên) |
| Thời gian | Toán và Ngữ văn 90 phút, các môn khác 60 phút | Chính thức |
| Tổng điểm thô | 150 | Chính thức |
| Số câu | 25 câu, "khoảng 85 tiểu mục" | Chính thức. Con số 85 không khớp phép cộng 36+6+20+5=67, nên **không dùng làm assertion** |
| Phần 1: Đúng/Sai | 9 câu, mỗi câu có đúng 4 ý a–d, tổng 54đ. Chấm theo số ý đúng: 0→0, 1→1, 2→2, 3→3, 4→6 | Thang điểm là chính thức. Số câu suy ra từ 54/6 |
| Phần 2: Trắc nghiệm 4 lựa chọn | 6 câu, mỗi câu 6đ, tổng 36đ. Đúng 4 phương án, đúng 1 đáp án. Có thể là nhóm câu cùng ngữ cảnh (passage) | Chính thức. Số câu suy ra |
| Phần 3: Ghép hợp | 5 câu, mỗi câu 4 tiểu mục, tổng 30đ. Spec repo chấm 1.5đ/ý | Chính thức. Số phương án ghép 5–6 chưa xác minh |
| Phần 4: Trả lời ngắn | 5 câu, mỗi câu 6đ, tổng 30đ. Bắt buộc có đáp án TEXT hoặc NUMERIC | Chính thức. Số câu suy ra |
| Ngữ văn | Phần 4 là 1 bài luận khoảng 200 chữ, 30đ, chấm tay (tổng 21 câu) | Có viết luận là chính thức. Con số 21 câu là suy ra |
| Thứ tự phần | Đúng/Sai, 4 lựa chọn, Ghép hợp, Trả lời ngắn | Theo thứ tự công bố và spec repo |
| Tiếng Anh nghe | Không có phần nghe | Chưa xác minh trực tiếp |

---

## 2. Hiện trạng: chuẩn V-SAT so với code (đã kiểm tra trực tiếp)

| # | Quy tắc V-SAT | Code hiện tại | Bằng chứng | Mức |
|---|---|---|---|---|
| G1 | Publish phải đủ 25 câu theo tỉ lệ 9/6/5/5 và tổng 150đ | Chỉ kiểm tra có ≥1 câu, số đếm khớp, câu đã APPROVED, đề miễn phí | `AdminExamServiceImpl.java` `validatePublishReadiness` (~L545) | CRITICAL |
| G2 | Câu trong đề phải cùng môn với đề | `addQuestion` không kiểm tra môn. Đổi môn khi DRAFT/HIDDEN vẫn giữ câu của môn cũ | `AdminExamServiceImpl.java` ~L188–212, ~L151 | HIGH |
| G3 | Sinh đề theo ma trận phải tuân thủ blueprint | Lấy 25 câu đầu tiên cùng môn, không chia theo dạng câu, không kiểm tra trạng thái đề (ghi đè được đề đã PUBLISHED), trả `rawScore=150` cứng | `generateByMatrixForExam` ~L449–486 | CRITICAL |
| G4 | Câu Đúng/Sai có đúng 4 ý, mỗi ý có đáp án Đ/S | Chấp nhận 2–4 option, không có luật nào về đáp án | `QuestionServiceImpl.java` ~L397 | HIGH |
| G5 | Câu 4 lựa chọn có đúng 4 option | Chấp nhận ≥2 option | `QuestionServiceImpl.validateOptionsForType` | MEDIUM |
| G6 | Câu ghép hợp có 4 tiểu mục, mỗi tiểu mục trỏ về 1 phương án hợp lệ | Chỉ kiểm tra có 2–6 option. Bảng `question_matching_items` không được API ghi | `QuestionServiceImpl.java` ~L403, V5 | HIGH |
| G7 | Câu trả lời ngắn có đáp án TEXT/NUMERIC (kèm sai số) | Lưu đáp án dưới dạng option. Bảng `question_short_answer_keys` không được ghi | V5, `QuestionServiceImpl` | HIGH |
| G8 | Import phải kiểm tra như nhập tay và đi qua bước duyệt | Lưu thẳng APPROVED. Dạng câu lạ tự thành SINGLE_CHOICE. Đáp án chỉ nhận A–D. Không gọi `validateOptionsForType` | `QuestionImportServiceImpl.java` L799, L817, ~L741 | CRITICAL |
| G9 | Lưu phần thi (section) và điểm từng câu | `exam_sections`, `exam_questions.max_raw_score`, `exams.scoring_profile_id` không có trong entity Java. Điểm 6.00 bị gán cứng | `ExamQuestion.java`, `Exam.java`, `getExamQuestions` ~L372 | HIGH |
| G10 | Profile chấm điểm hợp lệ | V17 gán `VSAT_2026_DEFAULT` nhưng profile này chưa từng được seed, nên blueprint nhận NULL. Web dùng mặc định `VSAT_2026_RAW_150`, cũng không tồn tại. Chỉ có `VSAT_2026_DRAFT_SPEC_150` là có thật | `V17__add_blueprint_cells_json.sql:14`, `Step1General.tsx:77-81` | HIGH |
| G11 | Có 8 môn (hoặc ít nhất 7 theo spec) | Chỉ seed MATH và ENG | `V0__base_schema.sql` ~L784 | HIGH |
| G12 | Blueprint đúng 25 câu / 150đ / 9-6-5-5 | Chỉ kiểm tra tổng số câu trong các ô bằng `totalQuestions` (giá trị 10 vẫn được chấp nhận) | `AdminBlueprintServiceImpl.java` ~L130, L217 | MEDIUM |
| G13 | Đề tải về không lộ đáp án cho thí sinh | Bundle chứa `isCorrect`, `correctOptionIds`, `correctAnswer`, `explanation` | `ExamServiceImpl.java` L174–216 | CRITICAL (cần quyết định thiết kế, xem mục 5) |
| G14 | Bundle mô tả được 4 phần thi và điểm | Không có trường section, điểm, profile. `correctAnswer` chỉ lấy đáp án đúng đầu tiên, nên sai với câu Đúng/Sai | `ExamServiceImpl.java` L178 | HIGH |
| G15 | Wizard web chặn publish khi đề chưa đạt | Các check chỉ để hiển thị, một số gán cứng `ok:true`. Thứ tự phần ngược chuẩn (Part 1 là SINGLE_CHOICE). Khi gặp 403, `publishExam` tự chuyển sang `submit-review` mà không báo | `Step3Preview.tsx:23-44`, `api.ts:512-520` | HIGH |
| G16 | Import lỗi phải báo lỗi | Khi backend lỗi, `uploadQuestionFile` giả lập kết quả thành công | `api.ts` ~L1240–1310 | HIGH |
| G17 | Thời lượng đề đúng 90/60 theo môn | Không kiểm tra | `AdminExamServiceImpl` create/update | LOW |

Tài liệu nội bộ mâu thuẫn và cần sửa theo chuẩn:
- `INDEPENDENT_AUTO_ACCEPTANCE_PLAN.md` ghi 40 câu (L99, L119).
- File này dùng thang Đ/S 0.1/0.25/0.5/1.0 (L246), trong khi SPEC L97 cấm thang này.
- File này đặt thứ tự phần với SINGLE_CHOICE đứng đầu (L302).
- File này dùng trạng thái `REJECTED`, không có trong enum (L114).

Hiện không có test nào (unit hoặc harness) kiểm tra định dạng V-SAT.

---

## 3. Oracle tự động

Tạo 1 file `acceptance-harness/lib/vsat-format.mjs` (Node, không dùng dependency) với các hàm:
- `validateVsatExam(examDetail, questions, subjectCode)`: trả về danh sách lỗi, mỗi lỗi có mã `VF_*`.
- `scoreVsat(answers, keys)`: chấm theo bảng ở mục 1 để đối chiếu với điểm do server trả về.

Các luật oracle kiểm tra (mỗi luật ứng với 1 mã lỗi):

| Mã | Luật |
|---|---|
| `VF_COUNT` | Đủ 25 câu (Ngữ văn 21) |
| `VF_MIX` | Đúng tỉ lệ 9/6/5/5 (Ngữ văn 9/6/5/1 essay) |
| `VF_ORDER` | Các phần nối tiếp đúng thứ tự Đ/S, 4 lựa chọn, ghép hợp, trả lời ngắn |
| `VF_TOTAL` | Tổng điểm 150 |
| `VF_TF4` | Câu Đúng/Sai có đúng 4 ý, mỗi ý có đáp án |
| `VF_MCQ4` | Câu 4 lựa chọn có 4 option, đúng 1 đáp án |
| `VF_MATCH` | Câu ghép hợp có 4 tiểu mục, các key ghép đều tồn tại |
| `VF_SA_KEY` | Câu trả lời ngắn có đáp án |
| `VF_SUBJECT` | Mọi câu cùng môn với đề |
| `VF_STATUS` | Mọi câu đã APPROVED |
| `VF_DUP` | Không có câu trùng |
| `VF_DURATION` | Thời lượng 90 hoặc 60 theo môn |
| `VF_PROFILE` | Profile chấm điểm tồn tại |
| `VF_KEY_LEAK` | Bundle của học sinh không chứa đáp án (nếu chốt như vậy ở mục 5) |

Oracle có tự kiểm tra kèm theo (`node acceptance-harness/lib/vsat-format.mjs --self-check`):
- 1 đề chuẩn phải PASS.
- 12 đề lỗi phải FAIL, mỗi đề đúng 1 mã lỗi.

---

## 4. Ca kiểm thử

Ký hiệu: "Kỳ vọng" là hành vi đúng chuẩn V-SAT. "Hiện tại" là kết quả dự đoán từ code (FAIL nghĩa là phát hiện lỗi).

**Manual/deferred, không đưa vào verdict-gate:** VF-D07, VF-E05, VF-G01–G03 cần kiểm thử UI thủ công. `verdict-gate.mjs` không có trạng thái manual/deferred; 5 ca này không có kết quả trong `results/vsat-format.json` nên giữ ngoài registry. Còn 44 ID VF có kết quả tự động được đăng ký trong feature inventory.


### Dữ liệu mẫu (seed vào DB sạch)
- Môn MATH và ENG, mỗi môn có ≥2 bộ câu chuẩn: 9 Đ/S, 6 câu 4 lựa chọn, 5 ghép hợp, 5 trả lời ngắn, tất cả APPROVED.
- Thêm câu "bẩn":
  - Đ/S chỉ có 3 ý
  - 4 lựa chọn có 2 đáp án đúng
  - Ghép hợp chỉ có 2 cặp
  - Trả lời ngắn không có đáp án
  - Câu ở trạng thái DRAFT
  - Câu thuộc môn khác
- Dữ liệu import: 1 file mỗi định dạng (xlsx/json/docx/txt), mỗi file có đủ 4 dạng câu và một số dòng cố ý lỗi.

### A. Thêm đề thủ công (create → add → submit-review → publish)

| ID | Kịch bản | Kỳ vọng | Hiện tại |
|---|---|---|---|
| VF-A01 | Tạo đề đủ 25 câu chuẩn và publish | 200, oracle PASS, bundle đủ 4 phần | Publish PASS, nhưng oracle dự kiến FAIL `VF_TOTAL`/`VF_ORDER` vì thiếu section/điểm (G9) |
| VF-A02 | Publish đề chỉ có 1 câu | 400 | 200 (G1) |
| VF-A03 | 25 câu nhưng toàn câu 4 lựa chọn | 400 `VF_MIX` | 200 (G1) |
| VF-A04 | 26 câu | 400 | 200 |
| VF-A05 | Thêm câu môn ENG vào đề MATH | 400 | 200 (G2) |
| VF-A06 | Thêm câu ở trạng thái DRAFT | 400 | 400 PASS |
| VF-A07 | Thêm cùng một câu 2 lần | 409/400 | PASS |
| VF-A08 | Tạo đề MATH với thời lượng 60 phút | 400 hoặc cảnh báo | 200 (G17) |
| VF-A09 | Đề có câu Đ/S chỉ 3 ý | Chặn ở bước publish | Lọt (G4) |
| VF-A10 | Gọi publish bằng CONTENT_ADMIN | 403, không tự chuyển sang submit-review | Web tự chuyển, user không biết (G15) |

### B. Sinh đề theo ma trận

| ID | Kịch bản | Kỳ vọng | Hiện tại |
|---|---|---|---|
| VF-B01 | Sinh đề từ `VSAT_2026_BP_MATH` khi ngân hàng đủ câu | Đúng 9/6/5/5, cùng môn, oracle PASS | Chỉ đúng nếu thứ tự `findAll` tình cờ khớp (G3) |
| VF-B02 | Ngân hàng chỉ có 30 câu SINGLE_CHOICE | 400 hoặc cảnh báo thiếu dạng câu | 200, đề toàn 4 lựa chọn (G3) |
| VF-B03 | Sinh đề vào đề đã PUBLISHED | 409 | Ghi đè đề đang phát hành (G3) |
| VF-B04 | Truyền `blueprintId` của blueprint 10 câu | Đề có 10 câu, hoặc từ chối nếu blueprint không hợp lệ | Bỏ qua tham số, luôn lấy 25 câu |
| VF-B05 | `rawScore` trả về so với tổng điểm thực tính | Bằng nhau | Luôn là 150 dù số câu ít hơn |

### C. Cập nhật đề

| ID | Kịch bản | Kỳ vọng | Hiện tại |
|---|---|---|---|
| VF-C01 | PUT đề đang PUBLISHED | 409 | PASS |
| VF-C02 | Thêm/xóa/sắp xếp câu khi đề HIDDEN | 409 | PASS |
| VF-C03 | Đổi môn khi DRAFT nhưng giữ câu của môn cũ, rồi publish | 400 `VF_SUBJECT` | 200 (G2) |
| VF-C04 | HIDDEN → sửa metadata → publish lại | Phải duyệt lại, hoặc ít nhất chạy lại validator | Publish thẳng |
| VF-C05 | Return-to-draft → thay câu → publish | Validator chạy lại, oracle PASS | Chỉ chạy validator yếu |
| VF-C06 | Sửa câu đã APPROVED đang nằm trong đề PUBLISHED | Bị chặn (chỉ sửa được khi DRAFT/NEEDS_REVISION) | PASS |
| VF-C07 | Hai admin cùng sửa (cùng `lockVersion`) | Người thứ hai nhận 409 | PASS |
| VF-C08 | Reorder làm Đ/S và 4 lựa chọn xen kẽ nhau | 400 `VF_ORDER` (vì phần thi cố định thứ tự) | 200 |

### D. Import câu hỏi (xlsx / json / docx / txt)

| ID | Kịch bản | Kỳ vọng | Hiện tại |
|---|---|---|---|
| VF-D01 | Import đủ 4 dạng câu hợp lệ | Có câu đúng dạng, đúng cấu trúc ý, trạng thái chờ duyệt | Lưu thẳng APPROVED (G8) |
| VF-D02 | Dòng có dạng câu `ABC` | Lỗi dòng | Tự thành SINGLE_CHOICE (G8) |
| VF-D03 | SINGLE_CHOICE với đáp án "A,B" | Lỗi dòng | Lọt (G8) |
| VF-D04 | Đ/S chỉ có 3 ý | Lỗi `TF_REQUIRES_4_STATEMENTS` | Lọt |
| VF-D05 | Trả lời ngắn với đáp án số `12.5` | Lưu NUMERIC kèm sai số | Bị chặn bởi regex A–D |
| VF-D06 | Ghép hợp với 4 cặp 1→C, 2→A… | Lưu được các cặp ghép | Không lưu được cặp ghép (G6) |
| VF-D07 | Backend trả 500 khi import trên web | UI báo lỗi | UI giả lập thành công (G16) |
| VF-D08 | dryRun rồi commit | Số dòng hợp lệ ở 2 lần giống nhau, không ghi DB khi dryRun | Cần chạy để biết |
| VF-D09 | File 1001 dòng hoặc lớn hơn 10MB | 400 | PASS |

### E. Blueprint

| ID | Kịch bản | Kỳ vọng | Hiện tại |
|---|---|---|---|
| VF-E01 | GET blueprint đã seed | 25 câu / 150đ / 9-6-5-5 / có profile | Có profile NULL (G10) |
| VF-E02 | POST với `totalQuestions=10` | 400 nếu bắt buộc chuẩn V-SAT | 200 (G12) |
| VF-E03 | Một ô có `questionCount` âm, hoặc dạng câu trùng | 400 | Lọt |
| VF-E04 | `scoringProfileId` không tồn tại | 400 | Lọt |
| VF-E05 | Xóa blueprint đang được dùng | 409 | Xóa được |

### F. Xuất đề (download bundle)

| ID | Kịch bản | Kỳ vọng | Hiện tại |
|---|---|---|---|
| VF-F01 | Bundle của đề A01 qua oracle | PASS | FAIL vì thiếu section/điểm (G14) |
| VF-F02 | Bundle với tài khoản học sinh | Không có trường đáp án (tùy quyết định ở mục 5) | Lộ đáp án (G13) |
| VF-F03 | `correctAnswer` của câu Đ/S | Có đủ 4 cờ Đ/S | Chỉ có 1 nhãn (G14) |
| VF-F04 | Chấm lại offline bằng `scoreVsat` so với điểm server | Bằng nhau trên 10 bộ đáp án ngẫu nhiên | Cần chạy để biết |
| VF-F05 | Tải bundle khi đề chưa PUBLISHED | 404 | PASS |

### G. Web admin wizard (Playwright, chạy local)

| ID | Kịch bản | Kỳ vọng | Hiện tại |
|---|---|---|---|
| VF-G01 | Đề có 0 câu ở Step3 | Các check đỏ, nút Publish bị khóa | Check xanh, Publish mở (G15) |
| VF-G02 | Nhãn phần thi | Part 1 là Đúng/Sai | Part 1 là SINGLE_CHOICE |
| VF-G03 | Dropdown profile chấm điểm | Chỉ hiện các profile có thật trong DB | Hiện `VSAT_2026_RAW_150`, không tồn tại |

---

## 5. Thứ tự thực hiện

1. **Chốt spec** (cần user quyết định, xem các câu hỏi bên dưới). Sau đó sửa `INDEPENDENT_AUTO_ACCEPTANCE_PLAN.md` cho khớp mục 1.
2. **Oracle**: viết `vsat-format.mjs` kèm self-check.
3. **Suite màu đỏ**: viết `acceptance-harness/suites/vsat-format.mjs` gồm VF-A..F, nối vào `run-all.mjs`. Chạy để ghi nhận baseline FAIL.
4. **Sửa** theo thứ tự rủi ro:
   1. G1/G3: validator publish và sinh ma trận theo blueprint
   2. G8: import phải validate và đưa câu về trạng thái chờ duyệt
   3. G13: bundle
   4. G4–G7, G9: cấu trúc từng dạng câu, section, điểm
   5. G10/G11: migration seed profile và môn
   6. G15/G16: web
5. **Xanh**: suite VF phải đạt 100%, `./gradlew test` không có regression. Bổ sung unit test cho validator publish trong `AdminExamServiceTest`.
6. **Nghiệm thu độc lập** theo skill `auto-acceptance-loop` (bước 5–7).

Điều kiện kết thúc:
- Tất cả ca VF-A..G PASS.
- Oracle self-check PASS.
- Không có FAIL nào mới trong `AdminExamServiceTest`, `QuestionServiceTest`, `QuestionImportServiceTest`, `ExamServiceTest`.
