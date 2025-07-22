package tn.ahm.projetmobile

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.util.*

class BookingActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: TimeSlotAdapter
    private lateinit var btnSelectDate: Button
    private lateinit var selectedDateTextView: TextView
    private var selectedDate: String = ""
    private val db = Firebase.firestore
    private lateinit var patientName: String
    private lateinit var doctorName: String

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        // Initialiser les vues
        recyclerView = findViewById(R.id.recyclerViewSlots)
        btnSelectDate = findViewById(R.id.btnSelectDate)
        selectedDateTextView = findViewById(R.id.selectedDateTextView)
        patientName = intent.getStringExtra("patientName") ?: "Patient inconnu"
        doctorName = intent.getStringExtra("doctorName") ?: "doctor inconnu"
        // Configurer le RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Ouvrir le sélecteur de date lorsque le bouton est cliqué
        btnSelectDate.setOnClickListener {
            showDatePickerDialog()
        }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, selectedYear, selectedMonth, selectedDay ->
                // Formater la date sélectionnée
                selectedDate = String.format("%02d/%02d/%d", selectedDay, selectedMonth + 1, selectedYear)
                selectedDateTextView.text = "Date sélectionnée : $selectedDate"

                // Vérifier si la date est valide (non passée)
                if (isDateValid(selectedDay, selectedMonth + 1, selectedYear)) {
                    // Charger les créneaux disponibles pour la date sélectionnée
                    fetchAvailableSlots(selectedDate)
                } else {
                    Toast.makeText(this, "Veuillez sélectionner une date valide", Toast.LENGTH_SHORT).show()
                }
            },
            year, month, day
        )
        // Empêcher la sélection de dates passées
        datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000
        datePickerDialog.show()
    }

    private fun isDateValid(day: Int, month: Int, year: Int): Boolean {
        val selectedCalendar = Calendar.getInstance().apply {
            set(year, month - 1, day)
        }
        val today = Calendar.getInstance()
        return !selectedCalendar.before(today)
    }

    private fun fetchAvailableSlots(date: String) {
        db.collection("reservations")
            .whereEqualTo("date", date)
            .whereEqualTo("doctorName", doctorName)
            .get()
            .addOnSuccessListener { result ->
                val reservedSlots = result.map { it.getString("heure") ?: "" }
                val allSlots = generateTimeSlots()
                val availableSlots = allSlots.filter { it !in reservedSlots }
                adapter = TimeSlotAdapter(availableSlots) { selectedSlot ->
                    confirmBooking(date, selectedSlot)
                }
                recyclerView.adapter = adapter
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erreur de chargement des créneaux", Toast.LENGTH_SHORT).show()
            }
    }

    private fun generateTimeSlots(): List<String> {
        val timeSlots = mutableListOf<String>()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 8) // Début à 8h
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)

        val endCalendar = Calendar.getInstance()
        endCalendar.set(Calendar.HOUR_OF_DAY, 17) // Fin à 17h
        endCalendar.set(Calendar.MINUTE, 0)
        endCalendar.set(Calendar.SECOND, 0)

        while (calendar.before(endCalendar)) {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)
            val timeSlot = String.format("%02d:%02d", hour, minute)
            timeSlots.add(timeSlot)
            calendar.add(Calendar.MINUTE, 15) // Ajouter 15 minutes
        }

        return timeSlots
    }

    private fun confirmBooking(date: String, slot: String) {
        // Afficher une boîte de dialogue de confirmation
        AlertDialog.Builder(this)
            .setTitle("Confirmer la réservation")
            .setMessage("Voulez-vous confirmer le rendez-vous le $date à $slot ?")
            .setPositiveButton("OK") { dialog, _ ->
                // Ajouter la réservation à Firestore lorsque l'utilisateur appuie sur "OK"
                val booking = hashMapOf(
                    "doctorName" to doctorName,
                    "date" to date,
                    "heure" to slot,
                    "patientName" to patientName
                )

                db.collection("reservations").add(booking)
                    .addOnSuccessListener {
                        // Afficher un message de succès
                        showFeedbackDialog("Succès", "Rendez-vous confirmé le $date à $slot")
                    }
                    .addOnFailureListener { e ->
                        // Afficher un message d'échec
                        Toast.makeText(this, "Échec de la réservation : ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                dialog.dismiss()
            }
            .setNegativeButton("Annuler") { dialog, _ ->
                // Fermer la boîte de dialogue si l'utilisateur annule
                dialog.dismiss()
            }
            .show()
    }
    private fun showFeedbackDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}