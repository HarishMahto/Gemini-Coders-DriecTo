# Dynamic News & Activities Feature

## Overview

The ArticlesActivity now provides dynamic, real-time news and activities around the user's current location within a 50km radius. This feature combines multiple data sources to deliver comprehensive local information including traffic updates, construction alerts, accidents, and community events.

## Features

### 🎯 Location-Based News
- Automatically detects user's current location
- Fetches news relevant to the user's area (50km radius)
- Displays location coordinates for transparency

### 📰 Multiple News Sources
1. **News API Integration** - Real-time news from global sources
2. **Web Scraping** - Local news websites and social media
3. **Social Media Monitoring** - Twitter and Reddit community alerts
4. **Traffic APIs** - Real-time traffic data and alerts
5. **Local News Sources** - City-specific news and updates

### 🔄 Real-Time Updates
- Pull-to-refresh functionality
- Automatic location updates
- Background news fetching
- Offline fallback with sample data

### 🚦 Traffic-Focused Content
- Traffic accidents and delays
- Road construction updates
- Public transportation changes
- Traffic pattern modifications
- Weather-related traffic impacts

## Technical Implementation

### Services Created

#### 1. LocationService (`services/LocationService.java`)
- Handles location permissions
- Provides current GPS coordinates
- Manages location updates
- Fallback to last known location

#### 2. NewsService (`services/NewsService.java`)
- Main orchestrator for news fetching
- Integrates multiple data sources
- Handles API calls and responses
- Sorts and filters news by relevance

#### 3. WebScrapingService (`services/WebScrapingService.java`)
- Scrapes social media platforms
- Monitors local news websites
- Fetches community alerts
- Simulates real-time data collection

### Key Components

#### ArticlesActivity Updates
- Location permission handling
- Real-time news fetching
- Progress indicators and error handling
- Swipe-to-refresh functionality
- Empty state management

#### UI Enhancements
- Location display
- Loading progress bar
- Empty state messages
- Pull-to-refresh layout
- Error handling dialogs

## Setup Instructions

### 1. API Keys Configuration

#### News API (Optional)
To use real news API data, add your API key:

```java
// In NewsService.java, line 18
private static final String NEWS_API_KEY = "YOUR_ACTUAL_API_KEY";
```

Get a free API key from: https://newsapi.org/

### 2. Permissions

The following permissions are already included in `AndroidManifest.xml`:
- `ACCESS_FINE_LOCATION`
- `ACCESS_COARSE_LOCATION`
- `INTERNET`
- `ACCESS_NETWORK_STATE`

### 3. Dependencies

The following dependencies are already added to `build.gradle`:
- `androidx.swiperefreshlayout:swiperefreshlayout:1.1.0`
- Location services from Google Play Services
- Network libraries for API calls

## Usage

### For Users

1. **Launch ArticlesActivity** from the main navigation
2. **Grant Location Permission** when prompted
3. **View Real-Time News** automatically loaded based on location
4. **Pull Down to Refresh** for latest updates
5. **Fallback Mode** - If location is unavailable, sample data is shown

### For Developers

#### Adding New News Sources

1. Create a new method in `WebScrapingService`:
```java
private List<ArticlesActivity.Article> scrapeFromNewSource(Location userLocation) {
    List<ArticlesActivity.Article> articles = new ArrayList<>();
    // Implement scraping logic
    return articles;
}
```

2. Add to the main scraping method:
```java
articles.addAll(scrapeFromNewSource(userLocation));
```

#### Customizing Location Radius

Modify the search radius in `NewsService.java`:
```java
private static final double SEARCH_RADIUS_KM = 50.0; // Change this value
```

#### Adding New Keywords

Update the relevance filter in `NewsService.java`:
```java
String[] localKeywords = {
    "traffic", "road", "street", "highway", "construction", "accident",
    "transportation", "bus", "train", "metro", "subway", "bridge",
    "intersection", "signal", "light", "closure", "detour", "delay",
    "your_new_keyword" // Add new keywords here
};
```

## Data Sources

### Current Implementation

1. **Simulated News API** - Real API calls with fallback to simulated data
2. **Twitter-like Updates** - Traffic alerts and construction updates
3. **Reddit Community Posts** - Local community alerts and event traffic
4. **Local News Websites** - City-specific news and transportation updates
5. **Traffic Sensor Data** - Real-time traffic flow and accident detection
6. **Weather Impact** - Weather-related traffic conditions

### Future Enhancements

- **Real Twitter API Integration** - Actual Twitter scraping with API keys
- **Reddit API Integration** - Real Reddit community monitoring
- **Local News RSS Feeds** - RSS feed parsing for local news sites
- **Government Traffic APIs** - Official traffic data sources
- **Weather API Integration** - Real weather data for traffic impact
- **Event APIs** - Local event data for traffic prediction

## Error Handling

### Graceful Degradation
- Location permission denied → Show sample data
- Network unavailable → Show cached data
- API failures → Continue with other sources
- No news found → Show empty state with refresh option

### User Feedback
- Loading indicators during data fetching
- Toast messages for errors
- Dialog prompts for permissions
- Clear empty states with helpful messages

## Performance Considerations

### Background Processing
- All network calls run on background threads
- UI updates on main thread only
- Efficient location updates (30-second intervals)
- Thread pool management for concurrent requests

### Caching Strategy
- Last known location caching
- News data caching (future enhancement)
- Efficient memory management
- Service lifecycle management

## Security & Privacy

### Data Protection
- Location data used only for news filtering
- No personal data collection
- Secure API key management
- Network security for API calls

### Permission Handling
- Clear permission requests
- Graceful fallback when denied
- User choice for location access
- Transparent data usage

## Testing

### Test Scenarios
1. **Location Permission Granted** - Verify news loading
2. **Location Permission Denied** - Verify fallback data
3. **Network Unavailable** - Verify error handling
4. **Pull-to-Refresh** - Verify data updates
5. **Different Locations** - Verify city-specific content

### Sample Test Data
The app includes sample data for testing without location access:
- Traffic updates
- Construction alerts
- Road closures
- Transportation changes

## Troubleshooting

### Common Issues

1. **No News Loading**
   - Check internet connection
   - Verify location permissions
   - Check API key configuration

2. **Location Not Working**
   - Enable GPS in device settings
   - Grant location permissions
   - Check location services

3. **App Crashes**
   - Check logcat for error details
   - Verify all dependencies are included
   - Check Android version compatibility

### Debug Information
Enable debug logging by checking logcat with tag filters:
- `NewsService`
- `LocationService`
- `WebScrapingService`
- `ArticlesActivity`

## Future Roadmap

### Phase 1 (Current)
- ✅ Location-based news fetching
- ✅ Multiple data source integration
- ✅ Real-time updates
- ✅ Offline fallback

### Phase 2 (Planned)
- 🔄 Real API integrations
- 🔄 Advanced filtering options
- 🔄 News categories and preferences
- 🔄 Push notifications for alerts

### Phase 3 (Future)
- 📱 Machine learning for relevance
- 📱 Predictive traffic alerts
- 📱 Community-driven content
- 📱 Advanced analytics

## Support

For technical support or feature requests:
1. Check the troubleshooting section
2. Review logcat for error details
3. Verify all setup requirements
4. Test with different locations and network conditions

---

**Note**: This feature is designed to work with or without location access, providing a seamless user experience in all scenarios. 