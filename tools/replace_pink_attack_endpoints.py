from pathlib import Path
import shutil

from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS\art_candidates")
MASTER = ROOT / "video_source" / "attack_sword_v2_final39_swordfixed_v6" / "frames" / "attack_sword_01.png"
OUT = ROOT / "matting_tests" / "matanyone_attack_pink_v1" / "selected47_50fps"
FRAMES = OUT / "frames"
BACKUP = OUT / "endpoint_backup_matanyone"


def alpha_bbox(im: Image.Image):
    return im.getchannel("A").getbbox()


def composite_on(im: Image.Image, color):
    bg = Image.new("RGBA", im.size, (*color, 255))
    return Image.alpha_composite(bg, im).convert("RGB")


def main():
    first_path = FRAMES / "attack_sword_01.png"
    last_path = FRAMES / "attack_sword_47.png"
    BACKUP.mkdir(parents=True, exist_ok=True)
    for path in (first_path, last_path):
        backup = BACKUP / path.name
        if not backup.exists():
            shutil.copy2(path, backup)

    target = Image.open(BACKUP / "attack_sword_01.png").convert("RGBA")
    target_bbox = alpha_bbox(target)
    if target_bbox is None:
        raise RuntimeError("Current first frame has no visible alpha pixels")

    master = Image.open(MASTER).convert("RGBA")
    master_bbox = alpha_bbox(master)
    if master_bbox is None:
        raise RuntimeError("Transparent master has no visible alpha pixels")

    cropped = master.crop(master_bbox)
    x0, y0, x1, y1 = target_bbox
    fitted = cropped.resize((x1 - x0, y1 - y0), Image.Resampling.LANCZOS)
    aligned = Image.new("RGBA", target.size, (0, 0, 0, 0))
    aligned.alpha_composite(fitted, (x0, y0))

    # Identical endpoints guarantee a clean closed pose if the animation is looped.
    aligned.save(first_path, optimize=True)
    aligned.save(last_path, optimize=True)
    aligned.save(OUT / "transparent_original_endpoint_aligned.png", optimize=True)

    frames = [Image.open(p).convert("RGBA") for p in sorted(FRAMES.glob("attack_sword_*.png"))]
    gif_frames = [composite_on(im, (32, 32, 32)) for im in frames]
    gif_frames[0].save(
        OUT / "attack_sword_pink47_50fps.gif",
        save_all=True,
        append_images=gif_frames[1:],
        duration=20,
        loop=0,
        disposal=2,
    )

    # Contact sheet on black, useful for catching fringe and endpoint jumps.
    thumb_w, thumb_h = 256, 192
    cols = 8
    rows = (len(frames) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + 22)), "black")
    draw = ImageDraw.Draw(sheet)
    for i, im in enumerate(frames):
        thumb = composite_on(im, (0, 0, 0)).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = (i % cols) * thumb_w
        y = (i // cols) * (thumb_h + 22)
        sheet.paste(thumb, (x, y))
        draw.text((x + 5, y + thumb_h + 3), f"{i + 1:02d}", fill="white")
    sheet.save(OUT / "black_contact_sheet.png", optimize=True)

    # Focused before/after proof.
    old_first = Image.open(BACKUP / "attack_sword_01.png").convert("RGBA")
    old_last = Image.open(BACKUP / "attack_sword_47.png").convert("RGBA")
    panels = [composite_on(old_first, (0, 0, 0)), composite_on(aligned, (0, 0, 0)), composite_on(old_last, (0, 0, 0))]
    proof = Image.new("RGB", (target.width * 3, target.height + 36), "#202020")
    labels = ["OLD FIRST", "NEW TRANSPARENT MASTER (FIRST = LAST)", "OLD LAST"]
    pd = ImageDraw.Draw(proof)
    for i, panel in enumerate(panels):
        proof.paste(panel, (i * target.width, 36))
        pd.text((i * target.width + 12, 10), labels[i], fill="white")
    proof.save(OUT / "endpoint_replacement_proof.png", optimize=True)

    print(f"master={MASTER}")
    print(f"master_bbox={master_bbox}")
    print(f"target_bbox={target_bbox}")
    print(f"aligned_bbox={alpha_bbox(aligned)}")
    print(f"frames={len(frames)} duration={len(frames) / 50:.2f}s")


if __name__ == "__main__":
    main()
