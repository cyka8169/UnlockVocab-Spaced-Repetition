# Theo dõi tiến độ dự án UnlockVocab

> **File ghi chú bắt buộc cho Huy & mọi AI Agent:** Đọc file này trước khi code và cập nhật lại ngay sau khi hoàn thành mỗi giai đoạn/tính năng.

---

## 1. Trạng thái tổng quan hiện tại

- **Giai đoạn hiện tại:** Hoàn thành **Giai đoạn 1 + Bản vá mở khóa Android 14 Samsung (`v0.1.1-fix-unlock`)**, đã test chu trình tắt/mở khóa thực tế trên máy thật.
- **Nhánh Git chính:** `main`
- **Mốc Git hiện tại:** `v0.1.1-fix-unlock` (đã gộp từ nhánh `fix/unlock-broadcast-samsung`)
- **Thiết bị thật kiểm thử:** Samsung Galaxy A04s (`SM_A047F`, Android 14 / API 34, ADB serial `R58TA1ESSZN`) — đã cài APK, cấp quyền Overlay/Thông báo và test thực tế OK.
- **Công cụ trên máy:** JDK 17 (`17.0.21.0.2+1-LTS`), Android SDK (`android-34` đến `android-37.0`), Gradle Wrapper `9.1.0` (AGP `9.0.1`, Compose BOM `2026.03.01`), Git `2.56.0`, `scrcpy 5.0.1`, Python `3.12.10`.

---

## 2. Nhật ký các hạng mục đã hoàn thành

### Mốc `v0.0-init` (`07d4e27`) & `v0.0-spec-ready` (`60f58d2`)
- [x] Kiểm tra kết nối máy thật Samsung Galaxy A04s (`R58TA1ESSZN`) qua `adb`.
- [x] Cài đặt **Microsoft OpenJDK 17** và **scrcpy 5.0.1**.
- [x] Khởi tạo Git (`main`), tạo `.gitignore`, `AGENTS.md`, `docs/PROJECT_PLAN.md`, `docs/GIT_ROLLBACK_GUIDE.md`.
- [x] Tạo 2 script an toàn: `scripts/checkpoint.ps1` (lưu mốc + tag) và `scripts/rollback.ps1` (quay ngược có tạo nhánh backup).
- [x] Tải 5 bộ dữ liệu từ điển nguồn mở về `data_sources/` + ghi giấy phép vào `data_sources/LICENSE_SOURCES.md`.

### Mốc `v0.1-mvp` (Hoàn thành Giai đoạn 1)
- [x] Tạo và chạy `scripts/build_offline_data.py` đóng gói dữ liệu offline 100% vào `app/src/main/assets/`:
  - `vocab_seed.json`: **700 từ cốt lõi** NGSL + CEFR-J A1–B2 (`208.2 KB`), đảm bảo có từ mẫu `sorry (adj)` -> `"xin lỗi"`.
  - `dict_compact.json`: **9.500 mục từ** tra cứu offline (`1.8 MB`) phục vụ gợi ý tự động.
- [x] Khởi tạo project Android Native (`com.huy.unlockvocab`) với Gradle Wrapper `9.1.0`, AGP `9.0.1`, Jetpack Compose BOM `2026.03.01`:
  - `data/db/Models.kt`, `data/db/AppDatabase.kt` (SQLite 2 lớp: `study_words`, `dict_entries`, `review_logs`), `data/prefs/SettingsStore.kt`, `data/VocabRepository.kt`.
  - `domain/Sm2Scheduler.kt`: thuật toán SM-2 tính theo Đúng/Sai + thời gian phản hồi (`ms`); trạng thái `DISMISSED` không bị phạt điểm SM-2.
  - `service/UnlockService.kt`, `service/BootReceiver.kt`: Foreground Service (`specialUse`) bắt `ACTION_USER_PRESENT` & `ACTION_SCREEN_ON`, kiểm tra cuộc gọi qua `AudioManager`, giờ im lặng và tần suất kích hoạt.
  - `ui/overlay/QuizOverlayManager.kt`: Thẻ trắc nghiệm nổi `WindowManager Overlay` + `TextToSpeech` offline + **chạm bất kỳ vùng nào ngoài 4 ô đáp án (và nút Nghe) để thoát ngay**.
  - `ui/home/HomeScreen.kt`: Trạng thái quyền Android 14 / Samsung, nút test Overlay ngay, chọn chế độ kích hoạt (mỗi lần mở khóa / sau N lần / cách X phút / giờ im lặng) và 5 tiêu chí chọn từ (`DUE_FIRST`, `NEW_WORDS`, `HARDEST_FIRST`, `BY_TOPIC_OR_POS`, `RANDOM_MIXED`).
  - `ui/vocab/VocabScreen.kt`: Danh sách kho từ, tìm kiếm/lọc, hộp thoại Thêm từ hỗ trợ **gợi ý tự động offline (`dict_entries`)** và **form tự thêm bắt buộc đủ Từ vựng + Từ loại + Nghĩa chính (+ nhiều nghĩa phụ)**.
  - `ui/stats/StatsScreen.kt`: Hiển thị tỷ lệ nhớ, phản hồi trung bình, số lượt thoát nhanh, lịch sử chi tiết và **xuất file CSV** qua `FileProvider`.
- [x] **Kiểm tra build & chạy trên máy thật `SM_A047F` (`R58TA1ESSZN`):**
  - `./gradlew.bat assembleDebug` thành công (`BUILD SUCCESSFUL in 2m 28s`, `app-debug.apk` ~11.8 MB).
  - `adb install -r` thành công; logcat ghi nhận `UnlockService started`, `Seeded study_words: 700`, `Seeded dict_entries: 9500`, **0 lỗi `AndroidRuntime:E`**.
  - Đã test trực tiếp trên màn hình máy thật: bấm `Test hiện câu hỏi Overlay ngay` hiện đúng thẻ `open (adj)` với 4 đáp án cùng từ loại `adj`; chạm vùng ngoài ô đáp án đóng thẻ ngay và ghi log `Thoát nhanh (DISMISSED)`; chọn đáp án ghi nhận Đúng/Sai + thời gian phản hồi `ms` trên tab `Thống kê`.

### Mốc `v0.1.1-fix-unlock` (Sửa lỗi tự bật khi khóa/mở khóa trên Android 14 Samsung One UI)
- [x] **Chẩn đoán nguyên nhân (`dumpsys activity broadcasts`):** Trên Samsung One UI 6 (Android 14), broadcast `ACTION_USER_PRESENT` được phát bởi `com.android.systemui` (`uid=10040`), nên bị chặn khi `UnlockService` dùng `RECEIVER_NOT_EXPORTED`.
- [x] **Sửa `service/UnlockService.kt`:** Chuyển sang `Context.RECEIVER_EXPORTED` cho Android 13+, thêm cơ chế theo dõi kép `KeyguardManager.isKeyguardLocked` + cờ `triggeredInCurrentSession` chống trùng lặp trong 1 phiên mở khóa.
- [x] **Sửa `data/prefs/SettingsStore.kt`:** Giảm độ trễ chống lặp burst từ `4000ms` xuống `700ms` để hỗ trợ khóa/mở lại nhanh.
- [x] **Kiểm chứng thực tế trên `R58TA1ESSZN`:** Thực hiện 2 chu trình khóa màn hình (`KEYCODE_SLEEP`) → mở khóa liên tiếp khi app ở nền; logcat ghi nhận `Unlock event [ACTION_USER_PRESENT] -> shouldShow=true` ở cả 2 lần, Overlay tự bật đúng từ `ground (noun)` và `across (adv)`, 0 lỗi `AndroidRuntime:E`.

---

## 3. Việc tiếp theo (Giai đoạn 2 → 4)

- [ ] **Test mở khóa màn hình thực tế trên Samsung A04s:** Bạn Huy khóa máy và mở khóa thực tế để cảm nhận độ mượt, đồng thời vào màn hình chính app bấm **"Tắt tối ưu pin"** để Samsung One UI không đưa app vào chế độ ngủ sâu.
- [ ] **Thử nghiệm tính năng thêm từ & xuất CSV:** Thử gõ tìm từ có sẵn trong gợi ý offline, tự thêm từ chuyên ngành mới (nhiều nghĩa), và kiểm tra xuất file CSV.
- [ ] **Mở rộng nâng cao (khi cần cho KHKT / Tin học trẻ bảng D):**
  - Thêm chế độ đảo chiều câu hỏi (Việt → Anh / Nghe phát âm chọn nghĩa) cho những từ đã ôn lặp >= 2 lần.
  - Bổ sung bộ từ theo chủ đề THPT / IELTS và tính năng nhập hàng loạt từ file CSV.
  - Thu thập dữ liệu thực nghiệm nhiều ngày trên `SM_A047F` để lập biểu đồ cho báo cáo KHKT.
