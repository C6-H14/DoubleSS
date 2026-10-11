package SS.animation;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/** Temporary, low-volume trace for the inner-character animation test. */
public final class CharacterAnimationDiagnostics {
    private static final long SESSION_ID = System.currentTimeMillis();
    private static final Path LOG_PATH = Paths.get(System.getProperty("user.dir", "."),
            "DoubleSS-animation-debug.log");

    private CharacterAnimationDiagnostics() {
    }

    public static void log(String message) {
        String line = "[DoubleSS animation trace] " + new Date() + " session=" + SESSION_ID
                + " " + message;
        System.err.println(line);
        try {
            Files.write(LOG_PATH, (line + System.lineSeparator()).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | SecurityException error) {
            System.err.println("[DoubleSS animation trace] Could not write " + LOG_PATH + ": " + error);
        }
    }

    public static String logPath() {
        return LOG_PATH.toAbsolutePath().toString();
    }

    public static String codeSource(Class<?> type) {
        try {
            URL location = type.getProtectionDomain().getCodeSource().getLocation();
            return location == null ? "unknown" : location.toString();
        } catch (SecurityException | NullPointerException error) {
            return "unavailable(" + error.getClass().getSimpleName() + ")";
        }
    }

    public static String caller() {
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String name = frame.getClassName();
            if (name.startsWith("SS.cards.") || name.startsWith("SS.modcore.")
                    || name.startsWith("SS.action.")) {
                return name + "." + frame.getMethodName() + ":" + frame.getLineNumber();
            }
        }
        return "unknown";
    }

    public static String stack(int maxFrames) {
        StringBuilder result = new StringBuilder();
        int count = 0;
        for (StackTraceElement frame : Thread.currentThread().getStackTrace()) {
            String name = frame.getClassName();
            if (name.equals(Thread.class.getName())
                    || name.equals(CharacterAnimationDiagnostics.class.getName())) {
                continue;
            }
            if (count++ >= maxFrames) {
                break;
            }
            if (result.length() > 0) {
                result.append(" <- ");
            }
            result.append(name).append('.').append(frame.getMethodName())
                    .append(':').append(frame.getLineNumber());
        }
        return result.toString();
    }
}
