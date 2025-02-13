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
                Toast.makeText(this, "Inscription validée", Toast.LENGTH_SHORT).show()
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}