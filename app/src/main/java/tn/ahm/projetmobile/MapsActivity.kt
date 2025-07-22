package tn.ahm.projetmobile

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.util.Locale

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private val db = Firebase.firestore
    private var userLocation: LatLng? = null
    private lateinit var recyclerView: RecyclerView
    private lateinit var doctorAdapter: DoctorAdapter
    private lateinit var patientName:String
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        userLocation = intent.getParcelableExtra("userLocation")
        patientName = intent.getStringExtra("patientName").toString()

        if (userLocation == null) {
            showToast("Localisation utilisateur introuvable")
            Log.e("MapsActivity", "userLocation est null")
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
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
            fetchDoctorsSimultaneously(specialite)
        } else {
            showToast("Aucune spécialité sélectionnée")
            Log.e("MapsActivity", "Spécialité null ou vide")
        }
    }

    private fun fetchDoctorsSimultaneously(specialite: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val firebaseDoctors = async { fetchDoctorsFromFirebase(specialite) }

            val allDoctors = firebaseDoctors.await()

            withContext(Dispatchers.Main) {
                if (allDoctors.isEmpty()) {
                    showToast("Aucun médecin trouvé.")
                } else {
                    displayDoctorsOnMap(allDoctors)
                    // Afficher les médecins dans le RecyclerView
                    doctorAdapter = DoctorAdapter(allDoctors,patientName)
                    recyclerView.adapter = doctorAdapter
                }
            }
        }
    }

    /**
     * 📌 Recherche des médecins depuis Firebase Firestore
     */
    private suspend fun fetchDoctorsFromFirebase(specialite: String): List<Doctor> {
        val doctorsList = mutableListOf<Doctor>()

        return try {
            val result = db.collection("medecins")
                .whereEqualTo("Specialité", specialite)
                .get()
                .await()

            if (result.isEmpty) {
                Log.d("MapsActivity", "Aucun médecin trouvé dans Firebase")
                return doctorsList
            }

            for (document in result) {
                val nom = document.getString("nomprenom") ?: "Médecin inconnu"
                val doctorAddress = document.getString("adresse") ?: "Adresse inconnue"
                val modePaiement = document.getString("mode de paiement") ?: "Mode de paiement inconnu"
                val numtel=document.getString("telephone") ?:"Numero de télephone inconnu"
                val doctorLocation = getLatLngFromAddress(doctorAddress)
                if (doctorLocation != null) {
                    val distance = userLocation?.let { calculateDistance(it, doctorLocation) } ?: 0f
                    doctorsList.add(Doctor(nom, doctorLocation, distance, doctorAddress, modePaiement,numtel))
                }
            }

            doctorsList
        } catch (e: Exception) {
            Log.e("MapsActivity", "Erreur Firebase", e)
            doctorsList
        }
    }


    /**
     * 📍 Affiche les médecins sur la carte Google Maps
     */
    private fun displayDoctorsOnMap(doctors: List<Doctor>) {
        doctors.forEach { doctor ->
            googleMap.addMarker(
                MarkerOptions()
                    .position(doctor.location)
                    .title("${doctor.name} - ${doctor.address}")
                    .snippet("Mode de paiement : ${doctor.paymentMode}")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
        }

        // 🎯 Affichage du médecin le plus proche
        userLocation?.let {
            val closestDoctor = doctors.minByOrNull { it.distance }
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
                LatLng(addresses[0].latitude, addresses[0].longitude)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("MapsActivity", "Erreur conversion adresse", e)
            null
        }
    }
}
class DoctorAdapter(
    private val doctors: List<Doctor>,
    private val patientName: String // Ajouter patientName ici
) : RecyclerView.Adapter<DoctorAdapter.DoctorViewHolder>() {

    inner class DoctorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val doctorName: TextView = itemView.findViewById(R.id.doctorName)
        val doctorAddress: TextView = itemView.findViewById(R.id.doctorAddress)
        val doctorDistance: TextView = itemView.findViewById(R.id.doctorDistance)
        val doctorPaymentMode: TextView = itemView.findViewById(R.id.doctorPaymentMode)
        val doctorNumero: TextView = itemView.findViewById(R.id.doctornumero)
        val btnInfo: ImageButton = itemView.findViewById(R.id.btnInfo) // 🔥 Ajout du bouton info
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DoctorViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_doctor, parent, false)
        return DoctorViewHolder(view)
    }

    override fun onBindViewHolder(holder: DoctorViewHolder, position: Int) {
        val doctor = doctors[position]
        holder.doctorName.text = doctor.name
        holder.doctorAddress.text = doctor.address
        holder.doctorDistance.text = "Distance : ${doctor.distance} m"
        holder.doctorPaymentMode.text = "Mode de paiement : ${doctor.paymentMode}"
        holder.doctorNumero.text="Numéro de télephone: ${doctor.numtel}"
        holder.btnInfo.setOnClickListener {
            val context = holder.itemView.context
            val intent = Intent(context, BookingActivity::class.java).apply {
                putExtra("doctorName", doctor.name)
                putExtra("doctorAddress", doctor.address)
                putExtra("patientName", patientName)
            }
            context.startActivity(intent)
        }
    }


    override fun getItemCount(): Int {
        return doctors.size
    }
}
// 🎯 Modèle pour stocker les infos des médecins
data class Doctor(
    val name: String,
    val location: LatLng,
    val distance: Float,
    val address: String,
    val paymentMode: String,
    val numtel:String
)
