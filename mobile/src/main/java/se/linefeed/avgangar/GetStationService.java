package se.linefeed.avgangar;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

public class GetStationService extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{
    private static final String TAG = "GetStationService";
    private static final long FASTEST_INTERVAL_MS = 5000;
    private static final long UPDATE_INTERVAL_MS = 30000;
    private GoogleApiClient mGoogleApiClient;
    private String mPeerId;
    private Location lastLocation;
    protected static final String PATH_STATION_INFO = "/GetStationService";


    @Override
    public int onStartCommand( Intent intent, int flags, int startId )
    {
        if ( intent != null )
        {
                mPeerId = intent.getStringExtra( "PeerId" );
        }

        connectApiClient();
        return super.onStartCommand( intent, flags, startId );
    }

    protected void connectApiClient() {
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .addApi(LocationServices.API)
                    .build();
        }
        mGoogleApiClient.connect();
    }
    @Override
    public void onMessageReceived(MessageEvent mEvent) {
        mPeerId = mEvent.getSourceNodeId();
        if (mEvent.getPath().equals("/GetStationService/Require")) {
            if (mGoogleApiClient.isConnected())
                readStations();
            else
                notifyNoConnection();
        } else if (mEvent.getPath().equals("/GetStationService/Station")) {
            DataMap dataMap = DataMap.fromByteArray(mEvent.getData());
            try {
                readAvgangar(dataMap);
            }
            catch (PackageManager.NameNotFoundException e) {}

        }
    }
    @Override
    public void onConnected(Bundle connectionHint) {
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
            lastLocation = location;
        }
    }
    protected void readStations() {
        if (lastLocation != null) {
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
    protected void readAvgangar(DataMap dataMap) throws PackageManager.NameNotFoundException {
        GooglePlacesReadAvgangar readAvgangarTask = new GooglePlacesReadAvgangar();
        Object passObj[] = new Object[4];
        String station = dataMap.getString("station");
        passObj[0] = station;
        passObj[1] = mGoogleApiClient;
        passObj[2] = mPeerId;
        ApplicationInfo ai = getPackageManager().getApplicationInfo(getPackageName(), PackageManager.GET_META_DATA);
        Bundle bundle = ai.metaData;
        passObj[3] = bundle;
        readAvgangarTask.execute(passObj);
    }
    protected void notifyNoConnection() {
        DataMap msgmap = new DataMap();
        msgmap.putString("Location not available. X-P", "0");
        Wearable.MessageApi.sendMessage(mGoogleApiClient,mPeerId,PATH_STATION_INFO, msgmap.toByteArray()).setResultCallback(
                new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                    }
                }
        );
    }
}
