# Vertex AI License Plate Detection Troubleshooting Guide

## Common Issues and Solutions

### 1. "No license plate detected" Error

This error can occur for several reasons. Here's how to troubleshoot:

#### A. API Key Issues
- **Problem**: Invalid or missing API key
- **Solution**: 
  1. Check your `Config.java` file
  2. Ensure `GOOGLE_CLOUD_API_KEY` is set to your actual API key
  3. Verify the API key has Vision API enabled
  4. Check if the API key has proper permissions

#### B. Image Quality Issues
- **Problem**: Poor image quality or no text in image
- **Solution**:
  1. Ensure the license plate is clearly visible
  2. Check that the image contains readable text
  3. Try with a high-resolution image
  4. Make sure the license plate is not blurry or at an extreme angle

#### C. API Response Issues
- **Problem**: API returns empty or invalid response
- **Solution**:
  1. Check the debug logs for API response details
  2. Verify the API endpoint is correct
  3. Check if you've exceeded API quotas

### 2. Using the Debug Tool

I've added a debug tool to help troubleshoot issues:

1. **Open the Debug Activity**:
   - Go to License Plate Activity
   - Tap the "Debug" button
   - This will show API key status and configuration

2. **Check API Key Status**:
   - The debug tool will show if your API key is properly configured
   - It will display the key length and first few characters
   - Verify it's not showing the placeholder value

3. **Test with Sample Image**:
   - Select an image with a clear license plate
   - Use the "Test API Key" button
   - Check the debug output for detailed logs

### 3. Step-by-Step Troubleshooting

#### Step 1: Verify API Key Configuration
```java
// In Config.java, ensure you have:
public static final String GOOGLE_CLOUD_API_KEY = "your_actual_api_key_here";
```

#### Step 2: Enable Required APIs
1. Go to Google Cloud Console
2. Navigate to APIs & Services > Library
3. Enable these APIs:
   - Cloud Vision API
   - Vertex AI API (if using Vertex AI endpoint)

#### Step 3: Check API Key Permissions
1. Go to APIs & Services > Credentials
2. Find your API key
3. Ensure it has access to Vision API
4. Check if there are any restrictions that might block the requests

#### Step 4: Test with Debug Tool
1. Open the app
2. Go to License Plate Activity
3. Tap "Debug" button
4. Check the status output
5. Select a test image
6. Run the API test
7. Review the detailed logs

#### Step 5: Check Logs
Look for these log messages:
- `"Starting Vision OCR with API key"`
- `"API Response Code: 200"` (should be 200 for success)
- `"Full text detected: [text]"`
- `"License plate found: [plate]"`

### 4. Common Error Codes

#### 400 Bad Request
- Check your request format
- Verify the image is properly encoded in base64
- Ensure the API key is valid

#### 403 Forbidden
- API key doesn't have permission for Vision API
- API key is restricted
- Billing not enabled

#### 429 Too Many Requests
- You've exceeded your API quota
- Wait and try again later
- Consider upgrading your quota

### 5. Testing with Different Images

Try these types of images:
1. **Clear license plate photo** - Best case scenario
2. **License plate from different angles** - Test robustness
3. **License plate with different lighting** - Test adaptability
4. **Image with multiple text elements** - Test text extraction
5. **Low-quality image** - Test error handling

### 6. Manual API Testing

You can test the API manually using curl:

```bash
curl -X POST \
  https://vision.googleapis.com/v1/images:annotate?key=YOUR_API_KEY \
  -H 'Content-Type: application/json' \
  -d '{
    "requests": [
      {
        "image": {
          "content": "base64_encoded_image_here"
        },
        "features": [
          {
            "type": "TEXT_DETECTION",
            "maxResults": 10
          }
        ]
      }
    ]
  }'
```

### 7. Regex Pattern Issues

The current regex patterns are designed for Indian license plates. If you're testing with different formats, you may need to adjust the patterns in `VertexAIService.java`:

```java
// Current patterns for Indian plates:
"\\b[A-Z]{2}[\\s-]?[0-9]{1,2}[\\s-]?[A-Z]{1,2}[\\s-]?[0-9]{1,4}\\b"
"\\b[A-Z]{2}[\\s-]?[0-9]{1,2}[\\s-]?[0-9]{1,4}\\b"
```

### 8. Getting Help

If you're still having issues:

1. **Check the debug logs** - They contain detailed information
2. **Verify your API key** - Make sure it's valid and has proper permissions
3. **Test with a simple image** - Use a clear, high-quality license plate image
4. **Check your internet connection** - API calls require internet access
5. **Review the error messages** - They often contain specific information about what went wrong

### 9. Alternative Solutions

If Vertex AI continues to have issues:

1. **Use a different OCR service** (Google Vision API directly, Tesseract, etc.)
2. **Implement manual input** as a fallback
3. **Use a pre-trained license plate detection model**
4. **Implement image preprocessing** to improve text detection

### 10. Performance Optimization

To improve detection accuracy:

1. **Image preprocessing**:
   - Resize images to optimal resolution
   - Apply contrast enhancement
   - Remove noise and blur

2. **Multiple attempts**:
   - Try different image orientations
   - Use multiple OCR services
   - Implement confidence scoring

3. **User feedback**:
   - Allow manual correction
   - Provide clear error messages
   - Guide users on how to take better photos 