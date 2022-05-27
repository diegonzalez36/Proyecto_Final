package com.example.logingoogle

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.activity_home.*
import java.util.HashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

enum class ProviderType{
    BASIC,
    GOOGLE
}

private const val REQUEST_CODE_IMAGE_PICK = 0

class HomeActivity : AppCompatActivity() {
    private val fileResult = 1
    val imageRef = Firebase.storage.reference
    private val database = Firebase.database
    private val myRef = database.getReference("user")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        //Setup
        val bundle:Bundle? = intent.extras
        val email:String? = bundle?.getString("email")
        val provider:String? = bundle?.getString("provider")
        setup(email ?: "", provider ?: "")
        //Guardado de datos
        val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
        prefs.putString("email", email)
        prefs.putString("provider", provider)
        //Subir archivo
        uploadImageView.setOnClickListener {
            fileManager();
        }

    }

    private fun setup(email: String, provider: String){

        title = "Inicio"
        emailTextView.text = email
        providerTextView.text = provider

        signOutButton.setOnClickListener{
            //Borrado de datos
            val prefs = getSharedPreferences(getString(R.string.prefs_file), Context.MODE_PRIVATE).edit()
            prefs.clear()
            prefs.apply()

            FirebaseAuth.getInstance().signOut()
            onBackPressed()
        }
    }
    //Storage
    private fun fileManager() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2) {
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        intent.type = "*/*"
        startActivityForResult(intent, fileResult)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == fileResult) {
            if (resultCode == RESULT_OK && data != null) {

                val clipData = data.clipData

                if (clipData != null){
                    for (i in 0 until clipData.itemCount) {
                        val uri = clipData.getItemAt(i).uri
                        uri?.let { fileUpload(it) }
                    }
                }else {
                    val uri = data.data
                    uri?.let { fileUpload(it) }
                }

            }
        }
    }

    private fun fileUpload(mUri: Uri) {
        val folder: StorageReference = FirebaseStorage.getInstance().reference.child("User")
        val path =mUri.lastPathSegment.toString()
        val fileName: StorageReference = folder.child(path.substring(path.lastIndexOf('/')+1))

        fileName.putFile(mUri).addOnSuccessListener {
            fileName.downloadUrl.addOnSuccessListener { uri ->
                val hashMap = HashMap<String, String>()
                hashMap["link"] = java.lang.String.valueOf(uri)

                myRef.child(myRef.push().key.toString()).setValue(hashMap)

                Log.i("message", "file upload successfully")
            }
        }.addOnFailureListener {
            Log.i("message", "file upload error")
        }
    }

    }
