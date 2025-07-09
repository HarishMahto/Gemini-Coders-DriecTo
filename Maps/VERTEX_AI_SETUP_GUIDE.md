# Vertex AI Implementation Guide for License Plate Recognition

This guide will help you set up Vertex AI for license plate recognition in your Android app.

## Prerequisites

1. Google Cloud Platform (GCP) account
2. A GCP project with billing enabled
3. Android Studio with your project

## Step 1: Set Up Google Cloud Project

### 1.1 Create a GCP Project
1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable billing for the project

### 1.2 Enable Required APIs
Enable the following APIs in your GCP project:
- **Cloud Vision API** - For OCR functionality
- **Vertex AI API** - For advanced AI features (optional)

```bash
# Using gcloud CLI
gcloud services enable vision.googleapis.com
gcloud services enable aiplatform.googleapis.com
```

### 1.3 Create Service Account (Optional - for backend method)
If you want to use the backend token method:

1. Go to IAM & Admin > Service Accounts
2. Create a new service account
3. Grant the following roles:
   - Cloud Vision API User
   - Vertex AI User
4. Create and download a JSON key file
5. Save it as `service-account.json` in your backend directory

## Step 2: Get API Keys

### 2.1 Google Cloud API Key (Recommended)
1. Go to APIs & Services > Credentials
2. Click "Create Credentials" > "API Key"
3. Copy the API key
4. Restrict the API key to:
   - Cloud Vision API
   - Your Android app's package name

### 2.2 Update Configuration
Open `app/src/main/java/harish/project/maps/Config.java` and update:

```java
public class Config {
    // Replace with your actual API key
    public static final String GOOGLE_CLOUD_API_KEY = "YOUR_ACTUAL_API_KEY_HERE";
    
    // Replace with your actual project ID
    public static final String VERTEX_AI_PROJECT_ID = "YOUR_PROJECT_ID_HERE";
    
    // Update backend URL if using backend method
    public static final String BACKEND_URL = "http://YOUR_BACKEND_IP:3000";
}
```

## Step 3: Backend Setup (Optional)

If you prefer to use the backend token method:

### 3.1 Install Python Dependencies
```bash
pip install fastapi uvicorn google-auth google-auth-oauthlib google-auth-httplib2
```

### 3.2 Update Backend Configuration
Update `main.py`:

```python
from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
from google.oauth2 import service_account
import google.auth.transport.requests

app = FastAPI()

SCOPES = ['https://www.googleapis.com/auth/cloud-platform']
SERVICE_ACCOUNT_FILE = 'service-account.json'  # Path to your downloaded key
API_KEY = "your-strong-random-api-key"  # Set this to a strong value

@app.get("/token")
def get_token(request: Request):
    if request.headers.get("x-api-key") != API_KEY:
        raise HTTPException(status_code=401, detail="Unauthorized")
    try:
        credentials = service_account.Credentials.from_service_account_file(
            SERVICE_ACCOUNT_FILE, scopes=SCOPES)
        auth_req = google.auth.transport.requests.Request()
        credentials.refresh(auth_req)
        return JSONResponse(content={"access_token": credentials.token})
    except Exception as e:
        return JSONResponse(status_code=500, content={"error": str(e)})

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=3000)
```

### 3.3 Run Backend
```bash
python main.py
```

## Step 4: Android App Configuration

### 4.1 Update Dependencies
The required dependencies are already added to `build.gradle`:

```gradle
// Vertex AI and Google Cloud dependencies
implementation 'com.google.cloud:google-cloud-aiplatform:3.45.0'
implementation 'com.google.auth:google-auth-library-oauth2-http:1.23.0'
implementation 'com.google.auth:google-auth-library-credentials:1.23.0'
implementation 'com.google.api-client:google-api-client:2.2.0'
implementation 'com.google.apis:google-api-services-vision:v1-rev20230926-2.0.0'

// JSON parsing
implementation 'com.google.code.gson:gson:2.10.1'
```

### 4.2 Sync Project
Sync your project with Gradle files in Android Studio.

## Step 5: Testing the Implementation

### 5.1 Test with API Key Method
1. Update `Config.java` with your API key
2. Run the app
3. Take a photo of a license plate
4. The app will use the API key directly

### 5.2 Test with Backend Method
1. Start your backend server
2. Update `Config.java` with your backend URL
3. Run the app
4. The app will fetch tokens from your backend

## Step 6: License Plate Recognition Features

The implementation includes:

### 6.1 Pattern Recognition
The system recognizes common Indian license plate formats:
- `KA-01-AB-1234` or `KA01AB1234`
- `KA-01-1234` or `KA011234`
- `AB-12-CD-1234`
- `12-AB-1234`

### 6.2 Fallback Mechanisms
- Primary: Backend token method
- Fallback: Direct API key method
- Error handling for network issues

### 6.3 Image Processing
- Automatic image conversion to base64
- Optimized image handling
- Error handling for corrupted images

## Step 7: Security Best Practices

### 7.1 API Key Security
- Restrict API keys to specific APIs and apps
- Use Android app restrictions
- Never commit API keys to version control

### 7.2 Backend Security
- Use strong API keys for backend authentication
- Implement rate limiting
- Use HTTPS in production

### 7.3 Error Handling
- Implement proper error handling
- Log errors for debugging
- Provide user-friendly error messages

## Troubleshooting

### Common Issues

1. **API Key Not Working**
   - Check if the API key is restricted correctly
   - Verify the API is enabled in GCP
   - Check billing status

2. **Backend Connection Issues**
   - Verify backend is running
   - Check network connectivity
   - Verify API key in backend configuration

3. **No License Plate Detected**
   - Ensure image quality is good
   - Check if license plate is clearly visible
   - Try different lighting conditions

4. **Build Errors**
   - Sync project with Gradle files
   - Check dependency versions
   - Clean and rebuild project

### Debug Mode
Enable debug logging by checking the Android logs:
```bash
adb logcat | grep VertexAIService
```

## Performance Optimization

1. **Image Compression**: Consider compressing images before sending to API
2. **Caching**: Cache results for similar images
3. **Batch Processing**: Process multiple images in batches
4. **Offline Mode**: Implement offline OCR using ML Kit as fallback

## Cost Optimization

1. **API Usage**: Monitor API usage in GCP Console
2. **Image Quality**: Optimize image quality vs. file size
3. **Caching**: Cache results to avoid duplicate API calls
4. **Quotas**: Set up usage quotas to control costs

## Support

For issues related to:
- **Google Cloud APIs**: Check [Google Cloud Documentation](https://cloud.google.com/docs)
- **Android Implementation**: Check Android logs and debug output
- **Backend Issues**: Check server logs and network connectivity 