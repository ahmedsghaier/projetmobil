package tn.ahm.projetmobile

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ReportFragment.Companion.reportFragment
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class MedecinActivity : AppCompatActivity() {
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_medecin)
        val spinnerSpecialite: Spinner = findViewById(R.id.spinner_specialite)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.specialites_array,
            R.drawable.custom_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecialite.adapter = adapter
        val spinnerPaiement: Spinner = findViewById(R.id.spinner_paiement)
        val adapterPaiement = ArrayAdapter.createFromResource(
            this,
            R.array.paiement_array,
            R.drawable.custom_spinner_item
        )
        adapterPaiement.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPaiement.adapter = adapterPaiement
        val db = Firebase.firestore
        val med_nompre: EditText = findViewById(R.id.med_nompre)
        val med_phone:EditText=findViewById(R.id.med_phone)
        val med_adr:EditText=findViewById(R.id.med_adr)
        val Button_med_ins:ImageButton=findViewById(R.id.Button_med_ins)
        val spinner_specialite:Spinner = findViewById(R.id.spinner_specialite)
        val spinner_paiement:Spinner = findViewById(R.id.spinner_paiement)
        Button_med_ins.setOnClickListener {
            val txtmed_nompre = med_nompre.text.toString().trim()
            val txtmed_phone = med_phone.text.toString().trim()
            val txtmed_adr = med_adr.text.toString().trim()
            val selectedSpecialite = spinner_specialite.selectedItem.toString()
            val selectedPaiement = spinner_paiement.selectedItem.toString()

            if (validateInputs(txtmed_nompre, txtmed_phone, txtmed_adr)) {
                Toast.makeText(this, "Inscription réussie", Toast.LENGTH_SHORT).show()
                val medecinsCollection = db.collection("medecins")
                val medecins = hashMapOf(
                    "nomprenom" to txtmed_nompre,
                    "telephone" to txtmed_phone,
                    "Specialité" to selectedSpecialite,
                    "adresse" to txtmed_adr,
                    "mode de paiement" to selectedPaiement
                )
                medecinsCollection.add(medecins)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Inscription réussie", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Erreur lors de l'ajout du document", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun validateInputs(nompre: String, phone: String, adr: String): Boolean {
        if (nompre.isEmpty() || phone.isEmpty() || adr.isEmpty()) {
            Toast.makeText(this, "Tous les champs sont requis", Toast.LENGTH_SHORT).show()
            return false
        }
        if (phone.length != 8) {
            Toast.makeText(this, "Le numéro de téléphone doit comporter 8 chiffres", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

}