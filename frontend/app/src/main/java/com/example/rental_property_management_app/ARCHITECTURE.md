# RoomCare - Android Architecture Guide

## Package Structure

### 📱 **activities/**
Chứa các Activity classes
- `MainActivity.kt` - Activity chính, chứa NavHostFragment

### 🖼️ **fragments/**
Chứa các Fragment classes (UI Layer)
- Authentication: LoginFragment, RegisterFragment, ForgotPasswordFragment
- Landlord: LandlordDashboardFragment, PropertyManagementFragment, ...
- Tenant: TenantDashboardFragment, RoomInfoFragment, ...

### 🔄 **adapters/**
Chứa RecyclerView Adapters cho danh sách
- PropertyAdapter, TenantAdapter, ContractAdapter, ...

### 📦 **models/**
Chứa Data Models/Entities
- User, Property, Room, Contract, Invoice, ...
- Request/Response DTOs cho API

### 🗄️ **repositories/**
Chứa Repository classes (Data Layer)
- AuthRepository, UserRepository, PropertyRepository, ...
- Trung gian giữa ViewModel và Data Sources (API, Database, SharedPreferences)

### 🎯 **viewmodels/**
Chứa ViewModel classes (Business Logic Layer)
- AuthViewModel, LandlordViewModel, TenantViewModel, ...
- Quản lý UI state và business logic

### 🌐 **network/**
Chứa Retrofit API interfaces và networking
- ApiService.kt - Retrofit API interface
- RetrofitClient.kt - Retrofit singleton instance
- AuthInterceptor.kt - JWT token interceptor

### 🛠️ **utils/**
Chứa utility classes và helper functions
- Constants.kt - App constants
- DateUtils.kt - Date formatting
- CurrencyUtils.kt - VND formatting
- SharedPreferencesManager.kt - Token storage
- ValidationUtils.kt - Input validation

### 🧭 **navigation/**
Chứa Navigation helpers (nếu cần)
- NavigationExtensions.kt - Navigation helper functions

## Architecture Flow

```
Fragment → ViewModel → Repository → Network/Database
   ↑                                      ↓
   └──────────────── StateFlow/LiveData ──┘
```

## MVVM Pattern

1. **View (Fragment)** - Hiển thị UI và observe data từ ViewModel
2. **ViewModel** - Quản lý UI logic và state, không reference View
3. **Repository** - Quản lý data sources (API, Database, Cache)
4. **Network** - API calls thông qua Retrofit

## Best Practices

- ✅ Sử dụng View Binding trong Fragment/Activity
- ✅ Sử dụng Kotlin Coroutines cho async operations
- ✅ Sử dụng StateFlow/LiveData cho reactive UI updates
- ✅ Sử dụng Repository pattern để tách biệt data layer
- ✅ Không truy cập trực tiếp API từ ViewModel
- ✅ Không reference Activity/Fragment từ ViewModel
