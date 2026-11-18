# Theo dõi tiến độ dự án UnlockVocab

> **File ghi chú bắt buộc cho Huy & mọi AI Agent:** Đọc file này trước khi code và cập nhật lại ngay sau khi hoàn thành mỗi giai đoạn/tính năng.

---

## 1. Trạng thái tổng quan hiện tại

- **Giai đoạn hiện tại:** Chuẩn bị bước vào **Giai đoạn 1 — Xây dựng MVP & Bộ dữ liệu Offline 2 lớp**.
- **Nhánh Git chính:** `main`
- **Mốc Git gần nhất:** `v0.0-spec-ready` (`60f58d2`)
- **Thiết bị thật kiểm thử:** Samsung Galaxy A04s (`SM_A047F`, Android 14 / API 34, ADB serial `R58TA1ESSZN`) — đã kết nối OK.
- **Công cụ đã cài trên máy:** JDK 17 (`17.0.21.0.2+1-LTS`), Android SDK (`android-34` đến `android-37.0`), Git `2.56.0`, `scrcpy 5.0.1`, Python `3.12.10`, Gradle `9.1.0` (cache sẵn AGP `9.0.1`, Compose BOM `2026.03.01`).

---

## 2. Nhật ký các hạng mục đã hoàn thành

### Mốc `v0.0-init` (`07d4e27`) & `v0.0-spec-ready` (`60f58d2`)
- [x] Kiểm tra kết nối máy thật Samsung Galaxy A04s (`R58TA1ESSZN`) qua `adb`.
- [x] Cài đặt **Microsoft OpenJDK 17** và **scrcpy 5.0.1**.
- [x] Khởi tạo Git (`main`), tạo `.gitignore`, `AGENTS.md`, `docs/PROJECT_PLAN.md`, `docs/GIT_ROLLBACK_GUIDE.md`.
- [x] Tạo 2 script an toàn: `scripts/checkpoint.ps1` (lưu mốc + tag) và `scripts/rollback.ps1` (quay ngược có tạo nhánh backup).
- [x] Chốt các yêu cầu đặc thù trong `AGENTS.md`:
  - Thoát nhanh 1 chạm: chạm bất kỳ vùng nào ngoài 4 ô đáp án → đóng ngay, không phạt điểm SM-2 (`DISMISSED`).
  - Kích hoạt & chọn từ đa tiêu chí (không ép chỉ theo khoảng thời gian).
  - Offline 100% + kiến trúc 2 lớp (`dict_entries` tra cứu gợi ý & `study_words` bộ học SM-2).
  - Form tự thêm từ bắt buộc đủ **Từ vựng + Từ loại + Các nghĩa (1 nghĩa chính + nhiều nghĩa phụ)**.
- [x] Tải 5 bộ dữ liệu từ điển nguồn mở về `data_sources/` + ghi giấy phép vào `data_sources/LICENSE_SOURCES.md`:
  - `cefrj-vocabulary-profile-1.5.csv` (CEFR-J A1–B2, CC BY-SA 4.0)
  - `octanove-vocabulary-profile-c1c2-1.0.csv` (C1–C2, CC BY-SA 4.0)
  - `NGSL-1.01.csv` (~2.800 từ tần suất cao, CC BY-SA)
  - `ipa-en_US.txt` (IPA Anh–Mỹ, MIT)
  - `anhviet109K.txt` (Từ điển TS. Hồ Ngọc Đức, GPL)

---

## 3. Danh sách việc đang triển khai (Checklist Giai đoạn 1 → 4)

### Đang làm (Nhánh `feat/mvp-unlockvocab`)
- [ ] Tạo script `scripts/build_offline_data.py` sinh `vocab_seed.json` & `dict_compact.json` vào `app/src/main/assets/`.
- [ ] Dựng khung Gradle Wrapper (`9.1.0`) + Project Android (`com.huy.unlockvocab`, AGP `9.0.1`, Kotlin + Compose + SQLite 2 lớp).
- [ ] Cài đặt thuật toán SM-2 (`Sm2Scheduler.kt`) kết hợp Đúng/Sai + thời gian phản hồi (`ms`).
- [ ] Cài đặt `UnlockService` + `UnlockReceiver` (`ACTION_USER_PRESENT`) + `QuizOverlayManager` (chạm ngoài 4 ô đáp án để đóng ngay + TTS).
- [ ] Giao diện 3 tab (`Home` cấu hình kích hoạt & chọn từ, `Từ vựng` có Autocomplete + Form tự thêm từ, `Thống kê` + xuất CSV).
- [ ] Build debug APK, cài đặt & test thực tế trên Samsung Galaxy A04s (`R58TA1ESSZN`).
- [ ] Lưu mốc `v0.1-mvp` và gộp vào `main`.

### Việc tiếp theo sau `v0.1-mvp`
- [ ] Thử nghiệm thực tế mở khóa qua đêm trên `SM_A047F` (tối ưu pin Samsung One UI).
- [ ] Mở rộng bộ lọc chủ đề (THPT / IELTS / Giao tiếp) & nhập từ hàng loạt từ CSV.
- [ ] Thu thập log thực nghiệm phục vụ báo cáo KHKT & Tin học trẻ bảng D.
