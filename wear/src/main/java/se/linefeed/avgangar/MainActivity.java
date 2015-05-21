package se.linefeed.avgangar;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import java.util.ArrayList;

public class MainActivity extends Activity
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener,
        NodeApi.NodeListener {

    private ListView mListView;
    private GoogleApiClient mGoogleApiClient;
    private static final String TAG = "MainActivity";
    protected static final String PATH_STATION_INFO = "/GetStationService";
    public ArrayList<String> stnArr;
    public ArrayAdapter<String> stnAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(Wearable.API)
                    .build();
        }
        mGoogleApiClient.connect();
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mListView = (ListView) stub.findViewById(R.id.stationListView);
                stnArr = new ArrayList<String>();
                stnAdapter = new ArrayAdapter<String>(getApplicationContext(),
                        android.R.layout.simple_list_item_1,
                        stnArr);
                mListView.setAdapter(stnAdapter);
                Log.d(TAG, "Adapter has been set");
            }
        });

    }
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (int i = 0; i < dataEvents.getCount(); i++) {
            DataEvent event = dataEvents.get(i);
            DataMap dataMap = DataMap.fromByteArray(event.getDataItem().getData());
            Log.d(TAG,"onDataChanged: " + dataMap);

        }
    }
    @Override
    public void onPeerConnected(Node node) {
        Log.d(TAG,"PeerConnected: " + node);
        askForStations();
    }

    @Override
    public void onPeerDisconnected(Node node) {
        Log.d(TAG, "PeerDisconnected: " + node);
    }
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG,"ConnectionFailed: " + connectionResult);

    }
    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "ConnectionSuspended: " + i);
    }
    @Override
    public void onConnected(Bundle bundle) {

        Wearable.NodeApi.addListener(mGoogleApiClient, this);
        Wearable.DataApi.addListener(mGoogleApiClient, this);

        Wearable.MessageApi.addListener(mGoogleApiClient, new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                Log.d(TAG, "onMessageReceived: " + messageEvent);

                DataMap dataMap = DataMap.fromByteArray(messageEvent.getData());
                String path = messageEvent.getPath();
                Log.d(TAG, "Message for path " + path);
                if (path.equals(PATH_STATION_INFO)) {
                    showStations(dataMap);
                } else if (path.equals(PATH_STATION_INFO + "/Departures")) {
                    showDepartures(dataMap);
                }
            }
        });
        stnArr.add(0, "...");
        stnAdapter.notifyDataSetChanged();
        askForStations();

    }

    private void showStations(DataMap dataMap) {
        String[] stations = new String[dataMap.keySet().size()];
        for (String key : dataMap.keySet()) {
            int order = Integer.parseInt(dataMap.getString(key));
            if (order <= dataMap.keySet().size()) {
                stations[order] = key;
            }
        }
        int listIndex = 0;
        for (String station : stations) {
            Log.d(TAG,station);
            stnArr.add(listIndex++,station);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stnAdapter.notifyDataSetChanged();
                mListView.setClickable(true);
                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        Object o = mListView.getItemAtPosition(position);
                        Log.d(TAG, "Clicked station " + o.toString());
                        askForAvgangar(o.toString());
                    }
                });
            }
        });

    }

    private void showDepartures(DataMap dataMap) {
        String[] departure = new String[dataMap.keySet().size()];
        for (String key : dataMap.keySet()) {
            int order = Integer.parseInt(dataMap.getString(key));
            if (order <= dataMap.keySet().size()) {
                departure[order] = key;
            }
        }
        stnArr.clear();
        int listIndex = 0;
        for (String dep : departure) {
            Log.d(TAG,dep);
            stnArr.add(listIndex++,dep);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                stnAdapter.notifyDataSetChanged();
                mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    }
                });
            }
        });

    }

    private void askForStations() {
        Wearable.MessageApi.sendMessage(mGoogleApiClient, "", "/GetStationService/Require", null)
                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "SendRequireMessage: " + sendMessageResult.getStatus());
                    }
                });
    }
    private void askForAvgangar(String station) {
        stnArr.clear();
        stnArr.add("---");
        stnAdapter.notifyDataSetChanged();
        DataMap dataMap = new DataMap();
        dataMap.putString("station",station);
        Wearable.MessageApi.sendMessage(mGoogleApiClient, "", "/GetStationService/Station", dataMap.toByteArray())
                .setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Log.d(TAG, "SendStationRequest: " + sendMessageResult.getStatus());
                    }

                });
    }
}
