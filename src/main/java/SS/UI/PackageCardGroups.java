package SS.UI;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

/** Pure, ordered view of the existing subpack lists; never changes their pools. */
public final class PackageCardGroups {
    public final List<String> common;
    public final List<List<String>> exclusive;

    public PackageCardGroups(List<String> value, List<String> consistency, List<String> ceiling) {
        List<List<String>> variants = new ArrayList<>();
        variants.add(value);
        variants.add(consistency);
        variants.add(ceiling);
        LinkedHashSet<String> shared = new LinkedHashSet<>(value);
        shared.retainAll(consistency);
        shared.retainAll(ceiling);
        common = Collections.unmodifiableList(new ArrayList<>(shared));
        List<List<String>> sections = new ArrayList<>();
        for (List<String> variant : variants) {
            LinkedHashSet<String> unique = new LinkedHashSet<>(variant);
            unique.removeAll(shared);
            sections.add(Collections.unmodifiableList(new ArrayList<>(unique)));
        }
        exclusive = Collections.unmodifiableList(sections);
    }
}
