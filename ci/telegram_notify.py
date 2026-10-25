#!/usr/bin/env python3
import os
import sys
import json
import html
import subprocess
import urllib.request
from datetime import datetime, timezone

MSG_ID_FILE = os.path.expanduser("/tmp/tg_build_msg_id.txt")

def get_env(key, default=""):
    return os.environ.get(key, default).strip()

def get_clean_token():
    token = get_env("TG_BOT_TOKEN")
    if token.lower().startswith("bot"):
        token = token[3:]
    return token.replace('"', '').replace("'", "").strip()

def get_clean_chat_id():
    return get_env("TG_CHAT_ID").replace('"', '').replace("'", "").strip()

def get_git_info():
    def run_git(args):
        try:
            return subprocess.check_output(["git"] + args, text=True).strip()
        except Exception:
            return ""

    short_sha = run_git(["rev-parse", "--short", "HEAD"]) or get_env("GITHUB_SHA")[:7]
    commit_msg = run_git(["log", "-1", "--pretty=%s"]) or "Manual trigger"
    branch = get_env("GITHUB_REF_NAME") or run_git(["branch", "--show-current"]) or "main"
    repo = get_env("GITHUB_REPOSITORY") or "AltafYafai/Android-DataBackup"
    actor = get_env("GITHUB_ACTOR") or "AltafYafai"
    server_url = get_env("GITHUB_SERVER_URL", "https://github.com")
    run_id = get_env("GITHUB_RUN_ID")
    run_url = f"{server_url}/{repo}/actions/runs/{run_id}" if run_id else f"{server_url}/{repo}"
    commit_url = f"{server_url}/{repo}/commit/{get_env('GITHUB_SHA', short_sha)}"

    return {
        "short_sha": short_sha,
        "commit_msg": commit_msg,
        "branch": branch,
        "repo": repo,
        "actor": actor,
        "run_url": run_url,
        "commit_url": commit_url,
    }

def send_message(token, chat_id, text):
    url = f"https://api.telegram.org/bot{token}/sendMessage"
    payload = json.dumps({
        "chat_id": chat_id,
        "text": text,
        "parse_mode": "HTML",
        "disable_web_page_preview": True
    }).encode("utf-8")
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            if data.get("ok"):
                return data.get("result", {}).get("message_id")
    except Exception as e:
        print(f"Failed to send Telegram message: {e}", file=sys.stderr)
    return None

def edit_message(token, chat_id, message_id, text):
    if not message_id:
        return False
    url = f"https://api.telegram.org/bot{token}/editMessageText"
    payload = json.dumps({
        "chat_id": chat_id,
        "message_id": int(message_id),
        "text": text,
        "parse_mode": "HTML"
    }).encode("utf-8")
    req = urllib.request.Request(url, data=payload, headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            return data.get("ok", False)
    except Exception as e:
        print(f"Failed to edit Telegram message: {e}", file=sys.stderr)
    return False

def send_document(token, chat_id, file_path, caption="", custom_filename=None):
    if not os.path.isfile(file_path):
        print(f"File not found for upload: {file_path}", file=sys.stderr)
        return False
    filename = custom_filename or os.path.basename(file_path)
    cmd = [
        "curl", "-sS", "--max-time", "620",
        "--form-string", f"chat_id={chat_id}",
        "--form-string", "parse_mode=HTML",
        "--form-string", f"caption={caption}",
        "-F", f"document=@{file_path};filename={filename}",
        f"https://api.telegram.org/bot{token}/sendDocument"
    ]
    try:
        res = subprocess.run(cmd, capture_output=True, text=True)
        resp_data = json.loads(res.stdout) if res.stdout else {}
        if resp_data.get("ok"):
            print(f"Document uploaded successfully: {file_path}")
            return True
        else:
            print(f"Telegram upload error response: {res.stdout}", file=sys.stderr)
            # If HTML parsing failed, retry with stripped plain text caption
            plain_caption = caption.replace("<b>", "").replace("</b>", "").replace("<i>", "").replace("</i>", "").replace("<code>", "").replace("</code>", "")
            fallback_cmd = [
                "curl", "-sS", "--max-time", "620",
                "--form-string", f"chat_id={chat_id}",
                "--form-string", f"caption={plain_caption}",
                "-F", f"document=@{file_path};filename={filename}",
                f"https://api.telegram.org/bot{token}/sendDocument"
            ]
            res2 = subprocess.run(fallback_cmd, capture_output=True, text=True)
            resp2 = json.loads(res2.stdout) if res2.stdout else {}
            if resp2.get("ok"):
                print(f"Document uploaded successfully with fallback caption: {file_path}")
                return True
            else:
                print(f"Fallback upload error: {res2.stdout}", file=sys.stderr)
    except Exception as e:
        print(f"Failed to upload document via curl: {e}", file=sys.stderr)
    return False

def save_msg_id(msg_id):
    if msg_id:
        try:
            with open(MSG_ID_FILE, "w") as f:
                f.write(str(msg_id))
        except Exception as e:
            print(f"Failed to save message ID: {e}", file=sys.stderr)

def read_msg_id():
    if os.path.isfile(MSG_ID_FILE):
        try:
            with open(MSG_ID_FILE, "r") as f:
                return f.read().strip()
        except Exception:
            return ""
    return os.environ.get("START_MSG_ID", "").strip()

def on_start():
    token = get_clean_token()
    chat_id = get_clean_chat_id()
    if not token or not chat_id:
        print("Telegram credentials not configured. Skipping start notification.")
        return

    info = get_git_info()
    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    text = (
        "🔨 <b>BackupVault Build Started</b>\n\n"
        f"📦 <b>Repository:</b> <code>{html.escape(info['repo'])}</code>\n"
        f"🌿 <b>Branch:</b> <code>{html.escape(info['branch'])}</code>\n"
        f"📝 <b>Commit:</b> <a href=\"{info['commit_url']}\"><code>{html.escape(info['short_sha'])}</code></a> — <i>{html.escape(info['commit_msg'])}</i>\n"
        f"👤 <b>Author:</b> {html.escape(info['actor'])}\n"
        f"🕒 <b>Started at:</b> {now_utc}\n"
        "⏳ <b>Status:</b> Compiling arm64 Foss Release APK...\n\n"
        f"🔗 <a href=\"{info['run_url']}\">View Live GitHub Actions Run</a>"
    )
    msg_id = send_message(token, chat_id, text)
    if msg_id:
        save_msg_id(msg_id)
        print(f"Start notification sent. Message ID: {msg_id}")

def on_success(apk_path=None):
    token = get_clean_token()
    chat_id = get_clean_chat_id()
    if not token or not chat_id:
        print("Telegram credentials not configured. Skipping success notification.")
        return

    info = get_git_info()
    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    msg_id = read_msg_id()

    if msg_id:
        edit_message(
            token, chat_id, msg_id,
            "✅ <b>BackupVault Build Successful!</b>\nAPK file uploaded below."
        )

    # Find APK file
    target_apk = apk_path
    if not target_apk or not os.path.isfile(target_apk):
        for candidate in ["source", ".", ".."]:
            for root, _, files in os.walk(candidate):
                for file in files:
                    if file.endswith(".apk") and "release" in file.lower():
                        target_apk = os.path.join(root, file)
                        break
                if target_apk and os.path.isfile(target_apk):
                    break

    if target_apk and os.path.isfile(target_apk):
        size_bytes = os.path.getsize(target_apk)
        size_mb = f"{size_bytes / (1024 * 1024):.1f} MB"
        apk_name = os.path.basename(target_apk)

        caption = (
            "✅ <b>BackupVault v1.0 — Build Successful</b>\n\n"
            f"📦 <b>File:</b> <code>{html.escape(apk_name)}</code> ({size_mb})\n"
            f"🌿 <b>Branch:</b> <code>{html.escape(info['branch'])}</code>\n"
            f"📝 <b>Commit:</b> <a href=\"{info['commit_url']}\"><code>{html.escape(info['short_sha'])}</code></a> — <i>{html.escape(info['commit_msg'])}</i>\n"
            f"👤 <b>Built by:</b> {html.escape(info['actor'])}\n"
            f"🕒 <b>Built at:</b> {now_utc}\n"
            f"🔗 <a href=\"{info['run_url']}\">View GitHub Actions Run</a>"
        )
        ok = send_document(token, chat_id, target_apk, caption=caption)
        if not ok:
            print("Failed to upload APK to Telegram", file=sys.stderr)
            sys.exit(1)
    else:
        send_message(
            token, chat_id,
            f"✅ <b>BackupVault Build Successful!</b> (No APK found to send)\n🔗 <a href=\"{info['run_url']}\">View Run</a>"
        )

def on_failure(log_path=None):
    token = get_clean_token()
    chat_id = get_clean_chat_id()
    if not token or not chat_id:
        print("Telegram credentials not configured. Skipping failure notification.")
        return

    info = get_git_info()
    now_utc = datetime.now(timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    msg_id = read_msg_id()

    if msg_id:
        edit_message(
            token, chat_id, msg_id,
            "❌ <b>BackupVault Build Failed!</b>\nSee detailed error log below."
        )

    # Locate build log
    actual_log = log_path
    if not actual_log or not os.path.isfile(actual_log):
        for candidate in ["build.log", "../build.log", "source/build.log"]:
            if os.path.isfile(candidate):
                actual_log = candidate
                break

    error_snippet = "No build log file found."
    if actual_log and os.path.isfile(actual_log):
        try:
            with open(actual_log, "r", encoding="utf-8", errors="replace") as f:
                lines = f.readlines()
                error_snippet = "".join(lines[-25:]).strip()
        except Exception as e:
            error_snippet = f"Could not read log file: {e}"

    if len(error_snippet) > 2500:
        error_snippet = error_snippet[-2500:]

    fail_text = (
        "❌ <b>BackupVault Build FAILED!</b>\n\n"
        f"📦 <b>Repository:</b> <code>{html.escape(info['repo'])}</code>\n"
        f"🌿 <b>Branch:</b> <code>{html.escape(info['branch'])}</code>\n"
        f"📝 <b>Commit:</b> <a href=\"{info['commit_url']}\"><code>{html.escape(info['short_sha'])}</code></a> — <i>{html.escape(info['commit_msg'])}</i>\n"
        f"👤 <b>Author:</b> {html.escape(info['actor'])}\n"
        f"🕒 <b>Failed at:</b> {now_utc}\n"
        f"🔗 <a href=\"{info['run_url']}\">View Full GitHub Actions Run Log</a>\n\n"
        "⚠️ <b>Error Preview:</b>\n"
        f"<pre>{html.escape(error_snippet)}</pre>"
    )

    send_message(token, chat_id, fail_text)

    if actual_log and os.path.isfile(actual_log):
        run_id = get_env("GITHUB_RUN_ID", "local")
        caption = f"❌ Build Log — BackupVault ({info['short_sha']})\nRun: {info['run_url']}"
        send_document(
            token, chat_id, actual_log,
            caption=caption,
            custom_filename=f"build_failed_{run_id}_{info['short_sha']}.log"
        )

def main():
    if len(sys.argv) < 2:
        print("Usage: telegram_notify.py <start|success|failure> [file_path]")
        sys.exit(1)

    action = sys.argv[1].lower()
    file_arg = sys.argv[2] if len(sys.argv) > 2 else None

    if action == "start":
        on_start()
    elif action == "success":
        on_success(file_arg)
    elif action == "failure":
        on_failure(file_arg)
    else:
        print(f"Unknown action: {action}")
        sys.exit(1)

if __name__ == "__main__":
    main()
