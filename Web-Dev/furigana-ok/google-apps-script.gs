/**
 * ================================================================
 *  Google Apps Script — Japanese Vocabulary Saver
 *  File: google-apps-script.gs
 *
 *  WHY doGet() INSTEAD OF doPost():
 *  ─────────────────────────────────────────────────────────────
 *  Browser → Apps Script cross-origin POST requests are blocked by
 *  CORS preflight. Apps Script does NOT set Access-Control-Allow-Origin
 *  automatically, so the browser drops POST before it reaches the script.
 *
 *  GET requests skip CORS preflight entirely — the browser sends them
 *  and can read the JSON response. We pass vocabulary data as URL
 *  query parameters instead of a request body.
 *
 *  SETUP INSTRUCTIONS:
 *  ─────────────────────────────────────────────────────────────
 *  1. Go to https://script.google.com
 *  2. Create a new project (or open an existing one linked to your Sheet)
 *  3. Replace ALL content of Code.gs with this file
 *  4. Click Deploy → New deployment
 *       Type: Web App
 *       Execute as: Me
 *       Who has access: Anyone   ← REQUIRED
 *  5. Copy the Web App URL (ends in /exec)
 *  6. Paste it in script.js:
 *       const GOOGLE_SHEETS_URL = "https://script.google.com/macros/s/.../exec";
 *
 *  TEST (paste in browser):
 *    YOUR_URL?word=テスト&reading=てすと&meaning=test&timestamp=2026-01-01
 *  Expected response: {"status":"ok","row":2,"word":"テスト"}
 *
 *  SHEET COLUMNS (auto-created):
 *    A: Word  |  B: Reading  |  C: Meaning  |  D: Date  |  E: Timestamp
 * ================================================================
 */

const SHEET_NAME = "Vocabulary";

/**
 * Handles GET requests from the Japanese Learning Tool frontend.
 *
 * URL: SCRIPT_URL?word=日本&reading=にほん&meaning=Japan&timestamp=2026-03-14
 *
 * Returns:
 *   { status: "ok",        row: N, word: "..." }   — saved successfully
 *   { status: "duplicate", word: "..." }            — word already in sheet
 *   { status: "error",     message: "..." }         — something went wrong
 *
 * @param {GoogleAppsScript.Events.DoGet} e
 */
function doGet(e) {
  try {
    const params   = e.parameter || {};
    const word     = (params.word      || "").trim();
    const reading  = (params.reading   || "").trim();
    const meaning  = (params.meaning   || "").trim();
    const dateStr  = (params.timestamp || new Date().toISOString().slice(0, 10)).trim();
    const isoStamp = new Date().toISOString();

    Logger.log("doGet: word=" + word + " reading=" + reading);

    // Health-check: no params → confirm script is running
    if (!word && Object.keys(params).length === 0) {
      return jsonOk({ status: "ok", message: "Japanese Vocabulary Saver is running." });
    }

    if (!word) {
      return jsonOk({ status: "error", message: "Missing required parameter: word" });
    }

    // ── Get or create the Sheet ──────────────────────────────
    const ss    = SpreadsheetApp.getActiveSpreadsheet();
    let   sheet = ss.getSheetByName(SHEET_NAME);

    if (!sheet) {
      sheet = ss.insertSheet(SHEET_NAME);
      sheet.appendRow(["Word", "Reading", "Meaning", "Date", "Timestamp"]);

      // Style the header
      const hdr = sheet.getRange(1, 1, 1, 5);
      hdr.setFontWeight("bold");
      hdr.setBackground("#2c3e6b");
      hdr.setFontColor("#ffffff");
      sheet.setFrozenRows(1);
      Logger.log("Created sheet: " + SHEET_NAME);
    }

    // ── Server-side duplicate check ──────────────────────────
    const lastRow = sheet.getLastRow();
    if (lastRow > 1) {
      const existing = sheet.getRange(2, 1, lastRow - 1, 1).getValues().flat();
      if (existing.includes(word)) {
        Logger.log("Duplicate: " + word);
        return jsonOk({ status: "duplicate", word: word });
      }
    }

    // ── Append row ───────────────────────────────────────────
    sheet.appendRow([word, reading, meaning, dateStr, isoStamp]);
    sheet.autoResizeColumns(1, 5);

    const newRow = sheet.getLastRow();
    Logger.log("Saved row " + newRow + ": " + word);

    return jsonOk({ status: "ok", row: newRow, word: word });

  } catch (err) {
    Logger.log("Error: " + err.toString());
    return jsonOk({ status: "error", message: err.toString() });
  }
}

/** Helper: return a JSON ContentService TextOutput */
function jsonOk(obj) {
  return ContentService
    .createTextOutput(JSON.stringify(obj))
    .setMimeType(ContentService.MimeType.JSON);
}
