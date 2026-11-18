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
1. **Thẻ trắc nghiệm nổi khi mở khóa (`WindowManager Overlay`):** Tự động hiện khi mở khóa (hỗ trợ Android 14 / Samsung One UI), tránh bật khi đang có cuộc gọi hoặc giờ im lặng ban đêm.
2. **Thoát nhanh 1 chạm:** Chạm vào bất kỳ vùng nào ngoài 4 ô đáp án (và nút Nghe) để đóng câu hỏi ngay lập tức, không phạt điểm SM-2.
3. **Chống học vẹt:** 3 đáp án nhiễu cùng từ loại (`pos`); tự động nâng cấp từ **Anh → Việt** sang **Việt → Anh** hoặc **Nghe phát âm → Việt** khi đã quen (`repetitions >= 2`).
4. **Cấu hình linh hoạt:** Chọn tần suất (mỗi lần / mỗi N lần / theo phút) và 5 tiêu chí chọn từ (đến hạn SM-2, từ mới, từ hay sai, lọc theo từ loại/CEFR, ngẫu nhiên).
5. **Offline 100% & Kho từ điển mở 2 lớp:**
   - 755 từ/cụm từ cốt lõi (NGSL + CEFR-J A1–B2 + cụm động từ `go to`, `look after`...) + nạp nhanh gói B1–B2.
   - 12.000 mục từ gợi ý tự động (Từ điển TS. Hồ Ngọc Đức + `ipa-dict` + phát âm `TextToSpeech` offline).
   - Tự thêm từ mới (bắt buộc Từ vựng + Từ loại + Nghĩa chính + nhiều nghĩa phụ), sửa từ và nhập nhanh hàng loạt.
6. **Thống kê & Xuất số liệu KHKT:** Biểu đồ 7 ngày, phân bố ghi nhớ SM-2, Top từ hay sai và xuất CSV (`review_logs` & `study_words`).

---

## Hướng dẫn nhanh cho người dùng & lập trình viên

### Cài đặt & Sử dụng trên điện thoại
- Xem hướng dẫn cấp quyền (Overlay, Thông báo, Tắt tối ưu pin Samsung) và cách dùng từng màn hình tại [docs/GIOI_THIEU_DU_AN.md](docs/GIOI_THIEU_DU_AN.md#5-hướng-dẫn-cài-đặt--sử-dụng).

### Build & Cài APK lên máy thật qua ADB
```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.21.21-hotspot"
.\gradlew.bat assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

### Tài liệu dự án & Quy trình an toàn Git
- Trạng thái giai đoạn hiện tại: [CURRENT_STAGE.md](CURRENT_STAGE.md)
- Lộ trình 5 giai đoạn: [docs/STAGES.md](docs/STAGES.md)
- Nhật ký tiến độ & kiểm thử: [docs/PROGRESS.md](docs/PROGRESS.md)
- Hướng dẫn lưu mốc & quay ngược khi code sai: [docs/GIT_ROLLBACK_GUIDE.md](docs/GIT_ROLLBACK_GUIDE.md) (`scripts/checkpoint.ps1`, `scripts/rollback.ps1`)
- Quy chuẩn dành cho AI Agent: [AGENTS.md](AGENTS.md)
