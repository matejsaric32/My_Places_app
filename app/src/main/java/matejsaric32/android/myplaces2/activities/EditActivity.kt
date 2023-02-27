package matejsaric32.android.myplaces2.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.location.Geocoder
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import matejsaric32.android.myplaces2.database.PlacesDatabase
import matejsaric32.android.myplaces2.databinding.ActivityAddBinding
import matejsaric32.android.myplaces2.databinding.ActivityEditBinding
import matejsaric32.android.myplaces2.models.PlaceEntity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

class EditActivity : AppCompatActivity() {

    private lateinit var appDb : PlacesDatabase
    private var binding: ActivityEditBinding? = null
    private var calendar = Calendar.getInstance()
    private lateinit var dateSetListener: DatePickerDialog.OnDateSetListener

    private var mUriImage: Uri? = null
    private var mLatitude: Double = 0.0
    private var mLongitude: Double = 0.0

    private lateinit var geoCoder: Geocoder

    private lateinit var place : PlaceEntity

    companion object {
        private const val CAMERA = 2
        private const val IMAGE_DIRECTORY = "MyPlacesImages"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBinding.inflate(layoutInflater)
        appDb = PlacesDatabase.getInstance(this)
        geoCoder = Geocoder(this, Locale.getDefault())

        setContentView(binding?.root)

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DEATAILS)){
            place = intent.getParcelableExtra<PlaceEntity>(
                MainActivity.EXTRA_PLACE_DEATAILS) as PlaceEntity
            Toast.makeText(this, "Place: ${place.title}", Toast.LENGTH_SHORT).show()
        }

        setSupportActionBar(binding?.toolbarEditPlace)

        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = place.title
        }
        binding?.toolbarEditPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        /**
         * Call function that sets data to form.
         */

        setFormEditFields()

        /**
         *  Initializer for Datetimepicker dialog.
         */

        dateSetListener = DatePickerDialog.OnDateSetListener {
                view, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            updateDateInView()
        }

        /**
         * A click event for calender icon to open the calender to select the date.
         */

        binding?.etDate?.setOnClickListener {
            DatePickerDialog(
                this@EditActivity,
                dateSetListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        /**
         * A click event for selecting the location manually.
         */

        binding?.etLocation?.setOnClickListener{
            try {
                val fields = listOf(
                    Place.Field.ID, Place.Field.NAME,
                    Place.Field.LAT_LNG, Place.Field.ADDRESS)
                val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                    .build(this)
                startAutocomplete.launch(intent)
            } catch (e: Exception) {
                e.printStackTrace()

            }
        }

        /**
         * A click event for selecting the current location.
         */

        binding?.tvSelectCurrentLocation?.setOnClickListener {
            if (isLocationEndabled()){
                getAccessToLocation()
            }else{
                Toast.makeText(this, "Your location is222", Toast.LENGTH_SHORT).show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }

        /**
         * A click event for selecting the image from phone storage or camera.
         */
        binding?.tvSelectImage?.setOnClickListener {
            Log.i("Request Code", "GALLERY2222")

            val pictureDialog = android.app.AlertDialog.Builder(this)
            pictureDialog.setTitle("Select Action")
            val pictureDialogItems = arrayOf("Select photo from gallery",
                "Capture photo from camera")
            pictureDialog.setItems(pictureDialogItems) {
                    dialog, which ->
                when (which) {
                    0 -> choosePhotoFromGallary()
                    1 -> takePhotoFromCamera()
                }
            }
            pictureDialog.show()
        }

        /**
         * A click event for saving the place details to the database.
         */

        binding?.tvSelectImage?.setOnClickListener {
            Log.i("Request Code", "GALLERY2222")

            val pictureDialog = android.app.AlertDialog.Builder(this)
            pictureDialog.setTitle("Select Action")
            val pictureDialogItems = arrayOf("Select photo from gallery",
                "Capture photo from camera")
            pictureDialog.setItems(pictureDialogItems) {
                    dialog, which ->
                when (which) {
                    0 -> choosePhotoFromGallary()
                    1 -> takePhotoFromCamera()
                }
            }
            pictureDialog.show()
        }

        binding?.btnSave?.setOnClickListener{
            editPlace()
        }

    }

    /**
     * Part of initializers for location picker.
     */

    private val startAutocomplete =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data!!
                val place = Autocomplete.getPlaceFromIntent(data)
                binding?.etLocation?.setText(place.address)
                mLatitude = place.latLng!!.latitude
                mLongitude = place.latLng!!.longitude
                Log.e("Place", "Place: ${mLatitude} - ${mLongitude}")
            } else if (result.resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show()
            }
        }

    /**
     * Function to check if the location is enabled or not and then gets current location.
     */

    private fun getAccessToLocation(){
        Dexter.withActivity(this)
            .withPermissions(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    if (report!!.areAllPermissionsGranted()){
                        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
                        try {
                            val location =
                                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            Log.e("Location", "Location: ${location?.latitude} - ${location?.longitude}")

                            mLatitude = location?.latitude!!
                            mLongitude = location?.longitude!!

                            val address = geoCoder?.getFromLocation(mLatitude, mLongitude, 1)

                            Log.e("Address", "Address: ${address?.get(0)?.locality} ${address?.get(0)?.countryName}")
                            binding?.etLocation?.setText("${address?.get(0)?.locality}, ${address?.get(0)?.countryName}")

                        }catch (e: SecurityException) {
                            e.printStackTrace()
                        }

                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }

            }).onSameThread()
            .check()
    }

    /**
     * Function to show the dialog if the location permission is denied.
     */
    private fun isLocationEndabled(): Boolean {
        val locationManager: LocationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    /**
     * Function to edit instance of place in database and validates the form.
     */

    private fun editPlace(){
        val title = binding?.etTitle?.text.toString()
        val description = binding?.etDescription?.text.toString()
        val date = binding?.etDate?.text.toString()
        val location = binding?.etLocation?.text.toString()
        val image = mUriImage.toString()

        if (title.isEmpty()){
            Toast.makeText(this, "Please enter title", Toast.LENGTH_SHORT).show()
        } else if (description.isEmpty()){
            Toast.makeText(this, "Please enter description", Toast.LENGTH_SHORT).show()
        } else if (date.isEmpty()){
            Toast.makeText(this, "Please enter date", Toast.LENGTH_SHORT).show()
        } else if (location.isEmpty()){
            Toast.makeText(this, "Please enter location", Toast.LENGTH_SHORT).show()
        } else if (image.isEmpty()){
            Toast.makeText(this, "Please enter image", Toast.LENGTH_SHORT).show()
        } else {
            val place = PlaceEntity(place.id, title, date, description, location, mLatitude, mLongitude, mUriImage.toString())

            GlobalScope.launch(Dispatchers.IO) {
                appDb.getPlacesDao().updatePlace(place)
            }
            Toast.makeText(this, "Place updated successfully", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    /**
     * A function to setup form fields with the data.
     */

    private fun setFormEditFields(){
        binding?.etTitle?.setText(place.title)
        binding?.etDescription?.setText(place.description)
        binding?.etDate?.setText(place.date)
        binding?.etLocation?.setText(place.location)
        binding?.ivImageLocation?.setImageURI(Uri.parse(place.imagePath))
        mUriImage = Uri.parse(place.imagePath)
    }

    /**
     * A function to launch the camera and take the image.
     */
    private fun updateDateInView() {
        val myFormat = "dd.MM.yyyy"
        val sdf = SimpleDateFormat(myFormat, Locale.getDefault())
        binding?.etDate?.setText(sdf.format(calendar.time).toString())
    }

    /**
     * A function to launch the gallery and select the image.
     */
    private fun choosePhotoFromGallary() {
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                    if (report!!.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                        openGalleryLauncher.launch(galleryIntent)

                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread()
            .check()

    }

    val openGalleryLauncher: ActivityResultLauncher<Intent>
            = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
            result->
        if (result.resultCode == RESULT_OK && result.data != null){
            binding?.ivImageLocation?.setImageURI(result.data?.data)
            mUriImage = saveImage(MediaStore.Images.Media.getBitmap(this.contentResolver, result.data?.data))
//            mUriImage = result.data?.data
        }
    }

    /**
     * A function to show the error dialog if the permission is not granted.
     */
    fun showRationalDialogForPermissions() {
        android.app.AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions required for this feature. It can be enabled under Application Settings")
            .setPositiveButton("GO TO SETTINGS") { _, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = android.net.Uri.fromParts("package", packageName, null)
                    intent.data = uri
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }.show()
    }

    /**
     * A function to launch the camera and take the image.
     */

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == EditActivity.CAMERA) {
                val thumbnail: Bitmap = data!!.extras!!.get("data") as Bitmap
                binding?.ivImageLocation?.setImageBitmap(thumbnail)
                mUriImage = saveImage(thumbnail)
//               saveImage(thumbnail)
                Log.e("Saved Image : ", "Path :: ${mUriImage.toString()}")
                Toast.makeText(this@EditActivity, "Image Saved!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * A function to save the image to internal storage.
     */

    private fun saveImage(bitmap: Bitmap) : Uri{

        val wrapper = ContextWrapper(applicationContext)
        var file = wrapper.getDir(IMAGE_DIRECTORY, Context.MODE_PRIVATE)
        file = File(file, "${UUID.randomUUID()}.jpg")

        try {
            val stream: OutputStream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            stream.flush()
            stream.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }
        return Uri.parse(file.absolutePath)
    }

    /**
     * A function to launch the camera and take the image.
     */

    private fun takePhotoFromCamera() {
        Dexter.withActivity(this).withPermissions(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.CAMERA)
            .withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {

                    if (report!!.areAllPermissionsGranted()) {
                        val galleryIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

                        startActivityForResult(galleryIntent, EditActivity.CAMERA)
                    }
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                    showRationalDialogForPermissions()
                }
            }).onSameThread()
            .check()
    }
}