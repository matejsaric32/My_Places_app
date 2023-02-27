package matejsaric32.android.myplaces2.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import matejsaric32.android.myplaces2.R
import matejsaric32.android.myplaces2.databinding.ActivityMapBinding
import matejsaric32.android.myplaces2.models.PlaceEntity

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private var binding: ActivityMapBinding? = null

    private lateinit var place: PlaceEntity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapBinding.inflate(layoutInflater)
        setContentView(binding!!.root)

        setSupportActionBar(binding!!.toolbarMap)


        if (intent.hasExtra(MainActivity.EXTRA_PLACE_DEATAILS)) {
            place = intent.getParcelableExtra<PlaceEntity>(
                MainActivity.EXTRA_PLACE_DEATAILS
            ) as PlaceEntity
        }
        if (supportActionBar != null) {
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = place.title
        }

        binding!!.toolbarMap.setNavigationOnClickListener {
            onBackPressed()
        }

        /**
         * This function to get the map.
         */

        val supportMapFragment =
            supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        supportMapFragment.getMapAsync(this)

    }

    /**
     * This function to place market on the map.
     */

    override fun onMapReady(maps: GoogleMap?) {
        val position = LatLng(place.latitude, place.longitude)
        maps?.addMarker(MarkerOptions().position(position))?.title = place.location
        maps?.moveCamera(com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom(position, 15f))
    }
}