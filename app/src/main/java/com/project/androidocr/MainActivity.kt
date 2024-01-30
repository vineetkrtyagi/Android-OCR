package com.project.androidocr

import android.Manifest
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.text.TextRecognizer

class MainActivity : AppCompatActivity() {
    private var mResultEt: EditText? = null
    private var mPreviewIv: ImageView? = null
    private lateinit var cameraPermission: Array<String>
    private lateinit var storagePermission: Array<String>
    private var image_uri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set action bar subtitle
        supportActionBar?.subtitle = "Click + button to insert image"

        mResultEt = findViewById(R.id.resultEt)
        mPreviewIv = findViewById(R.id.imageIv)

        // Camera permission
        cameraPermission = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )

        // Storage permission
        storagePermission = arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.addImage -> showImageImportDialog()
            R.id.about -> dialogAbout()
        }
        return true
    }

    private fun dialogAbout() {
        AlertDialog.Builder(this)
            .setTitle("About App")
            .setMessage("This app is made by Achmad Qomarudin.")
            .setPositiveButton("CLOSE") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    private fun showImageImportDialog() {
        val items = arrayOf("Camera", "Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Image")
            .setItems(items) { _, which ->
                if (which == 0) {
                    if (!checkCameraPermission()) {
                        // Camera permission not allowed, request it
                        requestCameraPermission()
                    } else {
                        // Permission allowed, take picture
                        pickCamera()
                    }
                }
                if (which == 1) {
                    if (!checkStoragePermission()) {
                        // Storage permission not allowed, request it
                        requestStoragePermission()
                    } else {
                        // Permission allowed, take picture
                        pickGallery()
                    }
                }
            }
            .create()
            .show()
    }

    private fun pickGallery() {
        // Intent to pick image from gallery
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, IMAGE_PICK_GALLERY_CODE)
    }

    private fun pickCamera() {
        // Intent to take image from camera, it will also be saved to storage to get a high-quality image
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "NewPick") // Title of the picture
        values.put(MediaStore.Images.Media.DESCRIPTION, "Image To Text") // Description of the picture
        image_uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri)
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE)
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermission, STORAGE_REQUEST_CODE)
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, cameraPermission, CAMERA_REQUEST_CODE)
    }

    private fun checkCameraPermission(): Boolean {
        val result = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        val result1 = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        return result && result1
    }

    // Handle permission result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CAMERA_REQUEST_CODE -> if (grantResults.isNotEmpty()) {
                val cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED
                val writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED
                if (cameraAccepted && writeStorageAccepted) {
                    pickCamera()
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
            }

            STORAGE_REQUEST_CODE -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickGallery()
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Handle image result
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            if (requestCode == IMAGE_PICK_GALLERY_CODE || requestCode == IMAGE_PICK_CAMERA_CODE) {
                // Image picked from gallery or camera, process it directly
                val resultUri: Uri? = if (requestCode == IMAGE_PICK_GALLERY_CODE) {
                    data?.data
                } else {
                    image_uri
                }

                resultUri?.let {
                    // Set image to image view
                    mPreviewIv?.setImageURI(it)

                    // Get drawable bitmap for text recognition
                    val bitmapDrawable = mPreviewIv?.drawable as BitmapDrawable
                    val bitmap = bitmapDrawable.bitmap
                    val recognizer = TextRecognizer.Builder(applicationContext).build()

                    if (!recognizer.isOperational) {
                        Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show()
                    } else {
                        val frame = Frame.Builder().setBitmap(bitmap).build()
                        val items = recognizer.detect(frame)
                        val sb = StringBuilder()

                        // Get text from sb until there is no text
                        for (i in 0 until items.size()) {
                            val myItem = items.valueAt(i)
                            sb.append(myItem.value)
                            sb.append("\n")
                        }

                        // Set text to edit text
                        mResultEt?.setText(sb.toString())
                    }
                }
            }
        }
    }

    companion object {
        // Permission Code
        private const val CAMERA_REQUEST_CODE = 200
        private const val STORAGE_REQUEST_CODE = 400
        private const val IMAGE_PICK_GALLERY_CODE = 1000
        private const val IMAGE_PICK_CAMERA_CODE = 2001
    }
}
