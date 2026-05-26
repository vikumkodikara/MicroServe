# MicroServe

Android app for local service requests — connects customers who need work done with service providers. The codebase combines an **admin dashboard**, a **user app** (from `main`), and a **request module** (Nethmina's feature work).

**Active branch:** `feature/nethmina-request-category-screens`  
**Repo:** [vikumkodikara/MicroServe](https://github.com/vikumkodikara/MicroServe)

---

## What is implemented so far

### App entry (user flow)

```
SplashActivity → LoginActivity → PostAdsActivity (Home tab)
```

- Launcher is **Splash** (animated loading from `main`)
- After login, user lands on **PostAdsActivity** with **Home** as the default tab
- Sign-up screen exists; successful sign-up returns to login (no auto-login yet)

### User bottom navigation

Shared layout: `view_home_bottom_nav.xml`  
Helper: `HomeBottomNavHelper.kt` (fixed center Home puck — from latest `main`)

| Tab     | Screen              | Notes                          |
|---------|---------------------|--------------------------------|
| Request | `RequestMainActivity` | My Service Requests hub      |
| Service | `PostServiceActivity` | Post a service (provider)    |
| Home    | `PostAdsActivity`   | Default tab after login        |
| Post    | `PostAdsActivity`   | Same screen, Post tab intent   |
| Profile | `EditProfileActivity` | User profile                   |

Nav is wired on **PostAdsActivity** and **RequestMainActivity**. Service and Profile screens do not yet include the bottom bar.

---

## Request module (Nethmina's work)

### 1. Request Main — `RequestMainActivity`

**Layout:** `activity_request_main.xml`

- Purple header: "My Service Requests"
- **3×3 category grid** (9 categories from `CategoryCatalog`)
- **My Requests** list — shows the user's own pending requests from `RequestStore`
- **Requests** button (purple pill) → opens **RequestServiceActivity** to create a **new** request
- Bottom nav: Request tab active

**Category images:** Figma PNGs as `nethmina_*.png` (including HVAC, Mechanic, Carpentry)

### 2. Create request form — `RequestServiceActivity`

**Layout:** `activity_request_service.xml`

- Form to submit a new service request
- Saves to `RequestStore` (SharedPreferences JSON)
- Supports pre-selected category via `EXTRA_CATEGORY` (from Edit on a row or category context)
- Also reachable from admin `Homepage` quick action

### 3. Category detail — `CategoryDetailActivity`

**Layout:** `activity_category_detail.xml`

- Opened by tapping a category on Request Main
- Header with category name + image
- Horizontal **category chips** to switch between all 9 categories
- Lists **published requests** in that category (from `RequestStore` + sample seed data)
- Tap a request card → **RequestDetailActivity**

### 4. Service request details — `RequestDetailActivity`

**Layout:** `activity_request_detail.xml`

- Requester name, age, avatar
- **Service Request Details** card: Service, Location, Job
- **Bid** button → toast ("Bid submitted")
- **Previous bids** list with **Purchase** buttons → toast (demo data from `BidSampleData.kt`)
- No real payment or bid persistence yet

---

## Data layer

### `RequestStore.kt`

Local persistence for user requests (SharedPreferences).

```kotlin
data class UserRequest(
    id, title, category, requesterName, contact, location, description, status
)
```

Key APIs:

- `addRequest()`, `getPendingRequests()`, `getPendingRequestsByCategory()`
- `getRequestById()`, `deleteRequest()`, `markRequestCompleted()`

### `CategoryCatalog.kt`

Single source of truth for 9 categories: id, display name, store keys, image drawable.

### `CategorySampleData.kt`

Seeds demo requests per category when a category list is empty.

### `BidSampleData.kt`

Demo ages, avatars, and bid rows for `RequestDetailActivity` UI.

---

## Admin module (from `main` + earlier work)

Still in the app, separate from the user login flow:

| Screen                 | Purpose                          |
|------------------------|----------------------------------|
| `Homepage`             | Admin dashboard (sidebar, stats)   |
| `RequestersActivity`   | List incoming requests           |
| `RequesterDetailsActivity` | Admin view of one request    |
| `ServicesActivity`     | Manage services                  |
| `UsersActivity`        | User management                  |
| `TransactionsActivity` | Transactions                     |
| `FeedbacksActivity`    | Feedback                         |
| `SettingsActivity`     | Settings                         |
| `AdminProfileActivity` | Admin profile                    |

Admin bottom nav: `AdminBottomNavHelper.kt` (3-tab: Home, Profile, Settings)

---

## User app screens (from `main`)

| Screen              | Purpose                    |
|---------------------|----------------------------|
| `PostAdsActivity`   | User home / post ads       |
| `PostServiceActivity` | Post a service           |
| `EditPostActivity`  | Edit a posted ad           |
| `EditProfileActivity` | Edit profile             |
| `DiscoverActivity`  | Discover feed              |
| `PersonalInfoActivity`, `SavedAddressActivity`, `WalletActivity`, etc. | Profile sub-screens |

---

## Navigation flow (user)

```
                    ┌─────────────────┐
                    │  SplashActivity │
                    └────────┬────────┘
                             ▼
                    ┌─────────────────┐
                    │  LoginActivity  │
                    └────────┬────────┘
                             ▼
              ┌──────────────────────────────┐
              │     PostAdsActivity          │
              │  (Home / Post via bottom nav)│
              └──────────────┬───────────────┘
                             │ Request tab
                             ▼
              ┌──────────────────────────────┐
              │    RequestMainActivity       │
              │  • category grid             │
              │  • My Requests list          │
              │  • Requests → new form       │
              └──────┬───────────────┬───────┘
                     │               │
         tap category│               │ tap request row (My Requests) → Edit form
                     ▼               │
         ┌───────────────────┐      │
         │CategoryDetailActivity│   │
         └─────────┬─────────┘      │
                   │ tap published  │
                   │ request card   │
                   ▼                ▼
         ┌──────────────────┐  ┌─────────────────────┐
         │RequestDetailActivity│ │RequestServiceActivity│
         │ (bids, job details) │ │ (create/edit request)│
         └──────────────────┘  └─────────────────────┘
```

---

## Key files (request module)

| File | Role |
|------|------|
| `RequestMainActivity.kt` | Request hub screen |
| `CategoryDetailActivity.kt` | Category-filtered request list |
| `RequestDetailActivity.kt` | Single request + bids UI |
| `RequestServiceActivity.kt` | Create request form |
| `RequestStore.kt` | Local request persistence |
| `CategoryCatalog.kt` | 9 categories definition |
| `CategorySampleData.kt` | Demo seed data |
| `BidSampleData.kt` | Demo bids for detail screen |
| `HomeBottomNavHelper.kt` | User 5-tab bottom navigation |
| `view_home_bottom_nav.xml` | Bottom nav layout (center Home puck) |

---

## Merge history (important)

1. **`origin/main` merged** — brought Splash, Login, SignUp, fixed homepage nav, profile screens, discover, post ads layouts.
2. **Old bubble nav removed** — replaced `UserBottomNavHelper` / `view_user_bottom_nav` with main's `view_home_bottom_nav`.
3. **User routes wired** — Request → `RequestMainActivity`, not admin `RequestersActivity`.
4. **Category PNGs** — HVAC, Mechanic, Carpentry use `nethmina_hvac.png`, `nethmina_mechanic.png`, `nethmina_carpentry.png`.

---

## Not done yet / known gaps

- **RequestServiceActivity UI** — still uses older admin-style layout; Figma redesign not applied
- **Bid / Purchase** — UI + toasts only; no backend or local bid storage
- **Bottom nav on Service & Profile** — those screens open without the shared nav bar
- **Real auth** — login accepts any username/password
- **Backend** — all data is local (`RequestStore` SharedPreferences)
- **Admin vs user** — both flows exist; admin `Homepage` is not the launcher but still reachable from code

---

## Build and run

```bash
.\gradlew assembleDebug
```

Open in Android Studio, run on emulator/device. Flow: Splash → Login (any credentials) → Post Ads home → Request tab for the request module.

---

## Assets

- **Figma exports:** `app/src/main/res/drawable/Nethmina/` (original) and copies as `nethmina_*.png`
- **Nav PNGs:** `rectanglenav.png`, `navhome.png`, etc.
- **Placeholder vectors:** `img_hvac.xml`, `img_mechanic.xml` — replaced in catalog by PNGs; files may still exist
- See also: `RESOURCES_TO_ADD.md` for fonts/images still listed as optional

---

## Contributors / branches

| Branch | Focus |
|--------|--------|
| `main` | User app, splash, login, homepage, post/discover/profile |
| `feature/nethmina-request-category-screens` | Request Main, categories, detail screens, nav integration |
| `develop` | Integration branch for the team |

---

*Last updated: reflects work through Service Request Details screen, main merge, home nav fix, and Requests button → create form.*
