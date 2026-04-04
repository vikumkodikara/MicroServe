# Admin Panel UI - Current Status (April 2026)

## Scope
This document summarizes the Admin Panel UI – all screens, data connections, and design conventions.

## Design System

### Theme Colors (from `admin_colors.xml`)
| Token | Color | Usage |
|-------|-------|-------|
| `admin_primary_purple` | #6C5CE7 | Primary accent, quick action icons |
| `admin_purple_dark` | #5F3DC4 | Headers, buttons, status bar |
| `admin_purple_light` | #A29BFE | Secondary accent, empty stars |
| `admin_stat_blue` | #5F5CE6 | Requests stat card |
| `admin_stat_cyan` | #66BB6A | Feedbacks stat card |
| `admin_stat_orange` | #F39C12 | Revenue stat card, star ratings |
| `admin_text_primary` | #2D3436 | Primary text |
| `admin_text_secondary` | #636E72 | Subtitles, descriptions |
| `admin_bg_light` | #F8F9FA | Screen backgrounds |

### Component Standards
- **Sub-page headers**: 160dp height, `sub_header_gradient`, back arrow ‹, title 28sp bold, subtitle 13sp alpha 0.8
- **List item cards**: white, 16dp corner radius, 4dp elevation, `selectableItemBackground` ripple
- **Detail cards**: white, 20dp corner radius, 5dp elevation
- **Buttons**: `block_button_bg` (purple gradient, 32dp radius), `delete_user_button_bg` (red, 32dp radius)
- **Tab buttons**: `tab_active_bg`/`tab_inactive_bg`, 24dp radius pills
- **Status pills**: `pending_tag_bg` (green), `active_tag_bg` (green), rounded 20dp
- **Empty states**: Emoji icon (60sp) + bold title (18sp) + description (14sp)

---

## Implemented Admin Screens

### 1. Admin Dashboard (Home)
- File: `app/src/main/res/layout/activity_admin_dashboard.xml`
- Activity: `app/src/main/java/com/example/microserve/Homepage.kt`
- Includes:
   - Gradient header (200dp)
   - Metrics cards: Requests, Completed, Feedbacks, Revenue
   - Quick actions: Requests, Services, Transactions, Feedbacks, Users
   - Bottom navigation (Home/Profile/Settings)
- Current data source (local DB layer):
   - Requests -> `RequestStore.getPendingRequests(...)`
   - Completed -> `TransactionStore.getSuccessCount(...)`
   - Feedbacks -> `FeedbackStore.getFeedbackCount(...)`
   - Revenue -> `TransactionStore.getTotalSuccessAmount(...)`

### 2. Requesters Management
- List: `app/src/main/res/layout/activity_requesters.xml`
- Item: `app/src/main/res/layout/item_requester.xml`
- Details: `app/src/main/res/layout/activity_requester_details.xml`
- Activities: `RequestersActivity.kt`, `RequesterDetailsActivity.kt`
- Store: `RequestStore.kt`
- Behavior:
   - Shows all pending service requests
   - Card-based list with category tag + status pill
   - Detail view with title, category, requester, location, status, description
   - Contact info card with purple-accented phone number
   - Block/Delete user actions
   - Data persists in local request store

### 3. Services Management
- List: `app/src/main/res/layout/activity_services.xml`
- Item: `app/src/main/res/layout/item_service_card.xml`
- Activity: `ServicesActivity.kt`
- Store: `ServiceStore.kt`
- Behavior:
   - Tab filtering: Current Services / Pending Services
   - Service cards with title, category tag, location, status badge
   - Block/Delete action buttons per service card
   - Empty state with description

### 4. Transactions Management
- List: `app/src/main/res/layout/activity_transactions.xml`
- Item: `app/src/main/res/layout/item_transaction.xml`
- Details: `app/src/main/res/layout/activity_transaction_details.xml`
- Dialog: `app/src/main/res/layout/dialog_transaction_success.xml`
- Activities: `TransactionsActivity.kt`, `TransactionDetailsActivity.kt`
- Store: `TransactionStore.kt`
- Behavior:
   - Tab filtering: Pending / Success
   - Transaction cards with provider name, amount, date, txn ID, status
   - Detail view with row-based layout, transfer button for pending
   - Success dialog with checkmark icon
   - Credits user cash points on successful transfer

### 5. Users Management
- List: `app/src/main/res/layout/activity_users.xml`
- Item: `app/src/main/res/layout/item_user_card.xml`
- Details: `app/src/main/res/layout/activity_user_details.xml`
- Activities: `UsersActivity.kt`, `UserDetailsActivity.kt`
- Store: `UserStore.kt`
- Behavior:
   - Tab filtering: All / Providers / Requesters / Active / Inactive
   - User cards with avatar circle, name, email, status badge
   - Detail view with profile card, email, cash points, phone, status
   - Ban/Delete user actions

### 6. Feedback Management
- File: `app/src/main/res/layout/activity_feedbacks.xml`
- Item file: `app/src/main/res/layout/item_feedback.xml`
- Activity: `app/src/main/java/com/example/microserve/FeedbacksActivity.kt`
- Store: `app/src/main/java/com/example/microserve/FeedbackStore.kt`
- Behavior:
   - Shows all feedback records in card format
   - Orange star rating rendering (filled/empty)
   - Delete feedback action per card
   - Data persists in local store and updates dashboard feedback count

### 7. Settings
- File: `app/src/main/res/layout/activity_settings.xml`
- Activity: `app/src/main/java/com/example/microserve/SettingsActivity.kt`
- Preferences: `app/src/main/java/com/example/microserve/AppPreferences.kt`
- Behavior:
   - Notifications toggle (persisted)
   - Dark mode toggle (persisted + applied immediately)
   - Language selection (persisted)
   - Help / Privacy / Terms actions
   - Profile card opens Admin Profile screen

### 8. Admin Profile (Editable)
- File: `app/src/main/res/layout/activity_admin_profile.xml`
- Activity: `app/src/main/java/com/example/microserve/AdminProfileActivity.kt`
- Input style: `app/src/main/res/drawable/admin_profile_input_bg.xml`
- Store integration: `app/src/main/java/com/example/microserve/UserStore.kt`
- Behavior:
   - Editable username and email
   - Current password validation required
   - Optional password change with confirm check
   - Save writes to local user database and updates other admin UI views

### 9. Post Service (Provider Form)
- File: `app/src/main/res/layout/activity_post_add.xml`
- Activity: `PostAddActivity.kt`
- Behavior:
   - Purple header with subtitle
   - Form fields: category, provider name, image, location, contact
   - Gradient submit button

### 10. Request Service (Requester Form)
- File: `app/src/main/res/layout/activity_request_service.xml`
- Activity: `RequestServiceActivity.kt`
- Behavior:
   - Purple header with subtitle
   - Form fields: name, title, category, contact, location, description
   - Gradient submit button

### 11. Edit Post
- File: `app/src/main/res/layout/activity_edit_post.xml`
- Activity: `EditPostActivity.kt`
- Behavior:
   - Gradient header matching admin sub-page style
   - Card-wrapped form with delete button
   - Edit fields: category, provider name, image, location, contact
   - Save changes button

## Bottom Navigation Status

- Bottom nav is active and wired on all admin pages.
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
- `TransactionStore` -> completed count, revenue metrics, transaction screens
- `FeedbackStore` -> feedback list and feedback count
- `UserStore` -> admin profile read/write and user-related screens
- `ServiceStore` -> service listings and management

## Current Functional Summary

- Dashboard metrics are live from local stores.
- All sub-screens (Requesters, Services, Transactions, Users, Feedbacks) use consistent admin design system.
- All list cards have touch ripple effects.
- All screens use theme color references instead of hardcoded hex values.
- All headers are 160dp with gradient, back arrow, title + subtitle.
- All empty states have emoji icon + title + description text.
- Settings screen actions are working.
- Dark mode is functional and persistent.
- Profile tab opens editable Admin Account Setting and saves to local DB.
- Post/Request/Edit forms use consistent purple theme with gradient buttons.
- Transaction success dialog has proper styling with checkmark icon.

## Manifest Entries (Admin-related)

- `Homepage`
- `FeedbacksActivity`
- `SettingsActivity`
- `AdminProfileActivity`
- `RequestersActivity`
- `RequesterDetailsActivity`
- `ServicesActivity`
- `TransactionsActivity`
- `TransactionDetailsActivity`
- `UsersActivity`
- `UserDetailsActivity`
- `PostAddActivity`
- `EditPostActivity`
- `RequestServiceActivity`

## Build Validation

- Latest verification: `:app:compileDebugKotlin` -> **BUILD SUCCESSFUL**.
