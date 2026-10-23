package com.example.vs_project.ui.views;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

public class InteractiveEyeView extends View {

    private Paint scleraPaint;
    private Paint irisPaint;
    private Paint pupilPaint;
    private Paint eyelidPaint;
    private Paint tearPaint;
    
    private Path eyelidPath;
    private Path tearPath;

    
    private float pupilOffsetX = 0f;
    private float pupilOffsetY = 0f;
    private float eyelidClosure = 0f; 
    private float tearY = -1f; 
    private float alphaGlow = 1f;

    public InteractiveEyeView(Context context) {
        super(context);
        init();
    }

    public InteractiveEyeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private Path scleraPath;
    
    private void init() {
        scleraPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        scleraPaint.setColor(Color.WHITE);
        scleraPaint.setStyle(Paint.Style.FILL);
        scleraPaint.setShadowLayer(10f, 0, 0, Color.parseColor("#33007AFF"));

        irisPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        irisPaint.setColor(Color.parseColor("#007AFF")); 
        irisPaint.setStyle(Paint.Style.FILL);

        pupilPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pupilPaint.setColor(Color.parseColor("#001A33")); 
        pupilPaint.setStyle(Paint.Style.FILL);

        eyelidPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        
        eyelidPaint.setColor(Color.parseColor("#F2F7FD")); 
        eyelidPaint.setStyle(Paint.Style.FILL);

        tearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        tearPaint.setColor(Color.parseColor("#88007AFF")); 
        tearPaint.setStyle(Paint.Style.FILL);

        eyelidPath = new Path();
        tearPath = new Path();
        scleraPath = new Path();
        
        
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        
        startAutoBlink();
    }
    
    private final android.os.Handler blinkHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private final Runnable blinkRunnable = new Runnable() {
        @Override
        public void run() {
            
            if (eyelidClosure == 0f) {
                quickBlink();
            }
            
            int nextBlink = 3000 + (int)(Math.random() * 3000);
            blinkHandler.postDelayed(this, nextBlink);
        }
    };

    private void startAutoBlink() {
        blinkHandler.postDelayed(blinkRunnable, 2000);
    }
    
    private void quickBlink() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f, 0f); 
        anim.setDuration(350); 
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            eyelidClosure = (float) animation.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        blinkHandler.removeCallbacks(blinkRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        
        
        float eyeWidth = Math.min(w, h) * 0.8f;
        float eyeHeight = eyeWidth * 0.65f; 
        
        float left = cx - eyeWidth / 2f;
        float right = cx + eyeWidth / 2f;

        canvas.save();

        
        scleraPath.reset();
        scleraPath.moveTo(left, cy);
        
        scleraPath.quadTo(cx, cy - eyeHeight, right, cy);
        
        scleraPath.quadTo(cx, cy + eyeHeight, left, cy);
        scleraPath.close();

        
        canvas.drawPath(scleraPath, scleraPaint);
        
        
        
        canvas.clipPath(scleraPath);

        
        float irisRadius = eyeHeight * 0.45f;
        
        float currentIrisX = cx + (pupilOffsetX * eyeWidth * 0.25f);
        float currentIrisY = cy + (pupilOffsetY * eyeHeight * 0.25f);
        canvas.drawCircle(currentIrisX, currentIrisY, irisRadius, irisPaint);

        
        float pupilRadius = irisRadius * 0.4f;
        canvas.drawCircle(currentIrisX, currentIrisY, pupilRadius, pupilPaint);

        
        
        canvas.restore();
        canvas.save();

        
        if (tearY >= 0) {
            float tX = cx + (eyeWidth * 0.3f);
            float tY = cy + (tearY * h); 
            
            tearPath.reset();
            tearPath.moveTo(tX, tY - 15);
            tearPath.cubicTo(tX + 15, tY, tX + 15, tY + 20, tX, tY + 20);
            tearPath.cubicTo(tX - 15, tY + 20, tX - 15, tY, tX, tY - 15);
            canvas.drawPath(tearPath, tearPaint);
        }

        
        if (eyelidClosure > 0) {
            float closureAmount = eyeHeight * eyelidClosure; 
            
            
            eyelidPath.reset();
            eyelidPath.moveTo(left - 20, cy);
            
            eyelidPath.quadTo(cx, cy - eyeHeight + closureAmount, right + 20, cy);
            
            eyelidPath.lineTo(right + 20, 0);
            eyelidPath.lineTo(left - 20, 0);
            eyelidPath.close();
            canvas.drawPath(eyelidPath, eyelidPaint);

            
            float bottomClosure = closureAmount * 0.3f; 
            eyelidPath.reset();
            eyelidPath.moveTo(left - 20, cy);
            
            eyelidPath.quadTo(cx, cy + eyeHeight - bottomClosure, right + 20, cy);
            
            eyelidPath.lineTo(right + 20, h);
            eyelidPath.lineTo(left - 20, h);
            eyelidPath.close();
            canvas.drawPath(eyelidPath, eyelidPaint);
        }

        canvas.restore();
    }

    

    public void lookNormal() {
        animatePupil(0f, 0f);
        animateEyelid(0f);
    }

    public void lookDown() {
        animatePupil(0f, 1f); 
        animateEyelid(0.2f); 
    }

    public void closeEye() {
        animatePupil(0f, 0f);
        animateEyelid(1.1f); 
    }

    public void cry() {
        
        animatePupil(0.5f, 0.5f);
        animateEyelid(0.3f);
        
        
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1.2f);
        anim.setDuration(1500);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            tearY = (float) animation.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                tearY = -1f; 
                invalidate();
            }
        });
        anim.start();
    }

    public void lookLeftRight() {
        ValueAnimator anim = ValueAnimator.ofFloat(0f, -1f, 1f, -1f, 0f);
        anim.setDuration(1000);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            pupilOffsetX = (float) animation.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }

    public void powerDown() {
        ValueAnimator anim = ValueAnimator.ofFloat(1f, 0.2f);
        anim.setDuration(1500);
        anim.addUpdateListener(animation -> {
            setAlpha((float) animation.getAnimatedValue());
        });
        anim.start();
        closeEye(); 
    }

    private void animatePupil(float targetX, float targetY) {
        ValueAnimator animX = ValueAnimator.ofFloat(pupilOffsetX, targetX);
        ValueAnimator animY = ValueAnimator.ofFloat(pupilOffsetY, targetY);
        
        ValueAnimator.AnimatorUpdateListener listener = animation -> {
            if (animation == animX) pupilOffsetX = (float) animation.getAnimatedValue();
            if (animation == animY) pupilOffsetY = (float) animation.getAnimatedValue();
            invalidate();
        };
        
        animX.addUpdateListener(listener);
        animY.addUpdateListener(listener);
        
        animX.setDuration(300);
        animY.setDuration(300);
        animX.setInterpolator(new OvershootInterpolator());
        animY.setInterpolator(new OvershootInterpolator());
        
        animX.start();
        animY.start();
    }

    private void animateEyelid(float targetClosure) {
        ValueAnimator anim = ValueAnimator.ofFloat(eyelidClosure, targetClosure);
        anim.setDuration(300);
        anim.setInterpolator(new AccelerateDecelerateInterpolator());
        anim.addUpdateListener(animation -> {
            eyelidClosure = (float) animation.getAnimatedValue();
            invalidate();
        });
        anim.start();
    }
}
