# Daily Hisab Android release checklist

## Automated gates

- `assembleDebug testDebugUnitTest connectedDebugAndroidTest`
- `lintRelease bundleRelease assembleRelease`
- Confirm Crashlytics mapping upload completes for the release variant.
- Confirm APK/AAB signatures with `apksigner verify --verbose` and `jarsigner -verify`.

## Physical-device acceptance

- Android 8/10/13/16: clean install, splash, launcher and round icon.
- Airplane mode: add/edit/delete an expense, relaunch, reconnect, and confirm one copy on website.
- Conflict: edit on website while Android is offline, add a different Android row, reconnect, and confirm both rows.
- Notifications: allow permission, set reminders 2 minutes ahead, verify daily/loan/budget notification and reboot reschedule.
- Reports: open exported PDF (long multiline description), Excel file, and PNG in independent viewer apps.
- Share PDF/Excel/PNG through Gmail/Drive/WhatsApp and verify recipient can open each attachment.
- Sign in/out, reinstall, restore cloud backup, profile image, and account deletion re-authentication behavior.

## Play Console

- Upload the signed `.aab`; keep the upload keystore outside Git and back it up securely.
- Add app icon, feature graphic, phone/tablet screenshots, short/full descriptions and support contact.
- Complete Data safety using `PLAY_STORE_DATA_SAFETY.md` and publish the public privacy-policy URL.
- Add internal testers, run pre-launch report, fix crashes/ANRs/accessibility blockers, then staged rollout.
