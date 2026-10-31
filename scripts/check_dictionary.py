#!/usr/bin/env python3
"""Validate the exact assets shipped by both apps, without a mobile runtime."""
import json
from pathlib import Path

root = Path(__file__).resolve().parents[1]
android = json.loads((root / 'android/app/src/main/assets/Dictionary.json').read_text())
ios = json.loads((root / 'ios/App/Resources/Dictionary.json').read_text())
assert android == ios, 'Platform dictionaries differ'
assert android['version'] > 0
categories = android['categories']
assert len(categories) == len(set(categories))
entries, snippets = android['entries'], android['snippets']
assert len(entries) == len({entry['command'] for entry in entries}), 'Duplicate command IDs'
assert len(snippets) == len({snippet['title'] for snippet in snippets}), 'Duplicate snippet IDs'
for entry in entries:
    assert entry['command'].strip() and entry['name'].strip()
    assert entry['category'] in categories, entry
    assert '\x00' not in entry['command']
assert next(entry for entry in entries if entry['name'] == 'alpha')['command'] == '\\alpha'
for snippet in snippets:
    assert all(snippet[field].strip() for field in ['title', 'category', 'code'])
    assert '\x00' not in snippet['code']
article = next(snippet for snippet in snippets if snippet['title'] == 'Article skeleton')['code']
assert article.startswith('\\documentclass') and '\n\\begin{document}\n' in article
print(f'Both platform assets agree: {len(entries)} commands, {len(snippets)} snippets; IDs, categories and source escaping pass.')
