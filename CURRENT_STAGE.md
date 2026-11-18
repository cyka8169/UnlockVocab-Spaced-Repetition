# Trạng thái Giai đoạn Hiện Tại — UnlockVocab

> **Cập nhật gần nhất:** 2026-11-18 | **Mốc Git:** `v0.1.1-fix-unlock` (nhánh `main`)

## Đang ở giai đoạn nào?
Dự án đã hoàn thành trọn vẹn **Giai đoạn 0, Giai đoạn 1, Giai đoạn 2** và **khoảng 75% Giai đoạn 3**.

Hiện tại chúng ta đang ở **nửa sau Giai đoạn 3 (Hoàn thiện tính năng nâng cao cho việc học & quản lý từ)**, chuẩn bị bước sang **Giai đoạn 4 (Biểu đồ thực nghiệm, tối ưu dài ngày & đóng gói dự thi KHKT / Tin học trẻ bảng D)**.

*(Xem bảng chi tiết toàn bộ 5 giai đoạn tại [docs/STAGES.md](docs/STAGES.md) và nhật ký chi tiết tại [docs/PROGRESS.md](docs/PROGRESS.md).)*

---

## Những gì đã chạy ổn định trên máy thật (`SM_A047F` / `R58TA1ESSZN`)
- **Offline 100%:** 700 từ cốt lõi (NGSL + CEFR-J A1–B2) + 9.500 mục từ gợi ý offline (Hồ Ngọc Đức + ipa-dict).
- **Mở khóa màn hình là hiện câu hỏi:** Đã vá lỗi `RECEIVER_EXPORTED` cho Android 14 / Samsung One UI 6 và test thực tế khóa → mở khóa liên tiếp thành công.
- **Thoát nhanh 1 chạm:** Chạm vào bất kỳ vùng nào ngoài 4 ô đáp án là đóng câu hỏi ngay, ghi `Thoát nhanh (DISMISSED)` không phạt SM-2.
- **Tùy biến linh hoạt:** 3 chế độ kích hoạt + 5 tiêu chí chọn từ + sinh 3 đáp án nhiễu cùng từ loại + TTS phát âm offline.
- **Gợi ý & Tự thêm từ:** Gõ từ có gợi ý tự động; không có sẵn thì form tự thêm bắt buộc đủ Từ vựng + Từ loại + Nghĩa chính (+ nhiều nghĩa phụ).
- **Thống kê KHKT:** Theo dõi tỷ lệ nhớ, tốc độ phản hồi (ms) và xuất file CSV.

---

## Các việc tiếp theo cần chọn để làm tiếp (Cuối Giai đoạn 3 → Giai đoạn 4)
1. **Nâng cấp chống học vẹt (Giai đoạn 3):** Với từ đã ôn lặp `>= 2` lần, tự động đổi sang câu hỏi **Việt → Anh** hoặc **Nghe phát âm chọn nghĩa**.
2. **Sửa từ & Nhập bộ từ theo chủ đề (Giai đoạn 3):** Cho phép sửa từ đang học, thêm bộ từ THPT / IELTS / Giao tiếp và nhập danh sách từ từ file CSV.
3. **Biểu đồ & Thực nghiệm KHKT (Giai đoạn 4):** Thêm biểu đồ 7 ngày trong tab `Thống kê` và thu thập số liệu chạy thực tế nhiều ngày trên Samsung A04s.
