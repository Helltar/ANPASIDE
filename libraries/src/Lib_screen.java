/**
 * The safe area of the screen for MIDletPascal: {@code uses Screen;}.
 *
 * <p>A phone's camera cutout and rounded corners can cover the edges of a full-screen canvas.
 * Each function says how far one edge is covered, in canvas pixels, so that controls drawn at
 * least that far from every edge stay whole. They are 0 where nothing covers the canvas, and
 * on a runtime that does not report them, which is everything but the IDE's emulator and the
 * apks it exports.
 *
 * <p>The values change when the screen turns, so a game that rotates reads them again after
 * {@code GetWidth} or {@code GetHeight} change.
 */
class Lib_screen {

    public static int screen_inset_left() {
        return inset("left");
    }

    public static int screen_inset_top() {
        return inset("top");
    }

    public static int screen_inset_right() {
        return inset("right");
    }

    public static int screen_inset_bottom() {
        return inset("bottom");
    }

    // the runtime answers with the inset of the current canvas, already in its pixels
    static int inset(String side) {
        String value = System.getProperty("anpaside.screen.inset." + side);

        if (value == null) {
            return 0;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
