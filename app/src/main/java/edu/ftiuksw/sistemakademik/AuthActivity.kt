package edu.ftiuksw.sistemakademik

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class AuthActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var realtimeDatabase: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_auth)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        realtimeDatabase = FirebaseDatabase.getInstance().getReference()

        etUsername = findViewById(R.id.et_username)
        etPassword = findViewById(R.id.et_password)
        btnLogin = findViewById(R.id.btn_login)

        btnLogin.setOnClickListener {
            performLogin()
        }
    }

    private fun performLogin() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Username dan password tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        if (!username.startsWith("67")) {
            Toast.makeText(this, "Username harus diawali dengan '67'", Toast.LENGTH_SHORT).show()
            return
        }

        val email = "$username@uksw.ac.id"

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.let { firebaseUser ->
                        firestore.collection("users").document(firebaseUser.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    val role = document.getString("role")
                                    val nim = document.getString("nim")
                                    val nidn = document.getString("nidn")

                                    if (role != null) {
                                        saveUserDataToRealtimeDatabase(firebaseUser.uid, role, nim, nidn)
                                    } else {
                                        Log.w("AuthActivity", "Role is null, not saving to Realtime Database.")
                                    }

                                    when (role) {
                                        "kaprodi" -> {
                                            if (nidn?.startsWith("67") == true && nidn.length == 7) {
                                                navigateToMain(role)
                                            } else {
                                                Toast.makeText(this, "Akses Kaprodi Ditolak: Kode Dosen tidak valid.", Toast.LENGTH_SHORT).show()
                                                auth.signOut()
                                            }
                                        }
                                        "dosen" -> {
                                            if (nidn?.startsWith("67") == true && nidn.length == 7) {
                                                navigateToMain(role)
                                            } else {
                                                Toast.makeText(this, "Akses Dosen Ditolak: Kode Dosen tidak valid.", Toast.LENGTH_SHORT).show()
                                                auth.signOut()
                                            }
                                        }
                                        "mahasiswa" -> {
                                            if (nim?.startsWith("67") == true) {
                                                navigateToMain(role)
                                            } else {
                                                Toast.makeText(this, "Akses Mahasiswa Ditolak: NIM tidak valid.", Toast.LENGTH_SHORT).show()
                                                auth.signOut()
                                            }
                                        }
                                        else -> {
                                            Toast.makeText(this, "Role tidak dikenali.", Toast.LENGTH_SHORT).show()
                                            auth.signOut()
                                        }
                                    }
                                } else {
                                    Toast.makeText(this, "Data pengguna tidak ditemukan.", Toast.LENGTH_SHORT).show()
                                    auth.signOut()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("AuthActivity", "Error getting user document", e)
                                Toast.makeText(this, "Gagal mendapatkan data pengguna.", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Autentikasi gagal: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMain(role: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("user_role", role)
        startActivity(intent)
        finish()
    }

    private fun saveUserDataToRealtimeDatabase(uid: String, role: String, nim: String?, nidn: String?) {
        val userData = hashMapOf(
            "role" to role,
            "nim" to nim,
            "nidn" to nidn
        )

        realtimeDatabase.child("users").child(uid).setValue(userData)
            .addOnSuccessListener {
                Log.d("Firebase", "User data saved to Realtime Database successfully!")
            }
            .addOnFailureListener { e ->
                Log.w("Firebase", "Error saving user data to Realtime Database", e)
                Toast.makeText(this, "Gagal menyimpan data ke Realtime DB.", Toast.LENGTH_SHORT).show()
            }
    }
}