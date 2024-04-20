package com.sipl.egs.ui.gramsevak

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.sipl.egs.R
import com.sipl.egs.database.AppDatabase
import com.sipl.egs.database.dao.LabourDao
import com.sipl.egs.database.entity.Labour
import com.sipl.egs.database.model.LabourWithAreaNames
import com.sipl.egs.databinding.ActivitySyncLabourDataBinding
import com.sipl.egs.adapters.OfflineLabourListAdapter
import com.sipl.egs.model.FamilyDetails
import com.sipl.egs.utils.CustomProgressDialog
import com.sipl.egs.utils.NoInternetDialog
import com.sipl.egs.webservice.ApiClient
import com.sipl.egs.webservice.FileInfo
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.Calendar

class SyncLabourDataActivity : AppCompatActivity() {
    private lateinit var binding:ActivitySyncLabourDataBinding
    private lateinit var database: AppDatabase
    private lateinit var labourDao: LabourDao
    lateinit var labourList:List<LabourWithAreaNames>
    lateinit var  adapter: OfflineLabourListAdapter
    private lateinit var dialog:CustomProgressDialog

    private var isInternetAvailable=false
    private lateinit var noInternetDialog: NoInternetDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding= ActivitySyncLabourDataBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title=resources.getString(R.string.sync_labour_data)
        dialog=CustomProgressDialog(this)
        dialog.show()
        val layoutManager=LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        binding.recyclerViewSyncLabourData.layoutManager=layoutManager
        database= AppDatabase.getDatabase(this)
        labourDao=database.labourDao()
        labourList=ArrayList<LabourWithAreaNames>()
        adapter= OfflineLabourListAdapter(labourList)
        adapter.notifyDataSetChanged()

        noInternetDialog= NoInternetDialog(this)
        ReactiveNetwork
            .observeNetworkConnectivity(applicationContext)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({ connectivity: Connectivity ->
                Log.d("##", "=>" + connectivity.state())
                if (connectivity.state().toString() == "CONNECTED") {
                    isInternetAvailable = true
                    noInternetDialog.hideDialog()
                } else {
                    isInternetAvailable = false
                    noInternetDialog.showDialog()
                }
            }) { throwable: Throwable? -> }
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_sync,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId==android.R.id.home){
            //finish()
        }
        if(item.itemId==R.id.navigation_sync){

            //UploadManager.startUploadTask(this@SyncLabourDataActivity)
            //syncLabourData()
           if(isInternetAvailable){
               CoroutineScope(Dispatchers.IO).launch {
                   uploadLabourOnline()
               }
           }else{
               noInternetDialog.showDialog()
           }

        }

        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.IO).launch{
            labourList=labourDao.getLabourWithAreaNames()
            Log.d("mytag","=>"+labourList.size)
            val listWithName=labourDao.getLabourWithAreaNames()
            if(!listWithName.isNullOrEmpty()){

                Log.d("mytag",""+Gson().toJson(listWithName))
            }else{
                Log.d("mytag","Empty Or Nukk ")
            }
            withContext(Dispatchers.Main) {
                dialog.dismiss()
                adapter= OfflineLabourListAdapter(labourList)
                binding.recyclerViewSyncLabourData.adapter=adapter
                adapter.notifyDataSetChanged() // Notify the adapter that the data has changed
            }
        }
        Log.d("mytag",""+labourList.size)

    }
    override fun onPostResume() {
        super.onPostResume()
    }

    override fun onRestart() {
        super.onRestart()
        dialog.show()
        CoroutineScope(Dispatchers.IO).launch{
            labourList=labourDao.getLabourWithAreaNames()
            Log.d("mytag","=>"+labourList.size)

            withContext(Dispatchers.Main) {
                dialog.dismiss()
                adapter= OfflineLabourListAdapter(labourList)
                binding.recyclerViewSyncLabourData.adapter=adapter
                adapter.notifyDataSetChanged() // Notify the adapter that the data has changed
            }
        }
        Log.d("mytag",""+labourList.size)
    }

    fun fetchUserList(){
        CoroutineScope(Dispatchers.IO).launch{
            labourList=labourDao.getLabourWithAreaNames()
            Log.d("mytag","=>"+labourList.size)

            withContext(Dispatchers.Main) {
                dialog.dismiss()
                adapter= OfflineLabourListAdapter(labourList)
                binding.recyclerViewSyncLabourData.adapter=adapter
                adapter.notifyDataSetChanged() // Notify the adapter that the data has changed
            }
        }
        Log.d("mytag",""+labourList.size)
    }




    private suspend fun getLaborRegistrationsFromDatabase(): List<Labour> {
        val list = AppDatabase.getDatabase(applicationContext).labourDao().getAllLabour()
        return list
    }
    suspend fun uriToFile(context: Context, uri: String): File? {
        return withContext(Dispatchers.IO) {
            try {
                val requestOptions = RequestOptions()
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache to avoid reading from cache
                    .skipMemoryCache(true) // Skip memory cache
                val bitmap = Glide.with(context)
                    .asBitmap()
                    .load(uri)
                    .apply(requestOptions)
                    .submit()
                    .get()
                val time=Calendar.getInstance().timeInMillis.toString()
                // Create a temporary file to store the bitmap
                val file = File(context.cacheDir, "$time.jpg")
                val outputStream = FileOutputStream(file)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.flush()
                outputStream.close()

                file // Return the temporary file
            } catch (e: Exception) {
                Log.d("mytag", "Exception uriToFile: ${e.message}")
                null // Return null if there's an error
            }
        }
    }
    private suspend fun createFilePart(fileInfo: FileInfo): MultipartBody.Part? {
        Log.d("mytag",""+fileInfo.fileUri)
        val file: File? = uriToFile(applicationContext, fileInfo.fileUri)
        return file?.let {
            val requestFile = RequestBody.create("image/*".toMediaTypeOrNull(), it)
            MultipartBody.Part.createFormData(fileInfo.fileName, it.name, requestFile)
        }
    }

    private suspend fun uploadLabourOnline(){
       runOnUiThread {
           dialog.show()
       }

        val apiService = ApiClient.create(this@SyncLabourDataActivity)
        CoroutineScope(Dispatchers.IO).launch {
            val labours = getLaborRegistrationsFromDatabase()
            try {
                labours.forEach { labour ->
                    val aadharCardImage =
                        createFilePart(FileInfo("aadhar_image", labour.aadharImage))
                    val voterIdImage =
                        createFilePart(FileInfo("voter_image", labour.voterIdImage))
                    val profileImage =
                        createFilePart(FileInfo("profile_image", labour.photo))
                    val mgnregaIdImage =
                        createFilePart(FileInfo("mgnrega_image", labour.mgnregaIdImage))

                    val response= apiService.uploadLaborInfo(
                        fullName = labour.fullName,
                        genderId = labour.gender,
                        dateOfBirth = labour.dob,
                        skillId = labour.skill,
                        districtId = labour.district,
                        talukaId = labour.taluka,
                        villageId = labour.village,
                        mobileNumber = labour.mobile,
                        mgnregaId = labour.mgnregaId,
                        landLineNumber = labour.landline,
                        family = labour.familyDetails,
                        longitude = labour.latitude,
                        latitude = labour.longitude,
                        file1 = aadharCardImage!!,
                        file2 = voterIdImage!!,
                        file3 = profileImage!!,
                        file4 = mgnregaIdImage!!)

                    if(response.isSuccessful){
                        if(response.body()?.status.equals("true")){
                            labour.isSynced=true
                            labourDao.updateLabour(labour)
                            val filesList= mutableListOf<Uri>()
                            filesList.add(Uri.parse(labour.aadharImage))
                            filesList.add(Uri.parse(labour.photo))
                            filesList.add(Uri.parse(labour.voterIdImage))
                            filesList.add(Uri.parse(labour.mgnregaIdImage))
                            deleteFilesFromFolder(filesList)
                        }else{
                            labour.isSyncFailed=true
                            labour.syncFailedReason=response.body()?.message
                            labourDao.updateLabour(labour)
                        }
                        Log.d("mytag",""+response.body()?.message)
                        Log.d("mytag",""+response.body()?.status)
                    }else{
                        labour.isSyncFailed=true
                        if(response.body()?.message.isNullOrEmpty()){

                        }else{
                            labour.syncFailedReason=response.body()?.message
                        }

                        labourDao.updateLabour(labour)
                        Log.d("mytag","Labour upload failed  "+labour.fullName)
                    }
                }
                    runOnUiThread {dialog.dismiss()  }
                fetchUserList()
            } catch (e: Exception) {
              runOnUiThread { dialog.dismiss() }
                Log.d("mytag","uploadLabourOnline "+e.message)
            }
        }

    }

    private suspend fun deleteFilesFromFolder(urisToDelete: List<Uri>) {
        try {
            val mediaStorageDir = File(externalMediaDirs[0], "myfiles")
            val files = mediaStorageDir.listFiles()
            files?.forEach { file ->
                if (file.isFile) {
                    val fileUri = Uri.fromFile(file)
                    if (urisToDelete.contains(fileUri)) {
                        if (file.delete()) {
                            Log.d("mytag", "Deleted file: ${file.absolutePath}")
                        } else {
                            Log.d("mytag", "Failed to delete file: ${file.absolutePath}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("mytag", "Failed to delete file: ${e.message}")
        }
    }



}