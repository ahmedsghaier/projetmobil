package tn.ahm.projetmobile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.Manifest
import android.os.Bundle
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.location.*
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.*
import java.util.*

class PatientActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var etLocation: EditText
    private lateinit var locationCallback: LocationCallback
    private var locationRequest: LocationRequest? = null
    private var locationActive = false

    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient)

        // Initialisation des vues
        val editTextDateNaissance: EditText = findViewById(R.id.editTextDateNaissance)
        editTextDateNaissance.setOnClickListener {
            showDatePickerDialog(editTextDateNaissance)
        }

        val spinnerSpecialite: Spinner = findViewById(R.id.spinner_specialite)
        val adapter = ArrayAdapter.createFromResource(
            this, R.array.specialites_array, R.drawable.custom_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecialite.adapter = adapter

        val spinnerGenre: Spinner = findViewById(R.id.spinner_genre)
        val adapterGenre = ArrayAdapter.createFromResource(
            this, R.array.genre_Array, R.drawable.custom_spinner_item
        )
        adapterGenre.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGenre.adapter = adapterGenre

        etLocation = findViewById(R.id.etLocation)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        etLocation.setOnClickListener {
            if (!locationActive) getLocation()
        }

        val db = Firebase.firestore
        val nomPrenom = findViewById<EditText>(R.id.pat_nompre)
        val phone = findViewById<EditText>(R.id.pat_phone)
        val dateNaissance = findViewById<EditText>(R.id.editTextDateNaissance)
        val checkBox = findViewById<CheckBox>(R.id.checkBox)
        val boutonChercher = findViewById<Button>(R.id.Button_chercher)

        boutonChercher.setOnClickListener {
            val nom = nomPrenom.text.trim().toString()
            val tel = phone.text.trim().toString()
            val dateN = dateNaissance.text.trim().toString()
            val loc = etLocation.text.trim().toString()
            val genre = spinnerGenre.selectedItem?.toString()
            val specialite = spinnerSpecialite.selectedItem?.toString()

            // Vérification des champs obligatoires
            if (nom.isEmpty() || tel.isEmpty() || dateN.isEmpty() || loc.isEmpty() ||
                genre.isNullOrEmpty() || specialite.isNullOrEmpty()) {
                showToast("Veuillez remplir tous les champs")
                return@setOnClickListener
            }

            if (tel.length != 8) {
                showToast("Le numéro de téléphone doit comporter 8 chiffres")
                return@setOnClickListener
            }

            if (!checkBox.isChecked) {
                showToast("Vous devez accepter les conditions générales")
                return@setOnClickListener
            }

            val patient = hashMapOf(
                "nomPrenom" to nom,
                "telephone" to tel,
                "dateNaissance" to dateN,
                "specialite" to specialite,
                "localisation" to loc,
                "genre" to genre
            )

            db.collection("patients")
                .add(patient)
                .addOnSuccessListener {
                    showToast("Patient ajouté avec succès !")
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    showToast("Erreur lors de l'ajout du patient")
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    @SuppressLint("DefaultLocale")
    private fun showDatePickerDialog(editText: EditText) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this, { _, selectedYear, selectedMonth, selectedDay ->
                editText.setText(String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear))
            },
            year, month, day
        )
        datePickerDialog.show()
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION)
            return
        }

        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000
            fastestInterval = 5000
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return

                if (location.accuracy < MAX_ACCEPTABLE_ACCURACY) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val address = getAddressFromLocation(location.latitude, location.longitude)
                        withContext(Dispatchers.Main) {
                            etLocation.setText(address ?: "Lat: ${location.latitude}, Long: ${location.longitude}")
                        }
                    }
                } else {
                    showToast("Localisation peu précise")
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest!!, locationCallback, null)
        locationActive = true
    }

    private fun getAddressFromLocation(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            addresses?.firstOrNull()?.getAddressLine(0)
        } catch (e: Exception) {
            null
        }
    }

    override fun onStop() {
        super.onStop()
        if (locationActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
            locationActive = false
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_LOCATION_PERMISSION && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getLocation()
        } else {
            showToast("Permission de localisation refusée")
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1001
        private const val MAX_ACCEPTABLE_ACCURACY = 50
    }
}
