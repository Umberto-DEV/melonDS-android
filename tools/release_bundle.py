#!/usr/bin/env python3
"""Package exact committed sources and an existing APK; never build, sign or publish."""
import argparse
from datetime import date, datetime, timezone
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile


APP_BASE = "273211564bea8f9da0c9fe71b4006a06f5d4a2b5"
CORE_BASE = "431ab4bd0003c4356e25fce89640ae1004579b9b"
RESERVE = 10 * 1024 ** 3


def git(repo, *args, data=None, env=None):
    clean = {k: v for k, v in os.environ.items() if not k.startswith("GIT_")}
    result = subprocess.run(["git", "-C", str(repo), *args], input=data,
                            stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                            env={**clean, **(env or {})})
    if result.returncode:
        raise ValueError(result.stderr.decode(errors="replace").strip())
    return result.stdout


def exact_commit(repo, value):
    if not re.fullmatch(r"[0-9a-f]{40}|[0-9a-f]{64}", value):
        raise ValueError("Use an exact commit SHA, not a branch, tag or abbreviated revision")
    if git(repo, "rev-parse", "--verify", value + "^{commit}").decode().strip() != value:
        raise ValueError("Not an exact commit: " + value)
    return value


def entries(repo, revision):
    result = []
    for record in git(repo, "ls-tree", "-rz", "--full-tree", revision).split(b"\0"):
        if not record:
            continue
        header, raw_name = record.split(b"\t", 1)
        mode, kind, oid = header.decode().split()
        name = raw_name.decode("utf-8")
        parts = PurePosixPath(name).parts
        lowered = [part.lower() for part in parts]
        private = {"local", ".agents", ".codex", ".aws", ".ssh", ".git", "plans", "planning"}
        if (not parts or name.startswith("/") or ".." in parts or "\\" in name
                or any(part in private for part in lowered)
                or re.match(r"^(agents|claude|skill)\.md$|^handoff(?:[._-]|$)|^plan(?:[._-]|$)",
                            lowered[-1])
                or lowered[-1] == "local.properties"
                or PurePosixPath(name).suffix.lower() in
                {".jks", ".keystore", ".pem", ".key", ".p12", ".nds", ".sav", ".dsv"}):
            raise ValueError("Refusing private or unsafe tracked path: " + name)
        if mode not in {"100644", "100755", "120000", "160000"}:
            raise ValueError("Unsupported Git mode: " + mode + " " + name)
        result.append((name, mode, oid, kind))
    return result


def collect(repo, revision, prefix="", pins=None):
    pins = [] if pins is None else pins
    tracked = entries(repo, revision)
    groups = [(repo, prefix, tracked)]
    for name, mode, oid, _ in tracked:
        if mode != "160000":
            continue
        child = repo / name
        if not (child / ".git").exists():
            raise ValueError("Initialize the pinned submodule locally: " + prefix + name)
        exact_commit(child, oid)
        pins.append({"path": prefix + name, "revision": oid,
                     "tree": git(child, "rev-parse", oid + "^{tree}").decode().strip()})
        groups.extend(collect(child, oid, prefix + name + "/", pins)[0])
    return groups, pins


def patch(repo, base, target):
    entries(repo, base)  # Deleted private files would also leak through a patch.
    entries(repo, target)
    delta = git(repo, "diff", "--binary", "--full-index", "--no-ext-diff",
                "--no-textconv", "--no-renames", base, target, "--")
    with tempfile.TemporaryDirectory(prefix="release-index-") as temporary:
        env = {"GIT_INDEX_FILE": str(Path(temporary) / "index")}
        git(repo, "read-tree", base, env=env)
        if delta:
            git(repo, "apply", "--cached", "--check", "--whitespace=nowarn", data=delta, env=env)
            git(repo, "apply", "--cached", "--whitespace=nowarn", data=delta, env=env)
        if git(repo, "write-tree", env=env) != git(repo, "rev-parse", target + "^{tree}"):
            raise ValueError("Patch did not reconstruct the complete target tree")
    return delta


def digest(data):
    return hashlib.sha256(data).hexdigest()


def zip_info(name, mode="100644"):
    info = zipfile.ZipInfo(name, (1980, 1, 1, 0, 0, 0))
    info.create_system = 3
    info.external_attr = (0o120777 if mode == "120000" else int(mode, 8)) << 16
    info.compress_type = zipfile.ZIP_DEFLATED
    return info


def check_zip(path, expected):
    with zipfile.ZipFile(path) as archive:
        if archive.namelist() != list(expected):
            raise ValueError("Archive member list mismatch: " + str(path))
        for info in archive.infolist():
            sha256, mode = expected[info.filename]
            if digest(archive.read(info)) != sha256 or info.external_attr != zip_info(info.filename, mode).external_attr:
                raise ValueError("Archive readback mismatch: " + info.filename)


def source_zip(path, groups):
    expected = {}
    with zipfile.ZipFile(path, "x") as archive:
        for repo, prefix, tracked in groups:
            process = subprocess.Popen(["git", "-C", str(repo), "cat-file", "--batch"],
                                       stdin=subprocess.PIPE, stdout=subprocess.PIPE,
                                       env={k: v for k, v in os.environ.items() if not k.startswith("GIT_")})
            try:
                for name, mode, oid, kind in tracked:
                    if mode == "160000":
                        continue
                    process.stdin.write((oid + "\n").encode())
                    process.stdin.flush()
                    header = process.stdout.readline().split()
                    if len(header) != 3 or header[:2] != [oid.encode(), b"blob"] or kind != "blob":
                        raise ValueError("Missing source blob: " + prefix + name)
                    data = process.stdout.read(int(header[2]))
                    if process.stdout.read(1) != b"\n" or len(data) != int(header[2]):
                        raise ValueError("Incomplete source blob: " + prefix + name)
                    member = "source/" + prefix + name
                    if member in expected:
                        raise ValueError("Duplicate source path: " + member)
                    archive.writestr(zip_info(member, mode), data)
                    expected[member] = (digest(data), mode)
            finally:
                process.stdin.close()
                process.stdout.close()
                if process.wait() != 0:
                    raise ValueError("git cat-file failed")
    check_zip(path, expected)
    return len(expected)


def package(args):
    repo, output = args.repo.resolve(), args.output.absolute()
    if output.exists() and (not output.is_dir() or any(output.iterdir())):
        raise ValueError("Output must be a new or empty directory; refusing to overwrite")
    if not re.fullmatch(r"[A-Za-z0-9][A-Za-z0-9._-]*", args.version):
        raise ValueError("Unsafe version string")
    release_date = date.fromisoformat(args.release_date).isoformat()
    revision = exact_commit(repo, args.revision)
    app_base = exact_commit(repo, args.app_base)
    groups, pins = collect(repo, revision)
    core_pin = next((pin for pin in pins if pin["path"] == "melonDS-android-lib"), None)
    if core_pin is None:
        raise ValueError("App must pin the melonDS-android-lib submodule")
    core = repo / "melonDS-android-lib"
    core_base = exact_commit(core, args.core_base)
    patches = {"app": patch(repo, app_base, revision),
               "core": patch(core, core_base, core_pin["revision"])}
    report = json.loads(args.verification_report.read_text()) if args.verification_report else None
    if report is not None and not isinstance(report, dict):
        raise ValueError("Verification report must be a JSON object of caller declarations")
    apk = args.apk.read_bytes()
    if not apk or args.apk.suffix.lower() != ".apk":
        raise ValueError("Provide a nonempty APK file")
    license_data = git(repo, "show", revision + ":LICENSE")
    total = len(apk) * 3 + sum(len(delta) * 3 for delta in patches.values())
    for directory, _, tracked in groups:
        ids = "".join(oid + "\n" for _, mode, oid, _ in tracked if mode != "160000")
        total += 3 * sum(int(line.split()[2]) for line in
                         git(directory, "cat-file", "--batch-check", data=ids.encode()).splitlines())
    ancestor = output
    while not ancestor.exists():
        ancestor = ancestor.parent
    if shutil.disk_usage(ancestor).free - total < RESERVE:
        raise ValueError("Packaging estimate would leave less than 10 GiB free")
    output.mkdir(parents=True, exist_ok=True)
    stem = "melonDS-UD-" + args.version
    expected = {}

    def write(name, data):
        with (output / name).open("xb") as handle:
            handle.write(data)
        expected[name] = (digest(data), "100644")

    apk_name = "app-gitHub-nightly-release.apk"
    source_name = "melonDS-UD-nightly-" + args.version + "-source.zip"
    write(apk_name, apk)
    source_count = source_zip(output / source_name, groups)
    expected[source_name] = (digest((output / source_name).read_bytes()), "100644")
    for kind, delta in patches.items():
        write(stem + "-" + kind + "-source.patch", delta)
    write("LICENSE", license_data)
    manifest = {
        "schema_version": 1, "version": args.version, "release_date": release_date,
        "app": {"revision": revision, "base": app_base,
                "tree": git(repo, "rev-parse", revision + "^{tree}").decode().strip()},
        "core": {**core_pin, "base": core_base}, "submodules": pins,
        "apk": {"file": apk_name, "sha256": digest(apk), "size": len(apk)},
        "source": {"file": source_name, "sha256": expected[source_name][0],
                   "blob_count": source_count, "selection": "all committed blobs, recursively pinned submodules"},
        "caller_verification": report,
        "package_checks": {"app_patch_full_tree": "passed", "core_patch_full_tree": "passed",
                           "source_zip_blob_and_mode_readback": "passed"},
        "limitations": ["APK build, signing, update safety and Android runtime were not checked by this tool.",
                        "Caller verification declarations are copied without independently validating them."]}
    write("BUILD-MANIFEST.json", (json.dumps(manifest, indent=2, sort_keys=True) + "\n").encode())
    readme = f"""melonDS UD {args.version}

Independent UD fork modifications, release {args.version}, dated {release_date}.
Original Android app by Rafael Caetano; emulator by the melonDS contributors.
See LICENSE and the complete source tree's contributor, copyright and license
notices, including those of bundled dependencies. Original notices are retained.

Install the standalone {apk_name} using Android's package installer. For an
existing installation, keep its application ID, signing identity and compatible
versionCode. Back up saves and settings first; do not uninstall or clear data
to work around an incompatible update. The official developer's signing key
and the persistent UD key are different identities.

Sources: extract {source_name}; its source/ directory contains the complete
committed app and recursively pinned dependencies, including their licenses.
Follow source/README.md for the declared Java/SDK/NDK/CMake and signing setup.
Build: sh ./gradlew :app:assembleGitHubNightlyRelease
Use your own signing key or the existing installation's authorized key.

Alternatively, start with app {app_base} and run:
  git apply --check --index {stem}-app-source.patch
  git apply --index {stem}-app-source.patch
  git submodule sync --recursive
  git submodule update --init --recursive
The app patch includes .gitmodules and gitlinks. Applying with --index updates
the index pins that submodule update reads; sync refreshes the configured URLs.
Do not apply the core patch again to that pinned core. The separate core patch
starts at {core_base}.
Stop if a patch check fails; do not force it onto another revision.

BUILD-MANIFEST.json records exact source identities and caller-supplied build
and test declarations. Packaging checks do not establish APK signing, build
reproducibility, Android runtime acceptance or preservation of personal data.
SHA256SUMS covers every payload file except the checksum file itself.
"""
    write("README.txt", readme.encode())
    checksums = "".join(f"{sha256}  {name}\n" for name, (sha256, _) in expected.items())
    write("SHA256SUMS", checksums.encode())
    distribution = output / ("melonDS-UD-nightly-" + args.version + ".zip")
    with zipfile.ZipFile(distribution, "x") as archive:
        for name in expected:
            data = (output / name).read_bytes()
            if digest(data) != expected[name][0]:
                raise ValueError("Payload changed during packaging: " + name)
            archive.writestr(zip_info(name), data)
    check_zip(distribution, expected)
    bundle_hash = digest(distribution.read_bytes())
    with (output / "BUNDLE-SHA256SUMS").open("x") as handle:
        handle.write(bundle_hash + "  " + distribution.name + "\n")
    return distribution, bundle_hash


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--revision", required=True, help="exact committed app SHA")
    parser.add_argument("--app-base", default=APP_BASE)
    parser.add_argument("--core-base", default=CORE_BASE)
    parser.add_argument("--apk", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--release-date", default=datetime.now(timezone.utc).date().isoformat(),
                        help="ISO release date; defaults to the current UTC date")
    parser.add_argument("--verification-report", type=Path,
                        help="JSON object copied as caller declarations, not automatic acceptance")
    try:
        path, sha256 = package(parser.parse_args())
    except (ValueError, OSError, UnicodeError, zipfile.BadZipFile) as error:
        parser.exit(1, "release_bundle: " + str(error) + "\n")
    print(str(path) + "\nSHA256 " + sha256)


if __name__ == "__main__":
    main()
