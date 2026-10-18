import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Image;

/**
 * Multi-touch input for MIDletPascal: {@code uses Touch;} and {@code Touch.Init;}.
 *
 * <p>A drop-in replacement for Lib_sensor. Every Lib_sensor function is here with the same
 * name and meaning, following the first finger, and the {@code touch_*} functions follow up
 * to ten fingers at once. A finger is the pointer number the runtime reports through the
 * Nokia {@code com.nokia.pointer.number} property, 0 for the first finger down; a runtime
 * without multi-touch only ever reports finger 0.
 *
 * <p>Only {@code public static} methods with int arguments are visible to the compiler, so
 * everything else here is package-private.
 */
class Lib_touch extends M {

    static final int FINGERS = 10;

    static final Object LOCK = new Object();

    static final boolean[] down = new boolean[FINGERS];
    static final int[] fingerX = new int[FINGERS];
    static final int[] fingerY = new int[FINGERS];
    static final int[] presses = new int[FINGERS];
    static final int[] releases = new int[FINGERS];

    static int pointerEvents;
    static int motionEvents;

    static int pressedX;
    static int pressedY;
    static int draggedX;
    static int draggedY;
    static int releasedX;
    static int releasedY;

    Lib_touch() {
        pointerEvents = hasPointerEvents() ? 1 : 0;
        motionEvents = hasPointerMotionEvents() ? 1 : 0;
    }

    public static void init() {
        synchronized (LOCK) {
            for (int i = 0; i < FINGERS; i++) {
                down[i] = false;
                fingerX[i] = 0;
                fingerY[i] = 0;
                presses[i] = 0;
                releases[i] = 0;
            }

            pressedX = pressedY = draggedX = draggedY = releasedX = releasedY = 0;
        }

        M.T = new Lib_touch();
        M.I = Image.createImage(M.T.getWidth(), M.T.getHeight());
        M.G = M.I.getGraphics();
        FW.CD = M.T;
        Display.getDisplay(FW.fw).setCurrent(M.T);
    }

    /** The number of fingers on the screen right now. */
    public static int touch_count() {
        int count = 0;

        for (int i = 0; i < FINGERS; i++) {
            if (down[i]) {
                count++;
            }
        }

        return count;
    }

    /** 1 while finger {@code n} is on the screen, otherwise 0. */
    public static int touch_down(int n) {
        return isFinger(n) && down[n] ? 1 : 0;
    }

    /** The last x of finger {@code n}; it stays where the finger was lifted. */
    public static int touch_x(int n) {
        return isFinger(n) ? fingerX[n] : 0;
    }

    /** The last y of finger {@code n}; it stays where the finger was lifted. */
    public static int touch_y(int n) {
        return isFinger(n) ? fingerY[n] : 0;
    }

    /**
     * 1 if finger {@code n} has touched the screen since the previous call for that finger,
     * otherwise 0. A tap shorter than a frame is not lost, unlike with {@code touch_down}.
     */
    public static int touch_pressed(int n) {
        if (!isFinger(n)) {
            return 0;
        }

        synchronized (LOCK) {
            int count = presses[n];
            presses[n] = 0;
            return count > 0 ? 1 : 0;
        }
    }

    /** 1 if finger {@code n} has been lifted since the previous call for that finger, otherwise 0. */
    public static int touch_released(int n) {
        if (!isFinger(n)) {
            return 0;
        }

        synchronized (LOCK) {
            int count = releases[n];
            releases[n] = 0;
            return count > 0 ? 1 : 0;
        }
    }

    /**
     * The number of fingers on the screen inside the rectangle at {@code x}, {@code y} of size
     * {@code w} by {@code h}: an on-screen button is held while this is above 0, whichever finger
     * holds it.
     */
    public static int touch_area(int x, int y, int w, int h) {
        int count = 0;

        synchronized (LOCK) {
            for (int i = 0; i < FINGERS; i++) {
                if (down[i]
                        && fingerX[i] >= x && fingerX[i] < x + w
                        && fingerY[i] >= y && fingerY[i] < y + h) {
                    count++;
                }
            }
        }

        return count;
    }

    // lib_sensor's functions, kept so that a program switches by changing its uses clause

    public static int has_pointer_events() {
        return pointerEvents;
    }

    public static int has_pointer_motion_events() {
        return motionEvents;
    }

    public static int pointer_state() {
        return down[0] ? 1 : 0;
    }

    public static int pointer_pressed_x() {
        return pressedX;
    }

    public static int pointer_pressed_y() {
        return pressedY;
    }

    public static int pointer_dragged_x() {
        return draggedX;
    }

    public static int pointer_dragged_y() {
        return draggedY;
    }

    public static int pointer_released_x() {
        return releasedX;
    }

    public static int pointer_released_y() {
        return releasedY;
    }

    protected void pointerPressed(int x, int y) {
        int n = finger();

        if (!isFinger(n)) {
            return;
        }

        synchronized (LOCK) {
            down[n] = true;
            fingerX[n] = x;
            fingerY[n] = y;
            presses[n]++;

            if (n == 0) {
                pressedX = x;
                pressedY = y;
            }
        }
    }

    protected void pointerDragged(int x, int y) {
        int n = finger();

        if (!isFinger(n)) {
            return;
        }

        synchronized (LOCK) {
            fingerX[n] = x;
            fingerY[n] = y;

            if (n == 0) {
                draggedX = x;
                draggedY = y;
            }
        }
    }

    protected void pointerReleased(int x, int y) {
        int n = finger();

        if (!isFinger(n)) {
            return;
        }

        synchronized (LOCK) {
            down[n] = false;
            fingerX[n] = x;
            fingerY[n] = y;
            releases[n]++;

            if (n == 0) {
                releasedX = x;
                releasedY = y;
            }
        }
    }

    // a hidden canvas gets no release for the fingers that were on it, so they would stay
    // down for good once the game is back
    protected void hideNotify() {
        synchronized (LOCK) {
            for (int i = 0; i < FINGERS; i++) {
                if (down[i]) {
                    down[i] = false;
                    releases[i]++;
                }
            }
        }

        super.hideNotify();
    }

    // the property only has a value inside a pointer callback of a multi-touch runtime
    static int finger() {
        String number = System.getProperty("com.nokia.pointer.number");

        if (number == null) {
            return 0;
        }

        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    static boolean isFinger(int n) {
        return n >= 0 && n < FINGERS;
    }
}
