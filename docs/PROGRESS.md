# Theo dõi tiến độ dự án UnlockVocab

> **File ghi chú bắt buộc cho Huy & mọi AI Agent:** Đọc file này trước khi code và cập nhật lại ngay sau khi hoàn thành mỗi giai đoạn/tính năng.

---

## 1. Trạng thái tổng quan hiện tại

- **Giai đoạn hiện tại:** Đã hoàn thành **100% Giai đoạn 0, 1, 2, 3** và **~40% Giai đoạn 4** (`v0.2.1-phrases-and-ui-fix`) — xem tổng quan tại [CURRENT_STAGE.md](../CURRENT_STAGE.md) và bảng giai đoạn tại [STAGES.md](STAGES.md).
- **Nhánh Git chính:** `main`
- **Mốc Git hiện tại:** `v0.2.1-phrases-and-ui-fix` (đã gộp từ nhánh `fix/phrases-crash-and-ui`)
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

### Mốc `v0.2-stage3-complete` (Hoàn thành 100% Giai đoạn 3 & Biểu đồ Giai đoạn 4)
- [x] **Chống học vẹt (`QuizQuestionStyle`):** Thêm lựa chọn kiểu câu hỏi (`AUTO_SM2`, `EN_TO_VI`, `VI_TO_EN`, `LISTEN_TO_VI`) trong `Models.kt`, `SettingsStore.kt`, `VocabRepository.kt`, `HomeScreen.kt` và `QuizOverlayManager.kt`.
- [x] **Sửa từ & Nạp gói từ / Nhập nhanh (`AppDatabase.kt`, `VocabScreen.kt`):** Cho phép sửa trực tiếp từ đã học (giữ nguyên tiến độ SM-2), nạp gói B1–B2 từ `dict_entries`, và nhập nhanh nhiều dòng từ danh sách.
- [x] **Biểu đồ & Phân tích KHKT (`AppDatabase.kt`, `StatsScreen.kt`):** Thanh phân bố mức độ ghi nhớ SM-2 (*Thuộc vững / Đang học / Chưa học*), biểu đồ cột 7 ngày gần nhất, Top 5 từ hay sai nhất, và nút xuất kho từ CSV (`unlockvocab_study_words.csv`).
- [x] **Kiểm thử trên `R58TA1ESSZN`:** Build thành công (`BUILD SUCCESSFUL in 1m 8s`), cài đè OK; test thực tế `Việt → Anh` trên Overlay (`ghi, ghi chép (verb)` với 4 đáp án động từ tiếng Anh `may/record/thank/follow`, ẩn nút Nghe); kiểm tra `Từ vựng` (`+ Nạp gói B1–B2`, `+ Nhập nhanh`, `Sửa`) và `Thống kê` (hiển thị 19/700 đã học, 47 lượt ngày 18/11, top từ sai), 0 lỗi `AndroidRuntime:E`.

### Mốc `v0.2.1-phrases-and-ui-fix` (Hỗ trợ từ ghép, sửa crash `+ Nạp gói B1–B2`, bỏ giới hạn 200 từ & responsive UI)
- [x] **Hỗ trợ từ ghép / cụm từ (`scripts/build_offline_data.py`):** Mở rộng nhận diện cụm từ 2–3 từ, bổ sung 55 cụm từ thông dụng (`go to`, `look after`, `give up`...) vào `vocab_seed.json` (755 mục) và 1.500 cụm từ/từ ghép vào `dict_compact.json` (12.000 mục); nâng `DB_VERSION = 2` để tự động làm mới `dict_entries` và nạp thêm cụm từ mới mà vẫn giữ nguyên tiến độ SM-2 và từ tự thêm.
- [x] **Sửa crash `+ Nạp gói B1–B2` (`AppDatabase.kt`):** Sửa lỗi `SQLiteException: ambiguous column name: cefr` bằng cách gắn rõ tiền tố `d.cefr`, `d.pos`, `d.word`.
- [x] **Bỏ giới hạn 200 từ (`AppDatabase.kt`, `VocabScreen.kt`):** Tăng limit lên `5000`, hiển thị tổng số từ thực tế (`Kho từ vựng (857 từ)`), thêm chip lọc `phrase`.
- [x] **Tối ưu Responsive UI màn hình 360dp (`VocabScreen.kt`, `HomeScreen.kt`, `StatsScreen.kt`):** Dùng `Modifier.weight(1f)` + `CompactTextAction` (`maxLines = 1, softWrap = false`) để các nút `Nghe`, `Sửa`, `Xóa` trên thẻ từ không bị gãy dòng chữ và không tràn khung trên `SM_A047F`.
- [x] **Kiểm chứng thực tế trên `R58TA1ESSZN`:** Build thành công (`BUILD SUCCESSFUL in 59s`), cài đè OK; logcat ghi nhận `Seeded study_words: 757`, `Seeded dict_entries: 12000`; bấm trực tiếp `+ Nạp gói B1–B2` thêm được 100 từ lên `857 từ` không crash; gõ `"go to"` gợi ý ra `go to (phrase) /ɡoʊ tuː/`; kiểm tra tọa độ nút `Xóa` trên `persevere (verb)` nằm gọn trên 1 dòng (`height = 38px`); 0 lỗi `AndroidRuntime:E`.

---

## 3. Việc tiếp theo (Hoàn tất Giai đoạn 4)

- [ ] **Chạy thực nghiệm thực tế nhiều ngày trên Samsung A04s (`SM_A047F`):** Thu thập log `unlockvocab_review_logs.csv` làm số liệu thực nghiệm đối chứng cho báo cáo KHKT & Tin học trẻ bảng D.
- [ ] **Đóng gói APK Release & Chuẩn bị demo:** Build bản ký số Release và quay video demo thực tế.
