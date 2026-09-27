from pathlib import Path

from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod")
SOURCE = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v4_new_video_2s" / "frames_120f_60fps"
OUTPUT = ROOT / "art_preview" / "alternate_fencer_combo_attack" / "combo_attack_v4_new_video_2s" / "first_thrust_right_edge_inspection.jpg"


def main() -> None:
    numbers = list(range(17, 39))
    crop = (760, 250, 1112, 520)
    scale = 2
    label_h = 22
    tile_w = (crop[2] - crop[0]) * scale
    tile_h = (crop[3] - crop[1]) * scale
    sheet = Image.new("RGB", (tile_w * 4, (tile_h + label_h) * 6), (25, 25, 25))
    draw = ImageDraw.Draw(sheet)
    for slot, number in enumerate(numbers):
        image = Image.open(SOURCE / f"frame_{number:03d}.png").convert("RGB")
        tile = image.crop(crop).resize((tile_w, tile_h), Image.Resampling.NEAREST)
        x = slot % 4 * tile_w
        y = slot // 4 * (tile_h + label_h)
        sheet.paste(tile, (x, y + label_h))
        draw.text((x + 5, y + 4), f"frame {number:03d}", fill="white")
    sheet.save(OUTPUT, quality=95)
    print(OUTPUT)


if __name__ == "__main__":
    main()
