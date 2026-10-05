#!/usr/bin/env python3
import argparse
import json
import sys
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path

SKIPPED_DIRS = {"node_modules", ".gradle", "dist"}


@dataclass(frozen=True)
class Counts:
    tests: int = 0
    failures: int = 0
    errors: int = 0
    skipped: int = 0

    def __add__(self, other: "Counts") -> "Counts":
        return Counts(
            self.tests + other.tests,
            self.failures + other.failures,
            self.errors + other.errors,
            self.skipped + other.skipped,
        )

    @property
    def failed(self) -> int:
        return self.failures + self.errors

    @property
    def passed(self) -> int:
        return self.tests - self.failed - self.skipped


def counts_of(suite: ElementTree.Element) -> Counts:
    def number(name: str) -> int:
        return int(suite.get(name) or 0)

    return Counts(number("tests"), number("failures"), number("errors"), number("skipped"))


def report_files(root: Path):
    for path in root.rglob("test-results/**/*.xml"):
        if SKIPPED_DIRS.isdisjoint(path.parts):
            yield path


def read(path: Path) -> Counts:
    tree = ElementTree.parse(path).getroot()
    suites = [tree] if tree.tag == "testsuite" else list(tree.iter("testsuite"))
    return sum((counts_of(suite) for suite in suites), Counts())


def badge(label: str, message: str, color: str) -> dict:
    return {"schemaVersion": 1, "label": label, "message": message, "color": color}


def tests_badge(counts: Counts) -> dict:
    parts = [f"{counts.passed} passed"]
    if counts.failed:
        parts.append(f"{counts.failed} failed")
    if counts.skipped:
        parts.append(f"{counts.skipped} skipped")
    color = "red" if counts.failed else "yellow" if counts.skipped else "brightgreen"
    return badge("tests", ", ".join(parts), color)


def version_badge(version: str) -> dict:
    color = "orange" if version.endswith("-SNAPSHOT") else "blue"
    return badge("version", version, color)


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", type=Path, default=Path("."))
    parser.add_argument("--out", type=Path, default=Path("badges"))
    parser.add_argument("--version", default="")
    args = parser.parse_args()

    files = sorted(report_files(args.root))
    total = sum((read(path) for path in files), Counts())
    args.out.mkdir(parents=True, exist_ok=True)
    (args.out / "tests.json").write_text(json.dumps(tests_badge(total)) + "\n")
    if args.version:
        (args.out / "version.json").write_text(json.dumps(version_badge(args.version)) + "\n")
    print(f"{len(files)} report files: {total.passed} passed, {total.failed} failed, {total.skipped} skipped")
    return 1 if not files else 0


if __name__ == "__main__":
    sys.exit(main())
