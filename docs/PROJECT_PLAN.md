# Kế hoạch triển khai dự án UnlockVocab

- **Cuộc thi mục tiêu:** Sáng tạo KHKT & Tin học trẻ bảng D
- **Thiết bị thử nghiệm chính:** Samsung Galaxy A04s (`SM_A047F`, Android 14 / SDK 34)
- **Stack đề xuất:** Kotlin + Jetpack Compose + Room Database + Coroutines + Android TextToSpeech (100% Offline)

---

## 1. Giải quyết bài toán & Điểm khác biệt

1. **Vấn đề:** Người học bỏ cuộc sau ~2 tuần vì phải chủ động nhớ và mở app.
2. **Giải pháp cốt lõi:** Biến thao tác mở khóa màn hình thành *micro-habit* 2–3 giây bằng một thẻ trắc nghiệm nổi (*Overlay*) ngay sau khi mở khóa.
3. **Các cải tiến quan trọng:**
   - **Offline hoàn toàn & Dữ liệu chuẩn mở:**
     - Nghĩa Anh–Việt: Từ điển TS. Hồ Ngọc Đức (FVDP/OVDP, GPL) & Kaikki/Wiktionary (CC BY-SA).
     - Bộ từ theo tần suất/cấp độ: NGSL + CEFR-J (CC BY-SA) — tránh rủi ro bản quyền từ điển thương mại khi thi KHKT.
     - Phiên âm IPA: `ipa-dict` (MIT). Câu ví dụ: Tatoeba (CC BY/CC0). Phát âm giọng đọc: Android `TextToSpeech` offline.
   - **Kho tra cứu + Tự thêm từ thông minh:**
     - Gõ thêm từ → gợi ý tự động (autocomplete) từ kho từ điển offline (từ vựng, IPA, từ loại, nhiều nghĩa để chọn).
     - Không có từ sẵn → form tự thêm có kiểm tra bắt buộc đủ **Từ vựng + Từ loại (`pos`) + Các nghĩa (nhiều nghĩa)**, kèm nút nghe thử phát âm.
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

### Giai đoạn 3 — Giao diện quản lý, Gợi ý/Tự thêm từ & Thống kê (`v0.3-ui-stats`)
- Màn hình Home: trạng thái bảo vệ nền (Overlay + Pin Samsung), Streak, số từ ôn hôm nay, chọn chế độ kích hoạt & tiêu chí chọn từ.
- Màn hình Từ vựng & Thêm từ:
  - Gõ từ → **gợi ý tự động (autocomplete)** từ kho từ điển offline (hiển thị IPA, từ loại, nhiều nghĩa để chọn nhanh).
  - Không có trong từ điển → **Form tự thêm** bắt buộc đủ **Từ vựng + Từ loại (`pos`) + Các nghĩa (nhiều nghĩa)**, kèm nút nghe thử TTS.
  - Xem, tìm kiếm, lọc theo chủ đề/cấp độ (NGSL, CEFR-J, THPT, IELTS), import CSV.
- Màn hình Thống kê: Retention rate, từ hay quên nhất, biểu đồ 7 ngày, nút xuất `review_logs.csv`.

### Giai đoạn 4 — Tối ưu Samsung One UI & Hoàn thiện thi đấu (`v1.0-release`)
- Thử nghiệm liên tục nhiều ngày trên `SM_A047F`, kiểm tra tiêu thụ pin và độ ổn định khi khóa máy qua đêm.
- Thêm hướng dẫn tắt tối ưu pin Samsung trong Onboarding.
- Chốt số liệu thực nghiệm và quay video demo.

---

## 3. Bộ khung kiến trúc đề xuất (2 lớp dữ liệu offline)

```text
app/src/main/
├── AndroidManifest.xml
├── assets/
│   ├── vocab_seed.json              # Bộ từ cốt lõi ban đầu (NGSL + CEFR-J)
│   └── offline_dict.db              # Kho từ điển tra cứu offline (Hồ Ngọc Đức / Kaikki + IPA)
└── java/com/huy/unlockvocab/
    ├── MainActivity.kt
    ├── data/
    │   ├── db/
    │   │   ├── AppDatabase.kt
    │   │   ├── DictionaryEntryEntity.kt  # Lớp 1: Kho từ điển tra cứu & gợi ý (chỉ đọc / FTS)
    │   │   ├── WordEntity.kt             # Lớp 2: Bộ từ đang học + SM-2 + nhiều nghĩa (primary + extras)
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
        ├── vocab/
        │   ├── VocabScreen.kt
        │   └── AddWordSheet.kt      # Autocomplete từ điển offline + Form tự thêm (Word, POS, Meanings)
        └── stats/StatsScreen.kt
```
