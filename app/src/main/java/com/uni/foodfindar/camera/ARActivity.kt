package com.uni.foodfindar.camera

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.*
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.opengl.Matrix
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.uni.foodfindar.R

class ARActivity : AppCompatActivity(), SensorEventListener, LocationListener {

        val TAG = "ARActivity"
        private var surfaceView: SurfaceView? = null
        private var cameraContainerLayout: FrameLayout? = null
        private var arOverlayView: ViewCamera? = null
        private var camera: Camera? = null
        private var arCamera: ARCamera? = null
        private var tvCurrentLocation: TextView? = null
        private var tvBearing: TextView? = null

        private var sensorManager: SensorManager? = null
        private val REQUEST_CAMERA_PERMISSIONS_CODE = 11
        val REQUEST_LOCATION_PERMISSIONS_CODE = 0

        private val MIN_DISTANCE_CHANGE_FOR_UPDATES: Long = 0 // 10 meters

        private val MIN_TIME_BW_UPDATES: Long = 0 //1000 * 60 * 1; // 1 minute


        private var locationManager: LocationManager? = null
        var location: Location? = null
        var isGPSEnabled = false
        var isNetworkEnabled = false
        var locationServiceAvailable = false
        private val declination = 0f


        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setContentView(R.layout.activity_camera)


            sensorManager = this.getSystemService(SENSOR_SERVICE) as SensorManager
            cameraContainerLayout = findViewById(R.id.camera_container_layout)
            surfaceView = findViewById(R.id.surface_view)
            tvCurrentLocation = findViewById(R.id.tv_current_location)
            tvBearing = findViewById(R.id.tv_bearing)
            arOverlayView = ViewCamera(this)

        }

        override fun onResume() {
            super.onResume()
            requestCameraPermission()
            requestLocationPermission()
            registerSensors()
            initAROverlayView()
        }

        override fun onPause() {
            releaseCamera()
            super.onPause()
        }

        fun requestCameraPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSIONS_CODE)
            } else {
                initARCameraView()
            }
        }

        fun requestLocationPermission() {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    REQUEST_LOCATION_PERMISSIONS_CODE
                )
            } else {
                initLocationService()
            }
        }

        private fun initAROverlayView() {
            if (arOverlayView!!.parent != null) {
                (arOverlayView!!.parent as ViewGroup).removeView(arOverlayView)
            }
            cameraContainerLayout!!.addView(arOverlayView)
        }

        private fun initARCameraView() {
            reloadSurfaceView()
            if (arCamera == null) {

                arCamera = ARCamera(this, findViewById(R.id.surface_view))
            }
            if (arCamera!!.getParent() != null) {
                    (arCamera!!.parent as ViewGroup).removeView(arCamera)
            }
            cameraContainerLayout!!.addView(arCamera)
            initCamera()
        }

        private fun initCamera() {
            val numCams = Camera.getNumberOfCameras()
            if (numCams > 0) {
                try {
                    camera = Camera.open()
                    //arCamera.camera(camera)
                } catch (ex: RuntimeException) {
                    Toast.makeText(this, "Camera not found", Toast.LENGTH_LONG).show()
                }
            }
        }

        private fun reloadSurfaceView() {
            if (surfaceView!!.parent != null) {
                (surfaceView!!.parent as ViewGroup).removeView(surfaceView)
            }
            cameraContainerLayout!!.addView(surfaceView)
        }

        private fun releaseCamera() {
            if (camera != null) {
                camera!!.setPreviewCallback(null)
                camera!!.stopPreview()
                //arCamera.setCamera(null)
                camera!!.release()
                camera = null
            }
        }

        private fun registerSensors() {
            sensorManager!!.registerListener(
                this,
                sensorManager!!.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_NORMAL
            )
        }



        //////////////////////////
        /*
        private fun openCamera() {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.TITLE, "YOUR WAY TO GO")
        }

        override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
        ) {
            when (requestCode) {
                PERMISSION_CODE -> {
                    if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //Permission was granted
                        openCamera()
                    } else {
                        //Permission was denied
                        Toast.makeText(this, "Permission was denied", Toast.LENGTH_SHORT).show()

                    }
                }
            }
        }

         */


        override fun onSensorChanged(sensorEvent: SensorEvent?) {
            if (sensorEvent != null) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
                    val rotationMatrixFromVector = FloatArray(16)
                    val rotationMatrix = FloatArray(16)
                    if (sensorEvent != null) {
                        SensorManager.getRotationMatrixFromVector(
                            rotationMatrixFromVector,
                            sensorEvent.values
                        )
                    }
                    val screenRotation = this.windowManager.defaultDisplay
                        .rotation

                    when (screenRotation) {
                        Surface.ROTATION_0 -> SensorManager.remapCoordinateSystem(
                            rotationMatrixFromVector,
                            SensorManager.AXIS_Y,
                            SensorManager.AXIS_MINUS_X,
                            rotationMatrix
                        )
                        Surface.ROTATION_90 -> SensorManager.remapCoordinateSystem(
                            rotationMatrixFromVector,
                            SensorManager.AXIS_MINUS_Y,
                            SensorManager.AXIS_X,
                            rotationMatrix
                        )
                        Surface.ROTATION_180 -> SensorManager.remapCoordinateSystem(
                            rotationMatrixFromVector,
                            SensorManager.AXIS_MINUS_X,
                            SensorManager.AXIS_MINUS_Y,
                            rotationMatrix
                        )
                        Surface.ROTATION_270 -> SensorManager.remapCoordinateSystem(
                            rotationMatrixFromVector,
                            SensorManager.AXIS_X,
                            SensorManager.AXIS_Y,
                            rotationMatrix
                        )
                    }

                    if(arCamera == null){
                        requestCameraPermission()
                        requestCameraPermission()
                    }
                        val projectionMatrix: FloatArray? = arCamera?.getProjectionMatrix()
                        val rotatedProjectionMatrix = FloatArray(16)
                        Matrix.multiplyMM(
                            rotatedProjectionMatrix,
                            0,
                            projectionMatrix,
                            0,
                            rotationMatrix,
                            0
                        )
                        arOverlayView!!.updateRotatedProjectionMatrix(rotatedProjectionMatrix)


                    //Heading

                    //Heading
                    val orientation = FloatArray(3)
                    SensorManager.getOrientation(rotatedProjectionMatrix, orientation)
                    val bearing = Math.toDegrees(orientation[0].toDouble()) + declination
                    tvBearing!!.text = String.format("Bearing: %s", bearing)

                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            if (accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
                Log.w("DeviceOrientation", "Orientation compass unreliable");
            }
        }

        fun initLocationService()  {

            if ( Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission( this, android.Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED) {
                return  ;
            }


            locationManager = this.getSystemService(LOCATION_SERVICE) as LocationManager

            // Get GPS and network status

            // Get GPS and network status
            isGPSEnabled = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
            isNetworkEnabled = locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isNetworkEnabled && !isGPSEnabled) {
                // cannot get location
                locationServiceAvailable = false
            }

            this.locationServiceAvailable = true;

            if (isNetworkEnabled) {
                locationManager!!.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this);
                if (locationManager != null)   {
                    location = locationManager!!.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    updateLatestLocation();
                }
            }

            if (isGPSEnabled)  {
                locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_TIME_BW_UPDATES,
                    MIN_DISTANCE_CHANGE_FOR_UPDATES.toFloat(), this);

                if (locationManager != null)  {
                    location = locationManager!!.getLastKnownLocation(LocationManager.GPS_PROVIDER)!!;
                    updateLatestLocation();
                }
            }

        }

        private fun updateLatestLocation() {
            if (arOverlayView != null && location != null) {
                arOverlayView!!.updateCurrentLocation(location)
                tvCurrentLocation!!.text = String.format(
                    "lat: %s \nlon: %s \naltitude: %s \n",
                    location!!.latitude, location!!.longitude, location!!.altitude
                )
            }
        }

        override fun onLocationChanged(location: Location) {
            TODO("Not yet implemented")
        }
}