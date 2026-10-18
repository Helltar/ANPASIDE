/*
 * Build-only stand-in for FW, the midlet class of the MIDletPascal runtime.
 *
 * Only the fields a library touches are declared; the real class comes from the bundled RTL
 * and this one never ships.
 */

import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;

public abstract class FW extends MIDlet
{
    public static FW fw;
    public static Displayable CD;
}
