#!/usr/bin/env python3
"""
Preprocesses open-license datasets in data_sources/ into two compact offline JSON assets:
  1. app/src/main/assets/vocab_seed.json    (600-800 core NGSL + CEFR-J words)
  2. app/src/main/assets/dict_compact.json  (8,000-10,000 lookup entries for offline autocomplete)
"""

import csv
import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
DATA_DIR = ROOT / "data_sources"
ASSETS_DIR = ROOT / "app" / "src" / "main" / "assets"

ASCII_WORD_RE = re.compile(r"^[a-z]{2,16}$")
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
            if word in ipa_map or not ASCII_WORD_RE.match(word):
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
            if ASCII_WORD_RE.match(headword) and headword not in ranks:
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
                    w = part.strip().lower()
                    if not ASCII_WORD_RE.match(w):
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
    # Strip repeated domain tags like "(chính trị) (thông tục) " at start, keeping content after.
    cleaned = text
    for _ in range(3):
        next_cleaned = LEADING_TAG_RE.sub("", cleaned).strip()
        if next_cleaned == cleaned or not next_cleaned:
            break
        cleaned = next_cleaned
    # Remove trailing ellipsis/semicolons and collapse whitespace.
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
    pos_buckets: dict[str, dict] = {}

    def flush_word() -> None:
        if not current_word or not ASCII_WORD_RE.match(current_word):
            return
        ipa = ipa_map.get(current_word) or current_ipa
        for pos, bucket in pos_buckets.items():
            meanings = bucket["meanings"]
            if not meanings:
                continue
            if (current_word, pos) in CURATED_PRIMARY_OVERRIDES:
                prim, extras = CURATED_PRIMARY_OVERRIDES[(current_word, pos)]
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
                    "pos": pos,
                    "ipa": ipa,
                    "meanings": all_meanings,
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
                    current_word = w_part.strip().lower()
                    current_ipa = "/" + ipa_part.strip().strip("/") + "/"
                else:
                    current_word = header.lower()
                    current_ipa = ""
                current_pos = "other"
                pos_buckets = {}
                continue

            if not current_word or not ASCII_WORD_RE.match(current_word):
                continue

            if line.startswith("*"):
                current_pos = normalize_dict_pos(line[1:])
                if current_pos not in pos_buckets:
                    pos_buckets[current_pos] = {"meanings": [], "example": ""}
            elif line.startswith("-"):
                cleaned = clean_meaning(line[1:])
                if cleaned:
                    bucket = pos_buckets.setdefault(current_pos, {"meanings": [], "example": ""})
                    if cleaned not in bucket["meanings"] and len(bucket["meanings"]) < 4:
                        bucket["meanings"].append(cleaned)
            elif line.startswith("=") and "+" in line:
                bucket = pos_buckets.setdefault(current_pos, {"meanings": [], "example": ""})
                if not bucket["example"]:
                    ex_part = line[1:].split("+", 1)
                    en = ex_part[0].strip()
                    vi = ex_part[1].strip()
                    if en and vi and len(en) + len(vi) <= 115:
                        bucket["example"] = f"{en} — {vi}"

    flush_word()
    return entries


def main() -> None:
    ASSETS_DIR.mkdir(parents=True, exist_ok=True)
    ipa_map = load_ipa()
    ngsl_ranks = load_ngsl_ranks()
    cefr_by_wp, cefr_best = load_cefr_profiles()
    raw_entries = parse_anhviet(ipa_map)

    # Enrich entries with CEFR and NGSL rank, deduplicating (word, pos).
    dedup: dict[tuple[str, str], dict] = {}
    for item in raw_entries:
        w = item["word"]
        pos = item["pos"]
        exact_cefr = cefr_by_wp.get((w, pos), "")
        cefr = exact_cefr or cefr_best.get(w) or ""
        rank = ngsl_ranks.get(w, 999999)
        key = (w, pos)
        enriched = {
            "word": w,
            "pos": pos,
            "ipa": item["ipa"],
            "cefr": cefr,
            "exactCefr": exact_cefr,
            "rank": rank,
            "meanings": item["meanings"],
            "example": item["example"],
        }
        if key not in dedup:
            dedup[key] = enriched

    all_entries = list(dedup.values())

    # 1) Build vocab_seed.json: 700 core NGSL + exact CEFR-J (A1-B2) entries in noun/verb/adj/adv.
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
        if e["word"] in skip_seed_words:
            continue
        if e["pos"] not in CORE_POS:
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

    # 2) Build dict_compact.json: up to 9,500 entries prioritizing NGSL, CEFR-J, then alphabetical common words.
    def dict_sort_key(e: dict) -> tuple:
        has_ngsl = 0 if e["rank"] < 999999 else 1
        has_cefr = 0 if e["cefr"] else 1
        return (has_ngsl, has_cefr, e["rank"], len(e["word"]), e["word"], e["pos"])

    compact_dict = []
    for e in sorted(all_entries, key=dict_sort_key):
        compact_dict.append(
            {
                "word": e["word"],
                "pos": e["pos"],
                "ipa": e["ipa"],
                "cefr": e["cefr"],
                "meanings": [shorten_primary(e["meanings"][0], 56)] + e["meanings"][1:3],
                "example": e["example"],
            }
        )
        if len(compact_dict) >= 9500:
            break

    # Sort compact_dict alphabetically by word for clean inspection & fast loading.
    compact_dict.sort(key=lambda x: (x["word"], x["pos"]))

    seed_path = ASSETS_DIR / "vocab_seed.json"
    dict_path = ASSETS_DIR / "dict_compact.json"

    with seed_path.open("w", encoding="utf-8") as f:
        json.dump(seed_candidates, f, ensure_ascii=False, separators=(",", ":"))
    with dict_path.open("w", encoding="utf-8") as f:
        json.dump(compact_dict, f, ensure_ascii=False, separators=(",", ":"))

    print(f"vocab_seed.json: {len(seed_candidates)} entries ({seed_path.stat().st_size / 1024:.1f} KB)")
    print(f"dict_compact.json: {len(compact_dict)} entries ({dict_path.stat().st_size / 1024:.1f} KB)")
    sorry_items = [x for x in seed_candidates if x["word"] == "sorry"]
    print(f"sample 'sorry' in seed: {sorry_items}")


if __name__ == "__main__":
    main()
