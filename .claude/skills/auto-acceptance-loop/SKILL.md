---
name: auto-acceptance-loop
description: Kích hoạt khi lập kế hoạch, triển khai thử nghiệm, kiểm thử độc lập và nghiệm thu tự động 100% không cần con người (zero-human manual QA) theo vòng lặp điều phối chuẩn Fable - Opus - Sonnet.
---

# Auto Acceptance Loop (Vòng lặp Nghiệm thu Tự động)

Skill điều phối quy trình tự động hóa kiểm thử, triển khai và nghiệm thu độc lập với phân tầng model: **Fable (Orchestrator)** - **Opus (Reviewer/Auditor)** - **Sonnet (Executor/Verifier)**.

---

## 1. Phân quyền Model (Model Routing Rules)

Mọi lệnh gọi `Agent` tool phải tuân thủ nghiêm ngặt quy tắc định tuyến:

1. **Model chính (Orchestrator - Fable / `ag/gemini-4-argon-high`)**:
   - Vai trò: Chỉ điều phối, lập kế hoạch cấp cao, tổng hợp và ra quyết định chuyển pha hoặc kết thúc vòng lặp.
   - **Ranh giới bất khả xâm phạm**: TUYỆT ĐỐI KHÔNG tự chạy thao tác đọc/ghi file (`Read`, `Write`, `Edit`), tìm kiếm mã nguồn (`Grep`, `Glob`) hoặc lệnh shell khảo sát (`Bash`). Mọi khảo sát thực tế phải ủy quyền cho subagent.
2. **Subagent mặc định (`model: "sonnet"`)**:
   - Áp dụng cho: Mọi tác vụ thực thi mã, kiểm thử độc lập, viết báo cáo và kiểm duyệt báo cáo.
   - **Quy tắc bắt buộc**: BẮT BUỘC truyền tường minh `model: "sonnet"` trong tham số của `Agent` tool. Nếu bỏ trống tham số `model`, Claude Code sẽ tự kế thừa model chính (Fable).
3. **Cấm dùng `subagent_type: "fork"`**:
   - Tuyệt đối KHÔNG sử dụng `subagent_type: "fork"` vì cơ chế fork luôn kế thừa model cha và bỏ qua tham số `model`.
   - Luôn sử dụng `subagent_type: "general-purpose"` (hoặc specialist agent tương ứng) kèm `model` tường minh.
4. **Subagent duyệt phân công (`model: "opus"`)**:
   - Riêng khâu thẩm định và phản biện Bảng phân công nhiệm vụ (Bước 2) BẮT BUỘC truyền tường minh `model: "opus"`.

---

## 2. Vòng lặp chuẩn 9 bước (9-Step Standard Loop)

```
[B1: Kế hoạch & Dự thảo] ──> [B2: Opus duyệt] ──> [B3: Chốt phân công v2]
                                                          │
┌─────────────────────────────────────────────────────────┘
│
▼
[B4: Sonnet thực thi (≤3)] ──> [B5: Clean-Room Verify] ──> [B6: Viết báo cáo]
                                                                  │
┌─────────────────────────────────────────────────────────────────┘
│
▼
[B7: Audit báo cáo] ──> [B8: Báo cáo Fable] ──> [B9: Quyết định: Vòng mới / Kết thúc]
```

### Bước 1: Lên kế hoạch bắt đầu & Dự thảo phân công
- Model chính (Fable) vạch mục tiêu và dự thảo Bảng phân công công việc (Task Assignment Matrix).
- Yêu cầu cấu trúc cho từng task:
  - **Exact Scope**: Danh sách file/thư mục cụ thể được phép chạm.
  - **Deliverable & Limits**: Đầu ra mong đợi, giới hạn output (số dòng/từ).
  - **Stop Condition**: Điều kiện dừng rõ ràng, có thể định lượng.
  - **Edit Permission**: Ghi rõ `Allowed` hoặc `Read-Only`.

### Bước 2: Opus duyệt bảng phân công
- Fable spawn 1 subagent (`model: "opus"`, `subagent_type: "general-purpose"`).
- Nhiệm vụ: Phản biện tính chặt chẽ, độ phủ yêu cầu, thứ tự phụ thuộc giữa các task, rủi ro đụng độ file, và chốt chặn an toàn.
- Đầu ra: Danh sách phê duyệt (`APPROVED`) hoặc điều chỉnh bắt buộc (`CHANGES_REQUIRED`).

### Bước 3: Chốt bảng phân công v2
- Fable tiếp thu phản biện từ Opus, cập nhật Bảng phân công chính thức.
- Xuất lệnh điều phối cho các subagent thực thi.

### Bước 4: Các Subagent thực thi
- Spawn subagent thực thi (`model: "sonnet"`).
- Tuân thủ quy tắc: Tối đa 3 subagent chạy song song mỗi đợt.
- Từng subagent bám sát scope và điều kiện dừng đã phân công; không mở rộng phạm vi ngoài giao phó.

### Bước 5: Subagent kiểm tra độc lập (Independent Clean-Room Verification)
- Spawn subagent thẩm tra độc lập (`model: "sonnet"`).
- **Nguyên tắc Clean-Room**:
  - Không dựa dẫm vào bộ test có sẵn (`src/test` cũ, test script legacy).
  - Tự lập Clean-Room Inventory đối chiếu trực tiếp với yêu cầu chức năng và hành vi runtime thực tế.
  - Khuyến nghị tách pha Blind Inventory chạy song song trước khi đọc log thực thi nhằm loại bỏ hoàn toàn thiên kiến xác nhận (confirmation bias).

### Bước 6: Subagent viết báo cáo
- Spawn subagent tổng hợp (`model: "sonnet"`).
- Lập Báo cáo Nghiệm thu với bằng chứng thực chứng:
  - Định dạng đối soát `file:line`.
  - Trích dẫn log kiểm thử thực tế, exit code, mã lỗi hoặc kết quả assertion.

### Bước 7: Subagent check báo cáo (Audit Gate)
- Spawn subagent kiểm toán (`model: "sonnet"`).
- Đối chiếu toàn bộ đường dẫn file, dòng code, lệnh test được trích dẫn trong Báo cáo Bước 6 với hệ thống file thực tế trên đĩa.
- Kết luận: `AUDIT_PASSED` hoặc `REJECTED`.
- Nếu `REJECTED`, yêu cầu Subagent Bước 6 chỉnh sửa tối đa 1 lần trước khi trình nộp.

### Bước 8: Báo cáo cho Model chính
- Trình nộp báo cáo hoàn chỉnh kèm biên bản kiểm toán (Audit Verdict) cho Fable.
- Định dạng súc tích, lược bỏ thông tin râu ria, giữ nguyên số liệu kỹ thuật.

### Bước 9: Điều chỉnh hoặc Kết thúc vòng lặp
- Fable đánh giá kết quả:
  - Nếu còn hạng mục fail hoặc cần bước sang pha tiếp theo (ví dụ: Vòng 1: Lập kế hoạch -> Vòng 2: Dựng Harness -> Vòng 3: Chạy nghiệm thu tự động): Kích hoạt vòng lặp kế tiếp từ Bước 1.
  - Nếu đạt 100% tiêu chí nghiệm thu tự động: Công bố kết thúc vòng lặp.

---

## 3. Nguyên tắc Nghiệm thu Tự động 100% (Zero-Human Acceptance & Safety Gates)

1. **Zero-Human Acceptance**:
   - Con người không tham gia nghiệm thu thủ công hay xác nhận cảm tính.
   - 100% tiêu chí Pass/Fail do máy chấm thông qua exit code (0), JSON/JUnit reports và Coverage Gate đạt 100% Feature IDs.
2. **Chốt chặn an toàn (Safety Gates)**:
   - **Tuyệt đối cấm** trỏ test tự động, stress test hoặc migration vào Database/API/URL production hoặc cloud thật. Mọi môi trường thử nghiệm phải cô lập tại local hoặc mock container.
   - **Tuyệt đối cấm** tự động chạy `git push` hoặc merge lên branch chia sẻ/remote khi chưa có xác nhận từ người dùng.
