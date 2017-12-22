package uk.co.dphin.albumview.net.android;

import android.util.Log;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.Payload;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import uk.co.dphin.albumview.models.Album;
import uk.co.dphin.albumview.models.Slide;
import uk.co.dphin.albumview.net.action.Action;

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
    private List<InetAddress> clients;

    /**
     * Endpoint ID of the host. Will only be populated if this is the client.
     */
    private InetAddress host;

    private int mode;

    private OutgoingRequestHandler()
    {
        mode = MODE_STANDALONE;
        clients = new ArrayList<>();
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

        switch(mode)
        {
            case MODE_STANDALONE:
                Log.d("Networking", "Entering standalone mode");
                removeAllClients();
                removeHost();
                // TODO: Tell net API to disconnect everything
                break;
            case MODE_HOST:
                Log.d("Networking", "Entering host mode");
                removeHost();
                // TODO: Tell net API to disconnect everything
                break;
            case MODE_CLIENT:
                Log.d("Networking", "Entering client mode");
                removeAllClients();
                // TODO: Tell net API to disconnect everything
                break;
        }
    }

    public int getMode()
    {
        return mode;
    }

    public void handleAction(Action action) {

        // Always carry out the action locally
        IncomingRequestHandler.getIncomingRequestHandler().handleAction(action);

        switch (mode) {
            case MODE_STANDALONE:

                break;

            case MODE_HOST:
                // TODO

            case MODE_CLIENT:
                // TODO

        }
    }



    public void registerClient(InetAddress newClient)
    {
        clients.add(newClient);
    }

    public void removeClient(InetAddress oldClient)
    {
        clients.remove(oldClient);
    }

    public void removeAllClients()
    {
        clients.clear();
    }

    public void registerHost(InetAddress newHost)
    {
        Log.i("Networking", "Connected to host " + newHost.getHostName() + "("+newHost.getHostAddress()+")");
        host = newHost;
    }

    public void removeHost()
    {
        host = null;
    }

    public List<InetAddress> getClients()
    {
        // Copy the list so it can be safely modified by the receiver
        return new ArrayList<>(clients);
    }

    public InetAddress getHost()
    {
        return host;
    }

}
