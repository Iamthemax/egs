package com.sumagoinfotech.digicopy.ui.fragments.dashboard

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.gson.Gson
import com.google.mlkit.vision.barcode.common.Barcode
import com.permissionx.guolindev.PermissionX
import com.sumagoinfotech.digicopy.R
import com.sumagoinfotech.digicopy.databinding.FragmentDashboardBinding
import com.sumagoinfotech.digicopy.model.apis.DocumentDownloadModel
import com.sumagoinfotech.digicopy.model.apis.documetqrdownload.QRDocumentDownloadModel
import com.sumagoinfotech.digicopy.model.apis.mapmarker.MapData
import com.sumagoinfotech.digicopy.model.apis.mapmarker.MapMarkerModel
import com.sumagoinfotech.digicopy.model.apis.projectlist.ProjectDataFromLatLong
import com.sumagoinfotech.digicopy.model.apis.projectlist.ProjectsFromLatLongModel
import com.sumagoinfotech.digicopy.model.apis.projectlistformap.ProjectListModel
import com.sumagoinfotech.digicopy.model.apis.projectlistformap.ProjectMarkerData
import com.sumagoinfotech.digicopy.model.apis.projectlistmarker.LabourData
import com.sumagoinfotech.digicopy.model.apis.projectlistmarker.ProjectData
import com.sumagoinfotech.digicopy.model.apis.projectlistmarker.ProjectLabourListForMarker
import com.sumagoinfotech.digicopy.ui.activities.LabourListByProjectActivity
import com.sumagoinfotech.digicopy.ui.activities.registration.LabourRegistration1Activity
import com.sumagoinfotech.digicopy.ui.activities.ScanBarcodeActivity
import com.sumagoinfotech.digicopy.ui.activities.ScannerActivity
import com.sumagoinfotech.digicopy.ui.activities.ViewLabourFromMarkerClick
import com.sumagoinfotech.digicopy.utils.CustomInfoWindowAdapter
import com.sumagoinfotech.digicopy.utils.CustomMarkerObject
import com.sumagoinfotech.digicopy.utils.CustomProgressDialog
import com.sumagoinfotech.digicopy.utils.FileDownloader
import com.sumagoinfotech.digicopy.utils.MySharedPref
import com.sumagoinfotech.digicopy.webservice.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DashboardFragment : Fragment(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener,
    GoogleMap.OnInfoWindowClickListener, MapTypeBottomSheetDialogFragment.BottomSheetListener {

    private var _binding: FragmentDashboardBinding? = null
    private var markersList = mutableListOf<ProjectMarkerData>()
    private var labourData = mutableListOf<LabourData>()
    private var projectData = mutableListOf<ProjectDataFromLatLong>()
    private var mapMarkerData = mutableListOf<MapData>()
    lateinit var dialog: CustomProgressDialog
    private var latitude = ""
    private var longitude = ""

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private lateinit var map: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocationMarker: Marker? = null // Reference to the current location marker
    private var isCurrentMarkerVisible = true
    val bottomSheetDialogFragment = MapTypeBottomSheetDialogFragment()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val dashboardViewModel =
            ViewModelProvider(this).get(DashboardViewModel::class.java)
        dialog = CustomProgressDialog(requireContext())
        _binding = FragmentDashboardBinding.inflate(inflater, container, false)
        val root: View = binding.root
        try {
            binding.layoutRegisterLabour.setOnClickListener {
                val intent = Intent(activity, LabourRegistration1Activity::class.java)
                startActivity(intent)
            }
            binding.layoutScanQR.setOnClickListener {
                /*val intent= Intent(activity,ScanBarcodeActivity::class.java)
                startActivity(intent)*/
                if (ActivityCompat.checkSelfPermission(
                        requireActivity(),
                        android.Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    startScanner()
                } else {
                    requestThePermissions()
                }
            }
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
            if (!isLocationEnabled()) {
                // If not enabled, show dialog to enable it
                showEnableLocationDialog()
            } else {
                // Request location updates
                //showProgressDialog()
                requestLocationUpdates()
            }
            val mapFragment =
                childFragmentManager.findFragmentById(R.id.map_container) as SupportMapFragment
            mapFragment.getMapAsync(this)
            binding.layoutByProjectId.setOnClickListener {
                fetchProjectDataForMarkerWhenSearchByName(binding.etInput.text.toString())
                Log.d("mytag", binding.etInput.text.toString())
            }
            binding.layoutByLabourId.setOnClickListener {
                fetchLabourDataForMarkerById(binding.etInput.text.toString())
            }
            binding.ivMapType.setOnClickListener {

                bottomSheetDialogFragment.setBottomSheetListener(this@DashboardFragment)
                bottomSheetDialogFragment.show(childFragmentManager, bottomSheetDialogFragment.tag)
            }
        } catch (e: Exception) {
            Log.d("mytag", "Exception " + e.message)
        }
        return root
    }

    private fun requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                1000
            )
            return
        }

        // Request last known location
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)

                    // Update UI with latitude and longitude
                    // Note: getAddressFromLatLong() needs to be implemented to fetch address
                    // from latitude and longitude

                } ?: run {
                    // Handle case where location is null
                    Toast.makeText(
                        requireContext(),
                        "Unable to retrieve location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager =
            requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun startScanner() {
        ScannerActivity.startScanner(requireContext()) { barcodes ->
            barcodes.forEach { barcode ->
                when (barcode.valueType) {
                    Barcode.TYPE_URL -> {

                    }

                    Barcode.TYPE_CONTACT_INFO -> {

                    }

                    else -> {

                        getFileDownloadUrl(barcode.rawValue.toString())
                        Log.d("mytag", "" + barcode.rawValue.toString())

                    }
                }
            }
        }
    }

    private fun requestThePermissions() {

        PermissionX.init(requireActivity())
            .permissions(android.Manifest.permission.CAMERA)
            .onExplainRequestReason { scope, deniedList ->
                scope.showRequestReasonDialog(
                    deniedList,
                    "Core fundamental are based on these permissions",
                    "OK",
                    "Cancel"
                )
            }
            .onForwardToSettings { scope, deniedList ->
                scope.showForwardToSettingsDialog(
                    deniedList,
                    "You need to allow necessary permissions in Settings manually",
                    "OK",
                    "Cancel"
                )
            }
            .request { allGranted, grantedList, deniedList ->
                if (allGranted) {

                } else {
                }
            }
    }

    private fun fetchDataAndShowMarkers() {

        val apiService = ApiClient.create(requireContext())
        apiService.getProjectListForMap().enqueue(object : Callback<ProjectListModel> {
            override fun onResponse(
                call: Call<ProjectListModel>,
                response: Response<ProjectListModel>
            ) {

                if (response.isSuccessful) {
                    Log.d("mytag", "" + response.body()?.data?.get(0)?.latitude)
                    markersList = response.body()?.data as MutableList<ProjectMarkerData>
                    isCurrentMarkerVisible = false
                    addMarkersToMap()
                } else {
                }
            }

            override fun onFailure(call: Call<ProjectListModel>, t: Throwable) {
            }
        })
    }

    private fun fetchLabourDataForMarkerById(mgnregaId: String) {
        dialog.show()
        val apiService = ApiClient.create(requireContext())
        apiService.getLabourDataForMarkerById(mgnregaId)
            .enqueue(object : Callback<ProjectLabourListForMarker> {
                override fun onResponse(
                    call: Call<ProjectLabourListForMarker>,
                    response: Response<ProjectLabourListForMarker>
                ) {
                    dialog.dismiss()
                    if (response.isSuccessful) {
                        if (!response.body()?.status.isNullOrEmpty() && response.body()?.status.equals(
                                "true"
                            )
                        ) {
                            if (!response.body()?.labour_data.isNullOrEmpty()) {
                                Log.d("mytag", Gson().toJson(response.body()))
                                labourData = response.body()?.labour_data as MutableList<LabourData>
                                if (labourData.size > 0) {
                                    showLabourMarkers(labourData)
                                }
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "No records found",
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        } else {
                            Toast.makeText(requireContext(), "Please try again", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Response unsuccessful",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }

                }

                override fun onFailure(call: Call<ProjectLabourListForMarker>, t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        "Error Ocuured during api call",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            })
    }

    private fun showLabourMarkers(labourData: MutableList<LabourData>) {
        map.clear()
        val redVue = 0F
        try {
            if (labourData.isNotEmpty()) {
                labourData.forEach { marker ->
                    Log.d("mytag", "showProjectMarkers: ${marker.latitude},${marker.longitude}")
                    val position = LatLng(marker.latitude.toDouble(), marker.longitude.toDouble())
                    var myMarker: Marker? = null
                    val markerIcon = BitmapDescriptorFactory.defaultMarker(redVue)
                    //val markerIcon=getMarkerIcon(Color.GREEN)
                    myMarker = map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .icon(markerIcon)
                            .title(marker.full_name)
                    )
                    var customMarkerObject = CustomMarkerObject(
                        id = marker.id.toString(),
                        type = "labour",
                        name = marker.full_name,
                        url = ""
                    )
                    myMarker?.tag = customMarkerObject
                    myMarker?.showInfoWindow()
                }
                val firstMarker = projectData.last()
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            firstMarker.latitude.toDouble(),
                            firstMarker.longitude.toDouble()
                        ), 15f
                    )
                )
            } else {
                // Handle case when projectData is empty
                Log.d("mytag", "showLabourMarkers:  data is empty")
            }
        } catch (e: Exception) {
            Log.d("mytag", "showLabourMarkers: Exception " + e.message)
            e.printStackTrace()
        }

    }

    private fun fetchProjectDataForMarkerWhenSearchByName(projectName: String) {
        dialog.show()
        val apiService = ApiClient.create(requireContext())
        apiService.getProjectListForMarkerByNameSearch(projectName, latitude, longitude)
            .enqueue(object : Callback<ProjectsFromLatLongModel> {
                override fun onResponse(
                    call: Call<ProjectsFromLatLongModel>,
                    response: Response<ProjectsFromLatLongModel>
                ) {
                    dialog.dismiss()
                    if (response.isSuccessful) {
                        Log.d("mytag", Gson().toJson(response.body()))
                        if (!response.body()?.project_data.isNullOrEmpty()) {
                            projectData =
                                response.body()?.project_data as MutableList<ProjectDataFromLatLong>
                            if (projectData.size > 0) {
                                showProjectMarkersWhenSearchByName(projectData)
                            }
                        } else {
                            Toast.makeText(requireContext(), "No records found", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Response unsuccessful",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }
                }

                override fun onFailure(call: Call<ProjectsFromLatLongModel>, t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        "Error Ocuured during api call",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            })
    }

    private fun showProjectMarkersWhenSearchByName(projectData: MutableList<ProjectDataFromLatLong>) {
        map.clear()
        val greenHue = 120F
        try {
            if (projectData.isNotEmpty()) {
                projectData.forEach { marker ->
                    Log.d("mytag", "showProjectMarkers: ${marker.latitude},${marker.longitude}")
                    val position = LatLng(marker.latitude.toDouble(), marker.longitude.toDouble())
                    var myMarker: Marker? = null
                    val markerIcon = BitmapDescriptorFactory.defaultMarker(greenHue)
                    //val markerIcon=getMarkerIcon(Color.GREEN)
                    myMarker = map.addMarker(
                        MarkerOptions()
                            .position(position)
                            .icon(markerIcon)
                            .title(marker.project_name)
                    )
                    var customMarkerObject = CustomMarkerObject(
                        id = marker.id.toString(),
                        type = "project",
                        name = marker.project_name,
                        url = ""
                    )
                    myMarker?.tag = customMarkerObject
                    myMarker?.showInfoWindow()
                }
                val firstMarker = projectData.last()
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            firstMarker.latitude.toDouble(),
                            firstMarker.longitude.toDouble()
                        ), 15f
                    )
                )
            } else {
                // Handle case when projectData is empty
                Log.d("mytag", "showProjectMarkers: Project data is empty")
            }
        } catch (e: Exception) {
            Log.d("mytag", "showProjectMarkers: Exception " + e.message)
            e.printStackTrace()
        }
    }

    private fun showProjectMarkersNew(mapData: MutableList<MapData>) {
        map.clear()
        val redHue = 0F
        val orangeHue = 30F
        val yellowHue = 60F
        val greenHue = 120F
        val cyanHue = 180F
        val blueHue = 240F
        val purpleHue = 270F
        try {
            if (mapData.isNotEmpty()) {
                mapData.forEach { marker ->
                    Log.d("mytag", "showProjectMarkersNew: ${marker.latitude},${marker.longitude}")
                    val position = LatLng(marker.latitude.toDouble(), marker.longitude.toDouble())
                    var myMarker: Marker? = null
                    if (marker.type.equals("project")) {
                        val markerIcon = BitmapDescriptorFactory.defaultMarker(greenHue)
                        //val markerIcon=getMarkerIcon(Color.GREEN)
                        myMarker = map.addMarker(
                            MarkerOptions()
                                .position(position)
                                .icon(markerIcon)
                                .title(marker.name)
                        )
                        var customMarkerObject = CustomMarkerObject(
                            id = marker.id.toString(),
                            type = marker.type,
                            name = marker.name,
                            url = ""
                        )
                        myMarker?.tag = customMarkerObject
                        myMarker?.showInfoWindow()
                    } else if (marker.type.equals("labour")) {
                        //val markerIcon=getMarkerIcon(Color.RED)
                        val markerIcon = BitmapDescriptorFactory.defaultMarker(redHue)
                        myMarker = map.addMarker(
                            MarkerOptions()
                                .icon(markerIcon)
                                .position(position)
                                .title(marker.name)
                        )
                        var customMarkerObject = CustomMarkerObject(
                            id = marker.id.toString(),
                            type = marker.type,
                            name = marker.name,
                            url = ""
                        )
                        myMarker?.tag = customMarkerObject
                        myMarker?.showInfoWindow()
                    } else if (marker.type.equals("document")) {
                        val markerIcon = BitmapDescriptorFactory.defaultMarker(purpleHue)
                        myMarker = map.addMarker(
                            MarkerOptions()
                                .icon(markerIcon)
                                .position(position)
                                .title(marker.document_name)
                        )
                        var customMarkerObject = CustomMarkerObject(
                            id = marker.id.toString(),
                            type = marker.type,
                            name = marker.document_name,
                            url = marker.document_pdf
                        )
                        myMarker?.tag = customMarkerObject
                        myMarker?.showInfoWindow()
                    }
                }
                // Move camera to the first marker
                val firstMarker = mapData.last()
                map.moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(
                            firstMarker.latitude.toDouble(),
                            firstMarker.longitude.toDouble()
                        ), 15f
                    )
                )
            } else {
                // Handle case when projectData is empty
                Log.d("mytag", "showProjectMarkers: Project data is empty")
            }
        } catch (e: Exception) {
            Log.d("mytag", "showProjectMarkers: Exception " + e.message)
            e.printStackTrace()
        }
    }

    fun getMarkerIcon(color: Int): BitmapDescriptor {
        val drawable: Drawable? =
            ContextCompat.getDrawable(requireContext(), R.drawable.ic_location)
        drawable?.setTint(color)
        val canvas = Canvas()
        val bitmap: Bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showEnableLocationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setMessage("Location services are disabled. Do you want to enable them?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->
                dialog.dismiss()
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
                // Handle the case when the user refuses to enable location services
                Toast.makeText(
                    requireContext(),
                    "Unable to retrieve location without enabling location services",
                    Toast.LENGTH_SHORT
                ).show()
            }
        val alert = builder.create()
        alert.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.setOnMarkerClickListener(this)
        map.setOnInfoWindowClickListener(this)
        // Check location permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Enable current location button
            map.isMyLocationEnabled = true

            // Get current location
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    location?.let {
                        val currentLatLng = LatLng(it.latitude, it.longitude)


                        var pref = MySharedPref(requireContext())
                        pref.setLatitude(it.latitude.toString())
                        pref.setLongitude(it.longitude.toString())
                        // fetchProjectDataFromLatLong(it.latitude.toString(),it.longitude.toString())
                        fetchProjectDataFromLatLongNew(
                            it.latitude.toString(),
                            it.longitude.toString()
                        )

                        // Move camera to current location
                        map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                        // Add marker for current location
                        if (currentLocationMarker == null) {
                            val markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)
                            // If marker doesn't exist, create a new one
                            currentLocationMarker = map.addMarker(
                                MarkerOptions()
                                    .position(currentLatLng)
                                    .title("You are here")
                                    .icon(markerIcon)
                                    .snippet("${it.latitude}, ${it.longitude}")
                            )
                            latitude = it.latitude.toString()
                            longitude = it.longitude.toString()
                            currentLocationMarker?.showInfoWindow()
                        } else {
                            // If marker already exists, update its position
                            currentLocationMarker?.position = currentLatLng
                        }
                    } ?: run {
                        Toast.makeText(
                            requireContext(),
                            "Unable to retrieve location",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_LOCATION_PERMISSION
            )
        }
        map.setInfoWindowAdapter(CustomInfoWindowAdapter(layoutInflater))
    }

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 1
    }

    public fun updateMarker() {
        if (isAdded && ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireActivity().applicationContext,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    val currentLatLng = LatLng(it.latitude, it.longitude)

                    // Move camera to current location
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                    // Add marker for current location
                    if (currentLocationMarker == null) {
                        val markerIcon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)

                        // If marker doesn't exist, create a new one
                        currentLocationMarker = map.addMarker(
                            MarkerOptions()
                                .position(currentLatLng)
                                .title("You are here")
                                .icon(markerIcon)
                                .snippet("${it.latitude}, ${it.longitude}")
                        )
                        currentLocationMarker?.showInfoWindow()
                    } else {
                        // If marker already exists, update its position
                        currentLocationMarker?.position = currentLatLng
                    }
                } ?: run {
                    Toast.makeText(
                        requireContext(),
                        "Unable to retrieve location",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun addMarkersToMap() {
        //val icon = bitmapDescriptorFromVector( R.drawable.ic_marker, R.color.appBlue)
        markersList.forEach { marker ->
            val position = LatLng(marker.latitude.toDouble(), marker.longitude.toDouble())
            val myMarker = map.addMarker(
                MarkerOptions()
                    .position(position)
                    .title(marker.project_name).snippet(marker.district)
            )

            myMarker?.showInfoWindow()
        }
        // Move camera to the first marker
        map.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(
                    markersList[0].latitude.toDouble(),
                    markersList[0].longitude.toDouble()
                ), 15f
            )
        )
    }

    override fun onMarkerClick(marker: Marker): Boolean {
        return false
    }

    private fun bitmapDescriptorFromVector(
        drawableId: Int,
        colorId: Int
    ): BitmapDescriptor {
        val drawable = ContextCompat.getDrawable(requireContext(), drawableId)
        drawable?.setTint(ContextCompat.getColor(requireContext(), colorId))
        val bitmap = Bitmap.createBitmap(
            drawable!!.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onInfoWindowClick(marker: Marker) {

        val customMarkerObject = marker.tag as CustomMarkerObject
        if (customMarkerObject.type.equals("project")) {
            val intent = Intent(context, LabourListByProjectActivity::class.java)
            intent.putExtra("id", "" + customMarkerObject.id)
            context?.startActivity(intent)
        } else if (customMarkerObject.type.equals("document")) {

            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.parse(customMarkerObject.url), "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            try {
                startActivity(intent)
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(
                    requireContext(),
                    "No PDF viewer application found",
                    Toast.LENGTH_SHORT
                ).show()
            }

        } else if (customMarkerObject.type.equals("labour")) {
            val intent = Intent(context, ViewLabourFromMarkerClick::class.java)
            intent.putExtra("id", "" + customMarkerObject.id)
            context?.startActivity(intent)
        }
        //Toast.makeText(requireContext(), "${marker.tag}Info window clicked: ${marker.title}", Toast.LENGTH_SHORT).show()
    }


    private fun fetchProjectDataFromLatLongNew(latitude: String, longitude: String) {
        dialog.show()
        val apiService = ApiClient.create(requireContext())
        apiService.getMapsMarkersFromLatLong(latitude, longitude)
            .enqueue(object : Callback<MapMarkerModel> {
                override fun onResponse(
                    call: Call<MapMarkerModel>,
                    response: Response<MapMarkerModel>
                ) {
                    dialog.dismiss()
                    if (response.isSuccessful) {
                        Log.d("mytag", Gson().toJson(response.body()))
                        if (!response.body()?.map_data.isNullOrEmpty()) {
                            mapMarkerData = response.body()?.map_data as MutableList<MapData>
                            if (mapMarkerData.size > 0) {
                                showProjectMarkersNew(mapMarkerData)
                            }
                        } else {
                            Toast.makeText(requireContext(), "No records found", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Response unsuccessful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onFailure(call: Call<MapMarkerModel>, t: Throwable) {
                    Toast.makeText(
                        requireContext(),
                        "Error Occurred during api call",
                        Toast.LENGTH_SHORT
                    ).show()
                    dialog.dismiss()
                }
            })
    }

    private fun getFileDownloadUrl(fileName: String) {


        val dialog = CustomProgressDialog(requireContext())
        dialog.show()
        val apiService = ApiClient.create(requireContext())
        val call = apiService.downloadPDF(fileName)
        call.enqueue(object : Callback<QRDocumentDownloadModel> {
            override fun onResponse(
                call: Call<QRDocumentDownloadModel>,
                response: Response<QRDocumentDownloadModel>
            ) {
                dialog.dismiss()
                if (response.isSuccessful) {

                    if (response.body()?.status.equals("true")) {
                        val url = response.body()?.data?.document_pdf.toString()
                        Log.d("mytag", url)

                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.setDataAndType(Uri.parse(url), "application/pdf")
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        try {
                            startActivity(intent)
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(
                                requireContext(),
                                "No PDF viewer application found",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        //FileDownloader.downloadFile(requireContext(), url, fileName)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            response.body()?.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    Toast.makeText(requireContext(), "response unsuccessful", Toast.LENGTH_SHORT)
                        .show()
                }
            }

            override fun onFailure(call: Call<QRDocumentDownloadModel>, t: Throwable) {
                dialog.dismiss()
                Toast.makeText(requireContext(), "response failed", Toast.LENGTH_SHORT).show()
            }
        })

    }

    override fun onDataReceived(data: String) {
        bottomSheetDialogFragment.dismiss()
        if (data.equals("normal")) {
            map.mapType = GoogleMap.MAP_TYPE_NORMAL
        } else if (data.equals("hybrid")) {
            map.mapType = GoogleMap.MAP_TYPE_HYBRID
        } else if (data.equals("satellite")) {
            map.mapType = GoogleMap.MAP_TYPE_SATELLITE
        }

    }
}