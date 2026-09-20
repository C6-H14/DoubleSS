#!/usr/bin/env python3
"""Convert the deliberately limited Spine 3.8 JSON subset used by DoubleSS to 3.4.

This is intentionally strict. Unsupported 3.8 features abort conversion instead of
being silently dropped and producing a subtly broken in-game skeleton.
"""

from __future__ import annotations

import argparse
import copy
import json
from pathlib import Path
from typing import Any, Dict


SUPPORTED_ATTACHMENT_TYPES = {"region", "mesh", "boundingbox", "path", "linkedmesh"}
UNSUPPORTED_TOP_LEVEL = {"transform", "path", "physics"}


def convert_skins(data: Dict[str, Any]) -> None:
    skins = data.get("skins")
    if not isinstance(skins, list):
        raise ValueError("Expected Spine 3.8 skins to be an array")

    converted: Dict[str, Any] = {}
    for skin in skins:
        if not isinstance(skin, dict) or not skin.get("name"):
            raise ValueError("Every skin must be an object with a non-empty name")
        extra = set(skin) - {"name", "attachments"}
        if extra:
            raise ValueError(f"Skin {skin['name']!r} uses unsupported fields: {sorted(extra)}")

        attachments = copy.deepcopy(skin.get("attachments", {}))
        for slot_name, slot_attachments in attachments.items():
            if not isinstance(slot_attachments, dict):
                raise ValueError(f"Skin slot {slot_name!r} attachments must be an object")
            for attachment_name, attachment in slot_attachments.items():
                if not isinstance(attachment, dict):
                    raise ValueError(
                        f"Attachment {slot_name}/{attachment_name} must be an object"
                    )
                attachment_type = attachment.get("type", "region")
                if attachment_type not in SUPPORTED_ATTACHMENT_TYPES:
                    raise ValueError(
                        f"Unsupported attachment type {attachment_type!r} at "
                        f"{slot_name}/{attachment_name}"
                    )
        converted[skin["name"]] = attachments

    data["skins"] = converted


def convert_animations(data: Dict[str, Any]) -> None:
    animations = data.get("animations", {})
    if not isinstance(animations, dict):
        raise ValueError("animations must be an object")
    for animation_name, animation in animations.items():
        if not isinstance(animation, dict):
            raise ValueError(f"Animation {animation_name!r} must be an object")
        if "drawOrder" in animation:
            if "draworder" in animation:
                raise ValueError(f"Animation {animation_name!r} has both drawOrder and draworder")
            animation["draworder"] = animation.pop("drawOrder")

        frame_lists = []

        for slot_timelines in animation.get("slots", {}).values():
            frame_lists.extend(slot_timelines.values())
        for bone_timelines in animation.get("bones", {}).values():
            frame_lists.extend(bone_timelines.values())
        for constraint_group in ("ik", "transform", "paths"):
            for constraint_timelines in animation.get(constraint_group, {}).values():
                if isinstance(constraint_timelines, list):
                    frame_lists.append(constraint_timelines)
                elif isinstance(constraint_timelines, dict):
                    frame_lists.extend(constraint_timelines.values())
        for skin_timelines in animation.get("deform", {}).values():
            for slot_timelines in skin_timelines.values():
                frame_lists.extend(slot_timelines.values())
        for simple_timeline in ("draworder", "events"):
            if simple_timeline in animation:
                frame_lists.append(animation[simple_timeline])

        for frames in frame_lists:
            if not isinstance(frames, list):
                raise ValueError(
                    f"Animation {animation_name!r} contains a non-array timeline"
                )
            for frame in frames:
                if not isinstance(frame, dict):
                    raise ValueError(
                        f"Animation {animation_name!r} contains a non-object keyframe"
                    )
                frame.setdefault("time", 0)
                curve = frame.get("curve")
                if isinstance(curve, (int, float)):
                    frame["curve"] = [
                        curve,
                        frame.pop("c2", 0),
                        frame.pop("c3", 1),
                        frame.pop("c4", 1),
                    ]


def convert(source: Path, destination: Path) -> None:
    with source.open("r", encoding="utf-8") as handle:
        data = json.load(handle)

    if not isinstance(data, dict):
        raise ValueError("Skeleton JSON root must be an object")
    skeleton = data.get("skeleton")
    if not isinstance(skeleton, dict):
        raise ValueError("Missing skeleton metadata object")
    version = str(skeleton.get("spine", ""))
    if not version.startswith("3.8"):
        raise ValueError(f"Expected a Spine 3.8 export, got {version!r}")

    used_unsupported = UNSUPPORTED_TOP_LEVEL.intersection(data)
    if used_unsupported:
        raise ValueError(f"Unsupported top-level constraints: {sorted(used_unsupported)}")

    skeleton["spine"] = "3.4.02"
    skeleton.pop("audio", None)
    convert_skins(data)
    convert_animations(data)

    destination.parent.mkdir(parents=True, exist_ok=True)
    with destination.open("w", encoding="utf-8", newline="\n") as handle:
        json.dump(data, handle, ensure_ascii=False, separators=(",", ":"))
        handle.write("\n")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("source", type=Path)
    parser.add_argument("destination", type=Path)
    args = parser.parse_args()
    convert(args.source, args.destination)
    print(f"Converted {args.source} -> {args.destination}")


if __name__ == "__main__":
    main()
