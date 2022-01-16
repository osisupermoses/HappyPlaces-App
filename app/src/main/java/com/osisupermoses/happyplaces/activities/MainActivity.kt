package com.osisupermoses.happyplaces.activities

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.osisupermoses.happyplaces.R
import com.osisupermoses.happyplaces.adapters.HappyPlacesAdapter
import com.osisupermoses.happyplaces.database.DatabaseHandler
import com.osisupermoses.happyplaces.databinding.ActivityMainBinding
import com.osisupermoses.happyplaces.models.HappyPlaceModel
import com.osisupermoses.happyplaces.utils.SwipeToDeleteCallback
import com.osisupermoses.happyplaces.utils.SwipeToEditCallback

open class MainActivity : BaseActivity() {
    private lateinit var binding: ActivityMainBinding

    val startForResult =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == Activity.RESULT_OK) {
            getHappyPlacesListFromLocalDB()
        } else {
            Log.e("Activity","Operation cancelled or back pressed")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.fabAddHappyPlace.setOnClickListener {
            val intent = Intent(this@MainActivity, AddHappyPlaceActivity::class.java)
            startForResult.launch(intent)
        }
        getHappyPlacesListFromLocalDB()
    }

    private fun getHappyPlacesListFromLocalDB() {
        val dbHandler = DatabaseHandler(this)
        val getHappyPlacesList: ArrayList<HappyPlaceModel> = dbHandler.getHappyPlacesList()

        if (getHappyPlacesList.size > 0) {
            binding.rvHappyPlacesList.visibility = View.VISIBLE
            binding.tvNoRecordsAvailable.visibility = View.GONE
            setUpHappyPlacesRecyclerView(getHappyPlacesList)
        } else {
            binding.rvHappyPlacesList.visibility = View.GONE
            binding.tvNoRecordsAvailable.visibility = View.VISIBLE
        }
    }

    private fun setUpHappyPlacesRecyclerView(
        happyPlacesList: ArrayList<HappyPlaceModel>) {

        binding.rvHappyPlacesList.layoutManager = LinearLayoutManager(this)
        binding.rvHappyPlacesList.setHasFixedSize(true)
        val happyPlacesAdapter = HappyPlacesAdapter(this@MainActivity, happyPlacesList)
        binding.rvHappyPlacesList.adapter = happyPlacesAdapter

        val editSwipeHandler = object : SwipeToEditCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = binding.rvHappyPlacesList.adapter as HappyPlacesAdapter
                adapter.notifyEditItem(
                    this@MainActivity,
                    viewHolder.adapterPosition
                )
            }
        }
        val editItemTouchHelper = ItemTouchHelper(editSwipeHandler)
        editItemTouchHelper.attachToRecyclerView(binding.rvHappyPlacesList)

        val deleteSwipeHandler = object: SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                showProgressDialog(resources.getString(R.string.please_wait))
                // (Call the adapter function when it is swiped for delete)
                val adapter = binding.rvHappyPlacesList.adapter as HappyPlacesAdapter
                adapter.removeAt(viewHolder.adapterPosition, this@MainActivity)

                getHappyPlacesListFromLocalDB() // Gets the latest list from the local database after item being delete from it.

            }
        }
        val deleteItemTouchHelper = ItemTouchHelper(deleteSwipeHandler)
        deleteItemTouchHelper.attachToRecyclerView(binding.rvHappyPlacesList)
    }

    fun deleteHappyPlaceSuccess() {
        hideProgressDialog()
        Toast.makeText(
            this,
            resources.getString(R.string.err_your_happyPlace_deleted_successfully),
            Toast.LENGTH_SHORT
        ).show()
    }

    override fun onBackPressed() {
        doubleBackToExit()
    }
}