#!/usr/bin/env python3
"""
Generates two publication-ready Word (.docx) deliverables in the project root:
  1. Gioi_Thieu_San_Pham_UnlockVocab.docx
  2. Huong_Dan_Su_Dung_UnlockVocab.docx
"""

from pathlib import Path
from docx import Document
from docx.enum.table import WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor

ROOT = Path(__file__).resolve().parent.parent


def set_cell_shading(cell, hex_color: str) -> None:
    tc_pr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), hex_color)
    tc_pr.append(shd)


def set_table_borders(table, color: str = "CBD5E1") -> None:
    tbl_pr = table._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for side in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{side}")
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), "4")
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), color)
        borders.append(el)
    tbl_pr.append(borders)


def create_base_doc() -> Document:
    doc = Document()
    for sec in doc.sections:
        sec.top_margin = Cm(2.0)
        sec.bottom_margin = Cm(2.0)
        sec.left_margin = Cm(2.3)
        sec.right_margin = Cm(2.0)

    normal = doc.styles["Normal"]
    normal.font.name = "Times New Roman"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "Times New Roman")
    normal.font.size = Pt(12.5)
    normal.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)
    normal.paragraph_format.line_spacing = 1.25
    normal.paragraph_format.space_after = Pt(5)
    return doc


def add_title_block(doc: Document, title: str, subtitle: str, meta_lines: list[str]) -> None:
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(4)
    r = p.add_run(title)
    r.bold = True
    r.font.name = "Times New Roman"
    r.font.size = Pt(19)
    r.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A)

    p2 = doc.add_paragraph()
    p2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p2.paragraph_format.space_after = Pt(6)
    r2 = p2.add_run(subtitle)
    r2.italic = True
    r2.font.name = "Times New Roman"
    r2.font.size = Pt(13)
    r2.font.color.rgb = RGBColor(0x33, 0x41, 0x55)

    for line in meta_lines:
        pm = doc.add_paragraph()
        pm.alignment = WD_ALIGN_PARAGRAPH.CENTER
        pm.paragraph_format.space_after = Pt(2)
        rm = pm.add_run(line)
        rm.font.name = "Times New Roman"
        rm.font.size = Pt(11)
        rm.font.color.rgb = RGBColor(0x47, 0x55, 0x69)

    add_divider(doc)


def add_divider(doc: Document) -> None:
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(2)
    p.paragraph_format.space_after = Pt(8)
    p_pr = p._p.get_or_add_pPr()
    p_bdr = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), "6")
    bottom.set(qn("w:space"), "1")
    bottom.set(qn("w:color"), "1E40AF")
    p_bdr.append(bottom)
    p_pr.append(p_bdr)


def add_h1(doc: Document, text: str) -> None:
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(12)
    p.paragraph_format.space_after = Pt(4)
    p.paragraph_format.keep_with_next = True
    r = p.add_run(text)
    r.bold = True
    r.font.name = "Times New Roman"
    r.font.size = Pt(14.5)
    r.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A)


def add_h2(doc: Document, text: str) -> None:
    p = doc.add_paragraph()
    p.paragraph_format.space_before = Pt(8)
    p.paragraph_format.space_after = Pt(3)
    p.paragraph_format.keep_with_next = True
    r = p.add_run(text)
    r.bold = True
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)
    r.font.color.rgb = RGBColor(0x0F, 0x17, 0x2A)


def add_para(doc: Document, segments: list[tuple[str, bool, bool]], space_after: int = 5) -> None:
    """Adds a paragraph from (text, bold, italic) tuples."""
    p = doc.add_paragraph()
    p.paragraph_format.space_after = Pt(space_after)
    for text, is_bold, is_italic in segments:
        r = p.add_run(text)
        r.bold = is_bold
        r.italic = is_italic
        r.font.name = "Times New Roman"
        r.font.size = Pt(12.5)


def add_bullet(doc: Document, lead: str, body: str) -> None:
    p = doc.add_paragraph(style="List Bullet")
    p.paragraph_format.space_after = Pt(3)
    if lead:
        r1 = p.add_run(lead)
        r1.bold = True
        r1.font.name = "Times New Roman"
        r1.font.size = Pt(12.5)
    r2 = p.add_run(body)
    r2.font.name = "Times New Roman"
    r2.font.size = Pt(12.5)


def add_callout(doc: Document, title: str, text: str, bg_hex: str = "EFF6FF") -> None:
    tbl = doc.add_table(rows=1, cols=1)
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(tbl, "93C5FD")
    cell = tbl.cell(0, 0)
    set_cell_shading(cell, bg_hex)
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(4)
    p.paragraph_format.space_after = Pt(4)
    r1 = p.add_run(title + "\n")
    r1.bold = True
    r1.font.name = "Times New Roman"
    r1.font.size = Pt(12)
    r1.font.color.rgb = RGBColor(0x1E, 0x3A, 0x8A)
    r2 = p.add_run(text)
    r2.font.name = "Times New Roman"
    r2.font.size = Pt(12)
    doc.add_paragraph().paragraph_format.space_after = Pt(2)


def add_styled_table(doc: Document, headers: list[str], rows: list[list[str]], col_widths_cm: list[float]) -> None:
    tbl = doc.add_table(rows=len(rows) + 1, cols=len(headers))
    tbl.alignment = WD_TABLE_ALIGNMENT.CENTER
    set_table_borders(tbl)

    for c_idx, h in enumerate(headers):
        cell = tbl.cell(0, c_idx)
        cell.width = Cm(col_widths_cm[c_idx])
        set_cell_shading(cell, "1E3A8A")
        p = cell.paragraphs[0]
        p.paragraph_format.space_before = Pt(3)
        p.paragraph_format.space_after = Pt(3)
        r = p.add_run(h)
        r.bold = True
        r.font.name = "Times New Roman"
        r.font.size = Pt(11.5)
        r.font.color.rgb = RGBColor(0xFF, 0xFF, 0xFF)

    for r_idx, row in enumerate(rows, start=1):
        fill = "F8FAFC" if r_idx % 2 == 0 else "FFFFFF"
        for c_idx, val in enumerate(row):
            cell = tbl.cell(r_idx, c_idx)
            cell.width = Cm(col_widths_cm[c_idx])
            set_cell_shading(cell, fill)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(2)
            p.paragraph_format.space_after = Pt(2)
            r = p.add_run(val)
            r.bold = (c_idx == 0)
            r.font.name = "Times New Roman"
            r.font.size = Pt(11)
    doc.add_paragraph().paragraph_format.space_after = Pt(3)


def build_intro_docx() -> Path:
    doc = create_base_doc()
    add_title_block(
        doc,
        "GIỚI THIỆU SẢN PHẨM UNLOCKVOCAB",
        "Ứng dụng học từ vựng tiếng Anh bằng Spaced Repetition kích hoạt khi mở khóa màn hình",
        [
            "Lĩnh vực: Sáng tạo Khoa học Kỹ thuật (KHKT) & Hội thi Tin học trẻ bảng D",
            "Nền tảng: Android Native (Kotlin, Jetpack Compose, SQLite 2 lớp) • Hoạt động Offline 100%",
        ],
    )

    add_h1(doc, "1. Lý do làm dự án (Từ vấn đề thực tiễn đến giải pháp)")
    add_h2(doc, "1.1. Vấn đề thực tiễn: \"Hội chứng bỏ cuộc sau 2 tuần\"")
    add_para(
        doc,
        [
            ("Khi mới bắt đầu học tiếng Anh, đa số học sinh và người tự học có động lực rất cao: tải nhiều ứng dụng flashcard, tìm website, lập kế hoạch học hàng trăm từ mỗi tuần. Tuy nhiên, thực tế cho thấy phần lớn người học chỉ duy trì được khoảng ", False, False),
            ("2 tuần đầu", True, False),
            (" rồi bỏ dở gần như hoàn toàn.", False, False),
        ],
    )
    add_para(
        doc,
        [
            ("Theo ", False, False),
            ("Mô hình Hành vi Fogg (Fogg Behavior Model — GS. B.J. Fogg, Đại học Stanford, 2009)", True, False),
            (", một hành vi B chỉ diễn ra khi hội tụ đồng thời 3 thành tố: ", False, False),
            ("B = M × A × T", True, False),
            (" (Motivation: Động lực, Ability: Khả năng thực hiện dễ dàng, Trigger: Tác nhân kích hoạt). Ở 2 tuần đầu, động lực M rất cao giúp người học vượt qua mọi thao tác bất tiện (nhớ giờ học, tìm biểu tượng ứng dụng, mở app, chờ tải mạng). Khi động lực M trở về mức bình thường, chỉ một ma sát nhỏ trong quy trình mở app cũng làm hành vi học bị đứt gãy.", False, False),
        ],
    )

    add_h2(doc, "1.2. Giải pháp cốt lõi: \"Không tự giác thì bị giác\"")
    add_para(
        doc,
        [
            ("Các nghiên cứu trong lĩnh vực Tương tác Người – Máy (HCI) chỉ ra trung bình người dùng tương tác với điện thoại thông minh từ ", False, False),
            ("57 đến 80+ lần mỗi ngày", True, False),
            (" (lên tới 80–110 lần sáng màn hình), trong đó khoảng ", False, False),
            ("43% – 46%", True, False),
            (" là thao tác mở khóa vào màn hình chính. ", False, False),
            ("UnlockVocab", True, False),
            (" gắn trực tiếp tác nhân kích hoạt T vào đúng thời điểm người dùng vừa mở khóa máy:", False, False),
        ],
    )
    add_bullet(doc, "Giảm ma sát về 0 (tối đa hóa Ability): ", "Thay vì bắt người dùng nhớ và tự mở app, thẻ trắc nghiệm 4 đáp án ngắn gọn tự bật ngay khi mở khóa (\"đưa cơm tận miệng\").")
    add_bullet(doc, "Tải nhận thức nhẹ (2–3 giây/lần): ", "Mỗi lần chỉ hỏi duy nhất 1 câu ngắn, tận dụng các khoảng vi-thời-gian (micro-sessions) trong ngày.")
    add_bullet(doc, "Giải quyết xung đột chú ý (Attention Economy Conflict): ", "Các nghiên cứu HCI cảnh báo việc ép trả lời ở mọi lần mở khóa lúc người dùng đang vội (thanh toán QR, nhận cuộc gọi, xem bản đồ) sẽ gây ức chế. Do đó UnlockVocab trang bị cơ chế Thoát nhanh 1 chạm: chạm vào bất kỳ vùng nào ngoài 4 ô đáp án sẽ tắt câu hỏi ngay lập tức và không phạt điểm ôn tập.")

    add_h1(doc, "2. Hiệu quả phương pháp & Nguồn chứng minh khoa học")
    add_para(
        doc,
        [
            ("UnlockVocab không chỉ là một ứng dụng hiển thị ngẫu nhiên mà kết hợp 4 nền tảng khoa học trí nhớ đã được chứng minh bằng thực nghiệm:", False, False),
        ],
    )
    add_styled_table(
        doc,
        ["Thành phần khoa học", "Tác giả & Công trình gốc", "Kết luận chứng minh & Cách áp dụng trong UnlockVocab"],
        [
            [
                "Đường cong quên lãng\n(Forgetting Curve)",
                "Hermann Ebbinghaus (1885), Über das Gedächtnis",
                "Trí nhớ giảm nhanh theo hàm mũ ngay sau khi tiếp thu nếu không được nhắc lại kịp thời trước ngưỡng quên.",
            ],
            [
                "Hiệu ứng giãn cách\n(Spacing Effect)",
                "Nicholas J. Cepeda et al. (2006), Psychological Bulletin (\"Distributed practice in verbal recall tasks\")",
                "Phân tích tổng hợp chứng minh ôn tập phân tán theo khoảng thời gian tăng dần giúp ghi nhớ dài hạn vượt trội so với học nhồi nhét.",
            ],
            [
                "Hiệu ứng kiểm tra chủ động\n(Testing Effect)",
                "Henry L. Roediger III & Jeffrey D. Karpicke (2006), Psychological Science (\"Test-enhanced learning\")",
                "Nhóm học bằng tự kiểm tra nhớ tốt hơn rõ rệt sau 2 ngày và 1 tuần so với nhóm chỉ đọc lại thụ động. UnlockVocab áp dụng trắc nghiệm chủ động kèm 3 đáp án nhiễu cùng từ loại.",
            ],
            [
                "Thuật toán SuperMemo SM-2\n+ Tốc độ phản hồi",
                "TS. Piotr Woźniak (1990), Optimization of learning",
                "Tự động tính Ease Factor (>= 1.3) và số ngày ôn kế tiếp. UnlockVocab kết hợp đo responseTimeMs (< 2,5s đạt mức phản xạ tối đa q = 5).",
            ],
        ],
        [4.0, 5.2, 7.5],
    )

    add_h1(doc, "3. Khảo sát thị trường & Điểm khác biệt của sản phẩm")
    add_para(
        doc,
        [
            ("Qua khảo sát các ứng dụng phổ biến trên thị trường, hiện chưa có sản phẩm miễn phí không quảng cáo nào hội tụ đầy đủ: hoạt động offline 100%, tự bật khi mở khóa Android 14, thoát nhanh 1 chạm ngoài ô đáp án và SM-2 theo thời gian phản hồi như UnlockVocab:", False, False),
        ],
    )
    add_styled_table(
        doc,
        ["Tiêu chí", "WordBit", "Semper (UnlockYourBrain)", "AnkiDroid", "UnlockVocab"],
        [
            ["Tự bật khi mở khóa", "Có", "Có (câu đố ngắn)", "Không (tự mở app)", "Có (Android 14 / One UI)"],
            ["Thoát nhanh 1 chạm ngoài đáp án", "Nút đóng nhỏ / vuốt", "Bắt giải / nút bỏ qua", "Không áp dụng", "Chạm mọi vùng ngoài 4 đáp án"],
            ["Miễn phí & Không quảng cáo", "Có quảng cáo + gói Turbo trả phí", "Giới hạn / gói nội dung", "Miễn phí, không QC", "Miễn phí 100%, không QC"],
            ["Offline 100%", "Offline phần đã tải", "Phụ thuộc gói tải", "Offline 100%", "Offline 100% (từ điển + TTS)"],
            ["SM-2 đo tốc độ phản hồi (ms) & chống học vẹt", "SRS theo mốc cơ bản", "Độ khó thô", "SM-2 tự bấm đánh giá", "Đo ms + đảo Việt↔Anh / Nghe / Cloze"],
            ["Gợi ý từ điển Việt–Anh & Xuất CSV KHKT", "Không", "Không", "Tự nhập tay", "Có (SQLite 50.000 mục A1–C2 + CSV)"],
        ],
        [3.6, 3.1, 3.3, 3.1, 3.6],
    )

    add_h1(doc, "4. Kiến trúc dữ liệu & Hệ thống tính năng nổi bật")
    add_bullet(doc, "Kiến trúc SQLite Offline 2 lớp & Tab Từ điển 50.000 mục: ", "Lớp 1 (offline_dict.db: SQLite đóng gói sẵn 50.000 mục gồm 4.500 từ ghép/cụm từ và đầy đủ cấp độ A1–C2) phục vụ cả Tab Từ điển tra cứu Anh ↔ Việt độc lập (xem nghĩa đánh số, ví dụ, cụm từ liên quan, nghe phát âm TTS và bấm + Học 1 chạm) lẫn gợi ý tự động; Lớp 2 (study_words: khởi tạo 855 từ/cụm từ A1–C2) lưu trạng thái SM-2 và từ tự thêm.")
    add_bullet(doc, "Chọn trình độ cá nhân & Nạp/Xoá từ theo cấp độ (A1 – C2): ", "Người dùng chọn trình độ của bản thân (A1, A2, B1, B2, C1, C2) ngay trên Trang chủ để ưu tiên từ mới đúng trình độ; tại tab Từ vựng, nút + Nạp theo cấp và - Xoá bớt từ cho phép tự chọn 1 hoặc nhiều cấp độ từ A1 đến C2, lọc theo từ loại và số lượng (25 / 50 / 100 / 200 từ hoặc xoá hết cấp, luôn giữ nguyên 100% từ tự thêm).")
    add_bullet(doc, "Trải nghiệm không gây phiền & Nhận diện ngữ cảnh: ", "Hỗ trợ Thoát nhanh 1 chạm ngoài 4 ô đáp án; Tự động tạm nghỉ 15 phút (Adaptive Snooze) nếu người dùng thoát nhanh >= 2 lần liên tiếp trong 10 phút; Nút Tạm nghỉ 30 phút / Bật lại ngay trên Trang chủ và thông báo dịch vụ nền; Nhận diện Im lặng / Rung / Tắt tiếng Media để không tự phát âm thanh bất ngờ khi không đeo tai nghe.")
    add_bullet(doc, "Đáp án nhiễu thông minh & Câu hỏi Ngữ cảnh 1 dòng (Cloze): ", "3 đáp án nhiễu ưu tiên cùng từ loại + cùng cấp độ CEFR và loại trừ nghĩa trùng từ khóa; chế độ Tự động theo SM-2 tự nâng cấp từ Anh→Việt sang Việt→Anh / Nghe→Việt (lặp >= 2) và Điền từ ngữ cảnh 1 dòng Cloze ____ (lặp >= 3).")
    add_bullet(doc, "Tùy biến tần suất & tiêu chí chọn từ: ", "Hỗ trợ hiện mỗi lần mở khóa, sau mỗi N lần mở khóa, hoặc cách tối thiểu X phút, kèm Giới hạn câu/ngày, Giờ im lặng ban đêm (23h–6h) và tự tránh khi đang có cuộc gọi. Hỗ trợ 5 tiêu chí chọn từ (Đến hạn SM-2, Từ mới, Từ hay sai, Lọc từ loại/CEFR A1–C2 có cảnh báo bộ lọc rỗng, Ngẫu nhiên).")
    add_bullet(doc, "Thêm từ có gợi ý & Sửa/Nhập hàng loạt: ", "Gõ từ (kể cả từ ghép nhiều chữ) có gợi ý offline từ 50.000 mục; nếu chưa có sẵn, form tự thêm kiểm tra bắt buộc đủ Từ vựng + Từ loại + Nghĩa chính + nhiều Nghĩa phụ. Hỗ trợ nạp từ tự chọn theo cấp độ A1–C2 và nhập nhiều dòng một lúc.")
    add_bullet(doc, "Thống kê, Đối chiếu Ebbinghaus & Pre/Post-test KHKT: ", "Thanh phân bố 4 mức SM-2 (Thuộc sâu >= 21 ngày / Mới nhớ 6–20 ngày / Đang học / Chưa học), bảng đối chiếu đường cong quên Ebbinghaus theo mốc D1–D21, bài kiểm tra đối chứng Pre-test / Post-test 15 câu đo độ tăng trưởng, biểu đồ cột 7 ngày gần nhất, Top từ hay sai nhất, bộ Tự kiểm thử lõi 11 test và xuất 3 file CSV (review_logs, study_words, pre_post_tests).")
    add_bullet(doc, "Chống đánh cắp & Xác thực bản quyền tác giả (5 lớp): ", "Bảo vệ sản phẩm bằng chữ ký số Release APK riêng kết hợp R8 làm rối mã nguồn (app-release.apk ~4,2 MB), bảng thủy vân _author_meta trong SQLite offline_dict.db, lớp OwnershipGuard kiểm tra chéo tên gói + SHA-256 lúc chạy, thẻ Bản quyền & Xác thực tác giả trên Trang chủ và cột origin_watermark trong toàn bộ file CSV KHKT.")

    add_h1(doc, "5. Nguồn dữ liệu mở hợp lệ bản quyền & Tài liệu tham khảo")
    add_bullet(doc, "Từ điển Anh–Việt: ", "TS. Hồ Ngọc Đức — Free Vietnamese Dictionary Project (FVDP / OVDP, giấy phép GNU GPL).")
    add_bullet(doc, "Từ vựng tần suất cốt lõi: ", "New General Service List (NGSL 1.01 — Browne, Culligan & Phillips, CC BY-SA).")
    add_bullet(doc, "Phân tầng trình độ CEFR: ", "CEFR-J Vocabulary Profile (openlanguageprofiles/olp-en-cefrj, Yukio Tono, CC BY-SA 4.0).")
    add_bullet(doc, "Phiên âm IPA: ", "open-dict-data/ipa-dict (en_US, giấy phép MIT). Phát âm sử dụng Android TextToSpeech offline.")
    add_callout(
        doc,
        "Danh mục tài liệu khoa học trích dẫn:",
        "1. Ebbinghaus, H. (1885). Über das Gedächtnis.\n"
        "2. Woźniak, P. A. (1990). Optimization of learning (SuperMemo SM-2 Algorithm).\n"
        "3. Cepeda, N. J., et al. (2006). Distributed practice in verbal recall tasks. Psychological Bulletin, 132(3), 354–380.\n"
        "4. Roediger, H. L., & Karpicke, J. D. (2006). Test-enhanced learning. Psychological Science, 17(3), 249–255.\n"
        "5. Fogg, B. J. (2009). A behavior model for persuasive design. Persuasive Technology Conference.",
    )

    out_path = ROOT / "Gioi_Thieu_San_Pham_UnlockVocab.docx"
    doc.save(out_path)
    return out_path


def build_guide_docx() -> Path:
    doc = create_base_doc()
    add_title_block(
        doc,
        "HƯỚNG DẪN CÀI ĐẶT & SỬ DỤNG UNLOCKVOCAB",
        "Phiên bản Offline hoàn toàn cho Android (Khuyến nghị Android 8.0 – Android 14+)",
        [
            "Tệp bộ cài đi kèm: UnlockVocab.apk (Application ID: com.huy.unlockvocab)",
            "Đã kiểm thử thực tế trên: Samsung Galaxy A04s (Android 14 / One UI 6)",
        ],
    )

    add_h1(doc, "1. Yêu cầu thiết bị & Chuẩn bị")
    add_bullet(doc, "Hệ điều hành: ", "Điện thoại Android 8.0 (API 26) trở lên (tối ưu tốt cho Android 10–14 và Samsung One UI).")
    add_bullet(doc, "Kết nối mạng: ", "Không cần Internet trong suốt quá trình học, tra gợi ý từ điển hay nghe phát âm.")
    add_bullet(doc, "Bộ nhớ trống: ", "Khoảng 35 MB để cài đặt và lưu dữ liệu từ vựng offline.")

    add_h1(doc, "2. Hướng dẫn cài đặt file UnlockVocab.apk")
    add_h2(doc, "Cách 1: Cài đặt trực tiếp trên điện thoại Android (Khuyên dùng cho người dùng phổ thông)")
    add_bullet(doc, "Bước 1 — Chép file vào điện thoại: ", "Chép file UnlockVocab.apk từ máy tính vào bộ nhớ điện thoại (qua cáp USB vào thư mục Download, hoặc gửi qua Zalo/Google Drive rồi tải về máy).")
    add_bullet(doc, "Bước 2 — Mở tệp APK: ", "Mở ứng dụng Quản lý tệp (trên Samsung là \"File của bạn\" / My Files) → vào mục Tải về (Downloads) → chạm vào tệp UnlockVocab.apk.")
    add_bullet(doc, "Bước 3 — Cấp quyền cài ứng dụng ngoài CH Play: ", "Nếu máy hỏi \"Vì lý do bảo mật, điện thoại của bạn không được phép cài đặt các ứng dụng không xác định từ nguồn này\" → bấm Cài đặt (Settings) → bật gạt \"Cho phép từ nguồn này\" → quay lại và bấm Cài đặt (Install).")
    add_bullet(doc, "Bước 4 — Xử lý thông báo Google Play Protect (nếu xuất hiện): ", "Vì file APK cài trực tiếp phục vụ nghiên cứu/thi đấu chưa đưa lên CH Play, Play Protect có thể hiện cảnh báo kiểm tra → bấm \"Chi tiết thêm\" (More details) → chọn \"Vẫn cài đặt\" (Install anyway).")
    add_bullet(doc, "Bước 5 — Hoàn tất: ", "Chờ vài giây báo \"Đã cài đặt ứng dụng\" rồi bấm Mở (Open).")

    add_h2(doc, "Cách 2: Cài đặt từ máy tính Windows qua cáp USB (Dùng ADB cho lập trình viên)")
    add_para(
        doc,
        [
            ("Bật ", False, False),
            ("Gỡ lỗi USB (USB Debugging)", True, False),
            (" trong Tùy chọn nhà phát triển trên điện thoại, cắm cáp USB vào máy tính và chạy lệnh sau trong PowerShell tại thư mục chứa APK:", False, False),
        ],
    )
    add_callout(
        doc,
        "Lệnh cài đặt & cấp nhanh quyền qua ADB:",
        "adb install -r UnlockVocab.apk\n"
        "adb shell appops set com.huy.unlockvocab SYSTEM_ALERT_WINDOW allow\n"
        "adb shell pm grant com.huy.unlockvocab android.permission.POST_NOTIFICATIONS",
        "F8FAFC",
    )

    add_h1(doc, "3. Thiết lập quyền lần đầu trên Android 14 / Samsung One UI")
    add_para(
        doc,
        [
            ("Để thẻ trắc nghiệm tự động hiện lên khi bạn vừa mở khóa màn hình và không bị hệ thống Samsung tắt ngầm khi khóa máy lâu, hãy kiểm tra mục ", False, False),
            ("Quyền chạy nền", True, False),
            (" ngay ở đầu tab Trang chủ:", False, False),
        ],
    )
    add_styled_table(
        doc,
        ["Tên quyền", "Tác dụng", "Thao tác bật trên điện thoại"],
        [
            [
                "1. Hiển thị trên ứng dụng khác (Overlay)",
                "Cho phép vẽ thẻ câu hỏi trắc nghiệm nổi lên ngay khi bạn mở khóa điện thoại.",
                "Bấm nút \"Cấp quyền\" → tìm UnlockVocab trong danh sách → gạt BẬT.",
            ],
            [
                "2. Thông báo dịch vụ nền",
                "Giữ Foreground Service hoạt động hợp lệ trên Android 13/14.",
                "Bấm nút \"Cấp quyền\" → chọn \"Cho phép\".",
            ],
            [
                "3. Tắt tối ưu pin",
                "Ngăn Samsung One UI đưa ứng dụng vào chế độ ngủ sâu làm mất tính năng mở khóa.",
                "Bấm \"Mở cài đặt\" → chọn \"Cho phép\" (hoặc vào Pin → Không hạn chế).",
            ],
        ],
        [4.5, 6.2, 6.0],
    )
    add_callout(
        doc,
        "Kiểm tra nhanh ngay sau khi cấp quyền:",
        "Bật công tắc \"Bật câu hỏi khi mở khóa\", sau đó bấm nút \"Test hiện câu hỏi Overlay ngay\". Nếu thấy thẻ câu hỏi nổi lên giữa màn hình là ứng dụng đã sẵn sàng 100%.",
    )

    add_h1(doc, "4. Cách thao tác khi thẻ trắc nghiệm hiện lên lúc mở khóa")
    add_bullet(doc, "Trả lời câu hỏi (2–3 giây): ", "Đọc từ vựng (hoặc bấm nút Nghe để nghe phát âm tiếng Anh offline) và chạm vào 1 trong 4 ô đáp án A / B / C / D. Ứng dụng lập tức báo Đúng/Sai, hiển thị thêm các nghĩa phụ + câu ví dụ và tự động đóng sau ~1 giây.")
    add_bullet(doc, "Thoát nhanh 1 chạm khi đang bận: ", "Chạm vào BẤT KỲ vùng nào trên màn hình NGOẠI TRỪ 4 ô đáp án (chạm nền mờ xung quanh, vùng trống trong thẻ, tiêu đề từ...) hoặc bấm nút Quay lại. Câu hỏi tắt ngay lập tức và được ghi là \"Thoát nhanh\", hoàn toàn KHÔNG bị trừ điểm SM-2.")

    add_h1(doc, "5. Hướng dẫn sử dụng 4 màn hình chính của ứng dụng")
    add_h2(doc, "5.1. Tab Trang chủ (Cấu hình kích hoạt & Học thông minh)")
    add_bullet(doc, "Trình độ của bạn (A1 – C2): ", "Chọn trình độ hiện tại (A1, A2, B1, B2, C1, C2) để hệ thống ưu tiên chọn từ mới đúng trình độ khi mở khóa, kèm nút + Nạp thêm 50 từ cấp độ đang chọn ngay tại chỗ.")
    add_bullet(doc, "Tạm nghỉ nhanh (Snooze) & Tần suất kích hoạt: ", "Có nút Tạm nghỉ 30 phút (và Bật lại ngay) ở Trang chủ lẫn ngay trên thanh thông báo dịch vụ nền khi bạn đang bận; ngoài ra nếu bạn Thoát nhanh >= 2 lần liên tiếp trong 10 phút, app tự tạm nghỉ 15 phút. Bạn có thể chọn hiện Ở mỗi lần mở khóa, Sau mỗi N lần mở khóa, Cách tối thiểu X phút, Giới hạn câu/ngày và Giờ im lặng (23h–6h).")
    add_bullet(doc, "Tiêu chí chọn từ hiển thị: ", "Chọn 1 trong 5 chế độ: Đến hạn ôn (SM-2), Từ mới chưa học, Từ hay sai / khó nhất, Lọc theo từ loại / cấp độ CEFR (A1–C2, có cảnh báo nếu bộ lọc đang 0 từ), hoặc Trộn ngẫu nhiên.")
    add_bullet(doc, "Kiểu câu hỏi & An toàn âm thanh: ", "Khuyên dùng \"Tự động theo SM-2\" (mới học hỏi Anh→Việt; lặp >= 2 chuyển sang Việt→Anh hoặc Luyện nghe→Việt; lặp >= 3 chuyển sang Điền từ ngữ cảnh 1 dòng Cloze ____). Nếu điện thoại đang Im lặng / Rung / Tắt Media và không đeo tai nghe, app tự tránh phát tiếng bất ngờ.")

    add_h2(doc, "5.2. Tab Từ vựng (Tra cứu, Gợi ý 50.000 mục, Tự thêm & Nạp/Xoá theo cấp độ)")
    add_bullet(doc, "Tìm kiếm & Lọc: ", "Gõ từ tiếng Anh hoặc nghĩa tiếng Việt để tìm trong toàn bộ kho học; lọc nhanh theo Tự thêm, noun, verb, adj, adv, phrase (từ ghép/cụm động từ như \"go to\") hoặc theo từng cấp độ A1, A2, B1, B2, C1, C2.")
    add_bullet(doc, "+ Thêm từ (Gợi ý 50.000 mục offline & Tự thêm): ", "Khi gõ từ vào ô Từ vựng, danh sách gợi ý offline (từ SQLite 50.000 mục) tự động hiện ra tức thì; chạm 1 gợi ý để điền sẵn từ loại, IPA, CEFR, nghĩa. Nếu là từ chuyên ngành chưa có sẵn, bạn tự chọn Từ loại (bắt buộc), Cấp độ CEFR và nhập Nghĩa chính + bấm \"+ Thêm nghĩa\" để thêm nhiều nghĩa phụ.")
    add_bullet(doc, "+ Nạp theo cấp, - Xoá bớt từ, Sửa từ & + Nhập nhanh: ", "Bấm \"+ Nạp theo cấp\" hoặc \"- Xoá bớt từ\" để tự chọn 1 hoặc nhiều cấp độ bất kỳ từ A1 đến C2, lọc theo từ loại và số lượng (25 / 50 / 100 / 200 hoặc xoá hết cấp — luôn bảo vệ 100% các từ Tự thêm); bấm \"Sửa\" trên từng thẻ từ để chỉnh nghĩa/từ loại mà không mất tiến độ SM-2; hoặc bấm \"+ Nhập nhanh\" để dán nhiều dòng theo mẫu: từ, từ_loại, nghĩa_chính; nghĩa_phụ.")

    add_h2(doc, "5.3. Tab Từ điển Offline (Tra cứu Anh ↔ Việt 50.000 mục & Đưa vào kho học 1 chạm)")
    add_bullet(doc, "Tra cứu 2 chiều Anh ↔ Việt tức thì: ", "Gõ từ tiếng Anh (ví dụ look), cụm từ hoặc nghĩa tiếng Việt vào ô tìm kiếm để tra ngay trong 50.000 mục từ & 4.500 cụm từ offline; lọc kết quả theo từ loại (noun, verb, adj, adv, phrase) hoặc cấp độ A1–C2.")
    add_bullet(doc, "Xem chi tiết, Nghe phát âm & + Học 1 chạm: ", "Chạm vào từng mục từ để mở bảng chi tiết gồm phiên âm IPA, nghĩa tiếng Việt đánh số, câu ví dụ song ngữ và các cụm từ liên quan (như look at, look in...). Bấm \"Nghe\" để nghe phát âm tiếng Anh hoặc bấm \"+ Học\" / \"+ Đưa vào kho học\" để chuyển ngay từ đó vào kho ôn mở khóa.")

    add_h2(doc, "5.4. Tab Thống kê (Theo dõi, Đối chiếu Ebbinghaus, Pre/Post-test & Xuất số liệu KHKT)")
    add_bullet(doc, "Chỉ số ghi nhớ & Đối chiếu Ebbinghaus (D1–D21): ", "Xem Tỷ lệ nhớ (%), Thời gian phản hồi trung bình (ms), Số lượt Thoát nhanh, Thanh phân bố 4 mức độ ghi nhớ SM-2 (Thuộc sâu >= 21d / Mới nhớ 6–20d / Đang học / Chưa học), Bảng đối chiếu Đường cong quên Ebbinghaus theo 4 mốc D1–D21, Biểu đồ 7 ngày gần nhất và Top từ hay sai nhất.")
    add_bullet(doc, "Pre-test / Post-test 15 câu, Tự kiểm thử & Xuất CSV: ", "Bấm \"Làm Pre-test\" / \"Làm Post-test\" để đo điểm đầu vào và sau thực nghiệm trên 15 câu trắc nghiệm; bấm \"Tự kiểm thử lõi\" để chạy nhanh 11 bài test thuật toán SM-2/50k từ điển/bản quyền; bấm \"Xuất log CSV\", \"Xuất kho từ CSV\" hoặc \"Xuất KHKT Test CSV\" khi cần số liệu làm báo cáo (mỗi file đều có cột origin_watermark xác thực nguồn gốc).")
    add_bullet(doc, "Kiểm tra bản quyền chính chủ ở cuối Trang chủ: ", "Thẻ \"Bản quyền & Xác thực tác giả\" hiển thị tên tác giả, mã gói gốc com.huy.unlockvocab và mã SHA-256 kèm trạng thái \"Chính chủ • Đã xác thực SQLite & APK\".")

    add_h1(doc, "6. Xử lý một số tình huống thường gặp (FAQ)")
    add_bullet(doc, "Mở khóa màn hình nhưng không thấy thẻ hiện lên? ", "Kiểm tra: (1) Công tắc \"Bật câu hỏi khi mở khóa\" đã BẬT chưa; (2) Quyền Overlay đã \"Đã cấp\" chưa; (3) Tần suất kích hoạt có đang đặt là \"Sau N lần\" hoặc \"Cách X phút\" hay đang trong Giờ im lặng 23h–6h không; (4) Đã tắt tối ưu pin Samsung cho app chưa.")
    add_bullet(doc, "Bấm nút Nghe không thấy phát âm thanh? ", "Kiểm tra âm lượng Media của điện thoại đã mở chưa, và máy có gói giọng đọc tiếng Anh (Google Text-to-Speech / Samsung TTS) cài sẵn chưa (vào Cài đặt máy → Quản lý chung → Chuyển văn bản thành giọng nói để kiểm tra offline).")
    add_bullet(doc, "Ứng dụng có tốn nhiều pin hay 4G không? ", "App chạy 100% offline không dùng dữ liệu mạng; Foreground Service chỉ lắng nghe sự kiện hệ thống nên mức tiêu thụ pin rất thấp.")

    out_path = ROOT / "Huong_Dan_Su_Dung_UnlockVocab.docx"
    doc.save(out_path)
    return out_path


def main() -> None:
    intro = build_intro_docx()
    guide = build_guide_docx()
    print(f"Created: {intro} ({intro.stat().st_size / 1024:.1f} KB)")
    print(f"Created: {guide} ({guide.stat().st_size / 1024:.1f} KB)")


if __name__ == "__main__":
    main()
