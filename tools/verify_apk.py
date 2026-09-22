#!/usr/bin/env python3
from pathlib import Path
import hashlib, sys, zipfile

if len(sys.argv) != 2:
    raise SystemExit("Uso: verify_apk.py <arquivo.apk>")

apk=Path(sys.argv[1])
if not apk.is_file():
    raise SystemExit(f"APK não encontrado: {apk}")
if apk.stat().st_size < 100_000:
    raise SystemExit(f"APK suspeitamente pequeno: {apk.stat().st_size} bytes")

with zipfile.ZipFile(apk) as z:
    names=set(z.namelist())
    required={"AndroidManifest.xml","classes.dex","resources.arsc"}
    missing=required-names
    if missing:
        raise SystemExit(f"APK inválido; faltam: {sorted(missing)}")

sha=hashlib.sha256(apk.read_bytes()).hexdigest()
out=apk.with_suffix(apk.suffix+".sha256")
out.write_text(f"{sha}  {apk.name}\n")
print(f"APK OK: {apk} ({apk.stat().st_size/1024/1024:.2f} MiB)")
print(f"SHA-256: {sha}")
print(f"Checksum: {out}")
