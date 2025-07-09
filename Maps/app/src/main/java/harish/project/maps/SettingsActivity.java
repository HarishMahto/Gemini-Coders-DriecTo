package harish.project.maps;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

public class SettingsActivity extends BaseActivity {
  private static final String PREFS_NAME = "SettingsPrefs";
  private static final String THEME_KEY = "theme";
  private static final String NOTIFICATIONS_KEY = "notifications";
  private static final String SOUND_KEY = "sound";
  private static final String VIBRATION_KEY = "vibration";

  private SharedPreferences preferences;
  private RadioGroup themeRadioGroup;
  private RadioGroup colorThemeRadioGroup;
  private SwitchCompat notificationSwitch;
  private SwitchCompat soundSwitch;
  private SwitchCompat vibrationSwitch;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_settings);

    // Initialize SharedPreferences
    preferences = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

    // Initialize views
    ImageButton backButton = findViewById(R.id.backButton);
    themeRadioGroup = findViewById(R.id.themeRadioGroup);
    colorThemeRadioGroup = findViewById(R.id.colorThemeRadioGroup);
    notificationSwitch = findViewById(R.id.notificationSwitch);
    soundSwitch = findViewById(R.id.soundSwitch);
    vibrationSwitch = findViewById(R.id.vibrationSwitch);

    // Setup back button
    backButton.setOnClickListener(v -> finish());

    // Load saved settings
    loadSettings();

    // Setup dark/light mode change listener
    themeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
      int theme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
      if (checkedId == R.id.lightTheme) {
        theme = AppCompatDelegate.MODE_NIGHT_NO;
      } else if (checkedId == R.id.darkTheme) {
        theme = AppCompatDelegate.MODE_NIGHT_YES;
      }
      AppCompatDelegate.setDefaultNightMode(theme);
      preferences.edit().putInt(THEME_KEY, theme).apply();
    });

    // Setup color theme change listener
    colorThemeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
      String colorTheme = ThemeManager.THEME_ORANGE;
      if (checkedId == R.id.blueTheme) {
        colorTheme = ThemeManager.THEME_BLUE;
      } else if (checkedId == R.id.greenTheme) {
        colorTheme = ThemeManager.THEME_GREEN;
      } else if (checkedId == R.id.orangeTheme) {
        colorTheme = ThemeManager.THEME_ORANGE;
      }

      ThemeManager.setTheme(this, colorTheme);

      // Restart the activity to apply the new theme
      Intent intent = new Intent(this, SettingsActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(intent);
      finish();
    });

    // Setup switch listeners
    notificationSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> preferences.edit().putBoolean(NOTIFICATIONS_KEY, isChecked).apply());

    soundSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> preferences.edit().putBoolean(SOUND_KEY, isChecked).apply());

    vibrationSwitch.setOnCheckedChangeListener(
        (buttonView, isChecked) -> preferences.edit().putBoolean(VIBRATION_KEY, isChecked).apply());
  }

  private void loadSettings() {
    // Load dark/light theme
    int savedTheme = preferences.getInt(THEME_KEY, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    int radioButtonId = R.id.systemTheme;
    if (savedTheme == AppCompatDelegate.MODE_NIGHT_NO) {
      radioButtonId = R.id.lightTheme;
    } else if (savedTheme == AppCompatDelegate.MODE_NIGHT_YES) {
      radioButtonId = R.id.darkTheme;
    }
    themeRadioGroup.check(radioButtonId);

    // Load color theme
    String savedColorTheme = ThemeManager.getCurrentTheme(this);
    int colorRadioButtonId = R.id.orangeTheme;
    if (savedColorTheme.equals(ThemeManager.THEME_BLUE)) {
      colorRadioButtonId = R.id.blueTheme;
    } else if (savedColorTheme.equals(ThemeManager.THEME_GREEN)) {
      colorRadioButtonId = R.id.greenTheme;
    } else if (savedColorTheme.equals(ThemeManager.THEME_ORANGE)) {
      colorRadioButtonId = R.id.orangeTheme;
    }
    colorThemeRadioGroup.check(colorRadioButtonId);

    // Load other settings
    notificationSwitch.setChecked(preferences.getBoolean(NOTIFICATIONS_KEY, true));
    soundSwitch.setChecked(preferences.getBoolean(SOUND_KEY, true));
    vibrationSwitch.setChecked(preferences.getBoolean(VIBRATION_KEY, true));
  }
}