package com.osisupermoses.happyplaces.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.osisupermoses.happyplaces.R
import com.osisupermoses.happyplaces.database.DatabaseHandler
import com.osisupermoses.happyplaces.databinding.ActivityAddHappyPlaceBinding
import com.osisupermoses.happyplaces.models.HappyPlaceModel
import com.osisupermoses.happyplaces.utils.CoilLoader
import com.osisupermoses.happyplaces.utils.Constants
import com.osisupermoses.happyplaces.utils.Constants.IMAGE_DIRECTORY
import com.osisupermoses.happyplaces.utils.GetAddressFromLatLng
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.lang.Exception
import java.text.SimpleDateFormat
import java.util.*

class AddHappyPlaceActivity : BaseActivity(), View.OnClickListener {

    private lateinit var binding: ActivityAddHappyPlaceBinding
    private var cal = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener


    private var saveImageToInternalStorage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0
    private var mHappyPlaceDetails: HappyPlaceModel? = null
    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    @RequiresApi(Build.VERSION_CODES.P)
    private val openGalleryLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val uri: Uri? = result.data?.data

                @Suppress("DEPRECATION")
                val bitmap = when {
                    Build.VERSION.SDK_INT < 28 -> MediaStore.Images.Media.getBitmap(
                        this.contentResolver,
                        uri
                    )
                    else -> {
                        val source =
                            uri?.let { ImageDecoder.createSource(this.contentResolver, it) }
                        ImageDecoder.decodeBitmap(source!!)
                    }
                }

                saveImageToInternalStorage = saveImageToInternalStorage(bitmap)
                Log.e("Saved Image: ", "Path :: $saveImageToInternalStorage")

                CoilLoader(this@AddHappyPlaceActivity)
                    .loadPicture(uri!!,binding.ivPlaceImage)
//                binding.ivPlaceImage.setImageURI(uri)

            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(
                    this@AddHappyPlaceActivity,
                    "You didn't select any image.", Toast.LENGTH_SHORT
                ).show()
                // A log is printed when user close or cancel the image selection.
                Log.e("Request Cancelled", "Image selection cancelled")
            }
    }

    private val openCameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val thumbnail: Bitmap = result.data?.extras?.get("data") as Bitmap

                saveImageToInternalStorage = saveImageToInternalStorage(thumbnail)
                Log.e("Saved Image: ", "Path :: $saveImageToInternalStorage")

                binding.ivPlaceImage.setImageBitmap(thumbnail)

            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Toast.makeText(
                    this@AddHappyPlaceActivity,
                    "You didn't capture any image.", Toast.LENGTH_SHORT
                ).show()
                // A log is printed when user close or cancel the image selection.
                Log.e("Request Cancelled", "Image selection cancelled")
            }
    }
    // (Receive the valid result as we required from the Place Picker.)
    private val placePicker =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val place: Place = Autocomplete.getPlaceFromIntent(result.data!!)

                binding.etLocation.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
            } else if (result.resultCode == Activity.RESULT_CANCELED) {
                Log.e("Cancelled", "Cancelled")
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddHappyPlaceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarAddPlace)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarAddPlace.setNavigationOnClickListener {
            onBackPressed()
        }

        dateSetListener =
            DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, monthOfYear)
                cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // (Called a function as updateDateInView where after selecting a date from date picker is populated in the UI component.)
                updateDateInView()
            }

        updateDateInView() // date is created automatically
        binding.etDate.setOnClickListener(this)
        binding.tvAddImage.setOnClickListener(this)
        binding.btnSave.setOnClickListener(this)
        binding.etLocation.setOnClickListener(this)
        binding.tvSelectCurrentLocation.setOnClickListener(this)

        if (intent.hasExtra(Constants.EXTRA_PLACE_DETAILS)) {
            mHappyPlaceDetails = intent.getSerializableExtra(
                Constants.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        if (mHappyPlaceDetails != null) {
            supportActionBar!!.title = "Edit Happy Place"

            binding.etTitle.setText(mHappyPlaceDetails!!.title)
            binding.etDescription.setText(mHappyPlaceDetails!!.description)
            binding.etDate.setText(mHappyPlaceDetails!!.date)
            binding.etLocation.setText(mHappyPlaceDetails!!.location)
            mLatitude = mHappyPlaceDetails!!.latitude
            mLongitude = mHappyPlaceDetails!!.longitude
            saveImageToInternalStorage = Uri.parse(mHappyPlaceDetails!!.image)

            binding.ivPlaceImage.setImageURI(saveImageToInternalStorage)
            binding.btnSave.text = getString(R.string.btnUpdate)
        }

        // (Initialize the places sdk if it is not initialized earlier.)
        /**
         * Initialize the places sdk if it is not initialized earlier using the api key.
         */
        if (!Places.isInitialized()) {
            Places.initialize(this@AddHappyPlaceActivity,
                resources.getString(R.string.google_maps_api_key))
        }

        // (Initialize the Fused location variable)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onClick(v: View?) {
        when (v!!.id) {
                R.id.et_date -> {
                DatePickerDialog(
                    this@AddHappyPlaceActivity,
                    dateSetListener,
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
                ).show()
            }
            R.id.tv_add_image -> {
                val pictureDialog = AlertDialog.Builder(this)
                pictureDialog.setTitle("Select Action")
                val pictureDialogItems = arrayOf("Select photo from Gallery",
                        "Capture photo from camera")
                pictureDialog.setItems(pictureDialogItems){ _, which ->
                    when (which) {
                        0 -> choosePhotoFromGallery()
                        1 -> takePhotoFromCamera()
                    }
                }
                pictureDialog.show()
            }
            R.id.btn_save -> {
                when {
                    binding.etTitle.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this,
                            "Please enter title",
                            Toast.LENGTH_SHORT).show()
                    }
                    binding.etDescription.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this,
                            "Please enter a description",
                            Toast.LENGTH_SHORT).show()
                    }
                    binding.etLocation.text.isNullOrEmpty() -> {
                        Toast.makeText(
                            this,
                            "Please enter a location",
                            Toast.LENGTH_SHORT).show()
                    }
                    saveImageToInternalStorage == null -> {
                        Toast.makeText(
                            this,
                            "Please enter an image",
                            Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        val happyPlaceModel = HappyPlaceModel(
                            if (mHappyPlaceDetails == null) 0 else mHappyPlaceDetails!!.id,  // this let's know when to add new entry t0 database and when to update an existing entry in our db
                            binding.etTitle.text.toString(),
                            saveImageToInternalStorage.toString(),
                            binding.etDescription.text.toString(),
                            binding.etDate.text.toString(),
                            binding.etLocation.text.toString(),
                            mLatitude,
                            mLongitude
                        )
                        // Here we initialize the database handler class.
                        val dbHandler = DatabaseHandler(this)

                        // (Call add or update details conditionally.)
                        if(mHappyPlaceDetails == null) {
                            val addHappyPlace = dbHandler.addHappyPlace(happyPlaceModel)

                            if (addHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                Toast.makeText(
                                    this,
                                    "The happy place details are inserted successfully.",
                                    Toast.LENGTH_SHORT).show()
                                finish() //finishing activity
                            }
                        } else {
                            val updateHappyPlace = dbHandler.updateHappyPlace(happyPlaceModel)

                            if (updateHappyPlace > 0) {
                                setResult(Activity.RESULT_OK)
                                Toast.makeText(
                                    this,
                                    "The happy place details are updated successfully.",
                                    Toast.LENGTH_SHORT).show()
                                finish() //finishing activity
                            }
                        }
                    }
                }
            }
            // (Add an onClick event on the location for place picker)
            R.id.et_location -> {
                try {
                    // These are the list of fields which we required is passed
                    val fields = listOf(
                        Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG,
                        Place.Field.ADDRESS
                    )
                    // Start the autocomplete intent.
                    val intent =
                        Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            .build(this@AddHappyPlaceActivity)
                    placePicker.launch(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            // (Add a click event for selecting the current location)
            R.id.tv_select_current_location -> {
                if (!isLocationEnabled()) {
                    Toast.makeText(
                        this,
                        "Your location provider is turned off. Please turn it on.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // This will redirect you to settings from where you need to turn on the location provider.
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(intent)
                } else {
                    locationAccessAndPlacesPicker()
                }

            }
        }
    }

    /**
     * A function to update the selected date in the UI with selected format.
     * This function is created because every time we don't need to add format which we have added here to show it in the UI.
     */
    private fun updateDateInView() {
        val myFormat = "dd-MM-yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault()) // A date format
        binding.etDate.setText(sdf.format(cal.time).toString()) // A selected date using format which we have used is set to the UI.
    }

    private fun takePhotoFromCamera() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
            )
            .withListener(object: MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport)
                {
                    if (report.areAllPermissionsGranted()) {
                        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                        openCameraLauncher.launch(cameraIntent)
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                )
                {
                    showRationaleDialogForPermissions("It looks like you have turned off permission " +
                            "required for this feature. It can be enabled under the Applications Settings ")
                }
            }).onSameThread().check()
    }

    // Using Third-Party Library DEXTER for permission handling
    private fun choosePhotoFromGallery() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            .withListener(object: MultiplePermissionsListener {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onPermissionsChecked(report: MultiplePermissionsReport)
                {
                    if (report.areAllPermissionsGranted()) {

                        val galleryIntent = Intent(Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLauncher.launch(galleryIntent)
                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                )
                {
                    showRationaleDialogForPermissions("It looks like you have " +
                            "turned off permission required for this feature. It can be " +
                            "enabled under the Applications Settings ")
                }
            }).onSameThread().check()
    }

    private fun locationAccessAndPlacesPicker() {
        Dexter.withContext(this)
            .withPermissions(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            .withListener(object: MultiplePermissionsListener {
                @RequiresApi(Build.VERSION_CODES.P)
                override fun onPermissionsChecked(report: MultiplePermissionsReport)
                {
                    if (report.areAllPermissionsGranted()) {

                        requestNewLocationData()

                    }
                }
                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>,
                    token: PermissionToken
                ) {
                    showRationaleDialogForPermissions("Location access required for " +
                            "this feature is denied. It can be enabled under the Applications " +
                            "Settings")
                }
            }).onSameThread()
            .check()
    }

    private fun showRationaleDialogForPermissions(errorMessage: String) {
        AlertDialog.Builder(this).setMessage(errorMessage)
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    if (View.generateViewId() != R.id.tv_select_current_location) {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        val uri = Uri.fromParts("package", packageName, null)
                        intent.data = uri
                        startActivity(intent)
                    }
                } catch (e: ActivityNotFoundException) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") {dialog,_ ->
                dialog.dismiss()
            }.show()
    }

    private fun saveImageToInternalStorage(bitmap: Bitmap): Uri {
        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    // (Create a function to check the GPS is enabled or not.)
    /**
     * A function which is used to verify that the location or let's GPS is enable or not of the user's device.
     */
    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // (Create a method or let say function to request the current location. Using the fused location provider client.)
    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestNewLocationData() {

        val mLocationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 0  // value is in milliseconds, therefore if you want to repeatedly request for location, you should set it to a large number
            fastestInterval = 1
            numUpdates = 1
        }

        Looper.myLooper()?.let {
            mFusedLocationClient.requestLocationUpdates(
                mLocationRequest, mLocationCallback,
                it
            )
        }
    }

    // (Create a location callback object of fused location provider client where we will get the current location details.)
    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
//            super.onLocationResult(locationResult)

            // Normally, you want to save a new location to a database. We are simplifying
            // things a bit and just saving it as a local variable, as we only need it again
            // if a Notification is created (when the user navigates away from app).
            val mLastLocation: Location = locationResult.lastLocation
            mLatitude = mLastLocation.latitude
            Log.e("Current Latitude", "$mLatitude")
            mLongitude = mLastLocation.longitude
            Log.e("Current Longitude", "$mLongitude")

            val addressTask = GetAddressFromLatLng(
                this@AddHappyPlaceActivity,
                mLatitude, mLongitude
            )

            addressTask.setAddressListener(object :
                GetAddressFromLatLng.AddressListener {
                override fun onAddressFound(address: String?) {
                    Log.e("Address ::", "" + address)
                    binding.etLocation.setText(address) // Address is set to the edittext
                }

                override fun onError() {
                    Log.e("Get Address ::", "Something is wrong...")
                }
            })
            addressTask.getAddress()
        }
    }
}