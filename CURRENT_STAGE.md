# Trạng thái Giai đoạn Hiện Tại — UnlockVocab

> **Cập nhật gần nhất:** 2026-11-18 | **Mốc Git:** `v1.5-anti-theft` (nhánh `main`)

## Đang ở giai đoạn nào?
Dự án đã **HOÀN THÀNH 100% Giai đoạn 0, Giai đoạn 1, Giai đoạn 2 và Giai đoạn 3**.

Hiện tại chúng ta đã đạt **~98% Giai đoạn 4 (Đã hoàn thành trọn vẹn Gói A + Gói B + Gói C nâng tầm KHKT, tab Từ điển Offline 50.000 mục, Gói Chống đánh cắp & Xác thực bản quyền 5 lớp + Release APK R8 ký số riêng, bộ Tự kiểm thử lõi 11 test, tài liệu QA Agent, bộ hồ sơ `.docx` & file cài `UnlockVocab.apk`, đồng bộ mã nguồn lên GitHub Private — chỉ còn chạy thu thập số liệu nhiều ngày & quay demo đi thi)**.

*(Xem bảng chi tiết toàn bộ 5 giai đoạn tại [docs/STAGES.md](docs/STAGES.md) và nhật ký chi tiết tại [docs/PROGRESS.md](docs/PROGRESS.md).)*

---

## Những gì đã chạy ổn định trên máy thật (`SM_A047F` / `R58TA1ESSZN`)
- **Offline 100% với SQLite đóng gói sẵn (`offline_dict.db`):** 855 từ/cụm từ cốt lõi khởi tạo sẵn bao phủ cả 6 cấp độ **`A1` – `C2`** (NGSL + CEFR-J A1–B2 + Octanove C1–C2 + 55 cụm từ/từ ghép thông dụng như `go to`, `look after`...) + **50.000 mục từ & 4.500 cụm từ** trong cơ sở dữ liệu SQLite `offline_dict.db` (`6.72 MB`) khởi động tức thì.
- **Tab Từ điển Offline (50.000 mục Anh ↔ Việt):** Tra cứu tức thì cả tiếng Anh lẫn nghĩa tiếng Việt, lọc theo từ loại (`noun`, `verb`, `adj`, `adv`, `phrase`) và cấp độ (`A1`–`C2`), chạm xem chi tiết nghĩa đánh số + câu ví dụ + cụm từ liên quan (`look` → `look at`, `look in`...), nghe phát âm `TextToSpeech` và bấm **`+ Học` 1 chạm** đưa thẳng vào kho ôn mở khóa.
- **Chọn trình độ cá nhân (`A1` – `C2`) & Nạp/Xoá từ theo cấp độ tự chọn:** Chọn trình độ hiện tại (`A1`, `A2`, `B1`, `B2`, `C1`, `C2`) ngay trên Trang chủ để ưu tiên từ mới đúng trình độ; hộp thoại trên tab Từ vựng cho phép **`+ Nạp theo cấp`** và **`- Xoá bớt từ`** theo 1 hoặc nhiều cấp độ bất kỳ từ `A1` đến `C2`, lọc theo từ loại và số lượng (`25 / 50 / 100 / 200` hoặc xoá hết cấp — ưu tiên xoá từ chưa học trước và **giữ nguyên 100% các từ tự thêm**).
- **Mở khóa màn hình tự bật câu hỏi:** Hoạt động tin cậy trên Android 14 / Samsung One UI 6 (`ACTION_USER_PRESENT` + `KEYGUARD_UNLOCKED`).
- **Thoát nhanh 1 chạm & Tạm nghỉ thông minh (Adaptive Snooze):** Chạm vào bất kỳ vùng nào ngoài 4 ô đáp án là đóng câu hỏi ngay không phạt SM-2; thoát nhanh `>= 2` lần liên tiếp trong 10 phút tự tạm nghỉ 15 phút; kèm nút **Tạm nghỉ 30 phút / Bật lại ngay** trên Trang chủ và thanh thông báo nền, và stepper **Giới hạn/ngày**.
- **Nhận diện Im lặng / Rung (Silent-aware TTS):** Tự động kiểm tra chế độ chuông, âm lượng Media và tai nghe/Bluetooth; không tự phát âm thanh bất ngờ khi điện thoại đang Im lặng/Rung trừ khi đeo tai nghe.
- **Chống học vẹt 5 chế độ & Đáp án nhiễu thông minh (`QuizQuestionStyle`):** 3 đáp án nhiễu ưu tiên **cùng từ loại (`pos`) + cùng cấp độ CEFR**, loại trừ nghĩa trùng từ khóa; hỗ trợ tự động nâng cấp theo SM-2 (mới học: Anh→Việt; lặp `>= 2`: Việt→Anh hoặc Luyện nghe → Việt; lặp `>= 3`: **Điền từ ngữ cảnh 1 dòng — Cloze `____`**), hoặc chọn cố định từng kiểu.
- **Quản lý từ vựng đầy đủ & Responsive 360dp:** Hiển thị đủ toàn bộ kho từ, lọc theo từ loại (`noun`, `verb`, `adj`, `adv`, `phrase`, `Tự thêm`) và cấp độ (`A1`..`C2`), cảnh báo rõ ngay trên Trang chủ khi bộ lọc `BY_TOPIC_OR_POS` có `0` từ phù hợp, gợi ý & tự thêm cả từ ghép (`"go to"`), **Sửa** từ, **`+ Nạp theo cấp`**, **`- Xoá bớt từ`**, **`+ Nhập nhanh`**, nút trên thẻ từ và hàng thao tác nằm gọn trên 1 dòng không bị xuống dòng.
- **Thống kê, Đối chiếu Ebbinghaus & Pre/Post-test KHKT (Gói C):** Phân bố **4 mức độ thuộc SM-2** (*Thuộc sâu ≥21d / Mới nhớ 6–20d / Đang học / Chưa học*), **bảng đối chiếu Đường cong quên Ebbinghaus (`D1–D21`)**, **Pre-test / Post-test 15 câu offline** đo tăng trưởng (`+X%`), biểu đồ cột 7 ngày gần nhất, Top từ hay sai nhất, bộ **Tự kiểm thử lõi (`CoreSelfTester` — 11/11 PASS)** và xuất 3 file CSV (`unlockvocab_review_logs.csv`, `unlockvocab_study_words.csv`, `unlockvocab_pre_post_tests.csv`).
- **Chống đánh cắp & Xác thực bản quyền tác giả (5 lớp — `v1.5-anti-theft`):** Ký số **Release R8 APK** (`app-release.apk`, `4,236,141 bytes`, `v1.5.0-khkt`) bằng khóa riêng (`keystore/` được `.gitignore` bảo vệ), nhúng thủy vân `_author_meta` trong `offline_dict.db` (`DB_VERSION = 4`), kiểm tra chéo tên gói + chứng thư SHA-256 + SQLite qua `OwnershipGuard.kt`, hiển thị thẻ **"Bản quyền & Xác thực tác giả"** (`Chính chủ • Đã xác thực SQLite & APK`) ở cuối `HomeScreen`, gắn `origin_watermark` vào 3 tệp CSV và kiểm chứng ở test #11 (`ANTI_THEFT_OWNERSHIP_GUARD`).
- **Tài liệu QA cho AI Agent & Bộ tệp bàn giao:** Đã tạo [AGENT_TESTING_GUIDE.md](docs/AGENT_TESTING_GUIDE.md) (`TC-01` → `TC-08` + 11 test `CoreSelfTester`), cập nhật [README.md](README.md), [GIOI_THIEU_DU_AN.md](docs/GIOI_THIEU_DU_AN.md) và 3 tệp bàn giao ở thư mục gốc: [Gioi_Thieu_San_Pham_UnlockVocab.docx](Gioi_Thieu_San_Pham_UnlockVocab.docx) (`42,935 bytes`), [Huong_Dan_Su_Dung_UnlockVocab.docx](Huong_Dan_Su_Dung_UnlockVocab.docx) (`42,224 bytes`) và `UnlockVocab.apk` (`14,589,798 bytes`).

---

## Các việc còn lại của Giai đoạn 4 (Chuẩn bị đi thi)
1. **Chạy thực nghiệm dài ngày trên Samsung A04s (`SM_A047F`):** Dùng thật hàng ngày (và so sánh nhóm đối chứng nếu cần cho KHKT), theo dõi pin và độ ổn định qua đêm.
2. **Xuất dữ liệu thực nghiệm CSV & vẽ biểu đồ báo cáo:** Thu thập file CSV log ôn tập làm minh chứng khoa học cho báo cáo KHKT & Tin học trẻ bảng D.
3. **Quay video demo dự thi:** Quay màn hình thao tác thực tế mở khóa, thoát nhanh, tra từ điển 50k và thống kê KHKT (bản Release ký số R8 đã build sẵn sàng tại `app\build\outputs\apk\release\app-release.apk`).
