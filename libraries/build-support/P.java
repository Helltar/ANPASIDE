/*
 * Build-only stand-in for P, the player behind OpenPlayer in the MIDletPascal runtime.
 *
 * Only the field a library touches is declared; the real class comes from the bundled RTL,
 * and only into a program that calls OpenPlayer. This one never ships.
 */

import javax.microedition.media.Player;

public class P
{
    public static Player p;
}
