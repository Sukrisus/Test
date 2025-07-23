package com.mycompany.application;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import com.mycompany.application.GLES3JNIView;

public class MainActivity extends Activity {
    
    static{
        System.loadLibrary("WIDGETS_EXPERT");
    }
     
    private static GLES3JNIView glView;
    private static View touchView;
    private static FrameLayout rootLayout;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Start(this);
    }
    
    public static void Start(Context context) {        
        Activity activity = (Activity) context;
        
        // Create root layout
        rootLayout = new FrameLayout(context);
        activity.setContentView(rootLayout);
        
        // Create OpenGL view for ImGui
        glView = new GLES3JNIView(context);
        FrameLayout.LayoutParams glParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        
        // Create transparent touch view overlay
        touchView = new View(context);
        FrameLayout.LayoutParams touchParams = new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 
            ViewGroup.LayoutParams.MATCH_PARENT
        );
        
        // Add views to layout
        rootLayout.addView(glView, glParams);
        rootLayout.addView(touchView, touchParams);

        // Set up touch handling
        touchView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int action = event.getAction();
                switch (action) {
                    case MotionEvent.ACTION_MOVE:
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        GLES3JNIView.MotionEventClick(action != MotionEvent.ACTION_UP, event.getX(), event.getY());
                        break;
                    default:
                        break;
                }
                return true;
            }
        });
        
        // Update window positioning periodically
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    String rect[] = GLES3JNIView.getWindowRect().split("\\|");
                    // Update any positioning if needed
                } catch (Exception e) {
                    // Ignore errors
                }
                handler.postDelayed(this, 20);
            }
        }, 20);          
    }
}