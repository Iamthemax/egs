package com.sumagoinfotech.digicopy.ui.activities.officer.ui.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.sumagoinfotech.digicopy.R
import com.sumagoinfotech.digicopy.adapters.OfficerUploadedDocsAdapter
import com.sumagoinfotech.digicopy.adapters.ViewAttendanceAdapter
import com.sumagoinfotech.digicopy.database.AppDatabase
import com.sumagoinfotech.digicopy.database.dao.AreaDao
import com.sumagoinfotech.digicopy.database.entity.AreaItem
import com.sumagoinfotech.digicopy.databinding.FragmentOfficerUploadedDocumentsBinding
import com.sumagoinfotech.digicopy.model.apis.attendance.AttendanceData
import com.sumagoinfotech.digicopy.model.apis.attendance.AttendanceModel
import com.sumagoinfotech.digicopy.model.apis.uploadeddocs.UploadedDocsModel
import com.sumagoinfotech.digicopy.model.apis.uploadeddocs.UploadedDocument
import com.sumagoinfotech.digicopy.utils.CustomProgressDialog
import com.sumagoinfotech.digicopy.utils.MySharedPref
import com.sumagoinfotech.digicopy.webservice.ApiClient
import com.sumagoinfotech.digicopy.webservice.ApiService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [OfficerUploadedDocumentsFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class OfficerUploadedDocumentsFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private lateinit var binding:FragmentOfficerUploadedDocumentsBinding
    private lateinit var apiService: ApiService
    private lateinit var dialog: CustomProgressDialog
    private lateinit var adapter: OfficerUploadedDocsAdapter
    private  var listDocuments=ArrayList<UploadedDocument>()
    private  var isInternetAvailable=false
    private lateinit var talukaList:List<AreaItem>
    private lateinit var villageList:List<AreaItem>
    private var villageNames= mutableListOf<String>()
    private var talukaNames= mutableListOf<String>()
    private var talukaId=""
    private var villageId=""
    private var startDate=""
    private var endDate=""
    private lateinit var mySharedPref: MySharedPref
    private lateinit var appDatabase: AppDatabase
    private lateinit var areaDao: AreaDao
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding= FragmentOfficerUploadedDocumentsBinding.inflate(layoutInflater, container, false)
        try {
            appDatabase=AppDatabase.getDatabase(requireActivity())
            areaDao=appDatabase.areaDao()
            mySharedPref= MySharedPref(requireContext())
            CoroutineScope(Dispatchers.IO).launch {
                talukaList=areaDao.getAllTalukas(mySharedPref.getOfficerDistrictId()!!)

                withContext(Dispatchers.Main){
                    for (taluka in talukaList)
                    {
                        talukaNames.add(taluka.name)
                    }
                    Log.d("mytag",""+talukaNames.size);
                    val talukaAdapter = ArrayAdapter(
                        requireActivity(), android.R.layout.simple_list_item_1, talukaNames
                    )
                    binding.actSelectTaluka.setAdapter(talukaAdapter)
                    binding.actSelectTaluka.setOnFocusChangeListener { abaad, asd ->
                        binding.actSelectTaluka.showDropDown()
                    }
                    binding.actSelectTaluka.setOnClickListener {
                        binding.actSelectTaluka.showDropDown()
                    }
                }
            }
            binding.recyclerView.layoutManager=GridLayoutManager(requireContext(),3,RecyclerView.VERTICAL,false)
            apiService=ApiClient.create(requireContext())
            dialog= CustomProgressDialog(requireContext())

            binding.actSelectTaluka.setOnItemClickListener { parent, view, position, id ->
                CoroutineScope(Dispatchers.IO).launch {
                    villageList=areaDao.getVillageByTaluka(talukaList[position].location_id)
                    withContext(Dispatchers.Main){
                        talukaId=talukaList[position].location_id
                        villageId=""
                        villageNames.clear();
                        binding.actSelectVillage.setText("")
                        for (village in villageList){
                            villageNames.add(village.name)
                        }
                        val villageAdapter = ArrayAdapter(
                            requireActivity(), android.R.layout.simple_list_item_1, villageNames
                        )
                        Log.d("mytag",""+villageNames.size)
                        binding.actSelectVillage.setAdapter(villageAdapter)
                        binding.actSelectVillage.setOnFocusChangeListener { abaad, asd ->
                            binding.actSelectVillage.showDropDown()
                        }
                        binding.actSelectVillage.setOnClickListener {
                            binding.actSelectVillage.showDropDown()
                        }
                        binding.actSelectVillage.setOnItemClickListener { parent, view, position, id ->
                            villageId=villageList[position].location_id
                            getDocumentsList()
                            Log.d("mytag","Select Village"+villageList[position].location_id);
                        }

                        getDocumentsList()
                    }
                }
            }
            binding.btnCloseTaluka.setOnClickListener {

                binding.actSelectTaluka.setText("")
                talukaId=""
                getDocumentsList()

            }
            binding.btnCloseVillage.setOnClickListener {
                binding.actSelectVillage.setText("")
                villageId=""
                getDocumentsList()
            }

            binding.layoutStartDate.setOnClickListener {
                showDatePicker(requireContext(),binding.etStartDate)
                endDate=binding.etStartDate.getText().toString()
            }
            binding.layoutEndDate.setOnClickListener {
                showDatePicker(requireContext(),binding.etEndDate)
                endDate=binding.etEndDate.getText().toString()
            }
            binding.layoutClearAll.setOnClickListener {

                binding.actSelectTaluka.setText("")
                binding.actSelectVillage.setText("")
                binding.etStartDate.setText("")
                binding.etEndDate.setText("")
                talukaId=""
                villageId=""
                startDate=""
                endDate=""
                getDocumentsList()
            }
            binding.actSelectVillage.setOnItemClickListener { parent, view, position, id ->
                villageId=villageList[position].location_id
            }
            binding.btnSearch.setOnClickListener {
                getDocumentsList()
            }
            getDocumentsList();
            addTextWatcher()
            return binding.root
        } catch (e: Exception) {

        }
        return binding.root
    }

    private fun addTextWatcher() {
        binding.actSelectTaluka.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {

                val length = s?.length ?: 0
                val text = s?.toString() ?: ""
                if (length > 0) {
                    binding.btnCloseTaluka.visibility = View.VISIBLE
                } else {
                    binding.btnCloseTaluka.visibility = View.GONE
                }
            }
        })
        binding.actSelectVillage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) {

            }
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }
            override fun afterTextChanged(s: Editable?) {

                val length = s?.length ?: 0
                val text = s?.toString() ?: ""
                if (length > 0) {
                    binding.btnCloseVillage.visibility = View.VISIBLE
                } else {
                    binding.btnCloseVillage.visibility = View.GONE
                }
            }
        })
    }
    private fun showDatePicker(context: Context, editText: TextView) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val datePickerDialog = DatePickerDialog(
            context, { view, year, monthOfYear, dayOfMonth ->
                val selectedDate = formatDate(dayOfMonth, monthOfYear, year)
                editText.setText(selectedDate)
            }, year, month, day
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
        datePickerDialog.show()
    }
    private fun formatDate(day: Int, month: Int, year: Int): String {
        val calendar = Calendar.getInstance()
        calendar.set(year, month, day)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return dateFormat.format(calendar.time)
    }

    private fun getDocumentsList() {
        try {
            dialog.show()
            startDate=binding.etStartDate.getText().toString()
            endDate=binding.etEndDate.getText().toString()
            val call=apiService.getDocumentsListForOfficer(talukaId=talukaId,villageId=villageId, from_date = startDate, to_date = endDate);
            call.enqueue(object : Callback<UploadedDocsModel> {
                override fun onResponse(
                    call: Call<UploadedDocsModel>,
                    response: Response<UploadedDocsModel>
                ) {
                    dialog.dismiss()
                    if(response.isSuccessful)
                    {
                        listDocuments.clear()
                        if(response.body()?.status.equals("true")){
                            listDocuments= (response.body()?.data as ArrayList<UploadedDocument>?)!!
                            adapter= OfficerUploadedDocsAdapter(listDocuments)
                            binding.recyclerView.adapter=adapter
                            adapter.notifyDataSetChanged()
                        }
                    }else{
                        Toast.makeText(requireActivity(), "Error Occurred during api call", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<UploadedDocsModel>, t: Throwable) {
                    Toast.makeText(requireActivity(), "Error Occurred during api call", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            })
        }catch (e:Exception){

        }
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            OfficerUploadedDocumentsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}