import json
import os

with open('/tmp/jira_response.json') as f:
    issue = json.load(f)

key = issue.get('key', '')
base_url = os.environ.get('JIRA_BASE_URL', '').rstrip('/')
fields = issue.get('fields', {})
summary = fields.get('summary', '')
jira_url = f"{base_url}/browse/{key}"


def extract_text(node):
    if not isinstance(node, dict):
        return ''
    t = node.get('type', '')
    if t == 'text':
        return node.get('text', '')
    if t == 'hardBreak':
        return '\n'
    text = ''.join(extract_text(c) for c in node.get('content', []))
    if t in ('paragraph', 'heading'):
        text += '\n'
    elif t == 'listItem':
        text = '- ' + text.strip() + '\n'
    return text


description_text = ''
desc = fields.get('description')
if desc and isinstance(desc, dict):
    description_text = extract_text(desc).strip()

lines = [f"### [{key}]({jira_url}): {summary}", ""]
if description_text:
    lines += ["**Description:**", description_text, ""]

with open('/tmp/jira_header.md', 'w') as f:
    f.write('\n'.join(lines))

print(f"Jira: {key} - {summary}")
