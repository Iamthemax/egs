package com.sipl.egs.ui.gramsevak
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import com.sipl.egs.R
import com.sipl.egs.adapters.FamilyDetailsListOnlineAdapter
import com.sipl.egs.adapters.RegistrationStatusHistoryAdapter
import com.sipl.egs.databinding.ActivityViewLabourDetailsNotApprovedBinding
import com.sipl.egs.model.apis.getlabour.HistoryDetailsItem
import com.sipl.egs.model.apis.getlabour.LabourByMgnregaId
import com.sipl.egs.ui.registration.LabourUpdateOnline1Activity
import com.sipl.egs.utils.CustomProgressDialog
import com.sipl.egs.webservice.ApiClient
import io.getstream.photoview.PhotoView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class ViewNotApprovedLabourDetailsActivity : AppCompatActivity() {
    private lateinit var binding:ActivityViewLabourDetailsNotApprovedBinding
    private var voterIdImage=""
    private var mgnregaIdImage=""
    private var photo=""
    private var aadharImage=""
    private lateinit var dialog:CustomProgressDialog
    private var historyList=ArrayList<HistoryDetailsItem>()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding=ActivityViewLabourDetailsNotApprovedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title=resources.getString(R.string.labour_details)
        binding.recyclerViewFamilyDetails.layoutManager=
            LinearLayoutManager(this, RecyclerView.VERTICAL,false)
        val mgnregaCardId=intent.getStringExtra("id")
        dialog= CustomProgressDialog(this)
        getLabourDetails(mgnregaCardId!!)
        var adapter= RegistrationStatusHistoryAdapter(historyList)
        binding.recyclerViewHistory.layoutManager=LinearLayoutManager(this,RecyclerView.VERTICAL,false)
        binding.recyclerViewHistory.adapter=adapter
        adapter.notifyDataSetChanged()
        binding.ivAadhar.setOnClickListener {
            showPhotoZoomDialog(aadharImage)
        }
        binding.ivPhoto.setOnClickListener {
            showPhotoZoomDialog(photo)
        }
        binding.ivMnregaCard.setOnClickListener {
            showPhotoZoomDialog(mgnregaIdImage)
        }
        binding.ivVoterId.setOnClickListener {
            showPhotoZoomDialog(voterIdImage)
        }
        binding.fabEdit.setOnClickListener {

            val intent= Intent(this, LabourUpdateOnline1Activity::class.java)
            intent.putExtra("id",mgnregaCardId)
            startActivity(intent);
        }
    }
    private fun getLabourDetails(mgnregaCardId:String) {


        try {
            dialog.show()
            val apiService= ApiClient.create(this@ViewNotApprovedLabourDetailsActivity)
            apiService.getLabourDetailsById(mgnregaCardId).enqueue(object :
                Callback<LabourByMgnregaId> {
                override fun onResponse(
                    call: Call<LabourByMgnregaId>,
                    response: Response<LabourByMgnregaId>
                ) {
                    dialog.dismiss()
                    if(response.isSuccessful){
                        if(!response.body()?.data.isNullOrEmpty()) {
                            val list=response.body()?.data
                            Log.d("mytag",""+ Gson().toJson(response.body()));
                            binding.tvFullName.text=list?.get(0)?.full_name
                            binding.tvGender.text=list?.get(0)?.gender_name
                            binding.tvDistritct.text=list?.get(0)?.district_name
                            binding.tvTaluka.text=list?.get(0)?.taluka_name
                            binding.tvVillage.text=list?.get(0)?.village_name
                            binding.tvMobile.text=list?.get(0)?.mobile_number
                            binding.tvLandline.text=list?.get(0)?.landline_number
                            binding.tvMnregaId.text=list?.get(0)?.mgnrega_card_id
                            binding.tvDob.text=list?.get(0)?.date_of_birth
                            binding.tvRegistrationStatus.text=list?.get(0)?.status_name
                            binding.tvSkills.text=list?.get(0)?.skills
                            if(!list?.get(0)?.other_remark.isNullOrEmpty()){
                                if(!list?.get(0)?.other_remark.equals("null")){

                                    binding.tvRemarks.text=list?.get(0)?.other_remark
                                    binding.tvRemarks.visibility=View.VISIBLE
                                    binding.tvLabelRemarks.visibility=View.VISIBLE
                          }else{
                                    binding.tvRemarks.text=list?.get(0)?.other_remark
                                    binding.tvRemarks.visibility=View.GONE
                                    binding.tvLabelRemarks.visibility=View.GONE
                                }
                            }
                            if(!list?.get(0)?.status_name.isNullOrEmpty()){
                                binding.tvRegistrationStatus.text=list?.get(0)?.status_name
                                if(list?.get(0)?.status_name.equals("Approved")){
                                    binding.tvReason.visibility=View.GONE
                                    binding.tvLabelReason.visibility=View.GONE
                                    binding.tvRemarks.visibility=View.GONE
                                    binding.tvLabelRemarks.visibility=View.GONE
                                }
                            }
                            if(!list?.get(0)?.reason_name.isNullOrEmpty()){
                                if(!list?.get(0)?.reason_name.equals("null")){
                                    binding.tvReason.text=list?.get(0)?.reason_name
                                    binding.tvReason.visibility=View.VISIBLE
                                    binding.tvLabelReason.visibility=View.VISIBLE
                                }else{
                                    binding.tvReason.visibility=View.GONE
                                    binding.tvLabelReason.visibility=View.GONE
                                }
                            }else{
                                binding.tvReason.visibility=View.GONE
                                binding.tvLabelReason.visibility=View.GONE
                            }
                            if(!list?.get(0)?.history_details.isNullOrEmpty())
                            {
                                Log.d("mytag","----->>>>")
                                binding.recyclerViewHistory.visibility=View.VISIBLE
                                binding.tvHistory.visibility=View.VISIBLE
                                historyList= list?.get(0)?.history_details as ArrayList<HistoryDetailsItem>
                                var adapter = RegistrationStatusHistoryAdapter(historyList)
                                binding.recyclerViewHistory.adapter=adapter
                                adapter.notifyDataSetChanged()
                                Log.d("mytag","----->>>>"+historyList.size)
                            }else{
                               // binding.tvHistory.visibility=View.GONE
                               // binding.recyclerViewHistory.visibility=View.GONE

                            }
                            photo= list?.get(0)?.profile_image.toString()
                            mgnregaIdImage= list?.get(0)?.mgnrega_image.toString()
                            aadharImage= list?.get(0)?.aadhar_image.toString()
                            voterIdImage= list?.get(0)?.voter_image.toString()
                            Glide.with(this@ViewNotApprovedLabourDetailsActivity).load(mgnregaIdImage).override(200,200).into(binding.ivMnregaCard)
                            Glide.with(this@ViewNotApprovedLabourDetailsActivity).load(photo).override(200,200).into(binding.ivPhoto)
                            Glide.with(this@ViewNotApprovedLabourDetailsActivity).load(aadharImage).override(200,200).into(binding.ivAadhar)
                            Glide.with(this@ViewNotApprovedLabourDetailsActivity).load(voterIdImage).override(200,200).into(binding.ivVoterId)
                            val familyList=response.body()?.data?.get(0)?.family_details
                            Log.d("mytag",""+familyList?.size);
                            var adapterFamily= FamilyDetailsListOnlineAdapter(familyList)
                            binding.recyclerViewFamilyDetails.adapter=adapterFamily
                            adapterFamily.notifyDataSetChanged()
                        }else {
                            Toast.makeText(this@ViewNotApprovedLabourDetailsActivity, "No records found", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else{
                        Toast.makeText(this@ViewNotApprovedLabourDetailsActivity, "Response unsuccessful", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<LabourByMgnregaId>, t: Throwable) {
                    dialog.dismiss()
                    Toast.makeText(this@ViewNotApprovedLabourDetailsActivity, "Error Ocuured during api call", Toast.LENGTH_SHORT).show()
                }
            })
        } catch (e: Exception) {
            dialog.dismiss()
            e.printStackTrace()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if(item.itemId==android.R.id.home){
            finish()
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showPhotoZoomDialog(uri:String){

        try {
            val dialog= Dialog(this@ViewNotApprovedLabourDetailsActivity)
            dialog.setContentView(R.layout.layout_zoom_image)
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            dialog.window?.setLayout(width, height)
            dialog.show()
            val photoView=dialog.findViewById<PhotoView>(R.id.photoView)
            val ivClose=dialog.findViewById<ImageView>(R.id.ivClose)
            Glide.with(this@ViewNotApprovedLabourDetailsActivity)
                .load(uri)
                .into(photoView)

            ivClose.setOnClickListener {
                dialog.dismiss()
            }
        } catch (e: Exception) {
        }
    }

}