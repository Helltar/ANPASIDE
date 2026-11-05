import javax.microedition.media.Manager;
import javax.microedition.media.Player;
import javax.microedition.media.control.VolumeControl;

/**
 * Several sounds at once for MIDletPascal: {@code uses Sound;}.
 *
 * <p>The built-in {@code OpenPlayer} keeps one player, and opening a sound closes the one
 * before it, so background music stops for every effect. Here a sound is opened on a channel,
 * a number from 0 to 7, and a channel only ever replaces its own sound: music on one channel,
 * effects on the others. Whether the phone mixes them is up to its runtime; the IDE's emulator
 * and J2ME Loader do.
 *
 * <p>Only {@code public static} methods with int and String arguments are visible to the
 * compiler, so everything else here is package-private.
 */
class Lib_sound {

    static final int CHANNELS = 8;

    static final Player[] players = new Player[CHANNELS];
    static final int[] loops = new int[CHANNELS];
    static final int[] levels = new int[CHANNELS];

    // set by sound_pause, so that sound_play_free leaves a paused channel alone
    static final boolean[] paused = new boolean[CHANNELS];

    // when each channel was last started, as a running count of sound_play calls
    static final int[] started = new int[CHANNELS];
    static int plays = 0;

    static {
        for (int i = 0; i < CHANNELS; i++) {
            loops[i] = 1;
            levels[i] = 100;
        }
    }

    /**
     * Loads the resource {@code name} of the given MIME {@code type} ({@code audio/midi},
     * {@code audio/x-wav}, {@code audio/mpeg}, ...) into channel {@code ch}, closing whatever the
     * channel held. 1 on success, 0 if the channel, the resource or the type is wrong.
     */
    public static int sound_open(int ch, String name, String type) {
        if (!isChannel(ch)) {
            return 0;
        }

        sound_close(ch);

        try {
            Player player = Manager.createPlayer(FW.fw.getClass().getResourceAsStream(name), type);
            player.realize();

            // a runtime built on j2me loader answers a type it cannot play with an empty
            // player instead of an exception, and that player reports no content type
            String contentType = player.getContentType();

            if (contentType == null || contentType.length() == 0) {
                player.close();
                return 0;
            }

            player.prefetch();
            players[ch] = player;
            applyLevel(ch);
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Plays channel {@code ch} from the beginning, starting it over if it is already playing,
     * so an effect fired again cuts off its previous run. 1 on success, otherwise 0.
     */
    public static int sound_play(int ch) {
        Player player = player(ch);

        if (player == null) {
            return 0;
        }

        paused[ch] = false;
        started[ch] = ++plays;

        try {
            player.stop();
            player.setLoopCount(loops[ch]);
            player.setMediaTime(0);
            player.start();
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Plays the first channel from {@code first} to {@code last} that holds a sound and is not
     * playing, so an effect opened on several channels can overlap itself. When all of them are
     * playing, the one started longest ago starts over. A paused channel is left alone. Returns
     * the channel it played, or -1 if none of them could be played.
     */
    public static int sound_play_free(int first, int last) {
        int from = first < 0 ? 0 : first;
        int to = last >= CHANNELS ? CHANNELS - 1 : last;
        int oldest = -1;

        for (int ch = from; ch <= to; ch++) {
            if (players[ch] == null || paused[ch]) {
                continue;
            }

            if (sound_playing(ch) == 0) {
                return sound_play(ch) == 1 ? ch : -1;
            }

            if (oldest < 0 || started[ch] - started[oldest] < 0) {
                oldest = ch;
            }
        }

        if (oldest < 0) {
            return -1;
        }

        return sound_play(oldest) == 1 ? oldest : -1;
    }

    /** Stops channel {@code ch} where it is; {@code sound_resume} continues from there. */
    public static void sound_pause(int ch) {
        Player player = player(ch);

        if (player == null) {
            return;
        }

        paused[ch] = true;

        try {
            player.stop();
        } catch (Exception e) {
        }
    }

    /** Continues channel {@code ch} from where it was paused. 1 on success, otherwise 0. */
    public static int sound_resume(int ch) {
        Player player = player(ch);

        if (player == null) {
            return 0;
        }

        paused[ch] = false;

        try {
            player.start();
            return 1;
        } catch (Exception e) {
            return 0;
        }
    }

    /** Stops channel {@code ch} and rewinds it, so the next play starts at the beginning. */
    public static void sound_stop(int ch) {
        Player player = player(ch);

        if (player == null) {
            return;
        }

        paused[ch] = false;

        try {
            player.stop();
            player.setMediaTime(0);
        } catch (Exception e) {
        }
    }

    /**
     * How many times channel {@code ch} plays in a row, -1 for endlessly; 1 by default. It is
     * kept for the channel across {@code sound_open} and takes effect at the next
     * {@code sound_play}.
     */
    public static void sound_loop(int ch, int count) {
        if (isChannel(ch) && count != 0) {
            loops[ch] = count < 0 ? -1 : count;
        }
    }

    /**
     * The volume of channel {@code ch}, from 0 to 100; 100 by default. It is kept for the
     * channel across {@code sound_open} and applies at once.
     */
    public static void sound_volume(int ch, int level) {
        if (!isChannel(ch)) {
            return;
        }

        levels[ch] = clampLevel(level);
        applyLevel(ch);
    }

    /** 1 while channel {@code ch} is playing, otherwise 0. */
    public static int sound_playing(int ch) {
        Player player = player(ch);

        try {
            return player != null && player.getState() == Player.STARTED ? 1 : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /** The length of the sound on channel {@code ch} in milliseconds, -1 if it is not known. */
    public static int sound_duration(int ch) {
        Player player = player(ch);

        if (player == null) {
            return -1;
        }

        try {
            long duration = player.getDuration();
            return duration < 0 ? -1 : (int) (duration / 1000);
        } catch (Exception e) {
            return -1;
        }
    }

    /** Stops channel {@code ch} and frees its sound; the channel can be opened again. */
    public static void sound_close(int ch) {
        Player player = player(ch);

        if (player == null) {
            return;
        }

        players[ch] = null;
        paused[ch] = false;

        try {
            player.close();
        } catch (Exception e) {
        }
    }

    /**
     * The volume of the sound the built-in {@code OpenPlayer} opened, from 0 to 100.
     * {@code OpenPlayer} makes a new player for every sound, at full volume, so call this again
     * after each one. It does nothing in a program that never calls {@code OpenPlayer}.
     */
    public static void sound_player_volume(int level) {
        try {
            // P is only in the jar when the program calls OpenPlayer, hence Throwable
            setLevel(P.p, clampLevel(level));
        } catch (Throwable e) {
        }
    }

    static void applyLevel(int ch) {
        setLevel(players[ch], levels[ch]);
    }

    static void setLevel(Player player, int level) {
        if (player == null) {
            return;
        }

        try {
            VolumeControl volume = (VolumeControl) player.getControl("VolumeControl");

            if (volume != null) {
                volume.setLevel(level);
            }
        } catch (Exception e) {
        }
    }

    static int clampLevel(int level) {
        return level < 0 ? 0 : level > 100 ? 100 : level;
    }

    static Player player(int ch) {
        return isChannel(ch) ? players[ch] : null;
    }

    static boolean isChannel(int ch) {
        return ch >= 0 && ch < CHANNELS;
    }
}
