# Trạng thái Giai đoạn Hiện Tại — UnlockVocab

> **Cập nhật gần nhất:** 2026-11-18 | **Mốc Git:** `v1.0-deliverables` (nhánh `main`)

## Đang ở giai đoạn nào?
Dự án đã **HOÀN THÀNH 100% Giai đoạn 0, Giai đoạn 1, Giai đoạn 2 và Giai đoạn 3**.

Hiện tại chúng ta đã đạt **~75% Giai đoạn 4 (Đã hoàn thiện bộ hồ sơ `.docx` thuyết minh + hướng dẫn cài APK, file cài `UnlockVocab.apk`, biểu đồ & CSV — còn chạy thực nghiệm nhiều ngày & quay demo thi KHKT / Tin học trẻ bảng D)**.

*(Xem bảng chi tiết toàn bộ 5 giai đoạn tại [docs/STAGES.md](docs/STAGES.md) và nhật ký chi tiết tại [docs/PROGRESS.md](docs/PROGRESS.md).)*

---

## Những gì đã chạy ổn định trên máy thật (`SM_A047F` / `R58TA1ESSZN`)
- **Offline 100%:** 755 từ/cụm từ cốt lõi (700 từ NGSL + CEFR-J A1–B2 + 55 cụm từ/từ ghép thông dụng như `go to`, `look after`...) + 12.000 mục từ gợi ý offline (gồm 1.500 từ ghép & cụm từ).
- **Mở khóa màn hình tự bật câu hỏi:** Hoạt động tin cậy trên Android 14 / Samsung One UI 6 (`ACTION_USER_PRESENT` + `KEYGUARD_UNLOCKED`).
- **Thoát nhanh 1 chạm:** Chạm vào bất kỳ vùng nào ngoài 4 ô đáp án là đóng câu hỏi ngay, ghi `Thoát nhanh (DISMISSED)` không phạt SM-2.
- **Chống học vẹt (`QuizQuestionStyle`):** Hỗ trợ tự động nâng cấp theo SM-2 (mới học: Anh→Việt; lặp `>= 2`: Việt→Anh cùng từ loại hoặc Luyện nghe phát âm → Việt), hoặc chọn cố định từng kiểu.
- **Quản lý từ vựng đầy đủ & Responsive 360dp:** Hiển thị đủ toàn bộ kho từ (không cắt ở 200), lọc được theo `phrase`, gợi ý & tự thêm cả từ ghép (`"go to"`), **Sửa** từ, **`+ Nạp gói B1–B2`**, **`+ Nhập nhanh`**, nút `Nghe / Sửa / Xóa` không bị xuống dòng.
- **Thống kê & Biểu đồ KHKT:** Phân bố mức độ thuộc SM-2, biểu đồ cột 7 ngày gần nhất, Top từ hay sai nhất, xuất cả lịch sử ôn (`unlockvocab_review_logs.csv`) lẫn kho từ (`unlockvocab_study_words.csv`).
- **Tài liệu giới thiệu & Cơ sở khoa học:** Đã hoàn thiện [README.md](README.md), [docs/GIOI_THIEU_DU_AN.md](docs/GIOI_THIEU_DU_AN.md) và 3 tệp bàn giao ở thư mục gốc: [Gioi_Thieu_San_Pham_UnlockVocab.docx](Gioi_Thieu_San_Pham_UnlockVocab.docx), [Huong_Dan_Su_Dung_UnlockVocab.docx](Huong_Dan_Su_Dung_UnlockVocab.docx) và `UnlockVocab.apk` (`12,542,274 bytes`).

---

## Các việc còn lại của Giai đoạn 4 (Chuẩn bị đi thi)
1. **Chạy thực nghiệm dài ngày trên Samsung A04s (`SM_A047F`):** Dùng thật hàng ngày (và so sánh nhóm đối chứng nếu cần cho KHKT), theo dõi pin và độ ổn định qua đêm.
2. **Xuất dữ liệu thực nghiệm CSV & vẽ biểu đồ báo cáo:** Thu thập file CSV log ôn tập làm minh chứng khoa học cho báo cáo KHKT & Tin học trẻ bảng D.
3. **Đóng gói APK Release & Quay video demo:** Build bản ký số (`assembleRelease`) và quay màn hình demo thao tác thực tế.
