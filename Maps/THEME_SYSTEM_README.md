# Theme System Documentation

## Overview
This Android app now supports 3 different color themes (Blue, Green, and Orange) that can be switched from the Settings activity. The theme system is implemented using Material Design color attributes and applies consistently across all activities.

## Features

### Color Themes
- **Blue Theme**: Uses blue primary and accent colors
- **Green Theme**: Uses green primary and accent colors  
- **Orange Theme**: Uses orange primary and accent colors (default)

### Theme Switching
- Users can switch between themes in the Settings activity
- Theme selection is persisted using SharedPreferences
- Theme changes are applied immediately with activity restart
- Visual indicators show the primary color of each theme

## Implementation Details

### Files Modified/Created

#### Core Theme Files
- `app/src/main/res/values/colors.xml` - Added color definitions for all themes
- `app/src/main/res/values/themes.xml` - Created theme styles for each color variant
- `app/src/main/java/harish/project/maps/ThemeManager.java` - Utility class for theme management
- `app/src/main/java/harish/project/maps/BaseActivity.java` - Base activity that applies themes automatically

#### Settings Integration
- `app/src/main/res/layout/activity_settings.xml` - Added color theme selection UI
- `app/src/main/java/harish/project/maps/SettingsActivity.java` - Added theme switching logic
- `app/src/main/res/drawable/blue_theme_indicator.xml` - Blue theme color indicator
- `app/src/main/res/drawable/green_theme_indicator.xml` - Green theme color indicator
- `app/src/main/res/drawable/orange_theme_indicator.xml` - Orange theme color indicator

#### Layout Files Updated for Theme Support
- `app/src/main/res/layout/activity_dashboard.xml` - Updated to use theme attributes
- `app/src/main/res/layout/activity_credit_store.xml` - Updated to use theme attributes
- `app/src/main/res/layout/activity_analytics.xml` - Updated to use theme attributes
- `app/src/main/res/layout/activity_account.xml` - Updated to use theme attributes
- `app/src/main/res/layout/activity_help.xml` - Updated to use theme attributes
- `app/src/main/res/layout/item_voucher.xml` - Updated to use theme attributes
- `app/src/main/res/color/bottom_nav_color.xml` - Updated to use theme attributes

#### Activities Updated
All activities now extend `BaseActivity` instead of `AppCompatActivity` to automatically apply themes:
- MainActivity
- DashboardActivity
- SplashActivity
- LoginActivity
- SignUpActivity
- SettingsActivity
- AccountActivity
- MyVoucherActivity
- LicensePlateActivity
- HelpActivity
- GeminiChatActivity
- CreditStoreActivity
- ArticlesActivity
- AnalyticsActivity

### Color Schemes

#### Blue Theme
- Primary: #2196F3 (Material Blue)
- Primary Dark: #1976D2
- Primary Light: #BBDEFB
- Accent: #03A9F4 (Light Blue)
- Accent Dark: #0288D1

#### Green Theme
- Primary: #4CAF50 (Material Green)
- Primary Dark: #388E3C
- Primary Light: #C8E6C9
- Accent: #8BC34A (Light Green)
- Accent Dark: #689F38

#### Orange Theme
- Primary: #FF9800 (Material Orange)
- Primary Dark: #F57C00
- Primary Light: #FFE0B2
- Accent: #FF5722 (Deep Orange)
- Accent Dark: #E64A19

## Usage

### For Users
1. Open the app and navigate to Settings
2. In the "Color Theme" section, select your preferred theme
3. The app will restart and apply the new theme immediately
4. Your theme choice is saved and will persist across app launches

### For Developers
To add a new activity that supports themes:
1. Make the activity extend `BaseActivity` instead of `AppCompatActivity`
2. The theme will be applied automatically

To add a new theme:
1. Add color definitions to `colors.xml`
2. Create a new theme style in `themes.xml`
3. Add theme constants to `ThemeManager.java`
4. Update the settings UI to include the new theme option

To update layouts for theme support:
1. Replace hardcoded colors with theme attributes:
   - `@color/orange` → `?attr/colorPrimary`
   - `@android:color/holo_orange_dark` → `?attr/colorPrimary`
   - Custom accent colors → `?attr/colorSecondary`
2. Use `?attr/colorPrimary` for primary UI elements
3. Use `?attr/colorSecondary` for secondary UI elements
4. Keep neutral colors (black, white, gray) as they are

## Technical Notes
- Themes are applied before `setContentView()` to ensure proper styling
- Theme changes trigger an activity restart to apply the new theme completely
- The system uses Material Design color attributes for consistency
- All theme data is stored in SharedPreferences for persistence
- Layout files have been updated to use theme attributes instead of hardcoded colors
- Bottom navigation and other UI elements now respond to theme changes 