package com.uni.foodfindar.views

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.android.volley.Request
import com.android.volley.RetryPolicy
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.gson.Gson
import com.uni.foodfindar.ApiResponse
import com.uni.foodfindar.Places
import com.uni.foodfindar.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sqrt


class MainActivity : AppCompatActivity() {


    private lateinit var cont : Context
    private lateinit var setting:Button
    private lateinit var nearby:Button
    private var coordinates: String = "(46.616062,14.265438,46.626062,14.275438)" //Bounding Box - Left top(lat-), Left bottom(lon-), right top(lat+), right bottom(lon+)
    private var amenity: String = "[\"amenity\"~\"restaurant\"]" //Amenity can be extended with e.g. restaurant|cafe etc.

    private var bbSize = 0.0175
    private var userPosLon : Double? = 0.0
    private var userPosLat : Double? = 0.0
    private var bbLonMin : Double? = 0.0
    private var bbLatMin : Double? = 0.0
    private var bbLonMax : Double? = 0.0
    private var bbLatMax : Double? = 0.0

    private lateinit var placesList : ArrayList<Places>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cont = this

        setting = findViewById(R.id.settings)
        nearby = findViewById(R.id.nearby_button)

        setting.setOnClickListener{

            val intent = Intent(this, Settings::class.java)
            startActivity(intent)
            overridePendingTransition(0, 0)
        }

        nearby.isEnabled = false
        nearby.alpha = .5f





        getLocation()
    }

    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            getLastKnownPosition()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(ACCESS_COARSE_LOCATION), 1)
            getLocation()
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownPosition() {
        val task: Task<Location> = LocationServices.getFusedLocationProviderClient(this).lastLocation
        task.addOnSuccessListener { location: Location? ->
            setBoundingBox(location)

            Log.i("Coordinates", coordinates)

            tryOverpasser()
        }
        task.addOnFailureListener { e: Exception ->
            e.printStackTrace()
        }
    }

    private fun tryOverpasser() = runBlocking {
            val job = launch {
                val queue = Volley.newRequestQueue(cont)
                val url = "http://overpass-api.de/api/interpreter?data=[out:json];(node$amenity$coordinates;way$amenity$coordinates;relation$amenity$coordinates;);out body;>;out skel;" //building the link for requests
                placesList = ArrayList<Places>()

                val jsonObjectRequest = JsonObjectRequest(Request.Method.GET, url, null,
                    { res ->
                        val responseObject = Gson().fromJson(
                            res.toString(),
                            ApiResponse::class.java
                        )

                        for (element in responseObject.elements!!) {
                            val tag = element.tags
                            if (tag?.name != "" && tag?.name != null && tag.amenity == "restaurant") {
                                if(element.lat != null && element.lon != null){
                                    if(tag.housenumber != null) {
                                        placesList.add(
                                            Places(
                                                tag.name,
                                                "" + tag.street + " " + tag.housenumber,
                                                element.lat,
                                                element.lon,
                                                tag.website,
                                                distance(userPosLat!!, userPosLon!!, element.lat, element.lon)
                                            )
                                        )
                                    } else {
                                        placesList.add(
                                            Places(
                                                tag.name,
                                                tag.street,
                                                element.lat,
                                                element.lon,
                                                tag.website,
                                                distance(userPosLat!!, userPosLon!!, element.lat, element.lon)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                        placesList.sortBy { it.distance }

                        Toast.makeText(cont, "Locations fetched successfully!", Toast.LENGTH_SHORT).show()

                        nearby.isEnabled = true
                        nearby.alpha = 1F

                        nearby.setOnClickListener{

                            val intent = Intent(cont, Nearby_locations::class.java)
                            val bundle = Bundle()
                            bundle.putParcelableArrayList("placesList", placesList)
                            intent.putExtras(bundle)
                            startActivity(intent)
                        }

                        debugPlaces() //must be deleted later
                    },
                    { e ->
                        Toast.makeText(cont, "Internet connection failed.. Please check your settings", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                )
                queue.add(jsonObjectRequest)

                jsonObjectRequest.retryPolicy = object : RetryPolicy {
                    override fun getCurrentTimeout(): Int {
                        return 50000
                    }

                    override fun getCurrentRetryCount(): Int {
                        return 50000
                    }

                    @Throws(VolleyError::class)
                    override fun retry(error: VolleyError) {
                    }
                }
            }
        job.join()
    }

    private fun setBoundingBox(location : Location?) {
        userPosLat = location?.latitude
        userPosLon = location?.longitude
        bbLatMin = userPosLat?.minus(bbSize)
        bbLonMin = userPosLon?.minus(bbSize)
        bbLatMax = userPosLat?.plus(bbSize)
        bbLonMax = userPosLon?.plus(bbSize)

        coordinates = "($bbLatMin,$bbLonMin,$bbLatMax,$bbLonMax)"
    }

    private fun distance(lat1 : Double, lon1 : Double, lat2 : Double, lon2 : Double) : Double {
        val p = 0.017453292519943295;    // Math.PI / 180
        val a = 0.5 - cos((lat2 - lat1) * p)/2 +
                cos(lat1 * p) * cos(lat2 * p) *
                (1 - cos((lon2 - lon1) * p))/2;

        return 12742 * asin(sqrt(a)); // 2 * R; R = 6371 km
    }

    private fun debugPlaces(){
        Log.i("Place", "Debugging Places..")
        for(places in placesList){
            Log.i(
                "Place",
                "${places.name} || ${places.address} || Lat: ${places.lat} || Lon: ${places.lon} || ${places.website} || ${places.distance}km"
            )
        }
    } //Must be deleted later

}

