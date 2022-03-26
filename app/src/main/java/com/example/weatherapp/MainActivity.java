package com.example.weatherapp;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.ReferenceQueue;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout homeRL;
    private ProgressBar loadingPB;
    private TextView cityNameTV,temperatureTV,conditionTV;
    private TextInputEditText cityEdt;
    private ImageView backIV,iconIV,searchIV;
    private RecyclerView weatherRV;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ArrayList<WeatherRVModal> weatherRVModalArrayList;
    private WeatherRVAdapter weatherRVAdapter;
    private LocationManager locationManager;
    private int PERMISSION_CODE = 1;
//    private String cityName;
    private Location location;
    FusedLocationProviderClient mFusedLocationClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        setContentView(R.layout.activity_main);

        initialize();

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
//                startActivity(new Intent(MainActivity.this, MainActivity.class));
                cityEdt.setText("");
                getLastLocation();
                swipeRefreshLayout.setRefreshing(false);

            }
        });

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        getLastLocation();

        searchIV.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String city = cityEdt.getText().toString();
                if(city.isEmpty()){
                    Toast.makeText(MainActivity.this, "Please Enter City Name", Toast.LENGTH_SHORT).show();
                }else{
                    getWeatherInfo(city);
                    cityEdt.setText("");
                }
            }
        });

    }
    @SuppressLint("MissingPermission")
    private void getLastLocation(){
//        Toast.makeText(this, "getLastLocation", Toast.LENGTH_SHORT).show();
        if(checkPermissions()){
            if(isLocationEnabled()){
                mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        location = task.getResult();
                        if (location == null) {
                            requestNewLocationData();
                        } else {
                            Toast.makeText(MainActivity.this, "Finding weather data...", Toast.LENGTH_SHORT).show();
//                            Toast.makeText(MainActivity.this, "Lat: "+location.getLatitude()+"\nLong: "+location.getLongitude(), Toast.LENGTH_SHORT).show();
                            getWeatherInfo(Double.toString(location.getLatitude()) + "," + location.getLongitude());
                        }
                    }
                });
//                Toast.makeText(this, "isLocationEnabled", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(this, "Please turn on" + " your location...", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivity(intent);
                loadingPB.setVisibility(View.GONE);
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setMessage("Please refresh the page.");
                builder.setPositiveButton("Refresh", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        startActivity(new Intent(MainActivity.this, MainActivity.class));
//                        finish();
                    }
                });
                builder.create().show();
//                Snackbar.make(this,homeRL,"Please refresh the page.", BaseTransientBottomBar.LENGTH_INDEFINITE)
//                        .setAction("Refresh", new View.OnClickListener() {
//                            @Override
//                            public void onClick(View v) {
//                                startActivity(new Intent(MainActivity.this, MainActivity.class));
//                            }
//                        })
//                        .show();
            }
        }else{
            requestPermissions();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == PERMISSION_CODE){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permissions granted...", Toast.LENGTH_SHORT).show();
                getLastLocation();
//                getWeatherInfo(location.getLatitude(), location.getLongitude());
            }else{
                Toast.makeText(this, "Please provide the permissions", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

//    private void getWeatherInfo(String cityName){
//        String url = "https://api.weatherapi.com/v1/forecast.json?key=723f28c45569424a93c60642211910&q="+cityName+"&days=10&aqi=yes&alerts=yes";
//
//        RequestQueue requestQueue = Volley.newRequestQueue(MainActivity.this);
//        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
//            @Override
//            public void onResponse(JSONObject response) {
//                loadingPB.setVisibility(View.GONE);
//                homeRL.setVisibility(View.VISIBLE);
//                weatherRVModalArrayList.clear();
//                if(response==null)
//                    Toast.makeText(MainActivity.this, "Response is null", Toast.LENGTH_SHORT).show();
//                try {
//                    String cityName = response.getJSONObject("location").getString("name");
//                    cityNameTV.setText(cityName);
//                    String temperature = response.getJSONObject("current").getString("temp_c");
//                    temperatureTV.setText(temperature+"°C");
//                    int isDay = response.getJSONObject("current").getInt("is_day");
//                    if(isDay == 1){
//                        Picasso.get().load("https://images.unsplash.com/photo-1566228015668-4c45dbc4e2f5?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=334&q=80").into(backIV);
//                    }else{
//                        Picasso.get().load("https://images.unsplash.com/photo-1532074534361-bb09a38cf917?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=334&q=80").into(backIV);
//                    }
//                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
//                    conditionTV.setText(condition);
//                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
//                    Picasso.get().load("https:".concat(conditionIcon)).into(iconIV);
//
//                    JSONArray hourArray = response.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONArray("hour");
//                    for(int i=0;i<hourArray.length();i++){
//                        JSONObject hourObj = hourArray.getJSONObject(i);
//                        String time = hourObj.getString("time");
//                        String temper = hourObj.getString("temp_c");
//                        String img = hourObj.getJSONObject("condition").getString("icon");
//                        String wind = hourObj.getString("wind_kph");
//                        String condition2 = hourObj.getJSONObject("condition").getString("text");
//                        weatherRVModalArrayList.add(new WeatherRVModal(time,temper,img,wind,condition2));
//                    }
//                    weatherRVAdapter.notifyDataSetChanged();
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(MainActivity.this, "Please enter a valid city name...", Toast.LENGTH_SHORT).show();
//            }
//        });
//        requestQueue.add(jsonObjectRequest);
//
//    }

    private void getWeatherInfo(String query){
        String url1 = "https://api.weatherapi.com/v1/forecast.json?key=723f28c45569424a93c60642211910&q=" + query + "&days=5&aqi=yes&alerts=yes";
        RequestQueue requestQueue1 = Volley.newRequestQueue(MainActivity.this);
        JsonObjectRequest jsonObjectRequest1 = new JsonObjectRequest(Request.Method.GET, url1, null, new Response.Listener<JSONObject>() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onResponse(JSONObject response) {
                loadingPB.setVisibility(View.GONE);
                homeRL.setVisibility(View.VISIBLE);
                weatherRVModalArrayList.clear();

                try {
                    String cityName = response.getJSONObject("location").getString("name");
                    cityNameTV.setText(cityName);
                    String temperature = response.getJSONObject("current").getString("temp_c");
                    temperatureTV.setText(temperature+"°C");
                    int isDay = response.getJSONObject("current").getInt("is_day");
                    if(isDay == 1){
                        Picasso.get().load("https://images.unsplash.com/photo-1566228015668-4c45dbc4e2f5?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=334&q=80").into(backIV);
                    }else{
                        Picasso.get().load("https://images.unsplash.com/photo-1532074534361-bb09a38cf917?ixid=MnwxMjA3fDB8MHxwaG90by1wYWdlfHx8fGVufDB8fHx8&ixlib=rb-1.2.1&auto=format&fit=crop&w=334&q=80").into(backIV);
                    }
                    String condition = response.getJSONObject("current").getJSONObject("condition").getString("text");
                    conditionTV.setText(condition);
                    String conditionIcon = response.getJSONObject("current").getJSONObject("condition").getString("icon");
                    Picasso.get().load("https:".concat(conditionIcon)).into(iconIV);
                    JSONArray hourArray = response.getJSONObject("forecast").getJSONArray("forecastday").getJSONObject(0).getJSONArray("hour");
                    for(int i=0;i<hourArray.length();i++) {
                        JSONObject hourObj = hourArray.getJSONObject(i);
                        String time = hourObj.getString("time");
                        String temper = hourObj.getString("temp_c");
                        String img = hourObj.getJSONObject("condition").getString("icon");
                        String wind = hourObj.getString("wind_kph");
                        String condition2 = hourObj.getJSONObject("condition").getString("text");
                        if((new Date()).compareTo(new SimpleDateFormat("yyyy-MM-dd HH:mm").parse(time)) < 0) {
                            weatherRVModalArrayList.add(new WeatherRVModal(time, temper, img, wind, condition2));
                            weatherRVAdapter.notifyDataSetChanged();
                        }
                    }

                } catch (JSONException | ParseException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(MainActivity.this, "Something went wrong...", Toast.LENGTH_SHORT).show();
            }
        });
        requestQueue1.add(jsonObjectRequest1);

//        String url2 = "https://api.openweathermap.org/data/2.5/forecast?lat="+query.split(",")[0]+"&lon="+query.split(",")[1]+"&exclude=minutely,alert&appid=59f8260cadff6856d25c3f3990625958";
//        RequestQueue requestQueue2 = Volley.newRequestQueue(MainActivity.this);
//        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url2, null, new Response.Listener<JSONObject>() {
//            @RequiresApi(api = Build.VERSION_CODES.O)
//            @Override
//            public void onResponse(JSONObject response) {
//                loadingPB.setVisibility(View.GONE);
//                homeRL.setVisibility(View.VISIBLE);
//                weatherRVModalArrayList.clear();
//
//                try {
//                    JSONArray hourArray = response.getJSONArray("list");
//                    for(int i=1;i<hourArray.length();i++){
//                        JSONObject hourObj = hourArray.getJSONObject(i);
//                        String time = hourObj.getString("dt_txt");
//                        String temper = (new DecimalFormat("#.#")).format(hourObj.getJSONObject("main").getDouble("temp") - 273).toString();
//                        String img = hourObj.getJSONArray("weather").getJSONObject(0).getString("icon");
//                        String wind = (new DecimalFormat("#.##")).format(hourObj.getJSONObject("wind").getDouble("speed") * 3.6).toString();
//                        String condition2 = hourObj.getJSONArray("weather").getJSONObject(0).getString("description");
////                        if((new Date()).compareTo(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time)) < 0) {
//                            weatherRVModalArrayList.add(new WeatherRVModal(time, temper, img, wind, condition2));
////                            weatherRVAdapter.notifyDataSetChanged();
////                        }
//                    }
//                    weatherRVAdapter.notifyDataSetChanged();
//
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//            }
//        }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError error) {
//                Toast.makeText(MainActivity.this, "Something went wrong...", Toast.LENGTH_SHORT).show();
//            }
//        });
//        requestQueue2.add(jsonObjectRequest);

    }

    private String getCityName(double longitude,double latitude){
        String cityName = "Not Found";
        Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
        try {
            List<Address> addresses = gcd.getFromLocation(latitude,longitude,10);
            for(Address adr : addresses){
                if(adr != null){
                    String city = adr.getLocality();
                    if(city != null && !city.equals("")){
                        cityName = city;
                    }else{
                        Log.d("TAG", "CITY NOT FOUND!");
                        Toast.makeText(this, "User City Not Found...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Toast.makeText(this, cityName, Toast.LENGTH_SHORT).show();
        return cityName;
    }

    // method to check for permissions
    private boolean checkPermissions() {
//        Toast.makeText(this, "checkPermission", Toast.LENGTH_SHORT).show();
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        // If we want background location
        // on Android 10.0 and higher,
        // use:
        // ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
    }

    // method to request for permissions
    private void requestPermissions() {
//        Toast.makeText(this, "requestPermission", Toast.LENGTH_SHORT).show();
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_CODE);
    }


    // method to check
    // if location is enabled
    private boolean isLocationEnabled() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }

    @SuppressLint("MissingPermission")
    private void requestNewLocationData() {
        // Initializing LocationRequest
        // object with appropriate methods
//        Toast.makeText(this, "requestNewLocationData", Toast.LENGTH_SHORT).show();
        LocationRequest mLocationRequest = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(5).setFastestInterval(0).setNumUpdates(1);
        // setting LocationRequest
        // on FusedLocationClient
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
//        Toast.makeText(this, "out", Toast.LENGTH_SHORT).show();
    }
    private LocationCallback mLocationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
//            Toast.makeText(MainActivity.this, "LocationCallback", Toast.LENGTH_SHORT).show();
            Location mLastLocation = locationResult.getLastLocation();
            getWeatherInfo(Double.toString(mLastLocation.getLatitude()) + "," + mLastLocation.getLongitude());
        }
    };

    private void initialize() {
        homeRL = findViewById(R.id.idRLHome);
        loadingPB = findViewById(R.id.idPBLoading);
        cityNameTV = findViewById(R.id.idTVCity);
        temperatureTV = findViewById(R.id.idTVTemperature);
        conditionTV = findViewById(R.id.idTVCondition);
        cityEdt = findViewById(R.id.idEdtCity);
        backIV = findViewById(R.id.idIVBack);
        iconIV = findViewById(R.id.idIVIcon);
        searchIV = findViewById(R.id.idIVSearch);
        weatherRV = findViewById(R.id.idRVWeather);
        swipeRefreshLayout = findViewById(R.id.idSwapRefreshLayout);

        weatherRVModalArrayList = new ArrayList<>();
        weatherRVAdapter = new WeatherRVAdapter(this, weatherRVModalArrayList);
        weatherRV.setAdapter(weatherRVAdapter);

//        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

}