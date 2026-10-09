# Play Store privacy and Data safety draft

This is a preparation worksheet, not legal advice. Revalidate every answer immediately before publishing.

## Data the app handles

| Data | Purpose | Storage/transfer | User control |
|---|---|---|---|
| Name, email, Firebase UID | Account management and private sync | Firebase Authentication/Realtime Database | Account deletion in Security |
| Profile image | Profile customization | Firebase Storage for signed-in users | User can replace; delete with account-support process |
| Expenses, income, categories, budgets, loans, savings | Core app functionality and backup | Device Room database; Firebase Realtime Database after sign-in | Add/edit/delete; backup/restore |
| Crash diagnostics, device/app metadata | Reliability and crash investigation | Firebase Crashlytics | Disclose under diagnostics |
| Analytics events/device identifiers | Product analytics and Crashlytics breadcrumbs | Firebase Analytics | Disclose under analytics |
| Notification settings | Reminders | Local device preferences | Profile settings |

## Suggested Play declarations

- Data is encrypted in transit: **Yes**.
- Account deletion request: **Available in app**; Firebase may require recent re-authentication.
- Data sharing: normally **No sale** and **No advertising use**; Firebase acts as service provider.
- Data collection: declare Personal info, Financial info, App activity, App info/performance, Device or other IDs as applicable.
- Optional data: profile image and optional transaction descriptions.
- Required data: authentication identifiers only for cloud-account features; guest/local use should be described separately.

## Public privacy policy must include

- Controller/developer identity and contact: `mirza.galib.palash@gmail.com`, `mirzagalib.xyz`.
- Exact Firebase services, retention, deletion, security, children policy, user rights and policy-change date.
- Explain local-only guest mode versus signed-in cloud synchronization.
- State that sensitive financial entries are user-provided and are not sold.
