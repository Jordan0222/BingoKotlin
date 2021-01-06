package com.jordan.bingokotlin

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.AuthUI.IdpConfig.EmailBuilder
import com.firebase.ui.auth.AuthUI.IdpConfig.GoogleBuilder
import com.firebase.ui.database.FirebaseRecyclerAdapter
import com.firebase.ui.database.FirebaseRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.room_row.view.*

class MainActivity : AppCompatActivity(), FirebaseAuth.AuthStateListener, View.OnClickListener {
    companion object{
        val TAG: String = MainActivity::class.java.simpleName
        const val RC_SIGN_IN = 100
    }

    private lateinit var adapter: FirebaseRecyclerAdapter<GameRoom, RoomHolder>
    var member: Member? = null
    var avatarIds = intArrayOf(R.drawable.avatar_0,
            R.drawable.avatar_1,
            R.drawable.avatar_2,
            R.drawable.avatar_3,
            R.drawable.avatar_4,
            R.drawable.avatar_5,
            R.drawable.avatar_6)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        nickname.setOnClickListener {
            FirebaseAuth.getInstance().currentUser?.let {
                showNickDialog(it.uid, nickname.text.toString())
            }
        }

        group_avatars.visibility = View.GONE

        avatar.setOnClickListener {
            group_avatars.visibility =
                    if (group_avatars.visibility == View.GONE) View.VISIBLE else View.GONE

        }

        avatar_0.setOnClickListener(this)
        avatar_1.setOnClickListener(this)
        avatar_2.setOnClickListener(this)
        avatar_3.setOnClickListener(this)
        avatar_4.setOnClickListener(this)
        avatar_5.setOnClickListener(this)
        avatar_6.setOnClickListener(this)

        fab.setOnClickListener{
            val roomText = EditText(this)
            roomText.setText("Welcome")

            AlertDialog.Builder(this)
                    .setTitle("Game Room")
                    .setMessage("Please enter your room Title")
                    .setView(roomText)
                    .setPositiveButton("OK") { dialog, which ->
                        val roomTitle = roomText.text.toString()
                        val room = GameRoom(roomTitle, member)
                        FirebaseDatabase.getInstance()
                                .getReference("rooms")
                                .push().setValue(room)
                    }.show()
        }

        // RecyclerView for Game Room
        recycler.setHasFixedSize(true)
        recycler.layoutManager = LinearLayoutManager(this)

        val query = FirebaseDatabase.getInstance()
                .getReference("rooms")
                .limitToLast(30)

        val options = FirebaseRecyclerOptions.Builder<GameRoom>()
                .setQuery(query, GameRoom::class.java)
                .build()

        adapter = object : FirebaseRecyclerAdapter<GameRoom, RoomHolder>(options) {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomHolder {
                val view = layoutInflater.inflate(R.layout.room_row, parent, false)
                return RoomHolder(view)
            }

            override fun onBindViewHolder(holder: RoomHolder, position: Int, model: GameRoom) {
                holder.image.setImageResource(avatarIds[model.init!!.avatarId])
                holder.title.text = model.title
            }
        }
        recycler.adapter = adapter
    }

    class RoomHolder(view: View) : RecyclerView.ViewHolder(view) {
        var image: ImageView = view.room_image
        var title: TextView = view.room_title
    }

    override fun onStart() {
        super.onStart()
        FirebaseAuth.getInstance().addAuthStateListener(this)
        adapter.startListening()
    }

    override fun onStop() {
        super.onStop()
        FirebaseAuth.getInstance().removeAuthStateListener(this)
        adapter.stopListening()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_menu_signout -> {
                FirebaseAuth.getInstance().signOut()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onAuthStateChanged(auth: FirebaseAuth) {
        // if currentUser is null, signUp
        auth.currentUser?.also {
            Log.d(TAG, it.uid.toString());

            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(it.uid)
                    .addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            member = snapshot.getValue(Member::class.java)
                            member?.nickname?.also { nick ->
                                nickname.text = nick
                            } ?: showNickDialog(it)
                            member?.let {
                                avatar.setImageResource(avatarIds[it.avatarId])
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {

                        }
                    })

            it.displayName?.run {
                FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(it.uid)
                    .child("displayName")
                    .setValue(this)
                    .addOnCompleteListener { Log.d(TAG, "done") }
            }

            /*FirebaseDatabase.getInstance()
                .getReference("users")
                .child(it.uid)
                .child("nickname")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        snapshot.value?.also { nick ->

                        } ?: showNickDialog(it)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })*/
        } ?: signUp()

        /*if (auth.currentUser == null) {
            signUp()
        } else {
            Log.d(TAG, auth.currentUser?.uid.toString());
        }*/
    }

    private fun showNickDialog(uid: String, nick: String?) {
        val nickEdit = EditText(this)
        nickEdit.setText(nick)

        AlertDialog.Builder(this)
                .setTitle("Your nickname")
                .setMessage("Please enter your nickname")
                .setView(nickEdit)
                .setPositiveButton("OK") { dialog, which ->
                    val nickname = nickEdit.text.toString()
                    FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(uid)
                            .child("nickname")
                            .setValue(nickname)
                }.show()
    }

    private fun showNickDialog(firebaseUser: FirebaseUser) {
        val nick = firebaseUser.displayName
        val uid = firebaseUser.uid
        showNickDialog(uid, nick)
    }

    private fun signUp() {
        startActivityForResult(
                AuthUI.getInstance().createSignInIntentBuilder()
                        .setAvailableProviders(
                                listOf(
                                        EmailBuilder().build(),
                                        GoogleBuilder().build()
                                )
                        )
                        .setIsSmartLockEnabled(false)
                        .build(), RC_SIGN_IN
        )
    }

    override fun onClick(v: View?) {
        val selectedId = when (v!!.id) {
            R.id.avatar_0 -> 0
            R.id.avatar_1 -> 1
            R.id.avatar_2 -> 2
            R.id.avatar_3 -> 3
            R.id.avatar_4 -> 4
            R.id.avatar_5 -> 5
            R.id.avatar_6 -> 6
            else -> 0
        }
        FirebaseDatabase.getInstance().getReference("users")
                .child(FirebaseAuth.getInstance().currentUser!!.uid)
                .child("avatarId")
                .setValue(selectedId)
        group_avatars.visibility = View.GONE
    }
}