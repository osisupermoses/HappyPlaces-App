package com.osisupermoses.happyplaces.adapters

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.osisupermoses.happyplaces.activities.AddHappyPlaceActivity
import com.osisupermoses.happyplaces.activities.HappyPlaceDetailActivity
import com.osisupermoses.happyplaces.activities.MainActivity
import com.osisupermoses.happyplaces.database.DatabaseHandler
import com.osisupermoses.happyplaces.databinding.ItemHappyPlaceBinding
import com.osisupermoses.happyplaces.models.HappyPlaceModel
import com.osisupermoses.happyplaces.utils.Constants

// (Creating an adapter class for binding it to the recyclerview in the new package which is adapters.)
// START
class HappyPlacesAdapter(
    private var context: Context,
    private var list: ArrayList<HappyPlaceModel>
) : RecyclerView.Adapter<HappyPlacesAdapter.HappyPlaceViewHolder>() {

    /**
     * A ViewHolder describes an item view and metadata about its place within the RecyclerView.
     */
    inner class HappyPlaceViewHolder(private val binding: ItemHappyPlaceBinding)
        :RecyclerView.ViewHolder(binding.root) {
        fun bindItem(model: HappyPlaceModel) {
            binding.tvTitle.text = model.title
            binding.tvDescription.text = model.description
            binding.ivPlaceImage.setImageURI(Uri.parse(model.image))

            itemView.setOnClickListener {
                val intent = Intent(context, HappyPlaceDetailActivity::class.java)
                intent.putExtra(Constants.EXTRA_PLACE_DETAILS, model)
                context.startActivity(intent)
            }
        }
    }

    /**
     * Inflates the item views which is designed in xml layout file
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HappyPlaceViewHolder {
        return HappyPlaceViewHolder(ItemHappyPlaceBinding.inflate(
            LayoutInflater.from(parent.context), parent, false)
        )
    }

    /**
     * Binds each item in the ArrayList to a view
     *
     * Called when RecyclerView needs a new {@link ViewHolder} of the given type to represent
     * an item.
     *
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     */
    override fun onBindViewHolder(holder: HappyPlaceViewHolder, position: Int) {
        val model = list[position]

        holder.bindItem(model)
    }

    /**
     * Gets the number of items in the list
     */
    override fun getItemCount(): Int {
        return list.size
    }

    fun notifyEditItem(activity: MainActivity, position: Int) {
        val intent = Intent(context, AddHappyPlaceActivity::class.java)
        intent.putExtra(Constants.EXTRA_PLACE_DETAILS, list[position])
        activity.startForResult.launch(intent)
        notifyItemChanged(position)
    }

    // (Create a function to delete the happy place details which is inserted earlier from the local storage.)
    /**
     * A function to delete the added happy place detail from the local storage.
     */
    fun removeAt(position: Int, activity: MainActivity) {

        val dbHandler = DatabaseHandler(context)
        val isDeleted = dbHandler.deleteHappyPlace(list[position])

        if (isDeleted > 0) {
            activity.deleteHappyPlaceSuccess()
            list.removeAt(position)
            notifyItemRemoved(position)
        }
    }
}
// END