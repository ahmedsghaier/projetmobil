package tn.ahm.projetmobile

import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class IncripitionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_incripition)
        val button_connexion: Button = findViewById(R.id.imageButton)
        button_connexion.setOnClickListener {
            val intent = Intent(this, ConnexionActivity::class.java)
            startActivity(intent)
        }
        val buttonAcceuil: Button = findViewById(R.id.imageButton2)
        buttonAcceuil.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val db = Firebase.firestore
        val button_Inscripition :ImageButton=findViewById(R.id.imageButton3)
        val nom : EditText =findViewById(R.id.editText_nom)
        val prenom : EditText =findViewById(R.id.editText_prenom)
        val email : EditText =findViewById(R.id.editText_email)
        val password : EditText =findViewById(R.id.editText_password)
        button_Inscripition.setOnClickListener{
            val txtnom = nom.text.toString().trim()
            val txtprenom = prenom.text.toString().trim()
            val txtemail = email.text.toString().trim()
            val txtpassword = password.text.toString().trim()

            if (txtnom.isEmpty() || txtprenom.isEmpty() || txtemail.isEmpty() || txtpassword.isEmpty()) {
                Toast.makeText(this, "Vous devez remplir tous les champs", Toast.LENGTH_SHORT).show()
            } else {
                val usersCollection = db.collection("users")
                usersCollection.whereEqualTo("email", txtemail).get()
                    .addOnSuccessListener { documents ->
                        if (!documents.isEmpty) {
                            Toast.makeText(this, "L'email existe déjà", Toast.LENGTH_SHORT).show()
                        }
                        else {
                            val user = hashMapOf(
                                "nom" to txtnom,
                                "prenom" to txtprenom,
                                "email" to txtemail,
                                "password" to txtpassword,
                                )
                            usersCollection.add(user)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "DocumentSnapshot added ",Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Error adding document", Toast.LENGTH_SHORT).show()
                    }
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}