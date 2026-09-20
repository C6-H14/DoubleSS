from pathlib import Path
import json

import cv2
import numpy as np
from PIL import Image, ImageDraw, ImageFilter


ROOT = Path(r"D:\StSmod\DoubleSS")
WORK = ROOT / "art_candidates" / "video_source" / "cast_attack_2_final_cut"
SOURCE = WORK / "alpha_frames_115s_30fps"
OUTPUT = WORK / "alpha_frames_115s_30fps_sword_completed_v2"
QA = WORK / "sword_completion_qa_v2"
FRAME20_RERENDER = WORK / "cast_attack_2_020_rerender_candidate.png"
REFERENCE_FRAME = 15
TARGET_FRAMES = (16, 17, 18)
TOP_PADDING = 400
LEFT_PADDING = 400
RIGHT_PADDING = 400


def sword_component(image: Image.Image) -> tuple[np.ndarray, np.ndarray, np.ndarray, float]:
    rgba = np.asarray(image.convert("RGBA"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgba[..., :3], cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    mask = (
        (rgba[..., 3] > 32)
        & (h > 125)
        & (h < 160)
        & (s > 70)
        & (v > 150)
    ).astype(np.uint8)
    count, labels, stats, _ = cv2.connectedComponentsWithStats(mask, 8)
    if count <= 1:
        raise RuntimeError("Sword glow component not found")
    component = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
    ys, xs = np.where(labels == component)
    points = np.column_stack((xs, ys)).astype(np.float64)
    center = points.mean(axis=0)
    centered = points - center
    values, vectors = np.linalg.eigh(centered.T @ centered)
    axis = vectors[:, int(np.argmax(values))]
    projections = centered @ axis
    endpoint_a = center + axis * projections.min()
    endpoint_b = center + axis * projections.max()
    # In the affected overhead poses, the guard is the endpoint nearest the
    # character (normally the lower endpoint); the other endpoint is the tip.
    if endpoint_a[1] >= endpoint_b[1]:
        guard, visible_tip = endpoint_a, endpoint_b
    else:
        guard, visible_tip = endpoint_b, endpoint_a
    length = float(np.linalg.norm(visible_tip - guard))
    return guard, visible_tip, mask, length


def add_missing_blade(canvas: Image.Image, guard: np.ndarray, visible_tip: np.ndarray, standard_length: float) -> tuple[Image.Image, np.ndarray]:
    direction = visible_tip - guard
    direction /= np.linalg.norm(direction)
    expected_tip = guard + direction * standard_length

    shifted_guard = guard + np.array([0.0, TOP_PADDING])
    shifted_tip = expected_tip + np.array([0.0, TOP_PADDING])
    # Extend directly into the measured visible blade by 12 px. Using the old
    # canvas-edge intersection fails on nearly horizontal poses because the
    # fitted centerline may meet y=0 beyond the last surviving sword pixels.
    shifted_visible_tip = visible_tip + np.array([0.0, TOP_PADDING])
    seam_end = shifted_visible_tip - direction * 12.0

    p0 = tuple(np.round(shifted_tip).astype(int))
    p1 = tuple(np.round(seam_end).astype(int))
    glow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow)
    glow_draw.line((p0, p1), fill=(190, 52, 255, 215), width=38)
    glow = glow.filter(ImageFilter.GaussianBlur(10))

    blade = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(blade)
    draw.line((p0, p1), fill=(151, 45, 243, 255), width=23)
    draw.line((p0, p1), fill=(226, 195, 255, 255), width=13)
    draw.line((p0, p1), fill=(255, 255, 255, 245), width=5)

    # Taper the final 30 px into one sharp point instead of leaving a round cap.
    normal = np.array([-direction[1], direction[0]])
    shoulder = shifted_tip - direction * 30.0
    polygon = [
        tuple(np.round(shifted_tip).astype(int)),
        tuple(np.round(shoulder + normal * 11.5).astype(int)),
        tuple(np.round(shoulder - normal * 11.5).astype(int)),
    ]
    draw.polygon(polygon, fill=(199, 105, 255, 255))
    draw.line((tuple(np.round(shifted_tip).astype(int)), tuple(np.round(shoulder).astype(int))), fill=(255, 255, 255, 255), width=4)

    result = Image.alpha_composite(canvas, glow)
    result = Image.alpha_composite(result, blade)
    return result, expected_tip


def composite(image: Image.Image, color: str) -> Image.Image:
    return Image.alpha_composite(Image.new("RGBA", image.size, color), image).convert("RGB")


def blade_texture(reference: Image.Image, guard: np.ndarray, tip: np.ndarray, half_width: float) -> Image.Image:
    rgba = np.asarray(reference.convert("RGBA"), dtype=np.uint8)
    rgb = rgba[..., :3]
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    direction = tip - guard
    length = np.linalg.norm(direction)
    direction /= length
    normal = np.array([-direction[1], direction[0]])
    yy, xx = np.indices(rgba.shape[:2], dtype=np.float64)
    dx, dy = xx - guard[0], yy - guard[1]
    along = dx * direction[0] + dy * direction[1]
    across = dx * normal[0] + dy * normal[1]
    corridor = (along >= 5) & (along <= length + 8) & (np.abs(across) <= half_width)
    purple = (h >= 120) & (h <= 170) & (s >= 35) & (v >= 65)
    metallic = (s < 100) & (v > 105) & (rgb[..., 2].astype(int) >= rgb[..., 0].astype(int) - 20)
    keep = corridor & (rgba[..., 3] > 0) & (purple | metallic)
    out = rgba.copy()
    out[..., 3] = np.where(keep, rgba[..., 3], 0)
    out[out[..., 3] == 0, :3] = 0
    return Image.fromarray(out, "RGBA")


def warp_texture(
    texture: Image.Image,
    ref_guard: np.ndarray,
    ref_tip: np.ndarray,
    target_guard: np.ndarray,
    target_tip: np.ndarray,
    canvas_size: tuple[int, int],
) -> Image.Image:
    source = np.asarray(texture, dtype=np.float32) / 255.0
    source[..., :3] *= source[..., 3:4]
    ref_axis = ref_tip - ref_guard
    target_axis = target_tip - target_guard
    ref_length = np.linalg.norm(ref_axis)
    target_length = np.linalg.norm(target_axis)
    ur = ref_axis / ref_length
    nr = np.array([-ur[1], ur[0]])
    ut = target_axis / target_length
    nt = np.array([-ut[1], ut[0]])
    basis_ref = np.column_stack((ur, nr))
    basis_target = np.column_stack((ut * (target_length / ref_length), nt))
    matrix2 = basis_target @ basis_ref.T
    shifted_target_guard = target_guard + np.array([LEFT_PADDING, TOP_PADDING])
    translation = shifted_target_guard - matrix2 @ ref_guard
    affine = np.column_stack((matrix2, translation)).astype(np.float32)
    warped = cv2.warpAffine(source, affine, canvas_size, flags=cv2.INTER_CUBIC, borderMode=cv2.BORDER_CONSTANT)
    alpha = np.clip(warped[..., 3:4], 0, 1)
    rgb = np.divide(warped[..., :3], alpha, out=np.zeros_like(warped[..., :3]), where=alpha > 1e-6)
    result = np.uint8(np.round(np.concatenate((np.clip(rgb, 0, 1), alpha), axis=2) * 255))
    return Image.fromarray(result, "RGBA")


def draw_effect_curve(canvas: Image.Image, points: list[tuple[float, float]]) -> Image.Image:
    # Sample a quadratic Bezier with the same white-hot core and magenta edge
    # used by the source sword-wave effect.
    p0, p1, p2 = (np.array(p, dtype=np.float64) + np.array([LEFT_PADDING, TOP_PADDING]) for p in points)
    samples = []
    for t in np.linspace(0, 1, 180):
        p = (1 - t) ** 2 * p0 + 2 * (1 - t) * t * p1 + t**2 * p2
        samples.append(tuple(np.round(p).astype(int)))
    glow = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.line(samples, fill=(196, 54, 255, 210), width=68, joint="curve")
    glow = glow.filter(ImageFilter.GaussianBlur(15))
    core = Image.new("RGBA", canvas.size, (0, 0, 0, 0))
    cd = ImageDraw.Draw(core)
    cd.line(samples, fill=(214, 91, 255, 235), width=38, joint="curve")
    cd.line(samples, fill=(255, 244, 255, 250), width=17, joint="curve")
    return Image.alpha_composite(Image.alpha_composite(canvas, glow), core)


def main() -> None:
    OUTPUT.mkdir(parents=True, exist_ok=True)
    QA.mkdir(parents=True, exist_ok=True)
    for path in OUTPUT.glob("*.png"):
        path.unlink()

    reference = Image.open(SOURCE / f"cast_attack_2_{REFERENCE_FRAME:03d}.png").convert("RGBA")
    ref_guard, ref_tip, _, standard_length = sword_component(reference)
    texture_reference = Image.open(SOURCE / "cast_attack_2_019.png").convert("RGBA")
    texture_guard = np.array([416.3, 88.5])
    texture_tip = np.array([149.7, 316.3])
    enchanted_texture = blade_texture(texture_reference, texture_guard, texture_tip, 42.0)
    ordinary_reference = Image.open(SOURCE / "cast_attack_2_032.png").convert("RGBA")
    ordinary_guard = np.array([373.0, 421.0])
    ordinary_tip = np.array([74.0, 628.0])
    ordinary_texture = blade_texture(ordinary_reference, ordinary_guard, ordinary_tip, 24.0)
    measurements = {
        "reference_frame": REFERENCE_FRAME,
        "reference_guard": [round(float(x), 3) for x in ref_guard],
        "reference_tip": [round(float(x), 3) for x in ref_tip],
        "standard_guard_to_tip_length_px": round(standard_length, 3),
        "padding_px": {"top": TOP_PADDING, "left": LEFT_PADDING, "right": RIGHT_PADDING},
        "targets": {},
    }

    built: list[Image.Image] = []
    for source in sorted(SOURCE.glob("cast_attack_2_*.png")):
        frame_no = int(source.stem.rsplit("_", 1)[1])
        image = Image.open(source).convert("RGBA")
        if frame_no in (20, 29):
            # Remove clipped sword-wave remnants before rebuilding frame 20 or
            # deliberately omitting frame 29's right-side effect. Include both
            # magenta edges and the low-saturation white-hot core.
            rgba = np.asarray(image).copy()
            hsv = cv2.cvtColor(rgba[..., :3], cv2.COLOR_RGB2HSV)
            h, s, v = cv2.split(hsv)
            yy, xx = np.indices(rgba.shape[:2])
            colored_effect = (h > 120) & (h < 175) & (s > 35) & (v > 90)
            white_core = (s < 100) & (v > 155)
            if frame_no == 29:
                region = (xx > 800) & (yy < 180)
            else:
                # Clear the clipped top wave and its rectangular right-hand
                # remnant. The actual sword is reconstructed after compositing.
                region = (yy < 180) | ((xx > 760) & (yy < 300))
            effect = region & (rgba[..., 3] > 0) & (colored_effect | white_core)
            rgba[effect] = 0
            if frame_no == 20:
                # The broad top-edge cleanup also intersects the blonde hair and
                # the upper half of the diagonal sword. Restore non-effect hair,
                # then restore only a narrow sword corridor from the untouched
                # source; the rebuilt arc remains outside this corridor.
                original = np.asarray(Image.open(source).convert("RGBA"))
                keep_hair = (
                    (yy >= 75) & (yy < 190) & (xx >= 360) & (xx < 760)
                    & (original[..., 3] > 0)
                )
                rgba[keep_hair] = original[keep_hair]
            image = Image.fromarray(rgba, "RGBA")
        canvas_size = (image.width + LEFT_PADDING + RIGHT_PADDING, image.height + TOP_PADDING)
        canvas = Image.new("RGBA", canvas_size, (0, 0, 0, 0))
        post_overlay = None
        if frame_no in TARGET_FRAMES:
            guard, visible_tip, _, visible_length = sword_component(image)
            direction = visible_tip - guard
            direction /= np.linalg.norm(direction)
            expected_tip = guard + direction * standard_length
            overlay = warp_texture(enchanted_texture, texture_guard, texture_tip, guard, expected_tip, canvas_size)
            if frame_no == 16:
                # Composite after the source so one uninterrupted texture hides
                # the source/template join instead of being cut by it.
                post_overlay = overlay
            else:
                canvas = Image.alpha_composite(canvas, overlay)
            measurements["targets"][str(frame_no)] = {
                "guard": [round(float(x), 3) for x in guard],
                "visible_tip_at_crop": [round(float(x), 3) for x in visible_tip],
                "visible_length_px": round(visible_length, 3),
                "completed_tip": [round(float(x), 3) for x in expected_tip],
                "completed_length_px": round(standard_length, 3),
            }
        elif frame_no == 29:
            guard = np.array([468.0, 47.3])
            visible_tip = np.array([514.6, 6.2])
            direction = visible_tip - guard
            direction /= np.linalg.norm(direction)
            expected_tip = guard + direction * standard_length
            overlay = warp_texture(enchanted_texture, texture_guard, texture_tip, guard, expected_tip, canvas_size)
            post_overlay = overlay
            measurements["targets"]["29"] = {
                "guard": guard.tolist(), "visible_tip_at_crop": visible_tip.tolist(),
                "completed_tip": [round(float(x), 3) for x in expected_tip],
                "completed_length_px": round(standard_length, 3),
            }
        elif frame_no == 31:
            guard = np.array([287.3, 299.3])
            visible_tip = np.array([-2.0, 240.7])
            direction = visible_tip - guard
            direction /= np.linalg.norm(direction)
            expected_tip = guard + direction * standard_length
            overlay = warp_texture(ordinary_texture, ordinary_guard, ordinary_tip, guard, expected_tip, canvas_size)
            canvas = Image.alpha_composite(canvas, overlay)
            measurements["targets"]["31"] = {
                "guard": guard.tolist(), "visible_tip_at_crop": visible_tip.tolist(),
                "completed_tip": [round(float(x), 3) for x in expected_tip],
                "completed_length_px": round(standard_length, 3),
            }
        canvas.alpha_composite(image, (LEFT_PADDING, TOP_PADDING))
        if frame_no == 20:
            # Draw last so the continuous curve closes over the jagged source
            # endpoints instead of being broken by the original cropped effect.
            canvas = draw_effect_curve(canvas, [(210.0, 61.0), (525.0, -260.0), (850.0, 180.0)])
            # Rebuild the diagonal sword separately after effect cleanup. This
            # prevents the white-hot wave mask from deleting its upper half.
            # Fit from the surviving lower blade centreline in the untouched
            # frame (roughly 733,396 -> 887,180), so the replacement does not
            # appear as a second parallel sword.
            guard20 = np.array([733.0, 396.0])
            axis20 = np.array([887.0, 180.0]) - guard20
            axis20 /= np.linalg.norm(axis20)
            tip20 = guard20 + axis20 * standard_length
            sword20 = warp_texture(
                enchanted_texture, texture_guard, texture_tip,
                guard20, tip20, canvas_size,
            )
            canvas = Image.alpha_composite(canvas, sword20)
            measurements["targets"]["20"] = {"effect": "continuous quadratic sword-wave drawn over both crop joins"}
        if post_overlay is not None:
            canvas = Image.alpha_composite(canvas, post_overlay)
        if frame_no == 20 and FRAME20_RERENDER.exists():
            # The generated repair uses a different canvas size. A uniform
            # 1.17x transform aligns its alpha bounds and planted feet with the
            # surrounding production frames without distorting the redraw.
            repaired = Image.open(FRAME20_RERENDER).convert("RGBA")
            repaired = repaired.resize(
                (round(repaired.width * 1.17), round(repaired.height * 1.17)),
                Image.Resampling.LANCZOS,
            )
            aligned = Image.new("RGBA", canvas_size, (0, 0, 0, 0))
            aligned.alpha_composite(repaired, (30, 46))
            canvas = aligned
        canvas.save(OUTPUT / source.name, optimize=True)
        built.append(canvas)

    # Save the requested point-and-measure evidence.
    overlay = reference.convert("RGB")
    draw = ImageDraw.Draw(overlay)
    g = tuple(np.round(ref_guard).astype(int))
    t = tuple(np.round(ref_tip).astype(int))
    draw.line((g, t), fill=(0, 255, 255), width=3)
    draw.ellipse((g[0] - 7, g[1] - 7, g[0] + 7, g[1] + 7), fill=(0, 255, 0))
    draw.ellipse((t[0] - 7, t[1] - 7, t[0] + 7, t[1] + 7), fill=(255, 0, 0))
    draw.text((20, 20), f"guard-tip = {standard_length:.1f}px", fill="white", stroke_width=2, stroke_fill="black")
    overlay.save(QA / "reference_frame_015_measurement.png")

    picks = [15, 16, 17, 18, 20, 29, 31, 32]
    previews = [composite(built[i - 1], "black").resize((382, 247), Image.Resampling.LANCZOS) for i in picks]
    sheet = Image.new("RGB", (4 * 382, 2 * (247 + 24)), "#202020")
    draw = ImageDraw.Draw(sheet)
    for slot, (frame_no, preview) in enumerate(zip(picks, previews)):
        x = (slot % 4) * 382
        y = (slot // 4) * (247 + 24)
        sheet.paste(preview, (x, y))
        draw.text((x + 5, y + 250), f"frame {frame_no:03d}", fill="white")
    sheet.save(QA / "completed_frames_black_contact.jpg", quality=95)

    duration_ms = round(1000 / 30)
    gif_frames = [composite(frame, "black").resize((574, 370), Image.Resampling.LANCZOS) for frame in built]
    gif_frames[0].save(QA / "preview_completed_black.gif", save_all=True, append_images=gif_frames[1:], duration=duration_ms, loop=0, disposal=2)
    (QA / "sword_measurements.json").write_text(json.dumps(measurements, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(measurements, indent=2))


if __name__ == "__main__":
    main()
