#!/usr/bin/env python3
"""
Preprocesses open-license datasets in data_sources/ into two compact offline JSON assets:
  1. app/src/main/assets/vocab_seed.json    (~755 core NGSL + CEFR-J words & common phrases)
  2. app/src/main/assets/dict_compact.json  (~11,500 lookup entries including phrases for offline autocomplete)
"""

import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT / "data_sources"
ASSETS_DIR = ROOT / "app" / "src" / "main" / "assets"

WORD_OR_PHRASE_RE = re.compile(r"^[a-z]{2,15}(?:[\s\-][a-z]{2,15}){0,2}$")
SINGLE_WORD_RE = re.compile(r"^[a-z]{2,16}$")
LEADING_TAG_RE = re.compile(r"^\([^)]{1,40}\)\s*")

CEFR_ORDER = {"A1": 1, "A2": 2, "B1": 3, "B2": 4, "C1": 5, "C2": 6}
CORE_POS = {"noun", "verb", "adj", "adv"}

# Curated overrides to ensure everyday words have concise, learner-friendly primary meanings.
CURATED_PRIMARY_OVERRIDES = {
    ("sorry", "adj"): ("xin lỗi", ["lấy làm tiếc", "lấy làm buồn", "đáng buồn"]),
    ("say", "verb"): ("nói, bảo", ["phát biểu", "cho biết"]),
    ("hello", "other"): ("xin chào", ["lời chào"]),
    ("delicious", "adj"): ("ngon", ["thơm ngon", "ngon miệng"]),
    ("good", "adj"): ("tốt", ["hay", "giỏi", "tuyệt"]),
    ("bad", "adj"): ("xấu", ["tệ", "dở"]),
    ("happy", "adj"): ("vui vẻ", ["hạnh phúc", "sung sướng"]),
    ("important", "adj"): ("quan trọng", ["trọng yếu", "cần thiết"]),
    ("remember", "verb"): ("nhớ", ["ghi nhớ", "nhớ lại"]),
    ("forget", "verb"): ("quên", ["bỏ quên", "sao nhãng"]),
    ("learn", "verb"): ("học", ["học hỏi", "tìm hiểu"]),
}

# Essential compound words, phrasal verbs, and everyday multi-word expressions.
CURATED_PHRASES = [
    ("go to", "phrase", "/ɡoʊ tuː/", "A1", ["đi đến, đi tới", "đi vào", "chuyển sang"], "I go to school every day — tôi đi học mỗi ngày"),
    ("look after", "phrase", "/lʊk ˈæftər/", "A2", ["chăm sóc", "trông nom"], "She looks after her little brother — cô ấy chăm sóc em trai"),
    ("look for", "phrase", "/lʊk fɔːr/", "A1", ["tìm kiếm", "tìm"], "I am looking for my keys — tôi đang tìm chìa khóa"),
    ("look up", "phrase", "/lʊk ʌp/", "A2", ["tra cứu", "tìm thông tin"], "Look up new words in the dictionary — tra từ mới trong từ điển"),
    ("look forward to", "phrase", "/lʊk ˈfɔːrwərd tuː/", "B1", ["mong đợi", "trông mong"], "I look forward to seeing you — tôi mong được gặp bạn"),
    ("give up", "phrase", "/ɡɪv ʌp/", "A2", ["từ bỏ", "bỏ cuộc"], "Never give up on your dream — đừng bao giờ từ bỏ ước mơ"),
    ("give back", "phrase", "/ɡɪv bæk/", "A2", ["trả lại", "hoàn trả"], "Please give back my pen — làm ơn trả lại bút cho tôi"),
    ("take off", "phrase", "/teɪk ɔːf/", "A2", ["cất cánh", "cởi ra"], "The plane takes off at 7 — máy bay cất cánh lúc 7 giờ"),
    ("take care of", "phrase", "/teɪk kɛr ʌv/", "A2", ["chăm sóc", "lo liệu"], "Take care of yourself — hãy tự chăm sóc bản thân"),
    ("take part in", "phrase", "/teɪk pɑːrt ɪn/", "B1", ["tham gia"], "Many students take part in the contest — nhiều học sinh tham gia cuộc thi"),
    ("turn on", "phrase", "/tɜːrn ɑːn/", "A1", ["bật lên", "mở (đèn/máy)"], "Turn on the light please — vui lòng bật đèn lên"),
    ("turn off", "phrase", "/tɜːrn ɔːf/", "A1", ["tắt đi", "ngắt"], "Remember to turn off the TV — nhớ tắt tivi nhé"),
    ("get up", "phrase", "/ɡɛt ʌp/", "A1", ["thức dậy", "đứng dậy"], "I get up at six — tôi thức dậy lúc sáu giờ"),
    ("wake up", "phrase", "/weɪk ʌp/", "A1", ["tỉnh giấc", "đánh thức"], "Wake up early tomorrow — ngày mai dậy sớm nhé"),
    ("get along", "phrase", "/ɡɛt əˈlɔːŋ/", "B1", ["hòa thuận", "hợp nhau"], "They get along very well — họ rất hòa thuận với nhau"),
    ("get over", "phrase", "/ɡɛt ˈoʊvər/", "B1", ["vượt qua", "khỏi bệnh"], "He got over the difficulty — anh ấy đã vượt qua khó khăn"),
    ("find out", "phrase", "/faɪnd aʊt/", "A2", ["tìm ra", "phát hiện"], "We need to find out the truth — chúng ta cần tìm ra sự thật"),
    ("figure out", "phrase", "/ˈfɪɡjər aʊt/", "B1", ["hiểu ra", "giải quyết được"], "I finally figured out the problem — cuối cùng tôi đã hiểu ra vấn đề"),
    ("depend on", "phrase", "/dɪˈpɛnd ɑːn/", "A2", ["phụ thuộc vào", "tùy thuộc"], "It depends on the weather — còn tùy thuộc vào thời tiết"),
    ("deal with", "phrase", "/diːl wɪð/", "B1", ["giải quyết", "đối phó với"], "How do you deal with stress? — bạn giải quyết căng thẳng thế nào?"),
    ("according to", "phrase", "/əˈkɔːrdɪŋ tuː/", "B1", ["theo như", "căn cứ vào"], "According to the news, it will rain — theo tin tức thì trời sẽ mưa"),
    ("because of", "phrase", "/bɪˈkɔːz ʌv/", "A2", ["bởi vì", "do"], "We stayed home because of the rain — chúng tôi ở nhà vì trời mưa"),
    ("in order to", "phrase", "/ɪn ˈɔːrdər tuː/", "B1", ["để mà", "nhằm mục đích"], "Study hard in order to succeed — học chăm để thành công"),
    ("as well as", "phrase", "/æz wɛl æz/", "B1", ["cũng như", "và cả"], "She speaks English as well as French — cô ấy nói tiếng Anh cũng như tiếng Pháp"),
    ("make sure", "phrase", "/meɪk ʃʊr/", "A2", ["đảm bảo", "chắc chắn rằng"], "Make sure you lock the door — nhớ đảm bảo đã khóa cửa"),
    ("run out of", "phrase", "/rʌn aʊt ʌv/", "B1", ["hết sạch", "cạn kiệt"], "We ran out of time — chúng tôi đã hết giờ"),
    ("pick up", "phrase", "/pɪk ʌp/", "A2", ["đón ai đó", "nhặt lên", "học lỏm"], "I will pick you up at five — tôi sẽ đón bạn lúc năm giờ"),
    ("set up", "phrase", "/sɛt ʌp/", "B1", ["thiết lập", "thành lập"], "Let's set up a study plan — hãy thiết lập kế hoạch học tập"),
    ("work out", "phrase", "/wɜːrk aʊt/", "B1", ["tập thể dục", "tìm ra cách"], "I work out three times a week — tôi tập thể dục ba lần một tuần"),
    ("put off", "phrase", "/pʊt ɔːf/", "B1", ["trì hoãn", "hoãn lại"], "Don't put off today's work — đừng trì hoãn việc hôm nay"),
    ("put on", "phrase", "/pʊt ɑːn/", "A1", ["mặc vào", "đội lên"], "Put on your coat — mặc áo khoác vào đi"),
    ("carry on", "phrase", "/ˈkæri ɑːn/", "B1", ["tiếp tục"], "Carry on with your work — hãy tiếp tục công việc của bạn"),
    ("come back", "phrase", "/kʌm bæk/", "A1", ["quay trở lại", "về"], "When will you come back? — khi nào bạn quay lại?"),
    ("come across", "phrase", "/kʌm əˈkrɔːs/", "B2", ["tình cờ gặp", "bắt gặp"], "I came across an old friend — tôi tình cờ gặp một người bạn cũ"),
    ("go on", "phrase", "/ɡoʊ ɑːn/", "A2", ["tiếp tục", "diễn ra"], "Please go on reading — xin hãy tiếp tục đọc"),
    ("go out", "phrase", "/ɡoʊ aʊt/", "A1", ["ra ngoài", "đi chơi"], "Let's go out tonight — tối nay đi chơi nhé"),
    ("hold on", "phrase", "/hoʊld ɑːn/", "A2", ["chờ chút", "giữ máy"], "Hold on a second — chờ một chút nhé"),
    ("hang out", "phrase", "/hæŋ aʊt/", "B1", ["đi chơi, tụ tập bạn bè"], "We often hang out on weekends — cuối tuần chúng tôi hay đi chơi cùng nhau"),
    ("break down", "phrase", "/breɪk daʊn/", "B1", ["bị hỏng (xe/máy)", "suy sụp"], "My bike broke down — xe đạp của tôi bị hỏng"),
    ("bring up", "phrase", "/brɪŋ ʌp/", "B1", ["nuôi dưỡng", "đề cập tới"], "She brought up an interesting idea — cô ấy nêu ra một ý tưởng thú vị"),
    ("call off", "phrase", "/kɔːl ɔːf/", "B2", ["hủy bỏ"], "They called off the meeting — họ đã hủy cuộc họp"),
    ("check in", "phrase", "/tʃɛk ɪn/", "A2", ["làm thủ tục nhận phòng/vé"], "We checked in at the hotel — chúng tôi đã nhận phòng khách sạn"),
    ("check out", "phrase", "/tʃɛk aʊt/", "A2", ["trả phòng", "kiểm tra thử"], "Check out this new app — hãy thử xem ứng dụng mới này"),
    ("clean up", "phrase", "/kliːn ʌp/", "A2", ["dọn dẹp sạch sẽ"], "Clean up your room — hãy dọn dẹp phòng bạn đi"),
    ("count on", "phrase", "/kaʊnt ɑːn/", "B1", ["tin cậy vào", "trông cậy"], "You can count on me — bạn có thể tin cậy ở tôi"),
    ("cut down", "phrase", "/kʌt daʊn/", "B1", ["cắt giảm", "giảm bớt"], "Cut down on sugar — hãy giảm bớt đường"),
    ("fill in", "phrase", "/fɪl ɪn/", "A2", ["điền vào (mẫu đơn)"], "Fill in this form — hãy điền vào mẫu đơn này"),
    ("grow up", "phrase", "/ɡroʊ ʌp/", "A2", ["lớn lên", "trưởng thành"], "I grew up in a small town — tôi lớn lên ở một thị trấn nhỏ"),
    ("keep on", "phrase", "/kiːp ɑːn/", "B1", ["tiếp tục làm gì"], "Keep on trying — hãy tiếp tục cố gắng"),
    ("point out", "phrase", "/pɔɪnt aʊt/", "B1", ["chỉ ra", "vạch ra"], "He pointed out my mistake — anh ấy đã chỉ ra lỗi của tôi"),
    ("show up", "phrase", "/ʃoʊ ʌp/", "B1", ["xuất hiện", "có mặt"], "He showed up on time — anh ấy đã có mặt đúng giờ"),
    ("slow down", "phrase", "/sloʊ daʊn/", "A2", ["chậm lại", "giảm tốc"], "Slow down when driving — hãy lái xe chậm lại"),
    ("throw away", "phrase", "/θroʊ əˈweɪ/", "A2", ["vứt bỏ", "ném đi"], "Don't throw away old books — đừng vứt bỏ sách cũ"),
    ("try on", "phrase", "/traɪ ɑːn/", "A2", ["mặc thử (quần áo)"], "Can I try on this shirt? — tôi có thể mặc thử chiếc áo này không?"),
    ("used to", "phrase", "/juːzd tuː/", "A2", ["đã từng (trong quá khứ)"], "I used to play football — tôi đã từng chơi bóng đá"),
]


def normalize_cefr_pos(raw_pos: str) -> str:
    p = raw_pos.strip().lower()
    if p in ("noun", "n"):
        return "noun"
    if p in ("verb", "v", "be-verb", "do-verb", "have-verb", "modal auxiliary"):
        return "verb"
    if p in ("adjective", "adj"):
        return "adj"
    if p in ("adverb", "adv"):
        return "adv"
    if p in ("preposition", "prep"):
        return "prep"
    return "other"


def normalize_dict_pos(line: str) -> str:
    low = line.lower()
    if "danh từ" in low:
        return "noun"
    if "động từ" in low:
        return "verb"
    if "tính từ" in low:
        return "adj"
    if "phó từ" in low or "trạng từ" in low:
        return "adv"
    if "giới từ" in low:
        return "prep"
    return "other"


def load_ipa() -> dict[str, str]:
    ipa_map: dict[str, str] = {}
    path = DATA_DIR / "ipa-en_US.txt"
    with path.open(encoding="utf-8") as f:
        for line in f:
            parts = line.rstrip("\r\n").split("\t")
            if len(parts) < 2:
                continue
            word = parts[0].strip().lower()
            if word in ipa_map or not SINGLE_WORD_RE.match(word):
                continue
            ipa = parts[1].split(",")[0].strip()
            if ipa:
                ipa_map[word] = ipa
    return ipa_map


def load_ngsl_ranks() -> dict[str, int]:
    ranks: dict[str, int] = {}
    path = DATA_DIR / "NGSL-1.01.csv"
    with path.open(encoding="utf-8") as f:
        reader = csv.reader(f)
        for idx, row in enumerate(reader, start=1):
            if not row:
                continue
            headword = row[0].strip().lower()
            if SINGLE_WORD_RE.match(headword) and headword not in ranks:
                ranks[headword] = idx
    return ranks


def load_cefr_profiles() -> tuple[dict[tuple[str, str], str], dict[str, str]]:
    by_word_pos: dict[tuple[str, str], str] = {}
    by_word_best: dict[str, str] = {}

    for filename in (
        "cefrj-vocabulary-profile-1.5.csv",
        "octanove-vocabulary-profile-c1c2-1.0.csv",
    ):
        path = DATA_DIR / filename
        with path.open(encoding="utf-8") as f:
            reader = csv.DictReader(f)
            for row in reader:
                cefr = (row.get("CEFR") or "").strip().upper()
                if cefr not in CEFR_ORDER:
                    continue
                pos = normalize_cefr_pos(row.get("pos") or "")
                raw_hw = (row.get("headword") or "").strip()
                for part in raw_hw.split("/"):
                    w = re.sub(r"\s+", " ", part.strip().lower())
                    if not WORD_OR_PHRASE_RE.match(w):
                        continue
                    key = (w, pos)
                    if key not in by_word_pos or CEFR_ORDER[cefr] < CEFR_ORDER[by_word_pos[key]]:
                        by_word_pos[key] = cefr
                    if w not in by_word_best or CEFR_ORDER[cefr] < CEFR_ORDER[by_word_best[w]]:
                        by_word_best[w] = cefr

    return by_word_pos, by_word_best


def clean_meaning(raw: str) -> str:
    text = raw.strip()
    is_archaic = text.startswith("(từ cổ") or text.startswith("(từ hiếm") or text.startswith("(cũ)")
    if is_archaic:
        return ""
    cleaned = text
    for _ in range(3):
        next_cleaned = LEADING_TAG_RE.sub("", cleaned).strip()
        if next_cleaned == cleaned or not next_cleaned:
            break
        cleaned = next_cleaned
    cleaned = re.sub(r"\s+", " ", cleaned).strip(" ;,.")
    if not cleaned:
        return ""
    return cleaned


def shorten_primary(meaning: str, max_len: int = 48) -> str:
    if len(meaning) <= max_len:
        return meaning
    for sep in (";", ","):
        if sep in meaning:
            first = meaning.split(sep)[0].strip()
            if 2 <= len(first) <= max_len:
                return first
    if " (" in meaning:
        head = meaning.split(" (")[0].strip()
        if 2 <= len(head) <= max_len:
            return head
    return meaning[: max_len - 1].rstrip() + "…"


def parse_anhviet(ipa_map: dict[str, str]) -> list[dict]:
    entries: list[dict] = []
    path = DATA_DIR / "anhviet109K.txt"

    current_word = ""
    current_ipa = ""
    current_pos = "other"
    in_idiom_block = False
    current_phrase = ""
    pos_buckets: dict[str, dict] = {}
    phrase_buckets: dict[str, dict] = {}

    def flush_word() -> None:
        if not current_word or not WORD_OR_PHRASE_RE.match(current_word):
            return
        ipa = ipa_map.get(current_word) or current_ipa
        is_compound = " " in current_word or "-" in current_word
        for pos, bucket in pos_buckets.items():
            meanings = bucket["meanings"]
            if not meanings:
                continue
            out_pos = "phrase" if (is_compound and pos == "other") else pos
            if (current_word, out_pos) in CURATED_PRIMARY_OVERRIDES:
                prim, extras = CURATED_PRIMARY_OVERRIDES[(current_word, out_pos)]
                all_meanings = [prim] + [m for m in extras if m != prim]
            else:
                sorted_meanings = sorted(
                    meanings,
                    key=lambda m: 1 if re.match(r"^[a-zA-Z]{2,}\s+[a-zA-Z]{2,}", m) else 0,
                )
                all_meanings = sorted_meanings[:4]
            entries.append(
                {
                    "word": current_word,
                    "pos": out_pos,
                    "ipa": ipa,
                    "meanings": all_meanings,
                    "example": bucket["example"],
                }
            )
        for phr, bucket in phrase_buckets.items():
            if bucket["meanings"]:
                entries.append(
                    {
                        "word": phr,
                        "pos": "phrase",
                        "ipa": "",
                        "meanings": bucket["meanings"][:4],
                        "example": bucket["example"],
                    }
                )

    with path.open(encoding="utf-8") as f:
        for raw_line in f:
            line = raw_line.rstrip("\r\n")
            if line.startswith("@"):
                flush_word()
                header = line[1:].strip()
                if " /" in header:
                    w_part, ipa_part = header.split(" /", 1)
                    current_word = re.sub(r"\s+", " ", w_part.strip().lower())
                    current_ipa = "/" + ipa_part.strip().strip("/") + "/"
                else:
                    current_word = re.sub(r"\s+", " ", header.lower())
                    current_ipa = ""
                current_pos = "phrase" if " " in current_word else "other"
                in_idiom_block = False
                current_phrase = ""
                pos_buckets = {}
                phrase_buckets = {}
                continue

            if not current_word or not WORD_OR_PHRASE_RE.match(current_word):
                continue

            if line.startswith("!"):
                in_idiom_block = True
                raw_phr = re.sub(r"^to\s+", "", line[1:].strip().lower())
                raw_phr = re.sub(r"\s+", " ", raw_phr)
                if WORD_OR_PHRASE_RE.match(raw_phr) and " " in raw_phr:
                    current_phrase = raw_phr
                    phrase_buckets.setdefault(current_phrase, {"meanings": [], "example": ""})
                else:
                    current_phrase = ""
            elif line.startswith("*"):
                in_idiom_block = False
                current_phrase = ""
                current_pos = normalize_dict_pos(line[1:])
                if current_pos not in pos_buckets:
                    pos_buckets[current_pos] = {"meanings": [], "example": ""}
            elif line.startswith("-"):
                cleaned = clean_meaning(line[1:])
                if cleaned:
                    if in_idiom_block:
                        if current_phrase:
                            pb = phrase_buckets[current_phrase]
                            if cleaned not in pb["meanings"] and len(pb["meanings"]) < 4:
                                pb["meanings"].append(cleaned)
                    else:
                        bucket = pos_buckets.setdefault(current_pos, {"meanings": [], "example": ""})
                        if cleaned not in bucket["meanings"] and len(bucket["meanings"]) < 4:
                            bucket["meanings"].append(cleaned)
            elif line.startswith("=") and "+" in line:
                ex_part = line[1:].split("+", 1)
                en = ex_part[0].strip()
                vi = ex_part[1].strip()
                if en and vi and len(en) + len(vi) <= 115:
                    ex_str = f"{en} — {vi}"
                    if in_idiom_block:
                        if current_phrase and not phrase_buckets[current_phrase]["example"]:
                            phrase_buckets[current_phrase]["example"] = ex_str
                    else:
                        bucket = pos_buckets.setdefault(current_pos, {"meanings": [], "example": ""})
                        if not bucket["example"]:
                            bucket["example"] = ex_str

    flush_word()
    return entries


def main() -> None:
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    ipa_map = load_ipa()
    ngsl_ranks = load_ngsl_ranks()
    cefr_by_wp, cefr_best = load_cefr_profiles()
    raw_entries = parse_anhviet(ipa_map)

    dedup: dict[tuple[str, str], dict] = {}

    # Insert curated common phrases first so they always appear cleanly in both assets.
    for idx, (phr, pos, ipa, cefr, meanings, example) in enumerate(CURATED_PHRASES, start=1):
        dedup[(phr, pos)] = {
            "word": phr,
            "pos": pos,
            "ipa": ipa,
            "cefr": cefr,
            "exactCefr": cefr,
            "rank": 3000 + idx,
            "meanings": meanings,
            "example": example,
        }

    for item in raw_entries:
        w = item["word"]
        pos = item["pos"]
        exact_cefr = cefr_by_wp.get((w, pos), "")
        cefr = exact_cefr or cefr_best.get(w) or ""
        rank = ngsl_ranks.get(w, 999999)
        key = (w, pos)
        if key not in dedup:
            dedup[key] = {
                "word": w,
                "pos": pos,
                "ipa": item["ipa"],
                "cefr": cefr,
                "exactCefr": exact_cefr,
                "rank": rank,
                "meanings": item["meanings"],
                "example": item["example"],
            }

    all_entries = list(dedup.values())

    # 1) Build vocab_seed.json: 700 core NGSL words + curated common phrases.
    skip_seed_words = {
        "the", "and", "of", "to", "in", "it", "is", "be", "are", "was", "were",
        "that", "this", "for", "on", "with", "as", "by", "at", "from", "or", "an",
        "not", "but", "have", "has", "had", "do", "does", "did", "will", "would",
        "can", "could", "we", "they", "you", "he", "she", "his", "her", "their",
    }
    seed_candidates = []
    seen_seed_words: set[str] = set()

    def seed_sort_key(e: dict) -> tuple:
        cefr_score = CEFR_ORDER.get(e["exactCefr"], 9)
        pos_pref = 0 if (e["word"] == "sorry" and e["pos"] == "adj") else 1
        return (pos_pref, 0 if e["rank"] < 999999 else 1, e["rank"], cefr_score, e["word"])

    for e in sorted(all_entries, key=seed_sort_key):
        if e["word"] in skip_seed_words or e["pos"] not in CORE_POS:
            continue
        if e["exactCefr"] not in ("A1", "A2", "B1", "B2"):
            continue
        if e["rank"] == 999999 and e["exactCefr"] not in ("A1", "A2"):
            continue
        if e["word"] in seen_seed_words:
            continue
        prim = shorten_primary(e["meanings"][0])
        if len(prim) < 2:
            continue
        seen_seed_words.add(e["word"])
        extras = [m for m in e["meanings"][1:] if m != prim]
        if prim != e["meanings"][0]:
            extras.insert(0, e["meanings"][0])
        seed_candidates.append(
            {
                "word": e["word"],
                "pos": e["pos"],
                "ipa": e["ipa"],
                "cefr": e["exactCefr"],
                "topic": f"NGSL-{e['exactCefr']}",
                "primaryMeaning": prim,
                "extraMeanings": extras[:3],
                "example": e["example"],
            }
        )
        if len(seed_candidates) >= 700:
            break

    # Append curated phrases (go to, look for, give up, ...)
    for phr, pos, ipa, cefr, meanings, example in CURATED_PHRASES:
        if phr in seen_seed_words:
            continue
        seen_seed_words.add(phr)
        seed_candidates.append(
            {
                "word": phr,
                "pos": pos,
                "ipa": ipa,
                "cefr": cefr,
                "topic": f"Phrases-{cefr}",
                "primaryMeaning": meanings[0],
                "extraMeanings": meanings[1:],
                "example": example,
            }
        )

    # 2) Build dict_compact.json: up to 10,500 core words + up to 1,500 common compound words & phrases.
    def dict_sort_key(e: dict) -> tuple:
        has_ngsl = 0 if e["rank"] < 999999 else 1
        has_cefr = 0 if e["cefr"] else 1
        return (has_ngsl, has_cefr, e["rank"], len(e["word"]), e["word"], e["pos"])

    single_entries = []
    phrase_entries = []
    for e in sorted(all_entries, key=dict_sort_key):
        entry_obj = {
            "word": e["word"],
            "pos": e["pos"],
            "ipa": e["ipa"],
            "cefr": e["cefr"],
            "meanings": [shorten_primary(e["meanings"][0], 56)] + e["meanings"][1:3],
            "example": e["example"],
        }
        if (" " in e["word"] or "-" in e["word"] or e["pos"] == "phrase"):
            if len(phrase_entries) < 1500:
                phrase_entries.append(entry_obj)
        elif len(single_entries) < 10500:
            single_entries.append(entry_obj)
        if len(single_entries) >= 10500 and len(phrase_entries) >= 1500:
            break

    compact_dict = single_entries + phrase_entries

    compact_dict.sort(key=lambda x: (x["word"], x["pos"]))

    seed_path = ASSETS_DIR / "vocab_seed.json"
    dict_path = ASSETS_DIR / "dict_compact.json"

    with seed_path.open("w", encoding="utf-8") as f:
        json.dump(seed_candidates, f, ensure_ascii=False, separators=(",", ":"))
    with dict_path.open("w", encoding="utf-8") as f:
        json.dump(compact_dict, f, ensure_ascii=False, separators=(",", ":"))

    print(f"vocab_seed.json: {len(seed_candidates)} entries ({seed_path.stat().st_size / 1024:.1f} KB)")
    print(f"dict_compact.json: {len(compact_dict)} entries ({dict_path.stat().st_size / 1024:.1f} KB)")
    print("sample 'go to' in seed:", [x for x in seed_candidates if x["word"] == "go to"])
    print("sample 'go to' in dict:", [x for x in compact_dict if x["word"] == "go to"])
    print("total phrases in dict:", sum(1 for x in compact_dict if " " in x["word"] or "-" in x["word"]))


if __name__ == "__main__":
    main()
