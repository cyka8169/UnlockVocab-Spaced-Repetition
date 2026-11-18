# Kế hoạch triển khai dự án UnlockVocab

- **Cuộc thi mục tiêu:** Sáng tạo KHKT & Tin học trẻ bảng D
- **Thiết bị thử nghiệm chính:** Samsung Galaxy A04s (`SM_A047F`, Android 14 / SDK 34)
- **Stack đề xuất:** Kotlin + Jetpack Compose + Room Database + Coroutines + Android TextToSpeech

---

## 1. Giải quyết bài toán & Điểm khác biệt

1. **Vấn đề:** Người học bỏ cuộc sau ~2 tuần vì phải chủ động nhớ và mở app.
2. **Giải pháp cốt lõi:** Biến thao tác mở khóa màn hình thành *micro-habit* 2–3 giây bằng một thẻ trắc nghiệm nổi (*Overlay*) ngay sau khi mở khóa.
3. **Các cải tiến quan trọng:**
   - **Thoát nhanh 1 chạm:** Chạm vào bất kỳ vùng nào ngoài các ô đáp án -> tắt câu hỏi ngay, không gây ức chế khi đang vội.
   - **Kích hoạt & Chọn từ linh hoạt:** Không bắt buộc chỉ đợi theo thời gian; hỗ trợ cấu hình theo mỗi lần mở khóa / sau N lần mở khóa / theo phút / theo tiêu chí từ đến hạn SM-2, từ khó, từ mới, ngẫu nhiên, theo chủ đề.
   - **Đáp án nhiễu thông minh:** Lọc 3 đáp án sai cùng từ loại (`noun`, `verb`, `adj`, `adv`).
   - **Đo lường thực nghiệm KHKT:** Ghi lại `ReviewLog` (đúng/sai/bỏ qua, thời gian phản hồi ms) và xuất CSV để chứng minh tỷ lệ ghi nhớ (*Retention Rate*).

---

## 2. Lộ trình 4 giai đoạn

### Giai đoạn 0 — Thiết lập công cụ & An toàn mã nguồn
- Cài JDK 17 (và Gradle / Gradle Wrapper), kiểm tra kết nối `adb` tới `SM_A047F`.
- Khởi tạo Git (`main`), `.gitignore`, `AGENTS.md`, tài liệu hướng dẫn quay ngược (`docs/GIT_ROLLBACK_GUIDE.md`) và các script `checkpoint.ps1`, `rollback.ps1`.
- Tạo commit gốc `v0.0-init`.

### Giai đoạn 1 — Bản MVP chạy trên máy thật (`v0.1-mvp-overlay`)
- Dựng project Android Kotlin + Jetpack Compose + Room.
- `UnlockService` (Foreground Service) + `UnlockReceiver` bắt sự kiện `ACTION_USER_PRESENT`.
- `QuizOverlayManager` vẽ màn hình Overlay:
  - Vùng nền + vùng tiêu đề thẻ bắt sự kiện tap -> **đóng ngay lập tức**.
  - 4 nút đáp án riêng biệt; bấm vào mới chấm điểm, phát âm TTS và tự đóng sau ~1 giây.
- Màn hình `MainActivity` tối giản: nút cấp quyền `SYSTEM_ALERT_WINDOW`, nút Bật/Tắt service, nút "Test hiện câu hỏi ngay".

### Giai đoạn 2 — Thuật toán SM-2 & Chính sách chọn từ đa tiêu chí (`v0.2-sm2-policies`)
- Triển khai `Sm2Scheduler` dựa trên Đúng/Sai + thời gian phản hồi (`responseTimeMs`).
- Triển khai `TriggerPolicy` (mỗi lần mở khóa / mỗi N lần / theo phút / giờ im lặng) và `WordSelectionMode` (Đến hạn SM-2 / Từ mới / Từ hay sai / Ngẫu nhiên / Theo chủ đề).
- Bộ sinh 3 đáp án nhiễu cùng `pos`.

### Giai đoạn 3 — Giao diện quản lý, Bộ từ vựng & Thống kê (`v0.3-ui-stats`)
- Màn hình Home: trạng thái bảo vệ nền (Overlay + Pin Samsung), Streak, số từ ôn hôm nay, chọn chế độ kích hoạt & tiêu chí chọn từ.
- Màn hình Từ vựng: xem, tìm kiếm, thêm từ thủ công, nạp bộ từ THPT/IELTS/Giao tiếp, import CSV.
- Màn hình Thống kê: Retention rate, từ hay quên nhất, biểu đồ 7 ngày, nút xuất `review_logs.csv`.

### Giai đoạn 4 — Tối ưu Samsung One UI & Hoàn thiện thi đấu (`v1.0-release`)
- Thử nghiệm liên tục nhiều ngày trên `SM_A047F`, kiểm tra tiêu thụ pin và độ ổn định khi khóa máy qua đêm.
- Thêm hướng dẫn tắt tối ưu pin Samsung trong Onboarding.
- Chốt số liệu thực nghiệm và quay video demo.

---

## 3. Bộ khung mã nguồn dự kiến

```text
app/src/main/
├── AndroidManifest.xml
├── assets/
│   └── sample_vocab.json
└── java/com/huy/unlockvocab/
    ├── MainActivity.kt
    ├── data/
    │   ├── db/
    │   │   ├── AppDatabase.kt
    │   │   ├── WordEntity.kt
    │   │   ├── ReviewLogEntity.kt
    │   │   └── WordDao.kt
    │   ├── prefs/
    │   │   └── SettingsStore.kt
    │   └── VocabRepository.kt
    ├── domain/
    │   ├── Sm2Scheduler.kt
    │   ├── TriggerPolicy.kt
    │   └── WordSelectionStrategy.kt
    ├── service/
    │   ├── UnlockService.kt
    │   ├── UnlockReceiver.kt
    │   └── BootReceiver.kt
    └── ui/
        ├── overlay/QuizOverlayManager.kt
        ├── home/HomeScreen.kt
        ├── vocab/VocabScreen.kt
        └── stats/StatsScreen.kt
```
