package com.sumagoinfotech.digicopy.ui.gramsevak.dashboard.sync

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.sumagoinfotech.digicopy.database.AppDatabase
import com.sumagoinfotech.digicopy.database.dao.DocumentDao
import com.sumagoinfotech.digicopy.database.dao.LabourDao
import com.sumagoinfotech.digicopy.databinding.FragmentSyncOfflineDataBinding
import com.sumagoinfotech.digicopy.ui.gramsevak.SyncLabourDataActivity
import com.sumagoinfotech.digicopy.ui.gramsevak.SyncLandDocumentsActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SyncOfflineData.newInstance] factory method to
 * create an instance of this fragment.
 */
class SyncOfflineData : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    private var _binding: FragmentSyncOfflineDataBinding? = null

    lateinit var appDatabase: AppDatabase
    lateinit var labourDao: LabourDao
    lateinit var documentDao:DocumentDao

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

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
        _binding = FragmentSyncOfflineDataBinding.inflate(inflater, container, false)
        val root: View = binding.root
        binding.layoutOfflineDocuments.setOnClickListener {
            val intent=Intent(activity, SyncLandDocumentsActivity::class.java)
            startActivity(intent)
        }
        binding.layoutLabourRegistrationsOffline.setOnClickListener {
            val intent=Intent(activity, SyncLabourDataActivity::class.java)
            startActivity(intent)
        }
        appDatabase=AppDatabase.getDatabase(requireActivity().applicationContext)
        labourDao=appDatabase.labourDao()
        documentDao=appDatabase.documentDao()

        CoroutineScope(Dispatchers.IO).launch{
            val labourCount=labourDao.getLaboursCount();
            val documentCount=documentDao.getDocumentsCount();
            withContext(Dispatchers.Main) {
                binding.tvRegistrationCount.setText("${labourCount}")
                binding.tvDocumentsCount.setText("${documentCount}")
            }
        }

//        val toggleGroup: MaterialButtonToggleGroup = findViewById(R.id.toggleGroup)
//        val button1: MaterialButton = bfindViewById(R.id.button1)
//        val button2: MaterialButton = findViewById(R.id.button2)
//
//        // Select the first button by default
//        toggleGroup.check(button1.id)
//
//        // Set listener for button selection change
//        toggleGroup.addOnButtonCheckedListener { group, checkedId, isChecked ->
//            if (isChecked) {
//                // Handle button selection
//                when (checkedId) {
//                    R.id.button1 -> {
//                        // Button 1 selected
//                    }
//                    R.id.button2 -> {
//                        // Button 2 selected
//                    }
//                    // Add more cases for other buttons if needed
//                }
//            }
//        }
        return root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment SyncOfflineData.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            SyncOfflineData().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onResume() {
        super.onResume()
        Log.d("mytag","OnResume Fragment")
        CoroutineScope(Dispatchers.IO).launch{
            val labourCount=labourDao.getLaboursCount();
            val documentCount=documentDao.getDocumentsCount();
            withContext(Dispatchers.Main) {
                binding.tvRegistrationCount.setText("${labourCount}")
                binding.tvDocumentsCount.setText("${documentCount}")
            }
        }
    }

}