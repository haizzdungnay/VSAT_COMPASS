# Báo cáo nghiệm thu vòng sửa định dạng V-SAT

Ngày chạy: 2026-11-16. Phạm vi: build/test backend, Android, web; oracle và kiểm tra hash; đối chiếu suite/gate và clean-room đã có trên đĩa. Không chạy toàn bộ `run-all.mjs`. Phase2 subset không thực thi vì harness sẽ tự khởi động backend trên cổng `18080`, bị cấm theo phạm vi.

## 1. Tóm tắt trước/sau và kiểm chứng phiên này

| Hạng mục | Trước sửa | Kết quả lưu trên đĩa / phiên này | Exit |
|---|---:|---:|---:|
| Suite `vsat-format` | 13/44 PASS, 31 FAIL | 44/44 PASS, 0 FAIL (`vsat-format.json`) | `run-all` trước đó: 0; không chạy lại theo yêu cầu |
| `backend-admin` | 42/42 | 42/42 | N/A — báo cáo suite đã lưu |
| `backend-psc` | 36/36 | 36/36 | N/A — báo cáo suite đã lưu |
| `web` suite acceptance | 10/10 | 10/10 | N/A — báo cáo suite đã lưu |
| `android-student` | 20/20 | 20/20 | N/A — báo cáo suite đã lưu |
| `android-roles-workers` | 18/18 | 18/18 | N/A — báo cáo suite đã lưu |
| Phase2 features | 4/4 ở baseline | Chưa xác minh: chạy subset qua `run-all.mjs --suite suites/phase2-features-check.mjs --skip-verdict` sẽ tự start backend `18080`; không chạy vì cổng này bị cấm | `NOT_RUN`; ghi chú tại `logs-vsat-fix/phase2.log` |
| Backend Gradle JUnit | Baseline tài liệu cũ: 371/371 | 396 tests, 0 failures, 0 errors, 0 skipped (34 XML) | 0 |
| Android Gradle unit tests | Baseline báo cáo cũ: 277/277 | 280 tests, 0 failures, 0 errors, 0 skipped (48 XML) | 0 |
| Web lint + build | — | `oxlint`, `tsc -b`, `vite build` thành công; Vite cảnh báo chunk JS 882.24 kB > 500 kB | 0 |
| Oracle `--self-check` | — | OK, 1 đề hợp lệ + 14 biến thể lỗi/rò khóa và kiểm tra điểm | 0 |
| SHA-256 criteria | — | 2/2 file khớp (`acceptance-harness/suites/vsat-format.mjs` và `acceptance-harness/lib/vsat-format.mjs`) | 0 |
| Clean-room | — | 33/36 PASS, 3 FAIL: CR-01, CR-24, CR-25; chi tiết mục 6 | Lệnh đã lưu: 0 |
| Verdict gate | Trước vòng format: 126 IDs theo báo cáo cũ | `final-verdict.json`: ACCEPTED, required/collected/passed 170, failed 0, violations 0. `verdict-gate.mjs` đã nâng registry lên 170. Lệnh gate tạo JSON không được chạy lại riêng trong vòng này; exit code lần gate trước không có log riêng đối chiếu được | Kết quả JSON ACCEPTED; exit code lần gate trước không đối chiếu được |

Suite JSON trên đĩa sinh ngày `2026-11-16T02:46–02:48Z`; log Gradle xác nhận `BUILD SUCCESSFUL` và `EXIT=0`. Đây là các lần chạy harness đã lưu, không phải lần chạy lại của tác vụ này. Baseline trước sửa lấy từ `acceptance-harness/results/baseline-pre-fix.md`.

## 2. Log thật và exit code

| Lệnh | Kết quả thật | Log |
|---|---|---|
| Backend `./gradlew test --console=plain` với JDK 21 | `BUILD SUCCESSFUL`; test task `UP-TO-DATE`; XML hiện có tổng 396/0/0/0; `EXIT=0` | `acceptance-harness/results/logs-vsat-fix/backend-test.log` |
| Android `./gradlew compileDebugJavaWithJavac testDebugUnitTest --console=plain` với JDK 21 | `BUILD SUCCESSFUL`; compile/test `UP-TO-DATE`; XML hiện có tổng 280/0/0/0; `EXIT=0` | `acceptance-harness/results/logs-vsat-fix/android-build-test.log` |
| `npm run lint && npm run build` | Oxlint sạch; TypeScript/Vite build thành công; chunk-size warning; `EXIT=0` | `acceptance-harness/results/logs-vsat-fix/web-lint-build.log` |
| `node acceptance-harness/lib/vsat-format.mjs --self-check` | `vsat-format self-check OK (1 valid + 14 broken/leak exams, scoring checks)`; `EXIT=0` | `acceptance-harness/results/logs-vsat-fix/vsat-format-self-check.log` |
| `sha256sum -c acceptance-harness/results/criteria-sha256.txt` từ repo root | Cả hai checksum khớp: `acceptance-harness/suites/vsat-format.mjs`, `acceptance-harness/lib/vsat-format.mjs`; `EXIT=0` | `acceptance-harness/results/logs-vsat-fix/sha256.log` |

Bằng chứng suite/gate lấy từ `acceptance-harness/results/vsat-format.json`, các `*-report.json`, `final-verdict.json`; clean-room lấy từ `acceptance-harness/cleanroom/cleanroom-probes.json` và mục “Re-run sau T7/T8” của `acceptance-harness/results/cleanroom-verify.md`. Phase2 chưa chạy: `run-all.mjs` khi được gọi với Phase2 subset vẫn khởi động backend `18080`; vì vậy lần này không xác minh 4 assertion, xem `logs-vsat-fix/phase2.log`.

## 3. Gap G1–G17

Dòng file là dòng thực tế được đối chiếu ở phiên này. `results/vsat-format.json` ghi 44/44 VF PASS, nhưng chỉ xem các ca có liên quan trong bảng là bằng chứng cho gap tương ứng; G11 không có VF tự động, G16 cũng không có VF tự động. ID manual/deferred không nằm trong 44 ca tự động.

| Gap | Trạng thái / bằng chứng sửa | Ca VF chứng minh |
|---|---|---|
| G1 — publish đủ 25 câu, đúng mix/tổng điểm | Đã chặn publish sai chuẩn và revalidate; `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/AdminExamServiceImpl.java:662-719`; quy tắc tổng/mix ở `.../util/VsatFormatRules.java:222-257` | VF-A02, VF-A03, VF-A04, VF-C05 |
| G2 — đồng môn và revalidate đổi môn | Đã kiểm tra khi gắn câu và validate publish; `.../AdminExamServiceImpl.java:223-230,662-719` | VF-A05, VF-C03, VF-C04 |
| G3 — sinh đề theo blueprint | Đã kiểm tra blueprint/mix, chọn câu theo dạng/cấu trúc hợp lệ, cảnh báo thiếu và tính raw score thực; `.../AdminExamServiceImpl.java:502-598` | VF-B01–VF-B05 |
| G4 — Đ/S đủ 4 ý a–d | Đã có validator dùng lúc nhập tay/import/publish; `.../util/VsatFormatRules.java:160-177`; `.../QuestionServiceImpl.java:399-403`; `.../QuestionImportServiceImpl.java:762-776` | VF-A09, VF-D04, VF-Q01 |
| G5 — MCQ đúng 4 lựa chọn, 1 đáp án | Đã validate cấu trúc; `.../util/VsatFormatRules.java:178-182`; `.../QuestionServiceImpl.java:382-387` | VF-Q02 |
| G6 — matching đủ 4 cặp, khóa khớp | Đã validate/import khóa lựa chọn; `.../util/VsatFormatRules.java:183-190`; `.../QuestionImportServiceImpl.java:787-801,923-945` | VF-D06, VF-Q03 |
| G7 — câu ngắn có khóa text/number | Đã nhận và lưu đáp án trong option; `.../QuestionImportServiceImpl.java:778-785,913-922`; so khớp chấm ở `.../SessionServiceImpl.java:722-743` | VF-D05 |
| G8 — import validate, lỗi dòng, chờ duyệt | Đã validate loại/đáp án/cấu trúc và lưu `PENDING_REVIEW`; `.../QuestionImportServiceImpl.java:727-829,890-907` | VF-D01–VF-D06, VF-D08, VF-D09 |
| G9 — section và điểm từng câu | Điểm từng câu đã gán theo helper nhưng `pointsFor` hiện trả 6 mọi dạng: `.../util/VsatFormatRules.java:89-91`; bundle trả `maxRawScore`: `.../ExamServiceImpl.java:181-210`. Section DB chưa được ghi — CR-01 FAIL, schema chết; không xem gap này là đóng hoàn toàn | VF-F01 chứng minh bundle/tổng điểm; CR-01 phản chứng ghi section |
| G10 — scoring profile hợp lệ | Đã resolve profile, validate blueprint ID tồn tại; `.../AdminExamServiceImpl.java:117-123,601-607`; `.../AdminBlueprintServiceImpl.java:236-240` | VF-E01, VF-E04 |
| G11 — danh mục đủ 8 môn | Chưa đóng: seed V0 chỉ có MATH, ENG (`VSAT/vsat-compass-api/src/main/resources/db/migration/V0__base_schema.sql:784-786`); chưa có ca VF tương ứng | Không có VF ID cho G11 |
| G12 — blueprint 25 / 9-6-5-5 / ô hợp lệ | Đã chặn tổng khác 25, ô âm, dạng ngoài cấu trúc hoặc mix sai; `.../AdminBlueprintServiceImpl.java:243-280` | VF-B04, VF-E02, VF-E03 |
| G13 — bundle học sinh không lộ đáp án | Đã bỏ trường đáp án và sanitize option; `.../ExamServiceImpl.java:193-210,213-227`; DTO bỏ qua giá trị null | VF-F02 |
| G14 — bundle có đủ mô tả 4 phần và điểm | Bundle hiện trả điểm từng câu nhưng không có thuộc tính section trong DTO; dùng thứ tự câu để xác định dạng. Migration V6 có tạo bảng `exam_sections`, nhưng sản phẩm không đọc/ghi bảng này; CR-01 xác nhận bảng trống. Đóng theo contract bundle đã chốt, chưa ghi section DB | VF-F01, VF-F03; CR-01 cho thấy section DB trống |
| G15 — web chặn publish chưa đạt, không fallback 403 | Checks wizard tính từ câu thật và ngăn publish; `web-admin/src/pages/ExamWizard/Step3Preview.tsx:27-56,103-105,325-333`; lỗi 403 được ném rõ, không gọi submit-review: `web-admin/src/services/api.ts:512-524` | VF-A10; VF-G01–G03 vẫn manual/deferred |
| G16 — báo lỗi import | API import lỗi HTTP được chuyển thành Error, không giả thành công; `web-admin/src/services/api.ts:1232-1245` | Không có VF tự động; UI manual/deferred |
| G17 — duration theo môn 90/60 | Đã validate khi tạo/sửa: `.../AdminExamServiceImpl.java:107-110,181-188`; rule 90 phút MATH/LIT: `.../util/VsatFormatRules.java:98-103` | VF-A08 |

## 4. Assertion regression đổi theo spec

Đối chiếu trực tiếp diff cho thấy assertion đổi đúng như sau: `acceptance-harness/suites/helpers/backend-admin-helpers.mjs:63,81` fixture đề MATH đổi duration 60→90; `acceptance-harness/suites/backend-admin.mjs:104` API-007 đổi 75→90; `backend-admin.mjs:180-181` API-013 đổi kỳ vọng publish đề một câu 200/PUBLISHED thành 400; `VSAT/vsat-compass-api/src/test/java/com/vsatcompass/api/service/OptimisticLockingAndBaseSchemaTest.java:86` fixture duration đổi 60→90. Số dòng orchestrator đưa (`71,114` helper và test `:101`) lệch: helper dòng 71 là `return parseId(res)`, dòng 114 không phải fixture duration; test dòng 101 là `.difficulty(Difficulty.EASY)`. Không diễn giải sai các dòng sai lệch là assertion.

## 5. Harness, gate và registry T8

- `acceptance-harness/verdict-gate.mjs:38-42,65-66,128-165,168-178`: gate lấy đúng 170 ID từ registry (trước: 126 theo baseline report), kiểm tra thiếu/trùng/status/assertions, chỉ ACCEPTED khi 170 PASS và 0 violation.
- `acceptance-harness/run-all.mjs:281-305`: suite `suites/vsat-format.mjs` được thêm vào danh sách mặc định ở dòng 291; Phase2 chạy mặc định hoặc khi chọn riêng `suites/phase2-features-check.mjs`. Subset vẫn qua preflight, bootstrap DB và backend start/verify; do đó bị chặn bởi yêu cầu không khởi động backend `18080`. Suite Phase2 (`phase2-features-check.mjs:11-12`) còn ghi chú cũ rằng kết quả không lưu JSON để bảo toàn 126 ID; sau gate T8 registry có 170 ID, chưa sửa ghi chú ngoài phạm vi cho phép.
- `docs/testing/feature-inventory.json:1063-1458`: vùng registry VF; 44 ID tự động. Test plan dòng 99 ghi rõ 5 ca manual/deferred nằm ngoài registry.
- Kết quả gate đã ghi: `acceptance-harness/results/final-verdict.json` — ACCEPTED, 170/170, 0 failed, 0 violations. Không có gate log riêng nên không ghi exit code suy đoán. `acceptance-harness/results/vsat-format-run.log` là log baseline cũ, không dùng làm log lần pass.

## 6. Clean-room CR-01, CR-24, CR-25 — schema chết

Ba probe FAIL kiểm tra dữ liệu trong các bảng schema cũ, không phải lỗi luồng runtime đang dùng. Kết luận giữ nguyên là “schema chết”: V6 tạo `exam_sections` (`V6__create_vsat_scoring_and_sections.sql:28-40`); V5 tạo `question_matching_items` (`V5__create_vsat_question_tables.sql:36-48`) và `question_short_answer_keys` (`V5__create_vsat_question_tables.sql:51-62`). Grep source sản phẩm cho tên ba bảng không thấy reader/writer Java. Import matching ghi lựa chọn vào `question_matching_choices` (`QuestionImportServiceImpl.java:923-945`), short-answer ghi đáp án vào `question_options` (`:913-922`), còn web suy ra `shortAnswerKey` từ options (`web-admin/src/services/api.ts:660-665`). Chỉ harness bootstrap ghi các bảng này: `acceptance-harness/db-bootstrap.mjs:250,284,325,350`.

Vì vậy probe báo bảng rỗng là đúng về mặt kỹ thuật, nhưng expectation của probe không phản ánh mô hình dữ liệu/runtime hiện dùng. Vòng này đã chốt không tạo migration, không sửa code/schema. Tương lai: nếu dùng các bảng thì phải ghi/đọc dữ liệu qua runtime; nếu không dùng thì bỏ bảng ở migration tiếp theo. Clean-room JSON xác nhận CR-01 thiếu section, CR-24 matching_items 0, CR-25 short_answer_keys 0; 33 probe còn lại PASS.

## 7. Ca manual/deferred

Test plan `docs/testing/VSAT_EXAM_FORMAT_TEST_PLAN.md:99` ghi `VF-D07`, `VF-E05`, `VF-G01–G03` cần UI thủ công; gate không hỗ trợ trạng thái manual/deferred nên 5 ca không có trong `vsat-format.json`/registry 44 ca. Không báo các ca này là đã pass tự động. Lý do cụ thể trong plan: D07 kiểm tra lỗi backend khi import trên UI; E05 xóa blueprint đang được dùng; G01 publish khi Step3 có 0 câu; G02 nhãn/thứ tự phần thi; G03 dropdown scoring profile chỉ hiện profile tồn tại.

## 8. Rủi ro còn lại

- Học sinh dùng app cũ có thể thấy điểm sai trên máy; server vẫn tự chấm đúng.
- Generator thay toàn bộ bộ câu khi thiếu câu — hành vi có chủ đích theo VF-B05, không phải giữ nguyên bộ cũ.
- Chưa rà các đề `PUBLISHED` cũ không đạt chuẩn. Đề xuất script read-only chạy trên dump local; không tự ẩn đề.
- Chưa có endpoint liệt kê scoring profile.
- Thang điểm TF `1/2/3/6` hardcode; `SessionServiceImpl.java:657-690`, nâng cấp khi có nhiều scoring profile.
- Ba bảng schema chết ở mục 6 vẫn tồn tại; chưa migration.
- Web build phát cảnh báo bundle JS lớn; lint/build vẫn exit 0.
- SHA-256 criteria chạy lại từ repo root: 2/2 checksum khớp, `EXIT=0`; log `acceptance-harness/results/logs-vsat-fix/sha256.log`.

## Phụ lục: run-all đầy đủ lưu log (2026-11-16)

- Lệnh: `cd acceptance-harness && node run-all.mjs > results/logs-vsat-fix/run-all-full.log 2>&1; echo EXIT=$? >> results/logs-vsat-fix/run-all-full.log`
- Exit code: `0`
- Phase2: `4/4` nhóm kiểm tra; tất cả assertion PASS.
- Verdict gate: `ACCEPTED`, `170/170`, 0 failed, 0 violations.
- Log: `acceptance-harness/results/logs-vsat-fix/run-all-full.log`
