# Daily Hisab Android

Daily Hisab-এর সম্পূর্ণ native Android application। এই repository-তে পুরোনো web/Capacitor code ব্যবহার করা হবে না।

## Technology

- Kotlin
- Jetpack Compose
- Material 3
- Clean, feature-based package structure
- Minimum Android 8.0 (API 26)
- Target Android API 36

## Development principles

- প্রতিটি feature আলাদা, অর্থপূর্ণ commit-এ তৈরি হবে।
- UI, domain logic এবং data access আলাদা রাখা হবে।
- টাকা সংক্রান্ত calculation test ছাড়া merge করা হবে না।
- কোনো secret, signing key বা local SDK path Git-এ commit করা হবে না।

## Planned modules

1. App foundation and design system
2. Navigation and dashboard
3. Expense and income management
4. Categories and budgets
5. Reports and calendar
6. Loans and reminders
7. Authentication and cloud sync
8. Settings, localization and personalization
9. Backup, security and release hardening

## Build

Install JDK 17 or newer, set `JAVA_HOME`, then run:

```powershell
.\gradlew.bat assembleDebug
```

