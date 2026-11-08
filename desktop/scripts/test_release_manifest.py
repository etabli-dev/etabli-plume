#!/usr/bin/env python3
# Copyright 2026 R. Heller
# SPDX-License-Identifier: Apache-2.0
import hashlib
from pathlib import Path
import tempfile
import unittest
from release_manifest import check_version, write_manifest


class ReleaseManifestTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)

    def packages(self):
        for extension in ("deb", "msi", "dmg"):
            (self.root / f"etabli-plume-1.0.0.{extension}").write_bytes(extension.encode())

    def test_manifest_hashes_all_platforms_and_repeats(self):
        self.packages()
        write_manifest(self.root, "1.0.0")
        first = (self.root / "SHA256SUMS").read_text()
        self.assertIn(hashlib.sha256(b"deb").hexdigest(), first)
        self.assertEqual(3, len(first.splitlines()))
        write_manifest(self.root, "1.0.0")
        self.assertEqual(first, (self.root / "SHA256SUMS").read_text())

    def test_rejects_missing_platform(self):
        self.packages()
        (self.root / "etabli-plume-1.0.0.msi").unlink()
        with self.assertRaises(ValueError):
            write_manifest(self.root, "1.0.0")

    def test_rejects_stale_version(self):
        self.packages()
        with self.assertRaises(ValueError):
            write_manifest(self.root, "1.0.1")

    def test_rejects_empty_package(self):
        self.packages()
        (self.root / "etabli-plume-1.0.0.dmg").write_bytes(b"")
        with self.assertRaises(ValueError):
            write_manifest(self.root, "1.0.0")

    def test_tag_matches_canonical_version(self):
        version_file = self.root / "VERSION"
        version_file.write_text("1.0.0\n")
        self.assertEqual("1.0.0", check_version("v1.0.0", version_file))
        with self.assertRaises(ValueError):
            check_version("v1.0.1", version_file)
        version_file.write_text("0.1.0\n")
        with self.assertRaises(ValueError):
            check_version("", version_file)


if __name__ == "__main__":
    unittest.main()
