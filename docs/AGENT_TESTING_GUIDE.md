# Hướng dẫn Kiểm thử Toàn diện cho AI Agent (QA Playbook — UnlockVocab)

> **Mục đích:** Tài liệu này hướng dẫn chi tiết cho các AI Agent QA/Tester cách thiết lập môi trường, chạy bộ **Tự kiểm thử lõi (`CoreSelfTester` — 11 test)**, và thực thi đầy đủ **8 kịch bản kiểm thử thực tế (`TC-01` → `TC-08`)** trên điện thoại thật qua `ADB` + `uiautomator dump`.

---

## 1. Chuẩn bị môi trường & Thiết bị kiểm thử

1. **Thiết bị thật ưu tiên:** Samsung Galaxy A04s (`SM_A047F`, Android 14 / One UI 6, màn hình `720x1600` — mật độ `360dp`, ADB serial `R58TA1ESSZN`).
2. **Kiểm tra JDK & kết nối thiết bị (PowerShell 1 dòng trên Windows):**
   ```powershell
   cd "d:\a_project\app_tieng_anh_Spaced_Repetition"
   $env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
   adb devices
   ```
3. **Build & Cài APK mới nhất lên thiết bị:**
   ```powershell
   $env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
   .\gradlew.bat assembleDebug --console=plain
   adb -s R58TA1ESSZN logcat -c
   adb -s R58TA1ESSZN install -r app\build\outputs\apk\debug\app-debug.apk
   ```
4. **Cấp quyền tự động nếu cài mới (hoặc sau khi `pm clear`):**
   ```powershell
   adb -s R58TA1ESSZN shell appops set com.huy.unlockvocab SYSTEM_ALERT_WINDOW allow
   adb -s R58TA1ESSZN shell pm grant com.huy.unlockvocab android.permission.POST_NOTIFICATIONS
   ```
5. **Mẹo xử lý màn hình khóa & Overlay khi dùng `uiautomator dump`:**
   - Khi mở khóa (`KEYCODE_WAKEUP` + `wm dismiss-keyguard`), `UnlockService` có thể bật ngay thẻ trắc nghiệm Overlay lên trên app, làm `uiautomator dump` chụp thẻ Overlay hoặc báo `null root node returned by UiTestAutomationBridge` trong lúc chuyển cảnh.
   - **Cách xử lý chuẩn:** Luôn chạm vào góc trên màn hình (`adb -s R58TA1ESSZN shell input tap 100 150`) để kích hoạt **Thoát nhanh 1 chạm** đóng Overlay (nếu có), chờ `500ms` rồi mới gọi `uiautomator dump`.
   - **Lưu ý Adaptive Snooze khi test:** Nếu tap thoát nhanh (`DISMISSED`) `>= 2` lần trong vòng 10 phút, app sẽ tự động bật chế độ tạm nghỉ 15 phút (đúng thiết kế Gói A). Khi cần test mở khóa tiếp, bấm nút **`Bật lại ngay`** trên tab **Trang chủ** hoặc trên thông báo nền.

---

## 2. Chạy bộ Tự kiểm thử Lõi (`CoreSelfTester` — 11 test tự động)

Ứng dụng tích hợp sẵn lớp [CoreSelfTester.kt](../app/src/main/java/com/huy/unlockvocab/domain/CoreSelfTester.kt) để kiểm tra **11 điều kiện bất biến** mà không làm hỏng tiến độ học của người dùng:

1. `SM2_DISMISSED_NO_PENALTY`: Chạm ngoài để thoát (`DISMISSED`) không làm thay đổi `repetitions`, `intervalDays`, `easeFactor`.
2. `SM2_FAST_CORRECT_Q5`: Trả lời đúng `< 2500ms` đạt `q = 5`, tăng `easeFactor > 2.5` và đặt `intervalDays = 1`.
3. `SM2_SECOND_REP_6_DAYS`: Đúng lần 2 đặt `intervalDays = 6`.
4. `SM2_WRONG_LAPSE_FLOOR`: Trả lời sai đặt `repetitions = 0`, tăng `lapses`, không đẩy `easeFactor < 1.3`.
5. `OFFLINE_DICT_50K_READY`: CSDL `offline_dict.db` có `>= 50.000` mục từ.
6. `CEFR_LEVELS_A1_TO_C2_PRESENT`: Đủ cả 6 cấp `A1`–`C2` trong `offline_dict.db`.
7. `DISTRACTOR_SAME_POS_NO_COLLISION`: Sinh 3 đáp án nhiễu khác đáp án đúng.
8. `DICTIONARY_BIDIRECTIONAL_LOOKUP`: Tra `"look"` trả về `>= 5` mục hợp lệ.
9. `CLOZE_MASKING_CHECK`: Kiểu câu hỏi `CLOZE_CONTEXT` tạo câu có chỗ trống `____` và đúng 1 đáp án đúng.
10. `PRE_POST_TEST_GENERATOR`: Bộ sinh đề Pre/Post-test tạo đủ 15 câu hỏi hợp lệ.
11. `ANTI_THEFT_OWNERSHIP_GUARD`: Kiểm tra chéo tên gói `com.huy.unlockvocab`, chứng thư ký số APK SHA-256 hợp lệ và bảng thủy vân `_author_meta` trong `offline_dict.db` (`VERIFIED_ORIGINAL`).

**Cách chạy và đọc kết quả:**
- Mở app → chạm tab **Thống kê** → cuộn tới thẻ **Đo lường đối chứng KHKT** → bấm **`Tự kiểm thử lõi`**.
- Đọc logcat bằng lệnh:
  ```powershell
  adb -s R58TA1ESSZN logcat -d -s UnlockVocabTest:I AndroidRuntime:E
  ```
- **Điều kiện Đạt:** Thấy dòng `[SELF_TEST_PASS] Passed 11/11 checks`, hộp xanh `SELF_TEST_PASS (11/11 kiểm tra lõi đạt)` trên UI, và `AndroidRuntime:E` trống (0 crash).

---

## 3. Danh sách 8 Kịch bản Kiểm thử Thực tế (`TC-01` → `TC-08`)

Agent QA thực hiện tuần tự bằng `adb shell input`, `uiautomator dump` và `logcat`:

### `TC-01`: Mở khóa màn hình thật & Thoát nhanh 1 chạm (Core Requirement)
- **Thao tác:**
  1. Đảm bảo công tắc `Bật câu hỏi khi mở khóa` đang **BẬT** và không trong chế độ tạm nghỉ / giờ im lặng.
  2. Đưa app về nền (`KEYCODE_HOME`), khóa màn hình (`KEYCODE_SLEEP`), đợi 2 giây, mở sáng (`KEYCODE_WAKEUP`) rồi mở khóa (`wm dismiss-keyguard`).
  3. Dump UI kiểm tra thẻ trắc nghiệm nổi lên với đúng 4 đáp án A/B/C/D cùng từ loại (`pos`).
  4. Chạm vào nền mờ xung quanh thẻ hoặc tiêu đề từ (ngoài 4 ô đáp án và nút Nghe).
- **Kỳ vọng:**
  - Thẻ đóng ngay lập tức (`< 300ms`).
  - Logcat `UnlockVocab:D` ghi nhận `Unlock event [ACTION_USER_PRESENT] -> shouldShow=true`.
  - Tab **Thống kê** ghi log mới dạng `Thoát nhanh`, số lượt `Thoát nhanh` tăng 1, thông số SM-2 của từ đó không bị phạt.

### `TC-02`: Gói A — Silent-aware TTS, Tạm nghỉ 30 phút, Adaptive Snooze & Giới hạn ngày
- **Thao tác:**
  1. Đặt điện thoại sang chế độ Rung (`adb -s R58TA1ESSZN shell cmd audio set-ringer-mode VIBRATE`), chọn kiểu câu hỏi `Luyện nghe → Việt`, bấm `Test hiện câu hỏi Overlay ngay`.
  2. Kiểm tra text nhắc `"Máy đang Im lặng/Rung — bấm 'Nghe' khi sẵn sàng:"` trên Overlay (không tự phát tiếng). Trả lại `set-ringer-mode NORMAL`.
  3. Chạm ngoài ô đáp án thoát nhanh 2 lần liên tiếp trong < 10 phút → kiểm tra trên **Trang chủ** tự hiện banner `Đang tạm nghỉ (còn 15 phút)` + nút `Bật lại ngay`.
  4. Bấm `Bật lại ngay`, sau đó bấm `Tạm nghỉ 30 phút` → kiểm tra notification `dumpsys notification` có `actions=1` và **Trang chủ** báo `Đang tạm nghỉ (còn ... phút)`. Bấm `Bật lại ngay`.
  5. Chỉnh stepper `Giới hạn/ngày` từ `Vô hạn` sang `10 câu` rồi trả lại `Vô hạn`; kiểm tra hàng không bị vỡ layout.
  6. Ở mục `Tiêu chí chọn từ`, chọn `Lọc theo từ loại / cấp độ` → chọn `prep` + `B2` → kiểm tra khung cảnh báo đỏ `"Chưa có từ nào khớp bộ lọc hiện tại (0 từ)..."`, sau đó trả lại `Đến hạn ôn (SM-2)`.

### `TC-03`: Chọn trình độ cá nhân (`A1`–`C2`) & Bộ lọc Từ loại/CEFR
- **Thao tác:**
  1. Trên tab **Trang chủ**, chọn lần lượt chip `C2` rồi `B1` ở thẻ **`Trình độ của bạn (A1 – C2)`**.
  2. Kiểm tra thẻ tóm tắt đầu trang cập nhật chữ `Trình độ` tương ứng (`C2` / `B1`) và mô tả chi tiết bên dưới.
  3. Bấm nút `+ Nạp thêm 50 từ cấp <trình độ> vào kho học` → kiểm tra thông báo xanh số từ đã nạp hoặc báo kho học đã đủ từ cấp độ đó.

### `TC-04`: Gợi ý Offline, Tự thêm từ đủ trường, Từ ghép & Nhập nhanh (Tab `Từ vựng`)
- **Thao tác:**
  1. Vào tab **Từ vựng**, bấm `+ Thêm từ`, gõ `"go to"` → xác nhận danh sách gợi ý từ `offline_dict.db` hiện `go to (phrase) /ɡoʊ tuː/`.
  2. Thử gõ một từ chưa có sẵn (ví dụ `"unlocktestword"`) và bấm `Lưu từ` khi chưa nhập nghĩa → xác nhận có thông báo lỗi validation bắt buộc nhập nghĩa chính. Nhập đủ Từ vựng + Từ loại (`noun`..`phrase`) + Cấp độ CEFR + Nghĩa chính + bấm `+ Thêm nghĩa` để thêm nghĩa phụ → Lưu.
  3. Kiểm tra thẻ từ vừa thêm hiện badge `Tự thêm`, nút `Nghe`, `Sửa`, `Xóa` nằm gọn trên **1 dòng** (tổng chiều cao mỗi nút ~`32–42px`, không gãy thành 2 dòng). Bấm `Xóa` từ test sau khi kiểm tra xong để giữ kho sạch.

### `TC-05`: Nạp thêm & Xoá bớt từ theo cấp độ (`A1`–`C2`) — Bảo vệ 100% từ Tự thêm
- **Thao tác:**
  1. Ghi nhận tổng số từ hiện tại trên tiêu đề `Kho từ vựng (N từ)` và ít nhất 1 từ có nhãn `Tự thêm`.
  2. Bấm **`+ Nạp theo cấp`** → xác nhận mở hộp thoại hiển thị đủ 6 cấp `A1`..`C2` kèm `(số đang có/tổng 50k)`. Chọn `C1`, `25 từ`, bấm `Nạp vào kho học` → kiểm tra tổng từ tăng đúng (hoặc báo đã đủ).
  3. Bấm **`- Xoá bớt từ`** → mặc định chọn `A1` và `50 từ` → bấm `Xoá bớt từ`.
  4. Xác nhận:
     - Tổng số từ giảm đúng số lượng đã xoá.
     - Toàn bộ từ có nhãn **`Tự thêm`** còn nguyên 100%.
     - Khởi động lại app (`am force-stop com.huy.unlockvocab` → `am start`) → kiểm tra số từ vẫn giữ nguyên sau khi xoá, **KHÔNG** bị tự động seed lại.

### `TC-06`: Gói B — Đáp án nhiễu thông minh & Câu hỏi Ngữ cảnh 1 dòng (`Cloze`)
- **Thao tác:**
  1. Trên tab **Trang chủ**, chọn `Kiểu câu hỏi` = `Điền từ ngữ cảnh (Cloze)`.
  2. Bấm `Test hiện câu hỏi Overlay ngay`.
- **Kỳ vọng:**
  - Thẻ Overlay che từ bằng `____`, hiển thị gợi ý nghĩa tiếng Việt, **ẩn nút `Nghe`** trước khi trả lời, và hiển thị 4 đáp án tiếng Anh **cùng từ loại (`pos`)**.
  - Trả lời xong, nhớ chuyển lại `Kiểu câu hỏi` = **`Tự động theo SM-2`**.

### `TC-07`: Tab `Từ điển` Offline 50.000 mục (Tra cứu 2 chiều & `+ Học` 1 chạm)
- **Thao tác:**
  1. Chạm tab **`Từ điển`** trên thanh điều hướng đáy.
  2. Xác nhận tiêu đề `Từ điển Offline (50000 mục)`.
  3. Gõ `"look"` vào ô tìm kiếm → kiểm tra kết quả có `look (noun)`, `look (verb)`...
  4. Chạm mở hộp thoại chi tiết của `look (verb)` → kiểm tra danh sách nghĩa đánh số (`1.`, `2.`, `3.`), câu ví dụ và mục `Cụm từ & Từ liên quan:` (`look at (phrase)`...).
  5. Chạm vào 1 cụm từ liên quan, kiểm tra chuyển sang chi tiết cụm từ đó, bấm `+ Đưa vào kho học` → kiểm tra đổi thành `Đã có trong kho học`.
  6. Thử lọc chip theo `phrase` hoặc `C1`/`C2`, và kiểm tra thanh 4 tab dưới đáy (`Trang chủ`, `Từ vựng`, `Từ điển`, `Thống kê`) không tab nào bị xuống dòng.

### `TC-08`: Gói C — Bảng Ebbinghaus `D1–D21`, Pre/Post-test 15 câu & Xuất CSV (Tab `Thống kê`)
- **Thao tác:**
  1. Vào tab **Thống kê**, kiểm tra:
     - Thẻ `Phân bố mức độ ghi nhớ (SM-2)` hiển thị đủ 4 mức (`Thuộc sâu (≥21d)`, `Mới nhớ (6–20d)`, `Đang học`, `Chưa học`).
     - Thẻ `Đối chiếu Đường cong quên Ebbinghaus (D1–D21)` hiển thị đủ 4 mốc `D1 (≥1d)`, `D3 (≥3d)`, `D7 (≥6d)`, `D21 (≥21d)`.
  2. Bấm `Làm Pre-test`, hoàn thành nhanh 15 câu trắc nghiệm → kiểm tra ô `Pre-test` cập nhật `%` và thông báo lưu kết quả (`score/15 • ms`).
  3. Bấm `Xuất log CSV`, `Xuất kho từ CSV`, `Xuất KHKT Test CSV` → kiểm tra app không crash (`AndroidRuntime:E` trống) và tạo thành công 3 file CSV tại `/sdcard/Android/data/com.huy.unlockvocab/files/` (có cột `origin_watermark`).
  4. Cuộn cuối **Trang chủ** kiểm tra thẻ `Bản quyền & Xác thực tác giả` hiển thị `Chính chủ • Đã xác thực SQLite & APK` (không hiện banner cảnh báo đỏ), và kiểm tra toàn bộ nút trên màn hình 360dp không bị tràn hoặc cắt chữ.

---

## 4. Tiêu chí Nghiệm thu & Mẫu Báo cáo QA cho Agent

Sau khi test xong, Agent QA **PHẢI** kiểm tra lại log lỗi hệ thống và dọn trạng thái máy về sạch (`AUTO_SM2`, `DUE_FIRST`, tắt snooze, ringer `NORMAL`, xóa các file `scratch_*.xml` tạm ở repo):

```powershell
adb -s R58TA1ESSZN logcat -d -s AndroidRuntime:E UnlockVocab:D UnlockVocabTest:I
```

**Mẫu báo cáo kết quả chuẩn:**
- **Phiên bản APK / Commit / Tag:** `...`
- **Thiết bị kiểm thử:** `SM_A047F (R58TA1ESSZN)` / Android 14
- **CoreSelfTester (`UnlockVocabTest`):** `PASS X/11`
- **Kết quả kịch bản (`TC-01` → `TC-08`):** Nêu rõ từng `TC` đạt hay phát hiện lỗi nào (kèm bounds tọa độ XML nếu có lỗi giao diện 360dp hoặc stacktrace từ `AndroidRuntime:E` nếu crash).
- **Trạng thái Logcat & File CSV:** `0 lỗi AndroidRuntime:E`; xác nhận tạo đủ file CSV nếu có test xuất file.
