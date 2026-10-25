import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import SS.UI.PackageCardGroups;

/** Runs without game graphics or library initialization. */
public class PackageCardGroupsCheck {
    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    public static void main(String[] args) {
        List<String> value = new ArrayList<>(Arrays.asList("shared", "v", "twoWay", "v"));
        List<String> consistency = Arrays.asList("shared", "c", "twoWay");
        List<String> ceiling = Arrays.asList("shared", "e");
        PackageCardGroups grouped = new PackageCardGroups(value, consistency, ceiling);
        equal(Arrays.asList("shared"), grouped.common);
        equal(Arrays.asList("v", "twoWay"), grouped.exclusive.get(0));
        equal(Arrays.asList("c", "twoWay"), grouped.exclusive.get(1));
        equal(Arrays.asList("e"), grouped.exclusive.get(2));
        equal(Arrays.asList("shared", "v", "twoWay", "v"), value);
        value.clear();
        equal(Arrays.asList("v", "twoWay"), grouped.exclusive.get(0));
        PackageCardGroups empty = new PackageCardGroups(Collections.<String>emptyList(),
                Arrays.asList("a"), Arrays.asList("a"));
        equal(Collections.emptyList(), empty.common);
        equal(Arrays.asList("a"), empty.exclusive.get(1));
        List<String> all = Arrays.asList("a", "b", "c", "d", "e");
        PackageCardGroups same = new PackageCardGroups(all, all, all);
        equal(all, same.common);
        equal(Collections.emptyList(), same.exclusive.get(0));
        try {
            grouped.common.add("mutate");
            throw new AssertionError("Catalog data must be immutable");
        } catch (UnsupportedOperationException expected) { }
        System.out.println("PASS: shared intersection, two-variant overlap, order, deduplication, empty lists, isolation");
    }
}
