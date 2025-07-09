# Firebase Storage Upload Fix

## Issue: "failed to upload object does not present at location"

This error occurs when Firebase Storage security rules are blocking uploads. Here's how to fix it:

## Step 1: Update Firebase Storage Rules

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project "driecto"
3. Go to **Storage** → **Rules** tab
4. Replace the existing rules with:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    // Allow authenticated users to read and write emergency images
    match /emergency_images/{imageId} {
      allow read, write: if request.auth != null;
    }
    
    // Allow authenticated users to read and write emergency alerts
    match /emergency_alerts/{userId}/{imageId} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Default rule - deny all other access
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

5. Click **Publish**

## Step 2: Enable Firebase Storage

1. Go to **Storage** in Firebase Console
2. If Storage is not initialized, click **Get Started**
3. Choose a location for your storage bucket
4. Start in test mode (you can change rules later)

## Step 3: Verify Authentication

Make sure the user is authenticated before uploading:
- The app should be logged in
- Firebase Auth should be working properly

## Step 4: Test the Fix

1. Run the app
2. Try the SOS emergency feature
3. Take a photo
4. The upload should now work

## Alternative: Temporary Open Rules (FOR TESTING ONLY)

If you want to test without authentication temporarily:

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if true;
    }
  }
}
```

⚠️ **WARNING**: Never use open rules in production!

## What We Fixed in the Code:

1. **Simplified Storage Path**: Changed from nested paths to simpler structure
2. **Added Metadata**: Proper content type for images
3. **Better Error Handling**: Specific error messages for different failures
4. **Improved Progress Tracking**: More detailed progress updates
5. **Simplified Database Path**: Flatter structure for easier access

## Storage Structure:

```
emergency_images/
├── emergency_1705123456789.jpg
├── emergency_1705123456790.jpg
└── ...

emergency_alerts/
├── alertId1
│   ├── type: "Accident"
│   ├── imageUrl: "https://..."
│   ├── timestamp: "2024-01-15T10:30:00Z"
│   ├── userId: "user123"
│   ├── status: "active"
│   └── location: {...}
└── alertId2
    └── ...
```

The upload should now work properly! 🎉 