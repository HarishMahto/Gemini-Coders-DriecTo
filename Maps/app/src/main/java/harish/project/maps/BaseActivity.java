package harish.project.maps;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public abstract class BaseActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    // Apply theme from SharedPreferences before calling super.onCreate
    String colorTheme = ThemeManager.getCurrentTheme(this);
    setTheme(ThemeManager.getThemeResourceId(colorTheme));
    super.onCreate(savedInstanceState);
  }
}