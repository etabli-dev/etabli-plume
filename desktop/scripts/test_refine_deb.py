#!/usr/bin/env python3
# Copyright 2026 R. Heller
# SPDX-License-Identifier: Apache-2.0
import unittest
import tempfile
from pathlib import Path
from refine_deb import desktop_entry, owned_desktop_entries


class DesktopEntryTest(unittest.TestCase):
    def test_name_and_measured_class_preserve_launcher_and_icon(self):
        before = "[Desktop Entry]\nName=etabli-plume\nExec=/opt/etabli-plume/bin/etabli-plume\nIcon=/opt/etabli-plume/lib/icon.png\n"
        after = desktop_entry(before, "com-raban-etabli-tex-desktop-MainKt")
        self.assertIn("Name=Établi Plume\n", after)
        self.assertIn("StartupWMClass=com-raban-etabli-tex-desktop-MainKt\n", after)
        for line in before.splitlines()[2:]:
            self.assertIn(line, after)
        self.assertEqual(after, desktop_entry(after, "com-raban-etabli-tex-desktop-MainKt"))

    def test_real_jpackage_layout_ignores_java_module_directory_and_updates_owned_copies(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "opt/etabli-plume/lib/runtime/legal/java.desktop").mkdir(parents=True)
            fixture = (Path(__file__).parent / "fixtures/jpackage-plume.desktop").read_text()
            paths = [root / "opt/etabli-plume/lib/etabli-plume-etabli-plume.desktop",
                     root / "usr/share/applications/etabli-plume.desktop"]
            for path in paths:
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text(fixture)
            unrelated = root / "unrelated.desktop"
            unrelated.write_text("[Desktop Entry]\nName=Other\nExec=/opt/other/bin/other\n")
            selected = owned_desktop_entries(root)
            self.assertEqual(set(paths), set(selected))
            for path in selected:
                path.write_text(desktop_entry(path.read_text(), "measured-class"))
                self.assertIn("StartupWMClass=measured-class", path.read_text())
            self.assertNotIn("StartupWMClass", unrelated.read_text())

    def test_fails_when_no_owned_launcher_exists(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            (root / "java.desktop").mkdir()
            with self.assertRaises(ValueError):
                owned_desktop_entries(root)

    def test_rejects_injected_desktop_entry_field(self):
        with self.assertRaises(ValueError):
            desktop_entry("[Desktop Entry]\nName=app\n", "class\nExec=other")

    def test_rejects_unexpected_template(self):
        with self.assertRaises(ValueError):
            desktop_entry("Name=app\n", "class")


if __name__ == "__main__":
    unittest.main()
