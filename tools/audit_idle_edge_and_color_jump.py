from pathlib import Path
import csv
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
FRAMES = ROOT / "art_candidates" / "video_source" / "idle" / "short_blink_final_v7_outer_ring_removed" / "alpha_frames"
RAW = ROOT / "art_candidates" / "video_source" / "idle" / "loop_v2_short_blink_180f" / "raw_frames"
SPINE = ROOT / "src" / "main" / "resources" / "img" / "char" / "spine"
OUT = ROOT / "art_candidates" / "video_source" / "idle" / "diagnostics_edge_color_v1"


def parse_atlas_region(atlas_text, target):
    lines = atlas_text.splitlines()
    page = None
    i = 0
    while i < len(lines):
        line = lines[i]
        if line.startswith("character") and line.endswith(".png"):
            page = line.strip()
        if line.strip() == target:
            fields = {}
            j = i + 1
            while j < len(lines) and (lines[j].startswith("  ") or not lines[j].strip()):
                if ":" in lines[j]:
                    k, v = lines[j].strip().split(":", 1)
                    fields[k] = v.strip()
                j += 1
            xy = tuple(int(x.strip()) for x in fields["xy"].split(","))
            size = tuple(int(x.strip()) for x in fields["size"].split(","))
            return page, xy, size
        i += 1
    raise KeyError(target)


def frame_metrics(path):
    rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8)
    rgb = rgba[:, :, :3]
    alpha = rgba[:, :, 3]
    solid = alpha >= 245
    inward = cv2.distanceTransform(solid.astype(np.uint8), cv2.DIST_L2, 5)
    interior = inward >= 6.0
    pix = rgb[interior]
    lab = cv2.cvtColor(pix.reshape(-1, 1, 3), cv2.COLOR_RGB2LAB).reshape(-1, 3).astype(np.float32)
    hsv = cv2.cvtColor(pix.reshape(-1, 1, 3), cv2.COLOR_RGB2HSV).reshape(-1, 3).astype(np.float32)
    # OpenCV Lab neutral point is 128/128 for a/b.
    chroma = np.sqrt((lab[:, 1] - 128.0) ** 2 + (lab[:, 2] - 128.0) ** 2)
    return {
        "l_mean": float(lab[:, 0].mean()),
        "l_std": float(lab[:, 0].std()),
        "sat_mean": float(hsv[:, 1].mean()),
        "chroma_mean": float(chroma.mean()),
        "rgb_mean_r": float(pix[:, 0].mean()),
        "rgb_mean_g": float(pix[:, 1].mean()),
        "rgb_mean_b": float(pix[:, 2].mean()),
        "interior_pixels": int(len(pix)),
    }


def edge_padding_evidence():
    atlas_path = SPINE / "character.atlas"
    atlas_text = atlas_path.read_text(encoding="utf-8")
    filters = [line.strip() for line in atlas_text.splitlines() if line.strip().startswith("filter:")]
    samples = []
    for target in ("idle_001", "idle_061", "idle_106", "idle_150", "idle_180"):
        page_name, (x, y), (w, h) = parse_atlas_region(atlas_text, target)
        page = np.asarray(Image.open(SPINE / page_name).convert("RGBA"), dtype=np.uint8)
        crop = page[y:y+h, x:x+w]
        a = crop[:, :, 3]
        fg = a > 0
        dilated = cv2.dilate(fg.astype(np.uint8), np.ones((3, 3), np.uint8)) > 0
        outside_neighbor = dilated & ~fg
        # The packed region is tightly cropped, so inspect its one-pixel page
        # perimeter as well when it exists.
        y1, y2 = max(0, y-1), min(page.shape[0], y+h+1)
        x1, x2 = max(0, x-1), min(page.shape[1], x+w+1)
        around = page[y1:y2, x1:x2]
        transparent = around[:, :, 3] == 0
        transparent_black = transparent & np.all(around[:, :, :3] == 0, axis=2)
        samples.append({
            "region": target, "page": page_name, "xy": [x, y], "size": [w, h],
            "transparent_pixels_around_region": int(np.count_nonzero(transparent)),
            "transparent_black_pixels_around_region": int(np.count_nonzero(transparent_black)),
            "transparent_black_ratio": float(np.count_nonzero(transparent_black) / max(1, np.count_nonzero(transparent))),
            "transparent_black_neighbors_inside_crop": int(np.count_nonzero(outside_neighbor & np.all(crop[:, :, :3] == 0, axis=2))),
        })
    return {"atlas_filters": sorted(set(filters)), "samples": samples}


def make_sampling_demo():
    # Exact straight-alpha bilinear sampling example: the same opaque hair edge
    # sampled against transparent black versus transparent pixels whose RGB is
    # extruded from the hair while alpha remains zero.
    bg = np.array([38.0, 27.0, 27.0])
    hair = np.array([224.0, 176.0, 102.0])
    rows = []
    for t in np.linspace(0, 1, 17):
        alpha = 1.0 - t
        rgb_black_padding = hair * (1.0 - t) + np.array([0.0, 0.0, 0.0]) * t
        rgb_extruded = hair
        out_black = rgb_black_padding * alpha + bg * (1.0 - alpha)
        out_extruded = rgb_extruded * alpha + bg * (1.0 - alpha)
        rows.append((out_black, out_extruded))
    scale = 22
    image = Image.new("RGB", (17 * scale, 2 * scale + 34), (30, 30, 30))
    draw = ImageDraw.Draw(image)
    draw.text((3, 3), "top: transparent black padding; bottom: RGB-extruded transparent padding", fill="white")
    for i, (a, b) in enumerate(rows):
        image.paste(tuple(np.clip(a, 0, 255).astype(np.uint8)), (i*scale, 34, (i+1)*scale, 34+scale))
        image.paste(tuple(np.clip(b, 0, 255).astype(np.uint8)), (i*scale, 34+scale, (i+1)*scale, 34+2*scale))
    image.save(OUT / "straight_alpha_linear_filter_evidence.png", optimize=True)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    paths = sorted(FRAMES.glob("idle_*.png"))
    if len(paths) != 180:
        raise RuntimeError(len(paths))
    metrics = []
    raw_paths = sorted(RAW.glob("idle_*.png"))
    interior_mae = []
    interior_exact_ratio = []
    for index, path in enumerate(paths, 1):
        row = {"frame": index, "time": (index-1)/60.0}
        row.update(frame_metrics(path))
        metrics.append(row)
        final_rgba = np.asarray(Image.open(path).convert("RGBA"), dtype=np.uint8)
        raw_rgb = np.asarray(Image.open(raw_paths[index-1]).convert("RGB"), dtype=np.uint8)
        inward = cv2.distanceTransform((final_rgba[:, :, 3] >= 245).astype(np.uint8), cv2.DIST_L2, 5)
        deep = inward >= 6.0
        difference = np.abs(final_rgba[:, :, :3].astype(np.int16) - raw_rgb.astype(np.int16))[deep]
        interior_mae.append(float(difference.mean()))
        interior_exact_ratio.append(float(np.all(difference == 0, axis=1).mean()))
    fields = list(metrics[0].keys())
    with (OUT / "interior_color_metrics.csv").open("w", newline="", encoding="utf-8-sig") as stream:
        writer = csv.DictWriter(stream, fieldnames=fields)
        writer.writeheader()
        writer.writerows(metrics)

    feature_names = ["l_mean", "l_std", "sat_mean", "chroma_mean"]
    jumps = {}
    for feature in feature_names:
        values = np.array([m[feature] for m in metrics])
        delta = np.diff(values)
        order = np.argsort(np.abs(delta))[::-1][:12]
        jumps[feature] = [{"from": int(i+1), "to": int(i+2),
                           "delta": float(delta[i]), "before": float(values[i]),
                           "after": float(values[i+1])} for i in order]

    # Generate a dependency-free strip chart.
    w, h = 1440, 760
    chart = Image.new("RGB", (w, h), (28, 28, 30))
    draw = ImageDraw.Draw(chart)
    colors = [(240,190,80), (120,210,255), (255,110,150), (150,235,150)]
    panel_h = 170
    for panel, (feature, color) in enumerate(zip(feature_names, colors)):
        vals = np.array([m[feature] for m in metrics])
        lo, hi = float(vals.min()), float(vals.max())
        top = 30 + panel * panel_h
        draw.text((10, top-20), f"{feature}: min={lo:.2f} max={hi:.2f}", fill="white")
        pts = []
        for i, value in enumerate(vals):
            x = 50 + i * (w-80) / 179
            y = top + panel_h-35 - (value-lo) / max(1e-6, hi-lo) * (panel_h-55)
            pts.append((x, y))
        draw.line(pts, fill=color, width=2)
        for frame in (92, 120):
            x = 50 + (frame-1) * (w-80) / 179
            draw.line((x, top, x, top+panel_h-35), fill=(120,120,120), width=1)
    chart.save(OUT / "interior_color_metrics_chart.png", optimize=True)

    evidence = edge_padding_evidence()
    make_sampling_demo()
    blink_slice = slice(91, 120)
    blink_window = {}
    for feature in feature_names:
        values = np.array([m[feature] for m in metrics])
        window = values[blink_slice]
        blink_window[feature] = {
            "frame_91_before_blink": float(values[90]),
            "blink_min": float(window.min()),
            "blink_max": float(window.max()),
            "frame_120_end": float(values[119]),
            "blink_peak_frame": int(92 + np.argmax(window)),
        }
    report = {
        "edge_runtime_evidence": evidence,
        "deep_interior_source_preservation": {
            "rgb_mae_mean": float(np.mean(interior_mae)),
            "rgb_mae_max_frame": float(np.max(interior_mae)),
            "exact_rgb_match_ratio_mean": float(np.mean(interior_exact_ratio)),
            "exact_rgb_match_ratio_min_frame": float(np.min(interior_exact_ratio)),
        },
        "blink_window_color_range": blink_window,
        "largest_adjacent_frame_jumps": jumps,
        "timeline_boundaries": {"blink_start": 92, "blink_end_recovery_start": 120},
        "notes": {
            "edge": "PNG/atlas transparent texels are black while atlas filtering is Linear,Linear.",
            "color": "Metrics use only pixels at least 6 px inside the opaque subject; edge processing cannot affect them.",
        },
    }
    (OUT / "diagnostic_report.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
