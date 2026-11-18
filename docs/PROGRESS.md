# Theo dõi tiến độ dự án UnlockVocab

> **File ghi chú bắt buộc cho Huy & mọi AI Agent:** Đọc file này trước khi code và cập nhật lại ngay sau khi hoàn thành mỗi giai đoạn/tính năng.

---

## 1. Trạng thái tổng quan hiện tại

- **Giai đoạn hiện tại:** Đã hoàn thành **100% Giai đoạn 0, 1, 2, 3** và **~98% Giai đoạn 4** (`v1.5.1-anti-theft-hardened`) — xem tổng quan tại [CURRENT_STAGE.md](../CURRENT_STAGE.md) và bảng giai đoạn tại [STAGES.md](STAGES.md).
- **Nhánh Git chính:** `main` (đồng bộ remote `https://github.com/cyka8169/UnlockVocab-Spaced-Repetition.git` — Private)
- **Mốc Git hiện tại:** `v1.5.1-anti-theft-hardened`
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

### Mốc `v0.2.2-docs-intro` (Tài liệu Giới thiệu, Cơ sở khoa học & So sánh thị trường)
- [x] Tạo `README.md` và `docs/GIOI_THIEU_DU_AN.md`: trình bày bài toán bỏ cuộc sau 2 tuần → giải pháp mở khóa (Fogg Behavior Model, tần suất mở khóa HCI), chứng minh khoa học của phương pháp (Ebbinghaus 1885, Cepeda et al. 2006, Roediger & Karpicke 2006, Woźniak SM-2 1990), bảng so sánh thị trường (WordBit, Semper, AnkiDroid, Quizlet/MochiMochi), hệ thống tính năng và hướng dẫn cài đặt/sử dụng chi tiết.

### Mốc `v1.0-deliverables` (Xuất bộ hồ sơ Word `.docx` & file cài `UnlockVocab.apk`)
- [x] Viết và chạy `scripts/generate_docx_deliverables.py` tạo `Gioi_Thieu_San_Pham_UnlockVocab.docx` (41.1 KB, 32 đoạn, 3 bảng) và `Huong_Dan_Su_Dung_UnlockVocab.docx` (40.3 KB, 42 đoạn, 3 bảng — hướng dẫn cài APK trực tiếp/Play Protect/ADB và sử dụng toàn app).
- [x] Xuất bộ cài `UnlockVocab.apk` (`12.542.274 bytes`) ngay tại thư mục gốc từ bản APK mới nhất đã kiểm chứng trên Samsung A04s (`R58TA1ESSZN`).
- [x] Bổ sung quy định bắt buộc vào `AGENTS.md` (mục 3.5): mọi thay đổi tính năng/giao diện phải cập nhật lại `Gioi_Thieu_San_Pham_UnlockVocab.docx`, `Huong_Dan_Su_Dung_UnlockVocab.docx` và xuất lại `UnlockVocab.apk`.

### Mốc `v1.1-package-a` (Gói A — UX không gây phiền, Adaptive Snooze & Chuẩn hóa 4 mức SM-2)
- [x] **Silent-aware TTS (`QuizOverlayManager.kt`, `VocabRepository.kt`):** Kiểm tra `AudioManager.ringerMode`, âm lượng `STREAM_MUSIC` và thiết bị tai nghe/Bluetooth (`AudioDeviceInfo`); khi máy ở chế độ Im lặng/Rung hoặc tắt Media và không đeo tai nghe, `AUTO_SM2` tự lùi khỏi `LISTEN_TO_VI`, và nếu chọn tay `LISTEN_TO_VI` sẽ không tự phát âm thanh mà hiện nhắc *"Máy đang Im lặng/Rung — bấm 'Nghe' khi sẵn sàng:"*.
- [x] **Adaptive Snooze, Nút Tạm nghỉ 30 phút & Giới hạn ngày (`SettingsStore.kt`, `UnlockService.kt`, `MainActivity.kt`, `HomeScreen.kt`):**
  - Tự động tạm nghỉ **15 phút** nếu thoát nhanh (`DISMISSED`) `>= 2` lần liên tiếp trong vòng 10 phút.
  - Thêm nút **Tạm nghỉ 30 phút** / **Bật lại ngay** đồng bộ trên `HomeScreen` và thanh thông báo Foreground Service (`actions=1`), xử lý cập nhật `SettingsStore` đồng bộ để tránh trễ UI.
  - Thêm stepper **Giới hạn/ngày** (`0` = `Vô hạn`, tăng/giảm bước `5`, hiển thị số câu đã trả lời hôm nay).
- [x] **Chuẩn hóa 4 mức SM-2 & Cảnh báo bộ lọc rỗng (`Models.kt`, `AppDatabase.kt`, `StatsScreen.kt`, `HomeScreen.kt`):**
  - Tách ngưỡng SM-2 thành 4 mức: `Thuộc sâu (≥21d)`, `Mới nhớ (6–20d)`, `Đang học`, `Chưa học`.
  - Hiển thị số từ khớp bộ lọc `BY_TOPIC_OR_POS` và khung cảnh báo rõ khi `0` từ phù hợp (`prep + B2`).
- [x] **Kiểm chứng thực tế trên Samsung Galaxy A04s (`R58TA1ESSZN`) & Đồng bộ bộ tệp bàn giao:**
  - `.\gradlew.bat assembleDebug --console=plain` thành công (`BUILD SUCCESSFUL in 40s`), cài đè `adb install -r` thành công, `AndroidRuntime:E` 0 lỗi crash.
  - Xác nhận thực tế qua `uiautomator dump`: **Tạm nghỉ 30 phút** → banner `Đang tạm nghỉ (còn 29 phút)` + nút `Bật lại ngay` + notification `actions=1`; bộ lọc `prep + B2` hiện cảnh báo `0 từ`; tab `Thống kê` hiện đủ 4 mức `Thuộc sâu (≥21d): 0`, `Mới nhớ (6–20d): 0`, `Đang học: 25`, `Chưa học: 1032`; chế độ `VIBRATE` + `LISTEN_TO_VI` hiện đúng `Máy đang Im lặng/Rung — bấm 'Nghe' khi sẵn sàng:`; thoát nhanh 2 lần liên tiếp tự bật banner `Đang tạm nghỉ (còn 15 phút)`.
  - Đồng bộ `README.md`, `docs/GIOI_THIEU_DU_AN.md`, `scripts/generate_docx_deliverables.py` và xuất mới bộ 3 tệp gốc: `Gioi_Thieu_San_Pham_UnlockVocab.docx` (`42,346 bytes`, 33 đoạn, 3 bảng), `Huong_Dan_Su_Dung_UnlockVocab.docx` (`41,413 bytes`, 42 đoạn, 3 bảng), `UnlockVocab.apk` (`12,542,274 bytes`).

### Mốc `v1.2-package-b` (Gói B — SQLite 50.000 mục, Chọn trình độ & Nạp từ theo cấp độ `A1`–`C2`, Đáp án nhiễu thông minh & Câu hỏi Ngữ cảnh Cloze)
- [x] **Đóng gói sẵn SQLite `offline_dict.db` 50.000 mục & Mở rộng `vocab_seed.json` lên 855 từ (`A1`–`C2`) (`scripts/build_offline_data.py`, `AppDatabase.kt`, `VocabRepository.kt`):**
  - Tạo sẵn `app/src/main/assets/offline_dict.db` (`6.72 MB`, **50.000 mục từ** gồm **4.500 cụm từ/từ ghép** và đầy đủ 6 cấp độ CEFR: `A1: 1971`, `A2: 2128`, `B1: 3542`, `B2: 3752`, `C1: 1209`, `C2: 1083`), đánh sẵn index `idx_dict_word` và `idx_dict_cefr_pos`.
  - Bổ sung 50 từ `C1` và 50 từ `C2` vào `vocab_seed.json` (tổng **855 từ/cụm từ cốt lõi**); nâng `DB_VERSION = 3` để copy trực tiếp `offline_dict.db` khi khởi chạy mà vẫn bảo toàn 100% tiến độ `study_words` và `review_logs`.
- [x] **Chọn trình độ của bạn (`A1` – `C2`) & Nạp từ theo cấp độ tự chọn (`Models.kt`, `SettingsStore.kt`, `HomeScreen.kt`, `VocabScreen.kt`, `MainActivity.kt`):**
  - Thêm `CefrLevel` (`A1`..`C2`) và `userLevel` vào `AppSettings`; hiển thị thẻ **Trình độ của bạn (A1 – C2)** trên `HomeScreen` kèm nút `+ Nạp thêm 50 từ cấp ... vào kho học` và ưu tiên chọn từ mới đúng trình độ khi mở khóa.
  - Thay nút cứng `B1–B2` trên `VocabScreen` bằng **`+ Nạp theo cấp độ`**, mở hộp thoại **`ImportByLevelDialog`** hiển thị số từ từng cấp `A1 (395/1971)` .. `C2 (50/1083)`, cho phép chọn 1 hoặc nhiều cấp độ bất kỳ từ `A1` đến `C2`, lọc theo từ loại và chọn số lượng (`25 / 50 / 100 / 200 từ`); bổ sung chip lọc `A1`..`C2` trên `VocabScreen` và `HomeScreen`.
- [x] **Đáp án nhiễu thông minh & Câu hỏi Ngữ cảnh 1 dòng — Cloze (`AppDatabase.kt`, `VocabRepository.kt`, `QuizOverlayManager.kt`):**
  - Ưu tiên 3 đáp án nhiễu **cùng `pos` + cùng `cefr`**, kiểm tra `tokenizeMeaningSet` loại bỏ đáp án trùng từ khóa tiếng Việt hoặc từ tiếng Anh cùng gốc 5 ký tự.
  - Thêm `QuizQuestionStyle.CLOZE_CONTEXT` và `ResolvedPromptType.CLOZE_CONTEXT` (tự động kích hoạt trong `AUTO_SM2` khi `repetitions >= 3` hoặc chọn cố định): ẩn nút `Nghe` trước khi trả lời và che từ gốc thành `____` trong câu ví dụ.
- [x] **Kiểm chứng thực tế trên Samsung Galaxy A04s (`R58TA1ESSZN`) & Đồng bộ bộ tệp bàn giao:**
  - `.\gradlew.bat assembleDebug --console=plain` thành công (`BUILD SUCCESSFUL in 1m 20s`), cài đè `adb install -r` thành công; logcat ghi nhận `Seeded study_words: 1157 | dict_entries: 50000`, `AndroidRuntime:E` 0 lỗi crash.
  - Xác nhận thực tế qua `uiautomator dump`: chọn trình độ `C2` trên `HomeScreen` hiển thị đúng `Đã đặt trình độ C2 — ưu tiên từ mới ở cấp C2.`; mở hộp thoại `Nạp từ theo cấp độ (A1 – C2)` hiển thị đủ 6 cấp `A1 (395/1971)` .. `C1 (50/1209)`, `C2 (50/1083)`, chọn nạp 25 từ `[C1, C2]` tăng kho từ lên `1182 từ` (`plunge (verb) | C1`); test Overlay `Điền từ ngữ cảnh (Cloze)` hiển thị đúng câu `to have ____ enough time to catch the train (có vừa đủ thời gian để bắt kịp xe lửa)` với 4 trạng từ `B1` (`barely / thus / personally / particularly`).
  - Xuất mới bộ 3 tệp bàn giao tại thư mục gốc: `Gioi_Thieu_San_Pham_UnlockVocab.docx` (`42,564 bytes`, 34 đoạn, 3 bảng), `Huong_Dan_Su_Dung_UnlockVocab.docx` (`41,594 bytes`, 43 đoạn, 3 bảng), `UnlockVocab.apk` (`14,952,746 bytes`).

### Mốc `v1.2.1-remove-by-level` (Xoá bớt từ theo cấp độ `A1`–`C2`, bảo vệ 100% từ tự thêm)
- [x] **Backend xoá có chọn lọc & chống seed lại từ đã xoá (`SettingsStore.kt`, `AppDatabase.kt`, `VocabRepository.kt`):**
  - Thêm `getRemovableCefrCounts()` và `removeWordsByLevels(levels, posFilter, limit)` luôn lọc chặt `WHERE is_custom = 0` (không bao giờ xoá từ tự thêm) và ưu tiên xoá từ chưa học (`repetitions ASC, (total_correct + total_wrong) ASC`) trước.
  - Bổ sung `KEY_SEEDED_VERSION` trong `SettingsStore.kt` để `ensureSeeded()` không tự động nạp lại những từ mặc định mà người dùng đã chủ động xoá khi khởi động lại ứng dụng.
- [x] **Giao diện `- Xoá bớt từ` trên tab Từ vựng (`VocabScreen.kt`):**
  - Bổ sung nút **`- Xoá bớt từ`** cạnh **`+ Nạp theo cấp`** và **`+ Nhập nhanh`** gọn trên 1 hàng trên màn hình 360dp (`SM_A047F`).
  - Nâng cấp `ImportByLevelDialog` hỗ trợ chuyển đổi 2 chế độ **Nạp thêm từ** và **Xoá bớt từ** (`A1`–`C2`), hiển thị số từ mặc định có thể xoá ở từng cấp, mặc định chọn sẵn `A1` và `50 từ` (có thêm lựa chọn `25 / 50 / 100 / 200 / Xoá hết cấp này`).
- [x] **Kiểm chứng thực tế trên Samsung Galaxy A04s (`R58TA1ESSZN`) & Đồng bộ bộ tệp bàn giao:**
  - `.\gradlew.bat assembleDebug --console=plain` thành công (`BUILD SUCCESSFUL in 1m 9s`), cài đè `adb install -r` thành công, `AndroidRuntime:E` 0 lỗi crash.
  - Xác nhận qua `uiautomator dump`: 3 nút `+ Nạp theo cấp`, `- Xoá bớt từ`, `+ Nhập nhanh` nằm gọn 1 dòng (`height = 38px`); mở hộp thoại xoá hiện đủ `A1 (395)`..`C2 (59)`; thực thi xoá 50 từ `A1` giảm kho từ `1182` → `1132` với thông báo `"Đã xoá bớt 50 từ cấp độ [A1] (giữ nguyên 100% từ tự thêm)."`, 2 từ tự thêm `algorithm (noun)` & `persevere (verb)` giữ nguyên 100%, và sau khi `am force-stop` mở lại vẫn giữ `1132 từ`.
  - Đồng bộ lại bộ 3 tệp bàn giao ở thư mục gốc: `Gioi_Thieu_San_Pham_UnlockVocab.docx` (`42,608 bytes`, 34 đoạn, 3 bảng), `Huong_Dan_Su_Dung_UnlockVocab.docx` (`41,647 bytes`, 43 đoạn, 3 bảng), `UnlockVocab.apk` (`14,952,746 bytes`).

### Mốc `v1.3-dictionary` (Tab Từ điển Offline 50.000 mục Anh ↔ Việt, Chi tiết cụm từ liên quan & `+ Học` 1 chạm)
- [x] **Backend tra cứu từ điển 2 chiều & từ liên quan (`AppDatabase.kt`, `VocabRepository.kt`):**
  - Thêm `lookupDictionary(query, posFilter, cefrFilter, limit)` tra cứu tức thì 50.000 mục từ & 4.500 cụm từ trong `offline_dict.db` (hỗ trợ cả tiếng Anh và nghĩa tiếng Việt, sắp xếp ưu tiên khớp đúng → tiền tố → cụm từ/nghĩa).
  - Thêm `getRelatedDictEntries(...)`, `getStudyWordKeySet()`, và `addDictEntryToStudy(...)` cho phép thêm ngay mục từ đang tra vào `study_words` chỉ bằng 1 chạm.
- [x] **Màn hình Từ điển & Thanh điều hướng 4 tab (`DictionaryScreen.kt`, `MainActivity.kt`):**
  - Thêm tab **`Từ điển` (📖)** ngay trên `NavigationBar` (`Trang chủ`, `Từ vựng`, `Từ điển`, `Thống kê`) với nhãn `maxLines = 1, softWrap = false`.
  - Xây dựng `DictionaryScreen` gồm ô tìm kiếm tức thì, chip lọc Từ loại & CEFR (`A1`–`C2`), nút `Nghe` & `+ Học` / `Đã có` trên từng thẻ, và hộp thoại chi tiết `DictionaryDetailDialog` hiển thị nghĩa đánh số, câu ví dụ, và danh sách cụm từ liên quan bấm chuyển tiếp trực tiếp.
- [x] **Kiểm chứng thực tế trên Samsung Galaxy A04s (`R58TA1ESSZN`) & Đồng bộ bộ tệp bàn giao:**
  - `.\gradlew.bat assembleDebug --console=plain` thành công (`BUILD SUCCESSFUL in 1m 2s`), cài đè `adb install -r` thành công, `AndroidRuntime:E` 0 lỗi crash.
  - Xác nhận qua `uiautomator dump`: thanh 4 tab `Trang chủ / Từ vựng / Từ điển / Thống kê` nằm gọn 1 dòng; tab `Từ điển Offline (50000 mục)` tìm `"look"` trả về `33 kết quả`; mở chi tiết `look (verb)` hiển thị 3 nghĩa đánh số, ví dụ và 5 cụm từ liên quan (`look at`, `look in`...); chạm `look at (phrase)` chuyển xem chi tiết và bấm `+ Đưa vào kho học` chuyển thành `Đã có trong kho học` kèm thông báo `"Đã thêm 'look at' (phrase) vào kho học mở khóa."`.
  - Đồng bộ `README.md`, `docs/GIOI_THIEU_DU_AN.md`, `scripts/generate_docx_deliverables.py` và xuất lại bộ 3 tệp bàn giao ở thư mục gốc: `Gioi_Thieu_San_Pham_UnlockVocab.docx` (`42,638 bytes`, 34 đoạn, 3 bảng), `Huong_Dan_Su_Dung_UnlockVocab.docx` (`41,922 bytes`, 46 đoạn, 3 bảng), `UnlockVocab.apk` (`14,952,746 bytes`).

### Mốc `v1.4-package-c-qa-guide` (Gói C — Đối chiếu Ebbinghaus D1–D21, Pre/Post-test 15 câu, CoreSelfTester & Tài liệu QA Agent)
- [x] **Bảng đối chiếu Đường cong quên Ebbinghaus `D1–D21` (`Models.kt`, `AppDatabase.kt`, `StatsScreen.kt`):**
  - Tính `retentionMilestones` cho 4 mốc `D1 (≥1d)`, `D3 (≥3d)`, `D7 (≥6d)`, `D21 (≥21d)` so sánh trực tiếp ngưỡng quên tự nhiên của Ebbinghaus (`33% → 25% → 23% → 21%`) với tỷ lệ nhớ thực tế theo chu kỳ SM-2.
- [x] **Bài kiểm tra đối chứng KHKT Pre-test / Post-test 15 câu (`Models.kt`, `SettingsStore.kt`, `AppDatabase.kt`, `VocabRepository.kt`, `StatsScreen.kt`):**
  - Cho phép chọn phạm vi cấp độ (`Tất cả` hoặc `A1`–`C2`), làm bài 15 câu trắc nghiệm, tự lưu điểm + tốc độ phản hồi TB (`ms`), tính mức tăng trưởng (`+X%`) và xuất file `unlockvocab_pre_post_tests.csv`.
- [x] **Bộ Tự kiểm thử lõi & Chuẩn hóa Release (`CoreSelfTester.kt`, `proguard-rules.pro`, `app/build.gradle.kts`):**
  - Xây dựng `CoreSelfTester` với 10 bài test bất biến (`SM2_DISMISSED_NO_PENALTY`, `SM2_FAST_CORRECT_Q5`, `SM2_SECOND_REP_6_DAYS`, `SM2_WRONG_LAPSE_FLOOR`, `OFFLINE_DICT_50K_READY`, `CEFR_LEVELS_A1_TO_C2_PRESENT`, `DISTRACTOR_SAME_POS_NO_COLLISION`, `DICTIONARY_BIDIRECTIONAL_LOOKUP`, `CLOZE_MASKING_CHECK`, `PRE_POST_TEST_GENERATOR`), ghi log tag `UnlockVocabTest`.
  - Cập nhật `versionCode = 4`, `versionName = "1.4.0-khkt"` và quy tắc ProGuard.
- [x] **Tài liệu Hướng dẫn Kiểm thử cho Agent QA (`docs/AGENT_TESTING_GUIDE.md`, `AGENTS.md`):**
  - Viết chi tiết cách dùng `ADB` + `uiautomator dump` xử lý Overlay mở khóa, cách chạy `CoreSelfTester` và toàn bộ **8 kịch bản kiểm thử (`TC-01` → `TC-08`)** kèm mẫu báo cáo QA chuẩn.
- [x] **Kiểm chứng thực tế trên Samsung Galaxy A04s (`R58TA1ESSZN`) & Đồng bộ bộ tệp bàn giao:**
  - `.\gradlew.bat assembleDebug --console=plain` thành công (`BUILD SUCCESSFUL in 1m 32s`), cài đè `adb install -r` thành công, `AndroidRuntime:E` 0 lỗi crash.
  - Xác nhận trực tiếp qua `uiautomator dump` & `logcat`: `EbbinghausComparisonCard` hiển thị `D1 (≥1d) | ~33% | 63% (14 từ)`; bấm `Tự kiểm thử lõi` ghi log `[SELF_TEST_PASS] Passed 10/10 checks`; mở `Làm Pre-test` hiển thị đúng `Câu 1/15` với 4 đáp án cùng từ loại.
  - Đồng bộ lại bộ 3 tệp bàn giao tại thư mục gốc: `Gioi_Thieu_San_Pham_UnlockVocab.docx` (`42,723 bytes`, 34 đoạn, 3 bảng), `Huong_Dan_Su_Dung_UnlockVocab.docx` (`42,072 bytes`, 46 đoạn, 3 bảng), `UnlockVocab.apk` (`14,572,919 bytes`).

### Mốc `v1.5-anti-theft` (Gói Chống đánh cắp 5 lớp, Ký số Release R8 & Đưa mã nguồn lên GitHub)
- [x] **Khóa ký số Release riêng & Làm rối mã nguồn R8 (`keystore/`, `app/build.gradle.kts`, `app/proguard-rules.pro`, `.gitignore`):**
  - Sinh cặp khóa RSA 2048-bit (`unlockvocab-release.jks`, SHA-256 `325f191da7d701801f1d69582228dc69a2aa49b1232cfb83df53b4f4ee18fc6e`), cấu hình `signingConfigs.create("release")`, bật `isMinifyEnabled = true` và `isShrinkResources = true` cho build `release` (`versionCode = 5`, `versionName = "1.5.0-khkt"`), giữ an toàn `keystore/` trong `.gitignore`.
- [x] **Thủy vân SQLite `_author_meta` & Kiểm tra toàn vẹn đa tầng (`scripts/build_offline_data.py`, `OwnershipGuard.kt`, `AppDatabase.kt`, `VocabRepository.kt`, `CoreSelfTester.kt`):**
  - Nhúng bảng `_author_meta` (`UNLOCKVOCAB-HUY-KHKT-2026-ORIGINAL`) vào `app/src/main/assets/offline_dict.db` (`6.73 MB`), nâng `DB_VERSION = 4` để cập nhật mượt không mất tiến độ người học.
  - Tạo `security/OwnershipGuard.kt` (mã hóa XOR định danh gói `com.huy.unlockvocab`, chứng thư SHA-256 hợp lệ & chữ ký SQLite), thêm cột `origin_watermark` (`UnlockVocab-Huy-Original`) vào 3 tệp CSV xuất ra, và bổ sung test #11 `ANTI_THEFT_OWNERSHIP_GUARD` vào `CoreSelfTester`.
- [x] **Thẻ Bản quyền & Cảnh báo trên `HomeScreen` (`MainActivity.kt`, `HomeScreen.kt`):**
  - Hiển thị thẻ **"Bản quyền & Xác thực tác giả"** ở cuối Trang chủ và banner cảnh báo đỏ nếu phát hiện đóng gói lại bằng khóa lạ hoặc giả mạo dữ liệu.
- [x] **Kiểm chứng thực tế trên Samsung Galaxy A04s (`R58TA1ESSZN`) & Đồng bộ bộ tệp bàn giao:**
  - `.\gradlew.bat assembleDebug assembleRelease --console=plain` thành công (`BUILD SUCCESSFUL in 5m 9s`), tạo `app-debug.apk` (`14,589,798 bytes`) và bản Release R8 ký số `app-release.apk` (`4,236,141 bytes`).
  - Cài đè `app-debug.apk` qua `adb install -r` thành công; UI Trang chủ hiển thị đúng `Chữ ký SHA-256: 7D5D146437AB9B02 (Chính chủ • Đã xác thực SQLite & APK)`; bấm `Tự kiểm thử lõi` ghi log `[PASS] ANTI_THEFT_OWNERSHIP_GUARD` và `[SELF_TEST_PASS] Passed 11/11 checks`, `0` lỗi `AndroidRuntime:E`.
  - Đồng bộ tài liệu và xuất lại bộ 3 tệp bàn giao tại thư mục gốc: `Gioi_Thieu_San_Pham_UnlockVocab.docx` (`42,935 bytes`), `Huong_Dan_Su_Dung_UnlockVocab.docx` (`42,224 bytes`), `UnlockVocab.apk` (`14,589,798 bytes`).

### Mốc `v1.5.1-anti-theft-hardened` (Gia cố R8 toàn phần, Cưỡng chế Release-only, LICENSE nguồn mở & QA trực tiếp Release APK)
- [x] **Gia cố R8 toàn phần (`app/proguard-rules.pro`, `app/build.gradle.kts`):**
  - Gỡ bỏ 3 luật `-keep` rộng trên `data.**`, `domain.**`, `security.**`, chỉ giữ tên các `enum` lưu DB/SharedPreferences (`PosTag`, `TriggerMode`, `WordSelectionPolicy`, `ReviewOutcome`, `CefrLevel`, `QuizQuestionStyle`, `ResolvedPromptType`).
  - Bật `buildConfig = true`, nâng `versionCode = 6`, `versionName = "1.5.1-khkt"`.
  - Kiểm tra `classes.dex` trong `app-release.apk` (`4,219,757 bytes`): xác nhận `OwnershipGuard`, `Sm2Scheduler`, `VocabRepository`, `CoreSelfTester`, `verifyIntegrity`, `325f191d`, `7d5d1464`, `_author_meta`, `UNLOCKVOCAB-HUY` đều biến mất khỏi tên lớp/chuỗi thô (`False`).
- [x] **Nâng cấp `OwnershipGuard.kt` & Cưỡng chế chống giả mạo (`AppDatabase.kt`, `VocabRepository.kt`, `UnlockService.kt`, `MainActivity.kt`, `HomeScreen.kt`, `CoreSelfTester.kt`):**
  - Chuyển sang mã hóa XOR phụ thuộc vị trí (`0x5A + idx * 7`) cho toàn bộ hằng số nhạy cảm (`com.huy.unlockvocab`, SHA-256 Release/Debug, `_author_meta`, `origin_signature`, `package_id`, `UNLOCKVOCAB-HUY-KHKT-2026-ORIGINAL`, tác giả & CSV watermark).
  - Trong bản Release (`!BuildConfig.DEBUG`), chỉ chấp nhận chứng thư Release (`325f191d...`) và từ chối nếu bị bật `FLAG_DEBUGGABLE`.
  - Xác thực đủ cả `origin_signature` và `package_id` trong `_author_meta`; nếu phát hiện giả mạo (`!isAuthentic`), `UnlockService` chặn bật thẻ Overlay, `MainActivity` báo Toast bảo mật và CSV gắn cờ `TAMPERED_UNAUTHORIZED`.
  - Thẻ bản quyền `HomeScreen` hiển thị động `BuildConfig.VERSION_NAME` (`v1.5.1-khkt (Release R8)`) kèm dòng ghi công giấy phép dữ liệu mở.
- [x] **Phân tách bản quyền & chuẩn hóa tài liệu (`LICENSE`, `data_sources/LICENSE_SOURCES.md`, `README.md`, `docs/GIOI_THIEU_DU_AN.md`, `docs/AGENT_TESTING_GUIDE.md`, `AGENTS.md`):**
  - Tạo [LICENSE](../LICENSE) tách bạch Phần A (bản quyền mã nguồn app & hồ sơ KHKT) và Phần B (tuân thủ `GPL`, `CC BY-SA 4.0`, `MIT` cho kho dữ liệu mở).
  - Chuẩn hóa diễn đạt thành **"4.500 cụm từ / từ ghép"** đúng thực tế `offline_dict.db`, cập nhật `AGENTS.md` để `UnlockVocab.apk` luôn lấy từ `app-release.apk`.
- [x] **Kiểm chứng thực tế bản Release R8 trên Samsung Galaxy A04s (`R58TA1ESSZN`) & Xuất bộ bàn giao:**
  - `.\gradlew.bat assembleDebug assembleRelease --console=plain` thành công (`BUILD SUCCESSFUL in 4m 42s`).
  - Gỡ bản Debug cũ, cài mới `app-release.apk` (`4,219,757 bytes`) qua `adb install -r`, cấp Overlay + Thông báo + Battery whitelist.
  - UI Trang chủ hiển thị đúng `v1.5.1-khkt (Release R8)` và `Chữ ký SHA-256: 325F191DA7D70180 (Chính chủ • Đã xác thực SQLite & APK)`, không có banner cảnh báo đỏ.
  - Bấm `Tự kiểm thử lõi` trên bản Release R8: `logcat` ghi `[PASS] ANTI_THEFT_OWNERSHIP_GUARD: mode=release, sha=325F191DA7D70180, reason=VERIFIED_ORIGINAL` và `[SELF_TEST_PASS] Passed 11/11 checks`.
  - Khóa/mở khóa thật (`KEYCODE_SLEEP` → `KEYCODE_WAKEUP` → `wm dismiss-keyguard`): `UnlockService` nhận `ACTION_USER_PRESENT -> shouldShow=true`, bật thẻ `amount (noun)`, chạm ngoài ô đáp án thoát ngay và ghi `Thoát nhanh = 1`, tab `Từ điển` (50.000 mục) hoạt động mượt, `0` lỗi `AndroidRuntime:E`.
  - Đồng bộ xuất lại đủ 3 tệp gốc: `Gioi_Thieu_San_Pham_UnlockVocab.docx` (`43,221 bytes`), `Huong_Dan_Su_Dung_UnlockVocab.docx` (`42,294 bytes`), `UnlockVocab.apk` (`4,219,757 bytes` — SHA-256 `62f7381b35e5873e3390702410da6adbdb7746a4075e72c602115620cbdab597`, trùng khớp `app-release.apk`).

---

## 3. Việc tiếp theo (Hoàn tất Giai đoạn 4)

- [ ] **Chạy thực nghiệm thực tế nhiều ngày trên Samsung A04s (`SM_A047F`):** Thu thập log `unlockvocab_review_logs.csv` làm số liệu thực nghiệm đối chứng cho báo cáo KHKT & Tin học trẻ bảng D.
- [ ] **Quay video demo thực tế & Hoàn thiện thuyết minh dự thi:** Sử dụng bản ký số R8 `app-release.apk` và số liệu CSV đã thu thập.
