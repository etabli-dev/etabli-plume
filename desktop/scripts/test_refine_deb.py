#!/usr/bin/env python3
# Copyright 2026 R. Heller
# SPDX-License-Identifier: Apache-2.0
import unittest
from refine_deb import desktop_entry


class DesktopEntryTest(unittest.TestCase):
    def test_name_and_measured_class_preserve_launcher_and_icon(self):
        before = "[Desktop Entry]\nName=etabli-plume\nExec=/opt/etabli-plume/bin/etabli-plume\nIcon=/opt/etabli-plume/lib/icon.png\n"
        after = desktop_entry(before, "com-raban-etabli-tex-desktop-MainKt")
        self.assertIn("Name=Établi Plume\n", after)
        self.assertIn("StartupWMClass=com-raban-etabli-tex-desktop-MainKt\n", after)
        for line in before.splitlines()[2:]:
            self.assertIn(line, after)
        self.assertEqual(after, desktop_entry(after, "com-raban-etabli-tex-desktop-MainKt"))

    def test_rejects_injected_desktop_entry_field(self):
        with self.assertRaises(ValueError):
            desktop_entry("[Desktop Entry]\nName=app\n", "class\nExec=other")

    def test_rejects_unexpected_template(self):
        with self.assertRaises(ValueError):
            desktop_entry("Name=app\n", "class")


if __name__ == "__main__":
    unittest.main()
