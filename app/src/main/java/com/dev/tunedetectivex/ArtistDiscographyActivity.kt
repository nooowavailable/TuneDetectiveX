package com.dev.tunedetectivex

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale

class ArtistDiscographyActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var apiService: DeezerApiService
    private lateinit var db: AppDatabase
    private var artistId: Long = 0
    private lateinit var artistName: String
    private lateinit var artistImageUrl: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_artist_discography)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recyclerView = findViewById(R.id.recyclerViewDiscography)
        recyclerView.layoutManager = LinearLayoutManager(this)

        artistId = intent.getLongExtra("artistId", 0)
        artistName = intent.getStringExtra("artistName") ?: "Unknown Artist"
        artistImageUrl = intent.getStringExtra("artistImageUrl") ?: ""

        supportActionBar?.title = artistName

        setupApiService()
        db = AppDatabase.getDatabase(applicationContext)

        loadArtistDiscography()
    }

    private fun setupApiService() {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.deezer.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(DeezerApiService::class.java)
    }

    private fun loadArtistDiscography() {
        apiService.getArtistReleases(artistId).enqueue(object : Callback<DeezerAlbumsResponse> {
            override fun onResponse(
                call: Call<DeezerAlbumsResponse>,
                response: Response<DeezerAlbumsResponse>
            ) {
                if (response.isSuccessful) {
                    val releases = response.body()?.data ?: emptyList()
                    Log.d("ArtistDiscographyActivity", "Releases: $releases") // Log the releases
                    if (releases.isNotEmpty()) {
                        displayDiscography(releases)
                    } else {
                        Toast.makeText(
                            this@ArtistDiscographyActivity,
                            "No releases found for $artistName.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    Log.e(
                        "ArtistDiscographyActivity",
                        "Error fetching discography: ${response.code()} ${response.message()}"
                    )
                    Toast.makeText(
                        this@ArtistDiscographyActivity,
                        "Error fetching discography.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<DeezerAlbumsResponse>, t: Throwable) {
                Log.e("ArtistDiscographyActivity", "Error fetching discography", t)
                Toast.makeText(
                    this@ArtistDiscographyActivity,
                    "Error fetching discography.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    private fun displayDiscography(releases: List<DeezerAlbum>) {
        val sortedReleases = releases.sortedByDescending { release ->
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(release.release_date)?.time
                ?: 0L
        }

        val adapter = DiscographyAdapter(sortedReleases) { album ->
            Toast.makeText(this, "Clicked on: ${album.title}", Toast.LENGTH_SHORT).show()
        }
        recyclerView.adapter = adapter
        val imageView: ImageView = findViewById(R.id.imageViewArtist)
        val textView: TextView = findViewById(R.id.textViewArtistName)

        // Load the image into the ImageView
        Glide.with(this)
            .load(artistImageUrl)
            .apply(
                RequestOptions()
                    .centerCrop()
            ) // Maintain the aspect ratio
            .into(imageView)

        // Set the artist name in the TextView
        textView.text = artistName

        // Apply fade-in animation to the TextView
        val fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in)
        textView.startAnimation(fadeInAnimation)

        recyclerView.visibility = View.VISIBLE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}