# V-SAT Compass — Task Roadmap

Tài liệu này tổng hợp lộ trình hoàn thiện app dựa trên:
- `VSAT/ui/tong_hop_du_an_vsat_android_web_v2.txt`
- `VSAT/ui/VSAT_COMPASS_BAO_CAO_VA_HUONG_DAN.txt`
- các mockup trong `VSAT/ui/extracted_screens/`
- trạng thái mã nguồn hiện tại của Android app + backend Spring Boot

Mục tiêu cuối:
- App Android chạy ổn định trên nhiều thiết bị
- Trải nghiệm thi thử nghiêm túc, local-first
- Backend tối thiểu nhưng đủ an toàn để dùng với NeonDB
- Có đủ luồng học viên, quản trị nội dung, cộng tác viên, và vận hành cơ bản

---

## 1. Nguyên tắc triển khai

- Ưu tiên hoàn thiện `Student MVP` trước mọi module khác.
- Giữ kiến trúc `client-first`: làm bài, timer, điều hướng, chấm điểm chạy trên thiết bị.
- Backend chỉ giữ phần bắt buộc: auth, profile, metadata đề online, đồng bộ kết quả cuối.
- Không mở rộng web/admin nâng cao trước khi Android student flow ổn định.
- Mỗi task chỉ được coi là xong khi có tiêu chí nghiệm thu rõ ràng.

---

## 2. Trạng thái hiện tại

### 2.1 Đã có

- [x] Android app nền tảng: Splash, Login, Register, Home, Exam List, Exam Detail, Exam Session, Exam Result, Profile
- [x] Admin mode cơ bản trong cùng app
- [x] Local-first exam flow cho chế độ `CLIENT_SIDE_EXAM_PROCESSING=true`
- [x] Multi-pack local exams từ `sample_*.json`
- [x] Đồng bộ kết quả cuối theo kiểu non-blocking nếu backend sẵn sàng
- [x] Auth backend cơ bản bằng Spring Boot + JWT
- [x] Neon PostgreSQL schema và tài liệu nền tảng
- [x] Mockup student + admin/collaborator từ Stitch

### 2.2 Chưa hoàn chỉnh

- [ ] Backend public production thực sự ổn định
- [ ] Student history / analytics thật sự dùng được end-to-end
- [ ] Review lời giải chi tiết sau bài thi
- [ ] Quản trị câu hỏi / duyệt nội dung / tạo đề hoàn chỉnh
- [ ] Cộng tác viên nhập và chỉnh sửa câu hỏi hoàn chỉnh
- [ ] Ticket lỗi / phản hồi nội dung
- [ ] User management / role management đủ dùng
- [ ] Test plan, regression checklist, release checklist

---

## 3. Roadmap theo giai đoạn

## Giai đoạn A — Khóa Student MVP

Mục tiêu: học viên có thể đăng nhập, vào kho đề, làm bài, nộp bài, xem kết quả, luyện tập tiếp mà không phụ thuộc backend exam engine đầy đủ.

### A1. Ổn định xác thực và hồ sơ
- [ ] Xác nhận `login`, `register`, `refresh`, `logout`, `getMe` hoạt động với backend public/dev
- [ ] Làm rõ thông báo lỗi mạng, token hết hạn, lỗi tài khoản
- [ ] Tự động xử lý refresh token khi access token hết hạn
- [ ] Kiểm tra đăng xuất sạch token + user cache

Tiêu chí xong:
- Đăng nhập thành công trên thiết bị thật
- Mất mạng hoặc token lỗi có thông báo rõ ràng
- Không bị vòng lặp login/logout bất thường

### A2. Hoàn thiện kho đề local-first
- [ ] Rà lại danh sách đề hiển thị từ nhiều file `sample_*.json`
- [ ] Chuẩn hóa metadata đề: môn, số câu, thời gian, mã đề, mô tả
- [ ] Thêm ít nhất 1 pack nữa cho môn Toán hoặc Tiếng Anh để test quy mô nhiều đề cùng môn
- [ ] Đảm bảo filter và search hoạt động đúng trên dữ liệu local

Tiêu chí xong:
- Có ít nhất 4-5 đề local hiển thị đúng
- Search/filter không bị lỗi dữ liệu trùng

### A3. Hoàn thiện màn thi thử
- [ ] Kiểm tra tất cả trạng thái UI từ mockup: điều hướng câu, bookmark, grid câu hỏi, hết giờ, xác nhận nộp bài
- [ ] Thêm autosave state trong RAM hoặc local storage khi app bị pause/rotate
- [ ] Xử lý resume exam nếu app bị đóng giữa chừng
- [ ] Hiển thị rõ trạng thái `Sync: local-only` / `Sync: online enabled`

Tham chiếu mockup:
- `v_sat_compass_giao_di_n_thi_1`
- `v_sat_compass_giao_di_n_thi_2`
- `v_sat_compass_exam_control`
- `v_sat_compass_danh_s_ch_c_u_h_i`
- `v_sat_compass_x_c_nh_n_n_p_b_i`

Tiêu chí xong:
- Làm bài 50 câu không crash
- Rotate/background app không mất state
- Nộp bài luôn tạo được kết quả local

### A4. Hoàn thiện màn kết quả và review
- [ ] Làm xong luồng `Xem lời giải chi tiết`
- [ ] Hiển thị câu đúng/sai, đáp án đã chọn, giải thích
- [ ] Tính điểm theo đúng thang V-SAT dự kiến
- [ ] Tạo gợi ý chủ đề yếu từ dữ liệu local

Tham chiếu mockup:
- `v_sat_compass_k_t_qu_thi`
- `v_sat_compass_xem_l_i_gi_i_chi_ti_t`
- `v_sat_compass_gi_i_th_ch_b_ng_ai`
- `v_sat_compass_luy_n_t_p_theo_ch`

Tiêu chí xong:
- Sau nộp bài có thể xem summary và review từng câu
- Có danh sách chủ đề cần luyện tập tiếp

### A5. History và stats cơ bản
- [ ] Lưu lịch sử bài làm local
- [ ] Hiển thị số lần thi, điểm gần nhất, điểm cao nhất theo đề
- [ ] Tạo dashboard học viên dựa trên local history
- [ ] Nếu có backend online, đồng bộ kết quả cuối và lịch sử

Tham chiếu mockup:
- `v_sat_compass_trang_ch`
- `v_sat_compass_thi_c_hi_u`
- `v_sat_compass_b_ng_x_p_h_ng` (nếu dùng sau)

Tiêu chí xong:
- Người dùng làm 2-3 đề liên tiếp vẫn xem được lịch sử và thống kê

---

## Giai đoạn B — Backend tối thiểu production-ready

Mục tiêu: backend đủ nhỏ để chi phí thấp nhưng đủ an toàn và vận hành được với NeonDB.

### B1. Public backend ổn định
- [ ] Xác định domain public thật sự đang chạy
- [ ] Sửa `BASE_URL_CLOUD` sang domain đúng
- [ ] Kiểm tra health endpoint, auth endpoint, CORS, SSL
- [ ] Tài liệu hóa cách deploy và rollback

Tiêu chí xong:
- App Android gọi được backend public thật
- Không còn 404/no-server ở domain cấu hình

### B2. API tối thiểu bắt buộc
- [ ] Hoàn thiện và test production cho:
  - [ ] `POST /auth/login`
  - [ ] `POST /auth/register`
  - [ ] `POST /auth/refresh`
  - [ ] `POST /auth/logout`
  - [ ] `GET /auth/me`
- [ ] Hoàn thiện endpoint đồng bộ kết quả cuối:
  - [ ] `POST /sessions/start` (bootstrap session nhẹ)
  - [ ] `POST /sessions/{sessionId}/client-submit`
- [ ] Nếu cần đề online:
  - [ ] `GET /exams`
  - [ ] `GET /exams/{id}`

Tiêu chí xong:
- Student flow vẫn chạy khi backend có hoặc không có
- Khi backend có, auth và sync kết quả hoạt động đúng

### B3. Bảo mật và ổn định
- [ ] Kiểm tra không để app truy cập trực tiếp NeonDB
- [ ] Chuẩn hóa validation request/response
- [ ] Chuẩn hóa error JSON cho Android parse ổn định
- [ ] Bật log vừa đủ cho prod và dev
- [ ] Kiểm tra secret/env/deploy config không lộ ra repo

Tiêu chí xong:
- Có checklist bảo mật backend tối thiểu
- Có thể redeploy mà không sửa code Android

---

## Giai đoạn C — Quản trị nội dung MVP

Mục tiêu: nội dung đề/câu hỏi có thể được quản lý thật sự, không chỉ dừng ở mockup.

### C1. Question bank
- [ ] Danh sách câu hỏi
- [ ] Xem chi tiết câu hỏi
- [ ] Tạo mới câu hỏi
- [ ] Chỉnh sửa câu hỏi
- [ ] Gắn môn / chủ đề / mức độ / đáp án / lời giải
- [ ] Filter nâng cao

Tham chiếu mockup:
- `qu_n_tr_danh_s_ch_c_u_h_i`
- `qu_n_tr_xem_tr_c_c_u_h_i`
- `qu_n_tr_chi_ti_t_c_u_h_i`
- `qu_n_tr_b_l_c_n_ng_cao`
- `qu_n_tr_bi_n_so_n_c_u_h_i`
- `v_sat_qu_n_tr_ng_n_h_ng_c_u_h_i`

Tiêu chí xong:
- Admin/collaborator tạo và sửa câu hỏi được end-to-end

### C2. Review workflow
- [ ] Gửi duyệt câu hỏi
- [ ] Duyệt / từ chối / yêu cầu chỉnh sửa
- [ ] Xem lịch sử phiên bản nội dung
- [ ] Ghi comment phản hồi

Tham chiếu mockup:
- `qu_n_tr_ph_duy_t_n_i_dung`
- `v_sat_qu_n_tr_duy_t_c_u_h_i`
- `qu_n_tr_l_ch_s_phi_n_b_n`
- `qu_n_tr_y_u_c_u_ch_nh_s_a`

Tiêu chí xong:
- Có workflow ít nhất: draft -> pending -> approved/rejected

### C3. Exam management
- [ ] Tạo đề thi từ question bank
- [ ] Gán câu hỏi vào đề
- [ ] Thiết lập thời gian, số câu, trạng thái publish
- [ ] Chọn đề miễn phí / trả phí / premium metadata

Tham chiếu mockup:
- `v_sat_qu_n_tr_t_o_thi`
- `v_sat_compass_kho_thi`
- `v_sat_compass_exam_minimal`
- `v_sat_compass_exam_premium`

Tiêu chí xong:
- Admin tạo được đề mới và student thấy đề khi publish

---

## Giai đoạn D — Ticket, user management, vận hành

Mục tiêu: có đủ công cụ để app vận hành như một sản phẩm thật ở quy mô nhỏ.

### D1. Ticket / báo lỗi
- [ ] Học viên gửi báo lỗi câu hỏi / nội dung
- [ ] Admin xem danh sách ticket
- [ ] Gán xử lý, cập nhật trạng thái, phản hồi

Tham chiếu mockup:
- `v_sat_qu_n_tr_ticket_l_i_n_i_dung`
- `v_sat_qu_n_tr_y_u_c_u_s_a_l_i`

Tiêu chí xong:
- Có vòng khép kín student report -> admin xử lý -> đóng ticket

### D2. User & role management
- [ ] Danh sách user
- [ ] Xem role/status
- [ ] Khóa/mở user
- [ ] Đổi role
- [ ] Chuyển mode student/admin trong app đúng quyền

Tham chiếu mockup:
- `v_sat_qu_n_tr_ng_i_d_ng_quy_n`
- `v_sat_compass_t_i_kho_n_admin_1`
- `v_sat_compass_t_i_kho_n_admin_2`
- `v_sat_compass_x_c_nh_n_chuy_n_ch`

Tiêu chí xong:
- Role thay đổi có hiệu lực đúng ở UI và backend

### D3. Dashboard & audit cơ bản
- [ ] Dashboard tổng quan admin
- [ ] Nhật ký hoạt động cơ bản
- [ ] Số lượng user, đề thi, câu hỏi, ticket

Tham chiếu mockup:
- `v_sat_qu_n_tr_t_ng_quan_1`
- `v_sat_qu_n_tr_t_ng_quan_2`
- `v_sat_qu_n_tr_nh_t_k_h_th_ng`

Tiêu chí xong:
- Admin có màn tổng quan đủ để quản lý hệ thống

---

## Giai đoạn E — Hoàn thiện chất lượng sản phẩm

Mục tiêu: từ bản chạy được sang bản có thể demo, bàn giao, hoặc phát hành thử.

### E1. Chất lượng dữ liệu
- [ ] Chuẩn hóa ngân hàng câu hỏi Toán và Tiếng Anh
- [ ] Đảm bảo mỗi môn có đủ dữ liệu demo hợp lý
- [ ] Soát lỗi chính tả, lời giải, đáp án
- [ ] Tạo file import chuẩn nếu dùng Excel

### E2. Test & QA
- [ ] Viết checklist smoke test Android
- [ ] Test login / offline / online / hết giờ / submit / rotate / background
- [ ] Test role student / collaborator / content admin / super admin
- [ ] Test migration dữ liệu local packs

### E3. Hiệu năng & UX
- [ ] Kiểm tra thời gian mở danh sách đề
- [ ] Kiểm tra bộ nhớ khi làm bài dài
- [ ] Tối ưu text overflow, spacing, dark edge cases
- [ ] Tinh chỉnh thông báo lỗi và trạng thái loading/empty/error

### E4. Release readiness
- [ ] Checklist cấu hình release build
- [ ] Chuẩn hóa icon, splash, app name, versioning
- [ ] Kiểm tra proguard/r8 tối thiểu
- [ ] Viết hướng dẫn vận hành ngắn cho dev/admin

---

## 4. Danh sách ưu tiên thực hiện ngay

### Ưu tiên P0
- [ ] Làm cho backend public chạy thật sự ổn định
- [ ] Hoàn thiện `Xem lời giải chi tiết`
- [ ] Lưu lịch sử bài làm local
- [ ] Bổ sung thêm pack đề local cho Toán / Tiếng Anh
- [ ] Hoàn thiện bootstrap + sync kết quả cuối

### Ưu tiên P1
- [ ] Question bank quản trị
- [ ] Review workflow câu hỏi
- [ ] Tạo đề thi quản trị
- [ ] Ticket nội dung
- [ ] User / role management

### Ưu tiên P2
- [ ] Dashboard admin nâng cao
- [ ] Commerce / đề premium / credit
- [ ] Web dashboard / web exam parity
- [ ] Leaderboard / social features

---

## 5. Định nghĩa “app hoàn chỉnh” cho giai đoạn hiện tại

App được coi là đủ hoàn chỉnh cho bản bàn giao/MVP nghiêm túc khi thỏa đồng thời:

- [ ] Học viên đăng nhập, chọn đề, làm bài, nộp bài, xem kết quả, xem lời giải, xem lịch sử
- [ ] App chạy ổn khi backend exam engine chưa đầy đủ
- [ ] Auth và sync kết quả cuối hoạt động với backend public
- [ ] Có ít nhất 2 môn demo tốt: Toán, Tiếng Anh
- [ ] Admin quản lý được câu hỏi và đề ở mức MVP
- [ ] Có phân quyền rõ giữa Student / Collaborator / Content Admin / Super Admin
- [ ] Có checklist test và changelog để quản lý thay đổi

---

## 6. Cách dùng file này

Mỗi khi hoàn thành một task:
- tick `[x]`
- thêm ngày hoàn thành nếu cần
- nếu thay đổi lớn, cập nhật thêm vào `CHANGELOG.md`
- nếu phát sinh scope mới, chỉ thêm vào đúng giai đoạn tương ứng, không ghi rải rác
