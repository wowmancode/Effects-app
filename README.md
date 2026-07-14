# effect-app

`effect-app` is a GPLv3 free/open-source Android video editor for effects-stacking edits: many simultaneous, time-ranged effects across a hard-cut sequence of video clips.

## Project status

This is a personal hobby project, maintained on a best-effort basis only. There is no warranty and no guaranteed support. Feature requests are only implemented if they are easy, interesting, or wanted by the maintainer. Pull requests are welcome, but review and merge are not guaranteed.

## Tech stack

- Kotlin with Gradle Kotlin DSL and a version catalog.
- Jetpack Compose, Compose Navigation, and stock Material 3 only.
- AndroidX Media3 Transformer and Effect/GlEffect APIs for processing/export.
- ExoPlayer for preview.
- kotlinx.serialization JSON save/load model.
- No `ffmpeg-kit` or FFmpeg wrapper dependency.

## Modules

- `app`: single-Activity Compose UI.
- `core-model`: project, clip, transform, and timeline segment data classes plus JSON serialization.
- `core-effects`: generic `LecEffect` contract and `EffectRegistry` wiring point.
- `core-pipeline`: Media3 Transformer composition/export wrapper.

## Roadmap phases

1. Scaffold modules, CI, GPLv3 license, and documentation.
2. Core pipeline with zero effects: import, arrange multiple clips, hard-cut concatenated export.
3. Generic effect registry, timeline UI, and exactly three end-to-end effects: hue rotate, pitch change, wave warp.
4. Remaining MVP effects.
5. Save/load JSON.
6. Polish: segment resize, error handling, empty states, and unit tests.

## License

App code is licensed under GPLv3. Dependencies may use OSI-approved compatible licenses such as Apache-2.0, MIT, or BSD. The combined distributed app remains fully copyleft under GPLv3 where required.
