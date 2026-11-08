#!/usr/bin/env python3
"""Release packaging integration tests using disposable Git histories only."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import shlex
import stat
import subprocess
import sys
import tempfile
from types import SimpleNamespace
import unittest
from unittest import mock
import zipfile


TOOL = Path(__file__).with_name("release_bundle.py")


def git(repo, *args, data=None, env=None):
    return subprocess.run(["git", "-C", str(repo), *args], input=data,
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          check=True, env=env).stdout


def commit(repo):
    git(repo, "add", "--all")
    git(repo, "commit", "-qm", "fixture")
    return git(repo, "rev-parse", "HEAD").decode().strip()


def init(repo):
    repo.mkdir()
    git(repo, "init", "-q", "-b", "main")
    git(repo, "config", "user.name", "Test")
    git(repo, "config", "user.email", "test@example.invalid")


class ReleaseBundleTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.dep = self.root / "dependency"
        init(self.dep)
        (self.dep / "LICENSE").write_text("Dependency license\n")
        (self.dep / "library.c").write_text("int dependency;\n")
        self.dep_revision = commit(self.dep)
        self.core = self.root / "core"
        init(self.core)
        (self.core / "LICENSE").write_text("Core license\n")
        (self.core / "deleted.c").write_text("remove me\n")
        (self.core / "binary.dat").write_bytes(b"old\0binary")
        (self.core / "script.sh").write_text("#!/bin/sh\n")
        git(self.core, "-c", "protocol.file.allow=always", "submodule", "add", "-q",
            str(self.dep), "nested")
        self.core_base = commit(self.core)
        (self.core / "deleted.c").unlink()
        (self.core / "binary.dat").write_bytes(b"new\0binary\xff")
        (self.core / "script.sh").chmod(0o755)
        (self.core / "rcheevos").mkdir()
        (self.core / "rcheevos" / "embedded.c").write_text("int embedded;\n")
        self.core_revision = commit(self.core)
        self.app = self.root / "app"
        init(self.app)
        (self.app / "LICENSE").write_text("App license\n")
        (self.app / "delete.txt").write_text("deleted app text\n")
        (self.app / "binary.dat").write_bytes(b"old\0app")
        (self.app / "run.sh").write_text("#!/bin/sh\n")
        git(self.app, "-c", "protocol.file.allow=always", "submodule", "add", "-q",
            str(self.core), "melonDS-android-lib")
        git(self.app / "melonDS-android-lib", "checkout", "-q", self.core_base)
        self.app_base = commit(self.app)
        git(self.app / "melonDS-android-lib", "checkout", "-q", self.core_revision)
        git(self.app / "melonDS-android-lib", "-c", "protocol.file.allow=always",
            "submodule", "update", "--init", "--recursive")
        for name in ("oboe", "faad2", "enet"):
            git(self.app, "-c", "protocol.file.allow=always", "submodule", "add", "-q",
                str(self.dep), "app/src/main/cpp/" + name)
        (self.app / "delete.txt").unlink()
        (self.app / "binary.dat").write_bytes(b"new\0app\xff")
        (self.app / "run.sh").chmod(0o755)
        (self.app / "docs").mkdir()
        (self.app / "docs" / "source.md").write_text("Public build instructions\n")
        (self.app / "link").symlink_to("docs/source.md")
        self.revision = commit(self.app)
        (self.app / "untracked-secret").write_text("must never be packaged")
        self.apk = self.root / "candidate.apk"
        self.apk.write_bytes(b"APK fixture bytes\0\xff")
        self.output = self.root / "release"

    def package(self, *extra):
        return subprocess.run([sys.executable, str(TOOL), "--repo", str(self.app),
                               "--revision", self.revision,
                               "--app-base", self.app_base, "--core-base", self.core_base,
                               "--apk", str(self.apk), "--output", str(self.output),
                               "--version", "2.1", *extra],
                              capture_output=True, text=True)

    def test_complete_sources_patches_modes_and_distribution_checksums(self):
        # Catches filtered app patches, missing recursive source, altered modes, or false acceptance.
        report = self.root / "checks.json"
        report.write_text(json.dumps({"app_revision": self.revision,
                                      "device_update": "not performed"}))
        index_before = (self.app / ".git/index").read_bytes()
        result = self.package("--verification-report", str(report))
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertEqual((self.app / ".git/index").read_bytes(), index_before)
        self.assertEqual(git(self.app, "rev-parse", "HEAD").decode().strip(), self.revision)
        source = self.output / "melonDS-UD-nightly-2.1-source.zip"
        self.assertTrue(source.is_file(), "Release source asset name must match the download recipe")
        with zipfile.ZipFile(source) as archive:
            names = archive.namelist()
            expected_names = {
                "source/.gitmodules", "source/LICENSE", "source/binary.dat",
                "source/run.sh", "source/docs/source.md", "source/link",
                "source/melonDS-android-lib/.gitmodules", "source/melonDS-android-lib/LICENSE",
                "source/melonDS-android-lib/binary.dat", "source/melonDS-android-lib/script.sh",
                "source/melonDS-android-lib/rcheevos/embedded.c",
                "source/melonDS-android-lib/nested/LICENSE",
                "source/melonDS-android-lib/nested/library.c",
            }
            for name in ("oboe", "faad2", "enet"):
                expected_names.update({"source/app/src/main/cpp/" + name + "/LICENSE",
                                       "source/app/src/main/cpp/" + name + "/library.c"})
            self.assertEqual(set(names), expected_names)
            self.assertIn("source/.gitmodules", names)
            self.assertIn("source/docs/source.md", names)
            self.assertIn("source/melonDS-android-lib/rcheevos/embedded.c", names)
            self.assertIn("source/melonDS-android-lib/nested/library.c", names)
            for name in ("oboe", "faad2", "enet"):
                self.assertIn("source/app/src/main/cpp/" + name + "/library.c", names)
            self.assertNotIn("source/untracked-secret", names)
            self.assertNotIn("source/delete.txt", names)
            self.assertEqual(archive.read("source/binary.dat"), b"new\0app\xff")
            self.assertEqual(archive.getinfo("source/run.sh").external_attr >> 16, 0o100755)
            self.assertTrue(stat.S_ISLNK(archive.getinfo("source/link").external_attr >> 16))
            self.assertEqual(archive.read("source/link"), b"docs/source.md")
        for repo, base, target, kind in ((self.app, self.app_base, self.revision, "app"),
                                         (self.core, self.core_base, self.core_revision, "core")):
            patch = next(self.output.glob("*-" + kind + "-source.patch")).read_bytes()
            with tempfile.TemporaryDirectory() as tmp:
                env = {**os.environ, "GIT_INDEX_FILE": str(Path(tmp) / "index")}
                git(repo, "read-tree", base, env=env)
                git(repo, "apply", "--cached", "--check", data=patch, env=env)
                git(repo, "apply", "--cached", data=patch, env=env)
                self.assertEqual(git(repo, "write-tree", env=env),
                                 git(repo, "rev-parse", target + "^{tree}"))
        manifest = json.loads((self.output / "BUILD-MANIFEST.json").read_text())
        self.assertEqual(manifest["app"]["revision"], self.revision)
        self.assertEqual(manifest["core"]["revision"], self.core_revision)
        self.assertEqual(manifest["caller_verification"]["device_update"], "not performed")
        self.assertEqual(manifest["apk"]["sha256"], hashlib.sha256(self.apk.read_bytes()).hexdigest())
        self.assertEqual(len(manifest["submodules"]), 5)
        self.assertIn({"path": "melonDS-android-lib/nested", "revision": self.dep_revision,
                       "tree": git(self.dep, "rev-parse", "HEAD^{tree}").decode().strip()},
                      manifest["submodules"])
        self.assertEqual(manifest["apk"]["file"], "app-gitHub-nightly-release.apk")
        bundle = self.output / "melonDS-UD-nightly-2.1.zip"
        with zipfile.ZipFile(bundle) as archive:
            for line in archive.read("SHA256SUMS").decode().splitlines():
                digest, name = line.split("  ", 1)
                self.assertEqual(hashlib.sha256(archive.read(name)).hexdigest(), digest)
            for name in archive.namelist():
                self.assertEqual(archive.read(name), (self.output / name).read_bytes())
        checksum, filename = (self.output / "BUNDLE-SHA256SUMS").read_text().strip().split("  ")
        self.assertEqual(filename, bundle.name)
        self.assertEqual(checksum, hashlib.sha256(bundle.read_bytes()).hexdigest())

    def test_download_checksums_cover_exactly_apk_and_complete_zip(self):
        # Catches publishing payload diagnostics or hashing source ZIPs instead of both downloads.
        result = self.package()
        self.assertEqual(result.returncode, 0, result.stderr)
        checksums = self.output / "DOWNLOADS-SHA256SUMS"
        self.assertTrue(checksums.is_file(), "The public downloads need one external checksum file")
        entries = [line.split("  ", 1) for line in checksums.read_text().splitlines()]
        self.assertEqual([name for _, name in entries], [
            "app-gitHub-nightly-release.apk", "melonDS-UD-nightly-2.1.zip",
        ])
        for checksum, name in entries:
            self.assertEqual(checksum, hashlib.sha256((self.output / name).read_bytes()).hexdigest())
        with zipfile.ZipFile(self.output / "melonDS-UD-nightly-2.1.zip") as archive:
            self.assertNotIn("DOWNLOADS-SHA256SUMS", archive.namelist())
            self.assertIn("SHA256SUMS", archive.namelist())

    def test_readme_recipe_pins_index_and_initializes_recursive_sources(self):
        # Catches instructions that patch the working tree but leave old gitlinks in the index.
        result = self.package("--release-date", "2026-11-08")
        self.assertEqual(result.returncode, 0, result.stderr)
        readme = (self.output / "README.txt").read_text()
        commands = [line.strip() for line in readme.splitlines() if line.startswith("  git ")]
        self.assertEqual(commands, [
            "git apply --check --index melonDS-UD-2.1-app-source.patch",
            "git apply --index melonDS-UD-2.1-app-source.patch",
            "git submodule sync --recursive",
            "git submodule update --init --recursive",
        ])
        self.assertIn("Independent UD fork modifications", readme)
        self.assertIn("2026-11-08", readme)
        self.assertIn("Rafael Caetano", readme)
        self.assertIn("melonDS contributors", readme)
        manifest = json.loads((self.output / "BUILD-MANIFEST.json").read_text())
        self.assertEqual(manifest["release_date"], "2026-11-08")
        checkout = self.root / "recipe-checkout"
        git(self.app, "clone", "-q", str(self.app), str(checkout))
        git(checkout, "checkout", "-q", self.app_base)
        for command in commands:
            arguments = shlex.split(command)[1:]
            if arguments[0] == "apply":
                arguments[-1] = str(self.output / arguments[-1])
            git(checkout, "-c", "protocol.file.allow=always", *arguments)
        self.assertEqual(git(checkout, "write-tree"), git(self.app, "rev-parse", self.revision + "^{tree}"))
        self.assertEqual((checkout / "melonDS-android-lib/nested/library.c").read_text(),
                         "int dependency;\n")

    def test_refuses_private_tracked_source_instead_of_silently_filtering(self):
        # Catches publication of private notes or lossy omission of tracked source.
        for name in ("local/notes.txt", "AGENTS.md", "docs/HANDOFF-current.md",
                     "docs/plans/future.md", "private.jks"):
            with self.subTest(name=name):
                path = self.app / name
                path.parent.mkdir(parents=True, exist_ok=True)
                path.write_text("private")
                self.revision = commit(self.app)
                result = self.package()
                self.assertNotEqual(result.returncode, 0)
                self.assertIn("Refusing private", result.stderr)
                self.assertFalse(self.output.exists())
                path.unlink()
                self.revision = commit(self.app)

    def test_refuses_private_base_even_when_deleted_at_target(self):
        # A deleted private base file would still leak its content in the cumulative patch.
        private = self.app / "CLAUDE.md"
        private.write_text("private base")
        self.app_base = commit(self.app)
        private.unlink()
        self.revision = commit(self.app)
        result = self.package()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("Refusing private", result.stderr)

    def test_refuses_existing_outputs_and_symbolic_revision(self):
        # Catches accidental release overwrite and mutable source identities.
        self.output.mkdir()
        marker = self.output / "keep.txt"
        marker.write_text("keep")
        result = self.package()
        self.assertNotEqual(result.returncode, 0)
        self.assertEqual(marker.read_text(), "keep")
        marker.unlink()
        self.revision = "HEAD"
        result = self.package()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn("exact commit", result.stderr)

    def test_preserves_ten_gibibytes_after_estimated_packaging(self):
        # Catches checking current free space without budgeting the generated outputs.
        spec = importlib.util.spec_from_file_location("release_bundle", TOOL)
        module = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(module)
        args = SimpleNamespace(repo=self.app, revision=self.revision,
                               app_base=self.app_base, core_base=self.core_base,
                               apk=self.apk, output=self.output, version="2.1",
                               release_date="2026-11-08", verification_report=None)
        with mock.patch.object(module.shutil, "disk_usage",
                               return_value=SimpleNamespace(free=10 * 1024 ** 3)):
            with self.assertRaisesRegex(ValueError, "less than 10 GiB"):
                module.package(args)
        self.assertFalse(self.output.exists())


if __name__ == "__main__":
    unittest.main()
