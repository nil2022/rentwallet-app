# Architecture Gap Analysis: RentWallet → Feature-First Architecture

**Reviewer:** Principal Android Architect  
**Date:** 25 July 2026  
**Reference Architecture:** Feature-first multi-layer (core/ → data/ → di/ → feature/*/)

---

## 1. Current Folder Structure (Complete Inventory)

```
MyAndroidTestApp/
├── build.gradle                          # Root build file (plugin declarations only)
├── settings.gradle                       # Module includes (only `:app`)
├── gradle.properties                     # AndroidX, nonTransitiveRClass
├── local.properties                      # SDK path (local, gitignored)
├── gradlew / gradlew.bat                 # Gradle wrapper
│
├── gradle/
│   └── libs.versions.toml                # Version catalog (14 libraries, 3 plugins)
│
├── .gradle/                              # Build cache (gitignored)
├── .idea/                                # IDE config (gitignored)
├── build/                                # Build output (gitignored)
│
└── app/                                  # Single module
    ├── build.gradle                      # App build config
    ├── proguard-rules.pro                # Empty (no rules, all commented)
    │
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml       # Single activity, no permissions
        │   │
        │   ├── java/com/thebackendguy/myandroidtestapp/
        │   │   ├── MainActivity.kt       # 3,259 lines — ALL production code
        │   │   │
        │   │   └── ui/
        │   │       └── theme/
        │   │           ├── Color.kt      # 30 lines — color palette
        │   │           ├── Theme.kt      # 90 lines — Material3 theme
        │   │           └── Type.kt       # 6 lines — default Typography
        │   │
        │   └── res/
        │       ├── drawable/
        │       │   ├── ic_launcher_background.xml
        │       │   └── ic_launcher_foreground.xml
        │       ├── mipmap-*/              # Launcher icons (8 density buckets)
        │       ├── mipmap-anydpi/         # Adaptive icon XML
        │       ├── values/
        │       │   ├── colors.xml         # Only launcher icon color
        │       │   ├── strings.xml        # Only app_name = "RentWallet"
        │       │   └── themes.xml         # Bridge theme (Material Light NoActionBar)
        │       └── xml/
        │           ├── backup_rules.xml
        │           └── data_extraction_rules.xml
        │
        ├── test/java/com/thebackendguy/myandroidtestapp/
        │   └── ExampleUnitTest.kt        # Placeholder (2 + 2 == 4)
        │
        └── androidTest/java/com/thebackendguy/myandroidtestapp/
            └── ExampleInstrumentedTest.kt # Placeholder (package name check)
```

**Summary:**
- **1 module** (`:app`)
- **1 package** with production code (`com.thebackendguy.myandroidtestapp`)
- **4 Kotlin source files** total (1 main + 3 theme)
- **1 production code file** (`MainActivity.kt`, 3,259 lines)
- **Zero** subpackages beyond `ui/theme/`
- **Zero** feature separation
- **Zero** architectural layers

---

## 2. Comparison Against Target Structure

### Target: `com.example.myapp/`

| Target Layer | Current Status | Assessment |
|-------------|----------------|------------|
| `core/database/` | **Missing** | No Room, no SQLite, no database-related code exists anywhere. |
| `core/network/` | **Missing** | No Retrofit, OkHttp, Ktor, serialization, API interfaces, or network config. |
| `core/designsystem/` | **Partially Present** | `ui/theme/` exists with Color + Theme + Type, but lacks: shapes, spacing, reusable component atoms (buttons, cards, pills are inline), icon system, and `Modifier` extensions. |
| `core/model/` | **Missing** | Data types exist but are `private` inside `MainActivity.kt`. They are not extracted into a shared model layer. |
| `core/util/` | **Missing** | Helper functions exist but are `private` file-scoped functions in `MainActivity.kt`. |
| `data/repository/` | **Missing** | No repository classes, interfaces, or abstractions. |
| `data/source/` | **Missing** | No remote or local data source implementations. |
| `di/` | **Missing** | No DI setup, no Application class, no module definitions. |
| `feature/auth/` | **Partially Present** | `LoginScreen`, `WelcomeScreen`, `UserRole` exist but are mixed into the monolith. No `data/` or `domain/` sub-layers. |
| `feature/tenant/` | **Partially Present** | `TenantDashboardScreen` and sub-composables exist in the monolith. |
| `feature/landlord/` | **Partially Present** | `LandlordDashboardScreen`, `LandlordTenantsScreen`, `PropertyDetailsScreen` exist. |
| `feature/payments/` | **Partially Present** | `PayRentScreen`, `PaymentSuccessScreen`, `PaymentHistoryScreen`, `ReceiptDetailsScreen` exist. |
| `feature/profile/` | **Partially Present** | `TenantProfileScreen`, `LandlordProfileScreen` exist. |
| `MainActivity.kt` | **Present** | Exists at the correct location (app entry point), but currently contains all code rather than just hosting the navigation graph. |

**Gap Summary:**
- **6 layers completely missing**: `core/database/`, `core/network/`, `core/model/`, `core/util/`, `data/repository/`, `data/source/`, `di/`
- **6 features partially present** but not extracted into feature modules
- **1 layer partially present** (`core/designsystem/`)
- **0 layers fully present** as defined by the target architecture

---

## 3. Core Layer Analysis

### 3.1 `core/database/`

**Status: Missing**

**Evidence:**
- `build.gradle` (line 41–57): No Room dependency listed. No `kapt` or `ksp` plugin for Room annotation processing.
- `libs.versions.toml`: No Room version, no Room library entry.
- `MainActivity.kt`: No `@Entity`, `@Dao`, `@Database`, `Room.databaseBuilder()`, or any SQLite operation.
- No database helper, no migration, no DAO interface exists anywhere.

**What would need to exist here (currently absent):**
- Database class
- DAO interfaces
- Entity data classes
- Type converters
- Migration definitions

### 3.2 `core/network/`

**Status: Missing**

**Evidence:**
- `build.gradle` (line 41–57): No Retrofit, OkHttp, Ktor, or kotlinx.serialization dependency.
- `libs.versions.toml`: No network-related library entries.
- `MainActivity.kt`: No HTTP calls, no API interface, no `@GET`/`@POST` annotations, no `suspend` functions that make network requests.
- No `Interceptor`, no `OkHttpClient`, no `Retrofit.Builder` exists.

**What would need to exist here (currently absent):**
- API service interfaces
- Network module configuration (base URL, timeouts, interceptors)
- DTO (Data Transfer Object) classes from API responses
- Interceptors (auth, logging)
- Network result/sealed class wrappers

### 3.3 `core/designsystem/`

**Status: Partially Present**

**Evidence of what exists:**

| Current Location | Content | Target Location |
|-----------------|---------|-----------------|
| `ui/theme/Color.kt` | Full Material3 color palette (Primary, PrimaryContainer, Secondary, Tertiary, Surface, Background, Error + dark variants) | `core/designsystem/Color.kt` |
| `ui/theme/Theme.kt` | `MyAndroidTestAppTheme` with light/dark/dynamic scheme | `core/designsystem/Theme.kt` |
| `ui/theme/Type.kt` | Default `Typography()` (no overrides) | `core/designsystem/Type.kt` |

**What is missing for a complete design system:**

| Missing Element | Currently Located | Notes |
|----------------|-------------------|-------|
| Shape definitions | **Not extracted** | `RoundedCornerShape(8.dp)` hardcoded in ~40+ composables. No centralized `Shapes` object. |
| Spacing constants | **Not extracted** | `20.dp`, `16.dp`, `24.dp`, `14.dp`, `12.dp`, `22.dp`, `18.dp` repeated across every composable. No `Spacing` or `Dimens` object. |
| Icon system | **Not extracted** | No custom icons. No Material Icons dependency. The app uses text characters (initials, status letters) in colored boxes as icon substitutes. |
| Button atoms | **Inline** | `Button`/`OutlinedButton` are used directly with hardcoded `ButtonDefaults.buttonColors(...)` in every screen. No `PrimaryButton`, `SecondaryButton`, `OutlinedButton` wrappers. |
| Card atoms | **Inline** | `Card` used with repeated `RoundedCornerShape(8.dp)`, `CardDefaults.cardColors(containerColor = Color.White)`, `CardDefaults.cardElevation(defaultElevation = 2.dp)`. No `SurfaceCard`, `ElevatedCard` wrappers. |
| `StatusPill` | **Reusable but private** | Line 2866. Should be `core/designsystem/StatusPill.kt`. |
| `PaymentInfoRow` | **Reusable but private** | Line 2811. Should be `core/designsystem/DataRow.kt`. |
| `SmallMetricCard` | **Reusable but private** | Line 2561. Should be `core/designsystem/MetricCard.kt`. |
| `BottomMenuItem` | **Reusable but private** | Line 3000. Should be `core/designsystem/BottomBarItem.kt`. |
| `StatusSummaryChip` | **Reusable but private** | Line 2648. Should be `core/designsystem/StatusChip.kt`. |
| `AlertsSection` / `AlertRow` | **Reusable but private** | Lines 2685/2718. Could be generic alert component. |
| `ProfileTitle` / `ProfileHeroCard` / `ProfileInfoCard` | **Reusable but private** | Lines 1435/1458/1510. Section header + info card pattern. |
| `CollectionProgressBar` | **Reusable but private** | Line 1809. Generic progress bar. |
| `TenantBottomBar` / `LandlordBottomBar` | **Duplicated** | Lines 2923/2960. Nearly identical (different accent color). Should be one parameterized component. |
| `loginTextFieldColors` | **Reusable but private** | Line 3249. TextField styling helper. |
| `DashboardDetailRow` | **Duplicated with PaymentInfoRow** | Line 2842. Same label-value pattern with different text color. |
| `Modifier` extensions | **Not extracted** | No custom `Modifier` extensions exist. Common patterns like `.padding(horizontal = 20.dp, vertical = 24.dp)` are repeated. |

### 3.4 `core/model/`

**Status: Missing**

**Evidence:**
- `TenantProperty` (line 70–84): `private data class` inside `MainActivity.kt`
- `PaymentRecord` (line 164–172): `private data class` inside `MainActivity.kt`
- `AlertItem` (line 213–217): `private data class` inside `MainActivity.kt`
- `AppScreen` (line 54–68): `private enum class` inside `MainActivity.kt`
- `UserRole` (line 271–286): `private enum class` inside `MainActivity.kt`

All five types are declared as `private` in `MainActivity.kt`, making them inaccessible from any other package. In the target architecture, these would be extracted to `core/model/` or respective feature `domain/` packages, with visibility changed from `private` to `public` (or `internal` for module-scoped).

### 3.5 `core/util/`

**Status: Missing**

**Evidence:**
- `paymentStatusBackground(status: String): Color` (line 2887) — `private` helper
- `paymentStatusContent(status: String): Color` (line 2894) — `private` helper
- `receiptActionText(status: String): String` (line 2901) — `private` helper
- `alertBackground(status: String, accent: Color): Color` (line 2908) — `private` helper
- `alertContent(status: String, accent: Color): Color` (line 2917) — `private` helper

All five are `private` file-scoped functions. In the target architecture:
- `paymentStatusBackground` and `paymentStatusContent` → `core/designsystem/ColorMappings.kt` (UI presentation helpers)
- `receiptActionText` → `feature/payments/domain/PaymentStatusMapper.kt` (domain logic)
- `alertBackground` and `alertContent` → `core/designsystem/AlertStyles.kt`

Additionally, there are no general-purpose utility classes (date formatters, number formatters, validation functions, extension functions) anywhere in the codebase.

---

## 4. Feature Analysis

### 4.1 `feature/auth/`

**Current Status: Partially Present (UI only, no data/domain layers)**

| Component | Current Location | Type | Target Location |
|-----------|-----------------|------|-----------------|
| `WelcomeScreen` | `MainActivity.kt:416` | Screen composable | `feature/auth/ui/WelcomeScreen.kt` |
| `LoginScreen` | `MainActivity.kt:468` | Screen composable | `feature/auth/ui/LoginScreen.kt` |
| `LoginHero` | `MainActivity.kt:3220` | Reusable composable | `feature/auth/ui/LoginHero.kt` or `core/designsystem/` |
| `UserRole` | `MainActivity.kt:271` | Domain model (enum) | `feature/auth/domain/UserRole.kt` |
| `AppBrandHeader` | `MainActivity.kt:3077` | Reusable composable | `core/designsystem/AppBrandHeader.kt` |
| `RoleCard` | `MainActivity.kt:3121` | Reusable composable | `core/designsystem/RoleCard.kt` |
| `TrustStrip` / `TrustItem` | `MainActivity.kt:3178` | Reusable composable | `core/designsystem/TrustStrip.kt` |
| `loginTextFieldColors` | `MainActivity.kt:3249` | Styling helper | `core/designsystem/TextFieldStyles.kt` |
| `mobileNumber` / `password` state | `MainActivity.kt:473–474` | Local UI state | `feature/auth/ui/LoginViewModel.kt` |

**Missing layers:**
- `feature/auth/data/`: No auth API, no login endpoint, no auth token storage, no session manager.
- `feature/auth/domain/`: No login use case, no validation use case, no auth repository interface, no user domain model.

**Current behavior:** Login button navigates instantly with no validation, no API call, no credential check.

### 4.2 `feature/tenant/`

**Current Status: Partially Present (UI only)**

| Component | Current Location | Type | Target Location |
|-----------|-----------------|------|-----------------|
| `TenantDashboardScreen` | `MainActivity.kt:579` | Screen composable | `feature/tenant/ui/TenantDashboardScreen.kt` |
| `DashboardHeader` | `MainActivity.kt:2354` | Section composable | `feature/tenant/ui/DashboardHeader.kt` |
| `CurrentRentCard` | `MainActivity.kt:2391` | Section composable | `feature/tenant/ui/CurrentRentCard.kt` |
| `TenantRentStatusCard` | `MainActivity.kt:2462` | Section composable | `feature/tenant/ui/TenantRentStatusCard.kt` |
| `PropertySummaryCard` | `MainActivity.kt:2505` | Section composable | `feature/tenant/ui/PropertySummaryCard.kt` |
| `DashboardStatsRow` | `MainActivity.kt:2541` | Section composable | `feature/tenant/ui/DashboardStatsRow.kt` |
| `RecentPaymentsCard` | `MainActivity.kt:2595` | Section composable | `feature/tenant/ui/RecentPaymentsCard.kt` |
| `PaymentPreviewRow` | `MainActivity.kt:2777` | Row composable | `feature/tenant/ui/PaymentPreviewRow.kt` or `core/designsystem/` |
| `TenantBottomBar` | `MainActivity.kt:2923` | Navigation bar | `core/designsystem/` or `feature/tenant/ui/` |

**Missing layers:**
- `feature/tenant/data/`: No tenant API, no tenant-specific data source.
- `feature/tenant/domain/`: No tenant use cases, no tenant repository interface, no tenant domain model.

### 4.3 `feature/landlord/`

**Current Status: Partially Present (UI only)**

| Component | Current Location | Type | Target Location |
|-----------|-----------------|------|-----------------|
| `LandlordDashboardScreen` | `MainActivity.kt:1539` | Screen composable | `feature/landlord/ui/LandlordDashboardScreen.kt` |
| `LandlordHeader` | `MainActivity.kt:1599` | Header composable | `feature/landlord/ui/LandlordHeader.kt` |
| `LandlordWalletCard` | `MainActivity.kt:1628` | Section composable | `feature/landlord/ui/LandlordWalletCard.kt` |
| `LandlordCollectionSummary` | `MainActivity.kt:1697` | Section composable | `feature/landlord/ui/LandlordCollectionSummary.kt` |
| `LandlordAttentionCard` | `MainActivity.kt:1760` | Alert composable | `feature/landlord/ui/LandlordAttentionCard.kt` |
| `LandlordRecentTransactions` | `MainActivity.kt:1829` | Section composable | `feature/landlord/ui/LandlordRecentTransactions.kt` |
| `LandlordTenantPreview` | `MainActivity.kt:1904` | Preview composable | `feature/landlord/ui/LandlordTenantPreview.kt` |
| `TenantStatusRow` | `MainActivity.kt:1949` | Row composable | `feature/landlord/ui/` or `core/designsystem/` |
| `LandlordTenantsScreen` | `MainActivity.kt:1987` | Screen composable | `feature/landlord/ui/LandlordTenantsScreen.kt` |
| `TenantListCard` | `MainActivity.kt:2076` | Card composable | `feature/landlord/ui/TenantListCard.kt` |
| `PropertyDetailsScreen` | `MainActivity.kt:2170` | Screen composable | `feature/landlord/ui/PropertyDetailsScreen.kt` |
| `PropertyWalletStatusCard` | `MainActivity.kt:2315` | Card composable | `feature/landlord/ui/PropertyWalletStatusCard.kt` |
| `LandlordPlaceholderScreen` | `MainActivity.kt:3035` | Placeholder | `feature/landlord/ui/LandlordPlaceholderScreen.kt` |
| `LandlordBottomBar` | `MainActivity.kt:2960` | Navigation bar | `core/designsystem/` or `feature/landlord/ui/` |

**Data currently associated:**
- `landlordTenants: List<TenantProperty>` (line 86) — hardcoded list of 5 tenants
- `landlordDashboardAlerts: List<AlertItem>` (line 245) — hardcoded alerts
- `landlordTenantAlerts: List<AlertItem>` (line 258) — hardcoded alerts

**Missing layers:**
- `feature/landlord/data/`: No landlord API, no property data source.
- `feature/landlord/domain/`: No landlord use cases.

### 4.4 `feature/payments/`

**Current Status: Partially Present (UI only)**

| Component | Current Location | Type | Target Location |
|-----------|-----------------|------|-----------------|
| `PayRentScreen` | `MainActivity.kt:641` | Screen composable | `feature/payments/ui/PayRentScreen.kt` |
| `PaymentSuccessScreen` | `MainActivity.kt:857` | Screen composable | `feature/payments/ui/PaymentSuccessScreen.kt` |
| `PaymentHistoryScreen` | `MainActivity.kt:964` | Screen composable | `feature/payments/ui/PaymentHistoryScreen.kt` |
| `ReceiptDetailsScreen` | `MainActivity.kt:1160` | Screen composable | `feature/payments/ui/ReceiptDetailsScreen.kt` |
| `PaymentHistoryRow` | `MainActivity.kt:1084` | Row composable | `feature/payments/ui/PaymentHistoryRow.kt` |
| `PaymentSafetyNote` | `MainActivity.kt:2621` | Note composable | `feature/payments/ui/PaymentSafetyNote.kt` |

**Data currently associated:**
- `tenantPaymentRecords: List<PaymentRecord>` (line 174) — hardcoded list of 4 payments
- `tenantPaymentAlerts: List<AlertItem>` (line 232) — hardcoded alerts

**Missing layers:**
- `feature/payments/data/`: No payment API, no transaction data source.
- `feature/payments/domain/`: No payment use cases, no payment domain models.

**Current behavior:** "Confirm Payment" navigates to `PaymentSuccessScreen` with no actual payment processing.

### 4.5 `feature/profile/`

**Current Status: Partially Present (UI only)**

| Component | Current Location | Type | Target Location |
|-----------|-----------------|------|-----------------|
| `TenantProfileScreen` | `MainActivity.kt:1263` | Screen composable | `feature/profile/ui/TenantProfileScreen.kt` |
| `LandlordProfileScreen` | `MainActivity.kt:1343` | Screen composable | `feature/profile/ui/LandlordProfileScreen.kt` |
| `ProfileTitle` | `MainActivity.kt:1435` | Reusable composable | `core/designsystem/SectionHeader.kt` |
| `ProfileHeroCard` | `MainActivity.kt:1458` | Reusable composable | `core/designsystem/AvatarCard.kt` |
| `ProfileInfoCard` | `MainActivity.kt:1510` | Reusable composable | `core/designsystem/InfoCard.kt` |

**Missing layers:**
- `feature/profile/data/`: No profile API, no user data source.
- `feature/profile/domain/`: No profile use cases.

---

## 5. Data Layer Analysis

### Overall Status: Completely Absent

| Target Layer Component | Current Status | Evidence |
|-----------------------|---------------|----------|
| **Repository** | **Absent** | No `class` or `interface` with "Repository" in its name exists anywhere in the project. No repository pattern abstraction. |
| **Remote Data Source** | **Absent** | No API interface, no Retrofit service, no HTTP client, no network call. The app has zero network communication capability. |
| **Local Data Source** | **Absent** | No Room database, no DAO, no SQLite helper, no DataStore, no file I/O, no SharedPreferences usage. |
| **DTOs** | **Absent** | No Data Transfer Object classes. No serialization annotations (`@Serializable`, `@SerializedName`). No API response wrappers. |
| **Entities** | **Absent** | No Room `@Entity` annotated classes. No table definitions. |
| **Mappers** | **Absent** | No `mapToDomain()`, `mapFromDomain()`, `asUiModel()` functions. No mapper/extensions. |
| **Serialization** | **Absent** | No `kotlinx.serialization`, `Gson`, or `Moshi` dependency. |
| **Offline Cache** | **Absent** | No caching strategy. No offline-first pattern. No `NetworkBoundResource` or similar. |

### Current Data Flow

```
Hardcoded Kotlin Lists (file-scoped vals)
    ↓
Passed directly as parameters to screens
    ↓
Rendered by composable functions
```

There is no data flow. There are no `suspend` functions. There is no reactive observation. Data is read synchronously from `val` declarations at initialization.

---

## 6. Dependency Injection Analysis

### Status: Completely Absent

| DI Component | Present? | Evidence |
|-------------|----------|----------|
| `Application` class | **No** | No class in the project extends `Application`. The manifest declares no custom application class. |
| Hilt | **No** | No `hilt-android` or `hilt-compiler` dependency. No `@HiltAndroidApp`, `@AndroidEntryPoint`, `@HiltViewModel`, `@Inject`, `@Module`, `@Provides`, `@Binds`. |
| Dagger | **No** | No Dagger dependency. No `@Component`, `@Module`, `@Provides`. |
| Koin | **No** | No Koin dependency. No `module { }`, `startKoin { }`, `get()` calls. |
| Manual DI | **No** | No factory classes. No service locator. No `object` that provides dependencies. No constructor injection (no constructors with dependencies at all). |
| Singleton Pattern | **No** | No `object` declarations for shared services. No `companion object` factories. |

**Current dependency resolution:** There are no dependencies to resolve. The app has:
- No ViewModels to inject
- No repositories to provide
- No API services to construct
- No database instances to create

All code is self-contained in Composable functions and file-scoped immutable vals.

---

## 7. Design System Analysis

### Current: `ui/theme/`
### Target: `core/designsystem/`

### Currently Present (can be moved directly)

| Asset | Current File | Line Count | Gap |
|-------|-------------|------------|-----|
| Color palette | `ui/theme/Color.kt` | 30 lines | Minimal. No tonal palette, no surface variants beyond what exists. |
| Theme composable | `ui/theme/Theme.kt` | 90 lines | Complete for current needs. Missing `Shapes` parameter. |
| Typography | `ui/theme/Type.kt` | 6 lines | Placeholder (default Typography). No custom type scale. |

### Currently absent from design system

| Design Element | Current Status | Notes |
|---------------|----------------|-------|
| **Shapes** | Inline | `RoundedCornerShape(8.dp)` appears in ~40 places. No `Shapes` object. |
| **Spacing scale** | Inline | 12 distinct dp values used: 4, 6, 8, 10, 12, 14, 16, 18, 20, 22, 24, 28, 32, 34, 40dp. No `Spacing(4, 8, 12, 16, 20, 24, 32)` object. |
| **Elevation scale** | Inline | `defaultElevation = 2.dp` and `0.dp` used. No elevation constants. |
| **Icon set** | Missing | App uses text characters (initials, status letters) inside colored boxes as visual indicators. No Material Icons or custom icon drawables. |
| **Text styles** | Inline | `fontSize`, `fontWeight`, `lineHeight`, `color` specified on every `Text()` composable. No reusable text style constants. |
| **Button styles** | Inline | `ButtonDefaults.buttonColors(containerColor = ..., contentColor = ...)` repeated per screen. No `PrimaryButton`, `SecondaryButton`, etc. |
| **Card styles** | Inline | Same card pattern repeated ~30 times: `RoundedCornerShape(8.dp)`, white background, 2dp elevation. |
| **Component library** | Private | Reusable atoms (`StatusPill`, `PaymentInfoRow`, `SmallMetricCard`, `BottomMenuItem`) are `private` in `MainActivity.kt`. Not importable. |
| **Modifier extensions** | Absent | No custom modifiers for common padding/background patterns. |
| **`@Preview` annotations** | Absent | Zero previews. Component development requires full app launch. |

### Extent of design system effort

Of all the reusable UI in the codebase, approximately **25 composables** qualify as design system atoms that should live in `core/designsystem/`. The remaining composables are screen-specific or feature-specific.

---

## 8. Models Analysis

### Current State

| Model | Scope | Type | Location | Status in Target |
|-------|-------|------|----------|-----------------|
| `AppScreen` | `private` | Enum | `MainActivity.kt:54` | → `core/navigation/` (not a model per se) |
| `TenantProperty` | `private` | Data class | `MainActivity.kt:70` | → `feature/landlord/domain/` or `core/model/` |
| `PaymentRecord` | `private` | Data class | `MainActivity.kt:164` | → `feature/payments/domain/` or `core/model/` |
| `AlertItem` | `private` | Data class | `MainActivity.kt:213` | → `core/model/` or per-feature |
| `UserRole` | `private` | Enum | `MainActivity.kt:271` | → `feature/auth/domain/` |

### Target Classification

| Category | Required | Currently Exists |
|----------|----------|-----------------|
| **Domain Models** | Pure Kotlin data classes representing business entities. No Android dependencies. | **4 exist** but are `private` and mixed use (serve as both domain and UI models). |
| **UI Models** | Data classes designed for composable consumption (derived/transformed from domain). | **None exist separately**. The existing models are used directly as UI models. |
| **Network DTOs** | Data classes annotated for serialization, matching API contracts. | **None exist**. |
| **Database Entities** | Room `@Entity` annotated classes. | **None exist**. |
| **Mappers** | Extension functions converting between DTO ↔ Domain ↔ UI ↔ Entity. | **None exist**. |

### Analysis

The four existing data types (`TenantProperty`, `PaymentRecord`, `AlertItem`, `UserRole`) currently serve as **both domain models and UI models simultaneously**. They are defined once and passed directly to composables. This is acceptable for a prototype but creates tight coupling in a layered architecture:

- `TenantProperty` has 14 fields (line 71–84) that mix domain data (lease dates, deposit), display metadata (dateLabel), and UI state (walletStatus). In a layered architecture, this would be split into a domain entity with core fields and a UI model with derived display values.
- Status fields are `String` types rather than enums or sealed classes, preventing compile-time safety.

---

## 9. Utilities Analysis

### Current Helper Functions

| Helper | Line | Purpose | Target Location |
|--------|------|---------|-----------------|
| `paymentStatusBackground` | 2887 | Maps status string → background color | `core/designsystem/ColorMappings.kt` |
| `paymentStatusContent` | 2894 | Maps status string → content color | `core/designsystem/ColorMappings.kt` |
| `receiptActionText` | 2901 | Maps status string → description text | `feature/payments/domain/PaymentStatus.kt` |
| `alertBackground` | 2908 | Maps alert status + accent → background | `core/designsystem/AlertStyles.kt` |
| `alertContent` | 2915 | Maps alert status + accent → content color | `core/designsystem/AlertStyles.kt` |

### Missing Utility Categories

| Utility Category | Present? | Notes |
|-----------------|----------|-------|
| Date/time formatting | **No** | Date strings are hardcoded (`"06 May 2026"`, `"10 May 2026"`). No `SimpleDateFormat`, `java.time`, or formatter utility. |
| Currency/number formatting | **No** | Currency strings are hardcoded (`"Rs. 18,500"`). No `NumberFormat` or currency formatter. |
| Input validation | **No** | Login form has no validation (mobile number length, password strength, empty field checks). |
| Extension functions | **No** | No `Modifier.`, `Context.`, `String.`, `Color.` extension functions exist. |
| Resource helpers | **No** | No string resource accessor utilities. No dimension resource helpers. |
| Coroutine utilities | **No** | No `viewModelScope`, no `Dispatchers` configuration, no `Flow` extensions. |

---

## 10. Feature Boundary Analysis

### Feature Mapping

| Logical Feature | Current Lines (approx) | Currently Mixed With | Sufficiently Isolated? |
|----------------|----------------------|---------------------|------------------------|
| **Navigation** | ~120 (lines 54–68, 300–413) | Same file as all code | ❌ No — mixed with state management, screen assembly, and data |
| **Auth / Welcome / Login** | ~380 (lines 416–575, 3077–3245) | Same file as all code | ❌ No — mixed with all other features |
| **Tenant Dashboard** | ~480 (lines 579–637, 2354–2862) | Same file as all code | ❌ No — mixed with payment history, profile, landlord |
| **Landlord** | ~700 (lines 1539–2311, 2960–3073) | Same file as all code | ❌ No — mixed with tenant/payments |
| **Payments** | ~620 (lines 641–1156, 2621–2681) | Same file as all code | ❌ No — mixed with tenant dashboard |
| **Profile** | ~200 (lines 1263–1535) | Same file as all code | ❌ No — mixed with all other features |
| **Alerts** | ~80 (lines 213–269, 2685–2920) | Same file + helper functions | ❌ No — mixed across data + UI |
| **State Management** | ~5 (lines 302–305) + ~40 callbacks | Same function as navigation | ❌ No — inline in `RentWalletApp()` |
| **Theme / Design** | ~130 (3 theme files) | Separate package | ✅ Yes — already in `ui/theme/` package |
| **Data Layer** | ~120 (data class + hardcoded list declarations) | Same file as UI | ❌ No — mixed with UI code |
| **Helpers** | ~40 (5 helper functions) | Same file as UI | ❌ No — mixed at end of file |

### Dependency Graph Between Current Features

```
RentWalletApp (main composable)
├── Navigation (AppScreen enum, when block, BackHandler)
├── Auth Feature (WelcomeScreen, LoginScreen, RoleCard, AppBrandHeader)
│   └── depends on: UserRole model
├── Tenant Feature (TenantDashboardScreen + sub-composables)
│   ├── depends on: PaymentRecord (shared with payments)
│   └── depends on: AlertItem (shared with alerts)
├── Landlord Feature (LandlordDashboardScreen + sub-composables)
│   ├── depends on: TenantProperty (shared with tenant)
│   └── depends on: AlertItem (shared)
├── Payments Feature (PayRentScreen + PaymentSuccessScreen + ...)
│   └── depends on: PaymentRecord model
└── Profile Feature (TenantProfileScreen + LandlordProfileScreen)
    └── depends on: shared design system components (ProfileTitle, etc.)
```

**Key finding:** `TenantProperty` is shared between the Tenant and Landlord features. `PaymentRecord` is shared between the Tenant Dashboard and Payments features. `AlertItem` is shared across all features. These shared models create implicit coupling between features.

---

## 11. Migration Complexity

| Area | Complexity | Rationale |
|------|-----------|-----------|
| **Feature file extraction** | **Low** | Composables can be extracted into separate feature packages as-is with minimal changes. No logic changes needed. |
| **Design system creation** | **Low** | Color/Theme/Type can be relocated directly. Component extraction requires making `private` → `public` and adding parameterization. |
| **Model extraction** | **Low** | Data classes can be moved to `core/model/` with visibility changes. String enums can remain as-is initially. |
| **Navigation component** | **High** | Replacing the `when`-block with Navigation Component requires rewriting `RentWalletApp()` and all screen callbacks. |
| **ViewModel + StateFlow** | **Medium** | Requires introducing ViewModel dependency, creating ViewModels per screen, adapting state from `rememberSaveable` to `StateFlow`. |
| **Data layer creation** | **Very High** | Networking + persistence + repository requires new dependencies, new modules, new architecture patterns, and new testing. |
| **DI integration** | **High** | Requires Hilt setup (Application class, modules, entry points), wrapping all new ViewModels and repositories. |
| **Testing** | **Medium** | Requires writing meaningful unit/instrumentation tests for all existing screens and new ViewModels. |
| **Offline support** | **Very High** | Requires Room + DataStore + sync logic + connectivity monitoring. |
| **API integration** | **Very High** | Requires Retrofit + serialization + error handling + auth interceptor + token management. |

---

## 12. Migration Risk

### Safe to Move First (Low Risk)

| Item | Reason |
|------|--------|
| **`ui/theme/` → `core/designsystem/`** | Already isolated. Pure relocation. |
| **Reusable composables** → `core/designsystem/components/` | Pure file extraction. No behavioral changes. |
| **Data classes** → `core/model/` | Pure class relocation. Change visibility from `private` to `public`. |
| **Helper functions** → `core/util/` or `core/designsystem/` | Pure relocation. |
| **Screen composables** → `feature/*/ui/` | Pure file extraction. Same parameters, same behavior. |

### Requires Careful Migration (Medium Risk)

| Item | Reason |
|------|--------|
| **Hardcoded data lists** → `data/source/` | Requires adding `object` or class wrappers. Data is currently consumed directly; sources must maintain same interface. |
| **Navigation** → Navigation Component | Requires restructuring `RentWalletApp()`, changing state management, and updating all screen invocations. Should be done after ViewModel migration. |
| **State management** → ViewModel | Requires extracting state + callbacks from composables into ViewModels. Changes composable signatures. |

### Should Remain Until Later (High Risk)

| Item | Reason |
|------|--------|
| **Network layer** | Cannot be implemented until API contracts are defined. Premature abstraction adds complexity without value. |
| **Database layer** | Should be designed after repository interfaces are defined. Premature schema design is wasteful. |
| **DI framework** | Should be introduced last, after all layers exist and have concrete dependencies to wire. |
| **Offline support** | Depends on both network and database layers being complete. |
| **Full test suite** | Testing is most valuable after stable abstractions exist. Write tests during migration, not before. |

---

## 13. Final Gap Analysis Table

| Target Layer | Current Status | Gap | Migration Difficulty | Risk |
|-------------|---------------|-----|---------------------|------|
| `core/database/` | **Missing** | Complete absence: no Room, no entities, no DAOs, no database class | Very High | High |
| `core/network/` | **Missing** | Complete absence: no Retrofit, no API interfaces, no DTOs, no serialization | Very High | High |
| `core/designsystem/` | **Partially Present** | Color + Theme + Type exist. Missing: Shapes, Spacing, Icons, component atoms, Modifier extensions, Previews | Low | Low |
| `core/model/` | **Missing** | 4 data types exist but are `private` in MainActivity.kt. No domain/UI/network/DTO separation. | Low | Low |
| `core/util/` | **Missing** | 5 helper functions exist but are `private`. No general-purpose utilities (date, currency, validation, extensions). | Low | Low |
| `data/repository/` | **Missing** | Complete absence: no repository interfaces or implementations. | Very High | High |
| `data/source/` | **Missing** | Complete absence: no remote or local data sources. | Very High | High |
| `di/` | **Missing** | Complete absence: no DI framework, no Application class, no modules. | High | Medium |
| `feature/auth/` | **Partially Present** | UI screens exist. No data/domain layers. No actual auth logic. | Medium | Low |
| `feature/tenant/` | **Partially Present** | UI screens exist. No data/domain layers. | Medium | Low |
| `feature/landlord/` | **Partially Present** | UI screens + data model exist. No data/domain layers. | Medium | Low |
| `feature/payments/` | **Partially Present** | UI screens + data model exist. No data/domain layers. | Medium | Low |
| `feature/profile/` | **Partially Present** | UI screens exist. No data/domain layers. | Medium | Low |
| `MainActivity.kt` | **Present** | Exists but contains all code. Should be lean entry point only. | Low | Low |

---

## 14. Final Verdict

### How close is the current project to the desired architecture?

**Distance: Very Far — Approximately 15–20% of the architectural foundation exists.**

### Breakdown

| Category | Percentage of Target | Details |
|----------|--------------------|---------|
| **UI Layer** | ~80% | All 13 screens and ~25 reusable composables exist. Need extraction from monolith and visibility changes. |
| **Theme / Design System** | ~40% | Color palette + Theme composable exist. Shapes, spacing, icons, component atoms, and previews need creation. |
| **Models** | ~30% | 4 data types exist but need extraction, separation into domain/UI/DTO layers, and type-safe status enums. |
| **Helpers / Utilities** | ~20% | 5 helper functions exist. Need expansion to cover date, currency, validation, extensions. |
| **Navigation** | ~20% | Screen routing logic exists but uses custom `when`-block. No Navigation Component, no type safety, no back stack. |
| **State Management** | ~10% | State exists in `rememberSaveable`. No ViewModel, no StateFlow, no reactive observation. |
| **Feature Boundaries** | ~5% | Feature code exists but is entirely interleaved in one file. No package or module boundaries. |
| **Data Layer** | **~0%** | No repository, no data source, no persistence, no networking. Hardcoded data only. |
| **Dependency Injection** | **~0%** | No DI mechanism of any kind. |
| **Testing** | **~0%** | Placeholder tests with zero meaningful assertions about application behavior. |
| **Module Structure** | **~0%** | Single `:app` module. No `:core`, `:data`, `:domain`, or feature modules. |

### Architectural Foundation Already Exists: ~18%

The following can be retained and relocated with minimal changes:
- All 52 Composable functions (UI code is well-structured internally)
- All 3 theme files (Color, Theme, Type)
- All 4 data types (need visibility + location changes)
- All 5 helper functions (need location changes)
- The single-Activity Compose host pattern (production-grade pattern)

### Architectural Foundation Still Needed: ~82%

The following need to be introduced from scratch:
- Module structure (`:core`, `:data`, `:domain`, feature modules)
- Navigation Component (or equivalent type-safe navigation)
- ViewModel + StateFlow for every feature screen
- Repository pattern (interfaces + implementations)
- Network layer (Retrofit + serialization + interceptors)
- Database layer (Room + DAOs + entities)
- DataStore (session/preference management)
- DI framework (Hilt)
- Testing (unit + integration + UI)
- Design system expansion (shapes, spacing, icons, component library)
- Complete data flow architecture (DTO → Domain → UI model mapping)

### Summary

The current project is a **functional UI prototype** with a solid Compose foundation but **zero production-grade architecture layers beyond the presentation layer**. Approximately **18%** of the target architecture exists in a usable form (the UI components, theme, and models), while **82%** must be introduced — including the entire data layer, state management layer, navigation framework, DI system, module structure, and testing infrastructure.

The existing UI code is **not waste** — every composable, every data type, and every theme file has a clear destination in the target architecture. The migration path is one of **extraction and expansion**, not replacement.
