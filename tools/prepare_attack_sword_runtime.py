#!/usr/bin/env python3
"""Build the game-runtime skeleton from the approved Spine 3.8 frame animation."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


SCRIPT_DIR = Path(__file__).resolve().parent
sys.path.insert(0, str(SCRIPT_DIR / "spine-compat-probe"))

from convert_spine_38_to_34 import convert  # noqa: E402


def retime_attack_sword(path: Path, fps: float) -> None:
    with path.open("r", encoding="utf-8") as handle:
        data = json.load(handle)

    animation = data["animations"]["attack_sword"]
    frames = animation["slots"]["character_frame"]["attachment"]
    for index, frame in enumerate(frames, start=1):
        frame["time"] = round(index / fps, 6)

    for event in animation.get("events", []):
        if event.get("name") == "attack_hit":
            event["time"] = round(10 / fps, 6)  # frame 11
        elif event.get("name") == "attack_end":
            event["time"] = round(16 / fps, 6)

    with path.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(data, handle, ensure_ascii=False, separators=(",", ":"))
        handle.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    parser.add_argument("--fps", type=float, default=30.0)
    args = parser.parse_args()

    if args.fps <= 0:
        raise ValueError("fps must be positive")
    convert(args.source, args.destination)
    retime_attack_sword(args.destination, args.fps)
    print(f"Prepared {args.destination} at {args.fps:g} FPS")


if __name__ == "__main__":
    main()
