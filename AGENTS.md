# AGENTS.md — Hướng dẫn dự án cho AI Agent & Người phát triển

## 1. Tổng quan dự án
- **Tên dự án:** UnlockVocab — App Android học từ vựng tiếng Anh bằng Spaced Repetition kích hoạt khi mở khóa màn hình.
- **Mục tiêu cuộc thi:** Sáng tạo Khoa học Kỹ thuật (KHKT) & Tin học trẻ bảng D.
- **Thiết bị thật thử nghiệm:** Samsung Galaxy A04s (`SM_A047F`, Android 14 / API 34, ADB serial `R58TA1ESSZN`).
- **Công nghệ cốt lõi:** Native Android (`Kotlin` + `Jetpack Compose` + `Room Database` + `Foreground Service` + `WindowManager Overlay` + Android `TextToSpeech`).

---

## 2. Yêu cầu sản phẩm BẮT BUỘC (không được làm mất khi sửa code)

### 2.1. Cơ chế thoát nhanh (Tap-outside-to-dismiss)
- Khi thẻ trắc nghiệm đang hiện trên màn hình sau khi mở khóa:
  - **Chạm vào bất kỳ vùng nào trên màn hình NGOẠI TRỪ các ô đáp án** (bao gồm nền mờ xung quanh thẻ, khoảng trống trong thẻ, tiêu đề từ vựng...) phải **đóng câu hỏi ngay lập tức**.
  - Không tính là trả lời sai trong thuật toán SM-2; chỉ ghi nhận trạng thái `DISMISSED` trong `ReviewLog` (nếu có bật log) để phục vụ thống kê hành vi.

### 2.2. Kích hoạt & Chọn từ theo nhiều tiêu chí (không ép chỉ cách theo thời gian)
- **Chính sách kích hoạt (Trigger Policy):** Người dùng có thể tùy chọn, không bắt buộc chỉ dùng thời gian chờ cố định:
  - Hiện ở **mỗi lần mở khóa** (cooldown = 0).
  - Hiện **sau mỗi N lần mở khóa** (ví dụ mỗi 2 hoặc 3 lần).
  - Hiện **theo khoảng thời gian chờ** (ví dụ tối thiểu X phút giữa 2 lần).
  - Tùy chọn **khung giờ im lặng** / giới hạn số câu mỗi ngày / bỏ qua khi đang có cuộc gọi.
- **Tiêu chí chọn từ (Word Selection Policy):**
  - `DUE_FIRST`: Ưu tiên từ đến hạn ôn theo thuật toán SM-2 (`nextReviewAt <= now`).
  - `NEW_WORDS`: Ưu tiên từ chưa học lần nào.
  - `HARDEST_FIRST`: Ưu tiên từ có số lần sai nhiều (`lapses` cao, `easeFactor` thấp, hoặc thời gian phản hồi chậm).
  - `BY_TOPIC_OR_POS`: Lọc theo chủ đề (THPT, IELTS, Giao tiếp...) hoặc từ loại (`noun`, `verb`, `adj`, `adv`).
  - `RANDOM_MIXED`: Trộn ngẫu nhiên để ôn phản xạ nhanh.

### 2.3. Chống học vẹt & Tương thích Android 14 (Samsung One UI)
- 3 đáp án nhiễu ưu tiên **cùng từ loại (`pos`)** với từ được hỏi.
- Thuật toán SM-2 kết hợp **Đúng/Sai + Thời gian phản hồi (ms)**.
- Lưu `ReviewLog` đầy đủ (wordId, outcome: `CORRECT` / `WRONG` / `DISMISSED`, responseTimeMs, timestamp) và hỗ trợ xuất CSV/JSON làm số liệu thực nghiệm KHKT.
- Vì Android 10+ chặn tự mở Activity từ nền khi nhận `ACTION_USER_PRESENT`, sử dụng **Foreground Service + `SYSTEM_ALERT_WINDOW` Overlay**, kèm màn hình **Onboarding** hướng dẫn cấp quyền Overlay và tắt tối ưu pin trên Samsung.

---

## 3. Quy trình Git & Quay ngược khi code sai (BẮT BUỘC TUÂN THỦ)

Mọi Agent khi làm việc trên repo này **PHẢI** tuân thủ kỷ luật Git để Huy luôn có thể quay ngược lại bản chạy ổn định:

1. **Kiểm tra trạng thái trước khi sửa:**
   ```powershell
   git status
   git log --oneline -n 5
   ```
2. **Tạo nhánh riêng cho mỗi tính năng hoặc thay đổi lớn:**
   ```powershell
   git checkout -b feat/ten-tinh-nang
   ```
   *(Chỉ gộp vào `main` sau khi đã build thành công và test ổn trên máy thật.)*
3. **Lưu mốc (Checkpoint) sau mỗi bước chạy ổn:**
   - Dùng script có sẵn:
     ```powershell
     .\scripts\checkpoint.ps1 -Message "feat: mo ta ngan gon" -Tag "v0.1-ten-moc"
     ```
   - Hoặc chạy thủ công `git add -A`, `git commit -m "..."`, `git tag v0.x-...`.
4. **Quy tắc an toàn:**
   - **KHÔNG** dùng `git push --force` hoặc xóa lịch sử trên `main` khi chưa có yêu cầu rõ ràng của chủ dự án.
   - Khi sửa code bị lỗi, xem hướng dẫn tại [docs/GIT_ROLLBACK_GUIDE.md](docs/GIT_ROLLBACK_GUIDE.md) hoặc chạy `.\scripts\rollback.ps1`.
   - Khi bàn giao cho người dùng hoặc kết thúc một thay đổi lớn, luôn báo rõ: commit/tag hiện tại, những file đã sửa, và kết quả kiểm tra thực tế trên `adb`/máy thật.
