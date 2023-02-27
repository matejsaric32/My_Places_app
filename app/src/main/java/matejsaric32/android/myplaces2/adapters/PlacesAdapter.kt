package matejsaric32.android.myplaces2.adapters

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import matejsaric32.android.myplaces2.activities.AddActivity
import matejsaric32.android.myplaces2.activities.EditActivity
import matejsaric32.android.myplaces2.activities.MainActivity
import matejsaric32.android.myplaces2.database.PlacesDatabase
import matejsaric32.android.myplaces2.databinding.PlacesEntryBinding
import matejsaric32.android.myplaces2.models.PlaceEntity
import androidx.lifecycle.lifecycleScope

class PlacesAdapter(
    val context: Context,
    val items : ArrayList<PlaceEntity>) : RecyclerView.Adapter<PlacesAdapter.ViewHolder>(){

    private var onClickListener: OnClickListener? = null

    class ViewHolder(bind: PlacesEntryBinding) : RecyclerView.ViewHolder(bind.root) {
        val tvTitle = bind?.tvTitle
        val tvDescription = bind?.tvDescription
        val sivPicture = bind?.sivPlaceImage
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            PlacesEntryBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val model: PlaceEntity = items[position]

        if (holder is ViewHolder) {
            holder.tvTitle?.text = model.title
            holder.tvDescription?.text = model.description
            holder.sivPicture?.setImageURI(Uri.parse(model.imagePath))

            holder.itemView.setOnClickListener {
                onClickListener?.onClick(position, model)
            }
        }

    }

    fun notifyEditItem(activity: Activity, position: Int) {
        val intent = Intent(context, EditActivity::class.java)
        intent.putExtra(MainActivity.EXTRA_PLACE_DEATAILS, items[position])
        startActivity(intent@ activity, intent, null)

        notifyItemChanged(position)
    }

    override fun getItemCount(): Int {
        return items.size
    }


    interface OnClickListener {
        fun onClick(position: Int, model: PlaceEntity)
    }

    fun setOnClickListener(onClickListener: OnClickListener) {
        this.onClickListener = onClickListener
    }

}