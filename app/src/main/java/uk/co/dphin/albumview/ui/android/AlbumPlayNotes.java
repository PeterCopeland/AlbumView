package uk.co.dphin.albumview.ui.android;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.drew.lang.GeoLocation;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.displayers.Displayer;
import uk.co.dphin.albumview.displayers.android.AndroidDisplayer;
import uk.co.dphin.albumview.logic.Controller;
import uk.co.dphin.albumview.logic.Dimension;
import uk.co.dphin.albumview.models.ImageSlide;
import uk.co.dphin.albumview.models.Slide;
import uk.co.dphin.albumview.net.android.OutgoingRequestHandler;

/**
 * Created by peter on 24/11/17.
 */

public class AlbumPlayNotes extends AlbumPlay {

    private TextView headingView;
    private TextView dateView;
    private TextView captionView;

    private ViewGroup prevButton;
    private ViewGroup nextButton;

    private MapView mapButton;
    private GoogleMap map;

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
        prevButton = (ViewGroup)findViewById(R.id.prevButton);
        nextButton = (ViewGroup)findViewById(R.id.nextButton);
        mapButton = (MapView)findViewById(R.id.mapView);

        prevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OutgoingRequestHandler.getOutgoingRequestHandler().requestPrevSlide();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OutgoingRequestHandler.getOutgoingRequestHandler().requestNextSlide();
            }
        });

        mapButton.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                map = googleMap;
            }
        });
        mapButton.onCreate(savedInstanceState);
    }

    public void onStart() {
        super.onStart();

        headingView = (TextView)findViewById(R.id.heading);
        dateView = (TextView)findViewById(R.id.date);
        captionView = (TextView)findViewById(R.id.caption);

        dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z", Locale.getDefault());

        mapButton.onStart();
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

        // Refresh the prev/next thumbnails
        prevButton.removeAllViews();
        nextButton.removeAllViews();
        if (map != null)
            map.clear();
        List<Slide> slides = getAlbum().getSlides();
        int currentSlide = getIndex();
        if (currentSlide > 0)
        {
            AndroidDisplayer prevSlideDisplayer = (AndroidDisplayer)slides.get(currentSlide-1).getDisplayer();
            prevSlideDisplayer.setPlayContext(this);
            if (!prevSlideDisplayer.isSizeLoaded(Displayer.Size_Medium))
                prevSlideDisplayer.load(Displayer.Size_Medium);
            View prevSlideView = prevSlideDisplayer.getView(Displayer.Size_Medium);
            prevSlideView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            prevButton.addView(prevSlideView);
            prevButton.invalidate();
        }
        if (currentSlide + 1 < slides.size())
        {
            AndroidDisplayer nextSlideDisplayer = (AndroidDisplayer)slides.get(currentSlide+1).getDisplayer();
            nextSlideDisplayer.setPlayContext(this);
            if (!nextSlideDisplayer.isSizeLoaded(Displayer.Size_Medium))
                nextSlideDisplayer.load(Displayer.Size_Medium);
            View nextSlideView = nextSlideDisplayer.getView(Displayer.Size_Medium);
            nextSlideView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
            nextButton.addView(nextSlideView);
            nextButton.invalidate();
        }

        if (map != null && getCurrentSlide() instanceof ImageSlide)
        {
            ImageSlide is = (ImageSlide)getCurrentSlide();
            GeoLocation location = is.getCoordinates();
            if (location != null)
            {
                MarkerOptions markerOptions = new MarkerOptions();
                LatLng newPosition = new LatLng(
                        location.getLatitude(),
                        location.getLongitude()
                );
                markerOptions.position(newPosition);
                map.addMarker(markerOptions);
                CameraUpdate cameraMove = CameraUpdateFactory.newLatLng(newPosition);
                map.animateCamera(cameraMove);
            }
        }



    }

    protected int getDisplaySize()
    {
        return Displayer.Size_Notes;
    }


    @Override
    public void onStop() {
        super.onStop();

        mapButton.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mapButton.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mapButton.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mapButton.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mapButton.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();

        mapButton.onLowMemory();
    }
}
