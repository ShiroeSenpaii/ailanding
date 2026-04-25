# Glyphbound APK

Glyphbound is a mobile hidden-phrase roguelike MVP for Android.

## V2 changes

- Cleaner portrait layout.
- QWERTY-style keyboard docked near the bottom.
- Less dead vertical space during gameplay.
- Card-grid relic shop instead of long text boxes.
- Buy multiple relics in one shop.
- Reroll shop offers with scaling cost.
- Leave shop while banking leftover Ink.
- Phrase answer reveal pause after clears and failures.
- Rebalanced early scoring so round 1 is less inflated.
- Safer transition locking to reduce crashy double-tap state issues.

## Core loop

- Choose a category route.
- Clear 3 cursed hidden phrases per round.
- Guess letters to earn Ink and build multiplier.
- Use Charge Guess for risky double Ink.
- Avoid filling the cursed effigy HP indicator.
- Reach the round Ink target to survive.
- Spend excess Ink in the relic shop.
- Choose the next category and keep scaling.
- Every 3rd round becomes a boss page.

## Build APK on GitHub

1. Open this repo on GitHub.
2. Go to **Actions**.
3. Run **Build Glyphbound APK** manually, or push a change.
4. Open the completed run.
5. Download the `Glyphbound-debug-apk` artifact.
6. Install `app-debug.apk` on your Android phone.

This is a debug APK for testing, not a Play Store release build.
