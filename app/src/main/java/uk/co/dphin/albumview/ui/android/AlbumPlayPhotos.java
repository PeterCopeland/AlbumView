package uk.co.dphin.albumview.ui.android;

import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ViewAnimator;

import java.io.File;

import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.displayers.Displayer;
import uk.co.dphin.albumview.displayers.ImageDisplayer;
import uk.co.dphin.albumview.logic.Controller;
import uk.co.dphin.albumview.logic.Dimension;
import uk.co.dphin.albumview.models.ImageSlide;
import uk.co.dphin.albumview.net.action.MoveToNextSlide;
import uk.co.dphin.albumview.net.action.MoveToPreviousSlide;
import uk.co.dphin.albumview.net.android.OutgoingRequestHandler;

/**
 * Created by peter on 25/11/17.
 */

public class AlbumPlayPhotos extends AlbumPlay implements GestureDetector.OnGestureListener {

    private GestureDetector gestureDetect;

    private DisplayMetrics metrics;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.albumplayphotos);

        // Hide the UI
        findViewById(R.id.frame).setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE | View.SYSTEM_UI_FLAG_LOW_PROFILE);

        metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);

        Controller.getController().setSize(Displayer.Size_Screen, new Dimension(metrics.widthPixels, metrics.heightPixels));

        setSwitcher((ViewAnimator)findViewById(R.id.switcher));

        gestureDetect = new GestureDetector(this, this, null);
    }

    public void onResume() {
        super.onResume();

        displayInitialSlide();
    }

    public boolean onTouchEvent(MotionEvent e)
    {
        this.gestureDetect.onTouchEvent(e);
        return super.onTouchEvent(e);
    }

    public boolean onDown(MotionEvent e) { return true; }

    /**
     * Detect taps on the screen to change to previous/next slide
     *
     * @param e
     *
     * @return
     */
    public boolean onSingleTapUp(MotionEvent e) {
        // A tap on the right means next slide, a tap on the left means previous slide
        int tapLoc = (int)e.getX();
        boolean forwards = tapLoc >= metrics.widthPixels/2;

        if (forwards)
        {
            OutgoingRequestHandler.getOutgoingRequestHandler().handleAction(new MoveToNextSlide());
        }
        else
        {
            OutgoingRequestHandler.getOutgoingRequestHandler().handleAction(new MoveToPreviousSlide());
        }
        return true;
    }

    public void onLongPress(MotionEvent e) {}

    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX,
                            float distanceY) { return false; }

    public void onShowPress(MotionEvent e) {}

    public boolean onFling(MotionEvent p1, MotionEvent p2, float p3, float p4)
    {
        return false;
    }



    /**
     * Cleans up extra bits on the screen before changing slides
     */
    protected void cleanUpBeforeSlideChange()
    {
        // Hide the panorama button
        Button openPano = (Button)findViewById(R.id.openPanoButton);
        openPano.setVisibility(Button.INVISIBLE);
    }

    protected void postSlideChange(Displayer newDisplayer)
    {
        // Show the open panorama button if needed
        if (newDisplayer instanceof ImageDisplayer && newDisplayer.isPanoramic())
        {
            Button openPano = (Button)findViewById(R.id.openPanoButton);
            openPano.setVisibility(openPano.VISIBLE);
        }
    }

    public void openPanorama(View view)
    {
        ImageSlide is = (ImageSlide)getCurrentSlide();
        if (is == null)
            return;

        Intent panoIntent = new Intent(AlbumPlayPhotos.this, PanoView.class);
        panoIntent.putExtra("uri", is.getFile().getUri());
        startActivity(panoIntent);
    }

    protected int getDisplaySize()
    {
        return Displayer.Size_Screen;
    }
}
