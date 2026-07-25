# RentWallet Android — Software Architecture Specification (RAS v1.0)

| Metadata | Value |
|----------|-------|
| **Document ID** | RAS-v1.0 |
| **Status** | Draft for review |
| **Author** | Principal Android Architect |
| **Domain** | Rental management platform |
| **Primary Actors** | Tenant, Landlord |
| **Future Actors** | Admin, Support Agent |

---

## 1. Executive Summary

### Overall Philosophy

The RentWallet Android application will adopt a **feature-first, layered architecture** built on four fundamental pillars:

1. **Separation of concerns** — UI, business logic, and data access live in distinct layers with strict dependency directions.
2. **Unidirectional data flow** — State flows down; events flow up. Screens observe state and emit user intents; ViewModels process intents and update state.
3. **Feature independence** — Each feature owns its UI, domain logic, and data contracts. Features communicate only through shared core models and a navigation graph.
4. **Offline-first readiness** — The data layer is designed from day one to support local persistence as the single source of truth, with network data as a sync mechanism.

### Architectural Style

**Feature-first Multi-layer MVVM with Repository Pattern**

`
Presentation layer (Compose UI + ViewModel)
    ^ (StateFlow)
Domain layer (Use Cases / business logic)
    ^ (Repository interfaces)
Data layer (Repository impl + Data Sources)
    ^ (HTTP / Local DB)
`

### Long-Term Vision

A modular, testable, backend-driven SaaS application where:

- Each feature can be developed, tested, and deployed independently
- Adding a new screen or API endpoint requires touching no code outside the feature boundary
- The app functions with or without network connectivity (graceful degradation)
- New actor roles (Admin, Support) are additive — they extend the navigation graph without modifying existing features
- AI features (chat, recommendations) integrate as features with their own domain and data layers, consuming the same shared core models


---

## 2. Architectural Principles

### P1 — Single Responsibility

Every type, function, and module must have exactly one reason to change.

**Why:** A ViewModel should not format dates. A repository should not render UI. A Composable should not call APIs. Violating this principle creates ripple-effect bugs where a change in one concern breaks unrelated functionality.

### P2 — Separation of Concerns

UI, business logic, data access, and dependency wiring must occupy different layers.

**Why:** Testing business logic requires extracting it from UI. Swapping data sources (mock > API > cache) requires abstracting data access. This principle enables both.

### P3 — Feature-First Organization

Code is grouped by business capability, not by technical layer.

**Why:** When all ViewModels are in one folder and all screens in another, adding a feature touches 7 locations. When feature code is together, it touches 1. Feature-first scales with team size—each feature is a natural ownership boundary.

### P4 — UI Independence

No Composable function may directly call a repository, a database, a network API, or any suspend function.

**Why:** Composable functions are called by the Compose runtime at any time, on any thread, potentially multiple times per frame. Direct side effects cause ANRs and untestable UI. All side effects must be initiated by ViewModels through structured concurrency.

### P5 — Dependency Inversion

High-level modules (domain) must not depend on low-level modules (data). Both must depend on abstractions (interfaces).

**Why:** The domain layer defines *what* the app does; the data layer defines *how*. Domain should not know about HTTP clients or database drivers. This allows swapping implementations without touching business logic.

### P6 — Unidirectional Data Flow (UDF)

`
Event (user tap / system callback)
    > ViewModel.onEvent()
        > Update StateFlow
            > Composable observes StateFlow
                > UI recomposes
`

**Why:** Bidirectional data flow creates unpredictable state mutations and makes debugging impossible. UDF ensures state changes are traceable to a single event source.

### P7 — State Hoisting

Composable state should be pushed to the highest ancestor that needs to read or control it.

**Why:** Local remember state in a composable makes that state untestable and unreachable from a ViewModel. State that survives configuration changes or process death must be hoisted to a ViewModel.

### P8 — Explicit Contracts

Every layer boundary must be defined by an interface or a sealed class, never by implicit conventions.

**Why:** Implicit conventions are invisible to the compiler. A teammate cannot know they violated an architectural rule until code review. Explicit contracts (Repository, UseCase, UiState, UiEvent) are enforceable at compile time.

### P9 — Consistency Over Cleverness

When multiple patterns could solve a problem, choose the most consistent one, even if more verbose.

**Why:** An architecture is only as good as its least consistent file. Inconsistency creates confusion, bugs, and onboarding friction. Every feature must follow the same internal structure.

### P10 — Progressive Enhancement

The architecture must support starting simple and adding complexity later without rewriting.

**Why:** Not every feature needs a Use Case initially. A simple screen can pass data directly from ViewModel to Repository. Later, if business logic grows, a Use Case can be inserted without changing ViewModel or Repository contracts.


---

## 3. Target Architecture

### High-Level Architecture

`
+---------------------------------------------------------------------+
|                         :app module                                  |
|                                                                      |
|  +-------------------------------------------------------------+    |
|  |                    Presentation Layer                         |    |
|  |  +--------------+  +--------------+  +------------------+   |    |
|  |  | Compose UI   |  | ViewModel    |  | Navigation Graph |   |    |
|  |  | (Screens +   |<--+ (StateFlow + |  | (NavHost +       |   |    |
|  |  |  Components) |  |  UiEvent)    |  |  Feature Graphs) |   |    |
|  |  +--------------+  +--------------+  +------------------+   |    |
|  +-------------------------------------------------------------+    |
|              |                                          ^            |
|              v (events)                                 | (State)    |
|  +-------------------------------------------------------------+    |
|  |                      Domain Layer                               |    |
|  |  +------------------+  +------------------+                    |    |
|  |  | Use Cases        |  | Repository       |                    |    |
|  |  | (business rules) |  | Interfaces       |                    |    |
|  |  +------------------+  +------------------+                    |    |
|  +-------------------------------------------------------------+    |
|              |                                                       |
|              v (repository contract calls)                           |
|  +-------------------------------------------------------------+    |
|  |                       Data Layer                                 |    |
|  |  +------------------+  +------------------+                    |    |
|  |  | Repository Impl  |  | Data Sources     |                    |    |
|  |  | (orchestrator)   |  | (API + DB +       |                    |    |
|  |  |                  |  |  DataStore)       |                    |    |
|  |  +------------------+  +------------------+                    |    |
|  +-------------------------------------------------------------+    |
|              |                                                       |
|              v                                                       |
|  +-------------------------------------------------------------+    |
|  |                      Core Layer                                  |    |
|  |  +-----------+ +----------+ +--------+ +--------+             |    |
|  |  | Design    | | Model    | | Util   | | Network|             |    |
|  |  | System    | | (shared) | |        | | Config |             |    |
|  |  +-----------+ +----------+ +--------+ +--------+             |    |
|  +-------------------------------------------------------------+    |
+---------------------------------------------------------------------+
`

### Multi-Module Architecture (Future Evolution)

`
+---------------------------------------------------------------------+
|                         :app module (shell)                          |
|  Hosts: MainActivity, Navigation Graph, DI setup                    |
|  Depends on: all feature modules, all core modules                   |
+---------------------------------------------------------------------+
         |                |              |                |
         v                v              v                v
+-------------+  +-------------+  +-----------+  +-------------+
| :feature:   |  | :feature:   |  | :feature: |  | :feature:   |
| auth        |  | tenant      |  | landlord  |  | payments    |
+-------------+  +-------------+  +-----------+  +-------------+
         |                |              |                |
         +--------+-------+-------+------+--------+
                  |               |               |
                  v               v               v
          +-------------+  +-------------+  +-------------+
          | :core:      |  | :core:      |  | :core:      |
          | design      |  | data        |  | model       |
          | system      |  | (repository)|  |             |
          +-------------+  +-------------+  +-------------+
                  |               |               |
                  +-------+-------+-------+------+
                          |               |
                          v               v
                  +-------------+  +-------------+
                  | :core:      |  | :core:      |
                  | network     |  | database    |
                  +-------------+  +-------------+
`

### Dependency Direction (Inviolable)

`
Presentation Layer
    | depends on
    v
Domain Layer
    | depends on
    v
Data Layer
    | depends on
    v
Core Layer

Reverse dependencies are FORBIDDEN.
- Domain must never import from Presentation (no imports to ViewModel, Composable, NavHost).
- Data must never import from Domain (except the Repository interfaces it implements).
- Core must never import from any other layer.
- Presentation may import from Domain and Core.
- Data may import from Domain (interfaces) and Core.
`

### Package Responsibilities

| Layer | Purpose | Contents |
|-------|---------|----------|
| Presentation | UI rendering + state observation | Composable screens, ViewModels, Navigation graphs, UI state classes, component composables |
| Domain | Business rules + use cases | Use cases (interactors), Repository interfaces, Domain model classes |
| Data | Data access + synchronization | Repository implementations, API data sources, DB data sources, DTOs, Mappers |
| Core | Shared foundation | Design system components, shared models, utilities, network interceptors, base classes |


---

## 4. Application Layers

### 4.1 Presentation Layer

**Purpose:** Render UI, collect user input, observe state.

**Responsibilities:**
- Composable screen functions (one per screen)
- Reusable UI components (design system atoms)
- ViewModel classes (one per screen or per feature)
- Navigation graphs (one per feature, one root)
- UI state data classes (sealed class per screen)
- UI event sealed classes (one per screen)

**What belongs here:**
- @Composable functions
- ViewModel subclasses
- @AndroidEntryPoint annotated classes
- NavHost, NavGraphBuilder extensions
- UiState sealed interfaces/classes
- UiEvent sealed classes
- StateFlow<UiState> in ViewModels
- Composable @Preview functions

**What must NEVER belong here:**
- Repository implementations
- Database access (Room DAO calls)
- Network API calls
- Domain entity definitions (use UI models instead)
- Business rule calculations that exceed simple formatting
- Dependency injection module definitions

### 4.2 Domain Layer

**Purpose:** Encapsulate business rules and use cases. This is the most stable layer and should have zero platform dependencies.

**Responsibilities:**
- Define what the application can do
- Orchestrate business workflows
- Validate business rules
- Transform data between external formats and internal models
- Define repository contracts (interfaces)

**What belongs here:**
- UseCase classes (single-responsibility interactors)
- Repository interface definitions
- Domain model classes (pure Kotlin, no Android annotations)
- Business rule validation functions
- Result wrapper classes (success/failure sealed hierarchy)
- Exception classes (domain-specific)

**What must NEVER belong here:**
- Android framework imports (ndroid.*, Context, Bundle, Parcelable)
- Compose imports
- HTTP library imports
- Database annotation imports
- Any platform-specific code

### 4.3 Data Layer

**Purpose:** Implement the repository contracts defined in the domain layer. Manage data sources and synchronize between remote and local storage.

**Responsibilities:**
- Fetch data from remote APIs
- Persist data locally (database, preferences)
- Map between DTOs and domain models
- Handle caching strategy (network-first, cache-first, offline-first)
- Manage connectivity state for offline support

**What belongs here:**
- Repository class implementations (implementing domain interfaces)
- API service interfaces (Retrofit or equivalent)
- Database DAO interfaces (Room or equivalent)
- DataStore helpers
- DTO (Data Transfer Object) classes matching API contracts
- Entity classes matching database schema
- Mapper extension functions (DTO > Domain, Entity > Domain, Domain > DTO)
- Data source classes (RemoteDataSource, LocalDataSource)
- Network interceptors and error handling

**What must NEVER belong here:**
- UI imports (Compose, ViewModel, Navigation)
- Business rule logic beyond data mapping
- Use case orchestration
- ViewModel state definitions

### 4.4 Core Layer

**Purpose:** Provide shared, reusable foundation code used by all layers.

**Sub-layers:**

| Sub-layer | Contents |
|-----------|----------|
| core/designsystem | Theme (Color, Type, Shapes), reusable component composables (Button, Card, StatusPill, InfoRow, BottomBar, etc.), Modifier extensions, spacing/dimens constants |
| core/model | Shared domain models shared across features (User, Address, Money, etc.), shared enums |
| core/util | Extension functions, date/number formatters, validation utilities, resource helpers |
| core/network | Base API configuration, interceptor setup, network result wrappers, connectivity monitor |
| core/database | Base database configuration, migration helpers, type converters |
| core/navigation | Navigation route definitions, deep link configuration |

**What must NEVER belong here:**
- Feature-specific screens or ViewModels
- Feature-specific use cases
- Feature-specific repository implementations
- Business rules for specific domains

### 4.5 Navigation Layer

**Purpose:** Define the navigation graph, screen routes, and navigation actions.

**Responsibilities:**
- Define route constants/sealed classes
- Configure NavHost with feature graphs
- Handle deep links
- Manage authentication flow (auth guard)
- Coordinate navigation between features

**What belongs here:**
- Route definitions (sealed class or string constants per feature)
- NavGraph extension functions (one per feature)
- Root NavHost composable
- Navigation actions (NavigateToLogin, NavigateToDashboard, etc.)

### 4.6 DI Layer

**Purpose:** Wire dependencies between layers. Configure how objects are created and scoped.

**What belongs here:**
- Application class with DI initialization
- Module definitions (one per feature, one per core layer)
- Qualifier annotations (e.g., for base URL, dispatchers)
- Scope definitions (Singleton, ViewModelScoped, FragmentScoped)

---

## 5. Feature Architecture

### 5.1 Feature Internal Structure

Every feature must follow this exact structure:

```
feature/<feature-name>/
    ui/
        <FeatureName>Screen.kt
        <FeatureName>ViewModel.kt
        <FeatureName>UiState.kt
        <FeatureName>UiEvent.kt
        <FeatureName>NavGraph.kt
        components/
            (feature-specific composables)
    domain/
        <FeatureName>UseCase.kt
        <FeatureName>Repository.kt (interface)
        <FeatureName>DomainModel.kt
    data/
        <FeatureName>RepositoryImpl.kt
        <FeatureName>ApiService.kt
        <FeatureName>Dto.kt
        <FeatureName>Entity.kt
        <FeatureName>Mapper.kt
        <FeatureName>DataSource.kt
```

**Rules:**
- The ui/ sub-package may depend on domain/ within the same feature.
- The data/ sub-package may depend on domain/ (to implement interfaces) and on core/.
- The domain/ sub-package must NOT depend on ui/ or data/.
- If a feature has no business logic beyond pass-through, the domain/ sub-package may be omitted initially.
- If a feature has no custom data sources, the data/ sub-package may be omitted initially.

### 5.2 Authentication Feature

**Purpose:** Handle user login, registration, session management, and role selection.

**Screens:**
- WelcomeScreen (role selection)
- LoginScreen (credentials)
- ForgotPasswordScreen (future)
- RegistrationScreen (future)

**ViewModel State (conceptual):**
- AuthUiState sealed interface: Loading, Welcome(selectedRole), Login(role, mobile, password), Authenticated(user, role), Error(message)

**Domain:**
- LoginUseCase — validates credentials, returns auth token
- AuthRepository interface — login, logout, refreshToken, isAuthenticated
- User domain model — id, name, mobile, role, token

**Data:**
- AuthRepositoryImpl — orchestrates AuthApi + SessionDataStore
- AuthApiService — POST /auth/login, POST /auth/refresh
- LoginRequestDto, LoginResponseDto, UserDto
- AuthMapper — Dto > User, User > SessionDataStore
- SessionDataStore — encrypted token storage via DataStore

**Navigation Role:**
- Auth graph is the default navigation start destination
- On successful authentication, navigate to TenantDashboard or LandlordDashboard based on role
- On logout, clear back stack and navigate to Welcome

### 5.3 Tenant Feature

**Purpose:** Tenant home dashboard, rent overview, property info.

**Screens:**
- TenantDashboardScreen (main dashboard with CurrentRentCard, Alerts, RecentPayment)
- TenantProfileScreen (profile details, settings) -- shares with Profile feature

**ViewModel State (conceptual):**
- TenantDashboardUiState sealed interface: Loading, Success(currentRent, recentPayments, alerts, property, leaseEnd, deposit), Error(message)

**Domain:**
- GetTenantDashboardUseCase — aggregates dashboard data
- TenantRepository interface — getDashboard, getRentInfo, getLeaseInfo

**Data:**
- TenantRepositoryImpl — orchestrates API responses + local cache
- TenantApiService — GET /tenant/dashboard
- DashboardResponseDto, RentInfoDto, mappers

### 5.4 Landlord Feature

**Purpose:** Landlord dashboard, tenant management, property oversight.

**Screens:**
- LandlordDashboardScreen (wallet balance, collection summary, recent transactions)
- LandlordTenantsScreen (tenant list with rent status)
- PropertyDetailsScreen (single tenant/property detail)
- LandlordProfileScreen (portfolio health, wallet info) -- shares with Profile feature

**Domain:**
- GetLandlordDashboardUseCase
- LandlordRepository interface
- TenantProperty domain model

### 5.5 Payments Feature

**Purpose:** Rent payment processing, payment history, receipt generation.

**Screens:**
- PayRentScreen (review rent details, confirm payment)
- PaymentSuccessScreen (confirmation with transaction ID)
- PaymentHistoryScreen (list of all payments with status)
- ReceiptDetailsScreen (single receipt)

**Domain:**
- ProcessPaymentUseCase — validate payment, submit, return receipt
- GetPaymentHistoryUseCase
- PaymentsRepository interface

### 5.6 Profile Feature

**Purpose:** User profile management shared across Tenant and Landlord roles.

**Screens:**
- ProfileScreen (parameterized by role -- displays either Tenant or Landlord details)

**Components:**
- ProfileHeader — avatar, name, role
- ProfileInfoSection — key-value info card collection

**Domain:**
- GetProfileUseCase
- ProfileRepository interface

### 5.7 Notifications Feature (Future)

**Purpose:** Push notification handling, in-app notification center.

**Architecture:** Will follow the same feature structure with Firebase Cloud Messaging service in the data layer.

### 5.8 Properties and Leases (Future Expansion)

**Purpose:** Full property lifecycle management. These will be features if the domain grows large enough, otherwise sub-packages of Tenant/Landlord.

### 5.9 AI Assistant Feature (Future)

**Purpose:** AI-powered chat, rent recommendations, dispute resolution.

**Architecture:** Will follow the same feature structure with a specialized AI API data source. Domain models will be shared with existing features.


---

## 6. Package Structure

### 6.1 Complete Package Hierarchy (Single-Module Phase)

```
com.rentwallet.app/
|
+-- RentWalletApplication.kt              (Application class)
+-- MainActivity.kt                        (Single Activity, hosts NavHost)
|
+-- di/
|   +-- AppModule.kt                       (core dependencies: dispatchers, resources)
|   +-- NetworkModule.kt                   (HTTP client, interceptors, base URL)
|   +-- DatabaseModule.kt                  (Room database, DAOs)
|   +-- DataStoreModule.kt                 (DataStore instance)
|   +-- feature/
|       +-- AuthModule.kt                  (auth repository, API, use cases)
|       +-- TenantModule.kt
|       +-- LandlordModule.kt
|       +-- PaymentsModule.kt
|       +-- ProfileModule.kt
|
+-- core/
|   +-- designsystem/
|   |   +-- theme/
|   |   |   +-- Color.kt
|   |   |   +-- Theme.kt
|   |   |   +-- Type.kt
|   |   |   +-- Shape.kt
|   |   +-- component/
|   |   |   +-- RentWalletButton.kt
|   |   |   +-- RentWalletCard.kt
|   |   |   +-- StatusPill.kt
|   |   |   +-- InfoRow.kt
|   |   |   +-- BottomBar.kt
|   |   |   +-- BottomBarItem.kt
|   |   |   +-- MetricCard.kt
|   |   |   +-- ProgressBar.kt
|   |   |   +-- AlertSection.kt
|   |   |   +-- AlertRow.kt
|   |   |   +-- SectionHeader.kt
|   |   |   +-- AvatarCard.kt
|   |   |   +-- TrustStrip.kt
|   |   |   +-- TrustItem.kt
|   |   |   +-- AppBrandHeader.kt
|   |   |   +-- RoleCard.kt
|   |   |   +-- LoginHero.kt
|   |   +-- dimens/
|   |   |   +-- Spacing.kt                 (spacing constants: xs, sm, md, lg, xl)
|   |   |   +-- Elevation.kt
|   |   +-- icon/
|   |       +-- RentWalletIcons.kt        (icon definitions)
|   |       +-- AppIcon.kt                 (icon composable)
|   |
|   +-- model/
|   |   +-- User.kt
|   |   +-- Role.kt
|   |   +-- Money.kt
|   |   +-- Address.kt
|   |   +-- PaymentStatus.kt (enum)
|   |   +-- AlertStatus.kt (enum)
|   |   +-- AlertItem.kt
|   |
|   +-- util/
|   |   +-- DateTimeFormatter.kt
|   |   +-- CurrencyFormatter.kt
|   |   +-- ValidationUtil.kt
|   |   +-- Extensions.kt                 (general Kotlin extensions)
|   |   +-- Resource.kt                   (Result wrapper: Success/Error/Loading)
|   |
|   +-- navigation/
|   |   +-- Route.kt                       (sealed route hierarchy)
|   |   +-- AppNavHost.kt                  (root NavHost composable)
|   |
|   +-- network/
|   |   +-- NetworkResult.kt              (sealed class: Success/Error/Loading)
|   |   +-- NetworkMonitor.kt             (connectivity observer)
|   |   +-- AuthInterceptor.kt
|   |   +-- ErrorInterceptor.kt
|   |
|   +-- database/
|       +-- RentWalletDatabase.kt
|       +-- Converters.kt                 (type converters for Room)
|
+-- feature/
|   +-- auth/
|   |   +-- ui/
|   |   |   +-- WelcomeScreen.kt
|   |   |   +-- WelcomeViewModel.kt
|   |   |   +-- WelcomeUiState.kt
|   |   |   +-- LoginScreen.kt
|   |   |   +-- LoginViewModel.kt
|   |   |   +-- LoginUiState.kt
|   |   |   +-- AuthNavGraph.kt
|   |   +-- domain/
|   |   |   +-- LoginUseCase.kt
|   |   |   +-- AuthRepository.kt
|   |   |   +-- User.kt
|   |   +-- data/
|   |       +-- AuthRepositoryImpl.kt
|   |       +-- AuthApiService.kt
|   |       +-- AuthDto.kt
|   |       +-- AuthMapper.kt
|   |       +-- SessionDataStore.kt
|   |
|   +-- tenant/
|   |   +-- ui/
|   |   |   +-- dashboard/
|   |   |   |   +-- TenantDashboardScreen.kt
|   |   |   |   +-- TenantDashboardViewModel.kt
|   |   |   |   +-- TenantDashboardUiState.kt
|   |   |   |   +-- components/
|   |   |   |       +-- CurrentRentCard.kt
|   |   |   |       +-- RentStatusCard.kt
|   |   |   |       +-- PropertySummaryCard.kt
|   |   |   |       +-- DashboardStatsRow.kt
|   |   |   |       +-- RecentPaymentsCard.kt
|   |   |   |       +-- DashboardHeader.kt
|   |   |   +-- TenantNavGraph.kt
|   |   +-- domain/
|   |   |   +-- GetDashboardUseCase.kt
|   |   |   +-- TenantRepository.kt
|   |   +-- data/
|   |       +-- TenantRepositoryImpl.kt
|   |       +-- TenantApiService.kt
|   |       +-- TenantDto.kt
|   |       +-- TenantEntity.kt
|   |       +-- TenantMapper.kt
|   |       +-- TenantLocalDataSource.kt
|   |
|   +-- landlord/
|   |   +-- ui/
|   |   |   +-- dashboard/
|   |   |   |   +-- LandlordDashboardScreen.kt
|   |   |   |   +-- LandlordDashboardViewModel.kt
|   |   |   |   +-- LandlordDashboardUiState.kt
|   |   |   |   +-- components/
|   |   |   |       +-- LandlordHeader.kt
|   |   |   |       +-- WalletCard.kt
|   |   |   |       +-- CollectionSummaryCard.kt
|   |   |   |       +-- AttentionCard.kt
|   |   |   |       +-- RecentTransactionsCard.kt
|   |   |   |       +-- TenantPreviewSection.kt
|   |   |   +-- tenantlist/
|   |   |   |   +-- LandlordTenantsScreen.kt
|   |   |   |   +-- LandlordTenantsViewModel.kt
|   |   |   |   +-- LandlordTenantsUiState.kt
|   |   |   |   +-- components/
|   |   |   |       +-- TenantListCard.kt
|   |   |   |       +-- TenantStatusRow.kt
|   |   |   +-- propertydetail/
|   |   |   |   +-- PropertyDetailsScreen.kt
|   |   |   |   +-- PropertyDetailsViewModel.kt
|   |   |   |   +-- PropertyDetailsUiState.kt
|   |   |   |   +-- components/
|   |   |   |       +-- PropertyWalletStatusCard.kt
|   |   |   +-- LandlordNavGraph.kt
|   |   +-- domain/
|   |   |   +-- GetLandlordDashboardUseCase.kt
|   |   |   +-- GetTenantsUseCase.kt
|   |   |   +-- GetPropertyDetailsUseCase.kt
|   |   |   +-- LandlordRepository.kt
|   |   |   +-- TenantProperty.kt
|   |   +-- data/
|   |       +-- LandlordRepositoryImpl.kt
|   |       +-- LandlordApiService.kt
|   |       +-- LandlordDto.kt
|   |       +-- LandlordEntity.kt
|   |       +-- LandlordMapper.kt
|   |       +-- LandlordLocalDataSource.kt
|   |
|   +-- payments/
|   |   +-- ui/
|   |   |   +-- payrent/
|   |   |   |   +-- PayRentScreen.kt
|   |   |   |   +-- PayRentViewModel.kt
|   |   |   |   +-- PayRentUiState.kt
|   |   |   +-- success/
|   |   |   |   +-- PaymentSuccessScreen.kt
|   |   |   |   +-- PaymentSuccessViewModel.kt
|   |   |   |   +-- PaymentSuccessUiState.kt
|   |   |   +-- history/
|   |   |   |   +-- PaymentHistoryScreen.kt
|   |   |   |   +-- PaymentHistoryViewModel.kt
|   |   |   |   +-- PaymentHistoryUiState.kt
|   |   |   |   +-- components/
|   |   |   |       +-- PaymentHistoryRow.kt
|   |   |   +-- receipt/
|   |   |   |   +-- ReceiptDetailsScreen.kt
|   |   |   |   +-- ReceiptDetailsViewModel.kt
|   |   |   |   +-- ReceiptDetailsUiState.kt
|   |   |   +-- PaymentsNavGraph.kt
|   |   +-- domain/
|   |   |   +-- ProcessPaymentUseCase.kt
|   |   |   +-- GetPaymentHistoryUseCase.kt
|   |   |   +-- PaymentsRepository.kt
|   |   |   +-- PaymentRecord.kt
|   |   +-- data/
|   |       +-- PaymentsRepositoryImpl.kt
|   |       +-- PaymentsApiService.kt
|   |       +-- PaymentsDto.kt
|   |       +-- PaymentsEntity.kt
|   |       +-- PaymentsMapper.kt
|   |
|   +-- profile/
|   |   +-- ui/
|   |   |   +-- ProfileScreen.kt
|   |   |   +-- ProfileViewModel.kt
|   |   |   +-- ProfileUiState.kt
|   |   |   +-- ProfileNavGraph.kt
|   |   +-- domain/
|   |   |   +-- GetProfileUseCase.kt
|   |   |   +-- ProfileRepository.kt
|   |   +-- data/
|   |       +-- ProfileRepositoryImpl.kt
|   |       +-- ProfileApiService.kt
|   |       +-- ProfileDto.kt
|   |       +-- ProfileMapper.kt
|   |
|   +-- notifications/  (future)
|   +-- ai/             (future)
|
+-- res/                                    (standard Android resources)
    +-- values/
        +-- strings.xml
        +-- colors.xml
        +-- themes.xml
```

### 6.2 Package Justification

| Package | Reason |
|---------|--------|
| `di/` | Centralizes dependency wiring. Separates DI configuration from business logic. Each feature gets its own module for clarity. |
| `core/designsystem/` | Isolates UI foundation from feature code. Enables visual consistency, theming changes, and component reuse across all features. |
| `core/model/` | Shared domain models used by multiple features. Prevents circular dependencies between features. |
| `core/util/` | Common utilities used across layers. No layer-specific code. |
| `core/navigation/` | Route definitions shared across feature navigation graphs. Centralizes deep link configuration. |
| `core/network/` | Base networking infrastructure. Feature-specific API services live in feature packages. |
| `core/database/` | Base database infrastructure. Feature-specific entities and DAOs live in feature packages. |
| `feature/*/` | Feature boundaries. Each feature is a self-contained vertical slice through all layers. |


---

## 7. Dependency Rules

### 7.1 Allowed Dependencies

| Package | May Depend On |
|---------|---------------|
| `MainActivity.kt` | `core/navigation/`, `di/` |
| `di/` | Everything (wires all layers) |
| `core/designsystem/` | Nothing internal (pure Compose + Material3 only) |
| `core/model/` | Nothing internal (pure Kotlin) |
| `core/util/` | `core/model/` |
| `core/navigation/` | `core/model/` |
| `core/network/` | `core/model/`, `core/util/` |
| `core/database/` | `core/model/`, `core/util/` |
| `feature/*/ui/` | Same-feature `domain/`, `core/designsystem/`, `core/model/`, `core/navigation/`, `core/util/` |
| `feature/*/domain/` | `core/model/`, `core/util/` (NO platform imports) |
| `feature/*/data/` | Same-feature `domain/`, `core/network/`, `core/database/`, `core/model/`, `core/util/` |

### 7.2 Forbidden Dependencies

| From | To | Why |
|------|----|-----|
| Any `domain/` package | Any `ui/` package | Domain must not know about UI. Creates circular dependency and prevents testing. |
| Any `domain/` package | Any `data/` package | Domain defines interfaces; data implements them. Domain must not depend on implementations. |
| Any `data/` package | Any `ui/` package | Data must not know about presentation. This would make API changes affect UI. |
| Any `domain/` package | Android framework | Domain must be pure Kotlin for unit testability and multiplatform compatibility. |
| Cross-feature `ui/` | Another feature's `ui/` or `data/` | Features must not depend on other features' internals. Cross-feature navigation uses `core/navigation/`. |
| `core/` packages | Any `feature/` package | Core must be generic. Feature-specific code creates coupling and prevents reuse. |

### 7.3 Cross-Feature Communication

**Allowed mechanisms:**
1. **Navigation only:** Feature A navigates to Feature B by calling a route defined in `core/navigation/Route.kt`. No direct class reference.
2. **Shared models:** Both features use a model from `core/model/`. Neither imports the other's package.
3. **Shared events:** Both features observe a shared event from `core/` (e.g., `AuthEvent.LoggedOut`). Neither imports the other.

**Forbidden mechanisms:**
1. Feature A importing a class from Feature B's `ui/`, `domain/`, or `data/`.
2. Shared global state that both features mutate directly.
3. Feature A referencing Feature B's ViewModel or repository.

### 7.4 Ownership Rules

| Artifact | Owner | May Modify |
|----------|-------|------------|
| `di/*Module.kt` for a feature | Feature team | DI team (review) |
| `core/designsystem/` component | Design system team | All teams (review) |
| `core/model/` | Architecture team | All teams (review) |
| `core/navigation/` | Architecture team | All teams (review) |
| `feature/*/ui/` | Feature team | Feature team only |
| `feature/*/domain/` | Feature team | Feature team only |
| `feature/*/data/` | Feature team | Feature team only |

### 7.5 Import Rules

- Wildcard imports (`import com.rentwallet.feature.tenant.*`) are forbidden. Every import must be explicit.
- Internal package imports within a feature (e.g., `ui/` importing from `domain/`) are permitted without restriction.
- Cross-package imports must be reviewed for architectural compliance.


---

## 8. Data Flow

### 8.1 Complete Data Flow

```
User taps "Pay Rent" button
    |
    v
Composable: PayRentScreen
    |
    | calls: viewModel.onEvent(PayRentUiEvent.ConfirmPayment)
    v
ViewModel: PayRentViewModel
    |
    | calls: processPaymentUseCase(amount, propertyId, paymentMethod)
    v
UseCase: ProcessPaymentUseCase
    |
    | performs business validation:
    |   - Amount > 0
    |   - Property is active
    |   - Not already paid for this period
    |   - Wallet has capacity
    |
    | calls: paymentsRepository.processPayment(request)
    v
Repository Interface: PaymentsRepository
    |
    | implemented by:
    v
RepositoryImpl: PaymentsRepositoryImpl
    |
    | Step 1: Save PendingPayment to local DB (optimistic update)
    | Step 2: Call apiService.submitPayment(request)
    | Step 3: On success: update local DB to Confirmed
    | Step 4: On failure: revert local DB, propagate error
    |
    | calls: paymentsApiService.submitPayment(requestDto)
    v
ApiService: PaymentsApiService (Retrofit interface)
    |
    | HTTP POST /api/v1/payments
    v
Backend Server
    |
    | HTTP 200 Response
    v
ApiService returns: PaymentResponseDto
    |
    v
RepositoryImpl maps: Dto -> Domain model (via PaymentsMapper)
    |
    v
RepositoryImpl returns: Result<PaymentReceipt> (Success or Error)
    |
    v
UseCase returns: Result<PaymentReceipt> to ViewModel
    |
    v
ViewModel updates: _uiState.update { it.copy(isLoading=false, receipt=receipt) }
    |
    v
StateFlow emits new UiState
    |
    v
Composable observes: val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    |
    v
Composable recomposes with Success state
    |
    | navigates to: PaymentSuccessScreen(receipt)
    v
Navigation: navController.navigate(Route.PaymentSuccess(receiptId))
```

### 8.2 Responsibility of Each Step

| Step | Responsibility |
|------|---------------|
| **Composable** | Collect user intent, render state. No business logic. No data access. |
| **ViewModel** | Convert user intents to use case calls. Hold UI state. Manage screen lifecycle. Handle loading/error states. |
| **Use Case** | Execute single business operation. Validate rules. Coordinate between repositories if needed. Return Result. |
| **Repository (interface)** | Define data contract. No implementation. Belongs to domain layer. |
| **Repository (impl)** | Orchestrate data sources. Implement caching strategy. Map DTOs/Entities to domain models. Handle data-level errors. |
| **Data Source (API)** | Network communication. DTO serialization/deserialization. HTTP error mapping. |
| **Data Source (DB)** | Local persistence. Entity CRUD. Offline cache. |
| **DI Module** | Wire all implementations to interfaces. Scope dependencies correctly. |

### 8.3 Data Flow for Read Operations

```
Screen renders
    |
    v
ViewModel.init() or viewModelScope.launch
    |
    | calls: getDashboardUseCase()
    v
UseCase -> Repository -> ApiService (GET)
    |
    v
Repository checks: Local cache valid?
    |   YES: return cached data + refresh in background
    |   NO:  fetch from API, cache locally, return fresh data
    v
ViewModel updates StateFlow
    |
    v
UI recomposes
```

### 8.4 Data Flow for Offline Scenarios

```
User opens app with no connectivity
    |
    v
Repository detects: NetworkMonitor.isOnline == false
    |
    v
Repository returns local data (if available)
    |
    v
UiState shows: stale data + banner "Showing cached data"
    |
    ...
    |
Connectivity restored
    |
    v
NetworkMonitor emits online event
    |
    v
Repository syncs: fetch latest, update local cache
    |
    v
ViewModel re-emits with fresh data
    |
    v
UI updates
```


---

## 9. State Management Philosophy

### 9.1 State Categories

| State Type | Definition | Storage | Example |
|-----------|------------|---------|---------|
| **UI State** | What the screen shows right now | `StateFlow<UiState>` in ViewModel | `TenantDashboardUiState.Success(rent, alerts, ...)` |
| **Screen State** | Transient UI-specific state | `rememberSaveable` in Composable (minimal) | Text field focus, scroll position, animation progress |
| **Shared State** | State needed by multiple features | `StateFlow` in a shared ViewModel or `core/` event bus | `AuthState(isLoggedIn, user)` |
| **Navigation State** | Current screen in the nav graph | `NavController` internal back stack | Current route, saved state handles |
| **Transient Events** | One-shot effects (snackbar, navigation) | `SharedFlow` in ViewModel | "Payment successful" snackbar, "Navigate to dashboard" |
| **Loading State** | Data fetching in progress | Boolean or enum in UiState | `UiState.Loading` |
| **Error State** | Operation failed | Sealed class in UiState with error details | `UiState.Error(NetworkError, message)` |
| **Success State** | Operation completed | Data class in UiState | `UiState.Success(data)` |

### 9.2 State Ownership

| Owner | Owns | Does NOT Own |
|-------|------|-------------|
| **ViewModel** | UI State, transient events, loading/error state | Domain logic, data access, navigation decisions |
| **Use Case** | Business logic execution, validation results | State storage, UI decisions |
| **Repository** | Data synchronization, caching strategy | Business rules, UI formatting |
| **Composable** | Local UI state (focus, scroll, animation) | Business state, persistent state |

### 9.3 UiState Pattern

Every screen must follow this exact pattern:

```kotlin
// In feature/<name>/ui/<Name>UiState.kt
sealed interface NameUiState {
    data object Loading : NameUiState
    data class Success(
        val field1: Type1,
        val field2: Type2,
        val isLoading: Boolean = false   // for refresh operations
    ) : NameUiState
    data class Error(val message: String) : NameUiState
}
```

**Rules:**
- Every screen has exactly one UiState sealed interface.
- The Success state contains ALL data the screen needs (no additional `var` state in ViewModel).
- Loading and Error are separate sealed variants, not fields inside Success.
- UiState is immutable. ViewModel uses `copy()` or `.update {}` to produce new states.
- UiState must NOT contain Android framework types (Context, View, etc.).

### 9.4 UiEvent Pattern

Every screen that accepts user input must define a UiEvent sealed class:

```kotlin
// In feature/<name>/ui/<Name>UiEvent.kt
sealed interface NameUiEvent {
    data class FieldChanged(val value: String) : NameUiEvent
    data object ButtonTapped : NameUiEvent
    data object Retry : NameUiEvent
}
```

**Rules:**
- Events are processed by ViewModel in a single `onEvent(event: NameUiEvent)` function.
- Events represent user intent, not state mutations ("ButtonTapped", not "SetLoadingTrue").
- Event sealed class is defined in the same package as the ViewModel.

### 9.5 State Position

```
                    ViewModel
                    (StateFlow<UiState>)
                         |
           +-------------+-------------+
           |             |             |
           v             v             v
     Composable A   Composable B   Composable C
     (observes)     (observes)     (observes)
```

- State is **pushed up** to the ViewModel.
- Composable children receive state as parameters, never observe a ViewModel directly.
- Shared state (e.g., auth state) is provided via DI as a singleton StateFlow.
- No two ViewModels share a MutableStateFlow that either can mutate.

### 9.6 Transient Events (One-Shot)

For events that should be consumed once and not replayed on recomposition:

```
ViewModel sends event via SharedFlow (replay = 0)
    |
    v
Composable collects via LaunchedEffect
    |
    | triggers: navigation, snackbar, dialog
    v
Event consumed
```

**Examples:** "Navigate to next screen", "Show error snackbar", "Launch external app".


---

## 10. Navigation Philosophy

### 10.1 Navigation Ownership

- **Navigation routes** are owned by `core/navigation/Route.kt`.
- **Feature navigation graphs** are owned by each feature (e.g., `feature/auth/ui/AuthNavGraph.kt`).
- **Root navigation host** is owned by `core/navigation/AppNavHost.kt` (or `MainActivity.kt`).
- **Navigation state** (back stack) is owned by `NavController`, never managed manually.

### 10.2 Graph Architecture

```
Root NavHost (AppNavHost.kt)
    |
    +-- Auth Graph (auth graph = start destination)
    |   |-- /welcome
    |   |-- /login
    |   |-- /forgot-password
    |   +-- (nested, no bottom bar)
    |
    +-- Tenant Graph (after Tenant login)
    |   |-- /tenant/dashboard (start, bottom bar: Dashboard)
    |   |-- /tenant/payments (bottom bar: Payments)
    |   |   |-- /tenant/payments/pay
    |   |   |-- /tenant/payments/success
    |   |   |-- /tenant/payments/history
    |   |   |-- /tenant/payments/receipt/{id}
    |   |-- /tenant/profile (bottom bar: Profile)
    |   +-- (bottom navigation scaffold)
    |
    +-- Landlord Graph (after Landlord login)
    |   |-- /landlord/dashboard (start, bottom bar: Dashboard)
    |   |-- /landlord/tenants (bottom bar: Tenants)
    |   |   |-- /landlord/tenants/{id}
    |   |-- /landlord/profile (bottom bar: Profile)
    |   +-- (bottom navigation scaffold)
    |
    +-- (future) Admin Graph
    |   +-- (separate scaffold)
```

### 10.3 Route Definitions

Routes must be centralized in `core/navigation/Route.kt` as a sealed class hierarchy:

```kotlin
sealed class Route(val route: String) {
    // Auth
    data object Welcome : Route("welcome")
    data object Login : Route("login")

    // Tenant
    data object TenantDashboard : Route("tenant/dashboard")
    data object TenantProfile : Route("tenant/profile")

    // Payments (nested under tenant)
    data object PayRent : Route("tenant/payments/pay")
    data class ReceiptDetails(val paymentId: String) : Route("tenant/payments/receipt/{paymentId}")

    // Landlord
    data object LandlordDashboard : Route("landlord/dashboard")
    data object LandlordTenants : Route("landlord/tenants")
    data class PropertyDetails(val tenantId: String) : Route("landlord/tenants/{tenantId}")

    // Profile
    data object Profile : Route("profile")
}
```

### 10.4 Auth Guard Navigation

- The root NavHost checks `authState.isAuthenticated` to determine which graph to show.
- If not authenticated -> Auth Graph (Welcome screen).
- If authenticated with Tenant role -> Tenant Graph.
- If authenticated with Landlord role -> Landlord Graph.
- Logout clears the entire back stack and navigates to Welcome.

### 10.5 Deep Links

- Each route must support deep link configuration for future notification support.
- Deep link format: `rentwallet://<route>` (e.g., `rentwallet://tenant/payments/receipt/{id}`).
- Deep links to authenticated routes trigger auth guard: if not logged in, navigate to login first, then complete the deep link.

### 10.6 Scalability

| Scenario | How Navigation Handles It |
|----------|--------------------------|
| New screen | Add route to Route sealed class, add composable to feature NavGraph |
| New feature | Create feature NavGraph, add routes, register in AppNavHost |
| New role | Create new role graph, add to AppNavHost route selection |
| Deep link from notification | Configure deep link on route, NavComponent handles parsing |
| Conditional navigation (auth guard) | Wrap root navigation in auth state observation |


---

## 11. Shared Components Strategy

### 11.1 What Belongs in `core/designsystem/`

**Theme Foundation:**
- `Color.kt` — color palette with light/dark variants
- `Theme.kt` — Material3 theme composable
- `Type.kt` — typography scale
- `Shape.kt` — reusable shape definitions (small, medium, large)
- `Spacing.kt` — spacing/dimension constants
- `Elevation.kt` — elevation constants

**Atomic Components (reused across 3+ features):**
- `RentWalletButton.kt` — primary, secondary, outline, text variants with consistent styling
- `RentWalletCard.kt` — elevated, filled, outlined card variants
- `StatusPill.kt` — colored status badge (currently duplicated in 15+ places)
- `InfoRow.kt` — label-value text row (currently `PaymentInfoRow` + `DashboardDetailRow`)
- `BottomBar.kt` — parameterizable bottom navigation bar (currently `TenantBottomBar` + `LandlordBottomBar`)
- `BottomBarItem.kt` — individual tab item
- `MetricCard.kt` — small stat display card (currently `SmallMetricCard` + `StatusSummaryChip`)
- `ProgressBar.kt` — horizontal progress bar (currently `CollectionProgressBar`)
- `AlertSection.kt` — alert list container
- `AlertRow.kt` — individual alert item
- `SectionHeader.kt` — title + subtitle header (currently `ProfileTitle`)
- `AvatarCard.kt` — initials avatar + name + role (currently `ProfileHeroCard`)
- `TrustStrip.kt` — trust indicator row (currently used only on Welcome screen, but potentially reusable)
- `AppBrandHeader.kt` — app logo + brand name header
- `RoleCard.kt` — role selection card

**Icons:**
- Centralized icon composable that wraps Material Icons
- App-specific icons defined as composable functions

**Utilities:**
- `Modifier` extensions (common padding, background, clickable patterns)
- Color mapping helpers (currently `paymentStatusBackground`, `paymentStatusContent`, `alertBackground`, `alertContent`)

### 11.2 What Stays in Features

**Screen-specific components** stay in the feature package:
- `CurrentRentCard.kt` — specific to `feature/tenant/ui/dashboard/`
- `WalletCard.kt` — specific to `feature/landlord/ui/dashboard/`
- `PaymentHistoryRow.kt` — specific to `feature/payments/ui/history/`
- `PropertyWalletStatusCard.kt` — specific to `feature/landlord/ui/propertydetail/`

**Rule of thumb:** If a component is used by one feature only, it lives in that feature. If it is used by two or more features, it moves to `core/designsystem/`. If it is used by one feature but could reasonably be used by others, it starts in the feature and is promoted later.

### 11.3 What Belongs in `core/model/`

- `User.kt` — shared user identity model
- `Role.kt` — enum for Tenant, Landlord, Admin
- `Money.kt` — value class for currency amounts
- `Address.kt` — address model shared by properties and users
- `PaymentStatus.kt` — sealed class/enum for payment lifecycle (Pending, Processing, Paid, Failed, Refunded)
- `AlertStatus.kt` — enum for alert severity/type
- `AlertItem.kt` — shared alert data class

### 11.4 What Belongs in `core/util/`

- DateTime formatting utilities
- Currency formatting utilities
- Input validation utilities (mobile number, email, amount)
- Extension functions (String.capitalizeWords(), List.orEmpty(), etc.)
- Resource wrapper (`core/util/Resource.kt` — Success/Error/Loading sealed class)


---

## 12. Error Handling Philosophy

### 12.1 Error Categories

| Category | Source | Examples | Handling |
|----------|--------|----------|----------|
| **API Errors** | Network layer | HTTP 400, 401, 500, timeout | Mapped to domain exceptions in Repository. ViewModel shows user-friendly message. |
| **Validation Errors** | Domain layer | Invalid amount, empty field | Returned as specific sealed class variant from Use Case. ViewModel shows inline field error. |
| **Business Errors** | Domain layer | Payment already processed, lease expired | Returned as domain exception from Use Case. ViewModel shows explanation + suggested action. |
| **Unexpected Exceptions** | Any layer | NullPointerException, IndexOutOfBounds | Caught at Repository/UseCase boundary. Logged. Converted to generic UiState.Error. |

### 12.2 Error Propagation Flow

```
Data Source (API/DB)
    | throws: IOException, HttpException, DatabaseException
    v
RepositoryImpl
    | catches and converts to: domain-specific exception or Result.failure()
    v
Use Case
    | may add business context: wraps in domain exception
    v
ViewModel
    | catches, maps to UiState.Error with user-facing message
    | triggers event: SharedFlow.emit(Event.ShowSnackbar(message))
    v
Composable
    | shows error state: retry button, error message, or snackbar
```

### 12.3 Error Mapping Rules

| Layer | Error Representation | Must NOT |
|-------|---------------------|----------|
| Data Source | Exceptions (IOException, HttpException) | Surface implementation details to upper layers |
| Repository | `Result<T>` or domain-specific exceptions | Leak HTTP status codes or SQL errors |
| Use Case | Domain-specific sealed result | Pass through generic exceptions |
| ViewModel | `UiState.Error(message: String, retry: (() -> Unit)?)` | Show raw exception messages |
| Composable | Error UI component with message + action | Expose error details to user beyond what is safe |

### 12.4 User-Facing Messages

- Every error message must be a string resource (extracted for localization).
- Error messages must be **actionable** — explain what happened AND what the user can do.
- Technical details (stack traces, HTTP codes, SQL errors) must NEVER be shown to users.
- Network errors show: "Unable to connect. Check your internet connection and try again."
- Validation errors show: specific field guidance ("Mobile number must be 10 digits.")
- Business errors show: context + resolution ("This payment has already been processed.")

### 12.5 Logging Philosophy

- All errors must be logged at the boundary where they are caught (Repository boundary for data errors, Use Case boundary for business errors).
- Logs must include: correlation ID, feature context, error type, stack trace (for unexpected exceptions).
- User-facing errors must be logged separately from debug logs.
- Production logs must NOT contain: personally identifiable information (PII), passwords, tokens, or full request/response bodies.


---

## 13. Scalability Strategy

### 13.1 New Features

**How the architecture supports adding new features:**
1. Create a new package under `feature/<name>/` following the standard structure.
2. Define routes in `core/navigation/Route.kt`.
3. Create a feature NavGraph composable.
4. Register the NavGraph in `AppNavHost.kt`.
5. Define repository interface in `domain/`, implement in `data/`.
6. Wire dependencies in a new `di/feature/<Name>Module.kt`.
7. No existing code needs modification (open for extension, closed for modification).

### 13.2 New Modules

**How the architecture supports modularization:**
- The single-module package structure maps directly to a multi-module Gradle structure.
- Each `feature/` package becomes `:feature:<name>` module.
- Each `core/` sub-package becomes `:core:<name>` module.
- The module dependency graph mirrors the package dependency rules.
- Modularization is an implementation concern, not an architecture redesign.

### 13.3 New APIs

**How the architecture supports new backend endpoints:**
1. Add new DTO classes in the feature's `data/` package.
2. Add new API service methods in the feature's API service interface.
3. Add mappers in the feature's mapper file.
4. Add repository interface methods in the feature's domain repository.
5. Implement in the feature's repository impl.
6. Existing code is not modified — only the data layer grows.

### 13.4 Offline Support

**How the architecture will support offline:**
- Repository pattern already provides the abstraction needed for offline-first.
- Repositories will implement a cache strategy (data freshness check).
- Room database will serve as the local cache/single source of truth.
- DataStore will store preferences and auth tokens.
- NetworkMonitor (observing ConnectivityManager) will be injected into repositories.
- ViewModels will expose `isOffline` state for UI adaptation (stale data banner).

### 13.5 Multi-Role Users

**How the architecture supports multiple roles:**
- `Route` sealed class groups routes by role (Auth, Tenant, Landlord, Admin).
- `AppNavHost.kt` selects the root graph based on authenticated role.
- `User` model in `core/model/` includes the role.
- AuthRepository exposes `currentUser: StateFlow<User?>` for reactive observation.
- Navigation guard logic is centralized, not duplicated per feature.

### 13.6 Future Admin App

**How the architecture supports an admin interface:**
- A new `feature/admin/` package follows the same structure.
- Admin routes are in `Route.kt`.
- Admin graph is registered in `AppNavHost.kt`.
- Admin may share existing data layer (view tenant dashboard data, view payment records) by calling the same repositories.
- Admin-specific data sources are in `feature/admin/data/`.

### 13.7 Future AI Assistant

**How the architecture supports AI features:**
- A new `feature/ai/` package follows the standard structure.
- `core/model/` is extended with AI-related models (ChatMessage, Suggestion).
- AI API service in `feature/ai/data/AiApiService.kt`.
- AI-specific local caching in `feature/ai/data/AiLocalDataSource.kt`.
- AI domain logic in `feature/ai/domain/` (prompt construction, response parsing).
- AI feature integrates with existing features by consuming shared domain models (e.g., AI suggests payment amounts based on PaymentRecord history).

### 13.8 Future Analytics

**How the architecture supports analytics:**
- Analytics tracking is handled at the DI layer or via a dedicated `core/analytics/` package.
- ViewModels emit analytics events alongside state changes.
- No feature code directly calls analytics SDKs — all analytics goes through a `core/analytics/AnalyticsEvent` sealed class.
- Adding a new analytics provider requires changing only the DI module.


---

## 14. Coding Standards

### 14.1 Naming Conventions

| Artifact | Convention | Example |
|----------|-----------|---------|
| **Package** | Lowercase, domain-first | `com.rentwallet.feature.payments.ui` |
| **Class/Interface** | PascalCase | `ProcessPaymentUseCase`, `PaymentsRepository` |
| **Composable function** | PascalCase | `fun PaymentHistoryScreen(...)` |
| **ViewModel** | PascalCase, ends with `ViewModel` | `LoginViewModel` |
| **Use Case** | PascalCase, ends with `UseCase` | `GetDashboardUseCase` |
| **UiState** | PascalCase, ends with `UiState` | `TenantDashboardUiState` |
| **UiEvent** | PascalCase, ends with `UiEvent` | `LoginUiEvent` |
| **Repository interface** | PascalCase | `AuthRepository` |
| **Repository impl** | PascalCase, ends with `Impl` | `AuthRepositoryImpl` |
| **Data source** | PascalCase, ends with `DataSource` | `AuthRemoteDataSource` |
| **DTO** | PascalCase, ends with `Dto` | `LoginRequestDto` |
| **Entity** | PascalCase, ends with `Entity` | `UserEntity` |
| **Mapper** | PascalCase, ends with `Mapper` | `AuthMapper` |
| **Composable parameter lambdas** | `on` + Verb | `onLoginClick`, `onAmountChanged` |
| **StateFlow** in ViewModel | Prefix with `uiState` or descriptive name | `uiState: StateFlow<LoginUiState>` |
| **Event SharedFlow** | `events: SharedFlow<UiEvent>` | `events` |

### 14.2 File Organization

| Rule | Applies To |
|------|-----------|
| One class/interface per file (except sealed class hierarchies) | All layers |
| File name matches primary class name | All layers |
| UiState and UiEvent may share a file with the ViewModel if small | UI layer |
| Composable, ViewModel, UiState, UiEvent are always separate files | UI layer |
| No file exceeds 400 lines | All layers |
| No composable function exceeds 100 lines | UI layer |
| No ViewModel function exceeds 30 lines | UI layer |
| No Use Case class exceeds 100 lines | Domain layer |

### 14.3 Composable Organization

| Rule | Rationale |
|------|-----------|
| Every screen composable has exactly one ViewModel parameter | Clear dependency |
| No screen composable calls `remember` for business state | State lives in ViewModel |
| No screen composable collects from multiple ViewModels | Prevents confusion |
| Every screen composable has a `@Preview` | Visual testing during development |
| Reusable components are extracted into separate functions | Single responsibility |
| Components that accept lambdas use `on` prefix for callback names | Consistent naming |

### 14.4 ViewModel Organization

| Rule | Rationale |
|------|-----------|
| ViewModel receives use cases via constructor injection | Testable, no hardcoded dependencies |
| ViewModel exposes exactly one `StateFlow<UiState>` | Single source of truth for screen |
| ViewModel exposes one `SharedFlow<UiEvent>` for one-shot events | Transient event handling |
| ViewModel contains a single `onEvent(event: UiEvent)` function | Centralized event processing |
| ViewModel does NOT hold any mutable `var` outside StateFlow | Ensures thread safety |
| ViewModel does NOT call `viewModelScope.launch { }` outside `init` or `onEvent` | Predictable lifecycle |
| ViewModel does NOT import any Android View class | Testability |

### 14.5 Repository Organization

| Rule | Rationale |
|------|-----------|
| Repository interface is defined in `domain/` | Dependency inversion |
| Repository implementation is in `data/` | Implementation is an infrastructure concern |
| Repository methods return `Flow<T>` for observable data or `suspend fun` for one-shot | Reactive or async |
| Repository maps DTOs/Entities to domain models before returning | Clean layer boundary |
| Repository does NOT catch exceptions unless it adds business value | Let errors propagate |
| Repository does NOT import ViewModel, Composable, or any UI type | Separation of concerns |

### 14.6 Visibility Rules

| Scope | Visibility | Used For |
|-------|-----------|----------|
| Cross-feature | `public` | Route definitions, shared models, DI modules |
| Within a feature (across ui/domain/data) | `internal` or `public` | Use cases, repositories, ViewModels |
| Within a package | `internal` | Internal helpers, private state, sub-composables |
| Within a class | `private` | Implementation details |

### 14.7 Immutability Rules

| Rule | Applies To |
|------|-----------|
| All data classes are `val` (read-only) properties | Models, DTOs, Entities |
| All List parameters are immutable (`List<T>`, not `MutableList<T>`) | Parameters |
| UiState data classes use `val` with `copy()` for mutation | State management |
| StateFlow values are replaced, not mutated in-place | State management |
| Use case inputs are value types, not mutable references | Domain layer |

### 14.8 Documentation Standards

| Artifact | Required Documentation |
|----------|----------------------|
| Repository interface | KDoc explaining the data contract and error behavior |
| Use Case | KDoc explaining the business operation, inputs, outputs, error cases |
| UiState sealed interface | KDoc explaining each state variant |
| Public API service methods | KDoc explaining endpoint, request format, response format |


---

## 15. Migration Strategy

### 15.1 Architectural Evolution, Not Rewrite

The migration from the current prototype to the target architecture is an **evolution**, not a rewrite. The existing UI code (52 composables, 3,259 lines) is structurally sound and will be preserved. The migration focuses on extracting, relocating, and wrapping — never discarding.

### 15.2 Phase Sequence (Architectural Order)

**Phase 1: Foundation (Core Layer)**
- Extract existing `ui/theme/` into `core/designsystem/theme/`.
- Extract reusable composables from `MainActivity.kt` into `core/designsystem/component/`.
- Define `core/model/` with shared data types extracted from `MainActivity.kt`.
- Define `core/util/` with extracted helper functions.
- This phase produces zero behavioral changes. Code moves to new packages with new visibility.

**Phase 2: Feature Extraction (Presentation Layer)**
- Extract screen composables into their respective `feature/*/ui/` packages.
- This is a mechanical file-split operation. Each screen becomes a separate file.
- Navigation lambdas remain as parameters (temporary — will be replaced in Phase 5).
- `RentWalletApp()` is replaced with a structured navigation host.
- `MainActivity.kt` shrinks from 3,259 lines to approximately 50 lines.

**Phase 3: State Management (ViewModel Layer)**
- Introduce ViewModel dependency.
- Create one ViewModel per screen, moving state from `rememberSaveable` to `StateFlow<UiState>`.
- Create UiState sealed interfaces for every screen.
- Compose screens now observe `viewModel.uiState` instead of receiving state as parameters.

**Phase 4: Domain Layer**
- Extract business rules from composables into Use Case classes.
- Define Repository interfaces for each feature.
- Move data models from `MainActivity.kt` into `feature/*/domain/` or `core/model/`.
- Create mapper functions for status/color mappings.

**Phase 5: Navigation (Navigation Component)**
- Replace the `when`-block navigation with Navigation Compose.
- Define Route sealed class.
- Create feature NavGraphs.
- Replace lambda navigation callbacks with `navController.navigate()`.

**Phase 6: Data Layer**
- Introduce network and persistence dependencies.
- Create API service interfaces.
- Create database entities and DAOs.
- Implement Repository interfaces with data source orchestration.
- Add DTOs and mappers.
- Replace hardcoded data with API-driven data.

**Phase 7: Dependency Injection**
- Introduce DI framework.
- Create Application class with DI initialization.
- Create modules for all layers.
- Replace manual dependency construction with injected dependencies.

**Phase 8: Testing and Polish**
- Add unit tests for ViewModels and Use Cases.
- Add Compose UI tests for screens.
- Add integration tests for repositories.
- Extract string resources.
- Add ProGuard rules.
- Performance profiling and optimization.

### 15.3 Preservation Guarantees

| Artifact | Preservation Strategy |
|----------|---------------------|
| All 52 composable functions | Retained as-is, only relocated. No signature changes until Phase 3. |
| All 4 data types (TenantProperty, PaymentRecord, AlertItem, UserRole) | Retained as-is, relocated to `core/model/` or feature `domain/`. |
| All 3 theme files | Retained as-is, relocated to `core/designsystem/theme/`. |
| All color definitions, spacing values | Retained as-is, potentially centralized into constants in later phases. |
| All screen flows (navigation order) | Preserved exactly. The user experience does not change. |

### 15.4 Architectural Debt That Resolves Itself

| Current Debt | Resolved By |
|-------------|-------------|
| Monolithic 3,259-line file | Phase 2 (feature extraction) |
| No ViewModel/StateFlow | Phase 3 (ViewModel introduction) |
| Business logic in composables | Phase 4 (Use Case extraction) |
| when-block navigation | Phase 5 (Navigation Component) |
| Hardcoded data | Phase 6 (data layer + API integration) |
| No dependency wiring | Phase 7 (DI framework) |
| No tests | Phase 8 (testing) |
| Hardcoded strings | Phase 8 (string extraction) |


---

## 16. Final Architectural Vision

### What RentWallet Looks Like After Migration

**Maintainability:**
- Every file is under 400 lines.
- Every function has a single responsibility.
- A new developer can understand the Tenant feature by reading only `feature/tenant/`.
- Changing the UI of the login screen touches only `feature/auth/ui/`.
- Adding a new API endpoint touches only one feature's `data/` package.
- Modifying the color palette touches only `core/designsystem/theme/Color.kt`.

**Extensibility:**
- A new feature is created by duplicating the feature template structure and implementing interfaces.
- A new API version is supported by adding a new data source implementation behind the existing repository interface.
- A new user role (Admin, Support) is added by creating a new feature graph and registering it in the navigation host.
- A new backend is integrated by swapping the API service implementation behind the same DTO contracts.

**Scalability:**
- 5 features can be developed in parallel by 5 developers with zero merge conflicts on source files.
- Feature teams own their packages end-to-end (ui, domain, data).
- The architecture supports 10, 20, or 50 features without structural changes.
- Multi-module compilation allows incremental builds (only changed modules recompile).

**Developer Experience:**
- `@Preview` annotations on every screen composable enable rapid UI iteration without app launch.
- ViewModel unit tests run in milliseconds on the host machine without an emulator.
- Use Cases are pure Kotlin — testable without Android framework.
- Repository interfaces provide clear contracts — a developer knows exactly what data is available.
- Navigation routes are centralized and type-safe — no runtime route string mismatches.
- Error handling is consistent across all features — a developer knows how errors behave without reading each feature.

**Team Collaboration:**
- Architecture decisions are documented and enforced by the package structure and dependency rules.
- Code reviews focus on feature logic, not architectural violations (the structure prevents most violations).
- Onboarding: a new developer reads the feature template (one screen, one ViewModel, one Use Case, one Repository) and understands 90% of the architecture.
- UI designers own `core/designsystem/` and can modify components without touching feature code.

**Future Growth:**
- AI Assistant = one new feature package + route registration. No existing code changes.
- Push Notifications = one new feature package + deep link configuration in `core/navigation/`.
- Analytics = one new `core/analytics/` package + event emissions from ViewModels. No feature modifications.
- Subscription billing = one new `feature/billing/` package + shared Money model in `core/model/`.
- Admin web dashboard = Android app acts as companion; data layer remains unchanged (same backend).
- Multi-language support = all strings already externalized to `strings.xml` in each feature (Phase 8).

### The Architecture in Three Sentences

The RentWallet Android application is a **feature-first, multi-layer architecture** where each business capability (auth, tenant, landlord, payments, profile) is a self-contained vertical slice through UI, domain logic, and data access. Layers communicate through explicit contracts (use cases, repository interfaces, StateFlow) and never through implicit coupling. The result is an application that can grow from 5 features to 50 without architectural friction, where a new feature can be added by creating 6 files and registering 2 things (a NavGraph and a DI module) — without modifying any existing code.

---

*End of RentWallet Android — Software Architecture Specification (RAS v1.0)*
*This document contains no implementation code, no library selections, and no refactoring steps.*
*It is a pure architectural contract for the engineering team.*

