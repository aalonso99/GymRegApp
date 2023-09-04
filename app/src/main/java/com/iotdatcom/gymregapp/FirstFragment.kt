package com.iotdatcom.gymregapp

// Google Drive API

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.aware.Aware
import com.chaquo.python.PyObject
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.iotdatcom.gymregapp.databinding.FragmentFirstBinding
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() { //class FirstFragment : Fragment(), ServiceListener {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var py: Python? = null
    private var pyobj: PyObject? = null

    private val REQUEST_CODE_SIGN_IN = 1
//    private val REQUEST_CODE_OPEN_DOCUMENT = 2
    private val signInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_FILE))
        .build()
    private var client: GoogleSignInClient? = null
    private var mDriveServiceHelper: DriveServiceHelper? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.buttonStart.setOnClickListener {
            startSensors()
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.uploadButton.setOnClickListener {
            requestSignInAndUpload()
        }

//        checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE, 101)
//        checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, 100)
//        checkPermission(Manifest.permission.INTERNET, 99)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun startSensors() {
        measureLinearAcceleration()
        measureRotation()
    }

    private fun measureLinearAcceleration() {

        Aware.startLinearAccelerometer(context?.applicationContext)

    }

    private fun measureRotation() {

        Aware.startGyroscope(context?.applicationContext)

    }

    private fun uploadSensorData() {

        uploadFile("/storage/emulated/0/Android/data/com.iotdatcom.gymregapp/files/AWARE/linear_accelerometer.db",
            "linear_accelerometer.db",
            false)
        uploadFile("/storage/emulated/0/Android/data/com.iotdatcom.gymregapp/files/AWARE/gyroscope.db",
            "gyroscope.db",
            true)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        when (requestCode) {
            REQUEST_CODE_SIGN_IN -> if (resultCode == Activity.RESULT_OK && resultData != null) {
                handleSignInResult(resultData)
            }
//            REQUEST_CODE_OPEN_DOCUMENT -> if (resultCode == Activity.RESULT_OK && resultData != null) {
//                val uri: Uri? = resultData.data
//                if (uri != null) {
//                    openFileFromFilePicker(uri)
//                }
//            }
        }
        super.onActivityResult(requestCode, resultCode, resultData)
    }

    /**
     * Starts a sign-in activity using [.REQUEST_CODE_SIGN_IN].
     */
    private fun requestSignInAndUpload() {
        Log.d(TAG, "Requesting sign-in")

        client = GoogleSignIn.getClient(requireActivity(), signInOptions)

        // The result of the sign-in Intent is handled in onActivityResult.
        startActivityForResult(client?.signInIntent, REQUEST_CODE_SIGN_IN)
    }

    /**
     * Handles the `result` of a completed sign-in activity initiated from [ ][.requestSignIn].
     */
    private fun handleSignInResult(result: Intent) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener { googleAccount: GoogleSignInAccount ->
                Log.d(TAG, "Signed in as " + googleAccount.email)

                // Use the authenticated account to sign in to the Drive service.
                val credential = GoogleAccountCredential.usingOAuth2(
                    requireContext(), Collections.singleton(DriveScopes.DRIVE_FILE)
                )
                credential.selectedAccount = googleAccount.account
                val googleDriveService = Drive.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    GsonFactory(),
                    credential
                )
                    .setApplicationName("GymRegApp")
                    .build()

                // The DriveServiceHelper encapsulates all REST API and SAF functionality.
                // Its instantiation is required before handling any onClick actions.
                mDriveServiceHelper = DriveServiceHelper(googleDriveService)

                uploadSensorData()
            }
            .addOnFailureListener { exception: Exception? ->
                Log.e(
                    TAG,
                    "Unable to sign in.",
                    exception
                )
            }
    }

    private fun createFile() {
        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.")
            mDriveServiceHelper!!.createFile()
                .addOnFailureListener { exception: java.lang.Exception? ->
                    Log.e(
                        TAG,
                        "Couldn't create file.",
                        exception
                    )
                }
        }
    }

    private fun uploadFile(file: String, targetFileName: String, logout: Boolean = false) {

        if (mDriveServiceHelper != null) {
            Log.d(TAG, "Creating a file.")
            mDriveServiceHelper!!.createFile()
                .addOnSuccessListener { fileId: String ->
                    val fileContent: ByteArray = Files.readAllBytes(Paths.get(file))
                    mDriveServiceHelper!!.saveFile(fileId, targetFileName, fileContent)
                        .addOnFailureListener { exception: java.lang.Exception? ->
                            Log.e(
                                TAG,
                                "Unable to save file via REST.",
                                exception
                            )
                            if(logout) client?.signOut()
                        }
                    if(logout) client?.signOut()
                }
                .addOnFailureListener { exception: java.lang.Exception? ->
                    Log.e(
                        TAG,
                        "Couldn't create file.",
                        exception
                    )
                    if(logout) client?.signOut()
                }
        }
    }

    // Function to check and request permission.
    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_DENIED) {
            // Requesting the permission
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(permission), requestCode)
        } else {
            Toast.makeText(requireContext(), "Permission already granted", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initPython(){
        // Setting Python
        if(!Python.isStarted()) {
            Python.start(AndroidPlatform(requireContext()))
        }

        py = Python.getInstance()
        pyobj = py!!.getModule("upload")
    }

    //    /**
//     * Opens the Storage Access Framework file picker using [.REQUEST_CODE_OPEN_DOCUMENT].
//     */
//    private fun openFilePicker() {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Opening file picker.")
//            val pickerIntent = mDriveServiceHelper!!.createFilePickerIntent()
//
//            // The result of the SAF Intent is handled in onActivityResult.
//            startActivityForResult(pickerIntent, REQUEST_CODE_OPEN_DOCUMENT)
//        }
//    }
//
//    /**
//     * Opens a file from its `uri` returned from the Storage Access Framework file picker
//     * initiated by [.openFilePicker].
//     */
//    private fun openFileFromFilePicker(uri: Uri) {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Opening " + uri.path)
//            mDriveServiceHelper!!.openFileUsingStorageAccessFramework(getContentResolver(), uri)
//                .addOnSuccessListener { nameAndContent: Pair<String, String> ->
//                    val name = nameAndContent.first
//                    val content = nameAndContent.second
//                    mFileTitleEditText.setText(name)
//                    mDocContentEditText.setText(content)
//
//                    // Files opened through SAF cannot be modified.
//                    setReadOnlyMode()
//                }
//                .addOnFailureListener { exception: java.lang.Exception? ->
//                    Log.e(
//                        TAG,
//                        "Unable to open file from picker.",
//                        exception
//                    )
//                }
//        }
//    }

    /**
     * Retrieves the title and content of a file identified by `fileId` and populates the UI.
     */
//    private fun readFile(fileId: String) {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Reading file $fileId")
//            mDriveServiceHelper!!.readFile(fileId)
//                .addOnSuccessListener { nameAndContent: Pair<String, String> ->
//                    val name = nameAndContent.first
//                    val content = nameAndContent.second
//                    mFileTitleEditText.setText(name)
//                    mDocContentEditText.setText(content)
//                    setReadWriteMode(fileId)
//                }
//                .addOnFailureListener { exception: java.lang.Exception? ->
//                    Log.e(
//                        TAG,
//                        "Couldn't read file.",
//                        exception
//                    )
//                }
//        }
//    }

    /**
     * Creates a new file via the Drive REST API.
     */
//    private fun createFile() {
//        if (mDriveServiceHelper != null) {
//            Log.d(TAG, "Creating a file.")
//            mDriveServiceHelper!!.createFile()
//                .addOnSuccessListener { fileId: String ->
//                    readFile(
//                        fileId
//                    )
//                }
//                .addOnFailureListener { exception: java.lang.Exception? ->
//                    Log.e(
//                        TAG,
//                        "Couldn't create file.",
//                        exception
//                    )
//                }
//        }
//    }

    /**
     * Saves the currently opened file created via [.createFile] if one exists.
     */
//    private fun saveFile(fileId: String, file: String, targetFileName: String) {
//
//        println("Hola1")
//        if (mDriveServiceHelper != null) {
//            println("Hola2")
//            val fileContent: ByteArray = Files.readAllBytes(Paths.get(file))
//            mDriveServiceHelper!!.saveFile(fileId, targetFileName, fileContent)
//                .addOnFailureListener { exception: java.lang.Exception? ->
//                    Log.e(
//                        TAG,
//                        "Unable to save file via REST.",
//                        exception
//                    )
//                }
//        }
//    }

    //    private fun uploadSensorDataPython() {
//
//        try {
//
//            pyobj?.callAttr("upload_and_remove",
//                "/storage/emulated/0/Android/data/com.iotdatcom.gymregapp/files/AWARE")
//
//        }catch (e:Exception) {
//            System.err.println("Exception caught: ${e.stackTraceToString()}")
//        }
//
//    }

//    private fun uploadSensorDataPost() {
//
//        Thread {
//            val token = "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"
//            val yourFile = "/storage/emulated/0/Android/data/com.iotdatcom.gymregapp/files/AWARE/linear_accelerometer.db"
//
//            try {
//                val response = httpPost {
//                    url("https://www.googleapis.com/upload/drive/v3/files?uploadType=media")
//                    header {
//                        "Authorization" to "Bearer $token"
//                    }
//
//                    body {
//                        file(File(yourFile))
//                    }
//                }
//
//                println(response)
//            } catch (e: Exception) {
//                e.printStackTrace()
//            }
//        }.start()
//
//    }

}