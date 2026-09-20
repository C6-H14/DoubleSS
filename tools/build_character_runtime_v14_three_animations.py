from pathlib import Path
import importlib.util
import json
import shutil

import cv2
import numpy as np
from PIL import Image, ImageDraw
from scipy.ndimage import distance_transform_edt

from build_attack_sword_pink40_runtime import premultiplied_resize


ROOT = Path(r"D:\StSmod\DoubleSS")
BASE = ROOT / "art_candidates" / "character_runtime_v13_attack40_cast37_edgeclean"
CANDIDATES = ROOT / "art_candidates" / "video_source" / "new_animation_candidates_v1"
SOURCES = {
    "attack_heavy": (CANDIDATES / "attack_heavy" / "pink_frames", "attack_heavy_*.png", 40.0),
    "cast_debuff": (CANDIDATES / "cast_debuff" / "pink_frames", "cast_debuff_*.png", 1000.0 / 22.0),
    "cast_buff": (CANDIDATES / "cast_buff_transition_test_v6" / "pink_frames", "cast_buff_*.png", 50.0),
}
OUTPUT = ROOT / "art_candidates" / "character_runtime_v14_three_animations"
SEMANTIC_RGBA = ROOT / "art_candidates" / "matting_tests" / "matanyone_v14_three" / "final_rgba"
IMAGES = OUTPUT / "images"
EXPORT = OUTPUT / "export"
CANVAS = 700
RESIZED_W = 582
RESIZED_H = 420
PASTE_X = 49
PASTE_Y = 174
ATTACHMENT_Y = 210.0


def chroma_alpha(image: Image.Image) -> Image.Image:
    rgb = np.asarray(image.convert("RGB"), dtype=np.uint8)
    hsv = cv2.cvtColor(rgb, cv2.COLOR_RGB2HSV)
    h, s, v = cv2.split(hsv)
    # Key by magenta colour bias as well as hue. The generated background fades
    # into low-saturation pale pink around bright effects; saturation-only keys
    # leave that pale mixture behind as a visible halo in game.
    rf = rgb[:, :, 0].astype(np.float32)
    gf = rgb[:, :, 1].astype(np.float32)
    bf = rgb[:, :, 2].astype(np.float32)
    magenta_bias = (rf + bf) * 0.5 - gf
    # Measured backgrounds and their pale mixed fringe occupy H=150..179.
    # Do not wrap hue distance across 179->0: skin is around H=0..12 and must
    # remain opaque, while purple debuff smoke around H=140 is also preserved.
    hue_gate = np.clip((h.astype(np.float32) - 148.0) / 8.0, 0.0, 1.0)
    pink_strength = np.clip((magenta_bias - 2.0) / 28.0, 0.0, 1.0) * hue_gate
    pink_strength *= np.clip((v.astype(np.float32) - 28.0) / 72.0, 0.0, 1.0)
    alpha = np.uint8(np.round((1.0 - pink_strength) * 255.0))

    # Apply the measured narrow hue key globally. Around cast_buff's gold aura,
    # background pink can become enclosed and therefore is not edge-connected.
    # Skin (H=0..12), gold and purple smoke (about H=140) are outside this band.
    alpha[alpha < 6] = 0

    # Replace partially transparent edge RGB with nearest solid foreground RGB,
    # preventing magenta/green halos under Spine's linear texture filtering.
    solid = alpha >= 248
    if not solid.any():
        raise RuntimeError("No opaque foreground found")
    _, nearest = distance_transform_edt(~solid, return_indices=True)
    clean_rgb = rgb.copy()
    fringe = (alpha > 0) & (alpha < 248)
    clean_rgb[fringe] = rgb[nearest[0], nearest[1]][fringe]
    clean_rgb[alpha == 0] = 0
    return Image.fromarray(np.dstack((clean_rgb, alpha)), "RGBA")


def runtime_canvas(source: Path, prefix: str, index: int) -> Image.Image:
    semantic = SEMANTIC_RGBA / prefix / f"{prefix}_{index:03d}.png"
    if semantic.exists():
        keyed = Image.open(semantic).convert("RGBA")
    else:
        keyed = chroma_alpha(Image.open(source))
    resized = premultiplied_resize(keyed, (RESIZED_W, RESIZED_H))
    canvas = Image.new("RGBA", (CANVAS, CANVAS), (0, 0, 0, 0))
    canvas.alpha_composite(resized, (PASTE_X, PASTE_Y))
    return canvas


def attachment(name: str) -> dict:
    return {"path": name, "x": 0, "y": ATTACHMENT_Y, "width": CANVAS, "height": CANVAS}


def timeline(prefix: str, count: int, fps: float) -> list[dict]:
    keys = []
    for index in range(1, count + 1):
        key = {"name": f"{prefix}_{index:03d}"}
        if index > 1:
            key["time"] = round((index - 1) / fps, 6)
        keys.append(key)
    return keys


def composite(image: Image.Image, color: tuple[int, int, int, int]) -> Image.Image:
    return Image.alpha_composite(Image.new("RGBA", image.size, color), image).convert("RGB")


def main() -> None:
    if OUTPUT.exists():
        shutil.rmtree(OUTPUT)
    IMAGES.mkdir(parents=True)
    EXPORT.mkdir(parents=True)

    # Preserve the two already game-tested animations byte-for-byte.
    for path in sorted((BASE / "images").glob("*.png")):
        shutil.copy2(path, IMAGES / path.name)

    source_json = json.loads((BASE / "DoubleSS_character_attack40_cast37_import.json").read_text(encoding="utf-8"))
    source_json["skeleton"]["images"] = str(IMAGES.resolve()).replace("\\", "/") + "/"
    attachments = source_json["skins"][0]["attachments"]["character_frame"]
    animations = source_json["animations"]
    preview_frames = []
    metadata = {}

    for prefix, (directory, pattern, fps) in SOURCES.items():
        paths = sorted(directory.glob(pattern))
        if not paths:
            raise RuntimeError(f"No frames found for {prefix}: {directory}")
        built = []
        for index, path in enumerate(paths, 1):
            name = f"{prefix}_{index:03d}"
            frame = runtime_canvas(path, prefix, index)
            frame.save(IMAGES / f"{name}.png", optimize=True)
            attachments[name] = attachment(name)
            built.append(frame)
        duration = len(built) / fps
        animations[prefix] = {
            "slots": {"character_frame": {"attachment": timeline(prefix, len(built), fps)}},
            "events": [{"time": round(duration, 6), "name": "attack_end"}],
        }
        metadata[prefix] = {"frames": len(built), "fps": fps, "duration": duration}
        picks = np.linspace(0, len(built) - 1, min(12, len(built))).round().astype(int)
        preview_frames.extend((prefix, int(i) + 1, built[int(i)]) for i in picks)

    import_json = OUTPUT / "DoubleSS_character_v14_import_3.8.json"
    runtime_json = EXPORT / "character.json"
    import_json.write_text(json.dumps(source_json, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    converter_path = ROOT / "tools" / "spine-compat-probe" / "convert_spine_38_to_34.py"
    spec = importlib.util.spec_from_file_location("converter", converter_path)
    converter = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(converter)
    converter.convert(import_json, runtime_json)

    # Black/white paired contact sheet for edge and accidental-hole inspection.
    thumb_w, thumb_h = 280, 280
    cols = 6
    rows = (len(preview_frames) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h * 2 + 24)), "#202020")
    draw = ImageDraw.Draw(sheet)
    for slot, (prefix, frame_no, frame) in enumerate(preview_frames):
        x = (slot % cols) * thumb_w
        y = (slot // cols) * (thumb_h * 2 + 24)
        black = composite(frame, (0, 0, 0, 255)).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        white = composite(frame, (255, 255, 255, 255)).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        sheet.paste(black, (x, y))
        sheet.paste(white, (x, y + thumb_h))
        draw.text((x + 4, y + thumb_h * 2 + 4), f"{prefix} {frame_no:03d}", fill="white")
    sheet.save(OUTPUT / "alpha_contact_black_white.jpg", quality=94)
    (OUTPUT / "metadata.json").write_text(json.dumps(metadata, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(metadata, indent=2))


if __name__ == "__main__":
    main()
