#!/usr/bin/env python3
"""Publish compact Maven/Vitest test and coverage totals to GitHub Actions."""

from __future__ import annotations

import glob
import os
import xml.etree.ElementTree as ET
from pathlib import Path


def junit_totals(paths: list[str]) -> tuple[int, int, int, int]:
    tests = failures = errors = skipped = 0
    for path in paths:
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError):
            continue
        suites = [root] if root.tag == "testsuite" else root.findall(".//testsuite")
        for suite in suites:
            tests += int(suite.attrib.get("tests", 0))
            failures += int(suite.attrib.get("failures", 0))
            errors += int(suite.attrib.get("errors", 0))
            skipped += int(suite.attrib.get("skipped", 0))
    return tests, failures, errors, skipped


def jacoco_line_totals(paths: list[str]) -> tuple[int, int]:
    covered = missed = 0
    for path in paths:
        try:
            root = ET.parse(path).getroot()
        except (ET.ParseError, OSError):
            continue
        for counter in root.findall("./counter"):
            if counter.attrib.get("type") == "LINE":
                missed += int(counter.attrib.get("missed", 0))
                covered += int(counter.attrib.get("covered", 0))
    return covered, missed


def lcov_line_totals(paths: list[str]) -> tuple[int, int]:
    found = hit = 0
    for path in paths:
        try:
            lines = Path(path).read_text(encoding="utf-8").splitlines()
        except OSError:
            continue
        for line in lines:
            if line.startswith("LF:"):
                found += int(line[3:])
            elif line.startswith("LH:"):
                hit += int(line[3:])
    return hit, found - hit


def percentage(covered: int, missed: int) -> str:
    total = covered + missed
    return "n/a" if total == 0 else f"{covered / total * 100:.2f}%"


def main() -> None:
    backend_tests = junit_totals(glob.glob("**/target/surefire-reports/TEST-*.xml", recursive=True))
    frontend_tests = junit_totals(["the_bot_web/frontend/coverage/junit.xml"])
    java_covered, java_missed = jacoco_line_totals(
        glob.glob("**/target/site/jacoco/jacoco.xml", recursive=True)
    )
    frontend_covered, frontend_missed = lcov_line_totals(
        ["the_bot_web/frontend/coverage/lcov.info"]
    )

    lines = [
        "## Unit Tests and Coverage",
        "",
        "| Suite | Tests | Failures | Errors | Skipped |",
        "| --- | ---: | ---: | ---: | ---: |",
        f"| Backend (JUnit) | {backend_tests[0]} | {backend_tests[1]} | {backend_tests[2]} | {backend_tests[3]} |",
        f"| Frontend (Vitest) | {frontend_tests[0]} | {frontend_tests[1]} | {frontend_tests[2]} | {frontend_tests[3]} |",
        "",
        "| Coverage | Covered lines | Missed lines | Line coverage |",
        "| --- | ---: | ---: | ---: |",
        f"| Backend (JaCoCo) | {java_covered} | {java_missed} | {percentage(java_covered, java_missed)} |",
        f"| Frontend (LCOV) | {frontend_covered} | {frontend_missed} | {percentage(frontend_covered, frontend_missed)} |",
        "",
        "Detailed XML, HTML, JaCoCo, LCOV, and Vitest reports are available in the workflow artifacts.",
    ]
    summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary_path:
        Path(summary_path).write_text("\n".join(lines) + "\n", encoding="utf-8")
    else:
        print("\n".join(lines))


if __name__ == "__main__":
    main()
