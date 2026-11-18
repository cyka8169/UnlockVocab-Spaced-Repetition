# Các giai đoạn phát triển dự án UnlockVocab

Tài liệu này mô tả lộ trình 5 giai đoạn của dự án và trạng thái thực tế của từng giai đoạn.

| Giai đoạn | Tên gọi | Trạng thái | Mốc Git |
|---|---|---|---|
| **Giai đoạn 0** | Chuẩn bị công cụ, Quy trình Git & Kho từ điển nguồn mở | **ĐÃ HOÀN THÀNH (100%)** | `v0.0-init`, `v0.0-spec-ready` |
| **Giai đoạn 1** | MVP Mở khóa hiện Overlay & Thoát nhanh 1 chạm | **ĐÃ HOÀN THÀNH (100%)** | `v0.1-mvp`, `v0.1.1-fix-unlock` |
| **Giai đoạn 2** | Thuật toán SM-2 & Chính sách kích hoạt/chọn từ đa tiêu chí | **ĐÃ HOÀN THÀNH (100%)** | *(gộp trong `v0.1-mvp`)* |
| **Giai đoạn 3** | Quản lý Kho từ (Gợi ý & Tự thêm), Từ ghép, Chống học vẹt & UI | **ĐÃ HOÀN THÀNH (100%)** | `v0.2-stage3-complete`, `v0.2.1-phrases-and-ui-fix` |
| **Giai đoạn 4** | Thực nghiệm đối chứng, Biểu đồ & Đóng gói thi KHKT / THT bảng D | **ĐANG THỰC HIỆN (~40%)** | Dự kiến `v1.0-release` |

---

## Chi tiết từng giai đoạn

### Giai đoạn 0 — Thiết lập công cụ, An toàn mã nguồn & Kho từ điển mở `[ĐÃ HOÀN THÀNH 100%]`
- Cài đặt **Microsoft OpenJDK 17**, **scrcpy 5.0.1**, thiết lập **Gradle Wrapper 9.1.0**, kết nối máy thật **Samsung Galaxy A04s (`SM_A047F`, `R58TA1ESSZN`)**.
- Khởi tạo Git (`main`), tạo quy trình chống lỗi ([GIT_ROLLBACK_GUIDE.md](GIT_ROLLBACK_GUIDE.md), `scripts/checkpoint.ps1`, `scripts/rollback.ps1`) và hướng dẫn chuẩn [AGENTS.md](../AGENTS.md).
- Tải 5 bộ từ điển nguồn mở hợp lệ bản quyền (`NGSL`, `CEFR-J`, `ipa-dict`, `anhviet109K`) và viết [build_offline_data.py](../scripts/build_offline_data.py) đóng gói thành `vocab_seed.json` (700 từ) + `dict_compact.json` (9.500 mục từ).

### Giai đoạn 1 — Bản MVP Mở khóa hiện Overlay & Thoát nhanh `[ĐÃ HOÀN THÀNH 100%]`
- Dựng ứng dụng Android Native (`Kotlin + Jetpack Compose + SQLite 2 lớp`).
- Cài `UnlockService` (Foreground Service `specialUse`) lắng nghe sự kiện mở khóa (`ACTION_USER_PRESENT` với `RECEIVER_EXPORTED` cho Android 14 / One UI 6 + watcher dự phòng `KeyguardManager`), tự tránh bật khi đang có cuộc gọi.
- Cài `QuizOverlayManager`: thẻ trắc nghiệm nổi `WindowManager Overlay`, phát âm `TextToSpeech` offline, và **chạm bất kỳ vùng nào ngoài 4 ô đáp án để tắt ngay (`DISMISSED`)** không bị phạt SM-2.
- Đã kiểm thử thực tế trên `R58TA1ESSZN`: khóa máy → mở khóa liên tiếp 2 lần đều tự bật câu hỏi mượt mà, 0 lỗi crash.

### Giai đoạn 2 — Thuật toán SM-2 & Kích hoạt / Chọn từ đa tiêu chí `[ĐÃ HOÀN THÀNH 100%]`
- Thuật toán `Sm2Scheduler`: điều chỉnh lịch ôn theo Đúng/Sai kết hợp thời gian phản hồi (`ms`).
- **3 chế độ kích hoạt linh hoạt:** Mỗi lần mở khóa / Sau mỗi N lần mở khóa / Cách tối thiểu X phút (+ Giờ im lặng 23h–6h).
- **5 tiêu chí chọn từ:** `DUE_FIRST` (đến hạn SM-2), `NEW_WORDS` (từ mới), `HARDEST_FIRST` (hay sai/ease thấp), `BY_TOPIC_OR_POS` (lọc từ loại/CEFR), `RANDOM_MIXED` (ngẫu nhiên).
- Sinh 3 đáp án nhiễu **cùng từ loại (`pos`)** để tránh loại trừ thô.

### Giai đoạn 3 — Quản lý Kho từ, Chống học vẹt & Thống kê `[ĐÃ HOÀN THÀNH 100%]`
- **Đã hoàn thành đầy đủ (`v0.2-stage3-complete`):**
  - Giao diện 3 tab (`Trang chủ`, `Từ vựng`, `Thống kê`).
  - Tìm kiếm & lọc từ trong kho học; **gợi ý tự động (Autocomplete) offline** từ 9.500 mục từ `dict_entries`.
  - **Form tự thêm từ mới** bắt buộc đủ: **Từ vựng + Từ loại (`pos`) + Nghĩa chính (+ nhiều nghĩa phụ)**, kèm nút nghe thử TTS.
  - **Chống học vẹt (`QuizQuestionStyle`):** Chế độ Tự động theo SM-2 (Anh→Việt khi mới học; tự chuyển Việt→Anh cùng từ loại hoặc Luyện nghe phát âm → Việt khi `repetitions >= 2`) + cho phép chọn tay từng chế độ.
  - **Chỉnh sửa & Mở rộng kho từ:** Nút `Sửa` trực tiếp trên từng từ, nút `+ Nạp gói B1–B2` từ kho từ điển offline, và nút `+ Nhập nhanh` dán nhiều dòng một lúc.
  - Màn hình `Thống kê` xuất cả `unlockvocab_review_logs.csv` lẫn `unlockvocab_study_words.csv`.

### Giai đoạn 4 — Thực nghiệm đối chứng & Đóng gói thi KHKT / THT bảng D `[ĐANG Ở GIAI ĐOẠN NÀY ~40%]`
- **Đã hoàn thành:**
  - Thanh phân bố mức độ ghi nhớ SM-2 (*Thuộc vững / Đang học / Chưa học*).
  - Biểu đồ cột thực nghiệm **7 ngày gần nhất** và danh sách **Top từ cần ôn kỹ (hay sai nhất)** ngay trong tab `Thống kê`.
- **Còn lại để hoàn tất dự án (`v1.0-release`):**
  - Chạy thử nghiệm thực tế dài ngày trên `SM_A047F` (theo dõi pin, độ ổn định nền One UI, thu thập log CSV đối chứng).
  - Build bản Release gọn nhẹ, hoàn thiện báo cáo khoa học & video demo dự thi.
