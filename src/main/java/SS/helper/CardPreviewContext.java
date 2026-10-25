package SS.helper;

import java.util.function.Supplier;

/** Only newly constructed codex copies capture this context; live cards are untouched. */
public final class CardPreviewContext {
    private static final ThreadLocal<Boolean> MANAGER = new ThreadLocal<>();

    private CardPreviewContext() { }

    public static Boolean managerMode() { return MANAGER.get(); }

    public static <T> T withManager(boolean enabled, Supplier<T> factory) {
        Boolean previous = MANAGER.get();
        MANAGER.set(enabled);
        try {
            return factory.get();
        } finally {
            if (previous == null) MANAGER.remove();
            else MANAGER.set(previous);
        }
    }
}
