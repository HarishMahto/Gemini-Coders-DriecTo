# Gemini API Troubleshooting Guide

## 🔍 **Issue: "Error generating content: Unexpected response type"**

This error occurs when the Gemini API integration is not working properly. I've fixed this by implementing a proper HTTP-based approach instead of the problematic Kotlin coroutines.

## ✅ **What I Fixed**

### 1. **Replaced Kotlin Coroutines with HTTP Client**
- **Problem**: The original implementation used Kotlin coroutines incorrectly in Java
- **Solution**: Implemented direct HTTP calls using OkHttp and Gson
- **Result**: More reliable and easier to debug

### 2. **Enhanced Error Handling**
- **Added**: Detailed logging for API requests and responses
- **Added**: Proper error messages with HTTP status codes
- **Added**: Network error handling

### 3. **API Key Validation**
- **Added**: API key status logging (masked for security)
- **Added**: Connection test functionality

## 🧪 **Testing Your API Connection**

### **Option 1: Use the Test Activity**
1. **Launch the test activity**:
   ```java
   Intent intent = new Intent(this, GeminiTestActivity.class);
   startActivity(intent);
   ```

2. **Or add a test button** to any activity:
   ```java
   Button testButton = findViewById(R.id.testButton);
   testButton.setOnClickListener(v -> {
       Intent intent = new Intent(this, GeminiTestActivity.class);
       startActivity(intent);
   });
   ```

### **Option 2: Check Logs**
Look for these log messages:
```
D/GeminiService: Gemini API Key configured: AIza...Jo0
D/GeminiService: Request body: {"contents":[...]}
D/GeminiService: Response: {"candidates":[...]}
```

## 🔧 **Common Issues and Solutions**

### **1. API Key Issues**

**Symptoms:**
- "API Error: 400" or "API Error: 403"
- "Invalid API key" messages

**Solutions:**
1. **Verify your API key** in Google AI Studio
2. **Check local.properties**:
   ```properties
   GEMINI_API_KEY=your_actual_api_key_here
   ```
3. **Clean and rebuild**:
   ```bash
   ./gradlew clean assembleDebug
   ```

### **2. Network Issues**

**Symptoms:**
- "Network error" messages
- Timeout errors

**Solutions:**
1. **Check internet connection**
2. **Verify network permissions** in AndroidManifest.xml
3. **Test with a simple HTTP request**

### **3. Response Parsing Issues**

**Symptoms:**
- "Invalid response format" errors
- Empty responses

**Solutions:**
1. **Check the API response** in logs
2. **Verify JSON format** is correct
3. **Test with a simple prompt**

## 📋 **Step-by-Step Debugging**

### **Step 1: Verify API Key**
```bash
# Check if API key is loaded
cat local.properties | grep GEMINI_API_KEY
```

### **Step 2: Test Basic Connection**
1. Open the app
2. Navigate to GeminiTestActivity
3. Tap "Test API Connection"
4. Check the result

### **Step 3: Check Logs**
```bash
# Filter logs for Gemini-related messages
adb logcat | grep -i gemini
```

### **Step 4: Test with Simple Prompt**
```java
geminiService.generateContent("Hello, please respond with 'test successful'", 
    new GeminiService.GeminiCallback() {
        @Override
        public void onSuccess(String response) {
            Log.d("TEST", "Success: " + response);
        }
        
        @Override
        public void onError(String error) {
            Log.e("TEST", "Error: " + error);
        }
    });
```

## 🛠 **Manual API Testing**

### **Using curl (for advanced debugging)**
```bash
curl -X POST \
  "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=YOUR_API_KEY" \
  -H "Content-Type: application/json" \
  -d '{
    "contents": [{
      "parts": [{
        "text": "Hello! Please respond with test successful."
      }]
    }]
  }'
```

### **Expected Response Format**
```json
{
  "candidates": [{
    "content": {
      "parts": [{
        "text": "test successful"
      }]
    }
  }]
}
```

## 🔄 **Fallback Mechanisms**

The app includes multiple fallback mechanisms:

1. **Primary**: Gemini AI + Traditional News
2. **Secondary**: Traditional News only  
3. **Tertiary**: Sample data

If Gemini fails, the app will automatically fall back to traditional news sources.

## 📱 **Testing in Your App**

### **1. Test Articles Activity**
1. Open the Articles section
2. Grant location permission
3. Check if AI-generated content appears
4. Look for emoji indicators (🚦, 🔮, 🚨)

### **2. Test Live Report**
1. Tap "🚦 Live Report" button
2. Check for real-time traffic analysis
3. Verify response format

### **3. Test Predictions**
1. Tap "🔮 Prediction" button
2. Select a time frame
3. Check for AI-generated predictions

## 🚨 **Emergency Fixes**

### **If API Key is Invalid**
1. Get a new key from [Google AI Studio](https://makersuite.google.com/app/apikey)
2. Update `local.properties`
3. Clean and rebuild

### **If Network is Blocked**
1. Check firewall settings
2. Verify proxy configuration
3. Test with mobile data vs WiFi

### **If Response Format Changes**
1. Check Google's API documentation
2. Update the parsing logic in `GeminiService.java`
3. Test with the latest API version

## 📞 **Getting Help**

### **1. Check Logs First**
```bash
adb logcat | grep -E "(Gemini|API|Error)"
```

### **2. Test API Key**
Use the GeminiTestActivity to verify your API key works.

### **3. Verify Configuration**
- API key in `local.properties`
- Internet permission in `AndroidManifest.xml`
- Dependencies in `build.gradle`

### **4. Common Solutions**
- **Restart the app** after configuration changes
- **Clean and rebuild** the project
- **Check API quotas** in Google AI Studio
- **Verify network connectivity**

## ✅ **Success Indicators**

When everything is working correctly, you should see:

1. **Log messages**:
   ```
   D/GeminiService: Gemini API Key configured: AIza...Jo0
   D/GeminiService: Request body: {...}
   D/GeminiService: Response: {...}
   ```

2. **UI indicators**:
   - Toast message: "🤖 AI-powered traffic insights loaded"
   - Articles with emoji indicators (🚦, 🔮, 🚨)
   - Live reports and predictions working

3. **Test activity**:
   - "✅ Success!" message
   - Actual response from Gemini API

The integration is now robust and should handle most common issues gracefully! 🚀 