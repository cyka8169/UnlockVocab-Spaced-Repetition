# Hướng dẫn "Quay ngược thời gian" khi code mới bị sai (Git & Backup)

Tài liệu này giúp bạn (và bất kỳ AI Agent nào hỗ trợ sau này) **luôn quay về được phiên bản chạy ổn định** nếu chẳng may code mới bị lỗi hoặc làm hỏng tính năng cũ.

---

## 1. Hai script làm sẵn trong thư mục `scripts/`

Mở PowerShell tại thư mục gốc `d:\a_project\app_tieng_anh_Spaced_Repetition`:

### Lưu mốc khi code đang chạy ngon (`checkpoint.ps1`)
```powershell
.\scripts\checkpoint.ps1 -Message "feat: chay duoc overlay khi mo khoa" -Tag "v0.1-mvp"
```
- Script sẽ tự động `git add -A`, `git commit`, và gắn nhãn (`tag`) nếu bạn truyền `-Tag`.

### Quay ngược khi code mới bị hỏng (`rollback.ps1`)
- **Xem danh sách các mốc đã lưu:**
  ```powershell
  .\scripts\rollback.ps1 -List
  ```
- **Hủy toàn bộ thay đổi chưa commit (khi vừa sửa nhầm và chưa kịp lưu mốc):**
  ```powershell
  .\scripts\rollback.ps1 -DiscardUncommitted
  ```
- **Quay về một commit hoặc tag cũ (an toàn — tự động tạo nhánh backup trước khi quay lại):**
  ```powershell
  .\scripts\rollback.ps1 -Target "v0.1-mvp"
  ```
  *(Bạn có thể truyền mã commit như `a1b2c3d` hoặc tên tag như `v0.1-mvp`. Script luôn tạo nhánh `backup/...` lưu trạng thái hiện tại trước khi reset nên không sợ mất hẳn code.)*

---

## 2. Các lệnh Git thủ công cần nhớ

| Tình huống | Lệnh dùng | Tác dụng |
|---|---|---|
| Xem đang ở đâu, sửa file gì | `git status` | Kiểm tra file đã sửa / chưa lưu |
| Xem lịch sử các mốc | `git log --oneline -n 15` | Lấy mã commit (7 ký tự đầu) |
| Thử tính năng mới không ảnh hưởng `main` | `git checkout -b feat/ten-moi` | Tách nhánh làm thử; hỏng thì `git checkout main` là xong |
| Gộp nhánh ngon vào `main` | `git checkout main` rồi `git merge feat/ten-moi` | Đưa tính năng đã test xong vào bản chính |
| Xóa sửa đổi chưa commit | `git restore .` | Trả code về đúng commit gần nhất |
| Cất tạm code đang viết dở | `git stash` *(lấy lại: `git stash pop`)* | Dọn sạch thư mục tạm thời để test bản cũ |
| Đảo ngược 1 commit lỗi mà vẫn giữ lịch sử | `git revert <ma-commit>` | Tạo commit mới undo commit lỗi (rất an toàn) |
| Quay thẳng về 1 mốc cũ | `git reset --hard <ma-commit-hoac-tag>` | Đưa toàn bộ code về y hệt mốc đó |

---

## 3. Hai lớp bảo vệ bổ sung nên dùng song song

1. **GitHub (Private Repository):**
   - Sau khi tạo repo trống trên GitHub (chế độ Private), kết nối 1 lần:
     ```powershell
     git remote add origin https://github.com/<tai-khoan>/<ten-repo>.git
     git push -u origin main --tags
     ```
   - Mỗi lần làm xong một tính năng: `git push` để dự phòng cả khi hỏng ổ cứng.
2. **Local History của IDE (Android Studio / VS Code):**
   - Trong Android Studio: chuột phải vào file hoặc thư mục → **Local History → Show History** để khôi phục từng đoạn code theo phút, ngay cả khi chưa kịp `git commit`.
