package com.example.mediaaccessdemo

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewTreeObserver.OnPreDrawListener
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private lateinit var cardLayout: CardView
    private lateinit var textView: TextView
    private lateinit var button: Button
    private lateinit var adapter: AlbumAdapter
    private lateinit var recyclerView: RecyclerView

    private val imageList = ArrayList<Image>()

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { _ ->
            checkPermissions()
            loadImages()
        }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissionLauncher.launch(
                arrayOf(
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_VIDEO,
                    READ_MEDIA_VISUAL_USER_SELECTED
                )
            )
        } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(
                arrayOf(
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_VIDEO
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    READ_EXTERNAL_STORAGE
                )
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardLayout = findViewById(R.id.card_layout)
        textView = findViewById(R.id.text_view)
        button = findViewById(R.id.button)
        recyclerView = findViewById(R.id.recycler_view)

        button.setOnClickListener { requestPermissions() }

        recyclerView.viewTreeObserver.addOnPreDrawListener(object : OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                recyclerView.viewTreeObserver.removeOnPreDrawListener(this)
                val columns = 3
                val imageSize = recyclerView.width / columns

                adapter = AlbumAdapter(this@MainActivity, imageList, imageSize)
                recyclerView.layoutManager = GridLayoutManager(this@MainActivity, columns)
                recyclerView.adapter = adapter

                loadImages()

                return false
            }
        })

        checkPermissions()
    }

    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && (ContextCompat.checkSelfPermission(this, READ_MEDIA_IMAGES) == PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(this, READ_MEDIA_VIDEO) == PERMISSION_GRANTED)) {
            // Full access on Android 13 (API level 33) or above
            cardLayout.visibility = View.GONE
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
            && ContextCompat.checkSelfPermission(this, READ_MEDIA_VISUAL_USER_SELECTED) == PERMISSION_GRANTED) {
            // Partial access on Android 14 (API level 34) and higher
            textView.text = "您已授權存取部分相簿中的照片或影片"
            button.text = "管理"
            cardLayout.visibility = View.VISIBLE
        } else if (ContextCompat.checkSelfPermission(this, READ_EXTERNAL_STORAGE) == PERMISSION_GRANTED) {
            // Full access up to Android 12 (API level 32)
            cardLayout.visibility = View.GONE
        } else {
            textView.text = "您尚未授權存取相簿中的照片或影片"
            button.text = "授權"
            cardLayout.visibility = View.VISIBLE
            return false
        }
        return true
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadImages() {
        thread {
            imageList.clear()

            if (checkPermissions()) {
                val cursor = contentResolver.query(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    null,
                    null,
                    null,
                    "${MediaStore.Images.Media.DATE_ADDED} desc"
                )

                if (cursor != null) {
                    while (cursor.moveToNext()) {
                        val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID))
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        val image = Image(uri)
                        imageList.add(image)
                    }
                    cursor.close()
                }

                runOnUiThread {
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
}