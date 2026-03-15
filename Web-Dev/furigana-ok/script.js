/**
 * ================================================================
 *  Japanese Learning Tool — script.js (v9 — performance)
 *
 *  PERFORMANCE IMPROVEMENTS IN THIS VERSION:
 *
 *  OPT 1 — Background preload on «Add Furigana» click (§5a)
 *    After tokenization, all unique hoverable words are looked up
 *    in the background via preloadDictionary(tokens).
 *    Requests are batched (5 at a time, 150ms apart) to avoid
 *    hammering the proxy. By the time the user moves the mouse,
 *    most words are already cached → instant tooltip.
 *
 *  OPT 2 — Instant display from cache (§6 showTooltip)
 *    If dictionaryCache[surface] already exists when hovering,
 *    the English meaning is filled in immediately (no spinner,
 *    no "…"). The tooltip appears fully populated at once.
 *
 *  OPT 3 — 300ms hover delay (§6 handleMouseEnter)
 *    A setTimeout of 300ms fires before any API call starts.
 *    If the user moves off the word within 300ms, the timer is
 *    cancelled (clearTimeout in handleMouseLeave). This prevents
 *    pointless API calls when scanning over text quickly.
 *    Cache hits bypass the delay entirely — they show instantly.
 *
 *  OPT 4 — Deduplication in preload (§5a)
 *    Multiple occurrences of the same surface form (e.g. 日本 ×2)
 *    are collapsed to a single lookup key. Each unique word is
 *    looked up exactly once per session.
 *
 *  PRESERVED UNCHANGED:
 *  §2  Kuromoji tokenizer
 *  §3  Katakana → Hiragana
 *  §4  Furigana HTML builder (Kanji + Katakana + jp-plain)
 *  §7  Dictionary lookup (3-form chain + 2 CORS proxy fallback)
 *  §8  kanjiapi.dev fallback
 *  §9  POS translation
 *  §10 TTS (Web Speech API)
 *  §11 Translate button
 *  §13 UI helpers + utilities
 * ================================================================
 */
"use strict";

/* ================================================================
   §0  DOM REFERENCES
   ================================================================ */
const inputText    = document.getElementById("inputText");
const outputArea   = document.getElementById("outputArea");
const outputLabel  = document.getElementById("outputLabel");
const outputBadge  = document.getElementById("outputBadge");
const charCount    = document.getElementById("charCount");
const btnFurigana  = document.getElementById("btnFurigana");
const btnTranslate = document.getElementById("btnTranslate");
const btnClear     = document.getElementById("btnClear");
const loadingBar   = document.getElementById("loadingBar");
const loadingFill  = loadingBar.querySelector(".loading-bar__fill");
const toast        = document.getElementById("toast");

/* Tooltip DOM nodes */
const tooltip        = document.getElementById("tooltip");
const tooltipWord    = document.getElementById("tooltipWord");
const tooltipReading = document.getElementById("tooltipReading");
const tooltipEnglish = document.getElementById("tooltipEnglish");
const tooltipPOS     = document.getElementById("tooltipPOS");
const tooltipSpinner = document.getElementById("tooltipSpinner");

/* Vocabulary save popup DOM nodes */
const vocabPopup     = document.getElementById("vocabPopup");
const vocabPopupClose= document.getElementById("vocabPopupClose");
const vpWord         = document.getElementById("vpWord");
const vpReading      = document.getElementById("vpReading");
const vpMeaning      = document.getElementById("vpMeaning");
const vpSaveBtn      = document.getElementById("vpSaveBtn");
const vpSaveLabel    = document.getElementById("vpSaveLabel");
const vpSaveIcon     = document.getElementById("vpSaveIcon");

/* Saved vocabulary panel DOM nodes */
const vocabPanel     = document.getElementById("vocabPanel");
const vocabCountEl   = document.getElementById("vocabCount");
const vocabList      = document.getElementById("vocabList");
const btnClearVocab  = document.getElementById("btnClearVocab");

/* ================================================================
   §1  CONSTANTS
   ================================================================ */
const KUROMOJI_CDN  = "https://cdn.jsdelivr.net/npm/kuromoji@0.1.2/build/kuromoji.js";
const KUROMOJI_DICT = "https://cdn.jsdelivr.net/npm/kuromoji@0.1.2/dict/";
const JISHO_BASE    = "https://jisho.org/api/v1/search/words";
const KANJIAPI_BASE = "https://kanjiapi.dev/v1/kanji/";
const TRANSLATE_API = "https://api.mymemory.translated.net/get";

/* BUG C FIX: Two CORS proxy URLs tried in sequence */
const PROXY_PRIMARY   = "https://api.allorigins.win/get?url=";
const PROXY_SECONDARY = "https://corsproxy.io/?";

/* ── Google Sheets integration ───────────────────────────────────
   Deploy a Google Apps Script Web App and paste its URL here.
   See the Apps Script code provided separately (google-apps-script.js).
   Leave as empty string "" to disable Sheets saving (local-only mode).
   ────────────────────────────────────────────────────────────── */
const GOOGLE_SHEETS_URL = "https://script.google.com/macros/s/AKfycbxIUmcmbfnATwsNfo8-TLN9D4r44utd_7U8n0STLkaTSdpoEl0CiJ6pnbCcQOm6acvk/exec";  // ← paste your Apps Script Web App URL here
const HAS_KANJI = /[\u4E00-\u9FAF\u3400-\u4DBF]/;
const IS_KANJI  = /^[\u4E00-\u9FAF\u3400-\u4DBF]$/;

/* ── Katakana word detection ──────────────────────────────────────
   Matches tokens that are ENTIRELY Katakana (loanwords, foreign names).
   Includes ー (prolonged sound mark U+30FC) which is part of katakana words.
   Excludes ・ (middle dot U+30FB) which is punctuation.
   Minimum length of 2 prevents single-char interjections (ア, ウ) from
   being treated as dictionary words.

   POS categories that are katakana but should NOT be hoverable:
     助詞   — particles (ヲ, ニ written in katakana)
     助動詞 — auxiliary verb endings (マス, デス in katakana)
     記号   — symbols / punctuation
   ────────────────────────────────────────────────────────────── */
const IS_KATAKANA_WORD = /^[\u30A1-\u30F6\u30FC]+$/;
const SKIP_KANA_POS    = ["助詞", "助動詞", "記号"];

/**
 * Returns true if the token should be treated as a hoverable katakana word.
 * @param {string} surface  token text  e.g. "コンピューター"
 * @param {string} pos      Kuromoji POS e.g. "名詞"
 */
function isKatakanaWord(surface, pos) {
  if (!IS_KATAKANA_WORD.test(surface)) return false;
  if (surface.length < 2) return false;
  if (SKIP_KANA_POS.some(p => (pos || "").startsWith(p))) return false;
  return true;
}

/* ================================================================
   §2  KUROMOJI TOKENIZER
   ================================================================
   Splits Japanese text into morphemes.
   Fields used per token:
     .surface_form — text as written  ("食べました")
     .basic_form   — dictionary base  ("食べる")   ← for API query
     .reading      — Katakana reading ("タベマシタ")
     .pos          — part-of-speech   ("動詞")
   ================================================================ */
let _tokenizer = null;

function getTokenizer() {
  if (_tokenizer) return Promise.resolve(_tokenizer);
  return new Promise((resolve, reject) => {
    if (typeof kuromoji === "undefined") {
      console.log("[Kuromoji] Loading script from CDN…");
      const s = document.createElement("script");
      s.src     = KUROMOJI_CDN;
      s.onload  = () => buildTokenizer(resolve, reject);
      s.onerror = () => reject(new Error("Failed to load Kuromoji.js"));
      document.head.appendChild(s);
    } else {
      buildTokenizer(resolve, reject);
    }
  });
}

function buildTokenizer(resolve, reject) {
  console.log("[Kuromoji] Building tokenizer (first call ~7MB dict download)…");
  kuromoji.builder({ dicPath: KUROMOJI_DICT }).build((err, tok) => {
    if (err) { reject(new Error("Kuromoji: " + err.message)); return; }
    _tokenizer = tok;
    console.log("[Kuromoji] Ready.");
    resolve(tok);
  });
}

async function tokenize(text) {
  const tok    = await getTokenizer();
  const tokens = tok.tokenize(text);
  console.log("[Kuromoji] Tokens:",
    tokens.map(t => `${t.surface_form}/${t.basic_form || "-"}`).join(" "));
  return tokens;
}

/* ================================================================
   §3  KATAKANA → HIRAGANA
   ================================================================ */
function katakanaToHiragana(str) {
  if (!str || str === "*") return "";
  return str.replace(/[\u30A1-\u30F6]/g, ch =>
    String.fromCharCode(ch.charCodeAt(0) - 0x60));
}

/* ================================================================
   §4  FURIGANA HTML BUILDER
   ================================================================
   Processes three token categories:

   ① Kanji-containing tokens  (HAS_KANJI)
     → <ruby class="jp-word"> with furigana <rt> above
     → hoverable for dictionary lookup

   ② Katakana-only words  (isKatakanaWord)
     → <span class="jp-word jp-katakana"> — NO furigana (already readable)
     → underlined + hoverable, same tooltip as kanji words
     → Jisho lookup uses surface form (コンピューター)

   ③ Everything else  (hiragana, particles, punctuation, Latin)
     → <span class="jp-plain"> — not interactive

   All hoverable elements carry data attributes:
     data-surface  — displayed text (used in tooltip header)
     data-basic    — Kuromoji dictionary base form (used for Jisho query)
     data-reading  — hiragana reading
     data-pos      — part-of-speech (Japanese label)

   CRITICAL: For <ruby>, NEVER read el.textContent — it includes the
   <rt> furigana text merged in. Always use el.dataset.surface.
   ================================================================ */
function buildFuriganaHTML(tokens) {
  let html = "";
  for (const token of tokens) {
    const surface = token.surface_form;
    if (surface === "\n" || surface === "\r\n" || surface === "\r") {
      html += "<br>"; continue;
    }

    const pos      = token.pos || "";
    const surfAttr = escapeAttr(surface);
    const basicRaw = (token.basic_form && token.basic_form !== "*")
                       ? token.basic_form : surface;
    const basicAttr = escapeAttr(basicRaw);
    const posAttr   = escapeAttr(pos);

    if (HAS_KANJI.test(surface)) {
      /* ── ① Kanji word: ruby with furigana ─────────────────── */
      if (token.reading && token.reading !== "*") {
        const hira = katakanaToHiragana(token.reading);
        html += `<ruby class="jp-word" data-surface="${surfAttr}" data-basic="${basicAttr}" data-reading="${escapeAttr(hira)}" data-pos="${posAttr}">${escapeHTML(surface)}<rt>${hira}</rt></ruby>`;
      } else {
        html += `<span class="jp-word jp-word--no-reading" data-surface="${surfAttr}" data-basic="${basicAttr}" data-reading="" data-pos="${posAttr}">${escapeHTML(surface)}</span>`;
      }

    } else if (isKatakanaWord(surface, pos)) {
      /* ── ② Katakana loanword: no furigana, still hoverable ── */
      // Katakana words are already readable (no furigana needed).
      // We use data-reading = surface itself (katakana is its own reading).
      // Jisho accepts katakana queries directly, e.g. "コンピューター".
      html += `<span class="jp-word jp-katakana" data-surface="${surfAttr}" data-basic="${basicAttr}" data-reading="${surfAttr}" data-pos="${posAttr}">${escapeHTML(surface)}</span>`;

    } else {
      /* ── ③ Non-interactive: particles, hiragana, punctuation ─ */
      html += `<span class="jp-plain">${escapeHTML(surface)}</span>`;
    }
  }
  return html;
}

/* ================================================================
   §5  FURIGANA BUTTON
   ================================================================ */
btnFurigana.addEventListener("click", async () => {
  const text = inputText.value.trim();
  if (!text) { showToast("Vui lòng nhập văn bản tiếng Nhật."); return; }

  setBtnsDisabled(true);
  setLoading(true, 10);
  showToast("Đang tải Kuromoji… (lần đầu ~5–10 giây)");

  try {
    setLoading(true, 20);
    const tokens = await tokenize(text);
    setLoading(true, 88);

    outputArea.innerHTML = buildFuriganaHTML(tokens);
    attachHoverHandlers();   // wire mouseenter/mouseleave on .jp-word elements
    setOutputMode("furigana");
    setLoading(false);

    const k  = tokens.filter(t => HAS_KANJI.test(t.surface_form)).length;
    const ka = tokens.filter(t => isKatakanaWord(t.surface_form, t.pos)).length;
    showToast(`Xong — ${tokens.length} từ, ${k} Kanji, ${ka} Katakana. Di chuột để xem nghĩa.`);

    // OPT 1: Start background preload — does NOT block the UI.
    // Fires after the current call stack clears so the page renders first.
    setTimeout(() => preloadDictionary(tokens), 0);

  } catch (err) {
    setLoading(false);
    showToast("Lỗi: " + err.message, true);
    console.error("[Furigana]", err);
  } finally {
    setBtnsDisabled(false);
  }
});

/* ================================================================
   §5a  BACKGROUND PRELOAD
   ================================================================
   OPT 1: After furigana renders, silently fetch dictionary data for
   every unique hoverable word in the sentence. Results are stored
   in dictionaryCache so the first hover is instant.

   WHY BATCHING?
   Firing all requests simultaneously (Promise.all) would:
     • Hammer the CORS proxy with 10–20 requests at once
     • Trigger rate-limiting → all requests fail
   Instead we batch: PRELOAD_BATCH_SIZE words at a time, waiting
   PRELOAD_BATCH_DELAY_MS between batches.

   WHY NOT BLOCK THE UI?
   Preload is a fire-and-forget background task. If it fails, hover
   still works — it just calls the API on demand as before.

   DEDUPLICATION:
   The same word can appear multiple times (日本 appears twice in
   "東京は日本の首都、日本語を話す"). We use a Set of cache keys to
   ensure each unique word is fetched exactly once.
   ================================================================ */

const PRELOAD_BATCH_SIZE     = 5;    // words fetched simultaneously
const PRELOAD_BATCH_DELAY_MS = 200;  // ms pause between batches
const HOVER_DELAY_MS         = 120;  // ms before tooltip fires (prevents scan flicker)

/**
 * Look up all unique hoverable words in the background and fill the cache.
 * @param {KuromojiToken[]} tokens  output of tokenize()
 */
async function preloadDictionary(tokens) {
  // ── Build deduplicated list of words to preload ────────────
  const seen  = new Set();
  const queue = [];   // [ { surface, basic, reading } ]

  for (const token of tokens) {
    const surface = token.surface_form;
    const pos     = token.pos || "";

    // Only preload words that will have hover handlers
    const isKanji    = HAS_KANJI.test(surface);
    const isKatakana = isKatakanaWord(surface, pos);
    if (!isKanji && !isKatakana) continue;

    // Skip if already in cache or already queued
    if (dictionaryCache[surface] !== undefined) continue;
    if (seen.has(surface)) continue;
    seen.add(surface);

    const basicRaw = (token.basic_form && token.basic_form !== "*")
                       ? token.basic_form : surface;
    const reading  = token.reading ? katakanaToHiragana(token.reading) : "";
    queue.push({ surface, basic: basicRaw, reading });
  }

  if (queue.length === 0) {
    console.log("[Preload] All words already cached — nothing to fetch.");
    return;
  }

  console.log(`[Preload] Starting background fetch for ${queue.length} unique words:`,
    queue.map(w => w.surface).join(" "));

  // ── Process in batches ─────────────────────────────────────
  let fetched = 0;
  let hits    = 0;

  for (let i = 0; i < queue.length; i += PRELOAD_BATCH_SIZE) {
    const batch = queue.slice(i, i + PRELOAD_BATCH_SIZE);

    // Fetch this batch in parallel
    await Promise.allSettled(
      batch.map(async ({ surface, basic, reading }) => {
        try {
          const result = await lookupWord(surface, basic, reading);
          fetched++;
          if (result) {
            hits++;
            console.log(`[Preload] ✓ ${surface} → "${result.english}" (${result.source})`);
          } else {
            console.log(`[Preload] ✗ ${surface} — no dictionary entry`);
          }
        } catch (err) {
          console.warn(`[Preload] Error for ${surface}:`, err.message);
        }
      })
    );

    // Pause between batches to avoid rate-limiting the proxy
    if (i + PRELOAD_BATCH_SIZE < queue.length) {
      await sleep(PRELOAD_BATCH_DELAY_MS);
    }
  }

  console.log(`[Preload] Done — ${hits}/${fetched} words found in dictionary.`);
}

/** Pause execution for `ms` milliseconds. */
function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

/* ================================================================
   §6  HOVER TOOLTIP
   ================================================================
   Three layers of performance optimisation work together here:

   OPT 2 — Instant display on cache hit:
     showTooltip() checks dictionaryCache[surface] before showing the
     spinner. If preload already finished, the meaning appears with
     zero visible delay. The spinner is only shown for genuine misses.

   OPT 3 — 300 ms hover delay (handleMouseEnter):
     A setTimeout fires showTooltip() after HOVER_DELAY_MS. If the
     user leaves before the timer fires, clearTimeout() cancels it.
     This prevents API calls for words the user just glances past.

   OPT 4 — Promise deduplication (in lookupWord):
     While a fetch is in flight, dictionaryCache[surface] holds the
     Promise itself. A second hover on the same word awaits that same
     Promise instead of firing a new request.

   Bug fixes from v7 preserved:
   BUG A — relatedTarget check ignores <rt> child mouseleave
   BUG B — position:fixed uses rect coords directly (no scroll offset)
   ================================================================ */

/* dictionaryCache: surface word → result object | null | Promise<result>
   - result object : lookup succeeded, use directly
   - null          : lookup tried, no entry found
   - Promise       : OPT 4 — fetch still in flight, await it         */
const dictionaryCache = {};

/* Stale-check counter: bumped on each genuine hover start and on hide */
let _hoverId = 0;

/* OPT 3: timer handle for the hover delay */
let _hoverTimer = null;

/* ── Attach handlers ────────────────────────────────────────────── */
function attachHoverHandlers() {
  const words = outputArea.querySelectorAll(".jp-word");
  console.log("[Hover] Attaching handlers to", words.length, "words.");
  words.forEach(el => {
    el.addEventListener("mouseenter", handleMouseEnter);
    el.addEventListener("mouseleave", handleMouseLeave);
    /* ── Click → open vocab save popup ─────────────────────── */
    el.addEventListener("click", handleWordClick);
  });
}

/**
 * OPT 3 — mouseenter: wait HOVER_DELAY_MS before doing anything.
 * If the cursor moves away within that window, clearTimeout cancels
 * the timer and no lookup is fired at all.
 */
function handleMouseEnter(e) {
  const el = e.currentTarget;

  // Cancel any previous pending timer (shouldn't happen, but defensive)
  clearTimeout(_hoverTimer);

  _hoverTimer = setTimeout(() => {
    showTooltip(el);
  }, HOVER_DELAY_MS);
}

/**
 * BUG A FIX — mouseleave: ignore if cursor moved into an <rt> child
 * or into the tooltip itself (those are not genuine "leave" events).
 */
function handleMouseLeave(e) {
  const el     = e.currentTarget;
  const target = e.relatedTarget;

  // Cancel the pending hover timer — user left before delay expired
  clearTimeout(_hoverTimer);

  if (target && (el.contains(target) || tooltip.contains(target))) {
    console.log("[Hover] Ignoring mouseleave — relatedTarget is child:", target?.nodeName);
    return;
  }

  hideTooltip();
}

/**
 * Show the tooltip for `el`.
 *
 * WORD EXTRACTION:
 * Always read from el.dataset — never el.textContent.
 * For <ruby>: textContent = "日本にほん" (base text + rt merged).
 * el.dataset.surface = "日本" (the clean kanji text only).
 *
 * OPT 2 — cache hit path:
 * If dictionaryCache[surface] already has a resolved result object,
 * fill the tooltip fields directly without showing the spinner.
 * The user sees the meaning instantly.
 */
async function showTooltip(el) {
  const myId = ++_hoverId;

  const surface = el.dataset.surface;
  const basic   = el.dataset.basic;
  const reading = el.dataset.reading;
  const pos     = el.dataset.pos;

  console.log("[Hover] Word:", surface, "| Base:", basic, "| Reading:", reading);

  /* Always fill word/reading/POS immediately (no network needed) */
  tooltipWord.textContent    = surface;
  tooltipReading.textContent = reading || "";
  tooltipPOS.textContent     = translatePOS(pos);
  positionTooltip(el);
  tooltip.hidden = false;

  /* ── OPT 2: instant path — result already in cache ─────────── */
  const cached = dictionaryCache[surface];
  if (cached !== undefined && !(cached instanceof Promise)) {
    // Cache has a resolved result (or confirmed null) — show immediately
    tooltipSpinner.hidden = true;

    if (cached && cached.english) {
      tooltipEnglish.textContent = cached.english;
      if (cached.pos) tooltipPOS.textContent = cached.pos;
      console.log("[Hover] ✓ Instant cache hit for", surface + ":", cached.english);
    } else {
      tooltipEnglish.textContent = "No dictionary result found.";
      console.log("[Hover] Cache hit (no entry) for:", surface);
    }
    return;  // done — no API call needed
  }

  /* ── Normal path — fetch needed (or in-flight promise) ─────── */
  tooltipEnglish.textContent = "…";
  tooltipSpinner.hidden      = false;

  try {
    /* lookupWord may return an existing in-flight Promise (OPT 4) */
    const result = await lookupWord(surface, basic, reading);

    if (myId !== _hoverId) {
      console.log("[Hover] Stale result for", surface, "— discarding.");
      return;
    }

    tooltipSpinner.hidden = true;

    if (result && result.english) {
      tooltipEnglish.textContent = result.english;
      if (result.pos) tooltipPOS.textContent = result.pos;
      console.log("[Hover] ✓ English for", surface + ":", result.english,
                  "(source:", result.source + ")");
    } else {
      tooltipEnglish.textContent = "No dictionary result found.";
      console.log("[Hover] No result for:", surface);
    }

  } catch (err) {
    if (myId !== _hoverId) return;
    tooltipSpinner.hidden = true;
    tooltipEnglish.textContent = "Lookup error — check network.";
    console.warn("[Hover] Error for", surface + ":", err.message);
  }
}

/** Hide tooltip and invalidate pending lookups. */
function hideTooltip() {
  _hoverId++;
  tooltip.hidden = true;
}

/**
 * BUG B FIX — position tooltip near anchor.
 * getBoundingClientRect() → viewport coords.
 * position:fixed also uses viewport coords → no scrollX/scrollY.
 */
function positionTooltip(anchor) {
  const rect = anchor.getBoundingClientRect();
  const W = 260, H = 140, GAP = 10, EDGE = 8;

  let left = rect.left;
  let top  = rect.bottom + GAP;

  if (left + W > window.innerWidth - EDGE)  left = window.innerWidth - W - EDGE;
  if (left < EDGE) left = EDGE;

  if (rect.bottom + H + GAP > window.innerHeight) {
    top = rect.top - H - GAP;
    if (top < EDGE) top = rect.bottom + GAP;
  }

  tooltip.style.left = left + "px";
  tooltip.style.top  = top  + "px";
  console.log("[Hover] Tooltip at", left, top,
              "(word rect:", rect.left.toFixed(0), rect.bottom.toFixed(0) + ")");
}

/* ================================================================
   §7  DICTIONARY LOOKUP — 3-form chain + 2-proxy + OPT 4
   ================================================================
   Japanese verbs/adjectives are conjugated ("食べました") but Jisho
   only indexes base forms ("食べる").

   Query order (stop at first hit):
     1. basic_form  "食べる"      — Kuromoji dict form (best for verbs)
     2. surface     "食べました"  — works for nouns ("日本")
     3. reading     "たべました"  — hiragana fallback

   For each term: Jisho via proxy A → proxy B → kanjiapi.dev

   OPT 4 — Promise deduplication:
   While a fetch is running, lookupWord stores the Promise itself in
   dictionaryCache[surface]. A second caller that hovers the same word
   during an in-flight request awaits the existing Promise — no
   duplicate network request is ever made.

   Cache values:
     undefined  — never looked up
     Promise    — fetch in flight (OPT 4)
     object     — resolved result  { english, pos, source }
     null       — looked up, not found
   ================================================================ */

/* ================================================================
   §7  DICTIONARY LOOKUP — race strategy + OPT 4
   ================================================================
   Speed fix: instead of trying proxies sequentially (each can take
   4 s before timing out), we now RACE kanjiapi.dev against Jisho:
   • kanjiapi.dev has real CORS, responds in ~300-600 ms
   • Jisho via proxy gives richer data but can take 1-4 s
   • Promise.race() returns whichever finishes first
   • If kanjiapi wins, we show it immediately; the Jisho result
     is quietly cached if it arrives later and is richer

   For verbs/adjectives, kanjiapi can only look up individual kanji,
   so we still try Jisho basic_form first in the race.

   Cache values:
     undefined  — never looked up
     Promise    — fetch in flight (OPT 4: deduplicate concurrent hovers)
     object     — resolved result  { english, pos, source }
     null       — looked up, not found
   ================================================================ */

async function lookupWord(surface, basic, reading) {

  /* ── Cache check ──────────────────────────────────────────── */
  if (dictionaryCache[surface] !== undefined) {
    const cached = dictionaryCache[surface];
    if (cached instanceof Promise) {
      console.log("[Cache] Awaiting in-flight fetch for:", surface);
      return cached;
    }
    console.log("[Cache] HIT:", surface, cached ? "→ " + cached.english : "(no entry)");
    return cached;
  }

  const terms = dedupeTerms([basic, surface, reading]);
  console.log("[Lookup] Starting for:", surface, "| Terms:", terms);

  /* OPT 4: store the Promise immediately */
  const fetchPromise = (async () => {

    /* ── Build all candidate fetches and race them ──────────── */
    // Jisho fetches (primary proxy first, secondary as backup)
    const jishoFetches = terms.flatMap(term =>
      [PROXY_PRIMARY, PROXY_SECONDARY].map(proxy =>
        fetchJisho(term, proxy).catch(() => null)
      )
    );

    // kanjiapi.dev fetch — fast, direct CORS, always works for kanji
    const kanjiApiFetch = lookupKanjiAPI(surface).catch(() => null);

    // Race everything: return the first non-null result
    // We use a custom race that skips null results
    const result = await firstNonNull([kanjiApiFetch, ...jishoFetches]);

    return result ?? null;

  })();

  dictionaryCache[surface] = fetchPromise;

  const result = await fetchPromise;
  dictionaryCache[surface] = result;

  if (result) {
    console.log("[Lookup] Resolved:", surface, "→", result.english,
                "(source:", result.source + ")");
  } else {
    console.log("[Lookup] No result for:", surface);
  }

  return result;
}

/**
 * Returns the first non-null resolved value from an array of Promises.
 * Unlike Promise.race(), it waits past null/rejected results.
 * @param {Promise[]} promises
 * @returns {Promise<any>}
 */
function firstNonNull(promises) {
  return new Promise((resolve) => {
    let remaining = promises.length;
    if (remaining === 0) { resolve(null); return; }
    promises.forEach(p =>
      Promise.resolve(p).then(val => {
        if (val != null) resolve(val);
        else if (--remaining === 0) resolve(null);
      }).catch(() => {
        if (--remaining === 0) resolve(null);
      })
    );
  });
}

/** Deduplicate and filter empty/invalid terms. */
function dedupeTerms(terms) {
  const seen = new Set();
  return terms.filter(t => {
    if (!t || t === "*" || t === "—" || seen.has(t)) return false;
    seen.add(t); return true;
  });
}

/**
 * Fetch from Jisho API through a given CORS proxy.
 *
 * allorigins.win response: { contents: "<json>", status: { http_code: 200 } }
 * corsproxy.io response: the raw Jisho JSON (no wrapper)
 *
 * @param {string} word      search keyword
 * @param {string} proxyBase proxy URL prefix
 * @returns {object|null}    parsed result or null if not found
 */
async function fetchJisho(word, proxyBase) {
  const jishoURL = `${JISHO_BASE}?keyword=${encodeURIComponent(word)}`;
  const url      = `${proxyBase}${encodeURIComponent(jishoURL)}`;

  console.log("[Jisho] API request URL:", url);

  const res = await fetchWithTimeout(url, {}, 4000);
  if (!res.ok) throw new Error(`HTTP ${res.status}`);

  const raw = await res.json();

  // Handle allorigins.win wrapper: { contents: "<json string>", status: {...} }
  let json;
  if (typeof raw.contents === "string") {
    // allorigins.win format
    const innerStatus = raw?.status?.http_code;
    if (innerStatus && innerStatus !== 200)
      throw new Error(`Jisho returned HTTP ${innerStatus}`);
    if (!raw.contents || raw.contents.length < 5)
      throw new Error("allorigins: empty contents");
    json = JSON.parse(raw.contents);
  } else if (raw.data) {
    // corsproxy.io passes Jisho JSON through directly
    json = raw;
  } else {
    throw new Error("Unknown proxy response format");
  }

  console.log("[Jisho] API response: got", json?.data?.length || 0, "entries");

  const entries = json?.data;
  if (!entries || entries.length === 0) return null;

  const entry   = entries[0];
  const jpEntry = (entry.japanese || [])[0] || {};

  const reading = katakanaToHiragana(jpEntry.reading || jpEntry.word || "");

  const senses      = entry.senses || [];
  const englishDefs = senses.slice(0, 2).flatMap(s =>
    (s.english_definitions || []).slice(0, 2));
  const english     = englishDefs.join("; ") || "—";

  console.log("[Jisho] English definitions:", englishDefs);

  const posList = (senses[0]?.parts_of_speech || []).slice(0, 2);
  const pos     = posList.map(translatePOSEnglish).filter(Boolean).join(", ");

  return { reading, english, pos, source: "Jisho" };
}

/**
 * kanjiapi.dev fallback — looks up each Kanji individually.
 * Always available because kanjiapi.dev has real CORS headers.
 */
async function lookupKanjiAPI(word) {
  const kanjis = [...word].filter(ch => IS_KANJI.test(ch));
  if (kanjis.length === 0) return null;

  console.log("[kanjiapi] Chars:", kanjis.join(" "));

  const results = await Promise.allSettled(
    kanjis.map(async k => {
      const url = `${KANJIAPI_BASE}${encodeURIComponent(k)}`;
      console.log("[kanjiapi] GET", url);
      const res = await fetchWithTimeout(url, {}, 6000);
      if (!res.ok) throw new Error(`HTTP ${res.status} for ${k}`);
      return res.json();
    })
  );

  const allMeanings = [];
  results.forEach((r, i) => {
    if (r.status === "fulfilled" && r.value?.meanings?.length) {
      console.log("[kanjiapi]", kanjis[i], "→", r.value.meanings.slice(0, 2));
      allMeanings.push(...r.value.meanings.slice(0, 2));
    } else {
      console.warn("[kanjiapi]", kanjis[i], "failed:", r.reason?.message);
    }
  });

  if (allMeanings.length === 0) return null;

  const english = [...new Set(allMeanings)].slice(0, 3).join("; ");
  return { reading: "", english, pos: "", source: "kanjiapi.dev" };
}

/* ================================================================
   §8  PART-OF-SPEECH LABELS
   ================================================================ */
function translatePOS(pos) {
  const m = {
    "名詞": "Noun",     "動詞": "Verb",     "形容詞": "i-Adjective",
    "形容動詞": "na-Adjective", "副詞": "Adverb", "助詞": "Particle",
    "助動詞": "Aux. verb", "接続詞": "Conjunction", "感動詞": "Interjection",
    "接頭詞": "Prefix", "接尾辞": "Suffix", "記号": "Symbol",
  };
  return m[pos] || pos || "";
}

function translatePOSEnglish(pos) {
  if (!pos) return "";
  const p = pos.toLowerCase();
  if (p.includes("i-adjective"))  return "i-Adjective";
  if (p.includes("na-adjective")) return "na-Adjective";
  if (p.includes("adjective"))    return "Adjective";
  if (p.includes("noun"))         return "Noun";
  if (p.includes("verb"))         return "Verb";
  if (p.includes("adverb"))       return "Adverb";
  if (p.includes("particle"))     return "Particle";
  if (p.includes("conjunction"))  return "Conjunction";
  if (p.includes("interjection")) return "Interjection";
  if (p.includes("expression"))   return "Expression";
  if (p.includes("prefix"))       return "Prefix";
  if (p.includes("suffix"))       return "Suffix";
  return pos;
}

/* ================================================================
   §8b  VOCABULARY SAVE POPUP + GOOGLE SHEETS INTEGRATION
   ================================================================
   CLICK INTERACTION:
   When the user clicks an underlined word (mouseenter shows tooltip;
   click opens this popup). The popup:
     1. Displays word, reading, and English meaning
     2. Has a "Lưu từ vựng" (Save Vocabulary) button
     3. Can save to Google Sheets via Apps Script Web App
     4. Tracks saved words locally to prevent duplicates

   GOOGLE SHEETS FLOW:
     Frontend → POST JSON → Apps Script Web App → append row in Sheet
     Body: { word, reading, meaning, timestamp }
     The Apps Script returns { status: "ok" } on success.

   DUPLICATE PREVENTION:
     savedVocabSet (a Set) tracks surface forms that have been saved.
     Before saving, we check if the word is already in the set.
     This prevents accidental double-saves within the same session.
     (Across sessions, the sheet itself is the source of truth.)

   LOCAL VOCAB PANEL:
     Each saved word is also displayed in the .vocab-panel section
     below the output area, so the user can review what they saved.
   ================================================================ */

/* ── State ────────────────────────────────────────────────────── */

/** Set of surface forms already saved this session — prevents duplicates */
const savedVocabSet = new Set();

/** The word currently shown in the vocab popup */
let _vpCurrentWord    = null;
let _vpCurrentReading = null;
let _vpCurrentMeaning = null;

/* ── handleWordClick — opens the vocab popup on word click ───── */

/**
 * Called when the user clicks an underlined .jp-word element.
 * Reads the word data from dataset, then shows the vocab popup.
 *
 * @param {MouseEvent} e
 */
function handleWordClick(e) {
  e.stopPropagation();  // prevent document click from closing it immediately

  const el      = e.currentTarget;
  const surface = el.dataset.surface;
  const reading = el.dataset.reading || "";

  // Get the English meaning from cache (may be null if not yet fetched)
  const cached = dictionaryCache[surface];
  const meaning = (cached && !(cached instanceof Promise))
                    ? (cached.english || "")
                    : "";

  console.log("[VocabPopup] Click on:", surface, "| meaning:", meaning || "(not yet cached)");

  // If meaning not cached yet (user clicked before hover loaded), show "…"
  openVocabPopup(surface, reading, meaning || "…", el);
}

/**
 * Position and populate the vocab save popup near the clicked element.
 *
 * @param {string}  surface  displayed word      ("日本")
 * @param {string}  reading  hiragana reading    ("にほん")
 * @param {string}  meaning  English meaning     ("Japan")
 * @param {Element} anchor   the clicked element (for positioning)
 */
function openVocabPopup(surface, reading, meaning, anchor) {
  // Store current word for the save button handler
  _vpCurrentWord    = surface;
  _vpCurrentReading = reading;
  _vpCurrentMeaning = meaning;

  // Fill popup content
  vpWord.textContent    = surface;
  vpReading.textContent = reading;
  vpMeaning.textContent = meaning;

  // Reset save button to default state
  resetSaveButton();

  // If this word is already saved, show the "already saved" state
  if (savedVocabSet.has(surface)) {
    setSaveBtnState("duplicate");
  }

  // Position popup below the clicked word (same logic as tooltip)
  positionVocabPopup(anchor);

  // Show popup
  vocabPopup.hidden = false;

  // Close hover tooltip while popup is open to reduce clutter
  hideTooltip();
}

/**
 * Position the vocab popup near the anchor element.
 * Uses viewport coordinates (position:fixed).
 */
function positionVocabPopup(anchor) {
  const rect = anchor.getBoundingClientRect();
  const W    = 240;
  const H    = 180;
  const GAP  = 8;
  const EDGE = 8;

  let left = rect.left;
  let top  = rect.bottom + GAP;

  if (left + W > window.innerWidth  - EDGE) left = window.innerWidth  - W - EDGE;
  if (left < EDGE) left = EDGE;

  if (rect.bottom + H + GAP > window.innerHeight) {
    top = rect.top - H - GAP;
    if (top < EDGE) top = rect.bottom + GAP;
  }

  vocabPopup.style.left = left + "px";
  vocabPopup.style.top  = top  + "px";
}

/** Close the vocab popup and clear current word state. */
function closeVocabPopup() {
  vocabPopup.hidden = true;
  _vpCurrentWord    = null;
  _vpCurrentReading = null;
  _vpCurrentMeaning = null;
}

/* Close on × button click */
vocabPopupClose.addEventListener("click", closeVocabPopup);

/* Close when clicking anywhere outside the popup */
document.addEventListener("click", e => {
  if (!vocabPopup.hidden &&
      !vocabPopup.contains(e.target) &&
      !e.target.closest(".jp-word")) {
    closeVocabPopup();
  }
});

document.addEventListener("keydown", e => {
  if (e.key === "Escape") closeVocabPopup();
});

/* ── Save button ──────────────────────────────────────────────── */

/**
 * vpSaveBtn click handler.
 *
 * Flow:
 *  1. Check duplicate (savedVocabSet)
 *  2. Show spinner / "Đang lưu…"
 *  3. POST to Google Sheets (if URL configured)
 *  4. Add to local vocab panel
 *  5. Update savedVocabSet
 *  6. Show success / error state on button
 */
vpSaveBtn.addEventListener("click", async () => {
  const surface = _vpCurrentWord;
  const reading = _vpCurrentReading;
  let   meaning = _vpCurrentMeaning;

  if (!surface) return;

  /* ── Duplicate check ───────────────────────────────────────── */
  if (savedVocabSet.has(surface)) {
    setSaveBtnState("duplicate");
    console.log("[Vocab] Duplicate — already saved:", surface);
    return;
  }

  /* ── If meaning is still "…", try to get it from cache ─────── */
  if (meaning === "…" || !meaning) {
    const cached = dictionaryCache[surface];
    if (cached && !(cached instanceof Promise) && cached.english) {
      meaning = cached.english;
      vpMeaning.textContent = meaning;
      _vpCurrentMeaning = meaning;
    } else {
      meaning = "(no translation)";
    }
  }

  const timestamp = new Date().toISOString().slice(0, 10);  // "YYYY-MM-DD"

  console.log("[Vocab] Saving:", { word: surface, reading, meaning, timestamp });

  /* ── Show "saving" state ───────────────────────────────────── */
  setSaveBtnState("saving");

  /* ── POST to Google Sheets ─────────────────────────────────── */
  let sheetsOk = true;

  if (GOOGLE_SHEETS_URL) {
    try {
      await saveToGoogleSheets({ word: surface, reading, meaning, timestamp });
      console.log("[Sheets] ✓ Saved to Google Sheets");
    } catch (err) {
      sheetsOk = false;
      console.error("[Sheets] Error:", err.message);
      setSaveBtnState("error");
      showToast("Lỗi Google Sheets: " + err.message, true);
      return;
    }
  } else {
    console.log("[Sheets] No URL configured — saving locally only.");
  }

  /* ── Mark as saved ─────────────────────────────────────────── */
  savedVocabSet.add(surface);

  /* ── Add to local vocab panel ──────────────────────────────── */
  addToVocabPanel(surface, reading, meaning);

  /* ── Show success state ────────────────────────────────────── */
  setSaveBtnState("saved");
  showToast(GOOGLE_SHEETS_URL ? "Đã lưu vào Google Sheets!" : "Đã lưu từ vựng!");
});

/* ── Google Sheets API call ───────────────────────────────────── */

/**
 * Send word data to Google Sheets via a GET request with URL parameters.
 *
 * WHY GET INSTEAD OF POST:
 * Google Apps Script Web Apps block cross-origin POST requests unless
 * the script explicitly sets CORS headers (which the default template
 * does NOT do). Using mode:"no-cors" lets the request through but
 * makes the response unreadable — so we can't detect errors.
 *
 * The solution: send all data as GET query parameters instead.
 * Apps Script's doGet(e) reads e.parameter.word / .reading / etc.
 * GET requests are NOT subject to the preflight CORS check, so the
 * browser sends them freely and we can read the response.
 *
 * URL pattern:
 *   GOOGLE_SHEETS_URL?word=日本&reading=にほん&meaning=Japan&timestamp=2026-03-14
 *
 * @param {{ word:string, reading:string, meaning:string, timestamp:string }} data
 */
async function saveToGoogleSheets(data) {
  // Build URL with all fields as query parameters
  const params = new URLSearchParams({
    word:      data.word,
    reading:   data.reading,
    meaning:   data.meaning,
    timestamp: data.timestamp,
  });

  const url = `${GOOGLE_SHEETS_URL}?${params.toString()}`;
  console.log("[Sheets] GET →", url);

  const res = await fetchWithTimeout(url, {
    method: "GET",
    // No mode:"no-cors" — we want to READ the response to detect errors
  }, 10000);

  // Apps Script redirects the GET to a unique execution URL.
  // fetch() follows the redirect automatically; res.ok checks the final response.
  if (!res.ok) throw new Error(`HTTP ${res.status}`);

  // Try to parse the JSON response { status: "ok" } from Apps Script
  try {
    const json = await res.json();
    console.log("[Sheets] Response:", json);
    if (json.status === "error") {
      throw new Error(json.message || "Apps Script error");
    }
    if (json.status === "duplicate") {
      console.log("[Sheets] Word already exists in sheet:", data.word);
      // Treat server-side duplicate as success (already there = fine)
    }
  } catch (parseErr) {
    // If parsing fails but request succeeded (200), treat as ok
    // (some Apps Script deployments return HTML on redirect)
    console.warn("[Sheets] Could not parse response JSON — treating as success:", parseErr.message);
  }
}

/* ── Save button state machine ────────────────────────────────── */

/**
 * Set the visual state of the save button.
 * States: "default" | "saving" | "saved" | "duplicate" | "error"
 */
function setSaveBtnState(state) {
  // Remove all state classes
  vpSaveBtn.classList.remove("saved", "duplicate", "error");
  vpSaveBtn.disabled = false;

  switch (state) {
    case "saving":
      vpSaveBtn.disabled = true;
      vpSaveLabel.textContent = "Đang lưu…";
      break;
    case "saved":
      vpSaveBtn.classList.add("saved");
      vpSaveBtn.disabled = true;
      vpSaveLabel.textContent = "✓ Đã lưu";
      break;
    case "duplicate":
      vpSaveBtn.classList.add("duplicate");
      vpSaveBtn.disabled = true;
      vpSaveLabel.textContent = "Đã có trong danh sách";
      break;
    case "error":
      vpSaveBtn.classList.add("error");
      vpSaveLabel.textContent = "Lỗi — thử lại";
      break;
    default:  // "default"
      vpSaveLabel.textContent = "Lưu từ vựng";
      break;
  }
}

function resetSaveButton() {
  setSaveBtnState("default");
}

/* ── Local vocabulary panel ───────────────────────────────────── */

/**
 * Add a word card to the saved vocab panel.
 * @param {string} surface  "日本"
 * @param {string} reading  "にほん"
 * @param {string} meaning  "Japan"
 */
function addToVocabPanel(surface, reading, meaning) {
  // Show the panel if hidden
  vocabPanel.hidden = false;

  // Build word card element
  const card = document.createElement("div");
  card.className       = "vocab-card";
  card.dataset.surface = surface;

  card.innerHTML = `
    <div class="vocab-card__word">${escapeHTML(surface)}</div>
    <div class="vocab-card__reading">${escapeHTML(reading)}</div>
    <div class="vocab-card__meaning">${escapeHTML(meaning)}</div>
    <button class="vocab-card__remove" title="Xóa khỏi danh sách" aria-label="Xóa">×</button>`;

  // Wire remove button
  card.querySelector(".vocab-card__remove").addEventListener("click", () => {
    savedVocabSet.delete(surface);
    card.remove();
    updateVocabCount();
    if (vocabList.children.length === 0) vocabPanel.hidden = true;
  });

  vocabList.appendChild(card);
  updateVocabCount();
}

function updateVocabCount() {
  const n = vocabList.children.length;
  vocabCountEl.textContent = n + (n === 1 ? " từ" : " từ");
}

/* Clear all saved vocab */
btnClearVocab.addEventListener("click", () => {
  savedVocabSet.clear();
  vocabList.innerHTML = "";
  vocabPanel.hidden = true;
  showToast("Đã xóa danh sách từ vựng.");
});

/* ================================================================
   §9  TEXT-TO-SPEECH (Web Speech API)
   ================================================================
   Built into all modern browsers — no library or API key needed.

   TOGGLE:
   • Click once  → start reading the input text aloud in Japanese.
   • Click again → stop immediately (speechSynthesis.cancel()).

   VOICE SELECTION:
   Prefers a local ja-JP voice from speechSynthesis.getVoices().
   Falls back to any voice with lang starting "ja".
   The 'voiceschanged' event is listened to because Chrome loads
   voices asynchronously after the page loads.

   EVENTS:
   utterance.onend / .onerror → reset button to idle when done.
   ================================================================ */

let _isSpeaking = false;   // true while speech is playing
let _utterance  = null;    // the active SpeechSynthesisUtterance

/**
 * Toggle TTS: start speaking the input text, or stop if already speaking.
 * Bound to btnSpeak click event below.
 */
function toggleSpeech() {
  const text = inputText.value.trim();

  /* ── Stop if currently speaking ──────────────────────────── */
  if (_isSpeaking) {
    speechSynthesis.cancel();
    setSpeakingState(false);
    console.log("[TTS] Stopped by user.");
    return;
  }

  if (!text) { showToast("Nhập văn bản tiếng Nhật trước."); return; }

  /* ── Browser support check ────────────────────────────────── */
  if (!("speechSynthesis" in window)) {
    showToast("Trình duyệt không hỗ trợ Text-to-Speech.", true);
    console.warn("[TTS] speechSynthesis not available in this browser.");
    return;
  }

  /* ── Build utterance ──────────────────────────────────────── */
  _utterance       = new SpeechSynthesisUtterance(text);
  _utterance.lang  = "ja-JP";
  _utterance.rate  = 0.9;    // slightly slower than default for clarity
  _utterance.pitch = 1.0;

  /* ── Pick the best available Japanese voice ───────────────── */
  const voices  = speechSynthesis.getVoices();
  // Prefer local (on-device) ja voice; fall back to network ja voice
  const jaVoice = voices.find(v => v.lang.startsWith("ja") && v.localService)
               || voices.find(v => v.lang.startsWith("ja"));

  if (jaVoice) {
    _utterance.voice = jaVoice;
    console.log("[TTS] Voice selected:", jaVoice.name, jaVoice.lang);
  } else {
    console.log("[TTS] No ja-JP voice found; browser will use its default.");
  }
  console.log("[TTS] Speaking:", text.slice(0, 60) + (text.length > 60 ? "…" : ""));

  /* ── Utterance event handlers ─────────────────────────────── */
  _utterance.onstart = () => setSpeakingState(true);

  _utterance.onend = () => {
    setSpeakingState(false);
    console.log("[TTS] Finished reading.");
  };

  _utterance.onerror = (e) => {
    setSpeakingState(false);
    // "interrupted" is normal when cancel() is called — not an error
    if (e.error !== "interrupted" && e.error !== "canceled") {
      showToast("Lỗi TTS: " + e.error, true);
      console.error("[TTS] Error:", e.error);
    }
  };

  /* ── Start speaking ───────────────────────────────────────── */
  speechSynthesis.cancel();          // clear any queued speech first
  speechSynthesis.speak(_utterance);
}

/**
 * Update the speak button appearance.
 * @param {boolean} speaking  true = reading aloud, false = idle
 */
function setSpeakingState(speaking) {
  _isSpeaking = speaking;
  const btn   = document.getElementById("btnSpeak");
  if (!btn) return;

  const label = btn.querySelector(".btn-speak-label");
  if (speaking) {
    btn.classList.add("speaking");
    btn.title = "Nhấn để dừng";
    if (label) label.textContent = "Đang đọc…";
  } else {
    btn.classList.remove("speaking");
    btn.title = "Đọc văn bản tiếng Nhật to";
    if (label) label.textContent = "Đọc tiếng Nhật";
  }
}

/* Wire button */
document.getElementById("btnSpeak")
  .addEventListener("click", toggleSpeech);

/* Pre-load voices (Chrome fires voiceschanged asynchronously) */
if ("speechSynthesis" in window) {
  window.speechSynthesis.addEventListener("voiceschanged", () => {
    const all = speechSynthesis.getVoices();
    const ja  = all.filter(v => v.lang.startsWith("ja"));
    console.log("[TTS] Voices ready:", all.length, "total,", ja.length, "Japanese:",
      ja.map(v => v.name).join(" | "));
  });
  speechSynthesis.getVoices(); // trigger load on Firefox/Chrome
}

/* ================================================================
   §10  TRANSLATE BUTTON (whole sentence → Vietnamese)
   ================================================================ */
btnTranslate.addEventListener("click", async () => {
  const text = inputText.value.trim();
  if (!text) { showToast("Vui lòng nhập văn bản tiếng Nhật."); return; }
  if (text.length > 4800) { showToast("Văn bản quá dài.", true); return; }

  hideTooltip();
  setBtnsDisabled(true);
  setLoading(true, 30);

  try {
    const params = new URLSearchParams({ q: text, langpair: "ja|vi", de: "learner@example.com" });
    const res  = await fetchWithTimeout(`${TRANSLATE_API}?${params}`, {}, 12000);
    if (!res.ok) throw new Error(`MyMemory HTTP ${res.status}`);
    const data = await res.json();
    if (data.responseStatus !== 200) throw new Error(data.responseDetails);

    setLoading(true, 90);
    const div = document.createElement("div");
    div.className   = "translation-result";
    div.textContent = data.responseData.translatedText;
    outputArea.innerHTML = "";
    outputArea.appendChild(div);
    setOutputMode("translation");
    setLoading(false);
  } catch (err) {
    setLoading(false);
    showToast(err.message || "Lỗi dịch.", true);
    console.error("[Translate]", err);
  } finally {
    setBtnsDisabled(false);
  }
});

/* ================================================================
   §10  CLEAR BUTTON
   ================================================================ */
btnClear.addEventListener("click", () => {
  inputText.value = "";
  charCount.textContent = "0";
  outputArea.innerHTML = `<div class="output-placeholder"><div class="placeholder-icon">⟨ ⟩</div><p>Kết quả hiển thị tại đây.</p><p class="placeholder-hint">Nhấn «Thêm Furigana» hoặc «Dịch» để bắt đầu.</p></div>`;
  setOutputMode("");
  hideTooltip();
  inputText.focus();
});

/* ================================================================
   §11  UI HELPERS
   ================================================================ */
inputText.addEventListener("input", () => { charCount.textContent = inputText.value.length; });

function setBtnsDisabled(s) {
  btnFurigana.disabled = btnTranslate.disabled = btnClear.disabled = s;
}

function setLoading(active, pct = 0) {
  if (active) {
    loadingBar.classList.add("visible");
    loadingFill.style.width = pct + "%";
  } else {
    loadingFill.style.width = "100%";
    setTimeout(() => { loadingBar.classList.remove("visible"); loadingFill.style.width = "0%"; }, 350);
  }
}

let _toastTimer = null;
function showToast(msg, isError = false) {
  clearTimeout(_toastTimer);
  toast.textContent = msg;
  toast.classList.toggle("error", isError);
  toast.classList.add("show");
  _toastTimer = setTimeout(() => toast.classList.remove("show"), 3200);
}

function setOutputMode(mode) {
  outputBadge.className = "output-mode-badge " + mode;
  outputBadge.textContent = mode === "furigana" ? "Furigana" : mode === "translation" ? "Dịch thuật" : "";
  outputLabel.textContent = mode === "furigana" ? "Văn bản có Furigana"
                          : mode === "translation" ? "Bản dịch Tiếng Việt" : "Kết quả";
}

/* ================================================================
   §12  UTILITIES
   ================================================================ */
function fetchWithTimeout(url, opts = {}, ms = 10000) {
  const ctrl = new AbortController();
  const id   = setTimeout(() => ctrl.abort(), ms);
  return fetch(url, { ...opts, signal: ctrl.signal }).finally(() => clearTimeout(id));
}

function escapeHTML(s) {
  return s.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
}

function escapeAttr(s) {
  if (!s) return "";
  return s.replace(/&/g,"&amp;").replace(/"/g,"&quot;").replace(/'/g,"&#39;");
}

/* ================================================================
   §13  INIT
   ================================================================ */
(function init() {
  // Sample covers: katakana loanwords (コンピューター), kanji + hiragana verbs,
  // noun compounds (日本語, 東京), and conjugated forms (食べました, 行きます)
  const sample = "コンピューターで日本語を勉強します。東京は日本の首都です。";
  inputText.value = sample;
  charCount.textContent = sample.length;
  console.log("[Init] v8 ready. Click «Thêm Furigana» then hover Kanji/Katakana words.");
})();
