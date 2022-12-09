package com.example.chatapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.module.AppGlideModule;
import com.google.android.gms.common.internal.service.Common
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.toolbar.*
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageBox: EditText
    private lateinit var sendButton: ImageView
    private lateinit var cameraButton: ImageView
    private lateinit var toolbarContent: TextView
    private lateinit var toolbarImageContent: ImageView
    private lateinit var messageAdapter: messageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: DatabaseReference
    private lateinit var adapter: UserAdapter

    var receiverRoom: String? = null
    var senderRoom: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        val name = intent.getStringExtra("name")
        val receiverUid = intent.getStringExtra("uid")
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid


        mDbRef = FirebaseDatabase.getInstance("https://metischat-default-rtdb.europe-west1.firebasedatabase.app").getReference()
        lifecycle.addObserver(ApplicationObserver())

        senderRoom = receiverUid + senderUid
        receiverRoom = senderUid + receiverUid

        toolbarContent = findViewById(R.id.largeToolbarcontent)
        toolbarContent.text = name
        toolbarImageContent = findViewById(R.id.toolbarImage)


        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageBox = findViewById(R.id.messageBox)
        sendButton = findViewById(R.id.sentButton)
        cameraButton = findViewById(R.id.cameraIcon)
        messageList = ArrayList()
        messageAdapter = messageAdapter(this, messageList)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatRecyclerView.adapter = messageAdapter


        mDbRef.child("user").child(receiverUid.toString()).child("profileImageURL").addValueEventListener(object: ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                //Glide.with(this@ChatActivity).load(snapshot.value.toString()).override(100,100).centerCrop().into(toolbarImageContent)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })

        mDbRef.child("chats").child(senderRoom!!).child("messages")
                .addValueEventListener(object: ValueEventListener{
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val service = CounterNotificationService(applicationContext)
                        messageList.clear()

                        for(postSnapshot in snapshot.children){
                            val message = postSnapshot.getValue(Message::class.java)
                            messageList.add(message!!)
                            chatRecyclerView.scrollToPosition(messageAdapter.itemCount - 1)
                        }
                        messageAdapter.notifyDataSetChanged()

                    }

                    override fun onCancelled(error: DatabaseError) {

                    }

                })

        cameraButton.setOnClickListener(){
            Toast.makeText(this, "CameraPopUP", Toast.LENGTH_SHORT).show()
        }

        sendButton.setOnClickListener(){
            val message = messageBox.text.toString()
            val format = SimpleDateFormat("HH:mm")
            val time = format.format(Date())

            val messageObject = Message(message, senderUid, receiverUid, time)
            val sentMessageObject = Message("You: " + message, senderUid, receiverUid, time)

            val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$senderUid/$receiverUid")
            latestMessageRef.setValue(sentMessageObject)
            val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$receiverUid/$senderUid")
            latestMessageToRef.setValue(messageObject)


            if (message != "") {
                mDbRef.child("chats").child(senderRoom!!).child("messages").push()
                    .setValue(messageObject).addOnSuccessListener {
                        mDbRef.child("chats").child(receiverRoom!!).child("messages").push()
                            .setValue(messageObject)
                        chatRecyclerView.smoothScrollToPosition(messageAdapter.itemCount - 1)
                    }
            }
            messageBox.setText("")
        }

    }

}