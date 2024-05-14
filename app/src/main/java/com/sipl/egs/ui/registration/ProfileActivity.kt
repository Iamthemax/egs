package com.sipl.egs.ui.registration

import android.content.DialogInterface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.github.pwittchen.reactivenetwork.library.rx2.Connectivity
import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.sipl.egs.R
import com.sipl.egs.databinding.ActivityGramsevakProfileBinding
import com.sipl.egs.model.apis.labourlist.LabourListModel
import com.sipl.egs.utils.CustomProgressDialog
import com.sipl.egs.utils.MySharedPref
import com.sipl.egs.utils.MyValidator
import com.sipl.egs.utils.NoInternetDialog
import com.sipl.egs.webservice.ApiClient
import com.sipl.egs.webservice.ApiService
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.Response

class ProfileActivity : AppCompatActivity() {
    private lateinit var binding:ActivityGramsevakProfileBinding
    private lateinit var progressDialog: CustomProgressDialog
    private lateinit var mySharedPref: MySharedPref
    private var isInternetAvailable=false
    private lateinit var noInternetDialog: NoInternetDialog
    private lateinit var apiService: ApiService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityGramsevakProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title=resources.getString(R.string.profile)
        progressDialog= CustomProgressDialog(this)
        mySharedPref= MySharedPref(this)
        noInternetDialog= NoInternetDialog(this)
        apiService=ApiClient.create(this)
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

        binding.btnSubmit.setOnClickListener {

            if(isInternetAvailable){

                if(validateFields())
                {
                    if(MyValidator.isValidConfirmPassword(binding.etPasswordNewReEnter.getText().toString().trim(),binding.etPasswordNew.getText().toString().trim())){

                        binding.txLayoutPasswordNewReEnter.setError(null)
                        binding.txLayoutPasswordNew.setError(null)
                        Log.d("mytag","validteFields")
                        if(MyValidator.isValidPasswordPattern(binding.etPasswordNew.text.toString().trim()))
                        {
                            changePassword(oldPassword = binding.etPasswordOld.text.toString().trim(), newPassword = binding.etPasswordNew.text.toString().trim(), id = mySharedPref.getId(), roleId = mySharedPref.getRoleId())
                        }else{
                            showPasswordExplainDialog()
                        }



                    }else{
                        binding.txLayoutPasswordNewReEnter.setError(resources.getString(R.string.both_password_did_not_match))
                        binding.txLayoutPasswordNew.setError(resources.getString(R.string.both_password_did_not_match))

                    }




                }else{

                    Toast.makeText(this,resources.getString(R.string.please_enter_all_details),Toast.LENGTH_LONG).show()
                }
            }else{
                Toast.makeText(this,resources.getString(R.string.internet_is_not_available_please_check),Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showPasswordExplainDialog() {

        val builder=AlertDialog.Builder(this@ProfileActivity)
            .setTitle("Invalid Password")
            .setMessage(resources.getString(R.string.password_text))
            .setPositiveButton(resources.getString(R.string.okay),object :DialogInterface.OnClickListener{
                override fun onClick(dialog: DialogInterface?, which: Int) {

                }
            })
        val dialog=builder.create()
        dialog.show()
    }

    private fun changePassword(oldPassword: String, newPassword: String, id: Int, roleId: Int) {


        progressDialog.show()
        val call=apiService.changePassword(
            oldPassword = oldPassword,
            newPassword = newPassword)

        call.enqueue(object : retrofit2.Callback<LabourListModel> {
            override fun onResponse(
                call: Call<LabourListModel>,
                response: Response<LabourListModel>
            ) {
                progressDialog.dismiss()
                if(response.isSuccessful)
                {
                    if(response.body()?.status.equals("true")){
                        val toast= Toast.makeText(this@ProfileActivity,
                            response.body()?.message,
                            Toast.LENGTH_LONG)
                        toast.show()
                        runOnUiThread {
                            progressDialog.dismiss()
                            binding.etPasswordNew.text?.clear()
                            binding.etPasswordOld.text?.clear()
                            binding.etPasswordNewReEnter.text?.clear()

                        }
                    }else{
                        runOnUiThread {
                            progressDialog.dismiss()
                            val toast= Toast.makeText(this@ProfileActivity,
                                response.body()?.message,
                                Toast.LENGTH_LONG)
                            toast.show()
                        }
                    }

                }else{
                    runOnUiThread {
                        progressDialog.dismiss()
                        val toast= Toast.makeText(this@ProfileActivity,
                            resources.getString(R.string.response_unsuccessfull),
                            Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
            }
            override fun onFailure(call: Call<LabourListModel>, t: Throwable) {
                runOnUiThread {
                    progressDialog.dismiss()
                    val toast= Toast.makeText(this@ProfileActivity,
                        getString(R.string.error_while_login),
                        Toast.LENGTH_LONG)
                    toast.show()
                }
            }
        })
    }

    fun  validateFields():Boolean
    {
        var list= mutableListOf<Boolean>()

        if(MyValidator.isValidPasswordOld(binding.etPasswordOld.getText().toString().trim())){

            binding.txLayoutPasswordOld.setError(null)
            list.add(true)
        }else{
            binding.txLayoutPasswordOld.setError(resources.getString(R.string.enter_valid_old_password))
            list.add(false)
        }
        if(MyValidator.isValidPassword(binding.etPasswordNew.getText().toString().trim())){

            binding.txLayoutPasswordNew.setError(null)
            list.add(true)
        }else{
            binding.txLayoutPasswordNew.setError(resources.getString(R.string.enter_valid_8_digit_new_password))
            list.add(false)
        }
        if(MyValidator.isValidPassword(binding.etPasswordNewReEnter.getText().toString().trim())){

            binding.txLayoutPasswordNewReEnter.setError(null)
            list.add(true)
        }else{
            binding.txLayoutPasswordNewReEnter.setError(resources.getString(R.string.enter_valid_8_digit_new_password))
            list.add(false)
        }
        return !list.contains(false)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if(item.itemId== android.R.id.home){
            finish()
        }
        return super.onOptionsItemSelected(item)
    }
}