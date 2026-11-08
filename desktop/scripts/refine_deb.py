#!/usr/bin/env python3
# Copyright 2026 R. Heller
# SPDX-License-Identifier: Apache-2.0
"""Align the Debian launcher with the WM_CLASS measured by launch-smoke.sh."""
import argparse
import hashlib
import os
from pathlib import Path
import re
import subprocess
import tempfile


def desktop_entry(text, wm_class):
    if not re.fullmatch(r"[A-Za-z0-9_.$-]+", wm_class):
        raise ValueError("Invalid measured WM_CLASS")
    if text.count("[Desktop Entry]") != 1 or "\nName=" not in text:
        raise ValueError("Unexpected jpackage desktop entry")
    text = re.sub(r"^Name=.*$", "Name=Établi Plume", text, flags=re.MULTILINE)
    text = re.sub(r"^StartupWMClass=.*\n?", "", text, flags=re.MULTILINE)
    return text.rstrip() + f"\nStartupWMClass={wm_class}\n"


def refine(package, wm_class):
    with tempfile.TemporaryDirectory(prefix="plume-deb-", dir=package.parent) as work:
        root = Path(work) / "root"
        subprocess.run(["dpkg-deb", "--raw-extract", str(package), str(root)], check=True)
        entries = list(root.rglob("*.desktop"))
        if len(entries) != 1:
            raise ValueError("Expected exactly one packaged desktop entry")
        entry = entries[0]
        entry.write_text(desktop_entry(entry.read_text(encoding="utf-8"), wm_class), encoding="utf-8")
        # Keep Debian integrity metadata consistent with the changed launcher.
        sums = []
        for path in sorted(root.rglob("*")):
            relative = path.relative_to(root)
            if relative.parts[0] == "DEBIAN" or not path.is_file() or path.is_symlink():
                continue
            digest = hashlib.md5()
            with path.open("rb") as stream:
                for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                    digest.update(chunk)
            sums.append(f"{digest.hexdigest()}  {relative.as_posix()}\n")
        (root / "DEBIAN/md5sums").write_text("".join(sums), encoding="utf-8")
        rebuilt = Path(work) / package.name
        subprocess.run(["dpkg-deb", "--build", "--root-owner-group", str(root), str(rebuilt)], check=True)
        os.replace(rebuilt, package)
    print(f"Aligned {package.name}: Établi Plume, StartupWMClass={wm_class}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("package", type=Path)
    parser.add_argument("--wm-class-file", type=Path, default=Path("build/generated/linux-wm-class.txt"))
    args = parser.parse_args()
    refine(args.package.resolve(), args.wm_class_file.read_text(encoding="utf-8").strip())
