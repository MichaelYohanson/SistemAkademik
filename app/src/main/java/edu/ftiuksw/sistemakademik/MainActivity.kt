package edu.ftiuksw.sistemakademik

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var tvAdditionalInfo: TextView
    private lateinit var auth: FirebaseAuth
    private lateinit var realtimeDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvWelcome = findViewById(R.id.tv_welcome)
        tvAdditionalInfo = findViewById(R.id.tv_additional_info)
        auth = FirebaseAuth.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance().getReference()

        val userRole = intent.getStringExtra("user_role")
        val currentUser = auth.currentUser

        tvWelcome.text = "Selamat datang, ${userRole?.capitalize()}!"

        currentUser?.let { user ->
            fetchUserDataFromRealtimeDatabase(user.uid)
        } ?: run {
            Toast.makeText(this, "Anda belum login.", Toast.LENGTH_SHORT).show()
            logout()
        }

        when (userRole) {
            "kaprodi" -> showKaprodiFeatures()
            "dosen" -> showDosenFeatures()
            "mahasiswa" -> showMahasiswaFeatures()
        }

        findViewById<Button>(R.id.btn_logout)?.setOnClickListener {
            logout()
        }

    }

    private fun fetchUserDataFromRealtimeDatabase(uid: String) {
        realtimeDatabase.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val userName = snapshot.child("name").getValue(String::class.java)
                    val userNim = snapshot.child("nim").getValue(String::class.java)
                    val userNidn = snapshot.child("nidn").getValue(String::class.java)

                    tvAdditionalInfo.text = "Nama: ${userName ?: "N/A"}\n" +
                            "NIM: ${userNim ?: "N/A"}\n" +
                            "NIDN: ${userNidn ?: "N/A"}"
                    Log.d("MainActivity", "User data fetched from Realtime DB: $snapshot")
                } else {
                    tvAdditionalInfo.text = "Data tambahan tidak ditemukan di Realtime DB."
                    Log.d("MainActivity", "No user data found in Realtime DB for UID: $uid")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Failed to read user data from Realtime DB: ${error.message}", error.toException())
                Toast.makeText(this@MainActivity, "Gagal memuat data tambahan.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showKaprodiFeatures() { }
    private fun showDosenFeatures() { }
    private fun showMahasiswaFeatures() { }

    private fun logout() {
        auth.signOut()
        val intent = Intent(this, AuthActivity::class.java)
        startActivity(intent)
        finish()
    }
}