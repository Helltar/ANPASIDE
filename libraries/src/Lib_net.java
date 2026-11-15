import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;
import javax.microedition.io.Connector;
import javax.microedition.io.StreamConnection;

/**
 * A connection to a server for MIDletPascal: {@code uses Net;}.
 *
 * <p>One TCP connection that stays open, for a chat or a game that talks to its server, rather
 * than the request and response of the built-in HTTP functions. Text goes both ways as lines in
 * UTF-8. Connecting, reading and writing all happen on threads of their own, so no function here
 * ever waits on the network: a game loop polls {@code net_status} and {@code net_lines} and keeps
 * drawing in the meantime.
 *
 * <p>The compiler copies only {@code Lib_net.class} into the jar, so there can be no inner or
 * helper classes: an instance of this class is one connection, and both of its threads run it.
 * Only {@code public static} methods with int and String arguments are visible to the compiler,
 * so everything else here is package-private, and synchronization is done with blocks, since a
 * {@code public static synchronized} method would vanish from the library.
 */
class Lib_net implements Runnable {

    static final int FAILED = -1;
    static final int CLOSED = 0;
    static final int CONNECTING = 1;
    static final int CONNECTED = 2;

    // a line longer than this is handed over in pieces rather than growing without end
    static final int MAX_LINE = 65536;

    // the connection of the last net_open, null before the first; one that was replaced or
    // closed keeps running only until its threads notice
    static Lib_net current;

    final String address;

    // everything below is guarded by the instance
    int status = CONNECTING;
    String error = "";
    boolean closed = false;
    int threads = 0;
    StreamConnection connection;
    InputStream input;
    OutputStream output;

    // received lines not read yet, and text waiting to be sent, both as Strings
    final Vector incoming = new Vector();
    final Vector outgoing = new Vector();

    Lib_net(String address) {
        this.address = address;
    }

    /**
     * Starts connecting to {@code address}, given as {@code host:port} or
     * {@code socket://host:port}, closing the connection opened before. Returns 1 when the
     * address is well formed and the attempt has started, otherwise 0; whether it succeeds
     * shows in {@code net_status}.
     */
    public static int net_open(String address) {
        net_close();

        String url = toUrl(address);

        if (url == null) {
            return 0;
        }

        Lib_net connection = new Lib_net(url);
        current = connection;
        new Thread(connection).start();
        return 1;
    }

    /**
     * The state of the connection: 1 while connecting, 2 when connected, 0 when closed (never
     * opened, closed by {@code net_close}, or closed by the server), -1 when connecting or the
     * connection itself failed; {@code net_error} then says why.
     */
    public static int net_status() {
        Lib_net connection = current;

        if (connection == null) {
            return CLOSED;
        }

        synchronized (connection) {
            return connection.status;
        }
    }

    /**
     * Sends {@code text} followed by a line break. It is queued and sent in the background, so
     * it may be called while still connecting: it goes out as soon as the connection is up.
     * Returns 1 when queued, 0 when there is no connection to send it on.
     */
    public static int net_send(String text) {
        return queue(text + "\n");
    }

    /**
     * Sends {@code text} exactly as it is, without a line break, for a protocol that wants
     * {@code chr(13) + chr(10)} or a prompt answered on the same line. Returns as
     * {@code net_send} does.
     */
    public static int net_write(String text) {
        return queue(text);
    }

    /** The number of received lines waiting to be read. */
    public static int net_lines() {
        Lib_net connection = current;

        if (connection == null) {
            return 0;
        }

        synchronized (connection) {
            return connection.incoming.size();
        }
    }

    /**
     * The next received line without its line break, or an empty string when none is waiting;
     * {@code net_lines} tells an empty line from no line. Lines that arrived before the
     * connection closed can still be read afterward, until the next {@code net_open}.
     */
    public static String net_read() {
        Lib_net connection = current;

        if (connection == null) {
            return "";
        }

        synchronized (connection) {
            if (connection.incoming.isEmpty()) {
                return "";
            }

            String line = (String) connection.incoming.elementAt(0);
            connection.incoming.removeElementAt(0);
            return line;
        }
    }

    /** Why the connection failed, as the runtime put it, or an empty string. */
    public static String net_error() {
        Lib_net connection = current;

        if (connection == null) {
            return "";
        }

        synchronized (connection) {
            return connection.error;
        }
    }

    /**
     * Closes the connection; text still queued is dropped. Received lines stay readable.
     * Nothing happens when there is no connection.
     */
    public static void net_close() {
        Lib_net connection = current;

        if (connection != null) {
            connection.finish(CLOSED, null);
        }
    }

    static int queue(String text) {
        Lib_net connection = current;

        if (connection == null) {
            return 0;
        }

        synchronized (connection) {
            if (connection.closed) {
                return 0;
            }

            connection.outgoing.addElement(text);
            connection.notifyAll();
            return 1;
        }
    }

    // "host:port" or "socket://host:port" as a socket url, null when the host or the port is
    // missing; a port of a server socket (no host) is refused too, a client is all this is
    static String toUrl(String address) {
        if (address == null) {
            return null;
        }

        String rest = address.trim();

        if (rest.startsWith("socket://")) {
            rest = rest.substring(9);
        }

        int colon = rest.lastIndexOf(':');

        if (colon <= 0 || colon == rest.length() - 1) {
            return null;
        }

        try {
            int port = Integer.parseInt(rest.substring(colon + 1));

            if (port < 1 || port > 65535) {
                return null;
            }
        } catch (NumberFormatException e) {
            return null;
        }

        return "socket://" + rest;
    }

    // the first thread to run connects and then reads; it starts the second, which writes
    public void run() {
        boolean writer;

        synchronized (this) {
            writer = threads++ > 0;
        }

        if (writer) {
            writeLoop();
        } else {
            connectAndRead();
        }
    }

    void connectAndRead() {
        StreamConnection opened;
        InputStream in;
        OutputStream out;

        try {
            opened = (StreamConnection) Connector.open(address, Connector.READ_WRITE);
            in = opened.openInputStream();
            out = opened.openOutputStream();
        } catch (Throwable e) {
            finish(FAILED, e);
            return;
        }

        synchronized (this) {
            connection = opened;
            input = in;
            output = out;

            if (closed) {
                // net_close came while connecting, and could not close what did not exist yet
                closeStreams();
                return;
            }

            status = CONNECTED;
        }

        new Thread(this).start();

        // bytes of the line being received, decoded once its line break arrives
        byte[] line = new byte[256];
        int length = 0;
        byte[] chunk = new byte[1024];

        try {
            while (true) {
                int count = in.read(chunk);

                if (count < 0) {
                    break;
                }

                for (int i = 0; i < count; i++) {
                    byte b = chunk[i];

                    if (b == '\n') {
                        deliver(line, length);
                        length = 0;
                        continue;
                    }

                    if (length == line.length) {
                        if (length == MAX_LINE) {
                            deliver(line, length);
                            length = 0;
                        } else {
                            byte[] grown = new byte[length * 2 > MAX_LINE ? MAX_LINE : length * 2];
                            System.arraycopy(line, 0, grown, 0, length);
                            line = grown;
                        }
                    }

                    line[length++] = b;
                }
            }

            // the server closed the connection; a last line without a line break still counts
            if (length > 0) {
                deliver(line, length);
            }

            finish(CLOSED, null);
        } catch (Throwable e) {
            finish(FAILED, e);
        }
    }

    void writeLoop() {
        try {
            while (true) {
                String text;
                OutputStream out;

                synchronized (this) {
                    while (outgoing.isEmpty() && !closed) {
                        wait();
                    }

                    if (closed) {
                        return;
                    }

                    text = (String) outgoing.elementAt(0);
                    outgoing.removeElementAt(0);
                    out = output;
                }

                out.write(encode(text));
                out.flush();
            }
        } catch (Throwable e) {
            finish(FAILED, e);
        }
    }

    void deliver(byte[] bytes, int length) {
        // a line break sent as "\r\n" leaves its "\r" behind
        int end = length > 0 && bytes[length - 1] == '\r' ? length - 1 : length;
        String text = decode(bytes, end);

        synchronized (this) {
            if (!closed) {
                incoming.addElement(text);
            }
        }
    }

    // ends the connection once: the first reason wins, so net_close is not later reported as a
    // failure by a thread whose read the closing interrupted
    void finish(int reason, Throwable cause) {
        synchronized (this) {
            if (closed) {
                return;
            }

            closed = true;
            status = reason;

            if (cause != null) {
                String message = cause.getMessage();
                error = message != null && message.length() > 0 ? message : cause.toString();
            }

            outgoing.removeAllElements();
            notifyAll();
            closeStreams();
        }
    }

    void closeStreams() {
        try {
            if (input != null) {
                input.close();
            }
        } catch (IOException e) {
        }

        try {
            if (output != null) {
                output.close();
            }
        } catch (IOException e) {
        }

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (IOException e) {
        }
    }

    // utf-8 by hand: cldc 1.0 only promises the platform's default encoding to String.getBytes
    static byte[] encode(String text) {
        int length = text.length();
        byte[] bytes = new byte[length * 4];
        int n = 0;

        for (int i = 0; i < length; i++) {
            int c = text.charAt(i);

            if (c >= 0xD800 && c <= 0xDBFF && i + 1 < length) {
                int low = text.charAt(i + 1);

                if (low >= 0xDC00 && low <= 0xDFFF) {
                    c = 0x10000 + ((c - 0xD800) << 10) + (low - 0xDC00);
                    i++;
                }
            }

            if (c < 0x80) {
                bytes[n++] = (byte) c;
            } else if (c < 0x800) {
                bytes[n++] = (byte) (0xC0 | (c >> 6));
                bytes[n++] = (byte) (0x80 | (c & 0x3F));
            } else if (c < 0x10000) {
                bytes[n++] = (byte) (0xE0 | (c >> 12));
                bytes[n++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytes[n++] = (byte) (0x80 | (c & 0x3F));
            } else {
                bytes[n++] = (byte) (0xF0 | (c >> 18));
                bytes[n++] = (byte) (0x80 | ((c >> 12) & 0x3F));
                bytes[n++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                bytes[n++] = (byte) (0x80 | (c & 0x3F));
            }
        }

        byte[] result = new byte[n];
        System.arraycopy(bytes, 0, result, 0, n);
        return result;
    }

    // a byte that does not belong to a valid sequence becomes U+FFFD
    static String decode(byte[] bytes, int length) {
        StringBuffer text = new StringBuffer(length);
        int i = 0;

        while (i < length) {
            int b = bytes[i] & 0xFF;
            int extra;
            int c;

            if (b < 0x80) {
                text.append((char) b);
                i++;
                continue;
            } else if (b >= 0xC2 && b <= 0xDF) {
                extra = 1;
                c = b & 0x1F;
            } else if (b >= 0xE0 && b <= 0xEF) {
                extra = 2;
                c = b & 0x0F;
            } else if (b >= 0xF0 && b <= 0xF4) {
                extra = 3;
                c = b & 0x07;
            } else {
                text.append('�');
                i++;
                continue;
            }

            int j = 1;

            while (j <= extra && i + j < length && (bytes[i + j] & 0xC0) == 0x80) {
                c = (c << 6) | (bytes[i + j] & 0x3F);
                j++;
            }

            boolean valid = j > extra
                && !(extra == 2 && (c < 0x800 || (c >= 0xD800 && c <= 0xDFFF)))
                && !(extra == 3 && (c < 0x10000 || c > 0x10FFFF));

            if (!valid) {
                text.append('�');
                i += j;
                continue;
            }

            if (c >= 0x10000) {
                c -= 0x10000;
                text.append((char) (0xD800 + (c >> 10)));
                text.append((char) (0xDC00 + (c & 0x3FF)));
            } else {
                text.append((char) c);
            }

            i += j;
        }

        return text.toString();
    }
}
