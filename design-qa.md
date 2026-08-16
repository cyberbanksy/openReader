# Design QA

- source visual truth: `/Users/macstudio/Downloads/5cfa8162cc746c5b70e78181_Apple Music_main.png`
- reader source truth: `/Users/macstudio/Downloads/ScreenRecording_08-12-2026 10-07-25_1.MP4`
- extracted reader frames: `/Volumes/Files/Projects/openReader/design-reference/reader-recording-2026-08-12/keyframes`
- implementation screenshots: `/Volumes/Files/Projects/openReader/design-reference/follow-along-2026-08-13`
- viewport: Lenovo TB132FU landscape, `2560 x 1536` physical pixels at `320 dpi`
- normalized comparison: source and native implementation rendered side-by-side at `1280 x 768` each
- tested state: Alice EPUB paired with the real Alice audiobook; playback paused after verified play/pause/resume

## Comparison evidence

- follow-along full view: `/Volumes/Files/Projects/openReader/design-reference/follow-along-2026-08-13/follow-along-comparison-final.png`
- Apple Books-like clean reader: `/Volumes/Files/Projects/openReader/design-reference/follow-along-2026-08-13/reader-comparison.png`
- live paused follow-along screen: `/Volumes/Files/Projects/openReader/design-reference/follow-along-2026-08-13/lenovo-follow-along-final.png`

## Findings

- No actionable P0/P1/P2 visual issues remain.
- Follow-along now matches the Apple Music hierarchy: real cover-derived blurred atmosphere, compact cover/title/creator metadata, one oversized high-contrast passage, and subdued blurred passages before and after it.
- The reading surface matches the supplied Apple Books recording more closely: the persistent floating audiobook card is gone, page content gets the full viewport, controls auto-hide, the top bar is quiet and compact, and reading progress is a thin bottom rule.
- Appearance controls are a compact bottom sheet instead of a centered generic dialog. Page, Spread, and Scroll remain available alongside Paper, Calm, and Night themes.
- Highlighting remains available by long-press with amber, blue, and rose choices.
- Transport controls remain functional and stable: scrub, ten seconds back, pause/resume, and ten seconds forward.
- Real source assets are used. The Alice cover is loaded from Audiobookshelf and drives both the sharp thumbnail and blurred background.
- Store copy remains intentionally excluded; the library continues to use ARR, Audiobookshelf, public-domain, and public-library source language.

## Verification

- `testDebugUnitTest`: passed, including passage-window and Readium JavaScript-result parsing coverage.
- `lintDebug`: passed.
- `assembleDebug`: passed.
- Real Lenovo/Audiobookshelf integration: Alice streamed, advanced, paused without advancing, resumed, advanced again, and finished paused.
- Accessibility tree verified explicit actions for return to reading, back 10 seconds, pause/resume, and forward 10 seconds.
- Wireless ADB target verified at `192.168.1.129:5555`; final APK installed and the paused follow-along screen left open.

## Open question

- Paragraph timing is estimated from audiobook progress against EPUB reading order because the current Alice audiobook has no transcript timestamps. Word-accurate karaoke coloring will require timed transcript or word-alignment data.

final result: passed
