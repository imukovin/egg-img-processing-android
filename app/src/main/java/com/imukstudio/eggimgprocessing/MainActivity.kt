package com.imukstudio.eggimgprocessing

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.imukstudio.eggimgprocessing.domain.ImageProcessing
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
    private lateinit var registerSelect: ActivityResultLauncher<Intent>
    private lateinit var registerCamera: ActivityResultLauncher<Intent>
    private val imageProcessing = ImageProcessing()

    override fun onStart() {
        super.onStart()
        registerSelect = registerForActivityResult(StartActivityForResult()) {
            onActivityResult(SELECTED_REQUEST_CODE, it)
        }
        registerCamera = registerForActivityResult(StartActivityForResult()) {
            onActivityResult(CAMERA_REQUEST_CODE, it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        getPermission()

        findViewById<Button>(R.id.selectImageBtn).setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "image/*"
            registerSelect.launch(intent)
        }

        findViewById<Button>(R.id.openCameraBtn).setOnClickListener {
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra("REQUEST_CODE", CAMERA_REQUEST_CODE)
            registerCamera.launch(intent)
        }

        imageProcessing.setEggParamListener { eggParams ->
            findViewById<TextView>(R.id.eggWidthText).text = getString(R.string.weight_height_size, eggParams.width)
            findViewById<TextView>(R.id.eggHeightText).text = getString(R.string.weight_height_size, eggParams.height)
            findViewById<TextView>(R.id.eggVolumeText).text = getString(R.string.volume_size, eggParams.volume)
            findViewById<TextView>(R.id.eggSquareText).text = getString(R.string.square_size, eggParams.square)
            findViewById<TextView>(R.id.eggMassText).text = getString(R.string.mass_size, eggParams.mass)
        }

        if (OpenCVLoader.initDebug()) {
            Log.d(App.APP_LOG_TAG, "OpenCV was init")
        } else {
            Log.d(App.APP_LOG_TAG, "OpenCV Error")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        registerSelect.unregister()
        registerCamera.unregister()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && grantResults.isNotEmpty()) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                getPermission()
            }
        }
    }

    private fun getPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        }
    }

    private fun onActivityResult(resultCode: Int, result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK  && result.data != null) {
            when (resultCode) {
                SELECTED_REQUEST_CODE -> {
                    val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, result.data?.data)
                    lifecycleScope.launch {
                        val resultBitmap = imageProcessing.processImage(bitmap, resizeImgCoefficient = RESIZE_IMG_COEFFICIENT)
                        findViewById<ImageView>(R.id.imageView).setImageBitmap(resultBitmap)
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val bitmap = result.data?.extras?.get("data") as Bitmap
                    lifecycleScope.launch {
                        val resultBitmap = imageProcessing.processImage(bitmap)
                        findViewById<ImageView>(R.id.imageView).setImageBitmap(resultBitmap)
                    }
                }
            }
        }
    }

    private companion object {
        private const val SELECTED_REQUEST_CODE = 201
        private const val CAMERA_REQUEST_CODE = 202
        private const val PERMISSION_REQUEST_CODE = 202
        // If img was taken from a real time phone camera then need coefficient to resize it
        private const val RESIZE_IMG_COEFFICIENT = 10
    }
}
