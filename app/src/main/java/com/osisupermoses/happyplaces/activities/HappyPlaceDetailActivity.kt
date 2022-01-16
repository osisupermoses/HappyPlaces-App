package com.osisupermoses.happyplaces.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import com.osisupermoses.happyplaces.databinding.ActivityHappyPlaceDetailBinding
import com.osisupermoses.happyplaces.models.HappyPlaceModel
import com.osisupermoses.happyplaces.utils.Constants


class HappyPlaceDetailActivity : BaseActivity() {

    private lateinit var binding: ActivityHappyPlaceDetailBinding
    private var happyPlaceDetailModel: HappyPlaceModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHappyPlaceDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (intent.hasExtra(Constants.EXTRA_PLACE_DETAILS)) {
            happyPlaceDetailModel = intent.getSerializableExtra(
                Constants.EXTRA_PLACE_DETAILS) as HappyPlaceModel
        }

        if (happyPlaceDetailModel != null) {
            setSupportActionBar(binding.toolbarHappyPlaceDetail)
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.title = happyPlaceDetailModel!!.title

            binding.toolbarHappyPlaceDetail.setNavigationOnClickListener {
                onBackPressed()
            }

            binding.ivPlaceImage.setImageURI(Uri.parse(happyPlaceDetailModel!!.image))
            binding.tvDescription.text = happyPlaceDetailModel!!.description
            binding.tvLocation.text = happyPlaceDetailModel!!.location
        }

        // (Add an click event for button view on map)
        binding.btnViewOnMap.setOnClickListener {
            val intent = Intent(this@HappyPlaceDetailActivity, MapsActivity::class.java)
            intent.putExtra(Constants.EXTRA_PLACE_DETAILS, happyPlaceDetailModel)
            startActivity(intent)
        }
    }
}