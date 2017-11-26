package uk.co.dphin.albumview.ui.android;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewAnimator;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.displayers.Displayer;
import uk.co.dphin.albumview.logic.Controller;
import uk.co.dphin.albumview.logic.Dimension;

/**
 * Created by peter on 24/11/17.
 */

public class AlbumPlayNotes extends AlbumPlay {

    private TextView headingView;
    private TextView dateView;
    private TextView captionView;

    private SimpleDateFormat dateFormat;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.albumplaynotes);

        final ViewAnimator switcher = (ViewAnimator)findViewById(R.id.switcher);
        setSwitcher(switcher);

        // Wait for the layout to initialise so we can get the image dimensions
        ViewTreeObserver vto = switcher.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // Load & display the image
                Controller.getController().setSize(Displayer.Size_Notes, new Dimension(switcher.getWidth(), switcher.getHeight()));

                // Prevent this from repeating on future updates
                ViewTreeObserver obs = switcher.getViewTreeObserver();
                obs.removeOnGlobalLayoutListener(this);

                displayInitialSlide();
            }
        });

        // Set up the previous & next buttons
        Button prevButton = (Button)findViewById(R.id.prevButton);
        Button nextButton = (Button)findViewById(R.id.nextButton);

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSlide(false);
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeSlide(true);
            }
        });
    }

    public void onStart() {
        super.onStart();



        headingView = (TextView)findViewById(R.id.heading);
        dateView = (TextView)findViewById(R.id.date);
        captionView = (TextView)findViewById(R.id.caption);

        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z", Locale.getDefault());
    }

    protected void cleanUpBeforeSlideChange()
    {
        headingView.setText("");
        dateView.setText("");
        captionView.setText("");
    }

    protected void postSlideChange(Displayer newDisplayer)
    {
        String heading = newDisplayer.getSlide().getHeading();
        if (heading.isEmpty()) {
            headingView.setVisibility(View.GONE);
        } else {
            headingView.setVisibility(View.VISIBLE);
            headingView.setText(heading);
        }
        Date date = newDisplayer.getSlide().getDate();
        if (date != null)
        {
            dateView.setText(dateFormat.format(date));
        }
        String caption = newDisplayer.getSlide().getCaption();
        if (caption.isEmpty()) {
            captionView.setVisibility(View.GONE);
        } else {
            captionView.setVisibility(View.VISIBLE);
            captionView.setText(caption);
        }

    }

    protected int getDisplaySize()
    {
        return Displayer.Size_Notes;
    }
}
