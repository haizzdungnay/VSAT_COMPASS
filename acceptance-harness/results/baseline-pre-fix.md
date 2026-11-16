# V-SAT Compass - Baseline Regression Report (Pre-Fix)

Thời điểm đo: 2026-11-13
Môi trường: Windows 11, Git Bash, JDK 21 (Eclipse Adoptium 21.0.7+6-LTS), Docker vsat-clean-pg (127.0.0.1:55432), Backend Spring Boot (127.0.0.1:18080).

---

## 1. Bảng tổng hợp Acceptance Test Suites

| Suite | Tổng số ca | PASS | FAIL | Trạng thái / Ghi chú môi trường |
|---|---|---|---|---|
| `backend-admin` (`backend-admin-report.json`) | 42 | 42 | 0 | Chạy đầy đủ qua REST API |
| `backend-public-student-collab` (`backend-psc-report.json`) | 36 | 36 | 0 | Chạy đầy đủ qua REST API |
| `web-suite` (`web-report.json`) | 10 | 10 | 0 | Chạy đầy đủ qua Vite server (port 15173) + Headless Browser |
| `android-student` (`android-student-report.json`) | 20 | 20 | 0 | Chạy kiểm tra tĩnh layout, manifest, asset KaTeX, offline crypto (không có thiết bị thật/emulator cắm qua ADB nên bỏ qua test tương tác thiết bị) |
| `android-roles-workers` (`android-roles-workers-report.json`) | 18 | 18 | 0 | Chạy kiểm tra tĩnh layout, manifest, RBAC UI, worker definition |
| `phase2-features-check` | 4 checks | 4 | 0 | E2E Admission Matching, TD-4 Optimistic Locking 409, P4 Proctoring Auto-Submit, P1 Web CBT UI |
| `vsat-format` (`vsat-format.json`) | 44 | 13 | 31 | Khớp chính xác baseline kỳ vọng: 13 PASS / 31 FAIL |

---

## 2. Chi tiết 31 ca FAIL của suite `vsat-format`

| Mã ca | Mô tả ca kiểm thử | Lý do thất bại chi tiết (1 dòng) |
|---|---|---|
| **VF-A01** | Đề 25 câu chuẩn: publish 200 và oracle PASS | Publish thành công (200) nhưng oracle V-SAT kiểm tra đề đã publish bị lỗi thiếu cấu hình profile (`VF_PROFILE`). |
| **VF-A02** | Publish đề 1 câu bị chặn (G1) | Trả về HTTP 200 thay vì HTTP 400 (chưa chặn đề thi không đủ 25 câu theo chuẩn V-SAT). |
| **VF-A03** | Publish đề 25 câu toàn 4 lựa chọn bị chặn VF_MIX (G1) | Trả về HTTP 200 thay vì HTTP 400 (chưa kiểm tra và chặn tỷ lệ dạng câu sai lệch 9/6/5/5). |
| **VF-A04** | Publish đề 26 câu bị chặn (G1) | Trả về HTTP 200 thay vì HTTP 400 (chưa chặn đề thi vượt quá 25 câu). |
| **VF-A05** | Thêm câu môn ENG vào đề MATH bị chặn (G2) | Trả về HTTP 200 thay vì HTTP 400 (chưa kiểm tra môn học của câu hỏi phải khớp môn học của đề thi). |
| **VF-A08** | Tạo đề MATH 60 phút bị chặn (G17) | Trả về HTTP 201 thay vì HTTP 400 (chưa validate ràng buộc thời lượng thi môn Toán chuẩn V-SAT là 90 phút). |
| **VF-A09** | Publish đề có câu Đ/S chỉ 3 ý bị chặn (G4) | Trả về HTTP 200 thay vì HTTP 400 (chưa chặn publish khi câu Đúng/Sai không đủ 4 ý a-d). |
| **VF-A10** | CONTENT_ADMIN publish trả 403, không tự chuyển submit-review (G15) | Web api.ts tự động fallback gọi submit-review khi bị 403 thay vì giữ nguyên lỗi cấm publish cho CONTENT_ADMIN. |
| **VF-B01** | Sinh đề từ VSAT_2026_BP_MATH ra đúng 9/6/5/5, oracle PASS (G3) | Đề sinh ra vi phạm toàn bộ kiểm tra oracle V-SAT: `VF_MATCH, VF_MIX, VF_ORDER, VF_PROFILE, VF_SA_KEY, VF_TF4`. |
| **VF-B02** | Ngân hàng ENG toàn 4 lựa chọn: sinh đề phải 400 hoặc cảnh báo thiếu dạng (G3) | Sinh đề thành công thay vì báo lỗi HTTP 400 hoặc cảnh báo khi thiếu dạng câu Đ/S, ghép hợp, trả lời ngắn. |
| **VF-B03** | Sinh đề vào đề PUBLISHED bị chặn 409 (G3) | Trả về HTTP 200 thay vì HTTP 409 (cho phép sinh đè câu hỏi vào đề thi đã phát hành). |
| **VF-B04** | blueprintId 10 câu được tôn trọng hoặc bị từ chối (G3/G12) | Không sinh đúng 10 câu theo blueprint cũng không từ chối lỗi HTTP 400. |
| **VF-B05** | rawScore trả về khớp tổng điểm thực của đề (G3) | rawScore trả về 150 điểm cố định thay vì 18 điểm (tổng điểm thực tế các câu hỏi được chọn). |
| **VF-C03** | DRAFT đổi môn sang ENG nhưng giữ câu MATH, publish bị chặn (G2) | Trả về HTTP 200 thay vì HTTP 400 (không re-validate môn của các câu hỏi hiện có khi đổi môn đề thi). |
| **VF-C04** | HIDDEN đổi môn rồi publish lại phải chạy lại validator (G2) | Trả về HTTP 200 thay vì HTTP 400 (publish lại đề HIDDEN sai môn không kích hoạt validator). |
| **VF-C05** | Return-to-draft, bỏ 1 câu Đ/S rồi publish lại bị chặn (G1) | Trả về HTTP 200 thay vì HTTP 400 (đề còn 24 câu sau khi bỏ câu Đ/S vẫn cho publish lại). |
| **VF-C08** | Reorder làm 4 lựa chọn đứng trước Đ/S bị chặn VF_ORDER | Trả về HTTP 200 thay vì HTTP 400 (không chặn vi phạm thứ tự cố định: Đ/S -> 4 lựa chọn -> Ghép hợp -> TL ngắn). |
| **VF-D01** | Import Đ/S + 4LC hợp lệ: câu vào trạng thái chờ duyệt, đúng cấu trúc (G8) | Câu import hợp lệ nhận trạng thái APPROVED thay vì PENDING_REVIEW (chưa qua bước duyệt của Admin). |
| **VF-D02** | Import dạng câu lạ "ABC" phải là dòng lỗi (G8) | Dòng câu hỏi bị tự động chuyển đổi thành SINGLE_CHOICE thay vì đánh dấu là dòng lỗi import. |
| **VF-D03** | Import SINGLE_CHOICE đáp án "A,B" phải là dòng lỗi (G8) | Dòng câu hỏi 4 lựa chọn có nhiều hơn 1 đáp án đúng không bị đánh dấu là dòng lỗi. |
| **VF-D04** | Import Đ/S chỉ 3 ý phải là dòng lỗi (G4/G8) | Câu Đúng/Sai chỉ có 3 ý không bị đánh dấu là dòng lỗi import. |
| **VF-D05** | Import trả lời ngắn đáp án số 12.5 hợp lệ (G7/G8) | Câu trả lời ngắn có đáp án dạng số thập phân hợp lệ (12.5) bị báo lỗi dòng import. |
| **VF-D06** | Import ghép hợp 4 cặp 1-C,2-A,3-D,4-B lưu được khóa ghép (G6/G8) | Không lưu được khóa ghép hợp cho câu hỏi dạng MATCHING khi import. |
| **VF-E02** | Tạo blueprint totalQuestions=10 bị chặn (G12) | Trả về HTTP 200 thay vì HTTP 400 (blueprint V-SAT chưa bị ép buộc phải đúng 25 câu). |
| **VF-E03** | Blueprint có ô âm / dạng câu trùng bị chặn (G12) | Trả về HTTP 200 thay vì HTTP 400 (chưa validate cấu trúc ô ma trận: số lượng câu âm hoặc trùng dạng). |
| **VF-F01** | Bundle của đề chuẩn mô tả đủ 4 phần và điểm (G14) | Bundle đề thi tải về thiếu thông tin tổng điểm và phân chia 4 phần thi (`VF_TOTAL`). |
| **VF-F02** | Bundle của học sinh không chứa đáp án (G13) | Bundle mã hóa cho học sinh để lộ toàn bộ đáp án (`isCorrect`, `correctAnswer`, `correctOptionIds`, khóa ghép, khóa TLN). |
| **VF-F03** | correctAnswer của câu Đ/S (nếu có) phải đủ 4 cờ Đ/S (G14) | Thuộc tính `correctAnswer` của câu Đúng/Sai không đảm bảo định dạng đủ 4 cờ Đ/S. |
| **VF-Q01** | Tạo câu Đ/S 3 ý bị chặn (G4) | Trả về HTTP 201 thay vì HTTP 400 (API cho phép tạo câu Đúng/Sai chỉ có 3 ý thay vì bắt buộc 4 ý). |
| **VF-Q02** | Tạo câu 4 lựa chọn chỉ 2 phương án bị chặn (G5) | Trả về HTTP 201 thay vì HTTP 400 (API cho phép tạo câu hỏi 4 lựa chọn chỉ với 2 phương án). |
| **VF-Q03** | Tạo câu ghép hợp 2 cặp bị chặn (G6) | Trả về HTTP 201 thay vì HTTP 400 (API cho phép tạo câu ghép hợp chỉ với 2 cặp thay vì đủ 4 cặp). |

---

## 3. Số liệu Backend Gradle Test (Spring Boot API)

- Lệnh thực thi:
  ```bash
  JAVA_HOME="C:\Users\PC\.gradle\jdks\eclipse_adoptium-21-amd64-windows.2" ./gradlew test --console=plain
  ```
- Thư mục: `VSAT/vsat-compass-api`
- Tổng số test: **371**
- Số test PASS: **371**
- Số test FAIL: **0** (failures: 0, errors: 0, skipped: 0)
- Danh sách test fail: Không có (tất cả test unit/integration backend đều vượt qua).
