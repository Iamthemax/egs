package com.sumagoinfotech.digicopy.webservice

import android.content.Context
import android.content.Intent
import android.util.Log
import com.sumagoinfotech.digicopy.ui.activities.start.LoginActivity
import com.sumagoinfotech.digicopy.utils.MySharedPref
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody

class AuthInterceptor(private val context: Context) : Interceptor {
    override fun  intercept(chain: Interceptor.Chain): Response {
        try {
            val token = getTokenFromSharedPreferences()
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Accept", "application/json")
                .build()
            val response = chain.proceed(request)

            if (response.code == 401) {
                // Clear token or perform other actions for unauthorized access
                handleUnauthorizedAccess(context)
            }

            return response
        } catch (e: Exception) {
            Log.d("mytag","intercept "+e.message)
            return Response.Builder()
                .code(500)
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .message("Internal Server Error")
                .body(ResponseBody.create("text/plain".toMediaTypeOrNull(), "Internal Server Error"))
                .build()
        }
    }

    private fun handleUnauthorizedAccess(context: Context) {

        try {
            Log.d("mytag","handleUnauthorizedAccess")
            val mySharedPref=MySharedPref(context)
            mySharedPref.clearAll()
            val intent= Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.d("mytag","handleUnauthorizedAccess"+e.message)
            e.printStackTrace()

        }

    }

    private fun getTokenFromSharedPreferences(): String? {
        val sharedPreferences = MySharedPref(context)
        return sharedPreferences.getRememberToken()
    }
}