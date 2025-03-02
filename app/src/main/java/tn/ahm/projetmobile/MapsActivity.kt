package tn.ahm.projetmobile

import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val db = Firebase.firestore
    private var userLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        userLocation = intent.getParcelableExtra("userLocation")

        if (userLocation == null) {
            showToast("Localisation utilisateur introuvable")
            Log.e("MapsActivity", "userLocation est null")
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Affichage de la position utilisateur si disponible
        userLocation?.let {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(it, 12f))
            googleMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title("Votre position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }

        val specialite = intent.getStringExtra("specialite")
        if (!specialite.isNullOrEmpty()) {
            fetchDoctorsBySpeciality(specialite)
        } else {
            showToast("Aucune spécialité sélectionnée")
            Log.e("MapsActivity", "Spécialité null ou vide")
        }
    }

    private fun fetchDoctorsBySpeciality(specialite: String) {
        db.collection("medecins")
            .whereEqualTo("Specialité", specialite)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    showToast("Aucun médecin trouvé pour cette spécialité")
                    return@addOnSuccessListener
                }

                val doctorsList = mutableListOf<Doctor>()

                // Parcourir chaque document (médecin)
                for (document in documents) {
                    val nom = document.getString("nomprenom") ?: "Médecin inconnu"
                    val doctorAddress = document.getString("adresse") ?: "Adresse inconnue"
                    val modePaiement = document.getString("mode de paiement") ?: "Mode de paiement inconnu"

                    // Convertir l'adresse en coordonnées GPS (latitude, longitude)
                    CoroutineScope(Dispatchers.IO).launch {
                        val doctorLocation = getLatLngFromAddress(doctorAddress)

                        withContext(Dispatchers.Main) {
                            if (doctorLocation != null) {
                                // Ajouter le médecin à la liste avec la distance
                                userLocation?.let { userLoc ->
                                    val distance = calculateDistance(userLoc, doctorLocation)
                                    doctorsList.add(Doctor(nom, doctorLocation, distance, doctorAddress, modePaiement))
                                }

                                // Ajouter un marqueur pour le médecin
                                googleMap.addMarker(
                                    MarkerOptions()
                                        .position(doctorLocation)
                                        .title("$nom - $doctorAddress")
                                        .snippet("Mode de paiement : $modePaiement")
                                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                                )
                            } else {
                                Log.e("MapsActivity", "Impossible de convertir l'adresse en coordonnées pour $nom")
                            }
                        }
                    }
                }

                // Trouver et afficher le médecin le plus proche
                userLocation?.let {
                    val closestDoctor = doctorsList.minByOrNull { it.distance }
                    closestDoctor?.let { doctor ->
                        showToast("Médecin le plus proche : ${doctor.name} (${doctor.distance} m)")

                        googleMap.addMarker(
                            MarkerOptions()
                                .position(doctor.location)
                                .title("Médecin le plus proche : ${doctor.name}")
                                .snippet("Mode de paiement : ${doctor.paymentMode}")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                        )

                        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(doctor.location, 14f))
                    }
                }
            }
            .addOnFailureListener {
                showToast("Erreur lors de la récupération des médecins")
                Log.e("MapsActivity", "Erreur Firestore", it)
            }
    }

    private fun calculateDistance(userLocation: LatLng, doctorLocation: LatLng): Float {
        val results = FloatArray(1)
        Location.distanceBetween(
            userLocation.latitude, userLocation.longitude,
            doctorLocation.latitude, doctorLocation.longitude,
            results
        )
        return results[0]
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    private suspend fun getLatLngFromAddress(address: String): LatLng? {
        return try {
            val geocoder = Geocoder(this, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(address, 1)
            if (!addresses.isNullOrEmpty()) {
                val latitude = addresses[0].latitude
                val longitude = addresses[0].longitude
                LatLng(latitude, longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MapsActivity", "Erreur lors de la conversion de l'adresse en coordonnées", e)
            null
        }
    }
}

// Data class pour stocker les informations des médecins
data  class Doctor(
    val name: String,
    val location: LatLng,
    val distance: Float,
    val address: String,
    val paymentMode: String
)
