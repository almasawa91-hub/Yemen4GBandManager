# Yemen 4G Band Manager

Native Android application focused on Yemen 4G routers.

## Primary features

- Router detection and compatibility management.
- LTE band information and band selection when supported.
- Accurate data usage tracking from available router counters.
- Download and upload statistics.
- Daily and monthly usage reports.
- Local persistence using Room and SQLite.
- Arabic RTL interface.
- Secure local credential handling.

## Planned technologies

- Kotlin
- Android SDK
- Jetpack Compose
- Room / SQLite
- MVVM
- GitHub Actions

## Important

Supported features depend on the router model and its available
management interface. Actual usage statistics must come from
real counters; estimated values must not be presented as measured data.

Advanced Wi-Fi administration features are planned for a later phase.
