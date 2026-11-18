# Giới thiệu Dự án UnlockVocab — Học Từ Vựng Tiếng Anh Khi Mở Khóa Màn Hình Bằng Spaced Repetition

> **Lĩnh vực dự thi:** Khoa học Kỹ thuật (KHKT) & Hội thi Tin học trẻ bảng D (Phần mềm sáng tạo)  
> **Nền tảng:** Android Native (Kotlin + Jetpack Compose + SQLite 2 lớp, Offline 100%)

---

## 1. Lý do làm dự án (Từ Vấn đề thực tiễn → Giải pháp)

### 1.1. Vấn đề thực tiễn: "Hội chứng bỏ cuộc sau 2 tuần"
Hầu hết người học tiếng Anh khi bắt đầu đều có động lực rất cao: tải nhiều app flashcard, tìm trang web, sưu tầm bộ từ vựng... Nhưng chỉ sau khoảng **2 tuần**, phần lớn đều **bỏ dở gần như hoàn toàn**.

**Nguyên nhân gốc rễ theo tâm lý học hành vi (Fogg Behavior Model):**
Theo **Mô hình Hành vi Fogg** của GS. **B.J. Fogg** (Đại học Stanford, 2009), một hành vi $B$ chỉ xảy ra khi hội tụ đủ 3 yếu tố cùng lúc:
$$B = M \times A \times T$$
*(Trong đó: **Motivation** = Động lực, **Ability** = Khả năng thực hiện / độ dễ, **Trigger** = Tác nhân kích hoạt).*

- Ở **2 tuần đầu**, động lực ($M$) rất cao nên người học chịu khó nhớ giờ học, tự tìm icon app, mở app lên ôn tập (dù thao tác rườm rà — độ dễ $A$ thấp).
- Sau ~2 tuần, động lực ($M$) giảm xuống mức bình thường. Lúc này chỉ một **ma sát nhỏ** (phải tự nhớ, tự mở app, chờ tải mạng, bị làm phiền bởi quảng cáo) cũng đủ khiến $B$ không xảy ra → **chuỗi thói quen đứt gãy**.

### 1.2. Giải pháp: "Không tự giác thì bị giác" (Micro-habit gắn vào thao tác mở khóa)
Các nghiên cứu **Tương tác Người – Máy (HCI)** cho thấy trung bình một người tương tác với điện thoại khoảng **57 – 80+ lần mỗi ngày** (có thể tới 80–110 lần bật sáng màn hình nếu tính cả xem nhanh màn hình khóa), trong đó khoảng **43% – 46%** là mở khóa hoàn toàn vào sử dụng máy.

**UnlockVocab** tận dụng chính hành vi vô thức lặp đi lặp lại hằng ngày này làm **Tác nhân kích hoạt ($T$) tự nhiên**:
- Ngay khi người dùng **mở khóa điện thoại**, hệ thống tự bật một thẻ trắc nghiệm 4 đáp án ngắn gọn ngay trước mắt (**"đưa cơm tận miệng"** — nâng **Ability ($A$)** lên tối đa, độ ma sát bằng 0).
- Mỗi lượt chỉ hỏi **1 câu trắc nghiệm ngắn** (mất 2–3 giây suy nghĩ nhẹ nhàng) thay vì ép học cả bài dài 15 phút.
- **Giải quyết xung đột chú ý (*Attention Economy Conflict*):** Các tài liệu HCI cũng cảnh báo nếu ép người dùng làm bài kiểm tra vào lúc đang vội (quét mã QR, nghe gọi, xem bản đồ), họ sẽ bực bội và gỡ app. Vì vậy UnlockVocab thiết kế cơ chế **Thoát nhanh 1 chạm**: chạm vào **bất kỳ vùng nào ngoài 4 ô đáp án** là đóng câu hỏi ngay tức thì và không phạt điểm ôn tập.

---

## 2. Cơ sở khoa học & Hiệu quả của phương pháp

Hiệu quả ghi nhớ dài hạn của **UnlockVocab** được xây dựng trên 4 trụ cột khoa học đã được kiểm chứng:

1. **Đường cong quên lãng (*Forgetting Curve*) — Hermann Ebbinghaus (1885), *Über das Gedächtnis*:**
   - Chứng minh trí nhớ suy giảm rất nhanh theo hàm mũ ngay sau khi học nếu không được ôn lại đúng điểm rơi trước khi quên.
2. **Hiệu ứng giãn cách (*Spacing Effect*) — Nicholas J. Cepeda et al. (2006), *"Distributed practice in verbal recall tasks: A review and quantitative synthesis"*, *Psychological Bulletin*:**
   - Phân tích tổng hợp (meta-analysis) trên quy mô lớn chứng minh việc **chia nhỏ và giãn cách thời gian ôn tập** theo khoảng tăng dần giúp ghi nhớ lâu dài vượt trội so với việc học dồn một lúc (cramming).
3. **Hiệu ứng kiểm tra chủ động (*Testing Effect / Active Recall*) — Henry L. Roediger III & Jeffrey D. Karpicke (2006), *"Test-enhanced learning: Taking memory tests improves long-term retention"*, *Psychological Science*:**
   - Thực nghiệm đối chứng cho thấy nhóm học bằng **tự kiểm tra/trả lời câu hỏi** nhớ tốt hơn rõ rệt sau **2 ngày** và **1 tuần** so với nhóm chỉ đọc lại thụ động tài liệu (dù ngay sau 5 phút nhóm đọc lại có cảm giác nhớ hơn).
   - UnlockVocab áp dụng *Testing Effect* ngay trên màn hình mở khóa bằng trắc nghiệm 4 đáp án **cùng từ loại (`pos`)** và tự động nâng cấp sang **hỏi ngược Việt → Anh** hoặc **Nghe phát âm → Việt** khi từ đã quen (`repetitions >= 2`) để tránh nhận diện thụ động / học vẹt.
4. **Thuật toán SuperMemo SM-2 — TS. Piotr Woźniak (1990), kết hợp thời gian phản hồi nhận thức:**
   - Tính toán hệ số dễ nhớ (`Ease Factor` $\ge 1.3$), số lần lặp (`repetitions`) và khoảng cách ngày ôn (`intervalDays`) cho từng từ.
   - UnlockVocab cải tiến thêm bằng cách đo **thời gian phản hồi thực tế (`responseTimeMs`)**: trả lời đúng dưới `2,5 giây` (thuộc phản xạ nhanh → chất lượng `q = 5`) được giãn lịch xa hơn so với đúng nhưng ngập ngừng trên `6 giây` (`q = 3`).

---

## 3. Khảo sát thị trường & Điểm khác biệt của UnlockVocab

Trên Google Play hiện nay có một số app khai thác ý tưởng học từ trên màn hình khóa hoặc flashcard, nhưng chưa có ứng dụng nào kết hợp đầy đủ **Offline 100% + Miễn phí không quảng cáo + SM-2 theo thời gian phản hồi + Thoát nhanh 1 chạm + Dữ liệu mở cho giáo dục Việt Nam**:

| Tiêu chí so sánh | WordBit | Semper *(trước là UnlockYourBrain)* | AnkiDroid | Quizlet / MochiMochi | **UnlockVocab (Dự án này)** |
|---|---|---|---|---|---|
| **Tự bật câu hỏi khi mở khóa máy** | Có (màn hình khóa) | Có (chủ yếu câu đố ngắn) | Không (phải tự nhớ mở app) | Không (phải tự nhớ mở app) | **Có (bật ngay khi mở khóa Android 14)** |
| **Thoát nhanh 1 chạm mọi vùng ngoài đáp án** | Phải vuốt/bấm nút đóng nhỏ | Bắt giải mới vào máy / bấm skip | N/A | N/A | **Chạm bất kỳ chỗ nào ngoài 4 ô đáp án là tắt ngay** |
| **Miễn phí & Không quảng cáo** | Miễn phí có **quảng cáo**, bán gói Turbo bỏ QC | Có giới hạn / thu phí gói nội dung | Miễn phí, không QC | Thu phí gói Pro / có QC | **Miễn phí 100%, không quảng cáo** |
| **Hoạt động Offline 100%** | Offline phần lõi (tải gói) | Phụ thuộc tải gói | Offline 100% | Cần mạng / đồng bộ tài khoản | **Offline 100% (từ điển + IPA + TTS)** |
| **SM-2 theo tốc độ phản hồi (ms) + chống học vẹt** | SRS cơ bản theo mốc ngày | Theo độ khó thô | SM-2 (tự bấm đánh giá chủ quan) | SRS theo mốc cố định | **SM-2 tự động đo `ms` + đảo chiều Việt↔Anh / Nghe** |
| **Gợi ý từ điển Việt–Anh + Tự thêm nhiều nghĩa** | Đóng gói sẵn | Hạn chế | Tự tạo thủ công từ đầu | Cần gõ/tìm online | **Kho gợi ý 12.000 mục offline + Form chuẩn POS** |
| **Xuất dữ liệu CSV phục vụ nghiên cứu KHKT** | Không | Không | Hạn chế cho người mới | Không | **Có (xuất Log + Kho từ + Biểu đồ 7 ngày)** |

> **Nhận xét:** Các sản phẩm thương mại buộc phải gắn quảng cáo, khóa tính năng sau tường phí (paywall) hoặc bắt tạo tài khoản online để duy trì doanh thu. **UnlockVocab** giải bài toán này cho học sinh/sinh viên Việt Nam bằng dữ liệu nguồn mở hợp pháp (NGSL, CEFR-J, Từ điển TS. Hồ Ngọc Đức, `ipa-dict`) đóng gói gọn nhẹ offline hoàn toàn ngay trong app.

---

## 4. Giới thiệu hệ thống tính năng

1. **Thẻ trắc nghiệm nổi khi mở khóa (`WindowManager Overlay`):**
   - Hoạt động ổn định trên Android 14 / Samsung One UI 6 nhờ Foreground Service (`specialUse`) + `RECEIVER_EXPORTED` + bộ theo dõi `KeyguardManager`.
   - Tự động tránh bật khi đang có cuộc gọi điện thoại (`AudioManager`), trong **giờ im lặng ban đêm (23h–6h)**, khi đạt **Giới hạn câu/ngày**, hoặc khi người dùng bật **Tạm nghỉ 30 phút**.
   - **Thoát nhanh & Tạm nghỉ thông minh (Adaptive Snooze):** Chạm bất kỳ vùng nào ngoài 4 ô đáp án (và nút Nghe) → đóng ngay, ghi log `DISMISSED` không phạt điểm SM-2; nếu thoát nhanh `>= 2` lần liên tiếp trong vòng 10 phút (dấu hiệu đang bận), hệ thống tự tạm nghỉ 15 phút và hiển thị nút **Bật lại ngay** trên Trang chủ lẫn thông báo nền.
   - **Nhận diện Im lặng / Rung (Silent-aware TTS):** Tự động kiểm tra chế độ chuông, mức âm lượng Media và thiết bị tai nghe/Bluetooth; không tự phát âm thanh bất ngờ ở chế độ Im lặng/Rung trừ khi đang đeo tai nghe.
2. **Thuật toán SM-2 & Chống học vẹt:**
   - 3 đáp án nhiễu được lọc **cùng từ loại (`noun`, `verb`, `adj`, `adv`, `phrase`)**.
   - 4 chế độ câu hỏi: *Tự động theo SM-2* (mới học Anh→Việt; từ đã lặp $\ge 2$ tự nâng cấp sang Việt→Anh hoặc Luyện nghe phát âm → Việt khi không ở chế độ Im lặng), hoặc chọn cố định.
3. **Tùy biến kích hoạt & tiêu chí chọn từ linh hoạt:**
   - *Tần suất kích hoạt:* Mỗi lần mở khóa / Sau mỗi N lần mở khóa / Cách tối thiểu X phút / Giới hạn câu tối đa mỗi ngày (`0` = không giới hạn).
   - *5 tiêu chí chọn từ:* Đến hạn ôn (SM-2) / Từ mới / Từ hay sai nhất / Lọc theo từ loại hoặc CEFR (`A1–B2`, kèm cảnh báo trực quan khi bộ lọc có `0` từ phù hợp) / Ngẫu nhiên.
4. **Kho từ điển Offline 2 lớp (không cần Internet):**
   - **755 từ & cụm từ cốt lõi** khởi tạo sẵn (NGSL + CEFR-J + 55 cụm động từ/từ ghép thông dụng như `go to`, `look after`, `give up`...) + nút **`+ Nạp gói B1–B2`** mở rộng tức thì.
   - **12.000 mục từ gợi ý (`dict_entries`):** Gõ vào ô thêm từ sẽ gợi ý tự động kèm IPA, từ loại, nhiều nghĩa.
   - **Tự thêm & Sửa từ:** Khi không có sẵn, form tự thêm bắt buộc đủ **Từ vựng + Từ loại + Nghĩa chính (+ nhiều nghĩa phụ)**; hỗ trợ **Sửa** trực tiếp và **`+ Nhập nhanh`** nhiều dòng một lúc.
   - **Phát âm offline:** Dùng sẵn `TextToSpeech` (tiếng Anh) tích hợp trên Android.
5. **Thống kê & Xuất số liệu thực nghiệm KHKT:**
   - Theo dõi tỷ lệ nhớ (*Retention Rate*), tốc độ phản hồi trung bình (`ms`), số lượt thoát nhanh, thanh phân bố **4 mức độ ghi nhớ SM-2** (*Thuộc sâu ≥ 21 ngày / Mới nhớ 6–20 ngày / Đang học / Chưa học*), biểu đồ cột 7 ngày, Top 5 từ hay sai nhất.
   - Xuất file `unlockvocab_review_logs.csv` và `unlockvocab_study_words.csv` để phân tích số liệu nghiên cứu.

---

## 5. Hướng dẫn cài đặt & sử dụng

### Bước 1: Cài đặt & Cấp quyền lần đầu (rất quan trọng trên Android 14 / Samsung)
1. Mở app **UnlockVocab**, tại tab **Trang chủ**, kiểm tra thẻ **Quyền chạy nền**:
   - **Hiển thị trên ứng dụng khác (Overlay):** Bấm **Cấp quyền** → bật cho phép *UnlockVocab*.
   - **Thông báo dịch vụ nền:** Bấm **Cấp quyền** → chọn *Cho phép* (để dịch vụ chạy nền không bị hệ thống thu hồi).
   - **Tắt tối ưu pin:** Bấm **Mở cài đặt** → chọn *Cho phép / Không hạn chế* để Samsung One UI không đưa app vào chế độ ngủ sâu.
2. Đảm bảo công tắc **Bật câu hỏi khi mở khóa** đang ở trạng thái **Bật**.
3. Bấm **Test hiện câu hỏi Overlay ngay** để xem thử giao diện thẻ câu hỏi.

### Bước 2: Thao tác khi mở khóa màn hình hằng ngày
- Mỗi khi bạn mở khóa điện thoại, thẻ câu hỏi sẽ tự động hiện lên:
  - **Nếu rảnh (2–3 giây):** Bấm vào đáp án đúng (**A / B / C / D**) hoặc bấm **Nghe** để nghe phát âm. Thẻ sẽ báo Đúng/Sai kèm nghĩa mở rộng + ví dụ rồi tự đóng sau ~1 giây.
  - **Nếu đang bận:** Chạm vào **bất kỳ vùng trống/nền mờ nào bên ngoài 4 ô đáp án** → thẻ tắt ngay lập tức mà không bị trừ điểm SM-2.

### Bước 3: Tùy chỉnh chế độ học & Thêm từ vựng
- Tại **Trang chủ**: chọn **Tần suất kích hoạt** (mỗi lần / mỗi N lần / theo phút), chỉnh **Giới hạn câu/ngày**, bật **Giờ im lặng (23h–6h)**, bấm **Tạm nghỉ 30 phút** (hoặc **Tiếp tục ngay** trên app/thông báo nền) khi cần tập trung, chọn **Tiêu chí chọn từ** và **Kiểu câu hỏi (Chống học vẹt)**.
- Tại tab **Từ vựng**:
  - Tìm từ, lọc theo loại (`noun`, `verb`, `adj`, `adv`, `phrase`, `Tự thêm`).
  - Bấm **`+ Thêm từ`** và gõ từ cần học (kể cả từ ghép như `go to`) để chọn từ gợi ý offline, hoặc tự nhập đầy đủ *Từ vựng + Từ loại + Nghĩa chính + Nghĩa phụ*.
  - Bấm **`+ Nạp gói B1–B2`** để mở rộng thêm 100 từ vựng nâng cao từ kho từ điển offline, hoặc **`+ Nhập nhanh`** để dán danh sách nhiều từ.
- Tại tab **Thống kê**: xem biểu đồ 7 ngày, các từ hay sai nhất, và bấm **Xuất log CSV** / **Xuất kho từ CSV** khi cần lấy dữ liệu làm báo cáo.

---

## 6. Nguồn dữ liệu mở & Tài liệu tham khảo khoa học

### Nguồn dữ liệu từ điển (Offline, hợp lệ bản quyền)
- **Từ điển Anh–Việt:** TS. Hồ Ngọc Đức — *Free Vietnamese Dictionary Project (FVDP / OVDP)* (Giấy phép GPL).
- **Danh sách từ tần suất cốt lõi:** *New General Service List (NGSL 1.01)* — Charles Browne, Brent Culligan & Joseph Phillips (CC BY-SA).
- **Phân tầng cấp độ CEFR:** *CEFR-J Vocabulary Profile* (`openlanguageprofiles/olp-en-cefrj`, Yukio Tono — CC BY-SA 4.0).
- **Phiên âm IPA:** `open-dict-data/ipa-dict` (`en_US`, Giấy phép MIT).

### Tài liệu tham khảo khoa học
1. Ebbinghaus, H. (1885). *Über das Gedächtnis* (Memory: A Contribution to Experimental Psychology).
2. Woźniak, P. A. (1990). *Optimization of learning* (SuperMemo SM-2 Algorithm).
3. Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., & Rohrer, D. (2006). *Distributed practice in verbal recall tasks: A review and quantitative synthesis*. Psychological Bulletin, 132(3), 354–380.
4. Roediger, H. L., III, & Karpicke, J. D. (2006). *Test-enhanced learning: Taking memory tests improves long-term retention*. Psychological Science, 17(3), 249–255.
5. Fogg, B. J. (2009). *A behavior model for persuasive design*. Proceedings of the 4th International Conference on Persuasive Technology (Persuasive '09).
