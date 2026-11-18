# UnlockVocab — Học Từ Vựng Tiếng Anh Khi Mở Khóa Màn Hình (Spaced Repetition • Offline 100%)

> **Dự án dự thi Khoa học Kỹ thuật (KHKT) & Tin học trẻ bảng D**  
> Giải pháp *"Không tự giác thì bị giác"*: biến thao tác mở khóa điện thoại hằng ngày thành thói quen ôn từ vựng 3 giây dựa trên thuật toán **SuperMemo SM-2**, chạy **offline 100%**, **miễn phí & không quảng cáo**.

---

## Tại sao làm dự án này?
- **Vấn đề:** Người học thường có động lực cao trong ~2 tuần đầu, tải nhiều app từ vựng nhưng nhanh chóng bỏ cuộc vì rào cản phải *tự nhớ và chủ động mở app* mỗi ngày.
- **Cơ sở khoa học:**
  - Theo **Mô hình hành vi Fogg (2009)** ($B = M \times A \times T$), khi động lực ($M$) giảm, thao tác phải tìm và mở app làm độ dễ ($A$) thấp khiến hành vi học bị đứt gãy. Gắn câu hỏi vào **lần mở khóa máy** (trung bình 57–80+ lần tương tác/ngày theo các nghiên cứu HCI) tạo ra tác nhân kích hoạt ($T$) tự nhiên với độ ma sát gần bằng 0.
  - Kết hợp **Hiệu ứng giãn cách** (Cepeda et al., 2006), **Hiệu ứng kiểm tra chủ động** (Roediger & Karpicke, 2006) và thuật toán **SM-2** (Woźniak, 1990) đo theo tốc độ phản hồi thực tế (`ms`).
- **Điểm khác biệt so với thị trường:** Khác với các app thương mại có quảng cáo/paywall (*WordBit*, *Quizlet*...) hay app phải tự mở thủ công (*AnkiDroid*), **UnlockVocab** vừa tự bật lúc mở khóa trên Android 14, vừa hỗ trợ **Thoát nhanh 1 chạm ngoài ô đáp án** khi người dùng đang vội, vừa miễn phí và offline 100%.

*(Xem phân tích chi tiết, trích dẫn đầy đủ và bảng so sánh thị trường tại [docs/GIOI_THIEU_DU_AN.md](docs/GIOI_THIEU_DU_AN.md).)*

---

## Tính năng chính
1. **Thẻ trắc nghiệm nổi khi mở khóa (`WindowManager Overlay`):** Tự động hiện khi mở khóa (hỗ trợ Android 14 / Samsung One UI), tránh bật khi đang có cuộc gọi, giờ im lặng ban đêm hoặc khi người dùng bật **Tạm nghỉ 30 phút**.
2. **Thoát nhanh 1 chạm & Tạm nghỉ thông minh (Adaptive Snooze):** Chạm vào bất kỳ vùng nào ngoài 4 ô đáp án (và nút Nghe) để đóng câu hỏi ngay lập tức không phạt SM-2; nếu thoát nhanh 2 lần liên tiếp trong 10 phút (dấu hiệu đang bận), app tự tạm nghỉ 15 phút để không gây phiền.
3. **Nhận diện Im lặng / Rung (Silent-aware TTS):** Tự động không phát tiếng ngoài khi máy đang ở chế độ Im lặng / Rung hoặc âm lượng Media = 0 (trừ khi đeo tai nghe / Bluetooth).
4. **Chống học vẹt & Đáp án nhiễu thông minh:** 3 đáp án nhiễu ưu tiên **cùng từ loại (`pos`) + cùng cấp độ CEFR**, loại trừ nghĩa trùng từ khóa; tự động nâng cấp từ **Anh → Việt** sang **Việt → Anh**, **Nghe phát âm → Việt** (`repetitions >= 2`) và **Điền từ ngữ cảnh 1 dòng — Cloze (`____`)** (`repetitions >= 3`).
5. **Chọn trình độ cá nhân (`A1` – `C2`) & Cấu hình linh hoạt:** Chọn trình độ của bản thân (`A1`, `A2`, `B1`, `B2`, `C1`, `C2`) để ưu tiên từ mới phù hợp; tùy chọn tần suất, **Giới hạn câu/ngày**, và 5 tiêu chí chọn từ (lọc từ loại / CEFR `A1–C2` có cảnh báo khi bộ lọc rỗng).
6. **Offline 100% & Kho từ điển SQLite nén sẵn 50.000 mục (`offline_dict.db`):**
   - **855 từ/cụm từ cốt lõi** khởi tạo sẵn bao phủ đủ 6 cấp độ **`A1` – `C2`** (NGSL + CEFR-J + Octanove C1/C2 + 55 cụm động từ `go to`, `look after`...).
   - **Tab Từ điển Offline (50.000 mục & 4.500 cụm từ / từ ghép):** Tra cứu tức thì 2 chiều Anh ↔ Việt, lọc theo từ loại/CEFR (`A1`–`C2`), nghe phát âm `TextToSpeech`, xem cụm từ liên quan và **bấm `+ Học` 1 chạm** đưa vào kho ôn mở khóa.
   - **Nạp thêm & Xoá bớt từ theo cấp độ (`A1` – `C2`):** Tự chọn 1 hoặc nhiều cấp độ bất kỳ từ `A1` đến `C2`, lọc theo từ loại và số lượng (`25 / 50 / 100 / 200 từ`) để nạp thêm hoặc xoá bớt từ mặc định chưa học (luôn **giữ nguyên 100% từ tự thêm**); hỗ trợ tự thêm từ mới (bắt buộc Từ vựng + Từ loại + Nghĩa chính + nhiều nghĩa phụ), sửa từ và nhập nhanh hàng loạt.
7. **Thống kê, Đối chiếu Ebbinghaus & Pre/Post-test KHKT:** Phân bố 4 mức ghi nhớ SM-2 (*Thuộc sâu ≥21d / Mới nhớ 6–20d / Đang học / Chưa học*), bảng đối chiếu đường cong quên **Ebbinghaus (`D1 / D3 / D7 / D21`)**, **bài kiểm tra đối chứng Pre-test / Post-test 15 câu offline**, biểu đồ 7 ngày, Top từ hay sai, nút **Tự kiểm thử lõi (11 test)** và xuất CSV (`review_logs`, `study_words`, `pre_post_tests`).
8. **Chống đánh cắp & Xác thực bản quyền tác giả (5 lớp gia cố R8):**
   - Ký số bản phát hành **Release APK bằng R8 obfuscation toàn phần** (`isMinifyEnabled = true`, `isShrinkResources = true`, dung lượng ~`4,0 MB`), làm rối cả các gói lõi `data`, `domain`, `security` (chỉ giữ tên các `enum` lưu DB/Prefs).
   - Nhúng bảng thủy vân `_author_meta` (`origin_signature` + `package_id`) trong CSDL SQLite `offline_dict.db`, kết hợp `OwnershipGuard` dùng mã hóa XOR phụ thuộc vị trí để kiểm tra chéo tên gói (`com.huy.unlockvocab`), chữ ký số **SHA-256** (bản Release chỉ chấp nhận chứng thư Release và từ chối cờ `FLAG_DEBUGGABLE`) và thủy vân SQLite lúc chạy.
   - **Cưỡng chế lúc chạy & minh chứng CSV:** Nếu phát hiện APK bị đóng gói lại trái phép, `UnlockService` tự chặn bật thẻ Overlay, Trang chủ hiển thị cảnh báo đỏ, CSV gắn nhãn `TAMPERED_UNAUTHORIZED`, và bài test #11 (`ANTI_THEFT_OWNERSHIP_GUARD`) báo lỗi. Khi chính chủ, CSV gắn `origin_watermark` và Trang chủ hiện thẻ xác thực kèm ghi công giấy phép dữ liệu mở.

---

## Bản quyền & Tuân thủ dữ liệu nguồn mở
- **Mã nguồn ứng dụng Android & hồ sơ dự thi:** Thuộc bản quyền tác giả (`com.huy.unlockvocab`), xem chi tiết tại [LICENSE](LICENSE).
- **Kho dữ liệu từ điển mở (`offline_dict.db` / `vocab_seed.json`):** Tuân thủ đầy đủ giấy phép gốc **GPL** (Từ điển TS. Hồ Ngọc Đức — FVDP), **CC BY-SA 4.0** (CEFR-J & NGSL) và **MIT** (`ipa-dict`), xem tại [data_sources/LICENSE_SOURCES.md](data_sources/LICENSE_SOURCES.md).

---

## Hướng dẫn nhanh cho người dùng & lập trình viên

### Cài đặt & Sử dụng trên điện thoại
- Xem hướng dẫn cấp quyền (Overlay, Thông báo, Tắt tối ưu pin Samsung) và cách dùng từng màn hình tại [docs/GIOI_THIEU_DU_AN.md](docs/GIOI_THIEU_DU_AN.md#5-hướng-dẫn-cài-đặt--sử-dụng).

### Build & Cài APK lên máy thật qua ADB
```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
.\gradlew.bat assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```
*(Nếu cần build bản Debug phục vụ phát triển: chạy `.\gradlew.bat assembleDebug` và cài `app\build\outputs\apk\debug\app-debug.apk`.)*

### Tài liệu dự án & Quy trình an toàn Git
- Trạng thái giai đoạn hiện tại: [CURRENT_STAGE.md](CURRENT_STAGE.md)
- Lộ trình 5 giai đoạn: [docs/STAGES.md](docs/STAGES.md)
- Nhật ký tiến độ & kiểm thử: [docs/PROGRESS.md](docs/PROGRESS.md)
- Hướng dẫn kiểm thử cho AI Agent QA (`CoreSelfTester` + 8 kịch bản ADB): [docs/AGENT_TESTING_GUIDE.md](docs/AGENT_TESTING_GUIDE.md)
- Hướng dẫn lưu mốc & quay ngược khi code sai: [docs/GIT_ROLLBACK_GUIDE.md](docs/GIT_ROLLBACK_GUIDE.md) (`scripts/checkpoint.ps1`, `scripts/rollback.ps1`)
- Quy chuẩn dành cho AI Agent: [AGENTS.md](AGENTS.md)
