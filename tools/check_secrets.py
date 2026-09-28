#!/usr/bin/env python3
"""Fail on likely GitHub credentials/private keys without printing matched secrets.
Scans working files and nested ZIP/JAR entries. --history also scans Git blobs.
"""
from pathlib import Path
from io import BytesIO
import re, sys, zipfile, subprocess
ROOT = Path(__file__).resolve().parents[1]
patterns = [re.compile(rb'\bgh[pousr]_[A-Za-z0-9]{20,255}\b'),
            re.compile(rb'\bgithub_pat_[A-Za-z0-9_]{30,255}\b'),
            re.compile(rb'-----BEGIN (?:RSA |OPENSSH |EC )?PRIVATE KEY-----')]
failures = []
count = 0

def inspect(label, data, depth=0):
    global count
    count += 1
    if any(pattern.search(data) for pattern in patterns):
        failures.append(label)
    if depth < 4 and data.startswith(b'PK\x03\x04'):
        try:
            with zipfile.ZipFile(BytesIO(data)) as archive:
                for entry in archive.infolist():
                    if not entry.is_dir():
                        if entry.file_size > 64 * 1024 * 1024:
                            raise RuntimeError('Oversized archive entry: ' + label)
                        inspect(label + '!' + entry.filename, archive.read(entry), depth + 1)
        except zipfile.BadZipFile:
            pass

for path in ROOT.rglob('*'):
    relative = path.relative_to(ROOT)
    if any(part in {'.git', '.cache', '__pycache__', 'node_modules'} for part in relative.parts):
        continue
    if path.is_file():
        inspect(relative.as_posix(), path.read_bytes())

if '--history' in sys.argv:
    objects = subprocess.check_output(['git', 'rev-list', '--objects', '--all'], cwd=ROOT, text=True)
    for line in objects.splitlines():
        oid = line.split(' ', 1)[0]
        kind = subprocess.check_output(['git', 'cat-file', '-t', oid], cwd=ROOT, text=True).strip()
        if kind == 'blob':
            inspect('git-blob:' + oid, subprocess.check_output(['git', 'cat-file', 'blob', oid], cwd=ROOT))

if failures:
    print('FAIL: possible secret(s) in files/blobs below; matched values are deliberately not printed.')
    print('\n'.join(sorted(set(failures))))
    raise SystemExit(1)
print(f'PASS: scanned {count} files/archive entries/blobs; no targeted credential/private-key patterns found.')
