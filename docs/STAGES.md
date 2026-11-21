# Các giai đoạn phát triển dự án UnlockVocab

Tài liệu này mô tả lộ trình 5 giai đoạn của dự án và trạng thái thực tế của từng giai đoạn.

| Giai đoạn | Tên gọi | Trạng thái | Mốc Git |
|---|---|---|---|
| **Giai đoạn 0** | Chuẩn bị công cụ, Quy trình Git & Kho từ điển nguồn mở | **ĐÃ HOÀN THÀNH (100%)** | `v0.0-init`, `v0.0-spec-ready` |
| **Giai đoạn 1** | MVP Mở khóa hiện Overlay & Thoát nhanh 1 chạm | **ĐÃ HOÀN THÀNH (100%)** | `v0.1-mvp`, `v0.1.1-fix-unlock` |
| **Giai đoạn 2** | Thuật toán SM-2 & Chính sách kích hoạt/chọn từ đa tiêu chí | **ĐÃ HOÀN THÀNH (100%)** | *(gộp trong `v0.1-mvp`)* |
| **Giai đoạn 3** | Quản lý Kho từ (Gợi ý & Tự thêm), Từ ghép, Chống học vẹt & UI | **ĐÃ HOÀN THÀNH (100%)** | `v0.2-stage3-complete`, `v0.2.1-phrases-and-ui-fix` |
| **Giai đoạn 4** | Thực nghiệm đối chứng, Biểu đồ & Đóng gói thi KHKT / THT bảng D | **ĐANG Ở GIAI ĐOẠN NÀY (~98%)** | `v1.0-deliverables` .. `v1.5.3-khkt` (dự kiến cuối: `v1.0-release`) |

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

### Giai đoạn 4 — Thực nghiệm đối chứng & Đóng gói thi KHKT / THT bảng D `[ĐANG Ở GIAI ĐOẠN NÀY ~98%]`
- **Đã hoàn thành (`v1.0-deliverables`, `v1.1-package-a`, `v1.2-package-b`, `v1.2.1-remove-by-level`, `v1.3-dictionary`, `v1.4-package-c-qa-guide`, `v1.5-anti-theft`, `v1.5.1-anti-theft-hardened` & `v1.5.2-overlay-read-explanation`):**
  - Thanh phân bố **4 mức độ ghi nhớ SM-2** (*Thuộc sâu ≥21 ngày / Mới nhớ 6–20 ngày / Đang học / Chưa học*).
  - Biểu đồ cột thực nghiệm **7 ngày gần nhất** và danh sách **Top từ cần ôn kỹ (hay sai nhất)** ngay trong tab `Thống kê`.
  - **Gói A (UX không gây phiền & Nhận diện ngữ cảnh):** Tự tránh phát TTS ở chế độ Im lặng/Rung trừ khi đeo tai nghe; Adaptive Snooze 15 phút khi thoát nhanh `>= 2` lần trong 10 phút; nút **Tạm nghỉ 30 phút / Bật lại ngay** trên Trang chủ và Notification; stepper **Giới hạn/ngày**; cảnh báo bộ lọc `BY_TOPIC_OR_POS` khi `0` từ khớp.
  - **Gói B, Cấp độ CEFR `A1` – `C2` & Tab Từ điển 50.000 mục:** Đóng gói sẵn SQLite `offline_dict.db` **50.000 mục từ & 4.500 cụm từ / từ ghép** (`A1`–`C2`); chọn trình độ cá nhân (`A1`–`C2`) trên Trang chủ; hộp thoại **`+ Nạp theo cấp`** và **`- Xoá bớt từ`** tự chọn nhiều cấp độ (`A1`–`C2`), từ loại và số lượng (`25–200` hoặc xoá hết cấp, luôn bảo vệ 100% từ tự thêm); **tab Từ điển Offline** tra cứu 2 chiều Anh ↔ Việt, xem chi tiết + cụm từ liên quan, nghe phát âm và bấm `+ Học` 1 chạm; đáp án nhiễu cùng `pos` + cùng `cefr` chống trùng nghĩa; câu hỏi **Điền từ ngữ cảnh 1 dòng — Cloze (`____`)**.
  - **Gói C & Tài liệu QA Agent:** Bảng đối chiếu **Đường cong quên Ebbinghaus (`D1 / D3 / D7 / D21`)**, bài thi đối chứng **Pre-test / Post-test 15 câu offline** + xuất `unlockvocab_pre_post_tests.csv`, bộ **Tự kiểm thử lõi (`CoreSelfTester` — 11 test)**, và tạo [AGENT_TESTING_GUIDE.md](AGENT_TESTING_GUIDE.md) cho các Agent khác tự động kiểm thử toàn app (`TC-01` → `TC-08`).
  - **Gói Chống đánh cắp 5 lớp gia cố R8 & Tuân thủ giấy phép (`v1.5-anti-theft` → `v1.5.1-anti-theft-hardened`):** Sinh khóa ký Release riêng (`keystore/` giữ bí mật qua `.gitignore`), gỡ bỏ `-keep` rộng ở `proguard-rules.pro` để R8 làm rối toàn phần cả gói lõi `data`/`domain`/`security`, nâng cấp XOR phụ thuộc vị trí trong `OwnershipGuard.kt`, siết bản Release (`v1.5.1-khkt`, `4,219,757 bytes`) chỉ nhận khóa Release và chặn `FLAG_DEBUGGABLE`, kiểm tra `origin_signature` + `package_id` trong `_author_meta`, chặn Overlay khi giả mạo, thêm tệp `LICENSE` phân tách mã nguồn & dữ liệu mở (`GPL`/`CC BY-SA 4.0`/`MIT`), kiểm thử `11/11` `CoreSelfTester` & mở khóa thật trực tiếp trên `app-release.apk` trên `R58TA1ESSZN`, và đẩy mã nguồn lên GitHub Private (`cyka8169/UnlockVocab-Spaced-Repetition`).
  - **Cải tiến Overlay đọc giải thích sau khi trả lời (`v1.5.2-overlay-read-explanation`, `v1.5.2-khkt`):** Gỡ bỏ tự động đóng sau `1,1s` trên `QuizOverlayManager.kt`; sau khi trả lời giữ nguyên thẻ mở (bọc `ScrollView` chừa lề tối ngoài thẻ), hiển thị hộp giải thích chi tiết (từ loại tiếng Việt/Anh, IPA, nghĩa chính/phụ, ví dụ), làm mờ các đáp án sai không chọn, mở lại nút `Nghe` ở cả chế độ `Việt → Anh` / `Cloze`, lộ từ tiếng Anh ở `LISTEN_TO_VI`, và chỉ đóng khi chạm vùng tối bên ngoài thẻ hoặc bấm Back (không ghi thêm `DISMISSED`). Kiểm thử 4 kịch bản thực tế trên Release R8 (`R58TA1ESSZN`) + `11/11` `CoreSelfTester`, `0` lỗi crash.
  - **Cập nhật Đồng tác giả & Chuẩn hóa tài liệu khoa học (`v1.5.3-khkt`):** Ghi nhận nhóm đồng tác giả **Trần Tiến Huy** (`huyerror666@gmail.com`) và **Nguyễn Minh Thư** (`nguyenminhthu20071982@gmail.com`) trong `OwnershipGuard.kt`, thủy vân SQLite `_author_meta` (`offline_dict.db`), thẻ Bản quyền `HomeScreen.kt`, `LICENSE`, `README.md`, `docs/GIOI_THIEU_DU_AN.md` và cả 2 file `.docx`; tự động nâng cấp `offline_dict.db` khi nâng bản trên máy (`hasCurrentAuthorMeta`); chuẩn hóa các trích dẫn khoa học (8 tài liệu kèm DOI, làm rõ chứng thư SHA-256 vs APK SHA-256, mốc nội suy Ebbinghaus, hiện trạng Semper); kiểm thử trực tiếp bản Release R8 `v1.5.3-khkt` trên `R58TA1ESSZN` đạt `11/11` `CoreSelfTester`, `0` lỗi `AndroidRuntime:E` và `9/9` chuỗi bảo vệ đều ẩn khỏi `classes.dex`.
  - Xuất trọn bộ hồ sơ Word & bộ cài APK đồng bộ mới nhất: `Gioi_Thieu_San_Pham_UnlockVocab.docx` (`44,425 bytes`), `Huong_Dan_Su_Dung_UnlockVocab.docx` (`47,523 bytes`), và `UnlockVocab.apk` (`4,219,793 bytes`, xuất từ `app-release.apk` bản R8).
- **Còn lại để hoàn tất dự án (`v1.0-release`):**
  - Chạy thử nghiệm thực tế dài ngày trên `SM_A047F` (theo dõi pin, độ ổn định nền One UI, thu thập log CSV đối chứng).
  - Hoàn thiện báo cáo khoa học & quay video demo dự thi từ bản Release/Debug đã đóng gói.
