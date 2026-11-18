# Trạng thái Giai đoạn Hiện Tại — UnlockVocab

> **Cập nhật gần nhất:** 2026-11-18 | **Mốc Git:** `v0.2-stage3-complete` (nhánh `main`)

## Đang ở giai đoạn nào?
Dự án đã **HOÀN THÀNH 100% Giai đoạn 0, Giai đoạn 1, Giai đoạn 2 và Giai đoạn 3**.

Hiện tại chúng ta đã chính thức bước vào **Giai đoạn 4 (~40% hoàn thành — Đã có đủ biểu đồ & công cụ thống kê, còn chạy thực nghiệm nhiều ngày & đóng gói thi KHKT / Tin học trẻ bảng D)**.

*(Xem bảng chi tiết toàn bộ 5 giai đoạn tại [docs/STAGES.md](docs/STAGES.md) và nhật ký chi tiết tại [docs/PROGRESS.md](docs/PROGRESS.md).)*

---

## Những gì đã chạy ổn định trên máy thật (`SM_A047F` / `R58TA1ESSZN`)
- **Offline 100%:** 700 từ cốt lõi (NGSL + CEFR-J A1–B2) + 9.500 mục từ gợi ý offline (Hồ Ngọc Đức + ipa-dict).
- **Mở khóa màn hình tự bật câu hỏi:** Hoạt động tin cậy trên Android 14 / Samsung One UI 6 (`ACTION_USER_PRESENT` + `KEYGUARD_UNLOCKED`).
- **Thoát nhanh 1 chạm:** Chạm vào bất kỳ vùng nào ngoài 4 ô đáp án là đóng câu hỏi ngay, ghi `Thoát nhanh (DISMISSED)` không phạt SM-2.
- **Chống học vẹt (`QuizQuestionStyle`):** Hỗ trợ tự động nâng cấp theo SM-2 (mới học: Anh→Việt; lặp `>= 2`: Việt→Anh cùng từ loại hoặc Luyện nghe phát âm → Việt), hoặc chọn cố định từng kiểu.
- **Quản lý từ vựng đầy đủ:** Gợi ý tự động offline, Form tự thêm bắt buộc Từ vựng + Từ loại + Nghĩa chính (+ nhiều nghĩa phụ), **Sửa** từ trực tiếp, **`+ Nạp gói B1–B2`**, và **`+ Nhập nhanh`** nhiều từ hàng loạt.
- **Thống kê & Biểu đồ KHKT:** Phân bố mức độ thuộc SM-2, biểu đồ cột 7 ngày gần nhất, Top từ hay sai nhất, xuất cả lịch sử ôn (`unlockvocab_review_logs.csv`) lẫn kho từ (`unlockvocab_study_words.csv`).

---

## Các việc còn lại của Giai đoạn 4 (Chuẩn bị đi thi)
1. **Chạy thực nghiệm dài ngày trên Samsung A04s (`SM_A047F`):** Dùng thật hàng ngày (và so sánh nhóm đối chứng nếu cần cho KHKT), theo dõi pin và độ ổn định qua đêm.
2. **Xuất dữ liệu thực nghiệm CSV & vẽ biểu đồ báo cáo:** Thu thập file CSV log ôn tập làm minh chứng khoa học cho báo cáo KHKT & Tin học trẻ bảng D.
3. **Đóng gói APK Release & Quay video demo:** Build bản ký số (`assembleRelease`) và quay màn hình demo thao tác thực tế.
