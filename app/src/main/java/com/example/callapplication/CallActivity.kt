package com.example.callapplication

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_call.*
import java.util.*

class CallActivity : AppCompatActivity() {

    var userName=""
    var friendsUserName=""

    var isPeerConnected=false
    var firebaseRef= Firebase.database.getReference("users")

    var isAudio=true
    var isVideo=true

    var uniqueId=""


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call)

        userName=intent.getStringExtra("username")!!


        btnCall.setOnClickListener{

            friendsUserName= et_friend_name.text.toString()
            sendCallRequest()

        }

        btnMic.setOnClickListener{
            isAudio=!isAudio
            callJavaScriptFunction("javascript:toggleAudio(\"$isAudio\")")
            btnMic.setImageResource(if(isAudio) R.drawable.ic_baseline_mic_24 else R.drawable.ic_baseline_mic_off_24)

        }
        btnVideo.setOnClickListener{
            isVideo=!isVideo
            callJavaScriptFunction("javascript:toggleVideo(\"$isVideo\")")
            btnVideo.setImageResource(if(isVideo) R.drawable.ic_baseline_videocam_24 else R.drawable.ic_baseline_videocam_off_24)

        }


        setUpWebView()
    }

    private fun sendCallRequest() {
        if(!isPeerConnected){
            Toast.makeText(this, "You are not connected", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseRef.child(friendsUserName).child("incoming").setValue(userName)
        firebaseRef.child(friendsUserName).child("isAvailable").addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.value.toString() == "true"){
                    listenForConnectionID()
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    private fun listenForConnectionID() {
        firebaseRef.child(friendsUserName).child("connId").addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(snapshot.value==null)
                    return
                switchToControls()
                callJavaScriptFunction("javascript:startCall(\"${snapshot.value}\")")
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setUpWebView() {

        web_view.webChromeClient=object :WebChromeClient(){
            override fun onPermissionRequest(request: PermissionRequest?) {
                request?.grant(request.resources)
            }
        }

        web_view.settings.javaScriptEnabled=true
        web_view.settings.mediaPlaybackRequiresUserGesture=false

        web_view.addJavascriptInterface(JavaScriptInterface(this),"Android")

        loadVideoCall()
    }

    private fun loadVideoCall() {
        val filePath = "file:android_asset/call.html"
        web_view.loadUrl(filePath)

        web_view.webViewClient=object :WebViewClient(){
            override fun onPageFinished(view: WebView?, url: String?) {
                initializePeer()
            }
        }
    }


    private fun initializePeer() {

        uniqueId=getUniqueID()
        callJavaScriptFunction("javascript:init(\"${uniqueId}\")")
        firebaseRef.child(userName).child("incoming").addValueEventListener(object :ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                onCallRequest(snapshot.value as? String)
            }

            override fun onCancelled(error: DatabaseError) {}
        })

    }

    private fun onCallRequest(caller: String?) {
        if(caller == null) return

        callLayout.visibility= View.VISIBLE
        tv_incoming_call_text.text= "$caller is calling..."

        acceptBtn.setOnClickListener{
            firebaseRef.child(userName).child("connId").setValue(uniqueId)
            firebaseRef.child(userName).child("isAvailable").setValue(true)


            callLayout.visibility=View.GONE
            switchToControls()
        }

        rejectBtn.setOnClickListener{
            firebaseRef.child(userName).child("incoming").setValue(null)
            callLayout.visibility=View.GONE
        }
    }

    private fun switchToControls() {
        inputLayout.visibility=View.GONE
        callControlLayout.visibility=View.VISIBLE
    }

    private fun getUniqueID():String{
        return UUID.randomUUID().toString()
    }

    private fun callJavaScriptFunction(functionString:String){
        web_view.post{
            web_view.evaluateJavascript(functionString,null)
        }
    }

    fun onPeerConnected() {
        isPeerConnected=true
    }

    override fun onBackPressed() {
        finish()
    }

    override fun onDestroy() {
        firebaseRef.child(userName).setValue(null)
        web_view.loadUrl("about:blank")
        super.onDestroy()
    }
}