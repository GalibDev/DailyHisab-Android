# Architecture

The app will use a single-activity Jetpack Compose architecture.

## Package direction

- `feature`: screens and feature-specific state
- `domain`: business models and use cases
- `data`: local database, Firebase and repository implementations
- `core`: reusable UI, utilities and platform services
- `ui`: app navigation and the shared design system

Dependencies must point inward: UI depends on domain contracts; data implements those contracts. Feature code must not call Firebase or database APIs directly.

