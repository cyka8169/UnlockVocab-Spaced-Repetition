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

### 2.3. Offline hoàn toàn, Từ điển uy tín & Cơ chế Thêm/Gợi ý từ
- **100% Offline:** Toàn bộ tra cứu từ điển, thuật toán SM-2, dữ liệu học tập và phát âm (`TextToSpeech`) phải chạy offline không cần Internet.
- **Nguồn dữ liệu từ điển mở & hợp lệ bản quyền (dùng cho KHKT/THT):**
  - Nghĩa Anh–Việt: Từ điển TS. Hồ Ngọc Đức (FVDP/OVDP, GPL) & Kaikki.org / Wiktionary (CC BY-SA).
  - Bộ từ cốt lõi theo tần suất/cấp độ: NGSL / NAWL / TSL (CC BY-SA) & CEFR-J / `olp-en-cefrj` (CC BY-SA 4.0).
  - Phiên âm IPA: `open-dict-data/ipa-dict` (MIT). Câu ví dụ: Tatoeba (CC BY / CC0).
- **Kiến trúc dữ liệu 2 lớp:**
  - `dict_entries` (kho tra cứu chỉ đọc + FTS): phục vụ tìm kiếm/gợi ý nhanh khi người dùng thêm từ.
  - `study_words` (Room DB): lưu từ người dùng đang học, trạng thái SM-2, và từ tự thêm.
- **Cơ chế Gợi ý & Tự thêm từ (bắt buộc đủ trường):**
  - Khi người dùng gõ vào ô thêm từ, hiển thị danh sách **gợi ý tự động (autocomplete)** từ kho từ điển offline (kèm IPA, từ loại, các nghĩa để chọn nhanh).
  - Nếu **không có từ sẵn** trong từ điển offline, cho phép **người dùng tự thêm mới** với kiểm tra hợp lệ bắt buộc đảm bảo đủ:
    1. **Từ vựng (`word`)** (không rỗng, đã chuẩn hóa).
    2. **Từ loại (`pos`)** (chọn từ danh sách chuẩn: `noun`, `verb`, `adj`, `adv`, `prep`, `phrase`, `other`... để thuật toán sinh đáp án nhiễu hoạt động đúng).
    3. **Các nghĩa (`meanings`)**: bắt buộc ít nhất 1 nghĩa, hỗ trợ thêm **nhiều nghĩa** (1 nghĩa chính ngắn gọn cho đáp án trắc nghiệm + danh sách các nghĩa phụ/mở rộng).

### 2.4. Chống học vẹt & Tương thích Android 14 (Samsung One UI)
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
4. **Theo dõi giai đoạn & tiến độ (`CURRENT_STAGE.md`, `docs/STAGES.md`, `docs/PROGRESS.md`) — BẮT BUỘC:**
   - Trước khi bắt đầu làm việc mới, đọc [CURRENT_STAGE.md](CURRENT_STAGE.md), [docs/STAGES.md](docs/STAGES.md) và [docs/PROGRESS.md](docs/PROGRESS.md) để nắm rõ dự án đang ở giai đoạn nào và những file/mốc nào đang hoạt động.
   - Sau mỗi hạng mục hoàn thành, cập nhật đầy đủ cả [CURRENT_STAGE.md](CURRENT_STAGE.md), [docs/STAGES.md](docs/STAGES.md) (nếu thay đổi % hoặc chuyển giai đoạn) và [docs/PROGRESS.md](docs/PROGRESS.md): việc đã làm, file đã tạo/sửa, kết quả test/build trên `adb`/máy thật, commit/tag mới nhất, và việc tiếp theo.
5. **Cập nhật bộ tệp bàn giao (`.docx` & `.apk`) — BẮT BUỘC:**
   - Sau mỗi lần thêm/sửa tính năng, giao diện hoặc nâng cấp phiên bản app, **PHẢI** đồng bộ và xuất lại đủ 3 tệp tại thư mục gốc dự án:
     1. **`Gioi_Thieu_San_Pham_UnlockVocab.docx`** (cập nhật qua [scripts/generate_docx_deliverables.py](scripts/generate_docx_deliverables.py), đồng thời giữ đồng bộ nội dung với [docs/GIOI_THIEU_DU_AN.md](docs/GIOI_THIEU_DU_AN.md) và [README.md](README.md)).
     2. **`Huong_Dan_Su_Dung_UnlockVocab.docx`** (cập nhật qua [scripts/generate_docx_deliverables.py](scripts/generate_docx_deliverables.py) nếu thay đổi cách cài đặt, quyền, giao diện hoặc thao tác người dùng).
     3. **`UnlockVocab.apk`** (build lại APK mới nhất bằng `.\gradlew.bat assembleDebug`, test trên máy thật qua `adb`, rồi copy `app\build\outputs\apk\debug\app-debug.apk` ra `UnlockVocab.apk` ở thư mục gốc).
6. **Quy tắc an toàn:**
   - **KHÔNG** dùng `git push --force` hoặc xóa lịch sử trên `main` khi chưa có yêu cầu rõ ràng của chủ dự án.
   - Khi sửa code bị lỗi, xem hướng dẫn tại [docs/GIT_ROLLBACK_GUIDE.md](docs/GIT_ROLLBACK_GUIDE.md) hoặc chạy `.\scripts\rollback.ps1`.
   - Khi bàn giao cho người dùng hoặc kết thúc một thay đổi lớn, luôn báo rõ: commit/tag hiện tại, những file đã sửa, và kết quả kiểm tra thực tế trên `adb`/máy thật.
