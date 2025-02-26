package tn.ahm.projetmobile

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class PatientActivity : AppCompatActivity() {
    @SuppressLint("ResourceType")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_patient)
        val editTextDateNaissance: EditText = findViewById(R.id.editTextDateNaissance)
        editTextDateNaissance.setOnClickListener {
            showDatePickerDialog(editTextDateNaissance)
        }
        val spinnerSpecialite: Spinner = findViewById(R.id.spinner_specialite)
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.specialites_array,
            R.drawable.custom_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerSpecialite.adapter = adapter
        val spinner_genre: Spinner = findViewById(R.id.spinner_genre)
        val adapter_genre = ArrayAdapter.createFromResource(
            this,
            R.array.genre_Array,
            R.drawable.custom_spinner_item
        )
        adapter_genre.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinner_genre.adapter = adapter_genre
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
        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedMonth = String.format("%02d", selectedMonth + 1)
            val formattedDay = String.format("%02d", selectedDay)
            val selectedDate = "$formattedDay/$formattedMonth/$selectedYear"
            editText.setText(selectedDate)
        }, year, month, day)
        datePickerDialog.show()
    }
}