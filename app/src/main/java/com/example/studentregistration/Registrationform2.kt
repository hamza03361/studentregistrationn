package com.example.studentregistration

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.view.animation.AnimationUtils
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.setMargins
import androidx.core.widget.NestedScrollView
import com.example.studentregistration.Apis.RetrofitClient
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.airbnb.lottie.LottieAnimationView
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class Registrationform2 : ComponentActivity() {

    private lateinit var cancelbutton: Button
    private lateinit var registerbutton: Button
    private lateinit var contactnumber: EditText
    private lateinit var batch: Spinner
    private lateinit var address: EditText
    private lateinit var semester: EditText
    private lateinit var department: EditText
    private lateinit var enrollmentyear: EditText
    private lateinit var nestedScrollView: NestedScrollView

    private val gson = Gson()

    private lateinit var smallImageViewContainer: FrameLayout
    private lateinit var animationView: LottieAnimationView
    private var capturedImagePaths: ArrayList<String>? = null

    private val uploadRes = RetrofitClient.uploadResponse
    private var hasCountryCode: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registrationform2)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = getColor(R.color.secondthemecolor)
        }

        //for animition
        animationView = findViewById(R.id.animationView)


        // Retrieve captured images from the Intent
        capturedImagePaths = intent.getStringArrayListExtra("capturedImagePaths") ?: arrayListOf()

        smallImageViewContainer = findViewById(R.id.smallImageViewContainer)
        smallImageViewContainer.setOnClickListener {
            val intent = Intent(this, Registrationform1::class.java)
            startActivity(intent)
        }
        batch = findViewById(R.id.batchSpinner)

        // Setup Spinner
        val genderOptions = arrayOf("Select Batch", "Fall", "Spring")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        batch.adapter = adapter

        registerbutton = findViewById(R.id.nextbutton)
        registerbutton.setOnClickListener {
            uploadData()
        }

        cancelbutton = findViewById(R.id.cancelbutton)
        cancelbutton.setOnClickListener {
            val intent = Intent(this, Registrationform1::class.java)
            startActivity(intent)
        }

        contactnumber = findViewById(R.id.contactnumberedittext)
        address = findViewById(R.id.addressEditText)
        semester = findViewById(R.id.semesteredittext)
        department = findViewById(R.id.departmentedittext)
        enrollmentyear = findViewById(R.id.enrollmentyearedittext)

        contactnumber.setOnClickListener {
            if (!hasCountryCode) {
                showCountryCodeDialog()
            }
        }

        contactnumber.gravity = Gravity.START or Gravity.CENTER_VERTICAL
        contactnumber.filters = arrayOf(InputFilter.LengthFilter(16))

        contactnumber.addTextChangedListener(object : TextWatcher {
            private var isUpdating: Boolean = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return

                val text = contactnumber.text.toString()
                val parts = text.split(" ")
                val countryCode = if (parts.isNotEmpty()) parts[0] else ""
                val numberLength = text.length - countryCode.length

                // Limit the total length of the phone number
                if (numberLength > 11) {
                    isUpdating = true
                    contactnumber.setText(text.substring(0, countryCode.length + 11))
                    contactnumber.setSelection(contactnumber.text.length)
                    isUpdating = false
                }
            }

            override fun afterTextChanged(s: Editable?) {
                // Validate if country code is included
                if (!hasCountryCode && !contactnumber.text.toString().startsWith("+")) {
                    contactnumber.error = "Please enter a valid phone number with country code."
                }
            }
        })

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        nestedScrollView=findViewById(R.id.nestedscrollview)
        nestedScrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            nestedScrollView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = nestedScrollView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is open, scroll to the Next button
                nestedScrollView.scrollTo(0, registerbutton.bottom)
            } else {
                // Keyboard is closed, handle any other actions if necessary
            }
        }
    }

    private fun showCountryCodeDialog() {
        val countryCodes = resources.getStringArray(R.array.country_codes)
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Select Country Code")
        builder.setItems(countryCodes) { _, which ->
            val selectedCountryCode = countryCodes[which].split(" ")[0]
            contactnumber.setText("$selectedCountryCode ")
            contactnumber.setSelection(contactnumber.text.length)
            hasCountryCode = true
        }
        builder.show()
    }

    fun disableInputs() {
        contactnumber.apply {
            isEnabled = false
        }
        batch.apply {
            isEnabled = false
        }
        address.apply {
            isEnabled = false
        }
        semester.apply {
            isEnabled = false
        }
        department.apply {
            isEnabled = false
        }
        enrollmentyear.apply {
            isEnabled = false
        }
    }

    fun enableInputs() {
        contactnumber.apply {
            isEnabled = true
        }
        batch.apply {
            isEnabled = true
        }
        address.apply {
            isEnabled = true
        }
        semester.apply {
            isEnabled = true
        }
        department.apply {
            isEnabled = true
        }
        enrollmentyear.apply {
            isEnabled = true
        }
    }

    private fun uploadData() {

        //start loading animition
        animationView.visibility = View.VISIBLE
        disableInputs()



        // Retrieve data from Intent and EditText without trimming quotes
        val firstName = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("firstName") ?: "")
        val lastName = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("lastName") ?: "")
        val fatherName = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("fatherName") ?: "")
        val email = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("email") ?: "")
        val gender = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("gender") ?: "")
        val dateOfBirth = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("dateofbirth") ?: "")
        val registrationNo = RequestBody.create("text/plain".toMediaTypeOrNull(), intent.getStringExtra("registrationnumber") ?: "")
        val contactNo = RequestBody.create("text/plain".toMediaTypeOrNull(), contactnumber.text.toString())
        val semester = RequestBody.create("text/plain".toMediaTypeOrNull(), semester.text.toString())
        val batch = RequestBody.create("text/plain".toMediaTypeOrNull(), batch.selectedItem.toString())
        val address = RequestBody.create("text/plain".toMediaTypeOrNull(), address.text.toString())
        val department = RequestBody.create("text/plain".toMediaTypeOrNull(), department.text.toString())
        val enrollmentyear = RequestBody.create("text/plain".toMediaTypeOrNull(), enrollmentyear.text.toString())

        if (capturedImagePaths.isNullOrEmpty()) {
            Log.e(TAG, "No images to upload.")
            return
        }

        printDataAsJson() // Print data as JSON before uploading

        val capturedImages = convertPathsToMultipartBodyParts(capturedImagePaths!!)

        if (isInternetAvailable()) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val response = uploadRes.uploadData(
                        firstName,
                        lastName,
                        fatherName,
                        email,
                        gender,
                        dateOfBirth,
                        registrationNo,
                        contactNo,
                        semester,
                        batch,
                        address,
                        department,
                        enrollmentyear,
                        capturedImages
                    )
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            val responseBody = response.body()?.string()
                            Log.d(TAG, "Data upload response: $responseBody")

                            // Inflate the custom layout
                            val inflater = LayoutInflater.from(this@Registrationform2)
                            val layout: View = inflater.inflate(R.layout.custom_toast, null)

                            // Find the TextView and set the message
                            val textView = layout.findViewById<TextView>(R.id.toast_message)
                            textView.text = "Student created successfully"

                            // Create and show the Toast
                            val toast = Toast(applicationContext)
                            toast.duration = Toast.LENGTH_SHORT
                            toast.view = layout
                            toast.show()
                        } else {
                            val errorBody = response.errorBody()?.string()
                            Log.e(TAG, "Server returned error: $errorBody")

                            // Inflate the custom layout
                            val inflater = LayoutInflater.from(this@Registrationform2)
                            val layout: View = inflater.inflate(R.layout.custom_toast, null)

                            // Find the TextView and set the message
                            val textView = layout.findViewById<TextView>(R.id.toast_message)
                            textView.text = "Failed to create student"

                            // Create and show the Toast
                            val toast = Toast(applicationContext)
                            toast.duration = Toast.LENGTH_SHORT
                            toast.view = layout
                            toast.show()
                        }
                        animationView.visibility = View.GONE
                        enableInputs()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e(TAG, "Data upload failed: ${e.message}", e)

                        // Inflate the custom layout
                        val inflater = LayoutInflater.from(this@Registrationform2)
                        val layout: View = inflater.inflate(R.layout.custom_toast, null)

                        // Find the TextView and set the message
                        val textView = layout.findViewById<TextView>(R.id.toast_message)
                        textView.text = "Data upload failed"

                        // Create and show the Toast
                        val toast = Toast(applicationContext)
                        toast.duration = Toast.LENGTH_SHORT
                        toast.view = layout
                        toast.show()

                        animationView.visibility = View.GONE
                        enableInputs()
                    }
                }
            }
        } else {
            Log.e(TAG, "No internet connection")
            animationView.visibility = View.GONE
            enableInputs()
        }
    }

    private fun printDataAsJson() {
        // Create an instance of RegistrationData
        val firstName = intent.getStringExtra("firstName") ?: ""
        val lastName = intent.getStringExtra("lastName") ?: ""
        val fatherName = intent.getStringExtra("fatherName") ?: ""
        val email = intent.getStringExtra("email") ?: ""
        val gender = intent.getStringExtra("gender") ?: ""
        val dateOfBirth = intent.getStringExtra("dateofbirth") ?: ""
        val registrationNo = intent.getStringExtra("registrationnumber") ?: ""
        val contactNo = contactnumber.text.toString()
        val semester = semester.text.toString()
        val batch = batch.selectedItem.toString()
        val address = address.text.toString()
        val department = department.text.toString()
        val enrollmentyear = enrollmentyear.text.toString()
        val capturedImagePaths = capturedImagePaths ?: arrayListOf()

        // Create an instance of RegistrationData
        val registrationData = RegistrationData(
            firstname = firstName,
            lastname = lastName,
            fathername = fatherName,
            email = email,
            gender = gender,
            dateOfBirth = dateOfBirth,
            registrationNo = registrationNo,
            contactNo = contactNo,
            semester = semester,
            batch = batch,
            address = address,
            department = department,
            enrollmentyear = enrollmentyear,
            capturedImagePaths = capturedImagePaths
        )

        // Convert the data to JSON
        val json = gson.toJson(registrationData)

        // Print the formatted JSON string
        Log.d(TAG, "Registration Data in JSON format: $json")
    }

    private fun convertPathsToMultipartBodyParts(paths: List<String>): List<MultipartBody.Part> {
        return paths.mapNotNull { path ->
            val file = File(path)
            if (file.exists()) {
                val fileName = file.name
                val sanitizedFileName = sanitizeFileName(fileName)
                Log.d(TAG, "Sanitized file name: $sanitizedFileName")
                val mediaType = "image/jpeg".toMediaTypeOrNull()
                val requestFile = file.asRequestBody(mediaType)
                MultipartBody.Part.createFormData("images", sanitizedFileName, requestFile)
            } else {
                Log.e(TAG, "File not found: $path")
                null
            }
        }
    }


    // Function to sanitize filenames
    private fun sanitizeFileName(fileName: String): String {
        return fileName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
    }

    private fun isInternetAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    companion object {
        private const val TAG = "RegistrationForm2"
    }

    data class RegistrationData(
        val firstname: String,
        val lastname: String,
        val fathername: String,
        val email: String,
        val gender: String,
        val dateOfBirth: String,
        val registrationNo: String,
        val contactNo: String,
        val semester: String,
        val batch: String,
        val address: String,
        val department: String,
        val enrollmentyear: String,

        val capturedImagePaths: ArrayList<String>
    )
}
