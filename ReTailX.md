# ReTailX - Complete Project Overview

## Project Summary
**ReTailX** is a modern, feature-rich Android retail management application built with Kotlin. It provides a complete solution for retail operations including inventory management, billing, sales analytics, employee management, and AI-powered insights.

---

## 1. Project Structure

```
ReTailX/
├── app/
│   ├── src/main/java/com/rajatt7z/retailx/
│   │   ├── adapters/           # RecyclerView Adapters (22 files)
│   │   ├── auth/               # Authentication Activities
│   │   ├── database/           # Room Database (Draft Products)
│   │   ├── di/                 # Dependency Injection Module
│   │   ├── fragments/          # UI Fragments (25+ files)
│   │   │   ├── orders/         # Order/Bill related fragments
│   │   │   ├── products/       # Product management fragments
│   │   │   └── settings/       # Settings fragments
│   │   ├── models/             # Data Models (9 files)
│   │   ├── repository/         # Data Repositories (6 files)
│   │   ├── utils/              # Utility Classes (10 files)
│   │   └── viewmodel/          # ViewModels (4 files)
│   ├── src/main/res/           # Resources
│   │   ├── anim/               # Animations
│   │   ├── drawable/           # Vector Drawables & Icons
│   │   ├── font/               # Custom Fonts
│   │   ├── layout/             # XML Layouts (50+ files)
│   │   ├── navigation/         # Navigation Graphs
│   │   └── values/             # Strings, Colors, Themes
│   ├── build.gradle.kts        # App-level Gradle Config
│   └── google-services.json    # Firebase Configuration
├── gradle/                     # Gradle Wrapper
├── build.gradle.kts            # Project-level Gradle Config
├── settings.gradle.kts         # Project Settings
└── README.md                   # Documentation
```

---

## 2. Tech Stack

### Core Technologies
| Category | Technology |
|----------|------------|
| **Language** | Kotlin (Target SDK 35, Min SDK 24) |
| **Architecture** | MVVM with Single Activity + Navigation Component |
| **Dependency Injection** | Dagger Hilt |
| **UI Framework** | Android Views + Material Design 3 + ViewBinding |

### Backend & Cloud Services
| Service | Purpose |
|---------|---------|
| **Firebase Authentication** | User authentication (Admin/Employee) |
| **Firebase Firestore** | Real-time NoSQL database |
| **Firebase Storage** | Media file storage |
| **Firebase Crashlytics** | Crash reporting |

### Local Storage
| Component | Purpose |
|-----------|---------|
| **Room Database** | Local draft product persistence |
| **SharedPreferences** | App configuration |
| **SecurityPreferences** | Encrypted PIN/biometric settings |

### AI & Machine Learning
| Library | Purpose |
|---------|---------|
| **Google Gemini AI** | Chatbot assistance, sales predictions, smart reordering |
| **Google ML Kit** | Barcode scanning with CameraX |

### Networking & Imaging
| Library | Purpose |
|---------|---------|
| **Retrofit + GSON** | API calls (ImgBB image hosting) |
| **Coil** | Image loading |

### Additional Libraries
| Library | Purpose |
|---------|---------|
| **MPAndroidChart** | Sales charts and analytics |
| **Lottie** | Animations (onboarding screens) |
| **OSMDroid** | Map integrations |
| **Androidx Biometric** | Fingerprint/PIN authentication |

---

## 3. Data Models

| Model | Description | Key Fields |
|-------|-------------|------------|
| **Product** | Inventory items | id, name, description, price, stock, category, barcode, imageUrls |
| **Order** | Customer orders | id, productId, quantity, totalPrice, status, soldBy |
| **Bill** | Generated invoices | id, products (CartItem list), customer info, subtotal, tax, total |
| **CartItem** | Shopping cart items | productId, quantity, unitPrice, discount, totalPrice |
| **Customer** | Customer records | id, name, phone, email, address, totalOrders, totalSpent |
| **Employee** | Staff accounts | uid, name, email, phone, role, permissions, userType |
| **LoginLog** | Activity tracking | logId, userId, userType, email, timestamp, deviceName |
| **TaxConfig** | Tax settings | name, rate, isEnabled, currency |
| **DraftProduct** | Offline drafts | Local Room entity for products pending upload |

---

## 4. Key Components

### Activities
1. **MainActivity** - Entry point with onboarding flow, session check
2. **AdminDashboardActivity** - Admin/Manager main dashboard
3. **EmployeeDashboardActivity** - Employee main dashboard
4. **LockScreenActivity** - PIN/Biometric lock screen
5. **AdminLogin/AdminReg** - Admin authentication
6. **EmployeeLogin** - Employee authentication

### Fragments (Key Screens)

**Admin Features:**
- `DashboardOverviewFragment` - Admin analytics dashboard
- `EmployeeListFragment/EmployeeGridFragment` - Employee management
- `AdminSettingsFragment` - Settings hub
- `RBACFragment` - Role-based access control
- `ActivityLogsFragment` - Login activity monitoring
- `TaxConfigFragment` - Tax rate configuration

**Employee Features:**
- `StoreManagerFragment` - Store dashboard for managers
- `InventoryManagerFragment` - Inventory tracking
- `SalesExecutiveFragment` - Sales dashboard
- `CreateBillFragment` - Point of sale billing
- `CreateOrderFragment` - Order creation
- `ChatBotFragment` - AI assistant

**Shared Features:**
- `ProductListFragment/AddProductFragment/UpdateProductFragment` - Product management
- `ProductDetailsFragment` - Product view
- `SalesChartFragment` - Sales analytics
- `SmartReorderFragment` - AI-powered reorder suggestions
- `LowStockFragment` - Low stock alerts

---

## 5. Repositories

| Repository | Purpose |
|------------|---------|
| **AuthRepository** | User registration, login, session management, employee CRUD |
| **ProductRepository** | Product CRUD, barcode lookup, image upload via ImgBB |
| **OrderRepository** | Order creation, status updates, employee order history |
| **BillRepository** | Bill generation, PDF creation, order synchronization |
| **CustomerRepository** | Customer database management, search, statistics |
| **ImgBBService** | Retrofit interface for alternate image hosting |

---

## 6. ViewModels

| ViewModel | Responsibilities |
|-----------|-------------------|
| **AuthViewModel** | Authentication state, user details, employee management |
| **ProductViewModel** | Product list state management |
| **OrderViewModel** | Order list state management |
| **LogsViewModel** | Activity logs state management |

---

## 7. Utility Classes

| Utility | Purpose |
|---------|---------|
| **GeminiHelper** | Google Gemini AI integration for chatbot & predictions |
| **BiometricHelper** | Fingerprint/PIN authentication wrapper |
| **PdfGenerator** | Bill and order PDF generation with Android PdfDocument |
| **BarcodeScannerDialog** | CameraX + ML Kit barcode scanning dialog |
| **NetworkConnectivityObserver** | Real-time network status monitoring |
| **NetworkUtils** | Internet availability checker |
| **SecurityPreferences** | Encrypted preference storage for PIN/biometric settings |
| **BaseActivity** | Base class with network status UI, font theming, session timeout |
| **Resource** | Sealed class for Loading/Success/Error states |

---

## 8. Main Features

### Role-Based Access Control (RBAC)
- **Admin/Store Manager**: Full access to all features
- **Inventory Manager**: Product and stock management
- **Sales Executive**: Billing and order creation

### Intelligent Billing System
- Cart management with quantity and discount tracking
- Barcode scanning for quick product lookup
- PDF bill generation and download
- Customer auto-lookup by phone number

### Inventory Management
- Real-time stock tracking
- Low stock alerts (< 10 units)
- Draft products for offline work
- AI-powered smart reorder suggestions

### Sales & Analytics
- MPAndroidChart-powered sales visualizations
- Top-selling products tracking
- Recent orders monitoring
- Employee sales performance

### Security Features
- Biometric authentication (fingerprint)
- PIN-based app lock with session timeout
- Failed attempt limiting (5 max)
- Secure credential storage

### AI Integration (Gemini)
- Chatbot assistant for store queries
- Product description auto-generation
- Smart inventory reorder suggestions

---

## 9. Navigation Flow

### Admin Navigation (`admin_nav_graph.xml`)
```
DashboardOverviewFragment
├── SalesChartFragment (Analytics)
├── LowStockFragment (Alerts)
├── RecentOrdersFragment
├── TopSellingFragment
├── ProductListFragment
│   ├── AddProductFragment
│   │   └── ProductImageUploadFragment
│   └── ProductDetailsFragment
│       └── UpdateProductFragment
├── EmployeeListFragment
├── AdminSettingsFragment
│   ├── AdminProfileFragment
│   ├── ChangePasswordFragment
│   ├── AppConfigFragment
│   ├── RBACFragment
│   ├── ActivityLogsFragment
│   ├── TaxConfigFragment
│   ├── CustomerListFragment
│   └── SecuritySettingsFragment
└── EmployeeGridFragment
```

### Employee Navigation (`employee_nav_graph.xml`)
```
EmployeeDashboardFragment
├── StoreManagerFragment (Role-specific)
│   ├── ProductDetailsFragment
│   └── CreateBillFragment
├── InventoryManagerFragment (Role-specific)
│   └── SmartReorderFragment
├── SalesExecutiveFragment (Role-specific)
│   ├── CreateOrderFragment
│   ├── CreateBillFragment
│   └── SalesChartFragment
├── ChatBotFragment
└── EmployeeProfileFragment
    └── BillHistoryFragment
```

---

## 10. Database Schema (Firestore Collections)

| Collection | Documents |
|------------|-----------|
| `users` | Admin and employee profiles |
| `products` | Product inventory |
| `orders` | Customer orders |
| `bills` | Generated bills/invoices |
| `customers` | Customer database |
| `login_logs` | Activity tracking |
| `config/tax` | Tax configuration |

---

## 11. Configuration Files

### `build.gradle.kts` (App Level)
- compileSdk: 36, minSdk: 24, targetSdk: 35
- ViewBinding and BuildConfig enabled
- ProGuard enabled for release builds
- Multiple dependencies for Firebase, Hilt, Room, Retrofit, CameraX, ML Kit

### `google-services.json`
- Project ID: `retailx-feca0`
- Package: `com.rajatt7z.ReTailX`

### Local Properties Required
- `API_KEY` - Gemini API key
- `IMGBB_API_KEY` - ImgBB image hosting key

---

## 12. Security Features

1. **Biometric Authentication** - Fingerprint unlock via BiometricPrompt API
2. **PIN Protection** - SHA-256 hashed PIN storage
3. **Session Timeout** - Configurable inactivity timeout (0 = never)
4. **Failed Attempt Lockout** - 5 attempts max before forced re-login
5. **Role-Based Access** - Admin vs Employee permission separation
6. **ProGuard** - Code obfuscation for release builds

---

## 13. Key Files Summary

| File Path | Purpose |
|-----------|---------|
| `app/src/main/java/com/rajatt7z/retailx/ReTailX.kt` | Application class with Hilt injection |
| `app/src/main/java/com/rajatt7z/retailx/di/AppModule.kt` | Hilt dependency injection module |
| `app/src/main/java/com/rajatt7z/retailx/utils/BaseActivity.kt` | Base activity with network monitoring, fonts, session lock |
| `app/src/main/java/com/rajatt7z/retailx/auth/MainActivity.kt` | Entry point with onboarding and session check |
| `app/src/main/java/com/rajatt7z/retailx/fragments/orders/CreateBillFragment.kt` | Core POS/billing functionality |
| `app/src/main/java/com/rajatt7z/retailx/utils/GeminiHelper.kt` | AI integration for chat and predictions |

---

This is a production-grade Android application following modern architectural patterns with comprehensive features for retail management, built with industry-standard libraries and best practices.