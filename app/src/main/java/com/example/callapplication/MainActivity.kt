package com.example.callapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.github.javiersantos.appupdater.AppUpdater
import com.github.javiersantos.appupdater.AppUpdaterUtils
import com.github.javiersantos.appupdater.enums.AppUpdaterError
import com.github.javiersantos.appupdater.enums.Display
import com.github.javiersantos.appupdater.enums.UpdateFrom
import com.github.javiersantos.appupdater.objects.Update
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.initialize
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val permissions= arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO)
    val requestCode=1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if(!isPermissionGranted()){
            askPermission()
        }

        Firebase.initialize(this)

        btn_login.setOnClickListener{
            val userName=et_username.text.toString()
            val intent= Intent(this,CallActivity::class.java)
            intent.putExtra("username",userName)
            startActivity(intent)
        }

       val appUpdaterUtils= AppUpdaterUtils(this)
           .setUpdateFrom(UpdateFrom.GITHUB)
           .setGitHubUserAndRepo("arif0706","CallApplication")
           .withListener(object :AppUpdaterUtils.UpdateListener{
               override fun onSuccess(update: Update?, isUpdateAvailable: Boolean?) {
                   println("updater"+ update?.latestVersion+ update?.urlToDownload + isUpdateAvailable)
               }

               override fun onFailed(error: AppUpdaterError?) {
                    println("error"+error?.name)
               }
           })

        appUpdaterUtils.start()



    }

    private fun askPermission() {
        ActivityCompat.requestPermissions(this,permissions,requestCode)
    }

    private fun isPermissionGranted(): Boolean {

        permissions.forEach {
            if(ActivityCompat.checkSelfPermission(this,it)!=PackageManager.PERMISSION_GRANTED)
                return false
        }

        return true

    }
}