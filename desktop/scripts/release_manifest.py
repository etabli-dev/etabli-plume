#!/usr/bin/env python3
# Copyright 2026 R. Heller
# SPDX-License-Identifier: Apache-2.0
"""Validate the desktop version and checksum exactly one package per platform."""
import argparse
import hashlib
from pathlib import Path
import re

VERSION_FILE = Path(__file__).resolve().parents[1] / "VERSION"


def check_version(tag, version_file=VERSION_FILE):
    version = version_file.read_text(encoding="utf-8").strip()
    if not re.fullmatch(r"[1-9][0-9]*\.[0-9]+\.[0-9]+", version):
        raise ValueError("VERSION must be major.minor.patch with major >= 1")
    if tag and tag != "v" + version:
        raise ValueError(f"Tag {tag!r} does not match desktop/VERSION ({version})")
    return version


def write_manifest(directory, version):
    packages = sorted(directory.iterdir())
    packages = [p for p in packages if p.name != "SHA256SUMS"]
    if len(packages) != 3 or {p.suffix for p in packages} != {".deb", ".msi", ".dmg"}:
        raise ValueError("Expected exactly one .deb, .msi and .dmg")
    rows = []
    for package in packages:
        if package.is_symlink() or not package.is_file() or package.stat().st_size == 0:
            raise ValueError(f"Invalid or empty package: {package.name}")
        if any(c in package.name for c in "\r\n\\") or not re.search(
            r"(?<![0-9.])" + re.escape(version) + r"(?![0-9.])", package.stem
        ):
            raise ValueError(f"Package does not carry expected version: {package.name}")
        digest = hashlib.sha256()
        with package.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
        rows.append(f"{digest.hexdigest()}  {package.name}\n")
    (directory / "SHA256SUMS").write_text("".join(rows), encoding="utf-8")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check-version", default="", metavar="TAG")
    parser.add_argument("--packages", type=Path)
    args = parser.parse_args()
    try:
        version = check_version(args.check_version)
        if args.packages:
            write_manifest(args.packages, version)
    except (ValueError, OSError) as error:
        parser.exit(1, f"Release validation failed: {error}\n")
    print(f"Desktop version {version}: release validation passed")
