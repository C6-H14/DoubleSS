import com.badlogic.gdx.files.FileHandle;
import com.esotericsoftware.spine.SkeletonJson;
import com.esotericsoftware.spine.SkeletonData;
import com.esotericsoftware.spine.Animation;
import com.esotericsoftware.spine.Skin;
import com.esotericsoftware.spine.attachments.*;

public final class SpineJsonProbe {
    public static void main(String[] args) {
        AttachmentLoader loader = new AttachmentLoader() {
            public RegionAttachment newRegionAttachment(Skin skin, String name, String path) {
                RegionAttachment a = new RegionAttachment(name);
                a.setPath(path);
                return a;
            }
            public MeshAttachment newMeshAttachment(Skin skin, String name, String path) {
                return new MeshAttachment(name);
            }
            public BoundingBoxAttachment newBoundingBoxAttachment(Skin skin, String name) {
                return new BoundingBoxAttachment(name);
            }
            public PathAttachment newPathAttachment(Skin skin, String name) {
                return new PathAttachment(name);
            }
        };
        try {
            SkeletonData data = new SkeletonJson(loader).readSkeletonData(new FileHandle(args[0]));
            Animation idle = data.findAnimation("idle");
            System.out.println("OK idleDuration=" + idle.getDuration()
                    + " timelines=" + idle.getTimelines().size);
            for (Object timeline : idle.getTimelines()) {
                System.out.println(timeline.getClass().getName());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
