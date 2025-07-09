# Firebase Setup Guide

## Issue: "Permission denied" when writing to Firebase Database

The error indicates that Firebase Database security rules are blocking write operations. Here's how to fix it:

## Step 1: Enable Authentication Providers

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project "driecto"
3. Go to **Authentication** → **Sign-in method**
4. Enable these providers:
   - **Email/Password** ✅
   - **Google** ✅
   - **Anonymous** ✅ (for testing)

## Step 2: Update Database Security Rules

1. Go to **Realtime Database** → **Rules** tab
2. Replace the existing rules with:

```json
{
  "rules": {
    ".read": "auth != null",
    ".write": "auth != null",
    "users": {
      "$uid": {
        ".read": "auth != null && auth.uid == $uid",
        ".write": "auth != null && auth.uid == $uid"
      }
    },
    "traffic_junctions": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "traffic_history": {
      ".read": "auth != null",
      ".write": "auth != null"
    },
    "test": {
      ".read": "auth != null",
      ".write": "auth != null"
    }
  }
}
```

3. Click **Publish**

## Step 3: Verify Google Sign-In Setup

1. Go to **Authentication** → **Sign-in method** → **Google**
2. Make sure it's enabled
3. Add your app's SHA-1 fingerprint:
   - Get SHA-1: `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android`
   - Add it to the Google Sign-In provider

## Step 4: Test Again

1. Run the app
2. The FirebaseTestActivity should now show:
   - ✓ Firebase App initialized
   - ✓ Firebase Auth initialized
   - ✓ Anonymous authentication successful
   - ✓ Database write successful
   - 🎉 All Firebase services working correctly!

## Alternative: Temporary Open Rules (FOR TESTING ONLY)

If you want to test without authentication temporarily:

```json
{
  "rules": {
    ".read": true,
    ".write": true
  }
}
```

⚠️ **WARNING**: Never use open rules in production!

## Next Steps

Once the test passes:
1. Switch back to SplashActivity as launcher
2. Test login/signup functionality
3. Firebase should now work properly for authentication 