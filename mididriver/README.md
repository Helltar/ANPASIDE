# MidiDriver 1.29

ANPASIDE vendors the unmodified official `MidiDriver-1.29.aar` release artifact as a local Maven dependency. It provides
MIDI and tone synthesis for the embedded J2ME runtime.

- Source: <https://github.com/billthefarmer/mididriver/tree/v1.29>
- Release: <https://github.com/billthefarmer/mididriver/releases/tag/v1.29>
- Artifact: `maven/com/github/billthefarmer/mididriver/1.29/mididriver-1.29.aar`
- SHA-256: `ed8a8f3744cffc2937ad1ab88323f8fceb40f2cfbff9f0e158390e8060604551`
- Licence: Apache License 2.0; see `LICENSE.txt`.

The release AAR was built with Android NDK r28b for API 21. Its `arm64-v8a` and `x86_64` native libraries use 16 KB ELF
`LOAD` alignment; the 32-bit libraries use 4 KB alignment. The AAR is kept locally because upstream states that version
1.29 cannot be built by JitPack.

When updating, download the official release AAR, verify its published digest, inspect every 64-bit native library with
`readelf -lW`, and update the version catalog, local Maven path, POM, checksum and this file together.
