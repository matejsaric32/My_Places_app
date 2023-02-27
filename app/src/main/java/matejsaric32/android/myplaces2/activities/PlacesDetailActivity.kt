package matejsaric32.android.myplaces2.activities

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import matejsaric32.android.myplaces2.databinding.ActivityPlacesDetailBinding
import matejsaric32.android.myplaces2.models.PlaceEntity

class PlacesDetailActivity : AppCompatActivity() {

    private var binding: ActivityPlacesDetailBinding? = null
    private lateinit var place: PlaceEntity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPlacesDetailBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        setSupportActionBar(binding?.toolbarDetailPlace)

        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DEATAILS)){
            place = intent.getParcelableExtra<PlaceEntity>(
                MainActivity.EXTRA_PLACE_DEATAILS) as PlaceEntity

            binding?.tvDescription?.text = place.description
            binding?.tvLocation?.text = place.location
            binding?.tvDate?.text = place.date
            binding?.ivPlaceImage?.setImageURI(Uri.parse(place.imagePath))
        }

        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = place.title
        }

        binding?.toolbarDetailPlace?.setNavigationOnClickListener {
            onBackPressed()
        }

        /**
         * This function to move the map activity.
         */

        binding?.btnShowOnMap?.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            intent.putExtra(MainActivity.EXTRA_PLACE_DEATAILS, place)
            startActivity(intent)
        }
    }
}