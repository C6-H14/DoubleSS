from pathlib import Path
import csv
import json
import os

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = Path(os.environ.get(
    "IDLE_AUDIT_SOURCE",
    ROOT / "art_candidates/video_source/idle/short_blink_final_v43_saturation_stable/alpha_frames",
))
OUT = Path(os.environ.get("IDLE_AUDIT_OUT", SOURCE.parent / "full_color_audit"))
COUNT = 180


def circular_smooth(values, radius=3):
    values = np.asarray(values, np.float32)
    padded = np.r_[values[-radius:], values, values[:radius]]
    return np.asarray([np.median(padded[i:i + radius * 2 + 1]) for i in range(len(values))])


def robust_mean(values):
    if values.size == 0:
        return float("nan")
    lo, hi = np.percentile(values, (10, 90))
    kept = values[(values >= lo) & (values <= hi)]
    return float(kept.mean()) if kept.size else float(values.mean())


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    frames = []
    rows = []
    for index in range(1, COUNT + 1):
        rgba = np.asarray(Image.open(SOURCE / f"idle_{index:03d}.png").convert("RGBA"), np.uint8)
        frames.append(rgba)
        rgb = rgba[:, :, :3]
        alpha = rgba[:, :, 3]
        hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
        lab = cv2.cvtColor(rgb, cv2.COLOR_RGB2LAB)
        h, s, v = [hsv[:, :, n] for n in range(3)]
        solid = alpha >= 220
        classes = {
            "all": solid & (v >= 25) & (v <= 248),
            "teal": solid & (h >= 70) & (h <= 105) & (s >= 45) & (v >= 35) & (v <= 175),
            "hair": solid & (h >= 8) & (h <= 32) & (s >= 28) & (s <= 155) & (v >= 125),
            "skin": solid & ((h <= 14) | (h >= 175)) & (s >= 12) & (s <= 105) & (v >= 145),
            "white": solid & (s <= 42) & (v >= 145),
            "gold": solid & (h >= 10) & (h <= 35) & (s >= 70) & (v >= 95),
            "brown": solid & ((h <= 25) | (h >= 175)) & (s >= 50) & (v >= 35) & (v <= 170),
        }
        row = {"frame": index, "opaque_pixels": int(solid.sum())}
        for name, mask in classes.items():
            row[f"{name}_count"] = int(mask.sum())
            row[f"{name}_sat"] = robust_mean(s[mask].astype(np.float32))
            row[f"{name}_val"] = robust_mean(v[mask].astype(np.float32))
            row[f"{name}_lab_l"] = robust_mean(lab[:, :, 0][mask].astype(np.float32))
            row[f"{name}_r"] = robust_mean(rgb[:, :, 0][mask].astype(np.float32))
            row[f"{name}_g"] = robust_mean(rgb[:, :, 1][mask].astype(np.float32))
            row[f"{name}_b"] = robust_mean(rgb[:, :, 2][mask].astype(np.float32))
        rows.append(row)

    metric_names = [key for key in rows[0] if key != "frame"]
    findings = []
    scores = np.zeros(COUNT, np.float32)
    for metric in metric_names:
        if metric.endswith("_count") or metric == "opaque_pixels":
            continue
        values = np.asarray([row[metric] for row in rows], np.float32)
        target = circular_smooth(values, 3)
        residual = values - target
        scale = max(float(np.median(np.abs(residual - np.median(residual))) * 1.4826), 0.15)
        z = np.abs(residual) / scale
        jumps = np.abs(np.r_[np.diff(values), values[0] - values[-1]])
        for i in range(COUNT):
            rows[i][f"{metric}_residual"] = float(residual[i])
            rows[i][f"{metric}_z"] = float(z[i])
            scores[i] = max(scores[i], min(float(z[i]), 20.0))
        worst = np.argsort(z)[-8:][::-1]
        findings.append({
            "metric": metric,
            "max_abs_residual": round(float(np.nanmax(np.abs(residual))), 4),
            "max_adjacent_jump": round(float(np.nanmax(jumps)), 4),
            "loop_jump_180_to_1": round(float(values[0] - values[-1]), 4),
            "worst_frames": [int(i + 1) for i in worst],
        })

    ranked = np.argsort(scores)[::-1]
    suspects = [int(i + 1) for i in ranked[:30]]
    # Rank transitions using all semantic color channels together.  Each
    # channel is normalized by its ordinary adjacent-frame variation so a
    # coherent whole-character color jump outranks pose-driven pixel counts.
    color_metrics = [name for name in metric_names if not name.endswith("_count")]
    transition_components = []
    for metric in color_metrics:
        values = np.asarray([row[metric] for row in rows], np.float32)
        diffs = np.r_[np.diff(values), values[0] - values[-1]]
        scale = max(float(np.median(np.abs(diffs - np.median(diffs))) * 1.4826), 0.25)
        transition_components.append(np.clip(np.abs(diffs) / scale, 0, 20))
    transition_matrix = np.asarray(transition_components)
    transition_scores = np.sqrt(np.mean(transition_matrix ** 2, axis=0))
    transition_rank = np.argsort(transition_scores)[::-1]
    transition_rows = []
    for i in transition_rank[:30]:
        strongest = np.argsort(transition_matrix[:, i])[-5:][::-1]
        transition_rows.append({
            "from": int(i + 1),
            "to": int((i + 1) % COUNT + 1),
            "score": round(float(transition_scores[i]), 3),
            "strongest_metrics": [
                {"metric": color_metrics[j], "normalized_jump": round(float(transition_matrix[j, i]), 3)}
                for j in strongest
            ],
        })
    with (OUT / "frame_metrics.csv").open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.DictWriter(stream, fieldnames=list(rows[0].keys()))
        writer.writeheader()
        writer.writerows(rows)
    summary = {
        "frames": COUNT,
        "top_suspect_frames": suspects,
        "top_scores": [{"frame": int(i + 1), "score": round(float(scores[i]), 3)} for i in ranked[:30]],
        "top_color_transitions": transition_rows,
        "metrics": sorted(findings, key=lambda item: item["max_abs_residual"], reverse=True),
    }
    (OUT / "summary.json").write_text(json.dumps(summary, indent=2) + "\n", encoding="utf-8")

    picks = sorted(set(suspects[:20] + [1, 2, 179, 180]))
    thumbs = []
    for frame_no in picks:
        image = Image.alpha_composite(Image.new("RGBA", (1112, 834), (45, 45, 45, 255)), Image.fromarray(frames[frame_no - 1], "RGBA"))
        image.thumbnail((278, 209), Image.Resampling.LANCZOS)
        panel = Image.new("RGB", (image.width, image.height + 22), (25, 25, 25))
        panel.paste(image.convert("RGB"), (0, 22))
        ImageDraw.Draw(panel).text((5, 4), f"F{frame_no:03d} score {scores[frame_no - 1]:.2f}", fill="white")
        thumbs.append(panel)
    cols = 5
    rows_n = (len(thumbs) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumbs[0].width, rows_n * thumbs[0].height), (20, 20, 20))
    for i, panel in enumerate(thumbs):
        sheet.paste(panel, ((i % cols) * panel.width, (i // cols) * panel.height))
    sheet.save(OUT / "suspect_frames_contact.png", optimize=True)
    print(json.dumps({"top_suspects": suspects, "output": str(OUT)}, indent=2))


if __name__ == "__main__":
    main()
