# WayCairn

WayCairn is a native Android **friction layer and daily habit tracker**. When you open an app you've chosen to gate, WayCairn slides a brief full-screen overlay in front of you showing the habits you still haven't finished today — a small, deliberate pause before you scroll. Your habits live in the app itself, with a calendar, streaks, and a per-habit "how built is this?" strength arc.

![WayCairn](app-screenshot.png)

## What it does

- **Gate apps you choose.** Pick apps to intercept. Opening one shows a 5-second friction screen with your incomplete habits before it lets you through.
- **Two trigger modes.** Show the overlay the moment an app opens (`ON_OPEN`), or after a delay you set (`AFTER_DELAY`).
- **Habits are a log, not a checkbox.** "Done today" means a completion was recorded today in your local timezone — so day rollovers and travel are handled correctly.
- **Timed reminders.** Habits with a deadline get an exact-alarm reminder 30 minutes before and a "you missed it" nudge at the deadline.
- **Unlock nudges.** Every third phone unlock, if you still have untimed habits open, WayCairn posts a gentle reminder.
- **Progress you can see.** A calendar heatmap, a global "perfect day" streak, and a strength arc that grows from *Begin → Form → Harden → Built* at 21 days.
- **Deliberate completion.** You can't complete a habit from the overlay — tapping a habit opens it in the app. Finishing something is a conscious act.

## Tech stack

| Layer | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Database | Room (via KSP) |
| Preferences | DataStore |
| Async | Coroutines + Flow |
| Navigation | navigation-compose |
| Interception | AccessibilityService + WindowManager overlay |
| Notifications | AlarmManager (exact) + BroadcastReceivers |
| Min / Target SDK | 26 / 36 |

## Permissions

WayCairn is a fully usable habit tracker on its own. The interceptor just needs four permissions, which a first-run onboarding walks you through in order:

1. **Notifications** — reminder and missed-deadline nudges.
2. **Display over other apps** — draws the friction overlay.
3. **Alarms & reminders** — fires timed reminders at the exact minute.
4. **Accessibility** — detects when a gated app opens.

> Sideloading on Android 13+? The Accessibility toggle may show as a "Restricted setting" at first. Enable it via **Settings → Apps → WayCairn → ⋮ → Allow restricted settings**, then turn the service on under **Settings → Accessibility → Installed services → WayCairn**.

## Build & run

With a device plugged in and USB debugging on:

```bash
./gradlew installDebug        # compile + install
adb logcat --pid=$(adb shell pidof -s com.waycairn)   # focused logs
```

See [`BUILD_PLAN.md`](BUILD_PLAN.md) for the full spec and phased build history.
