package com.example.studentregistration

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.Window
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Spinner
import com.example.studentregistration.FloatArrayListParcelable
import androidx.activity.ComponentActivity
import java.util.Calendar
import android.util.Log
import android.widget.*
import androidx.core.util.PatternsCompat
import java.util.*
import android.widget.*
import androidx.core.widget.NestedScrollView
import com.bumptech.glide.Glide
import java.io.File
import java.util.*

class Registrationform1 : ComponentActivity() {

    private lateinit var genderSpinner: Spinner
    private lateinit var dateofbirthedittext: EditText
    private lateinit var registrationnumberEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var nextButton: Button
    private lateinit var smallImageViewContainer: FrameLayout
    private lateinit var nestedScrollView: NestedScrollView

    private var capturedImagePaths: ArrayList<String>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.registrationform1)

        // Set up the status bar color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window: Window = window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.statusBarColor = getColor(R.color.secondthemecolor)
        }

        // Initialize capturedImagePaths
        capturedImagePaths = intent.getStringArrayListExtra("capturedImagePaths") ?: arrayListOf()
        // Log the received image paths
        Log.d("Registrationform1", "Received image paths: $capturedImagePaths")

        // Initialize UI elements
        smallImageViewContainer = findViewById(R.id.smallImageViewContainer)
        genderSpinner = findViewById(R.id.genderSpinner)
        dateofbirthedittext = findViewById(R.id.dateofbirthedittext)
        registrationnumberEditText = findViewById(R.id.registrationnumberedittext)
        emailEditText = findViewById(R.id.emailEditText)
        nextButton = findViewById(R.id.nextButton)

        smallImageViewContainer.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Setup Spinner
        val genderOptions = arrayOf("Select Gender", "Male", "Female", "Other")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genderOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genderSpinner.adapter = adapter

        // Setup Date Picker
        dateofbirthedittext.setOnClickListener {
            showDatePicker()
        }


        // Handle next button click
        nextButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val registrationNumber = registrationnumberEditText.text.toString()

            if (!isValidEmail(email)) {
                emailEditText.error = "Invalid email address"
            }  else {
                val intent = Intent(this, Registrationform2::class.java)
                intent.putExtra("firstName", findViewById<EditText>(R.id.firstnameedittext).text.toString())
                intent.putExtra("lastName", findViewById<EditText>(R.id.lastnameedittext).text.toString())
                intent.putExtra("fatherName", findViewById<EditText>(R.id.fathernameedittext).text.toString())
                intent.putExtra("email", findViewById<EditText>(R.id.emailEditText).text.toString())
                intent.putExtra("gender", genderSpinner.selectedItem.toString())
                intent.putExtra("dateofbirth", dateofbirthedittext.text.toString())
                intent.putExtra("registrationnumber", findViewById<EditText>(R.id.registrationnumberedittext).text.toString())

                // Pass the captured image paths to the next activity
                intent.putStringArrayListExtra("capturedImagePaths", capturedImagePaths)

                // Log before starting the next activity
                Log.d("Registrationform1", "Passing image paths to Registrationform2: $capturedImagePaths")

                startActivity(intent)
            }
        }
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        nestedScrollView=findViewById(R.id.nestedscrollview)
        nestedScrollView.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            nestedScrollView.getWindowVisibleDisplayFrame(rect)
            val screenHeight = nestedScrollView.height
            val keypadHeight = screenHeight - rect.bottom

            if (keypadHeight > screenHeight * 0.15) {
                // Keyboard is open, scroll to the Next button
                nestedScrollView.scrollTo(0, nextButton.bottom)
            } else {
                // Keyboard is closed, handle any other actions if necessary
            }
        }
    }

    private fun isValidEmail(email: CharSequence): Boolean {
        return PatternsCompat.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val selectedDate = "${selectedDay}/${selectedMonth + 1}/${selectedYear}"
            dateofbirthedittext.setText(selectedDate)
        }, year, month, day)
        datePickerDialog.show()
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
}
