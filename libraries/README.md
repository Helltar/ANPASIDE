# Bundled libraries

MIDletPascal libraries that ship inside the IDE. A program uses one like any other library,
with nothing copied into the project's `libs/`: the compiler is given the IDE's copy as its
global library directory, and a `Lib_<name>.class` in the project's own `libs/` still wins over
it.

| Library | `uses` | What it is for |
|---|---|---|
| `Lib_touch` | `Touch` | multi-touch input, a drop-in replacement for `Lib_sensor` |

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

A runtime only reports more than one finger when the jar asks for it, so when a program uses
`Touch` the IDE adds `Nokia-UI-Enhancement: EnableMultiPointTouchEvents` to its manifest. The
library reads the finger number through the Nokia `com.nokia.pointer.number` property, which
J2ME Loader and the IDE's own emulator support; on a runtime without multi-touch every touch is
finger 0, and the library works as `Lib_sensor` does.

## Building

`make` compiles every `src/Lib_*.java` against the stand-ins in `build-support/` (`M` and `FW`,
the classes a library links against, never shipped), preverifies the result for CLDC 1.0 and
copies it into `app/src/main/assets/libs/`. It needs JDK 8 and the Sun Wireless Toolkit that
[midletpascal-rtl](https://github.com/Helltar/midletpascal-rtl) installs into its
`toolchains/`; `WTK_HOME` and `JDK8_HOME` point elsewhere. The build is deterministic. After
changing a library, bump `AssetInstaller.ASSETS_VERSION`, or installed copies are not refreshed.

The compiler sees only the methods whose modifiers are exactly `public static`
(`read_library_file` in mp3cc's `structures/block.c`), so a helper has to be package-private,
and a `public static synchronized` function would silently vanish from the library.
