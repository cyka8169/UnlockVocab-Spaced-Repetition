# Nguồn dữ liệu từ điển Offline & Giấy phép (Licenses)

Thư mục này lưu các bộ dữ liệu gốc dùng để xây dựng cơ sở dữ liệu offline (2 lớp: `dict_entries` để tra cứu/gợi ý và `vocab_seed` cốt lõi) cho ứng dụng **UnlockVocab**.

| Tệp dữ liệu | Nguồn gốc | Vai trò | Giấy phép |
|---|---|---|---|
| `cefrj-vocabulary-profile-1.5.csv` | [openlanguageprofiles/olp-en-cefrj](https://github.com/openlanguageprofiles/olp-en-cefrj) (từ dự án CEFR-J) | Phân cấp trình độ A1–B2 theo từ loại | CC BY-SA 4.0 |
| `octanove-vocabulary-profile-c1c2-1.0.csv` | [openlanguageprofiles/olp-en-cefrj](https://github.com/openlanguageprofiles/olp-en-cefrj) | Bổ sung cấp độ C1–C2 | CC BY-SA 4.0 |
| `NGSL-1.01.csv` | New General Service List (Browne, Culligan, Phillips) qua [koba-ninkigumi/ngsl](https://github.com/koba-ninkigumi/ngsl) | Danh sách ~2.800 từ tần suất cao nhất | CC BY-SA |
| `ipa-en_US.txt` | [open-dict-data/ipa-dict](https://github.com/open-dict-data/ipa-dict) | Phiên âm IPA chuẩn Anh–Mỹ | MIT |
| `anhviet109K.txt` | Từ điển Anh–Việt TS. Hồ Ngọc Đức (FVDP/OVDP) qua [yenthanh132/avdict-database-sqlite-converter](https://github.com/yenthanh132/avdict-database-sqlite-converter) | Nghĩa tiếng Việt, từ loại & ví dụ (~109K mục từ) | GPL |

> **Lưu ý:** Khi đóng gói app Android, dùng script tiền xử lý ghép các tệp này thành cơ sở dữ liệu SQLite/JSON gọn nhẹ trong `app/src/main/assets/` để ứng dụng chạy **100% offline** trên thiết bị người dùng, không cần nhúng nguyên tệp thô nặng vào APK.

> **Phân tách bản quyền dự thi:** Phần dữ liệu từ điển biên soạn (`offline_dict.db`, `vocab_seed.json`) và script tiền xử lý (`scripts/build_offline_data.py`) tuân thủ đầy đủ giấy phép gốc **GPL / CC BY-SA 4.0 / MIT** như bảng trên; phần mã nguồn ứng dụng Android và hồ sơ dự thi thuộc bản quyền tác giả (xem chi tiết tại [LICENSE](../LICENSE)).
