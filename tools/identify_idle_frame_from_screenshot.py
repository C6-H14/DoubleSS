from pathlib import Path

import cv2
import numpy as np
from PIL import Image, ImageDraw


ROOT = Path(r"D:\StSmod\DoubleSS")
SOURCE = ROOT / "art_candidates/video_source/idle/short_blink_final_v44_semantic_color_stable/alpha_frames"
SCREENSHOT = Path(r"C:\Users\C6H14\AppData\Local\Temp\codex-clipboard-f9b6bbb4-b401-4582-822b-661867b897d7.png")
OUT = SOURCE.parent / "screenshot_frame_match"


def composite(frame):
    return Image.alpha_composite(Image.new("RGBA", frame.size, (45, 45, 45, 255)), frame).convert("RGB")


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    shot = cv2.imread(str(SCREENSHOT), cv2.IMREAD_COLOR)
    detector = cv2.SIFT_create(nfeatures=1000, contrastThreshold=0.015, edgeThreshold=12)
    kp_s, des_s = detector.detectAndCompute(cv2.cvtColor(shot, cv2.COLOR_BGR2GRAY), None)
    matcher = cv2.BFMatcher(cv2.NORM_L2)
    results = []
    frames = []
    for number in range(1, 181):
        rgba = Image.open(SOURCE / f"idle_{number:03d}.png").convert("RGBA")
        frames.append(rgba)
        bbox = rgba.getchannel("A").getbbox()
        crop = rgba.crop(bbox)
        rgb = np.asarray(composite(crop))[:, :, ::-1].copy()
        gray = cv2.cvtColor(rgb, cv2.COLOR_BGR2GRAY)
        kp_f, des_f = detector.detectAndCompute(gray, None)
        good = []
        if des_s is not None and des_f is not None:
            for pair in matcher.knnMatch(des_f, des_s, k=2):
                if len(pair) == 2 and pair[0].distance < 0.72 * pair[1].distance:
                    good.append(pair[0])
        inliers = 0
        median_error = 999.0
        if len(good) >= 5:
            src = np.float32([kp_f[m.queryIdx].pt for m in good]).reshape(-1, 1, 2)
            dst = np.float32([kp_s[m.trainIdx].pt for m in good]).reshape(-1, 1, 2)
            matrix, mask = cv2.estimateAffinePartial2D(src, dst, method=cv2.RANSAC, ransacReprojThreshold=3.0)
            if matrix is not None and mask is not None:
                inliers = int(mask.sum())
                projected = cv2.transform(src, matrix)
                errors = np.linalg.norm(projected[:, 0] - dst[:, 0], axis=1)
                median_error = float(np.median(errors[mask[:, 0] > 0])) if inliers else 999.0
        results.append((inliers, len(good), -median_error, number))
    results.sort(reverse=True)

    picks = [item[3] for item in results[:24]]
    panels = []
    for number in picks:
        frame = frames[number - 1]
        bbox = frame.getchannel("A").getbbox()
        view = composite(frame.crop(bbox))
        view.thumbnail((220, 300), Image.Resampling.LANCZOS)
        panel = Image.new("RGB", (224, 326), (28, 28, 28))
        panel.paste(view, ((224 - view.width) // 2, 24))
        score = next(item for item in results if item[3] == number)
        ImageDraw.Draw(panel).text((4, 4), f"F{number:03d} inliers={score[0]} good={score[1]}", fill="white")
        panels.append(panel)
    sheet = Image.new("RGB", (224 * 6, 326 * 4), (20, 20, 20))
    for i, panel in enumerate(panels):
        sheet.paste(panel, ((i % 6) * 224, (i // 6) * 326))
    sheet.save(OUT / "top_matches.png", optimize=True)

    # A face-only chronological sheet makes eyelid geometry easy to verify.
    face_panels = []
    for number, frame in enumerate(frames, 1):
        face = composite(frame).crop((500, 90, 710, 285)).resize((168, 156), Image.Resampling.LANCZOS)
        panel = Image.new("RGB", (168, 174), (25, 25, 25))
        panel.paste(face, (0, 18))
        ImageDraw.Draw(panel).text((3, 2), f"F{number:03d}", fill="white")
        face_panels.append(panel)
    sheet2 = Image.new("RGB", (168 * 12, 174 * 15), (18, 18, 18))
    for i, panel in enumerate(face_panels):
        sheet2.paste(panel, ((i % 12) * 168, (i // 12) * 174))
    sheet2.save(OUT / "all_faces_chronological.png", optimize=True)
    print("Top matches:")
    for item in results[:24]:
        print(f"frame={item[3]:03d} inliers={item[0]} good={item[1]} median_error={-item[2]:.3f}")


if __name__ == "__main__":
    main()
