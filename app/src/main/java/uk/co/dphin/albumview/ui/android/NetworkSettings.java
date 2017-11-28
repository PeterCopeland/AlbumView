package uk.co.dphin.albumview.ui.android;

import android.app.Activity;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.AdvertisingOptions;
import com.google.android.gms.nearby.connection.ConnectionInfo;
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback;
import com.google.android.gms.nearby.connection.ConnectionResolution;
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo;
import com.google.android.gms.nearby.connection.DiscoveryOptions;
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback;
import com.google.android.gms.nearby.connection.Strategy;

import uk.co.dphin.albumview.R;
import uk.co.dphin.albumview.net.android.IncomingRequestHandler;
import uk.co.dphin.albumview.net.android.OutgoingRequestHandler;

/**
 * Created by peter on 26/11/17.
 */

public class NetworkSettings extends Activity
{
    /**
     * Called when the activity is starting.  This is where most initialization
     * should go: calling {@link #setContentView(int)} to inflate the
     * activity's UI, using {@link #findViewById} to programmatically interact
     * with widgets in the UI, calling
     * {@link #managedQuery(Uri, String[], String, String[], String)} to retrieve
     * cursors for data being displayed, etc.
     * <p>
     * <p>You can call {@link #finish} from within this function, in
     * which case onDestroy() will be immediately called without any of the rest
     * of the activity lifecycle ({@link #onStart}, {@link #onResume},
     * {@link #onPause}, etc) executing.
     * <p>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @param savedInstanceState If the activity is being re-initialized after
     *                           previously being shut down then this Bundle contains the data it most
     *                           recently supplied in {@link #onSaveInstanceState}.  <b><i>Note: Otherwise it is null.</i></b>
     * @see #onStart
     * @see #onSaveInstanceState
     * @see #onRestoreInstanceState
     * @see #onPostCreate
     */
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.networksettings);
    }

    /**
     * Called after {@link #onCreate} &mdash; or after {@link #onRestart} when
     * the activity had been stopped, but is now again being displayed to the
     * user.  It will be followed by {@link #onResume}.
     * <p>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onCreate
     * @see #onStop
     * @see #onResume
     */
    @Override
    protected void onStart()
    {
        super.onStart();

        Button hostButton = (Button)findViewById(R.id.hostButton);
        Button clientButton = (Button)findViewById(R.id.clientButton);
        Button disconnectButton = (Button)findViewById(R.id.disconnectButton);

        hostButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OutgoingRequestHandler.getOutgoingRequestHandler().setMode(OutgoingRequestHandler.MODE_HOST);
                Nearby.getConnectionsClient(getApplicationContext()).startAdvertising(
                        Build.MODEL,
                        "uk.co.dphin.albumview",
                        new ConnectionLifecycleCallback() {
                            @Override
                            public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                                Log.d("Networking", "Host initiated connection with " + endpointId);
                                // Auto-accept connection
                                Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(
                                        endpointId,
                                        IncomingRequestHandler.getIncomingRequestHandler()
                                );
                            }

                            @Override
                            public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
                                Log.d("Networking", "Host completed connection with " + endpointId + ", result is " + connectionResolution.getStatus().toString());
                                if (connectionResolution.getStatus().isSuccess())
                                {
                                    OutgoingRequestHandler.getOutgoingRequestHandler().registerClient(endpointId);
                                }
                                else if (connectionResolution.getStatus().isInterrupted())
                                {
                                    Toast.makeText(NetworkSettings.this, "Connection to client was interrupted", Toast.LENGTH_SHORT).show();
                                }
                                else if (connectionResolution.getStatus().isCanceled())
                                {
                                    Toast.makeText(NetworkSettings.this, "Connection to client was cancelled", Toast.LENGTH_SHORT).show();
                                }
                                updateConnectedDevices();
                            }

                            @Override
                            public void onDisconnected(String endpointId) {
                                Log.d("Networking", "Host disconnected from " + endpointId);
                                OutgoingRequestHandler.getOutgoingRequestHandler().removeClient(endpointId);
                                updateConnectedDevices();
                            }
                        },
                        new AdvertisingOptions(Strategy.P2P_STAR)
                );

                Log.d("Network", "Started advertising");
            }
        });

        clientButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OutgoingRequestHandler.getOutgoingRequestHandler().setMode(OutgoingRequestHandler.MODE_CLIENT);
                Nearby.getConnectionsClient(getApplicationContext()).startDiscovery("uk.co.dphin.albumview", new EndpointDiscoveryCallback() {
                    @Override
                    public void onEndpointFound(String endpointId, DiscoveredEndpointInfo discoveredEndpointInfo) {
                        // TODO: User interface
                        if (discoveredEndpointInfo.getServiceId().equals("uk.co.dphin.albumview"))
                        {
                            Log.d("Networking", "Discovered endpoint "+discoveredEndpointInfo.getServiceId() + "("+endpointId+")");
                            Nearby.getConnectionsClient(getApplicationContext()).requestConnection(
                                    Build.HOST,
                                    endpointId,
                                    new ConnectionLifecycleCallback() {
                                        @Override
                                        public void onConnectionInitiated(String endpointId, ConnectionInfo connectionInfo) {
                                            Log.d("Networking", "Client initiated connection with endpoint "+endpointId);
                                            // Auto-accept connection
                                            Nearby.getConnectionsClient(getApplicationContext()).acceptConnection(
                                                    endpointId,
                                                    IncomingRequestHandler.getIncomingRequestHandler()
                                            );
                                        }

                                        @Override
                                        public void onConnectionResult(String endpointId, ConnectionResolution connectionResolution) {
                                            Log.d("Networking", "Client completed connection with "+endpointId+", result is "+connectionResolution.getStatus());
                                            if (connectionResolution.getStatus().isSuccess())
                                            {
                                                OutgoingRequestHandler.getOutgoingRequestHandler().registerClient(endpointId);
                                            }
                                            else if (connectionResolution.getStatus().isInterrupted())
                                            {
                                                Toast.makeText(NetworkSettings.this, "Connection to host was interrupted", Toast.LENGTH_SHORT).show();
                                            }
                                            else if (connectionResolution.getStatus().isCanceled())
                                            {
                                                Toast.makeText(NetworkSettings.this, "Connection to host was cancelled", Toast.LENGTH_SHORT).show();
                                            }
                                            updateConnectedDevices();
                                        }

                                        @Override
                                        public void onDisconnected(String endpointId) {
                                            Log.d("Networking", "Client disconnected from "+endpointId);
                                            OutgoingRequestHandler.getOutgoingRequestHandler().removeHost();
                                            updateConnectedDevices();
                                        }
                                    }
                            );
                        }
                    }

                    @Override
                    public void onEndpointLost(String endpointId) {
                        Log.d("Networking", "Client lost endpoint "+endpointId);
                    }
                },
                new DiscoveryOptions(Strategy.P2P_STAR));
                Log.d("Network", "Started listening");
            }
        });
    }

    /**
     * Called after {@link #onRestoreInstanceState}, {@link #onRestart}, or
     * {@link #onPause}, for your activity to start interacting with the user.
     * This is a good place to begin animations, open exclusive-access devices
     * (such as the camera), etc.
     * <p>
     * <p>Keep in mind that onResume is not the best indicator that your activity
     * is visible to the user; a system window such as the keyguard may be in
     * front.  Use {@link #onWindowFocusChanged} to know for certain that your
     * activity is visible to the user (for example, to resume a game).
     * <p>
     * <p><em>Derived classes must call through to the super class's
     * implementation of this method.  If they do not, an exception will be
     * thrown.</em></p>
     *
     * @see #onRestoreInstanceState
     * @see #onRestart
     * @see #onPostResume
     * @see #onPause
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    private void updateConnectedDevices()
    {
        LinearLayout deviceListView = (LinearLayout)findViewById(R.id.deviceListContent);
        deviceListView.removeAllViews();

        if (OutgoingRequestHandler.getOutgoingRequestHandler().getMode() == OutgoingRequestHandler.MODE_HOST)
        {
            TextView thisHost = new TextView(this);
            thisHost.setText("This device (host)");
            deviceListView.addView(thisHost);

            for (String endpointId : OutgoingRequestHandler.getOutgoingRequestHandler().getClients())
            {
                TextView client = new TextView(this);
                client.setText(endpointId);
                deviceListView.addView(client);
            }
        }
        else if (OutgoingRequestHandler.getOutgoingRequestHandler().getMode() == OutgoingRequestHandler.MODE_CLIENT)
        {
            TextView host = new TextView(this);
            host.setText(OutgoingRequestHandler.getOutgoingRequestHandler().getHostEndpoint() + " (host)");
            deviceListView.addView(host);

            TextView thisClient = new TextView(this);
            host.setText("This device");
            deviceListView.addView(thisClient);
        }
    }
}
