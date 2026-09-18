# a midlet lives in a dex of its own and resolves the runtime by name, so r8 sees no reference
# to any of it. everything a midlet can reach has to stay, under its own name, in whatever
# application carries this runtime; the rest - androidx, material, rxjava - is only reachable
# from the runtime itself and may be shrunk away freely

-keep class javax.microedition.** { *; }
-keep class javax.wireless.** { *; }
-keep class com.jblend.** { *; }
-keep class com.kddi.** { *; }
-keep class com.motorola.** { *; }
-keep class com.nokia.** { *; }
-keep class com.samsung.** { *; }
-keep class com.siemens.** { *; }
-keep class com.sonyericsson.** { *; }
-keep class com.sprintpcs.** { *; }
-keep class com.sun.** { *; }
-keep class com.vodafone.** { *; }
-keep class mmpp.** { *; }
-keep class org.microemu.** { *; }

# gson maps the emulator profile onto these field names
-keep class ru.playsoftware.j2meloader.config.ProfileModel { *; }
-keep class ru.playsoftware.j2meloader.config.ShaderInfo { *; }
