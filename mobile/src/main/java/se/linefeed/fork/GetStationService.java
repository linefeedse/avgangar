package se.linefeed.fork;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by torkel on 15-05-19.
 */
public class GetStationService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private static final String TAG = "GetStationService";
    private static final long FASTEST_INTERVAL_MS = 1000;
    private static final long UPDATE_INTERVAL_MS = 5000;
    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;
    private Location lastLocation;

    @Override
    public int onStartCommand( Intent intent, int flags, int startId )
    {
        if ( intent != null )
        {
                mPeerId = intent.getStringExtra( "PeerId" );
                Log.d(TAG,"onStartCommand PeerId" + mPeerId);

        }

        Log.d(TAG,"onStartCommand without intent");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
        return super.onStartCommand( intent, flags, startId );
    }
    @Override
    public void onMessageReceived(MessageEvent mEvent) {
        mPeerId = mEvent.getSourceNodeId();
        Log.d(TAG,"Message from " + mPeerId + " for path " + mEvent.getPath());
        if (mEvent.getPath().equals("/GetStationService/Require")) {
            readStations();
        }
    }
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "GoogleApiClient connected");
        // TODO: Start making API requests.
        LocationRequest locationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
                .setInterval(UPDATE_INTERVAL_MS)
                .setFastestInterval(FASTEST_INTERVAL_MS);

        LocationServices.FusedLocationApi
                .requestLocationUpdates(mGoogleApiClient, locationRequest, this)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(Status status) {
                        if (status.getStatus().isSuccess()) {
                            if (Log.isLoggable(TAG, Log.DEBUG)) {
                                Log.d(TAG, "Successfully requested location updates");
                            }
                        } else {
                            Log.e(TAG,
                                    "Failed in requesting location updates, "
                                            + "status code: "
                                            + status.getStatusCode()
                                            + ", message: "
                                            + status.getStatusMessage());
                        }
                    }
                });
    }

    /**
     * Called when {@code mGoogleApiClient} connection is suspended.
     */
    @Override
    public void onConnectionSuspended(int cause) {
        Log.i(TAG, "GoogleApiClient connection suspended");
    }

    /**
     * Called when {@code mGoogleApiClient} is trying to connect but failed.
     * Handle {@code result.getResolution()} if there is a resolution
     * available.
     */
    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "GoogleApiClient connection failed: " + result.toString());
    }
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.i(TAG, "location was provided by " + location.getProvider());
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient,this);
            lastLocation = location;
        }
    }
    protected void readStations() {
        if (lastLocation != null) {
            Log.i(TAG, "Latitude: " + lastLocation.getLatitude() + " Longitude: " + lastLocation.getLongitude());

            GooglePlacesReadStations readStationTask = new GooglePlacesReadStations();
            Object[] passObj = new Object[3];
            passObj[0] = lastLocation;
            passObj[1] = mGoogleApiClient;
            passObj[2] = mPeerId;
            readStationTask.execute(passObj);
        } else {
            Log.d(TAG,"readStations called too early - no location yet");
        }
    }
}
