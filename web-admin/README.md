# V-SAT Compass — Web Admin & Student CBT Portal

Phiên bản: **v1.2.0** | Node.js: **>=20** | React: **19.2** | TypeScript: **6.0** | Vite: **8.3**

Cổng thông tin quản trị học thuật và phân hệ thi trực tuyến trên máy tính (Computer-Based Testing - CBT) cho kỳ thi đánh giá năng lực V-SAT.

---

## 1. Kiến trúc & Công nghệ

- **Core**: React 19, TypeScript 6, Vite 8, Tailwind CSS
- **Toán học & Khoa học**: KaTeX 0.19 (offline render qua component `MathView`)
- **Âm thanh**: HTML5 Audio API với pessimistic play counter và seek-lock
- **Xác thực**: JWT phân quyền theo vai trò (`ADMIN` vs `STUDENT`) lưu độc lập:
  - `vsat_admin_access_token`
  - `vsat_student_access_token`

---

## 2. Phân hệ chức năng

### Phân hệ Quản trị (Admin & Collaborator)
- **Dashboard (`/`)**: Thống kê thời gian thực về đề thi, ca thi, câu hỏi và hoạt động thí sinh.
- **QuestionBank (`/questions`)**: Quản lý ngân hàng câu hỏi, hỗ trợ bộ lọc đa chiều và file nghe audio đính kèm.
- **ReviewQueue (`/reviews`)**: Hàng đợi duyệt câu hỏi từ cộng tác viên, xem trước công thức KaTeX và kiểm âm audio, phê duyệt/yêu cầu sửa/từ chối.
- **Blueprints (`/blueprints`)**: Quản lý khung ma trận đề thi 3 chiều V-SAT 2026, DB-backed CRUD (`WEB-3`), kiểm tra tỷ lệ phân bổ ma trận 7 môn.
- **ExamWizard (`/exams/wizard`)**: Trình soạn đề 4 bước (Thông tin chung -> Soạn cấu trúc/ma trận -> Xem trước câu hỏi -> Phân bản thể/Xuất bản).
- **ImportQuestions (`/questions/import`)**: Nạp câu hỏi hàng loạt đa định dạng (`.xlsx`, `.json`, `.docx`, `.txt`) với KaTeX preview và audio validation (`WEB-4`).
- **Schedules (`/schedules`)**: Quản lý ca thi và phòng thi tập trung (`exam_windows`), cấu hình chính sách mở ca và công bố điểm.
- **Users (`/users`)**: Quản lý tài khoản, phân quyền vai trò (`STUDENT`, `COLLABORATOR`, `CONTENT_ADMIN`, `SUPER_ADMIN`), khóa/mở khóa.
- **Tickets (`/tickets`)**: Tiếp nhận và xử lý báo lỗi nội dung câu hỏi từ thí sinh.

### Phân hệ Thí sinh Thi trên Máy tính (Student CBT)
- **StudentExamLobby**: Sảnh chọn đề thi tự do và các ca thi tập trung đang mở.
- **StudentCBTWorkspace**: Giao diện thi chuẩn hóa:
  - Split-pane layout cho bài đọc hiểu (`QuestionGroup`) và bài nghe tiếng Anh.
  - Hỗ trợ 4 định dạng câu hỏi V-SAT 2026 (MCQ, Đúng/Sai, Ghép hợp `MATCHING`, Điền số).
  - Web Worker timer, cơ chế tự động lưu bài (auto-save), phím tắt điều hướng nhanh.
  - Giới hạn số lượt nghe audio theo cấu hình đề thi.
- **StudentScorecard**: Bảng điểm chi tiết phân tầng, thống kê điểm thô/thang chuẩn hóa, xem lại đáp án và giải thích chi tiết, gửi ticket báo lỗi câu hỏi.
- **AdmissionPredictorWidget**: Dự đoán xác suất trúng tuyển theo tổ hợp điểm thi và điểm chuẩn các trường đại học.

---

## 3. Lệnh vận hành

```bash
# Cài đặt dependencies
npm install

# Khởi chạy môi trường phát triển (HMR)
npm run dev

# Kiểm tra cú pháp nhanh với Oxlint
npm run lint

# Biên dịch sản phẩm (TypeScript checking + Vite build)
npm run build

# Xem trước bản dựng production cục bộ
npm run preview
```
