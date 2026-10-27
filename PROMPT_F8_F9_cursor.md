# Prompt cho Cursor — F8 (Nhắc học theo lịch) + F9 (Leaderboard)

> Copy toàn bộ phần dưới dán vào Cursor.

---

## NGỮ CẢNH DỰ ÁN

**V-SAT Compass** — app luyện thi V-SAT, 1 repo:

- **Android (Java):** `app/src/main/java/com/example/v_sat_compass/`
  - `data/repository/GamificationRepository.java` (F3: streak/XP/level — vừa làm)
  - `ui/home/HomeFragment.java`, `ui/profile/ProfileFragment.java`, `ui/settings` (nếu có; nếu chưa thì tạo màn cài đặt thông báo)
  - `data/api/` Retrofit + `ApiResponse<T>`; `util/NetworkUtils.java`
- **Backend (Spring Boot):** `VSAT/vsat-compass-api/...`
  - `controller/student/MyStatsController.java`, user/role; `config/SecurityConfig.java` (rule cụ thể trước rule chung); `AppException`/`ApiResponse`

**Bối cảnh:** Neon treo → demo Postgres local Docker. `ddl-auto: validate`, không Flyway → thêm bảng phải kèm DDL tường minh (`docs/sql/`, mẫu `0001_create_tickets.sql`) + áp local. Làm + verify LOCAL trước.

## RÀNG BUỘC CHUNG
- KHÔNG commit / tag / deploy / prod smoke. Chỉ code + review.
- KHÔNG revert/commit file dirty cũ; nếu commit sau này: **không co-author**.
- Giữ local-first; auth theo role; rule SecurityConfig cụ thể đặt trước rule chung.
- Thêm bảng DB → kèm DDL tường minh + áp local; KHÔNG đổi `ddl-auto`.

---

## F8 — Nhắc học theo lịch (P2) — LOCAL NOTIFICATION, KHÔNG FCM

**Quyết định (đã chốt):** Giai đoạn này **chỉ làm nhắc-học bằng local notification** (WorkManager/AlarmManager), **KHÔNG tích hợp FCM/Firebase** (push "đề mới" từ server defer tới khi lên online thật). Lý do: local reminder không cần server/Firebase nên làm + demo được ngay.

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F8.1 | Lên lịch nhắc học hằng ngày bằng WorkManager (hoặc AlarmManager) ở giờ người dùng chọn | nổ notification đúng giờ kể cả app đóng |
| F8.2 | Màn cài đặt thông báo: bật/tắt nhắc học + chọn giờ + (tùy chọn) nhắc giữ streak | trạng thái lưu local |
| F8.3 | Xin quyền `POST_NOTIFICATIONS` (Android 13+) đúng cách | từ chối quyền → app không crash, báo nhẹ |
| F8.4 | Nội dung nhắc gắn với streak/daily challenge (F3) nếu có | vd "Giữ streak X ngày — luyện 10 câu hôm nay" |

**Nghiệm thu F8 (LOCAL):** bật nhắc học giờ gần hiện tại → nhận notification dù app đóng; tắt → không nhận; từ chối quyền → không crash; `./gradlew :app:assembleDebug` PASS.

> Ghi chú vào `PLAN_offline_phase_f.md`: phần push "đề mới" qua FCM được defer; F8 hiện chỉ là local reminder.

---

## F9 — Leaderboard (P2, tùy chọn)

**Mục tiêu:** Bảng xếp hạng theo XP/điểm để tạo động lực. **Quyết định (đã chốt): opt-in + tên hiển thị** — chỉ user **tự bật tham gia** mới xuất hiện trên bảng; ai không bật thì ẩn hoàn toàn.

### F9-A — Backend
| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F9.1 | Cờ opt-in trên user (`leaderboard_opt_in` boolean) + `PATCH /me/leaderboard` để bật/tắt | mặc định tắt (ẩn). **Thêm cột → kèm DDL tường minh `docs/sql/` + áp local** |
| F9.2 | `GET /leaderboard` (role STUDENT): top N theo điểm/XP, **chỉ gồm user opt-in**; trả tên hiển thị + thứ hạng | user chưa opt-in không xuất hiện |
| F9.3 | Nguồn điểm: tận dụng `exam_sessions`/score hoặc tổng XP (nêu rõ chọn nguồn nào) | Mockito test: chỉ opt-in, sắp xếp đúng |

> XP của F3 hiện lưu **local trên máy**. Nếu xếp hạng theo XP cần đẩy XP lên server (thêm cột/endpoint) — hoặc đơn giản hơn: **xếp hạng theo điểm bài thi đã có trên backend** (`exam_sessions`). Ưu tiên phương án dùng dữ liệu backend sẵn có để khỏi phát sinh sync phức tạp; nêu rõ lựa chọn trong báo cáo.

### F9-B — Android
| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F9.4 | Màn Leaderboard: danh sách top + thứ hạng của mình; toggle "Tham gia bảng xếp hạng" | tắt opt-in → mình ẩn khỏi bảng |
| F9.5 | Vào từ Home/Profile | empty state khi chưa ai opt-in |

**Nghiệm thu F9 (LOCAL):** 2 user bật opt-in → cùng lên bảng theo đúng thứ hạng; 1 user tắt → biến mất khỏi bảng; student gọi được, role khác theo convention; backend test PASS; DDL cột opt-in đã áp local + backend boot OK; `./gradlew :app:assembleDebug` PASS.

---

## BÁO CÁO KHI XONG
Liệt kê file thêm/sửa (tách F8 / F9), dependency mới (WorkManager...), DDL mới (cột opt-in...), nguồn điểm leaderboard đã chọn, cách verify, và chỗ điều chỉnh ngoài phạm vi.
