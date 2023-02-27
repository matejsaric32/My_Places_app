package matejsaric32.android.myplaces2.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import matejsaric32.android.myplaces2.adapters.PlacesAdapter
import matejsaric32.android.myplaces2.database.PlacesDatabase
import matejsaric32.android.myplaces2.databinding.ActivityMainBinding
import matejsaric32.android.myplaces2.models.PlaceEntity
import matejsaric32.android.myplaces2.utils.SwipeToDeleteCallback
import pl.kitek.rvswipetodelete.SwipeToEditCallback

class MainActivity : AppCompatActivity() {

    private lateinit var appDb : PlacesDatabase

    private lateinit var placesAdapter : PlacesAdapter

    private var binding: ActivityMainBinding? = null
    companion object {
        internal const val EXTRA_PLACE_DEATAILS = "extra_place_details"
        internal const val ADD_PLACE_ACTIVITY_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        appDb = PlacesDatabase.getInstance(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)

        binding?.fabAddNewPlace?.setOnClickListener {
            val intent = Intent(this, AddActivity::class.java)
            startActivityForResult(intent, ADD_PLACE_ACTIVITY_REQUEST_CODE)
        }

        /**
         * Calls function to get all places from database and displays them in recycler view and control edit and delete swipe
         */

        getAllPlaces()

        binding?.rvPlacesList?.setOnClickListener {
            Toast.makeText(this, "Clicked", Toast.LENGTH_SHORT).show()
        }


    }

    override fun onResume() {
        super.onResume()
        val size = appDb.getPlacesDao().getAllPlaces()

    }

    /**
     * Gets all places from database and displays them in recycler view and control edit and delete swipe
     */

    fun getAllPlaces() {

        lifecycleScope.launch{
            appDb.getPlacesDao().getAllPlaces().collect{
                    allPlaces ->
                if (allPlaces.isEmpty()){
                    binding?.rvPlacesList?.visibility = View.INVISIBLE
                    binding?.tvNoPlacesAdded?.visibility = View.VISIBLE
                } else {
                    binding?.rvPlacesList?.visibility = View.VISIBLE
                    binding?.tvNoPlacesAdded?.visibility = View.INVISIBLE

                    binding?.rvPlacesList?.layoutManager = LinearLayoutManager(this@MainActivity)

                    val places = ArrayList<PlaceEntity>()
                    for (place in allPlaces){
                        places.add(place)
                    }

                    /**
                     * Sets items adapter
                     */

                    var placesAdapter = PlacesAdapter(this@MainActivity, places)
                    binding?.rvPlacesList?.adapter = placesAdapter

                    /**
                     * Sets click listener for items to open detail activity
                     */

                    placesAdapter.setOnClickListener(object : PlacesAdapter.OnClickListener{
                        override fun onClick(position: Int, model: PlaceEntity) {
                            val intent = Intent(this@MainActivity, PlacesDetailActivity::class.java)
                            intent.putExtra(EXTRA_PLACE_DEATAILS, model)
                            startActivity(intent)
                        }
                    })

                    /**
                     * Sets swipe to edit, opens edit activity
                     */

                    val editSwipeHandler = object : SwipeToEditCallback(this@MainActivity) {
                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                            val adapter = binding?.rvPlacesList?.adapter as PlacesAdapter
                            adapter.notifyEditItem(this@MainActivity, viewHolder.adapterPosition)
                        }
                    }

                    val editeSwipeHalper = ItemTouchHelper(editSwipeHandler)
                    editeSwipeHalper.attachToRecyclerView(binding?.rvPlacesList)

                    /**
                     * Sets swipe to delete, deletes item from database
                     */

                    val deleteSwipeHandler = object : SwipeToDeleteCallback(this@MainActivity) {
                        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                            val adapter = binding?.rvPlacesList?.adapter as PlacesAdapter
                            Toast.makeText(this@MainActivity, "Deleted: ${viewHolder.adapterPosition}", Toast.LENGTH_SHORT).show()

                            lifecycleScope.launch {
                                Toast.makeText(this@MainActivity, "Deleted: ${viewHolder.adapterPosition} \n" +
                                        "${places.size}  ${adapter.items.size}", Toast.LENGTH_SHORT).show()
                                appDb.getPlacesDao().deletePlaceById(adapter.items[viewHolder.adapterPosition].id!!)
                                getAllPlaces()
                            }
                        }
                    }

                    val deleteSwipeHelper = ItemTouchHelper(deleteSwipeHandler)
                    deleteSwipeHelper.attachToRecyclerView(binding?.rvPlacesList)
                }
            }
        }
    }
}