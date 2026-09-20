import com.badlogic.gdx.files.FileHandle;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.attachments.AttachmentLoader;
import com.esotericsoftware.spine.attachments.BoundingBoxAttachment;
import com.esotericsoftware.spine.attachments.MeshAttachment;
import com.esotericsoftware.spine.attachments.PathAttachment;
import com.esotericsoftware.spine.attachments.RegionAttachment;

/**
 * Parses an exported Spine JSON with the exact runtime bundled in desktop-1.0.jar.
 * No OpenGL context or texture loading is required, so this is safe to run headlessly.
 */
public final class SpineJsonProbe {
    private SpineJsonProbe() {
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Usage: SpineJsonProbe <skeleton.json>");
            System.exit(2);
        }

        SkeletonJson json = new SkeletonJson(new AttachmentLoader() {
            @Override
            public RegionAttachment newRegionAttachment(Skin skin, String name, String path) {
                return new RegionAttachment(name);
            }

            @Override
            public MeshAttachment newMeshAttachment(Skin skin, String name, String path) {
                return new MeshAttachment(name);
            }

            @Override
            public BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name) {
                return new BoundingBoxAttachment(name);
            }

            @Override
            public PathAttachment newPathAttachment(Skin skin, String name) {
                return new PathAttachment(name);
            }
        });

        try {
            SkeletonData data = json.readSkeletonData(new FileHandle(args[0]));
            System.out.println("PARSE_OK");
            System.out.println("bones=" + data.getBones().size);
            System.out.println("slots=" + data.getSlots().size);
            System.out.println("animations=" + data.getAnimations().size);
            for (int i = 0; i < data.getAnimations().size; i++) {
                System.out.println("animation[" + i + "]=" + data.getAnimations().get(i).getName());
            }
        } catch (Throwable error) {
            System.out.println("PARSE_FAILED");
            error.printStackTrace(System.out);
            System.exit(1);
        }
    }
}
