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
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import java.util.regex.Pattern
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.firestore.ktx.firestore

class IncripitionActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 9001
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_incripition)
        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

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
        var auth = Firebase.auth
        val buttonInscription: ImageButton = findViewById(R.id.imageButton3)
        val nom: EditText = findViewById(R.id.editText_nom)
        val prenom: EditText = findViewById(R.id.editText_prenom)
        val email: EditText = findViewById(R.id.editText_email)
        val password: EditText = findViewById(R.id.editText_password)

        buttonInscription.setOnClickListener {
            val txtNom = nom.text.toString().trim()
            val txtPrenom = prenom.text.toString().trim()
            val txtEmail = email.text.toString().trim()
            val txtPassword = password.text.toString().trim()

            if (validateInputs(txtNom, txtPrenom, txtEmail, txtPassword)) {
                auth.createUserWithEmailAndPassword(txtEmail, txtPassword)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val usersCollection = db.collection("users")
                            usersCollection.whereEqualTo("email", txtEmail).get()
                                .addOnSuccessListener { documents ->
                                    if (!documents.isEmpty) {
                                        Toast.makeText(this, "L'email existe déjà", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val userId = auth.currentUser?.uid
                                        val user = hashMapOf(
                                            "id" to userId,
                                            "nom" to txtNom,
                                            "prenom" to txtPrenom,
                                            "email" to txtEmail,
                                            "password" to txtPassword
                                        )

                                        usersCollection.add(user)
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Inscription réussie", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                Toast.makeText(this, "Erreur lors de l'ajout du document", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                }
                                .addOnFailureListener {
                                    Toast.makeText(this, "Erreur lors de la vérification de l'email", Toast.LENGTH_SHORT).show()
                                }
                        } else {
                            Toast.makeText(this, "${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
        val googleSignInButton: Button = findViewById(R.id.googleSignInButton)
        googleSignInButton.setOnClickListener {
            signInWithGoogle()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
    private fun validateInputs(nom: String, prenom: String, email: String, password: String): Boolean {
        if (nom.isEmpty() || prenom.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vous devez remplir tous les champs", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }
    private fun signInWithGoogle() {
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "Échec de l'authentification Google : ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Connexion réussie", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(this, "Échec de l'authentification : ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}