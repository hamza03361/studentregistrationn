package com.example.studentregistration

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import android.widget.*
import java.io.File
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import java.io.FileOutputStream
import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.util.concurrent.Executors
import java.util.concurrent.ExecutorService




class Faceregistration1 : ComponentActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var firstpreview: PreviewView
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var smallImageViewContainer: FrameLayout
    private lateinit var progressBar: ProgressBar
    private val capturedImagePaths = mutableListOf<String>()
    private var imageCapture: ImageCapture? = null
    private var countdownTimer: CountDownTimer? = null
    private lateinit var countdownTextView: TextView
    private var isDialogShowing = false
    private var image = false
    private var capture = false
    private var i = 0
    private var k = 0
    private var r = 0
    private var x = 0
    private var y = 0
    private var u = 0
    private var j = 0
    var currentInstructionIndex = 0

    private val faceDetector by lazy {
        FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.faceregistration1)

        previewView = findViewById(R.id.previewView)
        firstpreview = findViewById(R.id.firstpreview)
        progressBar = findViewById(R.id.progressBar)
        countdownTextView = findViewById(R.id.countdownTextView)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = getColor(R.color.secondthemecolor) // Change to your color resource
        }

        smallImageViewContainer = findViewById(R.id.smallImageViewContainer)
        smallImageViewContainer.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        cameraExecutor = Executors.newFixedThreadPool(2)

        startCamera()

    }

    override fun onBackPressed() {
        super.onBackPressed()
        navigatetomainactivity()
    }

    private fun navigatetomainactivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Unbind all use cases before rebinding
            cameraProvider.unbindAll()

            // Create the Preview use case
            val preview = Preview.Builder()
                .setTargetResolution(Size(900, 900))
                .build()

            // Set the first preview view
            val firstPreviewView = findViewById<PreviewView>(R.id.firstpreview)
            preview.setSurfaceProvider(firstPreviewView.surfaceProvider)

            // Get the textureView for the second preview
            val textureView = findViewById<TextureView>(R.id.previewView)

            // Set up the TextureView's SurfaceTextureListener
            textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                override fun onSurfaceTextureAvailable(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {
                    // Create a Surface for the TextureView
                    val secondSurface = Surface(surfaceTexture)
                    // Set up a repeating task to draw the preview onto the TextureView
                    val handler = Handler(Looper.getMainLooper())
                    val drawRunnable = object : Runnable {
                        override fun run() {
                            // Copy the content of firstPreviewView to the TextureView
                            textureView.lockCanvas()?.let { canvas ->
                                val bitmap = firstPreviewView.bitmap
                                bitmap?.let {
                                    canvas.drawBitmap(it, 0f, 0f, null)
                                }
                                textureView.unlockCanvasAndPost(canvas)
                            }
                            handler.postDelayed(this, 16) // Run every ~16ms (60fps)
                        }
                    }
                    handler.post(drawRunnable)
                }

                override fun onSurfaceTextureSizeChanged(
                    surfaceTexture: SurfaceTexture,
                    width: Int,
                    height: Int
                ) {}

                override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture): Boolean {
                    return true
                }

                override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {}
            }

            // Create the image capture use case
            imageCapture = ImageCapture.Builder()
                .setTargetResolution(Size(250, 250))
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .setJpegQuality(100)
                .build()

            // Create the image analysis use case
            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(640, 480))
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, FaceAnalyzer())
                }

            // Select the front-facing camera
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build()

            // Bind the lifecycle to the camera with the preview, image capture, and image analysis use cases
            cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalysis
            )
        }, ContextCompat.getMainExecutor(this))
    }




    private fun capturePhoto(filename: String) {
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        imageCapture?.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    capturedImagePaths.add(file.absolutePath)

                    // Resize the captured image to 250x250 if necessary
                    resizeImageTo250x250(file.absolutePath) { resizedFile ->
                        // Correct image orientation and show dialog with the resized image
                        correctImageOrientation(resizedFile.absolutePath) { correctedBitmap ->
                            showImagePreviewDialog(correctedBitmap, resizedFile.absolutePath, object : DialogDismissListener {
                                override fun onDialogDismissed() {
                                    isDialogShowing = false
                                    // Restart the countdown timer
                                    startCountdownTimer()
                                }
                            })
                        }
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }

    private fun resizeImageTo250x250(imagePath: String, callback: (File) -> Unit) {
        val originalBitmap = BitmapFactory.decodeFile(imagePath)

        // Get the original image's EXIF orientation
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

        // Resize the bitmap
        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, 250, 250, true)

        // Rotate the bitmap to correct orientation
        val rotatedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(resizedBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(resizedBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(resizedBitmap, 270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> flipBitmap(resizedBitmap, horizontal = true, vertical = false)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> flipBitmap(resizedBitmap, horizontal = false, vertical = true)
            else -> resizedBitmap
        }

        // Save the corrected bitmap to the file
        val resizedFile = File(imagePath)
        FileOutputStream(resizedFile).use { outputStream ->
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        }

        callback(resizedFile)
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun flipBitmap(bitmap: Bitmap, horizontal: Boolean, vertical: Boolean): Bitmap {
        val matrix = Matrix().apply {
            if (horizontal) postScale(-1f, 1f)
            if (vertical) postScale(1f, -1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun correctImageOrientation(imagePath: String, callback: (Bitmap) -> Unit) {
        val bitmapOptions = BitmapFactory.Options().apply {
            inSampleSize = 1  // Adjust this based on your image quality and memory considerations
        }
        val bitmap = BitmapFactory.decodeFile(imagePath, bitmapOptions)
        val exif = ExifInterface(imagePath)
        val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()

        // Apply rotation based on EXIF data
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1.0f, 1.0f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1.0f, -1.0f)
        }

        // Create a new bitmap with the correct orientation
        val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

        // Apply horizontal flip if needed (for front-facing camera images)
        val correctedBitmap = if (isFrontFacingCamera()) {
            val flipMatrix = Matrix().apply { preScale(-1.0f, 1.0f) }
            Bitmap.createBitmap(rotatedBitmap, 0, 0, rotatedBitmap.width, rotatedBitmap.height, flipMatrix, true)
        } else {
            rotatedBitmap
        }

        callback(correctedBitmap)  // Pass the corrected bitmap to the callback
    }

    private fun isFrontFacingCamera(): Boolean {
        // Implement your logic here to check if the camera is front-facing
        // This can be based on the CameraSelector or any other method you use to determine the camera type
        return true  // Assuming it's front-facing for this example
    }

    interface DialogDismissListener {
        fun onDialogDismissed()
    }

    private fun showImagePreviewDialog(bitmap: Bitmap, filename: String, listener: DialogDismissListener) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_image_preview, null)
        val imageView: ImageView = dialogView.findViewById(R.id.imageView)
        val buttonSave: Button = dialogView.findViewById(R.id.buttonSave)
        val buttonDelete: Button = dialogView.findViewById(R.id.buttonDelete)

        imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        imageView.setImageBitmap(bitmap)  // Set bitmap here

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.setOnDismissListener {
            listener.onDialogDismissed()
        }

        dialog.show()
        isDialogShowing = true

        buttonSave.setOnClickListener {
            saveBitmapToGallery(bitmap, filename)
            updateProgressBar()
            dialog.dismiss()
            r=0
            startCamera()
        }

        buttonDelete.setOnClickListener {
            deleteImage(filename)
            dialog.dismiss()
            r=0
            startCamera()
        }
    }

    private fun updateProgressBar() {
        val progress = (capturedImagePaths.size * 20) // Assuming each image is 20% of progress
        progressBar.progress = progress
    }

    private fun deleteImage(filename: String) {
        capturedImagePaths.remove(filename)
        val file = File(filename)
        if (file.exists()) {
            file.delete()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap, filename: String) {

        image=true
        if (x==1){
            y++
        }
        if (y==1){
            u++
        }
        if (u==1){
            j++
        }
        x++

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Faceregistration")
            }
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
        uri?.let {
            contentResolver.openOutputStream(it).use { outputStream ->
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
            }
            navigateToRegistrationForm()
        }
    }

    private fun navigateToRegistrationForm() {
        if (capturedImagePaths.size == 5) {
            val intent = Intent(this, Registrationform1::class.java).apply {
                putStringArrayListExtra("capturedImagePaths", ArrayList(capturedImagePaths))
            }
            startActivity(intent)
            finish()
        }
    }

    private fun startCountdownTimer() {

        if (r<1) {

            i++


            val instructions = listOf(
                "  ",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards",
                "Look straight into the camera",
                "Tilt your face to the left",
                "Tilt your face to the right",
                "Tilt your face upwards",
                "Tilt your face downwards"
            )


            val instructionCount = instructions.size

            fun showNextInstruction() {
                if (currentInstructionIndex >= instructionCount) {
                    countdownTextView.text = "Done"
                    return
                }

                countdownTextView.text =
                    instructions[currentInstructionIndex] // Display the current instruction

                countdownTimer = object :
                    CountDownTimer(3000, 1000) { // 3 seconds countdown for each instruction
                    override fun onTick(millisUntilFinished: Long) {
                        val secondsRemaining = millisUntilFinished / 1000
                        countdownTextView.text =
                            "${instructions[currentInstructionIndex]} ($secondsRemaining seconds)"
                    }

                    override fun onFinish() {
                        countdownTextView.text = "Capturing photo..."

                        // Capture the photo
                        if (!isDialogShowing) {
                            capturePhoto("captured_${System.currentTimeMillis()}.jpg")
                        }
                    }
                }.start()
            }
            if (i == 1 || image == true && x == 1 || image == true && y == 1 || image == true && u == 1 || image == true && j == 1) {

                currentInstructionIndex++

                if (x == 1) {
                    x = 0
                }
            }
            showNextInstruction()
        }
        r=1
    }

    inner class FaceAnalyzer : ImageAnalysis.Analyzer {
        @OptIn(ExperimentalGetImage::class)
        override fun analyze(image: ImageProxy) {
            val mediaImage = image.image ?: return
            val imageRotation = image.imageInfo.rotationDegrees
            val inputImage = com.google.mlkit.vision.common.InputImage.fromMediaImage(
                mediaImage,
                imageRotation
            )

            faceDetector.process(inputImage)
                .addOnSuccessListener { faces ->
                    if (faces.isNotEmpty()) {
                        for (face in faces) {
                            if (isFullFaceDetected(face)) {
                                runOnUiThread {
                                    startCountdownTimer()
                                }
                                break
                            }
                        }
                    } else {
                        Log.d(TAG, "No face detected")
                    }
                }

                .addOnFailureListener { e ->
                    Log.e(TAG, "Error detecting face", e)
                }
                .addOnCompleteListener {
                    image.close()
                }
                k=1

        }

        private fun isFullFaceDetected(face: Face): Boolean {
            // List of required landmarks

            val requiredLandmarks = listOf(
                FaceLandmark.LEFT_EYE,
                FaceLandmark.RIGHT_EYE,
                FaceLandmark.NOSE_BASE,
                FaceLandmark.MOUTH_LEFT,
                FaceLandmark.MOUTH_RIGHT
            )

            // Get detected landmarks
            val detectedLandmarks = face.allLandmarks.map { it.landmarkType }.toSet()

            // Check if all required landmarks are detected
            val allLandmarksDetected = requiredLandmarks.all { it in detectedLandmarks }

            // Check if the face is sufficiently large (covers a significant portion of the camera view)
            val boundingBox = face.boundingBox
            val minFaceSize = 200 // Adjust this value as needed to ensure that only full faces trigger the toast

            return allLandmarksDetected && boundingBox.width() >= minFaceSize && boundingBox.height() >= minFaceSize
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        countdownTimer?.cancel()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "Faceregistration1"
    }
}
