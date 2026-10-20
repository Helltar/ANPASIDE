# Bundled libraries

MIDletPascal libraries that ship inside the IDE. A program uses one like any other library,
with nothing copied into the project's `libs/`: the compiler is given the IDE's copy as its
global library directory, and a `Lib_<name>.class` in the project's own `libs/` still wins over
it.

| Library | `uses` | What it is for |
|---|---|---|
| `Lib_touch` | `Touch` | multi-touch input, a drop-in replacement for `Lib_sensor` |
| `Lib_sound` | `Sound` | several sounds at once: music that effects do not cut off |
| `Lib_screen` | `Screen` | the safe area: how far the camera cutout and the rounded corners cover the canvas |

## Touch

```pascal
program game;
uses Touch;
begin
  Touch.Init;
  repeat
    if touch_area(0, GetHeight - 60, 60, 60) > 0 then MoveLeft;
    if touch_area(GetWidth - 60, GetHeight - 60, 60, 60) > 0 then Jump;
    { ... }
  until false;
end.
```

`Touch.Init` makes the library's canvas the current screen, as `Sensor.Init` does. A finger
is a number from 0 to 9: 0 is the first finger down, the next one gets the lowest number free.

| Function | Returns |
|---|---|
| `touch_count` | the number of fingers on the screen |
| `touch_down(n)` | 1 while finger `n` is on the screen |
| `touch_x(n)`, `touch_y(n)` | the last position of finger `n`, kept after it is lifted |
| `touch_pressed(n)` | 1 if finger `n` has touched the screen since the previous call for it, so a tap shorter than a frame is not lost |
| `touch_released(n)` | 1 if finger `n` has been lifted since the previous call for it |
| `touch_area(x, y, w, h)` | the number of fingers inside that rectangle: an on-screen button is held while this is above 0, whichever finger holds it |

Every `Lib_sensor` function is there too, with the same name and meaning, following finger 0:
`has_pointer_events`, `has_pointer_motion_events`, `pointer_state`, `pointer_pressed_x/y`,
`pointer_dragged_x/y`, `pointer_released_x/y`. A program written for `Sensor` switches by
changing `uses Sensor` to `uses Touch` and `Sensor.Init` to `Touch.Init`.

Do not mix the two kinds in one check: `touch_area` and the other `touch_*` functions see every
finger, the `pointer_*` functions only finger 0. Testing a rectangle with `touch_area` and then
taking the position from `pointer_pressed_x` reads the wrong finger as soon as two are down, for
instance an inventory tapped while finger 0 holds a walk button. Take the position from the
finger that is inside:

```pascal
for i := 0 to 9 do
  if (touch_down(i) = 1) and (touch_x(i) >= x) and (touch_x(i) < x + w)
    and (touch_y(i) >= y) and (touch_y(i) < y + h) then
    slot := (touch_x(i) - x) div cell;
```

A runtime only reports more than one finger when the jar asks for it, so when a program uses
`Touch` the IDE adds `Nokia-UI-Enhancement: EnableMultiPointTouchEvents` to its manifest. The
library reads the finger number through the Nokia `com.nokia.pointer.number` property, which
J2ME Loader and the IDE's own emulator support; on a runtime without multi-touch every touch is
finger 0, and the library works as `Lib_sensor` does.

## Sound

```pascal
program game;
uses Sound;
var ok: integer;
begin
  ok := sound_open(0, '/music.mid', 'audio/midi');
  ok := sound_open(1, '/shot.wav', 'audio/x-wav');
  sound_loop(0, -1);
  sound_volume(0, 60);
  ok := sound_play(0);
  repeat
    if KeyToAction(GetKeyPressed) = GA_FIRE then ok := sound_play(1);
    { ... }
  until false;
end.
```

The built-in `OpenPlayer` keeps one player, and opening a sound closes the one before it, so
background music stops for every effect. Here a sound is opened on a channel, a number from 0
to 7, and a channel only replaces its own sound. Nothing has to be initialized. A program can
still use `OpenPlayer` next to it; the built-in player is one more sound of its own and does not
stop the channels.

| Function | Does |
|---|---|
| `sound_open(ch, name, type)` | loads the resource `name` of MIME type `type` (`audio/midi`, `audio/x-wav`, `audio/mpeg`, ...) into channel `ch`, closing what the channel held; 1 on success, 0 otherwise |
| `sound_play(ch)` | plays from the beginning, and starts over if the channel is already playing, so an effect fired again cuts off its previous run; 1 on success |
| `sound_pause(ch)`, `sound_resume(ch)` | stops where it is, and continues from there; `sound_resume` returns 1 on success |
| `sound_stop(ch)` | stops and rewinds |
| `sound_loop(ch, count)` | how many times the channel plays in a row, -1 for endlessly; 1 by default, applied at the next `sound_play` |
| `sound_volume(ch, level)` | 0 to 100, 100 by default, applied at once |
| `sound_playing(ch)` | 1 while the channel is playing |
| `sound_duration(ch)` | the length in milliseconds, -1 if unknown; the IDE's emulator only knows it while the sound is playing and answers 0 otherwise |
| `sound_close(ch)` | stops the channel and frees its sound |

The type has to be one the runtime lists, written exactly; every sound is then played by
Android's `MediaPlayer`, which decides what actually works:

| Type | Format | Plays |
|---|---|---|
| `audio/midi`, `audio/x-midi` | MIDI | yes |
| `audio/wav`, `audio/x-wav` | WAV | yes |
| `audio/mpeg`, `audio/mp3` | MP3 | yes |
| `audio/aac`, `audio/mp4` | AAC, M4A | yes |
| `audio/amr`, `audio/amr-wb` | AMR | yes |
| `audio/mmf` | SMAF | no: accepted, but handed to `MediaPlayer` unconverted |
| `audio/x-tone-seq` | J2ME tone sequence | no, for the same reason |

Any other type, `audio/ogg` or a typo, gets an empty player from the runtime instead of an
error, which plays nothing; `sound_open` notices that and answers 0. The built-in `OpenPlayer`
does not, and returns true for it.

`sound_loop` and `sound_volume` belong to the channel, not to the sound, so they survive the next
`sound_open` on it: a music channel keeps its volume across tracks. Whether two sounds really
play at once is up to the runtime. The IDE's emulator, an exported apk and J2ME Loader give every
channel an Android `MediaPlayer` of its own and mix all of them, two MIDI files included; on an
old phone that mixes nothing, `sound_play` may answer 0 while another sound is playing.

## Screen

```pascal
program game;
uses Screen;
var l, t, r, b: integer;
begin
  repeat
    l := screen_inset_left;
    t := screen_inset_top;
    r := screen_inset_right;
    b := screen_inset_bottom;
    { the world fills the whole screen, the controls keep clear of the edges }
    FillRect(GetWidth - r - 64, t, 64, 32);  { a menu button in the top right corner }
    DrawText('v 1.2', l, GetHeight - b - GetStringHeight('A'));
    { ... }
  until false;
end.
```

The IDE's emulator and the apks it exports draw a canvas over the whole screen, under the
camera cutout and into the rounded corners, so whatever a game puts against an edge can be cut
off there. Each function says how far one edge of the canvas is covered, in canvas pixels; a
control that keeps at least that far from every edge stays whole.

| Function | Returns |
|---|---|
| `screen_inset_left`, `screen_inset_top`, `screen_inset_right`, `screen_inset_bottom` | how far that edge is covered, 0 if not at all |

An edge counts the cutout as a band along its whole length, the way Android reports it, and a
rounded corner as the distance that keeps a rectangle's corner inside the arc on both edges it
touches: a Pixel 9 in landscape answers 173 on the side of its camera and 39 on the other three.
Nothing needs initializing, and the values follow the screen when it turns, so a game that
rotates reads them again. They come from the runtime through the `anpaside.screen.inset.<side>`
system properties, which only the IDE's emulator and exported apks answer; anywhere else every
function returns 0.

## Building

`make` compiles every `src/Lib_*.java` against the stand-ins in `build-support/` (`M` and `FW`,
the classes a library links against, never shipped), preverifies the result for CLDC 1.0 and
copies it into `app/src/main/assets/libs/` as `Lib_<name>.bin`, which `AssetInstaller` installs
under its `.class` name again: bundletool silently drops every `.class` entry of an app bundle,
so a `.class` asset is missing from the Google Play build, and the bundle fails validation with
*"Targeted directory 'assets/libs' is empty"*. It needs JDK 8 and the Sun Wireless Toolkit that
[midletpascal-rtl](https://github.com/Helltar/midletpascal-rtl) installs into its
`toolchains/`; `WTK_HOME` and `JDK8_HOME` point elsewhere. The build is deterministic. After
changing a library, bump `AssetInstaller.ASSETS_VERSION`, or installed copies are not refreshed.

The compiler sees only the methods whose modifiers are exactly `public static`
(`read_library_file` in mp3cc's `structures/block.c`), so a helper has to be package-private,
and a `public static synchronized` function would silently vanish from the library. The same
reader takes every constant pool entry as one slot, while a `long` or `double` constant takes
two, so a class holding one fails with *E449 error reading external library file*; the build
refuses such a class. `x / 1000L` is the usual way to get one, and `x / n` with an `int n = 1000`
is the way around it.
