package com.example.weather;

import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;

public class DataCollection  {

    private static String WeatherUrl = "https://dataservice.accuweather.com/currentconditions/v1/";
    private static String LocationUrl = "https://dataservice.accuweather.com/locations/v1/cities/search";
    private static String WeatherLocationUrl;

    private final static String API ="0vYVVUyHSYZqTGsOVW0lQ03acewb60uJ";

    private final static String Param_API = "apikey";
    private final static String Param_Query = "q";
    private static final String TAG = "DataCollection";

    public final static URL buildWeatherUrl(String Key) {
        WeatherLocationUrl = WeatherUrl + Key;
        Uri builtUri = Uri.parse(WeatherLocationUrl).buildUpon().appendQueryParameter(Param_API,API).build();

        URL url = null;
        try {
            url= new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "buildWeatherUrl: url: "+url);
        return url;
    }

    public final static URL buildLocationUrl(String Location) {
        Uri builtUri = Uri.parse(LocationUrl).buildUpon().appendQueryParameter(Param_API,API).appendQueryParameter(Param_Query,Location).build();


        URL url = null;
        try {
            url= new URL(builtUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        Log.i(TAG, "buildWeatherUrl: url: "+url);
        return url;
    }

    public static String getResponse(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();
            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            if(hasInput) {
                return scanner.next();
            } else {
                return null;
            }
            } finally {
                urlConnection.disconnect();
            }
        }
    }

