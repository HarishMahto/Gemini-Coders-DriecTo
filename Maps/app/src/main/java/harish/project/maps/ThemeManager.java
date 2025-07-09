package harish.project.maps;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class ThemeManager {
  private static final String PREFS_NAME = "ThemePrefs";
  private static final String COLOR_THEME_KEY = "color_theme";

  public static final String THEME_BLUE = "blue";
  public static final String THEME_GREEN = "green";
  public static final String THEME_ORANGE = "orange";

  public enum Theme {
    GREEN, ORANGE, BLUE
  }

  public static void applyTheme(Activity activity, Theme theme) {
    switch (theme) {
      case GREEN:
        activity.setTheme(R.style.Theme_Maps_Green);
        break;
      case ORANGE:
        activity.setTheme(R.style.Theme_Maps_Orange);
        break;
      case BLUE:
        activity.setTheme(R.style.Theme_Maps_Blue);
        break;
    }
    activity.recreate();
  }

  public static void setTheme(Context context, String theme) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    prefs.edit().putString(COLOR_THEME_KEY, theme).apply();
  }

  public static String getCurrentTheme(Context context) {
    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    return prefs.getString(COLOR_THEME_KEY, THEME_ORANGE);
  }

  public static int getThemeResourceId(String theme) {
    switch (theme) {
      case THEME_BLUE:
        return R.style.Theme_Maps_Blue;
      case THEME_GREEN:
        return R.style.Theme_Maps_Green;
      case THEME_ORANGE:
      default:
        return R.style.Theme_Maps_Orange;
    }
  }
}