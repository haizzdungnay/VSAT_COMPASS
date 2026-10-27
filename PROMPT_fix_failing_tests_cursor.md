# Prompt cho Cursor — Fix 3 Android unit test đang FAIL

> Copy toàn bộ phần dưới dán vào Cursor.

---

## NGỮ CẢNH

Repo **V-SAT Compass**, module Android `app/`. `./gradlew :app:assembleDebug` PASS nhưng `:app:testDebugUnitTest` có **3 test fail** (các test khác PASS):

- `app/src/test/java/com/example/v_sat_compass/data/repository/ExamRepositoryTest.java`
- `app/src/test/java/com/example/v_sat_compass/AdminExamViewModelTest.java`
- `app/src/test/java/com/example/v_sat_compass/AdminExamDetailViewModelTest.java`

**Đã điều tra trước:** Cả 3 file test **và** code chúng test (`ExamRepository.java`, `AdminExamViewModel.java`) **không bị đổi** trong các batch tính năng gần đây (Phase F). `AdminExamViewModel` lần cuối đổi ở commit `b7d30b0` — cùng lúc tạo test. Tức 3 lỗi này là **nợ test có sẵn / do runtime**, không phải regression mới. Build vẫn xanh nên **không phải lỗi compile** (test sources compile được, đa số test khác PASS).

**Môi trường:** máy thiếu `JAVA_HOME` global; verify bằng JBR: `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`.

## NGUYÊN NHÂN (đã phân tích — xác nhận lại khi chạy)

### ExamRepositoryTest — gần như chắc chắn
`ExamRepository.loadPublishedExams/loadExamDetail` trả kết quả qua `mainHandler.post(...)` (`Handler` gắn main `Looper`). Trong **plain JVM unit test** (không Robolectric, không instrumentation) main `Looper` không chạy → Runnable trong `post` không bao giờ thực thi → callback `onSuccess/onError` không được gọi → `CountDownLatch` không count down → `assertTrue(latch.await(2, SECONDS))` trả false → FAIL (timeout).

### AdminExamViewModelTest / AdminExamDetailViewModelTest — cần xác nhận bằng stack trace
Hai test này dùng `InstantTaskExecutorRule` + `FakeRepository` đồng bộ (không qua mạng/handler), code dưới test không đổi → **trước hết phải chạy và đọc lỗi thật**. Nghi vấn ưu tiên: xung đột phiên bản `androidx.lifecycle`/`androidx.arch.core` do dependency mới `androidx.work:work-runtime-ktx:2.9.1` (F8) kéo theo, làm LiveData/`observeForever` hành xử khác. Kiểm tra `./gradlew :app:dependencies` xem `androidx.lifecycle:*` có bị nâng/xung đột không.

## YÊU CẦU

1. **Chạy riêng từng test, chụp lại stack trace / assertion thật** trước khi sửa:
   ```
   set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
   gradlew.bat :app:testDebugUnitTest --tests "com.example.v_sat_compass.data.repository.ExamRepositoryTest"
   gradlew.bat :app:testDebugUnitTest --tests "com.example.v_sat_compass.AdminExamViewModelTest"
   gradlew.bat :app:testDebugUnitTest --tests "com.example.v_sat_compass.AdminExamDetailViewModelTest"
   ```
   Dán nguyên văn lỗi vào báo cáo.

2. **ExamRepositoryTest:** sửa **gốc**, không nới lỏng test. Hướng ưu tiên (chọn cái ít xâm lấn nhất, KHÔNG đổi hành vi production):
   - Cho phép `ExamRepository` dispatch kết quả qua một "result poster" có thể thay khi test: production vẫn post lên main thread; trong unit test chạy **inline/đồng bộ**. Ví dụ: tách cách post ra một `Executor`/callback injectable, hoặc fallback chạy inline khi `Looper.myLooper()`/main looper không khả dụng.
   - Hoặc chuyển 2 test này sang chạy bằng Robolectric (nếu repo đã có hạ tầng) để có main looper.
   - **Không** dùng `@Ignore`/xoá assertion để "qua" test.

3. **2 test admin:** dựa trên stack trace thật:
   - Nếu là xung đột dependency (lifecycle/arch-core): pin/resolutionStrategy cho `androidx.lifecycle` + `androidx.arch.core:core-testing` về phiên bản tương thích; xác nhận `:app:dependencies` hết xung đột.
   - Nếu là lỗi khác (assertion/NPE): sửa đúng nguyên nhân, **không** che bằng `@Ignore`. Chỉ sửa test khi test sai so với hợp đồng đúng của ViewModel; nếu ViewModel sai thì sửa ViewModel.

4. **Verify lại toàn bộ:** chạy `:app:testDebugUnitTest` (cả suite) → **0 failed**; `:app:assembleDebug` vẫn PASS; xác nhận không làm hỏng test nào đang xanh (gồm `AdaptiveDifficultyEngineTest`, `FlashcardSchedulerTest`, F3/F1 tests...).

## RÀNG BUỘC

- KHÔNG commit / tag / deploy. Chỉ sửa để test xanh + để onii-chan review.
- KHÔNG revert/commit file dirty cũ; nếu commit sau này: **không co-author**.
- KHÔNG đổi `ddl-auto`, không đụng backend trừ khi lỗi thực sự nằm ở đó (3 test này đều thuần Android).
- Không hạ chất lượng test để qua cho nhanh (`@Ignore`, xoá assert, `assumeTrue` vô điều kiện... đều KHÔNG chấp nhận).
- Nếu thêm/đổi dependency (vd pin lifecycle) → nêu rõ trong báo cáo.

## NGHIỆM THU
- Dán stack trace gốc của cả 3 test (trước khi fix).
- `:app:testDebugUnitTest` toàn suite: 0 failed; nêu tổng số test.
- `:app:assembleDebug` PASS.
- Liệt kê file đã sửa + giải thích nguyên nhân thật của từng test + cách fix; nêu mọi thay đổi dependency.
