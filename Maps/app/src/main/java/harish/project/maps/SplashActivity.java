package harish.project.maps;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.animation.AlphaAnimation;
import android.view.animation.TranslateAnimation;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import androidx.core.content.ContextCompat;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.view.View;
import android.view.animation.OvershootInterpolator;

public class SplashActivity extends BaseActivity {
  private static final long SPLASH_DURATION = 3000; // 3 seconds
  private ImageView splashLogo;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash);

    // Initialize views
    splashLogo = findViewById(R.id.splashLogo);
    final View rootView = findViewById(android.R.id.content);

    // Start animations
    startAnimations();

    // Navigate to Dashboard after delay
    new Handler(Looper.getMainLooper()).postDelayed(() -> {
      // Add exit animation before starting the new activity
      Animation exitAnimation = new AlphaAnimation(1.0f, 0.0f);
      exitAnimation.setDuration(700);
      exitAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
      exitAnimation.setAnimationListener(new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
          // Fade out background color smoothly
          int colorFrom = Color.WHITE;
          int colorTo = Color.TRANSPARENT;
          ValueAnimator colorAnim = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
          colorAnim.setDuration(700);
          colorAnim.addUpdateListener(animator -> rootView.setBackgroundColor((int) animator.getAnimatedValue()));
          colorAnim.start();
        }

        @Override
        public void onAnimationEnd(Animation animation) {
          Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
          startActivity(intent);
          overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
          finish();
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
      });
      splashLogo.startAnimation(exitAnimation);
    }, SPLASH_DURATION - 700); // Start exit animation 700ms before transition
  }

  private void startAnimations() {
    // Create a sequence of animations
    // 1. Initial scale and fade in
    ScaleAnimation scaleAnimation = new ScaleAnimation(
        0.0f, 1.0f, // Start and end X scale
        0.0f, 1.0f, // Start and end Y scale
        Animation.RELATIVE_TO_SELF, 0.5f, // Pivot X
        Animation.RELATIVE_TO_SELF, 0.5f // Pivot Y
    );
    scaleAnimation.setDuration(1400);
    scaleAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

    AlphaAnimation fadeAnimation = new AlphaAnimation(0.0f, 1.0f);
    fadeAnimation.setDuration(1400);
    fadeAnimation.setInterpolator(new AccelerateDecelerateInterpolator());

    // 2. Bounce animation (refined)
    TranslateAnimation bounceAnimation = new TranslateAnimation(
        Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, 0.0f,
        Animation.RELATIVE_TO_SELF, -0.15f);
    bounceAnimation.setStartOffset(1400); // Start after scale/fade
    bounceAnimation.setDuration(1700);
    bounceAnimation.setRepeatCount(1); // Bounce once
    bounceAnimation.setInterpolator(new OvershootInterpolator(1.2f));

    // Combine all animations
    AnimationSet animationSet = new AnimationSet(true);
    animationSet.addAnimation(scaleAnimation);
    animationSet.addAnimation(fadeAnimation);
    animationSet.addAnimation(bounceAnimation);

    // Start the animation
    splashLogo.startAnimation(animationSet);
  }
}