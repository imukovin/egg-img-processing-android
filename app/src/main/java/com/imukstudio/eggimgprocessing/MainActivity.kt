package com.imukstudio.eggimgprocessing

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.imukstudio.eggimgprocessing.domain.ImageProcessing
import java.io.File
import java.io.IOException
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

class MainActivity : AppCompatActivity() {
    private lateinit var registerSelect: ActivityResultLauncher<Intent>
    private lateinit var registerCamera: ActivityResultLauncher<Intent>
    private val imageProcessing = ImageProcessing()
    private var currentPhotoPath = ""

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
            getImageInRealSizeFromCamera()
        }

        imageProcessing.setEggParamListener { eggParams ->
            findViewById<TextView>(R.id.eggWidthText).text = getString(R.string.weight_height_size, eggParams.width)
            findViewById<TextView>(R.id.eggHeightText).text = getString(R.string.weight_height_size, eggParams.height)
            findViewById<TextView>(R.id.eggVolumeText).text = getString(R.string.volume_size, eggParams.volume)
            findViewById<TextView>(R.id.eggSquareText).text = getString(R.string.square_size, eggParams.square)
            findViewById<TextView>(R.id.eggMassText).text = getString(R.string.mass_size, eggParams.mass)
            findViewById<TextView>(R.id.eggAreaRatioToVolumeText).text = eggParams.rationAreaToVolume.toString()
            findViewById<TextView>(R.id.eggShellMassText).text = getString(R.string.mass_size, eggParams.shellMass)
            findViewById<TextView>(R.id.eggYolkMassText).text = getString(R.string.mass_size, eggParams.yolkMass)
            findViewById<TextView>(R.id.eggProteinMassText).text = getString(R.string.mass_size, eggParams.proteinMass)
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

    private fun getImageInRealSizeFromCamera() {
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        try {
            val imageFile = File.createTempFile(
                FILE_NAME_FROM_CAMERA,
                FILE_EXTENSION_FROM_CAMERA,
                storageDir
            )
            currentPhotoPath = imageFile.absolutePath

            val imageUri = FileProvider.getUriForFile(
                this,
                "com.imukstudio.eggimgprocessing.fileprovider",
                imageFile
            )
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            registerCamera.launch(intent)
        } catch (e: IOException) {
            // TODO:
        }
    }

    private fun getPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
        }
    }

    private fun onActivityResult(resultCode: Int, result: ActivityResult) {
        if (result.resultCode == Activity.RESULT_OK) {
            when (resultCode) {
                SELECTED_REQUEST_CODE -> {
                    result.data?.let {
                        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, it.data)
                        Log.d(App.APP_LOG_TAG, "Input image size: w = ${bitmap.width}px h = ${bitmap.height} px")
                        lifecycleScope.launch {
                            val resultBitmap = imageProcessing.processImage(bitmap)
                            findViewById<ImageView>(R.id.imageView).setImageBitmap(resultBitmap)
                        }
                    }
                }
                CAMERA_REQUEST_CODE -> {
                    val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
                    Log.d(App.APP_LOG_TAG, "Input image size: w = ${bitmap.width}px h = ${bitmap.height} px")
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

        private const val FILE_NAME_FROM_CAMERA = "photoFromCamera"
        private const val FILE_EXTENSION_FROM_CAMERA = ".jpg"
    }
}
