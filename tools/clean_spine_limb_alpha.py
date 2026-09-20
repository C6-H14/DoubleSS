from pathlib import Path
from shutil import copy2

from PIL import Image


ROOT = Path(r"E:\图与psd\img\DoubleSS\char\spine_work")
LIMB_DIR = ROOT / "images" / "limbs"
BACKUP_DIR = ROOT / "images_original_pre_alpha_cleanup" / "limbs"
FILES = ("arm_L.png", "arm_R_grip.png", "leg_L.png", "leg_R.png")


def main() -> None:
    BACKUP_DIR.mkdir(parents=True, exist_ok=True)

    for name in FILES:
        source = LIMB_DIR / name
        backup = BACKUP_DIR / name
        if not backup.exists():
            copy2(source, backup)

        image = Image.open(source).convert("RGBA")
        red, green, blue, alpha = image.split()
        # Image-generation left isolated alpha=1 pixels far outside each limb.
        # Removing only those pixels preserves every perceptible edge and color.
        cleaned_alpha = alpha.point(lambda value: 0 if value <= 1 else value)
        Image.merge("RGBA", (red, green, blue, cleaned_alpha)).save(source)

        print(f"cleaned: {source}")
        print(f"backup:  {backup}")


if __name__ == "__main__":
    main()
