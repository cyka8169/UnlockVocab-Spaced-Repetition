# Giới thiệu Dự án UnlockVocab — Học Từ Vựng Tiếng Anh Khi Mở Khóa Màn Hình Bằng Spaced Repetition

> **Lĩnh vực dự thi:** Khoa học Kỹ thuật (KHKT) & Hội thi Tin học trẻ bảng D (Phần mềm sáng tạo)  
> **Nền tảng:** Android Native (Kotlin + Jetpack Compose + SQLite 2 lớp, Offline 100%)  
> **Phiên bản phát hành:** `v1.5.3-khkt` (Release R8, `4,02 MiB` / `4.219.793 bytes` • SHA-256 tệp APK: `A6D9A3782C1BC7848CFA2AAF83BA06F449AFF06659AA2459002E1690D0CCBF80` • SHA-256 chứng thư ký: `325F191DA7D70180`)  
> **Nhóm tác giả:**  
> - **Trần Tiến Huy** (`huyerror666@gmail.com`) — Kiến trúc hệ thống, thiết kế kỹ thuật, lập trình Android Native (Kotlin, Jetpack Compose, SQLite 2 lớp, Foreground Service/Overlay, bảo mật R8) & kiểm thử đóng gói sản phẩm.  
> - **Nguyễn Minh Thư** (`nguyenminhthu20071982@gmail.com`) — Đóng góp ý tưởng và góp ý hoàn thiện bộ dữ liệu từ điển, phương pháp Spaced Repetition cùng các tính năng hỗ trợ trải nghiệm người học.

---

## 1. Lý do làm dự án (Từ Vấn đề thực tiễn → Giải pháp)

### 1.1. Vấn đề thực tiễn: "Hội chứng bỏ cuộc sau 2 tuần"
Theo quan sát thực tiễn phổ biến về hành vi bỏ cuộc sớm ở người tự học, hầu hết người học tiếng Anh khi bắt đầu đều có động lực rất cao: tải nhiều app flashcard, tìm trang web, sưu tầm bộ từ vựng... Nhưng chỉ sau khoảng **2 tuần đầu**, phần lớn đều **bỏ dở gần như hoàn toàn**.

**Nguyên nhân gốc rễ theo tâm lý học hành vi (Fogg Behavior Model):**
Theo **Mô hình Hành vi Fogg** của GS. **B.J. Fogg** (Đại học Stanford, 2009), một hành vi $B$ chỉ xảy ra khi hội tụ đủ 3 yếu tố cùng lúc:
$$B = M \times A \times T$$
*(Trong đó: **Motivation** = Động lực, **Ability** = Khả năng thực hiện / độ dễ, **Trigger** = Tác nhân kích hoạt).*

- Ở **2 tuần đầu**, động lực ($M$) rất cao nên người học chịu khó nhớ giờ học, tự tìm icon app, mở app lên ôn tập (dù thao tác rườm rà — độ dễ $A$ thấp).
- Sau ~2 tuần, động lực ($M$) giảm xuống mức bình thường. Lúc này chỉ một **ma sát nhỏ** (phải tự nhớ, tự mở app, chờ tải mạng, bị làm phiền bởi quảng cáo) cũng đủ khiến $B$ không xảy ra → **chuỗi thói quen đứt gãy**.

### 1.2. Giải pháp: "Không tự giác thì bị giác" (Micro-habit gắn vào thao tác mở khóa)
Các nghiên cứu **Tương tác Người – Máy (HCI)** cho thấy trung bình một người tương tác với điện thoại khoảng **57 – 80+ lần mỗi ngày** (lên tới 80–110 lần sáng màn hình trong các báo cáo ngành của Apple và Locket), trong đó khoảng **43% – 46%** là thao tác mở khóa hoàn toàn vào sử dụng máy *(Hintze et al., 2014, 2017; Dearman & Truong, 2012)*.

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

| Tiêu chí so sánh | WordBit | Semper *(UnlockYourBrain — đã ngừng phát triển)* | AnkiDroid | Quizlet / MochiMochi | **UnlockVocab (Dự án này)** |
|---|---|---|---|---|---|
| **Tự bật câu hỏi khi mở khóa máy** | Có (màn hình khóa) | Có (chủ yếu câu đố ngắn) | Không (phải tự nhớ mở app) | Không (phải tự nhớ mở app) | **Có (bật ngay khi mở khóa Android 14)** |
| **Thoát nhanh 1 chạm mọi vùng ngoài đáp án** | Phải vuốt/bấm nút đóng nhỏ | Bắt giải mới vào máy / bấm skip | N/A | N/A | **Chạm bất kỳ chỗ nào ngoài 4 ô đáp án là tắt ngay** |
| **Miễn phí & Không quảng cáo** | Miễn phí có **quảng cáo**, bán gói Turbo bỏ QC | Có giới hạn / thu phí gói nội dung | Miễn phí, không QC | Thu phí gói Pro / có QC | **Miễn phí 100%, không quảng cáo** |
| **Hoạt động Offline 100%** | Offline phần lõi (tải gói) | Phụ thuộc tải gói | Offline 100% | Cần mạng / đồng bộ tài khoản | **Offline 100% (từ điển + IPA + TTS)** |
| **SM-2 theo tốc độ phản hồi (ms) + chống học vẹt** | SRS cơ bản theo mốc ngày | Theo độ khó thô | SM-2 (tự bấm đánh giá chủ quan) | SRS theo mốc cố định | **SM-2 tự động đo `ms` + đảo chiều Việt↔Anh / Nghe** |
| **Gợi ý từ điển Việt–Anh + Tự thêm nhiều nghĩa** | Đóng gói sẵn | Hạn chế | Tự tạo thủ công từ đầu | Cần gõ/tìm online | **SQLite 50.000 mục offline (A1–C2) + Form chuẩn POS** |
| **Xuất dữ liệu CSV phục vụ nghiên cứu KHKT** | Không | Không | Hạn chế cho người mới | Không | **Có (xuất Log + Kho từ + Biểu đồ 7 ngày)** |

> **Nhận xét:** Các sản phẩm thương mại buộc phải gắn quảng cáo, khóa tính năng sau tường phí (paywall) hoặc bắt tạo tài khoản online để duy trì doanh thu. **UnlockVocab** giải bài toán này cho học sinh/sinh viên Việt Nam bằng dữ liệu nguồn mở hợp pháp (NGSL, CEFR-J, Từ điển TS. Hồ Ngọc Đức, `ipa-dict`) đóng gói gọn nhẹ offline hoàn toàn ngay trong app.

---

## 4. Giới thiệu hệ thống tính năng

1. **Thẻ trắc nghiệm nổi khi mở khóa (`WindowManager Overlay`):**
   - Hoạt động ổn định trên Android 14 / Samsung One UI 6 nhờ Foreground Service (`specialUse`) + `RECEIVER_EXPORTED` + bộ theo dõi `KeyguardManager`.
   - Tự động tránh bật khi đang có cuộc gọi điện thoại (`AudioManager`), trong **giờ im lặng ban đêm (23h–6h)**, khi đạt **Giới hạn câu/ngày**, hoặc khi người dùng bật **Tạm nghỉ 30 phút**.
   - **Thoát nhanh & Tạm nghỉ thông minh (Adaptive Snooze):** Chạm bất kỳ vùng nào ngoài 4 ô đáp án (và nút Nghe) → đóng ngay, ghi log `DISMISSED` không phạt điểm SM-2; nếu thoát nhanh `>= 2` lần liên tiếp trong vòng 10 phút (dấu hiệu đang bận), hệ thống tự tạm nghỉ 15 phút và hiển thị nút **Bật lại ngay** trên Trang chủ lẫn thông báo nền.
   - **Nhận diện Im lặng / Rung (Silent-aware TTS):** Tự động kiểm tra chế độ chuông, mức âm lượng Media và thiết bị tai nghe/Bluetooth; không tự phát âm thanh bất ngờ ở chế độ Im lặng/Rung trừ khi đang đeo tai nghe.
2. **Thuật toán SM-2, Đáp án nhiễu thông minh & Chống học vẹt (5 chế độ):**
   - 3 đáp án nhiễu được lọc ưu tiên **cùng từ loại (`noun`, `verb`, `adj`, `adv`, `phrase`) + cùng cấp độ CEFR**, đồng thời tự loại các đáp án trùng từ khóa tiếng Việt với từ đang hỏi.
   - 5 chế độ câu hỏi: *Tự động theo SM-2* (mới học: Anh→Việt; lặp $\ge 2$: Việt→Anh hoặc Luyện nghe → Việt; lặp $\ge 3$: **Điền từ ngữ cảnh 1 dòng — Cloze `____`**), hoặc chọn cố định từng chế độ.
3. **Chọn trình độ cá nhân (`A1` – `C2`) & Tùy biến kích hoạt linh hoạt:**
   - *Chọn trình độ của bạn (`A1`, `A2`, `B1`, `B2`, `C1`, `C2`):* Tự động ưu tiên từ mới đúng trình độ khi mở khóa và hỗ trợ nạp nhanh +50 từ theo trình độ ngay trên Trang chủ.
   - *Tần suất kích hoạt:* Mỗi lần mở khóa / Sau mỗi N lần mở khóa / Cách tối thiểu X phút / Giới hạn câu tối đa mỗi ngày (`0` = không giới hạn).
   - *5 tiêu chí chọn từ:* Đến hạn ôn (SM-2) / Từ mới / Từ hay sai nhất / Lọc theo từ loại hoặc CEFR (`A1–C2`, kèm cảnh báo trực quan khi bộ lọc có `0` từ phù hợp) / Ngẫu nhiên.
4. **Kho từ điển SQLite Offline 2 lớp (`offline_dict.db` — không cần Internet):**
   - **855 từ & cụm từ cốt lõi** khởi tạo sẵn bao phủ đủ **6 cấp độ `A1` – `C2`** (NGSL + CEFR-J A1–B2 + Octanove C1–C2 + 55 cụm động từ/từ ghép thông dụng như `go to`, `look after`, `give up`...).
   - **Tab Từ điển Offline (50.000 mục & 4.500 cụm từ / từ ghép):** Tra cứu 2 chiều Anh ↔ Việt tức thì (từ đơn, từ ghép & cụm từ), lọc theo Từ loại (`noun`, `verb`, `adj`, `adv`, `phrase`) và CEFR (`A1`–`C2`), xem chi tiết nghĩa đánh số + câu ví dụ + cụm từ liên quan, nghe phát âm `TextToSpeech` và **bấm `+ Học` 1 chạm** đưa thẳng vào kho ôn tập mở khóa.
   - **Nạp thêm & Xoá bớt từ theo cấp độ tự chọn (`A1` – `C2`):** Nút **`+ Nạp theo cấp`** và **`- Xoá bớt từ`** cho phép người dùng tự chọn 1 hoặc nhiều cấp độ bất kỳ từ `A1` đến `C2`, lọc theo từ loại và số lượng (`25 / 50 / 100 / 200` hoặc xoá hết cấp) để tinh gọn kho học (ưu tiên xoá từ chưa học, **bảo vệ 100% từ tự thêm**).
   - **Tự thêm & Sửa từ:** Khi không có sẵn, form tự thêm bắt buộc đủ **Từ vựng + Từ loại + Nghĩa chính (+ nhiều nghĩa phụ)**; hỗ trợ **Sửa** trực tiếp và **`+ Nhập nhanh`** nhiều dòng một lúc.
   - **Phát âm offline:** Dùng sẵn `TextToSpeech` (tiếng Anh) tích hợp trên Android.
5. **Thống kê, Đối chiếu Ebbinghaus & Đo lường Pre-test / Post-test KHKT:**
   - Theo dõi tỷ lệ nhớ (*Retention Rate*), tốc độ phản hồi trung bình (`ms`), số lượt thoát nhanh, thanh phân bố **4 mức độ ghi nhớ SM-2** (*Thuộc sâu ≥ 21 ngày / Mới nhớ 6–20 ngày / Đang học / Chưa học*), biểu đồ cột 7 ngày, Top 5 từ hay sai nhất.
   - **Bảng đối chiếu Đường cong quên Ebbinghaus (`D1 / D3 / D7 / D21`):** So sánh tỷ lệ nhớ thực tế của người học theo từng chu kỳ SM-2 với bộ mốc tham chiếu nội suy gần đúng (`33% → 25% → 23% → 21%`) từ dữ liệu *savings* gốc năm 1885 của Ebbinghaus (`33,7%` sau 1 ngày, `27,8%` sau 2 ngày, `25,4%` sau 6 ngày, `21,1%` sau 31 ngày).
   - **Đo lường Pre-test / Post-test 15 câu offline & Bộ Tự kiểm thử (`CoreSelfTester`):** Cho phép thí sinh làm bài test đầu vào và đầu ra ngay trong app để đo mức tăng trưởng, chạy **11 bài test tự động** thuật toán SM-2/CSDL/bản quyền, và xuất 3 tệp `unlockvocab_review_logs.csv`, `unlockvocab_study_words.csv`, `unlockvocab_pre_post_tests.csv`.
6. **Chống đánh cắp & Xác thực bản quyền tác giả (5 lớp bảo vệ đa tầng gia cố R8):**
   - **Lớp 1 — Đóng gói Release R8 toàn phần & Ký số riêng (`app-release.apk` / `UnlockVocab.apk`):** Làm rối toàn bộ mã nguồn bao gồm cả các gói lõi `data`, `domain`, `security` và rút gọn tài nguyên (`isMinifyEnabled = true`, `isShrinkResources = true`, dung lượng `~4,0 MiB` / `4.219.793 bytes`), ký số bằng chứng thư SHA-256 riêng (`325F191DA7D70180`; khóa ký `.jks` giữ bí mật tuyệt đối, không đưa lên mã nguồn công khai).
   - **Lớp 2 — Thủy vân trong CSDL SQLite (`_author_meta`):** Nhúng bảng siêu dữ liệu bản quyền (`origin_signature` + `package_id` + thông tin 2 đồng tác giả) ngay trong `offline_dict.db` để chứng minh nguồn gốc bộ từ điển biên soạn.
   - **Lớp 3 — Xác thực & Cưỡng chế lúc chạy (`OwnershipGuard`):** Dùng mã hóa XOR phụ thuộc vị trí để kiểm tra tên gói gốc `com.huy.unlockvocab`, đối chiếu chứng thư APK SHA-256 (bản Release chỉ chấp nhận khóa Release và chặn cờ `FLAG_DEBUGGABLE`) và thủy vân SQLite; nếu phát hiện giả mạo, dịch vụ `UnlockService` từ chối bật thẻ Overlay.
   - **Lớp 4 — Hiển thị & Cảnh báo trên Trang chủ:** Thẻ **"Bản quyền & Xác thực tác giả"** công khai nhóm tác giả (**Trần Tiến Huy** & **Nguyễn Minh Thư**), mã SHA-256 chứng thư rút gọn, ghi công giấy phép dữ liệu mở và bật cảnh báo đỏ ngay lập tức nếu ứng dụng bị biên dịch lại trái phép bằng khóa lạ hoặc đổi package name.
   - **Lớp 5 — Thủy vân trong tệp CSV đối chứng (`origin_watermark`):** Tự động gắn cột `origin_watermark` vào cả 3 file CSV xuất ra (và đánh dấu `TAMPERED_UNAUTHORIZED` nếu tệp được xuất từ APK giả mạo).

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
  - **Nếu rảnh (2–3 giây):** Bấm vào đáp án (**A / B / C / D**) hoặc bấm **Nghe** để nghe phát âm. Thẻ sẽ báo Đúng/Sai, **giữ nguyên trên màn hình** (không tự tắt) để bạn đọc kỹ hộp giải thích (từ loại, IPA, nghĩa chính/phụ, câu ví dụ) và nghe lại phát âm; khi đọc xong chỉ cần **chạm vào vùng tối bên ngoài thẻ** (hoặc bấm Quay lại) để đóng.
  - **Nếu đang bận (chưa trả lời):** Chạm vào **bất kỳ vùng trống/nền mờ nào bên ngoài 4 ô đáp án** → thẻ tắt ngay lập tức mà không bị trừ điểm SM-2.

### Bước 3: Tùy chỉnh chế độ học & Thêm từ vựng
- Tại **Trang chủ**: chọn **Trình độ của bạn (`A1` – `C2`)** (hoặc bấm `+ Nạp thêm 50 từ` của trình độ đó), chọn **Tần suất kích hoạt**, chỉnh **Giới hạn câu/ngày**, bật **Giờ im lặng (23h–6h)**, bấm **Tạm nghỉ 30 phút** khi cần tập trung, chọn **Tiêu chí chọn từ** và **Kiểu câu hỏi (Chống học vẹt, gồm cả Điền từ ngữ cảnh Cloze)**.
- Tại tab **Từ vựng**:
  - Tìm từ, lọc theo loại (`noun`, `verb`, `adj`, `adv`, `phrase`, `Tự thêm`) hoặc theo cấp độ **`A1`, `A2`, `B1`, `B2`, `C1`, `C2`**.
  - Bấm **`+ Thêm từ`** và gõ từ cần học (kể cả từ ghép như `go to`) để chọn từ gợi ý 50.000 mục offline, hoặc tự nhập đầy đủ *Từ vựng + Từ loại + Cấp độ CEFR + Nghĩa chính + Nghĩa phụ*.
  - Bấm **`+ Nạp theo cấp`** hoặc **`- Xoá bớt từ`** để tự chọn cấp độ (`A1` đến `C2`), từ loại và số lượng muốn nạp thêm hoặc xoá bớt khỏi kho học (toàn bộ từ *Tự thêm* luôn được bảo vệ giữ nguyên); hoặc bấm **`+ Nhập nhanh`** để dán danh sách nhiều từ.
- Tại tab **Từ điển**: tra cứu Anh ↔ Việt trên 50.000 mục từ & 4.500 cụm từ / từ ghép offline, lọc theo Từ loại/CEFR, bấm **Nghe** để phát âm, chạm vào từng mục để xem chi tiết nghĩa đánh số + cụm từ liên quan và bấm **`+ Học`** để đưa từ vừa tra vào kho học ngay.
- Tại tab **Thống kê**: xem bảng đối chiếu **Ebbinghaus (`D1–D21`)**, biểu đồ 7 ngày, các từ hay sai nhất; bấm **Làm Pre-test / Làm Post-test (15 câu)** để đo tăng trưởng học tập, bấm **Tự kiểm thử lõi** để xác nhận **11 test bất biến**, và bấm **Xuất log CSV** / **Xuất kho từ CSV** / **Xuất KHKT Test CSV** khi làm báo cáo.
- Tại cuối **Trang chủ**: xem thẻ **Bản quyền & Xác thực tác giả** để xác nhận đúng bản chính chủ (`Chính chủ • Đã xác thực SQLite & APK`).

---

## 6. Nguồn dữ liệu mở & Tài liệu tham khảo khoa học

### Nguồn dữ liệu từ điển (Offline, hợp lệ bản quyền)
- **Từ điển Anh–Việt:** TS. Hồ Ngọc Đức — *Free Vietnamese Dictionary Project (FVDP / OVDP)* (Giấy phép GNU GPL).
- **Danh sách từ tần suất cốt lõi:** *New General Service List (NGSL 1.01)* — Charles Browne, Brent Culligan & Joseph Phillips (Giấy phép CC BY-SA 4.0).
- **Phân tầng cấp độ CEFR:** *CEFR-J Vocabulary Profile* (`Tono Lab / TUFS` — miễn phí cho nghiên cứu & thương mại kèm trích dẫn) và phần mở rộng *Octanove Vocabulary Profile C1–C2* (`CC BY-SA 4.0`) qua `openlanguageprofiles/olp-en-cefrj`.
- **Phiên âm IPA:** `open-dict-data/ipa-dict` (`en_US`, Giấy phép MIT).

> **Minh bạch tách bạch bản quyền ([LICENSE](../LICENSE)):** Kho dữ liệu từ điển mở và script tái tạo ([build_offline_data.py](../scripts/build_offline_data.py)) tuân thủ đầy đủ giấy phép nguồn mở **GPL / CC BY-SA 4.0 / MIT**; mã nguồn ứng dụng Android và tài liệu dự thi thuộc quyền đồng tác giả của **Trần Tiến Huy** (`huyerror666@gmail.com`) và **Nguyễn Minh Thư** (`nguyenminhthu20071982@gmail.com`).

### Tài liệu tham khảo khoa học
1. Ebbinghaus, H. (1885). *Über das Gedächtnis: Untersuchungen zur experimentellen Psychologie* (Bản dịch tiếng Anh: Ruger, H. A., & Bussenius, C. E., 1913).
2. Woźniak, P. A. (1990). *Optimization of learning* (SuperMemo SM-2 Algorithm, Poznań University of Technology).
3. Cepeda, N. J., Pashler, H., Vul, E., Wixted, J. T., & Rohrer, D. (2006). *Distributed practice in verbal recall tasks: A review and quantitative synthesis*. Psychological Bulletin, 132(3), 354–380. DOI: `10.1037/0033-2909.132.3.354`.
4. Roediger, H. L., III, & Karpicke, J. D. (2006). *Test-enhanced learning: Taking memory tests improves long-term retention*. Psychological Science, 17(3), 249–255. DOI: `10.1111/j.1467-9280.2006.01693.x`.
5. Fogg, B. J. (2009). *A behavior model for persuasive design*. Proceedings of the 4th International Conference on Persuasive Technology (PERSUASIVE '09). DOI: `10.1145/1541948.1541999`.
6. Hintze, D., Findling, R. D., Muaaz, M., Scholz, S., & Mayrhofer, R. (2014). *Diversity in locked and unlocked mobile device usage*. ACM UbiComp '14 Adjunct. DOI: `10.1145/2638728.2641697`.
7. Hintze, D., Hintze, P., Findling, R. D., & Mayrhofer, R. (2017). *A large-scale, long-term analysis of mobile device usage characteristics*. Proceedings of the ACM on Interactive, Mobile, Wearable and Ubiquitous Technologies (IMWUT), 1(2), Article 13. DOI: `10.1145/3090078`.
8. Dearman, D., & Truong, K. N. (2012). *Evaluating the implicit acquisition of second language vocabulary using a live wallpaper*. ACM CHI 2012.
