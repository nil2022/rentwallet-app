<p align="center">
  <img src="docs/screenshots/app-icon.png" width="96" alt="RentFlow app icon">
</p>

<h1 align="center">RentFlow for Android</h1>

<p align="center">
  A rent app for tenants and landlords. Landlords manage properties, rooms, tenants and leases; tenants pay rent and keep receipts.<br>
  Built with Kotlin and Jetpack Compose, and designed to match the RentFlow web app’s phone layout.
</p>

> **Status:** sign-in, and the landlord’s properties, rooms, tenants and leases, run on the **live RentFlow API**, the same one the web app uses. Rent collection, payments and the wallet still show **sample numbers** until their APIs exist (see [What’s next](#whats-next)).

## Contents

- [Screens](#screens)
- [App flow](#app-flow)
- [What uses real data](#what-uses-real-data)
- [Design system](#design-system)
- [Tech stack](#tech-stack)
- [Project structure](#project-structure)
- [Build and run](#build-and-run)
- [What’s next](#whats-next)
- [Credits and licences](#credits-and-licences)

## Screens

The screenshots are rendered from the approved design mockups the app was built from, with example names and numbers. The numbers match the screen numbers used during design review.

### Welcome and sign-in

| 01 Welcome | 02 Login: Tenant | 02 Login: Landlord | 02 Login: One-time code |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/01-welcome.png" width="170" alt="Welcome screen"> | <img src="docs/screenshots/02-login-tenant.png" width="170" alt="Tenant login"> | <img src="docs/screenshots/02-login-landlord.png" width="170" alt="Landlord login"> | <img src="docs/screenshots/02-login-otp.png" width="170" alt="Login with one-time code"> |
| Pick Tenant or Landlord, or tap Sign In. | One login for both roles. Email and password, with “Remember for 30 days”. | The switch changes the headline, subtitle and the “Register here” link. | “Login with OTP instead” emails a 6-digit code. Resend unlocks after 60 seconds. |

A wrong password or code floats up from the bottom as a red note. Buttons show a spinner while a request runs.

### Forgot password and registration

| 14 Forgot password | 14 New password | 14 Done | 15 Register (landlord) | 15 Verify email |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/14-forgot-password-email.png" width="135" alt="Forgot password: email"> | <img src="docs/screenshots/14-forgot-password-reset.png" width="135" alt="Forgot password: code and new password"> | <img src="docs/screenshots/14-forgot-password-done.png" width="135" alt="Password reset successful"> | <img src="docs/screenshots/15-register.png" width="135" alt="Landlord registration"> | <img src="docs/screenshots/15-register-verify.png" width="135" alt="Verify email"> |
| Emails a reset code for the role chosen on login. | Code, new password and confirmation, with the password rule. | Confirms the reset and returns to login. | Only landlords sign up; they add their tenants later. Phone has a fixed +91. | Six code boxes. Once verified, the landlord is signed in. |

### Landlord

| 09 Overview | 10 Tenants | 11 Tenant Details | 12 Profile |
|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/09-landlord-overview.png" width="170" alt="Landlord overview"> | <img src="docs/screenshots/10-tenants.png" width="170" alt="Tenants"> | <img src="docs/screenshots/11-tenant-details.png" width="170" alt="Tenant details"> | <img src="docs/screenshots/12-landlord-profile.png" width="170" alt="Landlord profile"> |
| Portfolio health counts real properties, tenants, vacant rooms and leases ending soon. Add property and Add tenant are one tap away. | Search and filter tenants. Each card shows their property and room; tenants without a room get an “Assign room” link. | Contact details and the full lease. Call, edit, end the lease or delete the tenant. | Real owner details and counts. Edit profile and change password. |

### Properties and rooms

| 16 Properties | 17 Property Details | 18 Add or Edit Property |
|:---:|:---:|:---:|
| <img src="docs/screenshots/16-properties.png" width="220" alt="Properties"> | <img src="docs/screenshots/17-property-details.png" width="220" alt="Property details"> | <img src="docs/screenshots/18-property-form.png" width="220" alt="Add property"> |
| Rooms, let and vacant counts, then a card per property with its cover photo and how many rooms are let. | Swipeable photos, address, floors, parking and lift, and every room with its tenant or “Vacant”. | The web form’s fields in short sections, with chips, switches and up to 10 photos. Mistakes show under the field. |

| 19 Room: let | 19 Room: vacant | 20 Add or Edit Room |
|:---:|:---:|:---:|
| <img src="docs/screenshots/19-room-let.png" width="220" alt="Room with a tenant"> | <img src="docs/screenshots/19-room-vacant.png" width="220" alt="Vacant room"> | <img src="docs/screenshots/20-room-form.png" width="220" alt="Add room"> |
| The current tenant and lease terms. “End lease” frees the room. | A vacant room offers “Assign tenant”. | Room number, floor, occupancy, rent, deposit, facilities and photos. |

### Adding tenants and leases

| 21 Add tenant: details | 21 Add tenant: room | 21 Add tenant: lease | 22 Edit Tenant | 23 Assign Room |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/21-add-tenant-details.png" width="135" alt="Add tenant, step 1"> | <img src="docs/screenshots/21-add-tenant-room.png" width="135" alt="Add tenant, step 2"> | <img src="docs/screenshots/21-add-tenant-lease.png" width="135" alt="Add tenant, step 3"> | <img src="docs/screenshots/22-edit-tenant.png" width="135" alt="Edit tenant"> | <img src="docs/screenshots/23-assign-room.png" width="135" alt="Assign room"> |
| Name, mobile, sign-in email, initial password and an optional photo. | Pick a property, then one of its vacant rooms. The room can be skipped. | Move-in date, length, due day, rent, deposit and terms, with the web’s defaults. | Name, mobile, photo and whether the account is active. The email is locked. | From a vacant room: a tenant without a room, or a new one. |

### Profile settings

| 24 Edit Profile | 25 Change Password |
|:---:|:---:|
| <img src="docs/screenshots/24-edit-profile.png" width="220" alt="Edit profile"> | <img src="docs/screenshots/25-change-password.png" width="220" alt="Change password"> |
| Photo, name and mobile. The sign-in email is locked. | Current password, then the new one twice, with the password rule. |

### Confirmations and list states

| End lease | Delete | Loading | Empty | Can’t connect |
|:---:|:---:|:---:|:---:|:---:|
| <img src="docs/screenshots/sheet-end-lease.png" width="135" alt="End lease confirmation"> | <img src="docs/screenshots/sheet-delete-property.png" width="135" alt="Delete property confirmation"> | <img src="docs/screenshots/state-loading.png" width="135" alt="Loading"> | <img src="docs/screenshots/state-empty.png" width="135" alt="Empty list"> | <img src="docs/screenshots/state-error.png" width="135" alt="Error with retry"> |
| Says what happens before a lease ends. | Deletes ask first. A property, room or tenant with an active lease can’t be deleted until the lease ends. | Placeholder cards while a list loads. | A first-time landlord sees what to add first. | The reason and a “Try again” button. |

After every save or delete, a short message such as “Property saved” floats up from the bottom.

### Tenant

| 03 Home | 04 Pay Rent | 05 Payment Success |
|:---:|:---:|:---:|
| <img src="docs/screenshots/03-tenant-home.png" width="220" alt="Tenant home"> | <img src="docs/screenshots/04-pay-rent.png" width="220" alt="Pay rent"> | <img src="docs/screenshots/05-payment-success.png" width="220" alt="Payment successful"> |
| This month’s rent with a Pay rent button, quick actions, lease tiles, alerts and recent payments. | Rent details, payment method and summary. Confirm sits in a bottom sheet. | Confirmation with the transaction details. Links to the receipt. |

| 06 Payment History | 07 Receipt | 08 Profile |
|:---:|:---:|:---:|
| <img src="docs/screenshots/06-payment-history.png" width="220" alt="Payment history"> | <img src="docs/screenshots/07-receipt.png" width="220" alt="Receipt details"> | <img src="docs/screenshots/08-tenant-profile.png" width="220" alt="Tenant profile"> |
| Year summary, counts and every payment, filtered by All, Paid, Pending or Failed. | One receipt card with the transaction and wallet status. | Real name, email and mobile; rent and lease rows are sample data. Log out. |

The tenant’s rent screens use sample data until the tenant payment and lease APIs exist.

## App flow

```mermaid
flowchart TD
    W["01 Welcome"] -->|"Tenant or Landlord card"| L["02 Login"]
    W -->|"Sign In"| L
    L -->|"Forgot Password?"| F1["14 Forgot password"]
    F1 --> F2["14 Code and new password"] --> F3["14 Password reset"] --> L
    L -->|"Register here (landlord)"| R1["15 Register"] --> R2["15 Verify email"]
    L -->|"Tenant signs in"| TH
    L -->|"Landlord signs in"| LO
    R2 --> LO

    subgraph Tenant
        TH["03 Home"] -->|"Pay rent"| PR["04 Pay Rent"]
        PR -->|"Confirm Payment"| PS["05 Payment Success"]
        PS -->|"View receipt"| RC["07 Receipt"]
        TH -->|"Payments tab"| PH["06 Payment History"]
        PH -->|"Tap a payment"| RC
        TH -->|"Profile tab"| TP["08 Profile"]
    end

    subgraph Landlord
        LO["09 Overview"] -->|"Properties tab"| PL["16 Properties"]
        LO -->|"Tenants tab"| TS["10 Tenants"]
        LO -->|"Profile tab"| LP["12 Profile"]
        PL -->|"Tap a property"| PD["17 Property Details"]
        PL -->|"Add property"| PF["18 Property form"]
        PD -->|"Edit property"| PF
        PD -->|"Add room"| RF["20 Room form"]
        PD -->|"Tap a room"| RD["19 Room"]
        RD -->|"Edit room"| RF
        RD -->|"Assign tenant"| AR["23 Assign Room"]
        RD -->|"Tap the tenant"| TD["11 Tenant Details"]
        TS -->|"Tap a tenant"| TD
        TS -->|"Add tenant"| AT["21 Add tenant: details, room, lease"]
        LO -->|"Add tenant"| AT
        AR -->|"Add a new tenant"| AT
        TD -->|"Edit"| TF["22 Edit Tenant"]
        TD -->|"Assign a room"| AT
        LP -->|"Edit"| EP["24 Edit Profile"]
        LP -->|"Change password"| CP["25 Change Password"]
    end

    TP -->|"Log out"| W
    LP -->|"Log out"| W
```

How navigation works:

- **Bottom tabs.** Tenants have Home, Payments and Profile. Landlords have Overview, Properties, Tenants and Profile.
- **Top bar.** The menu button opens a side drawer with the same tabs and Log out. The bell lists alerts. The avatar opens Profile. Inner screens show a back arrow and the screen title.
- **Back button.** Screens form a stack, so Back returns to the screen you came from. A tab goes back to the home screen; on a home screen or Welcome, Back leaves the app.
- **Staying signed in.** With “Remember for 30 days”, the app opens signed in next time. Without it, the session ends when the app closes. If the server rejects the saved session, the app returns to login and says why.

## What uses real data

| Screen | From the API | Sample data |
|---|---|---|
| Login, OTP, Forgot password, Register | Everything | None |
| Landlord Overview | Property, tenant and vacant-room counts, leases ending soon | Collections card, needs-attention cards, payment ledger |
| Properties, property details, rooms | Everything: list, details, add, edit, delete, photos | None |
| Tenants, tenant details | Names, contacts, rooms, leases. Add, edit, delete, assign a room, end a lease | Paid / Due badges, rent status, tenant alerts, Send reminder |
| Landlord Profile | Name, email, mobile, photo, counts. Edit profile, change password | Collection tiles, wallet, bank account |
| Tenant Profile | Name, email, mobile | Rent and lease rows |
| Tenant Home, Pay Rent, Payments, Receipt | None yet | Everything |

Every call is one the web app already makes. The landlord endpoints are under `/api/v1/landlord` (property, room, tenant, lease, profile), photos go through `/api/v1/upload/presigned`, and sign-in uses `/api/v1/{landlord|tenant}/auth`.

## Design system

The app copies the phone design of the RentFlow web app (`rent-management-ui`), so both look the same.

**Colours.** One primary colour for both roles, from the web’s Material 3 tokens ([`Color.kt`](app/src/main/java/com/thebackendguy/myandroidtestapp/ui/theme/Color.kt)):

| Role | Hex | | Role | Hex |
|---|---|---|---|---|
| Primary | `#3525CD` | | Surface | `#F8F9FF` |
| Primary container | `#4F46E5` | | Surface container low | `#EFF4FF` |
| Primary fixed | `#E2DFFF` | | Surface container | `#E5EEFF` |
| Secondary (success) | `#006C4A` | | On surface | `#0B1C30` |
| Secondary container | `#82F5C1` | | On surface variant | `#464555` |
| Error | `#BA1A1A` | | Inverse surface (dark cards) | `#213145` |

**Type.** Plus Jakarta Sans, with the web’s phone type scale ([`Type.kt`](app/src/main/java/com/thebackendguy/myandroidtestapp/ui/theme/Type.kt)):

| Style | Size / line height | Weight | Used for |
|---|---|---|---|
| Metric | 32 / 38 | Bold | Big amounts (₹18,500) |
| Headline large | 28 / 36 | Bold | Login and auth headlines |
| Headline medium | 22 / 28 | SemiBold | App name next to the logo |
| Headline small | 18 / 24 | SemiBold | Section titles, top bar title |
| Body large / medium / small | 16 / 14 / 13 | Regular | Text |
| Label medium / small | 13 / 11 | SemiBold | Buttons, chips, captions |

**Icons.** Lucide v0.488, the same set and version as the web. They’re generated into [`Lucide.kt`](app/src/main/java/com/thebackendguy/myandroidtestapp/ui/icons/Lucide.kt) from the web project’s icon data.

**Logo.** The web Home page logo: a `#4F46E5` tile with a white Lucide “Home” icon. It is also the adaptive launcher icon ([`ic_launcher_foreground.xml`](app/src/main/res/drawable/ic_launcher_foreground.xml)).

**Components.** Cards with 12 dp corners and a soft shadow, dark summary cards, pill-shaped filters with a sliding thumb, quick-action chips that scroll sideways, and a bottom bar with the Material 3 pill indicator. Forms use choice chips, switches, steppers, dropdowns, a date picker and a photo grid, with the save button in a bar pinned to the bottom. Buttons shrink slightly when pressed instead of showing a ripple, as on the web.

## Tech stack

| | |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose (BOM 2024.09), Material 3 |
| Android | minSdk 28, targetSdk and compileSdk 36 |
| Build | Android Gradle Plugin 9.3, Gradle version catalog |
| Networking | Retrofit 3, OkHttp 4.12, kotlinx.serialization 1.9 |
| Images | Coil 3; photos are resized to 1600 px JPEGs before upload |
| Session | Token and account details in SharedPreferences (only with “Remember me”) |
| Navigation | A back stack of screens in Compose state, no navigation library |
| Sample data | [`DemoData.kt`](app/src/main/java/com/thebackendguy/myandroidtestapp/data/DemoData.kt) for rent, payments and wallet |

## Project structure

```
app/src/main/
├── java/com/thebackendguy/myandroidtestapp/
│   ├── MainActivity.kt           # Screens, back stack, drawer, session handling
│   ├── data/
│   │   ├── AuthRepository.kt     # Sign-in, OTP, password reset, registration, log out
│   │   ├── LandlordStore.kt      # Properties, rooms and tenants; every add, edit and delete
│   │   ├── Labels.kt             # Floors, dates, amounts and types as the web shows them
│   │   ├── DemoData.kt           # Sample rent, payments and wallet; ₹ formatting
│   │   ├── session/              # SessionStore: the signed-in account and token
│   │   └── remote/               # ApiConfig (server address), Retrofit APIs, DTOs, photo uploads
│   └── ui/
│       ├── theme/                # Color.kt, Type.kt, Theme.kt
│       ├── icons/                # Lucide.kt (generated), LucideIcon.kt
│       ├── components/           # Cards, buttons, fields, forms, states, floating messages, page frames
│       └── screens/
│           ├── Shell.kt          # Top bar and tabs per role, profile header, log out button
│           ├── auth/             # Welcome, Login, Forgot Password, Register
│           ├── tenant/           # Home, Pay Rent, Payment Success, History, Receipt, Profile
│           └── landlord/         # Overview, Properties, Rooms, Tenants, leases, Profile
├── res/
│   ├── font/plus_jakarta_sans.ttf
│   ├── drawable/                 # Launcher icon layers
│   ├── xml/network_security_config.xml
│   └── values/                   # App name, colours, window theme
└── assets/licenses/OFL-PlusJakartaSans.txt
```

## Build and run

1. Open the project in Android Studio.
2. Let Gradle sync, then run the **app** configuration on a device or emulator (Android 9 or newer).

From the command line:

```bash
./gradlew assembleDebug
```

On Windows use `gradlew.bat assembleDebug`. The APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Choosing the server

The server address is set in code, in [`ApiConfig.kt`](app/src/main/java/com/thebackendguy/myandroidtestapp/data/remote/ApiConfig.kt). Keep one line and comment out the other:

```kotlin
// const val BASE_URL = "http://localhost:4000"          // local backend
const val BASE_URL = "https://api.thebackendguy.click"   // live
```

For the local backend on a phone connected by USB, forward the port once per connection:

```bash
adb reverse tcp:4000 tcp:4000
```

On Wi-Fi, use your PC’s IP instead of `localhost`, and add that IP to [`network_security_config.xml`](app/src/main/res/xml/network_security_config.xml). Plain `http` is allowed only for the addresses listed there; the live server uses `https`.

## What’s next

The app uses the same backend as the web app (`rent-management`, Express and MongoDB, base path `/api/v1`).

1. ~~**Sign-in:** login with password or code, forgot password, registration, log out and remembering the session.~~ Done.
2. ~~**Landlord data:** properties, rooms, tenants and leases, with add, edit and delete.~~ Done.
3. **Tenant rent screens:** need backend work first. Payments and notifications must be limited to the signed-in user (today `/tenant/payment` and `/tenant/notification` return everyone’s), and tenants need an endpoint for their own lease.
4. **Landlord collections:** need landlord payment, monthly summary and reminder endpoints. They replace the sample numbers on Overview, Tenants and Profile.
5. **Payments gateway and wallet (optional):** today the backend records payments (cash, UPI or cheque) but moves no money, so there is no wallet or withdrawal yet.

Also worth fixing in the backend: deleting a property leaves its rooms behind. The app avoids the worst case by not deleting anything that still has an active lease.

## Credits and licences

- [Plus Jakarta Sans](https://github.com/tokotype/PlusJakartaSans), SIL Open Font License 1.1 (licence in `app/src/main/assets/licenses/`).
- [Lucide](https://lucide.dev) icons, ISC License.
- [Coil](https://coil-kt.github.io/coil/), [Retrofit](https://square.github.io/retrofit/) and [OkHttp](https://square.github.io/okhttp/), Apache License 2.0.
