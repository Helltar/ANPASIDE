/*
 * Build-only stand-in for the M class mp3cc generates for every program.
 *
 * A library extends it and links against these fields, so it has to keep their names and
 * types; its class file never ships.
 */

import javax.microedition.lcdui.Canvas;
import javax.microedition.lcdui.Graphics;
import javax.microedition.lcdui.Image;

public class M extends Canvas implements Runnable
{
    public static Image I;
    public static Graphics G;
    public static M T;
    public static int KC;

    public void paint(Graphics graphics)
    {
    }

    public void run()
    {
    }
}
