package com.example.upworksheetsproject

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.PersistableBundle
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.example.upworksheetsproject.data.Feedback
import com.example.upworksheetsproject.data.SheetsApi
import com.example.upworksheetsproject.databinding.ActivityMainBinding
import com.example.upworksheetsproject.receivers.SmsDeliveredReceiver
import com.example.upworksheetsproject.receivers.SmsSentReceiver
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var otpGenerated: String
    private lateinit var phoneNumberG: String
    private lateinit var mainViewModel: MainViewModel
    private lateinit var feedBack: Feedback
    private lateinit var map: Map<String, List<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.e("LifeTest", "onCreate")
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        mainViewModel = ViewModelProvider(this)[MainViewModel::class.java]
        checkPermissionSms()

        mainViewModel.getCompanyData()

        mainViewModel.companyData.observe(this, Observer {
            if (it == null) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(
                    this@MainActivity,
                    "Error loading drop down items",
                    Toast.LENGTH_SHORT
                ).show()
            }
            it?.let {
                binding.progressBar.visibility = View.GONE
                map = it
                setCompanyAndActivity()
            }
        })

        binding.spinnerCompany.onItemSelectedListener = object :
            AdapterView.OnItemSelectedListener {

            override fun onNothingSelected(p0: AdapterView<*>?) {

            }

            override fun onItemSelected(
                parentView: AdapterView<*>?,
                selectedItemView: View?,
                position: Int,
                id: Long
            ) {
                val selectedCompany = binding.spinnerCompany.selectedItem
                val activity = map[selectedCompany]!!
                val spinnerActivityAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
                    this@MainActivity, android.R.layout.simple_spinner_item, activity
                )
                binding.spinnerActivity.adapter = spinnerActivityAdapter
            }
        }

        binding.btnGetOtp.setOnClickListener {
            val phone = binding.etPhone.text.toString()
            if (phone.length == 10) {
                phoneNumberG = phone
                binding.progressBar.visibility = View.VISIBLE
                getOtp(phone)
                binding.btnVerify.visibility = View.VISIBLE
                binding.etOtp.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            } else {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVerify.setOnClickListener {
            val otp = binding.etOtp.text.toString()
            if (otp.length == 4 && otp == otpGenerated) {
                mainViewModel.setIsVerified(true)
                binding.btnSubmit.isEnabled = true
                binding.btnVerify.visibility = View.GONE
                binding.etOtp.visibility = View.GONE
                binding.verifiedSign.visibility = View.VISIBLE
            } else {
                Toast.makeText(this, "INVALID OTP", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnSubmit.setOnClickListener {
            binding.apply {
                val name = etName.text.toString()
                val company = spinnerCompany.selectedItem.toString()
                val activity = spinnerActivity.selectedItem.toString()
                val phone = etPhone.text.toString()
                val comment = etComment.text.toString()
                val stars = ratingStar.rating
                feedBack = Feedback(
                    name,
                    phone,
                    company,
                    activity,
                    stars.toString(),
                    comment
                )
                if (name.isNotBlank() && stars != 0f) {
                    sendToSheets(feedBack)
                } else {
                    Toast.makeText(this@MainActivity, "Fill details properly", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun getOtp(phoneNumber: String) {
        otpGenerated = "${(0..9).random()}${(0..9).random()}${(0..9).random()}${(0..9).random()}"
        val message = "$otpGenerated is your OTP for submitting the feedback"
        sendSMS(phoneNumber, message)
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        val sentPendingIntents = ArrayList<PendingIntent>()
        val deliveredPendingIntents = ArrayList<PendingIntent>()
        val sentPI = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, SmsSentReceiver::class.java), 0
        )
        val deliveredPI = PendingIntent.getBroadcast(
            this, 0,
            Intent(this, SmsDeliveredReceiver::class.java), 0
        )
        try {
            val sms: SmsManager = SmsManager.getDefault()
            val mSMSMessage = sms.divideMessage(message)
            for (i in 0 until mSMSMessage.size) {
                sentPendingIntents.add(i, sentPI)
                deliveredPendingIntents.add(i, deliveredPI)
            }
            sms.sendMultipartTextMessage(
                phoneNumber, null, mSMSMessage,
                sentPendingIntents, deliveredPendingIntents
            )
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
            Log.e("RishuTest", e.message.toString())
            Toast.makeText(this, "SMS sending failed...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionSms() {
        val requestPermissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show()
                }
            }
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.SEND_SMS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Already granted
        } else {
            requestPermissionLauncher.launch(
                Manifest.permission.SEND_SMS
            )
        }
    }

    private fun sendToSheets(feedback: Feedback) = CoroutineScope(Dispatchers.IO).launch {
        try {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.VISIBLE
            }
            val res = SheetsApi.instance.exportData(
                feedback.name,
                feedback.mobile,
                feedback.company,
                feedback.activity,
                feedback.rating,
                feedback.comment
            )
            withContext(Dispatchers.Main) {
                Toast.makeText(this@MainActivity, "Submitted", Toast.LENGTH_SHORT).show()
                binding.progressBar.visibility = View.INVISIBLE
                //setting default data
                binding.verifiedSign.visibility = View.GONE
                binding.etName.setText("")
                binding.etComment.setText("")
                binding.etOtp.setText("")
                binding.etPhone.setText("")
                binding.ratingStar.rating = 0f
                showDialogToGetResponse()
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                binding.progressBar.visibility = View.INVISIBLE
                Log.e("RishuTest", e.message.toString())
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDialogToGetResponse() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Response")
        builder.setMessage("Do you want to get submitted data as sms ?")

        builder.setPositiveButton("Yes") { dialog, which ->
            val data =
                "Name: ${feedBack.name}\nCompany: ${feedBack.company}Activity: ${feedBack.activity}\nRating: ${feedBack.rating}\nPhone: ${feedBack.mobile}\nComment: ${feedBack.comment}"
            sendSMS(phoneNumberG, data)
        }

        builder.setNegativeButton("No") { dialog, which ->

        }
        builder.show()
    }

    private fun setCompanyAndActivity() {
        val companySelected = map.keys.toList()
        val activities = map[companySelected[0]]!!
        val spinnerCompanyAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, companySelected
        )
        binding.spinnerCompany.adapter = spinnerCompanyAdapter
        val spinnerActivityAdapter: ArrayAdapter<String> = ArrayAdapter<String>(
            this, android.R.layout.simple_spinner_item, activities
        )
        binding.spinnerActivity.adapter = spinnerActivityAdapter
    }

    override fun onStart() {
        super.onStart()
        Log.e("LifeTest", "onStart")
    }

    override fun onStop() {
        super.onStop()
        Log.e("LifeTest", "onStop")
    }

    override fun onPause() {
        super.onPause()
        Log.e("LifeTest", "onPause")
    }

    override fun onRestart() {
        super.onRestart()
        Log.e("LifeTest", "onRestart")
    }

    override fun onResume() {
        super.onResume()
        Log.e("LifeTest", "onResume")
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.e("LifeTest", "onDestroy")
    }

    override fun onSaveInstanceState(outState: Bundle, outPersistentState: PersistableBundle) {
        super.onSaveInstanceState(outState, outPersistentState)
        Log.e("LifeTest", "onSaveInstance")
    }


}