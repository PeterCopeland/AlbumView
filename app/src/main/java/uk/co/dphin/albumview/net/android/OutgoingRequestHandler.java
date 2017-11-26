package uk.co.dphin.albumview.net.android;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.List;

import uk.co.dphin.albumview.models.Album;
import uk.co.dphin.albumview.models.Slide;

/**
 * Handles requests made from this client, and sends them out to the network
 */
public class OutgoingRequestHandler {

    private static OutgoingRequestHandler outgoingRequestHandler;

    public static final int MODE_STANDALONE = 0;

    public static final int MODE_HOST = 10;

    public static final int MODE_CLIENT = 20;

    public static final String ACTION_OPENALBUM = "action.album.open";
    public static final String ACTION_SLIDE_NEXT = "action.slide.next";
    public static final String ACTION_SLIDE_PREV = "action.slide.prev";

    /**
     * List of connected client IDs. Will only be populated if this is the host.
     */
    private List<String> endpointIds;

    /**
     * Endpoint ID of the host. Will only be populated if this is the client.
     */
    private String hostEndpoint;

    private int mode;

    private OutgoingRequestHandler() {
        mode = MODE_STANDALONE;
    }

    public static OutgoingRequestHandler getOutgoingRequestHandler()
    {
        if (outgoingRequestHandler == null)
        {
            outgoingRequestHandler = new OutgoingRequestHandler();
        }

        return outgoingRequestHandler;
    }

    public void setMode(int mode)
    {
        this.mode = mode;
    }

    /**
     * Request all clients to open the specified album
     *
     * For now, only the host can do this because albums are only stored on the host.
     * It needs to stream the full contents of the album, but images are only filenames at this point
     *
     * @todo Strip filenames - potential security risk?
     *
     * @param album Allbum to open
     */
    public void requestOpenAlbum(Album album)
    {
        switch(mode) {
            case MODE_STANDALONE:
                IncomingRequestHandler.getIncomingRequestHandler().openAlbum(album);
                break;

            case MODE_HOST:
                Payload request = Payload.fromBytes(ACTION_OPENALBUM.getBytes());
                sendPayloadToClients(request);

                try {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream out = new ObjectOutputStream(bos);
                    out.writeObject(album);
                    Payload payload = Payload.fromBytes(bos.toByteArray());

                    sendPayloadToClients(payload);


                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case MODE_CLIENT:
                // For now, don't do anything - only hosts can

        }
    }

    /**
     * Select a particular slide within the current album
     *
     * @param slide
     */
    public void requestSelectSlide(Slide slide)
    {
        switch (mode)
        {
            case MODE_STANDALONE:
                IncomingRequestHandler.getIncomingRequestHandler().selectSlide(slide);
        }
    }

    /**
     * Move to the next slide
     */
    public void requestNextSlide()
    {
        Payload request;

        switch (mode)
        {
            case MODE_STANDALONE:
                IncomingRequestHandler.getIncomingRequestHandler().nextSlide();
                break;
            case MODE_HOST:
                request = Payload.fromBytes(ACTION_SLIDE_NEXT.getBytes());
                sendPayloadToClients(request);
                break;
            case MODE_CLIENT:
                request = Payload.fromBytes(ACTION_SLIDE_NEXT.getBytes());
                sendPayloadToHost(request);
                break;
        }
    }

    /**
     * Move to the next slide
     */
    public void requestPrevSlide()
    {
        Payload request;

        switch (mode)
        {
            case MODE_STANDALONE:
                IncomingRequestHandler.getIncomingRequestHandler().prevSlide();
                break;
            case MODE_HOST:
                request = Payload.fromBytes(ACTION_SLIDE_PREV.getBytes());
                sendPayloadToClients(request);
                break;
            case MODE_CLIENT:
                request = Payload.fromBytes(ACTION_SLIDE_PREV.getBytes());
                sendPayloadToHost(request);
                break;
        }
    }

    public byte[] stringToByteArray(String s)
    {
        return s.getBytes();
    }

    private void sendPayloadToHost(Payload payload)
    {
        Nearby.Connections.sendPayload(
                null, //tODO
                hostEndpoint,
                payload
        );
    }

    private void sendPayloadToClients(Payload payload)
    {
        Nearby.Connections.sendPayload(
                null, //tODO
                endpointIds,
                payload
        );
    }

}
