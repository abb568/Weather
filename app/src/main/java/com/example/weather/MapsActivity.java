package com.example.weather;

        import androidx.appcompat.app.AlertDialog;
        import androidx.appcompat.app.AppCompatActivity;
        import androidx.core.app.ActivityCompat;
        import androidx.core.content.ContextCompat;
        import android.Manifest;
        import android.content.Context;
        import android.content.DialogInterface;
        import android.content.pm.PackageManager;
        import android.graphics.Bitmap;
        import android.graphics.BitmapFactory;
        import android.graphics.Canvas;
        import android.graphics.drawable.BitmapDrawable;
        import android.graphics.drawable.Drawable;
        import android.location.Address;
        import android.location.Geocoder;
        import android.location.Location;
        import android.nfc.Tag;
        import android.os.AsyncTask;
        import android.os.Bundle;
        import android.os.Looper;
        import android.util.Log;
        import android.view.GestureDetector;
        import android.view.MotionEvent;
        import android.view.View;
        import android.widget.Button;
        import android.widget.ImageView;
        import android.widget.TextView;
        import android.widget.Toast;
        import com.google.android.gms.location.LocationCallback;
        import com.google.android.gms.location.LocationRequest;
        import com.google.android.gms.location.LocationResult;
        import com.google.android.gms.location.LocationServices;
        import com.google.android.gms.maps.CameraUpdateFactory;
        import com.google.android.gms.maps.GoogleMap;
        import com.google.android.gms.maps.OnMapReadyCallback;
        import com.google.android.gms.maps.SupportMapFragment;
        import com.google.android.gms.maps.model.BitmapDescriptor;
        import com.google.android.gms.maps.model.BitmapDescriptorFactory;
        import com.google.android.gms.maps.model.LatLng;
        import com.google.android.gms.maps.model.Marker;
        import com.google.android.gms.maps.model.MarkerOptions;
        import com.google.android.gms.location.FusedLocationProviderClient;

        import org.json.JSONArray;
        import org.json.JSONException;
        import org.json.JSONObject;

        import java.io.IOException;
        import java.net.URL;
        import java.util.List;
        import java.util.Locale;


public class MapsActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    GoogleMap mGoogleMap;
    SupportMapFragment mapFrag;
    LocationRequest mLocationRequest;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    FusedLocationProviderClient mFusedLocationClient;
    TextView Location;
    TextView CurrentTemp;
    TextView WeatherDesc;
    ImageView WeatherIcon;
    Button Update;
    String LocationKey;
    String TemperatureValue;
    String WeatherText;
    String IconValue;
    Marker CustomMarker;
    private static final String TAG = "Main";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        Location = this.findViewById(R.id.Location);
        CurrentTemp = this.findViewById(R.id.CurrentTemp);
        WeatherIcon = this.findViewById(R.id.Icon);
        WeatherDesc = this.findViewById(R.id.WeatherDesc);
        Update = this.findViewById(R.id.UpdateBtn);
        mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFrag.getMapAsync(this);


        Update.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                if( CustomMarker != null) {
                    CustomMarker.remove();
                }
                onMapReady(mGoogleMap);
                Toast toast = Toast.makeText(getApplicationContext(),"Location and Weather Updated!",Toast.LENGTH_SHORT);
                toast.show();
            }
        });


    }

    @Override
    public void onPause() {
        super.onPause();

        //stop location updates when Activity is no longer active
        if (mFusedLocationClient != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        mGoogleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            //Location Permission already granted
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
        } else {
            //Request Location Permission
            checkLocationPermission();
        }

        mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                if(CustomMarker != null) {
                    CustomMarker.remove();
                }
                CustomMarker = mGoogleMap.addMarker(new MarkerOptions().position(point)
                .title(getLocation(point)));
                Location.setText(getLocation(point));
                mGoogleMap.animateCamera(CameraUpdateFactory.newLatLng(point));
                URL LocationUrl = DataCollection.buildLocationUrl(getLocation(point));
                new FetchApiInformation().execute(LocationUrl);
            }
        });
    }

    LocationCallback mLocationCallback = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult locationResult) {

            mLastLocation = locationResult.getLastLocation();
            LatLng latLng = new LatLng(mLastLocation.getLatitude(), mLastLocation.getLongitude());
            Location.setText(getLocation(latLng));
            //get location key from API
            URL LocationUrl = DataCollection.buildLocationUrl(getLocation(latLng));
            new FetchApiInformation().execute(LocationUrl);

            //use location key to collect weather data from API
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng);
            markerOptions.title("Current Position");

            //icon creation

            Drawable icon = getResources().getDrawable(R.mipmap.person_foreground);
            markerOptions.icon(BitmapDescriptorFactory.fromBitmap(makeBitMap(icon)));

            //place marker on mobile position

            mCurrLocationMarker = mGoogleMap.addMarker(markerOptions);

            //move map camera

            mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
        }
    };

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();

            } else {
                //requesting permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    //if permission is granted
                    if (ContextCompat.checkSelfPermission(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
                        mGoogleMap.setMyLocationEnabled(true);
                    }

                } else {
                    //toast to say application wont work
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
            }
    }
    private Bitmap makeBitMap(Drawable drawable) {
    //coverts drawable icon into bitmap
        try {
            Bitmap bitmap;
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            // Handle the error
            return null;
        }
    }
    public String getLocation(LatLng loc) {
        //gets location name from mobile lat/long
        try {
            //work around as gcd for address list at 0 can return null
            Geocoder gcd = new Geocoder(this.getBaseContext(), Locale.getDefault());
            List<Address> addresses = gcd.getFromLocation(loc.latitude,loc.longitude, 10);
            if (addresses != null && addresses.size() > 0) {
                for (Address adr : addresses) {
                    if (adr.getLocality() != null && adr.getLocality().length() > 0) {
                        return adr.getLocality();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class FetchApiInformation extends AsyncTask<URL,Void,String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(URL... urls) {
            URL TempURL = urls[0];
            String Results = null;
            try {
                Results = DataCollection.getResponse(TempURL);

            } catch (IOException e) {
                e.printStackTrace();
            }
            return Results;
        }

        @Override
        protected void onPostExecute(String Results) {
            if(Results != null && !Results.equals("")) {
                Log.i(TAG,"Result:"+Results);
                if (Results.contains("WeatherText")) {
                    parseWeatherJSON(Results);
                }
                else {
                    parseLocationJSON(Results);
                }

            }
            super.onPostExecute(Results);
        }
    }

    private String parseLocationJSON(String Results) {
        if(Results != null) {
            try {
                JSONArray array = new JSONArray(Results);
                    //collects key string and gets weather information with said key
                    LocationKey = array.getJSONObject(0).getString("Key");
                    Log.i(TAG,"key:"+LocationKey);
                    URL WeatherUrl = DataCollection.buildWeatherUrl(LocationKey);
                    new FetchApiInformation().execute(WeatherUrl);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private String parseWeatherJSON(String Results) {
        if(Results != null) {
            try {
                JSONArray array = new JSONArray(Results);
                    JSONObject Data = array.getJSONObject(0);
                    JSONObject JSONTemperature = new JSONObject(Data.getString("Temperature"));
                    JSONObject JSONMetric = new JSONObject(JSONTemperature.getString("Metric"));
                    TemperatureValue = JSONMetric.getString("Value")+ "°";
                    IconValue = ("w"+Data.getString("WeatherIcon"));
                    int id = getResources().getIdentifier(IconValue,"drawable",getPackageName());
                    Drawable draw = getResources().getDrawable(id);
                    WeatherIcon.setImageDrawable(draw);
                    WeatherDesc.setText(Data.getString("WeatherText"));
                    CurrentTemp.setText(TemperatureValue);
                    WeatherText = Data.getString("WeatherText");

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


}