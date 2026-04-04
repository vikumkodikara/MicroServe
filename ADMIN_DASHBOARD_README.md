# Admin Panel UI - Current Status (April 2026)

## Scope
This document summarizes only the current Admin Panel UI and its connected local data behavior.

## Implemented Admin Screens

### 1. Admin Dashboard (Home)
- File: `app/src/main/res/layout/activity_admin_dashboard.xml`
- Activity: `app/src/main/java/com/example/microserve/Homepage.kt`
- Includes:
   - Gradient header
   - Metrics cards: Requests, Completed, Feedbacks, Revenue
   - Quick actions: Requests, Services, Transactions, Feedbacks, Users
   - Bottom navigation (Home/Profile/Settings)
- Current data source (local DB layer):
   - Requests -> `RequestStore.getPendingRequests(...)`
   - Completed -> `TransactionStore.getSuccessCount(...)`
   - Feedbacks -> `FeedbackStore.getFeedbackCount(...)`
   - Revenue -> `TransactionStore.getTotalSuccessAmount(...)`

### 2. Feedback Management
- File: `app/src/main/res/layout/activity_feedbacks.xml`
- Item file: `app/src/main/res/layout/item_feedback.xml`
- Activity: `app/src/main/java/com/example/microserve/FeedbacksActivity.kt`
- Store: `app/src/main/java/com/example/microserve/FeedbackStore.kt`
- Behavior:
   - Shows all feedback records
   - Star rating rendering
   - Delete feedback action
   - Data persists in local store and updates dashboard feedback count

### 3. Settings
- File: `app/src/main/res/layout/activity_settings.xml`
- Activity: `app/src/main/java/com/example/microserve/SettingsActivity.kt`
- Preferences: `app/src/main/java/com/example/microserve/AppPreferences.kt`
- Behavior:
   - Notifications toggle (persisted)
   - Dark mode toggle (persisted + applied immediately)
   - Language selection (persisted)
   - Help / Privacy / Terms actions
   - Profile card opens Admin Profile screen

### 4. Admin Profile (Editable)
- File: `app/src/main/res/layout/activity_admin_profile.xml`
- Activity: `app/src/main/java/com/example/microserve/AdminProfileActivity.kt`
- Input style: `app/src/main/res/drawable/admin_profile_input_bg.xml`
- Store integration: `app/src/main/java/com/example/microserve/UserStore.kt`
- Behavior:
   - Editable username and email
   - Current password validation required
   - Optional password change with confirm check
   - Save writes to local user database and updates other admin UI views

## Bottom Navigation Status

- Bottom nav is active and wired on admin pages.
- Routes:
   - Home -> `Homepage`
   - Profile -> `AdminProfileActivity`
   - Settings -> `SettingsActivity`

## Theme/Dark Mode Status

- App-level dark mode is applied using:
   - `app/src/main/java/com/example/microserve/MicroServeApp.kt`
   - Manifest registration in `app/src/main/AndroidManifest.xml`
- Toggle state is saved in `AppPreferences` and persists after app restart.

## Local Database Layer Used by Admin UI

- `RequestStore` -> request metrics and request management screens
- `TransactionStore` -> completed count and revenue metrics
- `FeedbackStore` -> feedback list and feedback count
- `UserStore` -> admin profile read/write and user-related screens

## Current Functional Summary

- Dashboard metrics are live from local stores.
- Feedback screen is connected and manageable.
- Settings screen actions are working.
- Dark mode is functional and persistent.
- Profile tab opens editable Admin Account Setting and saves to local DB.

## Manifest Entries (Admin-related)

- `Homepage`
- `FeedbacksActivity`
- `SettingsActivity`
- `AdminProfileActivity`

## Build Validation

- Latest verification: `:app:compileDebugKotlin` -> **BUILD SUCCESSFUL**.
