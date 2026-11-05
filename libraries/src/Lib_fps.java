/**
 * The frame rate of a game loop for MIDletPascal: {@code uses FPS;}.
 *
 * <p>{@code fps_count} is called once per frame and {@code fps_get} answers how many frames the
 * last full second had; {@code fps_limit} keeps the loop from running faster than a given rate.
 * Everything is integer arithmetic, since CLDC 1.0 phones have no floating point. After a
 * library GOAR posted on annimon.com.
 */
class Lib_fps {

    // frames counted since windowStart, and the rate of the last finished second
    static int frames = 0;
    static int rate = 0;
    static long windowStart;
    static boolean counting = false;

    // the time the next frame is due, kept whole: the step is 1000 / target with the remainder
    // carried over in stepRemainder, so 60 frames take exactly a second rather than 60 × 16 ms
    static long due;
    static int target = 0;
    static int stepRemainder = 0;

    /** Counts one frame. Call it once per pass of the game loop. */
    public static void fps_count() {
        long now = System.currentTimeMillis();

        if (!counting) {
            counting = true;
            windowStart = now;
            frames = 0;
        }

        frames++;

        // an int, so that no long constant lands in the class: compilers before mp3cc 3.5.5
        // misread one and fail with E449
        int elapsed = (int) (now - windowStart);

        if (elapsed >= 1000) {
            rate = (frames * 1000 + elapsed / 2) / elapsed;
            frames = 0;
            windowStart = now;
        }
    }

    /**
     * The number of frames {@code fps_count} saw in the last full second, 0 until one has
     * passed. It changes once a second, so it can be drawn every frame without flickering.
     */
    public static int fps_get() {
        return rate;
    }

    /**
     * Waits until the next frame is due at {@code n} frames per second, so a loop that calls it
     * once per pass runs no faster than that. A loop that is already slower is not held up, and
     * one that falls behind, or was paused, does not race to catch up. 0 or less does nothing.
     */
    public static void fps_limit(int n) {
        if (n <= 0) {
            target = 0;
            return;
        }

        long now = System.currentTimeMillis();

        if (n != target) {
            target = n;
            due = now;
            stepRemainder = 0;
        }

        int step = 1000 / n;
        stepRemainder += 1000 % n;

        if (stepRemainder >= n) {
            stepRemainder -= n;
            step++;
        }

        due += step;

        long wait = due - now;

        if (wait > 0) {
            try {
                Thread.sleep(wait);
            } catch (InterruptedException e) {
            }
        } else if (wait < -step) {
            due = now;
        }
    }
}
