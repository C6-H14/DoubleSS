from pathlib import Path
import shutil

from PIL import Image


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = (ROOT / "art_candidates" / "video_source" / "death_splice_analysis" /
          "death_splice_v25_alpha_conservative" / "alpha_frames" / "death_132.png")
TARGET = ROOT / "src" / "main" / "resources" / "img" / "char" / "Corpse.png"
OUTPUT = ROOT / "art_candidates" / "death_corpse_static_v1"


def main():
    OUTPUT.mkdir(parents=True, exist_ok=True)
    backup = OUTPUT / "Corpse_before_death_final.png"
    if not backup.exists():
        shutil.copy2(TARGET, backup)

    source = Image.open(SOURCE).convert("RGBA")
    bbox = source.getchannel("A").getbbox()
    if bbox is None:
        raise RuntimeError("Final death frame is empty")
    subject = source.crop(bbox)
    # The source-to-runtime scale used by the Spine animation is exactly 0.5.
    subject = subject.resize((round(subject.width * 0.5), round(subject.height * 0.5)),
                             Image.Resampling.LANCZOS)
    canvas = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    x = (canvas.width - subject.width) // 2
    # Idle's feet touch the bottom of its source canvas. Match that ground
    # baseline instead of inheriting the old Corpse.png transparent padding.
    y = canvas.height - subject.height
    canvas.alpha_composite(subject, (x, y))
    canvas.save(TARGET, optimize=True)
    canvas.save(OUTPUT / "Corpse_death_final.png", optimize=True)

    black = Image.new("RGBA", canvas.size, (24, 24, 24, 255))
    Image.alpha_composite(black, canvas).convert("RGB").save(
        OUTPUT / "Corpse_death_final_black.png", quality=96)
    print({"source_bbox": bbox, "subject_size": subject.size,
           "paste": (x, y), "target": str(TARGET)})


if __name__ == "__main__":
    main()
