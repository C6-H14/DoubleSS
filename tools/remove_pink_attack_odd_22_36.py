from pathlib import Path
import shutil

from PIL import Image, ImageDraw


OUT = Path(r"D:\StSmod\DoubleSS\art_candidates\matting_tests\matanyone_attack_pink_v1\selected47_50fps")
FRAMES = OUT / "frames"
BACKUP = OUT / "backup_before_remove_odd_22_36"
REMOVE = {23, 25, 27, 29, 31, 33, 35}


def on_background(im: Image.Image, rgb):
    bg = Image.new("RGBA", im.size, (*rgb, 255))
    return Image.alpha_composite(bg, im).convert("RGB")


def main():
    paths = sorted(FRAMES.glob("attack_sword_*.png"))
    if len(paths) != 47:
        raise RuntimeError(f"Expected 47 input frames, found {len(paths)}")

    BACKUP.mkdir(parents=True, exist_ok=True)
    backup_frames = BACKUP / "frames"
    if not backup_frames.exists():
        shutil.copytree(FRAMES, backup_frames)
        for name in ("attack_sword_pink47_50fps.gif", "black_contact_sheet.png", "frame_mapping.txt"):
            source = OUT / name
            if source.exists():
                shutil.copy2(source, BACKUP / name)

    kept = []
    mapping = []
    for old_index, path in enumerate(paths, start=1):
        if old_index in REMOVE:
            continue
        kept.append(Image.open(path).convert("RGBA"))
        mapping.append((len(kept), old_index, path.name))

    staged = OUT / "frames_after_remove_odd_22_36"
    if staged.exists():
        shutil.rmtree(staged)
    staged.mkdir()
    for new_index, image in enumerate(kept, start=1):
        image.save(staged / f"attack_sword_{new_index:02d}.png", optimize=True)

    shutil.rmtree(FRAMES)
    staged.rename(FRAMES)

    gif_frames = [on_background(im, (32, 32, 32)) for im in kept]
    gif_path = OUT / "attack_sword_pink40_50fps.gif"
    gif_frames[0].save(
        gif_path,
        save_all=True,
        append_images=gif_frames[1:],
        duration=20,
        loop=0,
        disposal=2,
    )

    thumb_w, thumb_h = 256, 192
    cols = 8
    rows = (len(kept) + cols - 1) // cols
    sheet = Image.new("RGB", (cols * thumb_w, rows * (thumb_h + 22)), "black")
    draw = ImageDraw.Draw(sheet)
    for i, im in enumerate(kept):
        thumb = on_background(im, (0, 0, 0)).resize((thumb_w, thumb_h), Image.Resampling.LANCZOS)
        x = (i % cols) * thumb_w
        y = (i // cols) * (thumb_h + 22)
        sheet.paste(thumb, (x, y))
        draw.text((x + 5, y + thumb_h + 3), f"{i + 1:02d}", fill="white")
    sheet.save(OUT / "black_contact_sheet_40.png", optimize=True)

    with (OUT / "frame_mapping_40.txt").open("w", encoding="utf-8") as f:
        f.write("new_frame <- old_frame\n")
        for new_index, old_index, _ in mapping:
            f.write(f"{new_index:02d} <- {old_index:02d}\n")
        f.write("\nremoved old frames: 23, 25, 27, 29, 31, 33, 35\n")

    print(f"removed={sorted(REMOVE)}")
    print(f"remaining={len(kept)}")
    print(f"duration={len(kept) / 50:.2f}s")
    print(f"gif={gif_path}")


if __name__ == "__main__":
    main()
