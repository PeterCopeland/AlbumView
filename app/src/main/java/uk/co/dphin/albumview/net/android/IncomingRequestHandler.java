package uk.co.dphin.albumview.net.android;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.nearby.connection.Payload;
import com.google.android.gms.nearby.connection.PayloadCallback;
import com.google.android.gms.nearby.connection.PayloadTransferUpdate;

import java.util.Collection;
import java.util.HashSet;

import uk.co.dphin.albumview.listeners.SlideChangeListener;
import uk.co.dphin.albumview.models.*;
import uk.co.dphin.albumview.net.action.Action;
import uk.co.dphin.albumview.net.action.MoveToNextSlide;
import uk.co.dphin.albumview.net.action.MoveToPreviousSlide;
import uk.co.dphin.albumview.net.action.OpenAlbum;
import uk.co.dphin.albumview.net.action.SelectSlide;
import uk.co.dphin.albumview.ui.android.AlbumPlayPaused;

/**
 * Created by peter on 26/11/17.
 */

public class IncomingRequestHandler extends PayloadCallback {

    private static IncomingRequestHandler incomingRequestHandler;

    private Activity activity;

    private Collection<SlideChangeListener> slideChangeListeners;

    private IncomingRequestHandler(Activity activity) {
        this.activity = activity;

        slideChangeListeners = new HashSet<>();
    }


    /**
     * Initialise a new request handler
     *
     * @param activity
     *
     * @return
     */
    public static IncomingRequestHandler initialise(Activity activity)
    {
        incomingRequestHandler = new IncomingRequestHandler(activity);
        return incomingRequestHandler;
    }

    /**
     * Get the incoming request handler
     *
     * @return
     */
    public static IncomingRequestHandler getIncomingRequestHandler()
    {
        if (incomingRequestHandler == null)
        {
            throw new IllegalStateException("Call setupRequestHandler first");
        }

        return incomingRequestHandler;
    }

    public void handleAction(Action action)
    {
        if (action instanceof OpenAlbum)
        {
            openAlbum((Album)action.getAdditionalData());
        }
        else if (action instanceof SelectSlide)
        {
            selectSlide((Slide)action.getAdditionalData());
        }
        else if (action instanceof MoveToNextSlide) {
            nextSlide();
        }
        else if (action instanceof MoveToPreviousSlide) {
            prevSlide();
        }
        else
        {
            Log.w("RequestHandler", "Unknown action type: " + action.getClass().getSimpleName());
        }
    }

    private void openAlbum(Album album)
    {
        Intent intent = new Intent(activity, AlbumPlayPaused.class);
        intent.putExtra("album", album.getID());
        activity.startActivity(intent);
    }

    private void selectSlide(Slide slide)
    {
        for (SlideChangeListener listener : slideChangeListeners)
        {
            listener.selectSlide(slide);
        }
    }

    private void nextSlide()
    {
        for (SlideChangeListener listener : slideChangeListeners)
        {
            listener.nextSlide();
        }
    }

    private void prevSlide()
    {
        for (SlideChangeListener listener : slideChangeListeners)
        {
            listener.prevSlide();
        }
    }

    /**
     * Register an object to receive notifications when a slide changes
     *
     * @param listener
     */
    public void registerSlideChangeListener(SlideChangeListener listener)
    {
        slideChangeListeners.add(listener);
    }

    /**
     * Stop sending notifications of slide changes to an object
     *
     * @param listener
     */
    public void unregisterSlideChangeListener(SlideChangeListener listener)
    {
        slideChangeListeners.remove(listener);
    }

    @Override
    public void onPayloadReceived(String s, Payload payload) {
        Log.d("Networking", "Received payload");
        Log.d("Networking", payload.toString());
    }

    @Override
    public void onPayloadTransferUpdate(String s, PayloadTransferUpdate payloadTransferUpdate) {

    }
}
