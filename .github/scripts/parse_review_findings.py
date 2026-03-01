import json
import os
import re

with open("/tmp/review.md") as f:
    review = f.read()
with open("/tmp/changed_files.txt") as f:
    changed_files = [l.strip() for l in f if l.strip()]

commit_sha = os.environ.get("COMMIT_SHA", "")

header_pat = re.compile(
    r'\*\*\[([WC])-(\d+)\]\s*(.*?)\*\*(.*?)(?=-\s*\*\*Problem:\*\*)',
    re.DOTALL
)
path_pat = re.compile(
    r'(?:`([^`]+?):(\d+)(?:-\d+)?`|(\S+\.java)\s+line\s+(\d+))'
)


def resolve_path(short_path):
    for cf in changed_files:
        if cf.endswith(short_path) or cf.endswith("/" + short_path):
            return cf
    return None


findings = []
for m in header_pat.finditer(review):
    severity, num = m.group(1), m.group(2)
    title_text = m.group(3).strip()
    after_bold = m.group(4)

    pm = path_pat.search(title_text + " " + after_bold)
    if not pm:
        continue
    short_path = pm.group(1) or pm.group(3)
    line = int(pm.group(2) or pm.group(4))

    full_path = resolve_path(short_path)
    if not full_path:
        continue

    clean_title = re.sub(r'\s*[—\-]+\s*`[^`]+`\s*$', '', title_text).strip()

    body_start = m.end()
    next_end = len(review)
    for end_pat in [r'\n\*\*\[(?:W|C)-\d+\]', r'\n---', r'\n### ']:
        em = re.search(end_pat, review[body_start:])
        if em:
            next_end = min(next_end, body_start + em.start())
    body = review[body_start:next_end].strip()
    label = "Critical" if severity == "C" else "Warning"
    findings.append({
        "path": full_path,
        "line": line,
        "body": f"**[{severity}-{num}] {clean_title}** ({label})\n\n{body}"
    })

payload = {"event": "COMMENT", "body": review}
if commit_sha:
    payload["commit_id"] = commit_sha
if findings:
    payload["comments"] = [
        {"path": f["path"], "line": f["line"], "body": f["body"]}
        for f in findings
    ]

with open("/tmp/review_payload.json", "w") as f:
    json.dump(payload, f)

print(f"Parsed {len(findings)} inline comment(s) from review text")
for f in findings:
    print(f"  - {f['path']}:{f['line']}")
