package tn.ahm.projetmobile

import android.content.Intent
import android.os.Bundle
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

class ConnexionActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_connexion)
        val buttonInscription: Button = findViewById(R.id.imageButton)
        buttonInscription.setOnClickListener {
            val intent = Intent(this, IncripitionActivity::class.java)
            startActivity(intent)
        }
        val buttonAcceuil: Button = findViewById(R.id.imageButton2)
        buttonAcceuil.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
        val db = Firebase.firestore
        val email : EditText =findViewById(R.id.connexion_email)
        val password : EditText =findViewById(R.id.connexion_password)
        val button_connexion : ImageButton=findViewById(R.id.imageButton4)
        button_connexion.setOnClickListener{
            val txtemail = email.text.toString().trim()
            val txtpassword = password.text.toString().trim()
            if ( txtemail.isEmpty() || txtpassword.isEmpty()) {
                Toast.makeText(this, "Vous devez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
            else{
                db.collection("users").whereEqualTo("email", txtemail).get()
                    .addOnSuccessListener { documents ->
                        if(documents.isEmpty){
                            Toast.makeText(this,"Email non trouvé",Toast.LENGTH_SHORT).show()
                        }
                        else{
                            for(document in documents){
                                val pass=document.getString("password")
                                if(pass==txtpassword){
                                    Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()
                                }
                                else{
                                    Toast.makeText(this, "Mot de passe incorrect", Toast.LENGTH_SHORT).show()
                                }
                            }

                        }
                    }
                    .addOnFailureListener(){
                        Toast.makeText(this, "Erreur lors de la vérification}", Toast.LENGTH_SHORT).show()
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