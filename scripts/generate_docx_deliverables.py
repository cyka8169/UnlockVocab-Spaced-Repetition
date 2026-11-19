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
            "Phiên bản phát hành: v1.5.2-khkt (Release R8, ~4,0 MB • SHA-256: 325F191DA7D70180)",
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
    add_bullet(doc, "Chống đánh cắp & Xác thực bản quyền tác giả (5 lớp gia cố R8): ", "Bảo vệ sản phẩm bằng chữ ký số Release APK riêng kết hợp R8 làm rối toàn phần cả gói lõi (data/domain/security, app-release.apk / UnlockVocab.apk ~4,0 MB), bảng thủy vân _author_meta trong SQLite offline_dict.db, lớp OwnershipGuard kiểm tra chéo tên gói + SHA-256 (chỉ nhận khóa Release trên bản phát hành, chặn cờ debuggable và chặn bật Overlay nếu bị đóng gói lại), thẻ Bản quyền & Xác thực tác giả trên Trang chủ và cột origin_watermark trong toàn bộ file CSV KHKT.")

    add_h1(doc, "5. Nguồn dữ liệu mở hợp lệ bản quyền & Tài liệu tham khảo")
    add_bullet(doc, "Phân tách bản quyền rõ ràng (tệp LICENSE): ", "Mã nguồn ứng dụng Android & hồ sơ dự thi thuộc bản quyền tác giả (com.huy.unlockvocab); kho dữ liệu từ điển mở và script tái tạo tuân thủ đầy đủ giấy phép gốc GPL / CC BY-SA 4.0 / MIT.")
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
        "Cẩm nang toàn diện cho người dùng (Offline 100% • Android 8.0 – Android 14+)",
        [
            "Tệp bộ cài đi kèm: UnlockVocab.apk — v1.5.2-khkt (Release R8, 4.219.757 bytes • ID: com.huy.unlockvocab)",
            "SHA-256 tệp APK: E20DB89BDC549EDD8EA98D6BAD5446CC43FE4C03799C5CFB3E92DBF180C6AD36",
            "Chứng thư xác thực chính chủ (SHA-256): 325F191DA7D70180 • Đã kiểm thử thực tế trên Samsung Galaxy A04s (Android 14)",
        ],
    )

    add_h1(doc, "1. Yêu cầu thiết bị & Chuẩn bị")
    add_bullet(doc, "Hệ điều hành: ", "Điện thoại Android 8.0 (API 26) trở lên (tối ưu tốt cho Android 10–14 và Samsung One UI).")
    add_bullet(doc, "Kết nối mạng: ", "Không cần Internet trong suốt quá trình học, tra gợi ý từ điển hay nghe phát âm.")
    add_bullet(doc, "Bộ nhớ trống: ", "Khoảng 35 MB để cài đặt và lưu dữ liệu từ vựng offline.")

    add_h1(doc, "2. Hướng dẫn cài đặt file UnlockVocab.apk")
    add_h2(doc, "Cách 1: Cài đặt trực tiếp trên điện thoại Android (Khuyên dùng cho người dùng phổ thông)")
    add_bullet(doc, "Bước 1 — Chép file vào điện thoại: ", "Chép file UnlockVocab.apk từ máy tính vào bộ nhớ điện thoại (qua cáp USB vào thư mục Download, hoặc tải từ mục Releases trên GitHub về máy).")
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
            ("Quyền chạy nền (Android 14 / Samsung)", True, False),
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
                "3. Tắt tối ưu pin để không bị ngủ nền",
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

    add_h1(doc, "4. Hướng dẫn sử dụng app — Cách dùng tổng quan (Quy trình 5 bước)")
    add_para(
        doc,
        [
            ("Giao diện chính của ", False, False),
            ("UnlockVocab", True, False),
            (" gồm ", False, False),
            ("4 tab ở thanh điều hướng đáy", True, False),
            (": ", False, False),
            ("Trang chủ", True, False),
            (" (cấu hình kích hoạt, trình độ & kiểu câu hỏi), ", False, False),
            ("Từ vựng", True, False),
            (" (quản lý kho từ đang ôn), ", False, False),
            ("Từ điển", True, False),
            (" (tra cứu 50.000 mục Anh ↔ Việt offline) và ", False, False),
            ("Thống kê", True, False),
            (" (theo dõi SM-2, kiểm tra trình độ Pre/Post-test & xuất CSV). Người mới bắt đầu chỉ cần làm theo lộ trình 5 bước dưới đây:", False, False),
        ],
    )
    add_styled_table(
        doc,
        ["Bước", "Mục tiêu", "Thao tác nhanh trên ứng dụng"],
        [
            [
                "Bước 1\nCài đặt & Cấp quyền",
                "Đảm bảo thẻ trắc nghiệm tự bật khi mở khóa máy.",
                "Tại tab Trang chủ: cấp đủ 3 quyền (Overlay, Thông báo, Pin), bật công tắc \"Bật câu hỏi khi mở khóa\" và bấm \"Test hiện câu hỏi Overlay ngay\".",
            ],
            [
                "Bước 2\nXác định trình độ",
                "Đo năng lực hiện tại (A1–C2) và đặt mục tiêu học.",
                "Vào tab Thống kê → bấm \"Làm Pre-test\" (15 câu) để tự đánh giá; sau đó chọn chip A1–C2 tương ứng ở mục \"Trình độ của bạn (A1 – C2)\" trên Trang chủ.",
            ],
            [
                "Bước 3\nLọc sạch & Nạp từ đúng cấp",
                "Tránh học lẫn từ quá dễ hoặc quá khó.",
                "Vào tab Từ vựng → bấm \"- Xoá bớt từ\" để dọn các cấp không cần (hoặc xoá hết từ mặc định) → bấm \"+ Nạp theo cấp\" để nạp đúng cấp độ đã chọn.",
            ],
            [
                "Bước 4\nHọc khi mở khóa",
                "Tạo phản xạ ôn từ hằng ngày không gây phiền.",
                "Mở khóa máy: rảnh thì chọn A/B/C/D rồi đọc kỹ hộp giải thích, chạm vùng tối bên ngoài thẻ để đóng; đang vội thì chạm ngoài 4 ô đáp án để thoát nhanh ngay.",
            ],
            [
                "Bước 5\nTra cứu & Theo dõi",
                "Bổ sung từ riêng và đánh giá tiến bộ.",
                "Tra từ mới tại tab Từ điển (bấm \"+ Học\" 1 chạm), tự thêm từ chuyên ngành ở tab Từ vựng, và xem tỷ lệ nhớ + Post-test ở tab Thống kê.",
            ],
        ],
        [3.3, 4.4, 9.0],
    )

    add_h1(doc, "5. Cách xác định trình độ, xoá hết từ không cần & thêm từ cùng trình độ")
    add_para(
        doc,
        [
            ("Mặc định khi cài mới, kho học khởi tạo sẵn ", False, False),
            ("855 từ/cụm từ cốt lõi (A1 – C2)", True, False),
            (" để bạn có thể dùng ngay. Tuy nhiên, nếu bạn đã vững các từ cơ bản (A1–A2) hoặc muốn học tập trung 100% vào một trình độ mục tiêu (như B1, B2 hoặc C1), hãy thực hiện đúng 3 bước dưới đây:", False, False),
        ],
    )

    add_h2(doc, "5.1. Cách xác định trình độ hiện tại (A1 – C2)")
    add_para(
        doc,
        [
            ("Kho dữ liệu offline 50.000 mục của UnlockVocab phân tầng sẵn ", False, False),
            ("6 cấp độ chuẩn khung tham chiếu châu Âu CEFR (A1 – C2)", True, False),
            (":", False, False),
        ],
    )
    add_styled_table(
        doc,
        ["Cấp độ", "Số từ phân tầng sẵn", "Phù hợp với đối tượng & mục tiêu"],
        [
            ["A1 (Cơ bản)", "1.971 mục từ", "Người mới bắt đầu / mất gốc, học sinh tiểu học – đầu THCS."],
            ["A2 (Sơ cấp)", "2.128 mục từ", "Học sinh THCS, giao tiếp thông dụng & tình huống đời sống hằng ngày."],
            ["B1 (Trung cấp)", "3.542 mục từ", "Học sinh THPT, ôn thi tốt nghiệp THPT Quốc gia (~IELTS 4.5–5.0)."],
            ["B2 (Trung cao cấp)", "3.752 mục từ", "Luyện thi IELTS 5.5–6.5, đọc hiểu tài liệu học thuật & báo chí."],
            ["C1 (Cao cấp)", "1.209 mục từ", "Luyện thi IELTS 7.0–8.0, học sinh giỏi tiếng Anh & từ vựng chuyên sâu."],
            ["C2 (Thành thạo)", "1.083 mục từ", "Từ vựng học thuật nâng cao, đọc hiểu văn bản chuyên môn sâu."],
        ],
        [3.6, 3.6, 9.5],
    )
    add_bullet(doc, "Cách tự kiểm tra trình độ ngay trong app (Pre-test 15 câu): ", "Vào tab Thống kê → cuộn đến thẻ \"Đo lường đối chứng KHKT (Pre / Post-test 15 câu)\" → bấm \"Làm Pre-test\". Chọn phạm vi từng cấp độ (ví dụ B1 hoặc B2) rồi trả lời 15 câu trắc nghiệm. Quy tắc gợi ý: đạt >= 80% (12/15 câu trở lên) thì nâng lên 1 cấp cao hơn; đạt 50%–75% thì học đúng cấp đó; dưới 50% nên lùi 1 cấp để củng cố nền tảng.")
    add_bullet(doc, "Đặt trình độ mục tiêu trên Trang chủ: ", "Quay về tab Trang chủ → mục \"Trình độ của bạn (A1 – C2)\" → chạm chọn đúng chip A1 / A2 / B1 / B2 / C1 / C2. Hệ thống sẽ tự động ưu tiên chọn các từ mới thuộc trình độ này khi bạn mở khóa điện thoại.")

    add_h2(doc, "5.2. Cách xoá hết từ mặc định (xoá các cấp quá dễ hoặc xoá sạch kho cũ)")
    add_para(
        doc,
        [
            ("Nếu bạn muốn loại bỏ toàn bộ từ vựng cũ/dễ để chỉ học từ ở trình độ mới, hãy làm theo các bước sau:", False, False),
        ],
    )
    add_bullet(doc, "Bước 1: ", "Chuyển sang tab Từ vựng → bấm nút chữ đỏ \"- Xoá bớt từ\" ở hàng nút phía trên.")
    add_bullet(doc, "Bước 2: ", "Tại hộp thoại \"Xoá bớt từ theo cấp độ (A1 – C2)\", chạm chọn các cấp muốn xoá (hoặc bấm nút \"Chọn cả A1–C2\" ở góc phải nếu muốn xoá sạch toàn bộ từ mặc định đang có; bên cạnh mỗi chip hiển thị số từ có thể xoá, ví dụ A1 (395)).")
    add_bullet(doc, "Bước 3: ", "Ở mục \"Lọc theo từ loại cần xoá\", chọn \"Tất cả\" (hoặc chọn riêng noun / verb / adj / adv / phrase nếu chỉ muốn lọc bớt một từ loại).")
    add_bullet(doc, "Bước 4: ", "Ở mục \"Số lượng từ muốn xoá bớt\", chọn \"Xoá hết cấp này\" (hoặc chọn mức 25 / 50 / 100 / 200 từ nếu chỉ muốn tỉa bớt).")
    add_bullet(doc, "Bước 5: ", "Bấm nút đỏ \"Xoá bớt từ\" để xác nhận. Tổng số từ trên tiêu đề \"Kho từ vựng (N từ)\" sẽ cập nhật giảm ngay lập tức.")
    add_callout(
        doc,
        "Cơ chế an toàn khi xoá kho từ:",
        "1. Toàn bộ từ bạn tự thêm (nhãn \"Tự thêm\") luôn được BẢO VỆ 100%, không bị xoá bởi chức năng \"- Xoá bớt từ\".\n"
        "2. Khi xoá một phần (25–200 từ), app ưu tiên xoá những từ bạn chưa học trước để không làm mất tiến độ ôn tập.\n"
        "3. App đã ghi nhớ trạng thái khởi tạo nên các từ bạn đã xoá sẽ KHÔNG tự động bị nạp lại khi tắt mở lại ứng dụng.",
        "FFF7ED",
    )

    add_h2(doc, "5.3. Cách thêm (nạp) từ mới cùng trình độ của bạn")
    add_bullet(doc, "Cách 1 — Nạp từ chi tiết theo cấp độ (Khuyên dùng tại tab Từ vựng): ", "Mở tab Từ vựng → bấm \"+ Nạp theo cấp\" (nếu đang mở hộp thoại xoá thì chạm chip \"+ Nạp thêm từ\" ở đầu hộp thoại). Chọn 1 hoặc nhiều cấp độ mong muốn (ví dụ chỉ chọn B2 — hiển thị dạng B2 (đang có/tổng 50k)), chọn từ loại (\"Tất cả\" hoặc noun/verb/adj/adv/phrase), chọn số lượng (25 / 50 / 100 / 200 từ) rồi bấm \"Nạp vào kho học\". Có thể lặp lại nhiều lần để nạp thêm 200, 400 hoặc nhiều hơn từ cấp độ đó.")
    add_bullet(doc, "Cách 2 — Nạp nhanh 50 từ ngay tại Trang chủ: ", "Ở tab Trang chủ, ngay dưới thẻ \"Trình độ của bạn (A1 – C2)\", bấm nút \"+ Nạp thêm 50 từ cấp <trình độ> vào kho học\" để nạp tức thì 50 từ thuộc cấp đang chọn.")
    add_bullet(doc, "Mẹo chỉ ôn đúng 1 cấp độ khi mở khóa (không cần xoá kho): ", "Nếu muốn giữ nguyên các từ đã học mà chỉ muốn thẻ mở khóa hỏi đúng từ thuộc trình độ mình chọn, vào Trang chủ → mục \"Tiêu chí chọn từ hiển thị\" → chọn \"Lọc theo từ loại / cấp độ\" → chọn cấp độ CEFR mục tiêu (ví dụ B2 hoặc C1).")

    add_h1(doc, "6. Cách thêm từ mới, sửa từ & quản lý kho từ vựng")
    add_h2(doc, "6.1. Cách 1 — Thêm từ có gợi ý tự động từ 50.000 mục offline (+ Thêm từ)")
    add_bullet(doc, "Bước 1: ", "Ở tab Từ vựng, bấm nút \"+ Thêm từ\" ở góc trên bên phải.")
    add_bullet(doc, "Bước 2: ", "Gõ từ tiếng Anh hoặc cụm từ (ví dụ: look after, algorithm, cut down) vào ô \"Từ vựng (bắt buộc) *\". Ngay khi bạn gõ, danh sách \"Gợi ý từ điển 50.000 mục offline\" hiện ra bên dưới.")
    add_bullet(doc, "Bước 3: ", "Chạm vào gợi ý đúng → ứng dụng tự động điền sẵn Từ loại, Phiên âm IPA, Cấp độ CEFR, Nghĩa chính, các Nghĩa phụ và Câu ví dụ. Bạn có thể bấm nút \"Nghe\" bên cạnh để kiểm tra phát âm, chỉnh lại nghĩa nếu muốn, rồi bấm \"Lưu từ\".")

    add_h2(doc, "6.2. Cách 2 — Tự thêm từ mới chưa có sẵn trong từ điển offline")
    add_para(
        doc,
        [
            ("Khi nhập từ chuyên ngành hoặc cụm từ riêng chưa xuất hiện trong gợi ý, hệ thống yêu cầu kiểm tra hợp lệ đủ các trường bắt buộc để thuật toán sinh 3 đáp án nhiễu hoạt động chính xác:", False, False),
        ],
    )
    add_bullet(doc, "1. Từ vựng (bắt buộc) *: ", "Không để trống; hỗ trợ cả từ đơn và từ ghép/cụm từ có khoảng trắng hoặc gạch nối.")
    add_bullet(doc, "2. Từ loại (bắt buộc) *: ", "Chạm chọn đúng 1 chip trong danh sách chuẩn: noun (Danh từ), verb (Động từ), adj (Tính từ), adv (Trạng từ), prep (Giới từ), phrase (Cụm từ), other (Khác).")
    add_bullet(doc, "3. Cấp độ CEFR (tùy chọn): ", "Chọn A1 – C2 (hoặc để \"Không đặt\").")
    add_bullet(doc, "4. Nghĩa chính (bắt buộc) *: ", "Nhập 1 nghĩa tiếng Việt ngắn gọn, rõ ràng để hiển thị làm đáp án trắc nghiệm.")
    add_bullet(doc, "5. Các nghĩa khác, IPA & Câu ví dụ (tùy chọn): ", "Bấm \"+ Thêm nghĩa\" để thêm nhiều nghĩa phụ; nhập IPA và câu ví dụ (nếu câu ví dụ chứa từ tiếng Anh đang thêm, app có thể tự động dùng làm câu hỏi điền từ Cloze ____). Cuối cùng bấm \"Lưu từ\" — từ mới sẽ có nhãn màu xanh \"Tự thêm\".")

    add_h2(doc, "6.3. Cách 3 — Nhập nhanh nhiều dòng cùng lúc (+ Nhập nhanh)")
    add_bullet(doc, "Thao tác: ", "Tại tab Từ vựng → bấm \"+ Nhập nhanh\" → dán danh sách nhiều từ (mỗi dòng 1 từ) rồi bấm \"Nhập danh sách\".")
    add_bullet(doc, "Định dạng chuẩn mỗi dòng: ", "từ, từ_loại (noun/verb/adj/adv/phrase), nghĩa_chính; nghĩa_phụ_1; nghĩa_phụ_2 (ví dụ: algorithm, noun, thuật toán; giải thuật).")

    add_h2(doc, "6.4. Cách 4 — Đưa vào kho học 1 chạm từ tab Từ điển")
    add_bullet(doc, "Thao tác: ", "Khi đang tra từ tại tab Từ điển, bấm trực tiếp nút \"+ Học\" trên thẻ kết quả (hoặc mở chi tiết từ rồi bấm \"+ Đưa vào kho học\"). Nút sẽ chuyển thành \"Đã có\" / \"Đã có trong kho học\".")

    add_h2(doc, "6.5. Tìm kiếm, lọc, sửa từ (giữ nguyên SM-2) & xoá từ")
    add_bullet(doc, "Tìm kiếm & Lọc kho học: ", "Gõ từ tiếng Anh hoặc nghĩa tiếng Việt vào ô \"Tìm từ hoặc nghĩa tiếng Việt...\"; lọc nhanh bằng các chip \"Tất cả\", \"Tự thêm\", theo từ loại (noun, verb, adj, adv, phrase) hoặc cấp độ (A1–C2).")
    add_bullet(doc, "Nghe, Sửa & Xoá trên từng thẻ từ: ", "Mỗi thẻ từ hiển thị tiến độ \"SM-2: lặp ... | đúng ... / sai ... | EF ...\" cùng nút \"Nghe\" (phát âm offline), nút \"Sửa\" (chỉnh nghĩa, IPA, ví dụ mà không làm mất lịch sử SM-2), và nút \"Xóa\" (áp dụng cho từ \"Tự thêm\").")

    add_h1(doc, "7. Thao tác trên thẻ trắc nghiệm & Tất cả các chức năng trọng yếu khác")

    add_h2(doc, "7.1. Thao tác trên thẻ câu hỏi khi mở khóa màn hình")
    add_bullet(doc, "Khi bạn đang rảnh (Trả lời & đọc giải thích): ", "Đọc từ vựng (hoặc bấm nút \"Nghe\") và chạm chọn 1 trong 4 đáp án A / B / C / D. Ứng dụng lập tức tô màu xanh đáp án đúng, đỏ đáp án sai (làm mờ nhẹ các phương án sai không chọn), đồng thời GIỮ NGUYÊN thẻ trên màn hình (không tự tắt) để bạn đọc kỹ hộp giải thích chi tiết (từ loại tiếng Việt/Anh, phiên âm IPA, nghĩa chính, nghĩa khác, câu ví dụ). Sau khi trả lời, nút \"Nghe\" cũng tự động mở lại ở mọi chế độ (kể cả Việt→Anh / Cloze) và từ tiếng Anh được hiển thị rõ ở chế độ Nghe→Việt để bạn đối chiếu lại. Khi đọc xong, chạm vào vùng tối bên ngoài thẻ (hoặc bấm nút Quay lại) để đóng (không ghi thêm lượt Thoát nhanh).")
    add_bullet(doc, "Khi bạn đang bận (Thoát nhanh 1 chạm trước khi trả lời): ", "Chạm vào BẤT KỲ vùng nào ngoài 4 ô đáp án và nút \"Nghe\" (nền mờ xung quanh, tiêu đề từ, khoảng trống trong thẻ...) hoặc bấm Quay lại. Thẻ tắt ngay lập tức, chỉ ghi nhận \"Thoát nhanh\" (DISMISSED) và hoàn toàn KHÔNG bị phạt điểm SM-2.")

    add_h2(doc, "7.2. Kiểm soát tần suất & Trải nghiệm không gây phiền (Tab Trang chủ)")
    add_bullet(doc, "Tần suất kích hoạt khi mở khóa: ", "Tùy chọn 1 trong 3 chế độ: \"Mỗi lần mở khóa\" (hiện mỗi lần mở máy), \"Sau mỗi N lần mở khóa\" (chỉnh từ 1–20 lần), hoặc \"Cách tối thiểu X phút\" (chỉnh từ 1–180 phút).")
    add_bullet(doc, "Giới hạn/ngày & Giờ im lặng: ", "Stepper \"Giới hạn/ngày\" (chọn Vô hạn hoặc bước 5 câu/ngày) hiển thị kèm số câu đã trả lời hôm nay; bật công tắc \"Giờ im lặng ban đêm (23h – 6h)\" để không hiện thẻ vào ban đêm; app cũng tự động không bật thẻ khi bạn đang có cuộc gọi điện thoại.")
    add_bullet(doc, "Tạm nghỉ nhanh (Snooze) & Tự động tạm nghỉ thông minh: ", "Bấm \"Tạm nghỉ 30 phút\" ở Trang chủ hoặc trực tiếp trên thanh thông báo dịch vụ nền khi cần tập trung; bấm \"Bật lại ngay\" bất kỳ lúc nào. Ngoài ra, nếu bạn Thoát nhanh >= 2 lần liên tiếp trong vòng 10 phút, Adaptive Snooze sẽ tự động tạm nghỉ 15 phút để tránh làm phiền.")
    add_bullet(doc, "An toàn âm thanh theo ngữ cảnh (Silent-aware TTS): ", "App tự kiểm tra chế độ chuông, mức âm lượng Media và tai nghe/Bluetooth. Khi điện thoại đang Im lặng / Rung hoặc tắt Media mà không đeo tai nghe, chế độ tự động không bật câu hỏi Nghe, và nếu bạn chọn tay \"Luyện nghe → Việt\" thì app không tự phát tiếng mà hiện dòng nhắc để bạn chủ động bấm \"Nghe\".")

    add_h2(doc, "7.3. 5 Tiêu chí chọn từ & 5 Kiểu câu hỏi Chống học vẹt (Tab Trang chủ)")
    add_bullet(doc, "5 Tiêu chí chọn từ hiển thị: ", "(1) Đến hạn ôn (SM-2) — ưu tiên từ đến lịch ôn trước rồi mới đến từ mới cùng trình độ; (2) Từ mới chưa học; (3) Từ hay sai / khó nhất; (4) Lọc theo từ loại / cấp độ (lọc theo Từ loại + CEFR A1–C2, có khung cảnh báo đỏ nếu bộ lọc hiện có 0 từ); (5) Trộn ngẫu nhiên.")
    add_bullet(doc, "5 Kiểu câu hỏi (Chống học vẹt) & Đáp án nhiễu thông minh: ", "Khuyên dùng \"Tự động theo SM-2\" (mới học hỏi Anh → Việt; lặp >= 2 tự đổi sang Việt → Anh hoặc Luyện nghe → Việt; lặp >= 3 tự chuyển sang Điền từ ngữ cảnh Cloze ____), hoặc chọn cố định từng kiểu. 3 đáp án nhiễu luôn ưu tiên cùng từ loại (pos) + cùng cấp độ CEFR và tự loại trừ các nghĩa trùng từ khóa.")

    add_h2(doc, "7.4. Tab Từ điển Offline 50.000 mục (Tra cứu Anh ↔ Việt độc lập)")
    add_bullet(doc, "Tra cứu 2 chiều tức thì: ", "Gõ từ tiếng Anh (ví dụ: look), cụm từ (go to) hoặc nghĩa tiếng Việt vào ô tìm kiếm để tra ngay trong 50.000 mục từ & 4.500 cụm từ / từ ghép offline; lọc nhanh theo từ loại (noun, verb, adj, adv, phrase) và CEFR (A1–C2).")
    add_bullet(doc, "Chi tiết nghĩa & Cụm từ liên quan: ", "Chạm vào bất kỳ mục nào để mở hộp thoại chi tiết gồm IPA, toàn bộ nghĩa tiếng Việt đánh số (1., 2., 3...), câu ví dụ song ngữ và danh sách \"Cụm từ & Từ liên quan\" (như look at, look in...). Chạm cụm từ liên quan để chuyển tiếp ngay, bấm \"Nghe\" để phát âm hoặc bấm \"+ Đưa vào kho học\" trong 1 chạm.")

    add_h2(doc, "7.5. Tab Thống kê — Ghi nhớ SM-2, Đối chiếu Ebbinghaus, Pre/Post-test & Xuất CSV")
    add_bullet(doc, "Chỉ số học tập & 4 mức độ ghi nhớ SM-2: ", "Theo dõi 6 thẻ chỉ số (Tỷ lệ nhớ %, Phản hồi TB ms, Thoát nhanh, Đã học, Trả lời đúng, Trả lời sai), thanh phân bố 4 mức SM-2 (Thuộc sâu >= 21d / Mới nhớ 6–20d / Đang học / Chưa học), biểu đồ cột thực nghiệm 7 ngày gần nhất, danh sách \"Top từ cần ôn kỹ (hay sai nhất)\" và \"Lịch sử gần nhất\".")
    add_bullet(doc, "Đối chiếu Đường cong quên Ebbinghaus (D1–D21): ", "Bảng so sánh trực tiếp tỷ lệ nhớ tự nhiên theo mốc D1 (≥1d: ~33%), D3 (≥3d: ~25%), D7 (≥6d: ~23%), D21 (≥21d: ~21%) với tỷ lệ nhớ thực tế theo thuật toán SM-2 của chính bạn.")
    add_bullet(doc, "Kiểm tra đối chứng Pre-test / Post-test (15 câu) & Tự kiểm thử lõi: ", "Bấm \"Làm Pre-test\" / \"Làm Post-test\" để làm bài 15 câu theo \"Tất cả\" hoặc từng cấp A1–C2, xem mức \"Tăng trưởng\" (+X%); bấm \"Tự kiểm thử lõi\" để chạy 11 bài test bất biến kiểm chứng SM-2, từ điển 50k, Cloze và bản quyền APK.")
    add_bullet(doc, "Xuất 3 tệp dữ liệu CSV phục vụ báo cáo KHKT: ", "Bấm \"Xuất log CSV\" (unlockvocab_review_logs.csv), \"Xuất kho từ CSV\" (unlockvocab_study_words.csv) hoặc \"Xuất KHKT Test CSV\" (unlockvocab_pre_post_tests.csv) để lưu và chia sẻ số liệu (mỗi tệp đều gắn cột origin_watermark xác thực nguồn gốc).")

    add_h2(doc, "7.6. Thẻ Bản quyền & Xác thực tác giả (Cuối tab Trang chủ)")
    add_bullet(doc, "Kiểm tra bản chính chủ: ", "Thẻ cuối Trang chủ hiển thị tác giả, mã gói gốc com.huy.unlockvocab • v1.5.2-khkt (Release R8), chữ ký SHA-256: 325F191DA7D70180 (Chính chủ • Đã xác thực SQLite & APK) và dòng ghi công nguồn dữ liệu mở. Nếu tệp APK bị đóng gói lại trái phép, ứng dụng sẽ hiện cảnh báo đỏ và khóa bật thẻ Overlay.")

    add_h1(doc, "8. Xử lý một số tình huống thường gặp (FAQ)")
    add_bullet(doc, "Làm sao đóng thẻ câu hỏi sau khi đã chọn đáp án và đọc xong giải thích? ", "Bạn chỉ cần chạm vào vùng nền tối bên ngoài khung thẻ trắng (phía trên, phía dưới hoặc hai bên lề màn hình) hoặc bấm nút Quay lại trên điện thoại. Chạm vào phần chữ hay hộp giải thích bên trong thẻ sẽ không làm thẻ đóng nhầm.")
    add_bullet(doc, "Tôi đã dùng \"- Xoá bớt từ\" để xoá hết các cấp cũ, liệu mở lại app có tự thêm lại không? ", "Không. Ứng dụng lưu dấu phiên bản khởi tạo nên những từ mặc định bạn đã xoá sẽ không bị nạp lại khi khởi động lại, và các từ bạn \"Tự thêm\" luôn được giữ nguyên 100%.")
    add_bullet(doc, "Mở khóa màn hình nhưng không thấy thẻ hiện lên? ", "Kiểm tra: (1) Công tắc \"Bật câu hỏi khi mở khóa\" đã BẬT chưa; (2) Quyền Overlay đã \"Đã cấp\" chưa; (3) Có đang trong chế độ \"Đang tạm nghỉ\" (bấm \"Bật lại ngay\"), \"Sau N lần\", \"Cách X phút\", \"Giới hạn/ngày\" hay \"Giờ im lặng 23h–6h\" không; (4) Đã tắt tối ưu pin Samsung cho app chưa.")
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
