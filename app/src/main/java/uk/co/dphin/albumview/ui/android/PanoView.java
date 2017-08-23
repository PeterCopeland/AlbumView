package uk.co.dphin.albumview.ui.android;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.logic.Dimension;

/**
 * Created by peter on 22/08/17.
 */

public class PanoView extends Activity {

    public static final int SCROLL_HORIZONTAL = 1;
    public static final int SCROLL_VERTICAL = 2;

    /**
     * The image path to display
     */
    private File imagePath;

    /**
     * The dimensions of the full-size image
     */
    private Dimension imageDimensions;

    /**
     * The screen dimensions
     */
    private Dimension screenDimensions;

    /**
     * The dimensions of the rendered image - to fit the screen on one axis
     */
    private Dimension renderDimensions;

    /**
     * The orientation of the panorama - SCROLL_HORIZONTAL or SCROLL_VERTICAL
     */
    private int orientation;

    /**
     * Rendered bitmap at required size
     */
    private Bitmap bitmap;

    /**
     * Maximum texture size for OpenGL.
     * Actually varies by device, but this should cover most without complex calls
     */
    private static final int MAX_SIZE = 1024;

    /**
     * Get the slide this activity will display, and check its dimensions
     */
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.panoview);

        imagePath = (File)(getIntent().getSerializableExtra("file"));
        // TODO: Move image handling code to a separate class. AndroidImageDisplayer is too coupled to the activities
        BitmapFactory.Options metadataOpts = new BitmapFactory.Options();
        metadataOpts.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(imagePath.getAbsolutePath(), metadataOpts);
        imageDimensions = new Dimension(metadataOpts.outWidth, metadataOpts.outHeight);
        screenDimensions = new Dimension(2048,1536); // TODO: Screen dimensions

        // This ignores cases where the panorama's aspect ratio is smaller than the screen.
        // In those cases we don't expect to end up on a PanoView, but I guess it could happen
        /**
         * Ratio at which the image can be loaded. Will be an integer, 2 to load at half size
         */
        int ratio;
        /**
         * Scaling ratio for the image. Will be <= 1, 0.5 to display at half size
         */
        double scaleRatio;
        if (imageDimensions.width > imageDimensions.height) {
            // Horizontal panorama, fit height to screen
            orientation = SCROLL_HORIZONTAL;
            ratio = Math.round((float)imageDimensions.height / (float)screenDimensions.height);
            scaleRatio = (double)screenDimensions.height / (double)imageDimensions.height;
            renderDimensions = new Dimension(imageDimensions.width * ratio, screenDimensions.height);
        } else {
            // Vertical panorama, fit width to screen
            // Screen will be rotated to vertical, so match the width to screen height
            orientation = SCROLL_VERTICAL;
            ratio = Math.round((float)imageDimensions.width / (float)screenDimensions.height);
            scaleRatio = (double)screenDimensions.width / (double)imageDimensions.width;
            renderDimensions = new Dimension(imageDimensions.height * ratio, screenDimensions.height);
        }

        // Load a bitmap at this size

//        bitmap = Bitmap.createScaledBitmap(preScaleBitmap, renderDimensions.width, renderDimensions.height, false);

        try {
        final ImageView panoImageView = (ImageView)this.findViewById(R.id.panoImageView);
            panoImageView.setImageDrawable(createLargeDrawable(imagePath, scaleRatio));
            panoImageView.setOnTouchListener(new View.OnTouchListener()
            {
                float startX, startY;
                int totalX, totalY;
                int scrollByX, scrollByY;
                float currentX, currentY, mx, my;

                public boolean onTouch (View arg0, MotionEvent event)
                {

                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            // Start drag event
                            startX = currentX = event.getX();
                            startY = currentY = event.getY();
                            totalX = totalY = 0;
                            break;
                        case MotionEvent.ACTION_MOVE:
                            // Move event
                            // TODO: Kinetic scroll
                            if (orientation == SCROLL_HORIZONTAL) {
                                scrollByX = (int) (currentX - event.getX());
                                scrollByY = 0;
                                totalX += scrollByX;
                            } else {
                                scrollByX = 0;
                                scrollByY = (int) (currentY - event.getY());
                                totalY += scrollByY;
                            }

                            currentX = event.getX();
                            currentY = event.getY();

                            // TODO: Don't allow scrolling beyond edge of image

                            panoImageView.scrollBy(scrollByX, scrollByY);
                            break;
                    }

                    return true;
                }
            });
            panoImageView.getLayoutParams().width = renderDimensions.width;
            panoImageView.getLayoutParams().height = renderDimensions.height;
            panoImageView.requestLayout();
        } catch (IOException e) {
            Toast.makeText(this, "Could not load panorama", Toast.LENGTH_LONG).show();
            finish();
        }


    }

    /**
     * Draw the image on the screen, shortest dimension filling the corresponding screen size
     *
     * TODO: Remember where the user was looking
     */
    protected void onStart() {
        super.onStart();
    }

    /**
     * Enable dragging by the longest dimension only
     */
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Split a large bitmap into multiple drawable objects for diwplay
     *
     * Modified from
     * https://stackoverflow.com/questions/15655713/dealing-with-androids-texture-size-limit
     * (main change is that we scale the image to fit the screen first)
     *
     * @param source Image file to load
     * @param scaleRatio Ratio to scale the image by to fit the screen
     * @return
     */
    private Drawable createLargeDrawable(File source, double scaleRatio) throws IOException
    {
        // Scale the input bitmap to fit the screen
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        options.inSampleSize = 1; // TODO
        Bitmap preScaleBitmap = BitmapFactory.decodeFile(imagePath.getAbsolutePath(), options);
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(
                preScaleBitmap,
                renderDimensions.width,
                renderDimensions.height,
                false
        );

        if (scaledBitmap.getWidth() <= (MAX_SIZE) && scaledBitmap.getHeight() <= (MAX_SIZE)) {
            return new BitmapDrawable(getResources(), scaledBitmap);
        }

        int rowCount = (int)Math.ceil((float)scaledBitmap.getHeight() / (float)MAX_SIZE);
        int colCount = (int)Math.ceil((float)scaledBitmap.getWidth() / (float)MAX_SIZE);

        BitmapDrawable[] drawables = new BitmapDrawable[rowCount * colCount];

        int top, bottom, left, right, width, height;
        for (int i = 0; i < rowCount; i++) {
            top = MAX_SIZE * i;
            if (i == rowCount - 1) {
                bottom = scaledBitmap.getHeight();
            } else {
                bottom = top + MAX_SIZE;
            }
            height = bottom - top;

            for (int j = 0; j < colCount; j++) {
                left = MAX_SIZE * j;
                if (j == colCount - 1) {
                    right = scaledBitmap.getWidth();
                } else {
                    right = left + MAX_SIZE;
                }
                width = right - left;

                Bitmap b = Bitmap.createBitmap(scaledBitmap, left, top, width, height);
                BitmapDrawable bd = new BitmapDrawable(getResources(), b);
                bd.setGravity(Gravity.TOP | Gravity.LEFT);
                drawables[i * colCount + j] = bd;
            }
        }

        LayerDrawable ld = new LayerDrawable(drawables);
        for (int i = 0; i < rowCount; i++) {
            for (int j = 0; j < colCount; j++) {
                ld.setLayerInset(
                        i*colCount + j,
                        MAX_SIZE * j,
                        MAX_SIZE * i,
                        0,
                        0
                );
            }
        }

        return ld;
    }
}
