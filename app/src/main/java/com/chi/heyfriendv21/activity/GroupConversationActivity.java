package com.chi.heyfriendv21.activity;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.chi.heyfriendv21.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import adapter.GroupMessagesForRecyclerViewAdapter;
import common.CommonMethod;
import common.Constant;
import common.MessageViewHolder;
import dialog.EditConversationNameDialogFragment;
import dialog.ListParticipantsDialogFragment;
import object.GroupChatData;
import object.LastMessage;
import object.Message;


public class GroupConversationActivity extends AppCompatActivity implements
        View.OnClickListener, PopupMenu.OnMenuItemClickListener, EditConversationNameDialogFragment.OnCompleteListener{


    private String clientUid, myUid;
    private GroupChatData groupChatData;
    private ArrayList<Message> messages;
    private DatabaseReference databaseReference;
    private FirebaseDatabase firebaseDatabase;
    ImageView ibBack;
    ImageView ivMap, ivCamera;
    TextView tvName, tvState;
    RecyclerView recyclerView;
    ImageButton ibMenu;
    private GroupMessagesForRecyclerViewAdapter groupMessagesForRecyclerViewAdapter;
    private LinearLayoutManager linearLayoutManager;

    private ImageView ivSend;
    private EditText etMessage;
    @Override
    public boolean onContextItemSelected(MenuItem item) {
        int position = -1;
        try {
            position = groupMessagesForRecyclerViewAdapter.getPosition();
        } catch (Exception e) {
//            Log.d(TAG, e.getLocalizedMessage(), e);
            return super.onContextItemSelected(item);
        }
        switch (item.getItemId()) {
            case 1:
                // do your stuff

                break;
            case 2:
                // copy text
                if (position!= -1){
                    ClipboardManager clipboard = (ClipboardManager) GroupConversationActivity.this.getSystemService(Context.CLIPBOARD_SERVICE);
                    String text = groupMessagesForRecyclerViewAdapter.getItem(position).getMessage();
                    ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
                    clipboard.setPrimaryClip(clip);

                }

                break;
            case 3:
                //delete message
//                if (position!= -1){
//                    final int finalPosition = position;
//                    if (position==groupMessagesForRecyclerViewAdapter.getItemCount()-1 && groupMessagesForRecyclerViewAdapter.getItemCount()>1){
////                        Log.e("----------index", position+"");
//                        Message prevMessage = groupMessagesForRecyclerViewAdapter.getItem(position-1);
//
//                        databaseReference.child(Constant.CHILD_FRIENDLISTS).child(myUid).child(clientUid).
//                                child(Constant.CHILD_LASTMESSAGE).setValue(new LastMessage(prevMessage.getMessage(), prevMessage.getTime(), true, prevMessage.getSenderUid()), new DatabaseReference.CompletionListener() {
//                            @Override
//                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//                                if(databaseError!= null){
//                                    Toast.makeText(GroupConversationActivity.this,
//                                            getString(R.string.announce_cant_delete_msg),Toast.LENGTH_SHORT)
//                                            .show();
//                                }
//                                else{
//                                    deleteMessage(finalPosition);
//                                }
//                            }
//                        });
//                    }else
//                    if (position==0){
//                        databaseReference.child(Constant.CHILD_FRIENDLISTS).child(myUid).child(clientUid).
//                                child(Constant.CHILD_LASTMESSAGE).removeValue(new DatabaseReference.CompletionListener() {
//                            @Override
//                            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
//                                if(databaseError!= null){
//                                    Toast.makeText(GroupConversationActivity.this,getString(R.string.announce_cant_delete_msg),Toast.LENGTH_SHORT).show();
//                                }
//                                else{
//                                    deleteMessage(finalPosition);
//                                }
//                            }
//                        });
//                    }else {
//                        deleteMessage(position);
//                    }
//                }
                break;
        }
        return super.onContextItemSelected(item);
    }
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onetoone_conversation);
        getDataIntent();
        initViews();

//        getClientInfo();
        CommonMethod.updateOnlineState(myUid, databaseReference);
        groupMessagesForRecyclerViewAdapter = new GroupMessagesForRecyclerViewAdapter(Message.class,
                R.layout.item_message_onetoone,
                MessageViewHolder.class,
                databaseReference.child(Constant.CHILD_GROUPCHAT).child(groupChatData.getUid()).child(Constant.KEY_MESSAGES).getRef(),
                GroupConversationActivity.this,  groupChatData, myUid);

        groupMessagesForRecyclerViewAdapter.registerAdapterDataObserver(
                new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                int friendlyMessageCount = groupMessagesForRecyclerViewAdapter.getItemCount();
                int lastVisiblePosition =linearLayoutManager.findLastCompletelyVisibleItemPosition();
                // If the recycler view is initially being loaded or the user is at the bottom of the list, scroll
                // to the bottom of the list to show the newly added message.
                if (lastVisiblePosition == -1 ||
                        (positionStart >= (friendlyMessageCount - 1) && lastVisiblePosition == (positionStart - 1))) {
                    recyclerView.scrollToPosition(positionStart);
                }
            }
        });

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.hasFixedSize();
        recyclerView.setAdapter(groupMessagesForRecyclerViewAdapter);

        //auto scroll to bottom
        if (Build.VERSION.SDK_INT >= 11) {
            recyclerView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
                @Override
                public void onLayoutChange(View v,
                                           int left, int top, int right, int bottom,
                                           int oldLeft, int oldTop, int oldRight, int oldBottom) {
                    if (bottom < oldBottom) {
                        recyclerView.smoothScrollToPosition(recyclerView.getAdapter().getItemCount());
                    }
                }
            });
        }
    }
    public void deleteMessage(int position){
        final MessageViewHolder messageViewHolder= (MessageViewHolder) recyclerView.findViewHolderForAdapterPosition(position);
        messageViewHolder.llAllComponent.setVisibility(View.GONE);
        groupMessagesForRecyclerViewAdapter.getRef(position).removeValue(new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                if (databaseError!= null){
                    if(GroupConversationActivity.this!=null)
                        Toast.makeText(GroupConversationActivity.this,getString(R.string.announce_cant_delete_msg), Toast.LENGTH_SHORT).show();
                    messageViewHolder.llAllComponent.setVisibility(View.VISIBLE);
                }
                else{
                    if (GroupConversationActivity.this!= null)
                        Toast.makeText(GroupConversationActivity.this, R.string.announce_msg_is_deleted, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }
    public void initViews(){
        ibBack = (ImageButton) findViewById(R.id.ibBack);
        ivCamera = (ImageView) findViewById(R.id.ivCamera);
        ivMap = (ImageView) findViewById(R.id.ivLocation);
        ivSend = (ImageView) findViewById(R.id.ivSend);
        tvName = (TextView) findViewById(R.id.tvName);
        tvState= (TextView) findViewById(R.id.tvState);
        etMessage = (EditText) findViewById(R.id.etMessage);
        recyclerView= (RecyclerView) findViewById(R.id.messageRecyclerView);
        ibMenu = (ImageButton) findViewById(R.id.ibMenu);

        //set on click button
        ivMap.setOnClickListener(this);
        ivCamera.setOnClickListener(this);
        ibBack.setOnClickListener(this);
        ivSend.setOnClickListener(this);

        ivSend.setEnabled(false);
        ibMenu.setOnClickListener(this);
//        if (groupChatData!=null)
            if (groupChatData.getName()!=null)
                tvName.setText(groupChatData.getName());
        etMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.toString().trim().length() > 0) {
                    ivSend.setEnabled(true);
                }
                else {
                    ivSend.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });

        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference();
        linearLayoutManager= new LinearLayoutManager(this);
        updateView();

    }
    public void getDataIntent(){
        Intent intent = getIntent();
        groupChatData=(GroupChatData) intent.getSerializableExtra(Constant.GROUP_CHAT_DATA);

        myUid = intent.getStringExtra(Constant.MY_UID);

    }

    public void getClientInfo(){
        ValueEventListener valueEventListenerClientInfo = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.hasChild(clientUid)){
                    String name="", state="";
                    Boolean connection= false;
                    long lastOnline=0;
                    if (dataSnapshot.child(clientUid).hasChild(Constant.KEY_NAME))
                        name= dataSnapshot.child(clientUid).child(Constant.KEY_NAME).getValue().toString();
                    if (tvName!= null)
                        tvName.setText(name);
                    if (dataSnapshot.child(clientUid).hasChild(Constant.KEY_CONNECTION))
                        connection= (Boolean) dataSnapshot.child(clientUid).child(Constant.KEY_CONNECTION).getValue();
                    if (dataSnapshot.child(clientUid).hasChild(Constant.KEY_LASTONLINE))
                        lastOnline= Long.parseLong(dataSnapshot.child(clientUid).child(Constant.KEY_LASTONLINE).getValue().toString());
                    if (connection==false){
                        state=getString(R.string.txt_state)+ " "+CommonMethod.diffTime(getApplicationContext(), lastOnline);
                    }
                    else state = "Online";
                    if(tvState!= null)
                        tvState.setText(state);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        databaseReference.child(Constant.CHILD_GROUPCHAT).addValueEventListener(valueEventListenerClientInfo);
    }
    @Override
    public void onClick(View view) {
//        view.startAnimation(Constant.buttonClick);
        CommonMethod.showAnimation(view, GroupConversationActivity.this);
        if (view == ibBack)
            finish();
        else if (view == ivSend){
            //update in CHILD_CHATONETOONE and CHILD_LASTMESSAGE

            if (etMessage.getText().toString().trim().equals("")){
                Toast.makeText(GroupConversationActivity.this, R.string.announce_empty_msg, Toast.LENGTH_SHORT).show();
            }else{
                databaseReference.child(Constant.CHILD_GROUPCHAT).child(groupChatData.getUid()).child(Constant.KEY_MESSAGES).push().setValue(new Message(etMessage.getText().toString().trim(), myUid), new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if(databaseError!= null){
                            Log.e("completion error", databaseError.getDetails());
                            if (GroupConversationActivity.this!=null)
                                Toast.makeText(GroupConversationActivity.this, R.string.announce_cant_send_msg, Toast.LENGTH_SHORT).show();
                        }
                        else{
                            if (etMessage!=null)
                                etMessage.setText("");
                        }
                    }
                });

                databaseReference.child(Constant.CHILD_GROUPCHAT).child(groupChatData.getUid()).child(Constant.CHILD_LASTMESSAGE).setValue(new LastMessage(etMessage.getText().toString().trim(), myUid, true), new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                        if(databaseError!= null){
                            Log.e("completion error", databaseError.getDetails());
                            if (GroupConversationActivity.this!=null)
                                Toast.makeText(GroupConversationActivity.this, R.string.announce_cant_send_msg, Toast.LENGTH_SHORT).show();
                        }
                        else{
                            if (etMessage!=null)
                                etMessage.setText("");
                        }
                    }
                });
                for (GroupChatData.Participant participant: groupChatData.getParticipants()){
                    String uid = participant.getUid();
                    if (uid.equals(myUid)){
                        databaseReference.child(Constant.CHILD_GROUPCHAT).child(groupChatData.getUid())
                                .child(Constant.CHILD_USERS).child(myUid).child(Constant.KEY_STATUS).setValue(true);
                    }
                    else databaseReference.child(Constant.CHILD_GROUPCHAT).child(groupChatData.getUid())
                            .child(Constant.CHILD_USERS).child(uid).child(Constant.KEY_STATUS).setValue(false);
                }
            }

        }
        else if (view == ibMenu){
            showPopup(view);
        }
    }
    private void updateView(){
        Thread t = new Thread() {

            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(30000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
//                                getClientInfo();
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };

        t.start();
    }

    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.list_participants_menu:
                ListParticipantsDialogFragment dialog = new ListParticipantsDialogFragment(groupChatData);
                dialog.show(getFragmentManager(), "");
                return true;
            case R.id.edit_name_menu:
                EditConversationNameDialogFragment dialogEditName = new EditConversationNameDialogFragment(groupChatData);
                dialogEditName.show(getFragmentManager(), "");
                return true;
            case R.id.leave_this_group_menu:
                DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int choice) {
                        switch (choice) {
                            case DialogInterface.BUTTON_POSITIVE:
                                databaseReference.child(Constant.CHILD_GROUPCHAT).child(groupChatData.getUid()).child(Constant.CHILD_USERS)
                                        .child(myUid).child(Constant.KEY_STATUS).setValue(false, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
                                        if (databaseError!= null){

                                        }
                                        else{
                                            finish();
                                        }
                                    }
                                });

                                break;
                            case DialogInterface.BUTTON_NEGATIVE:
                                break;
                        }
                    }
                };

                AlertDialog.Builder builder = new AlertDialog.Builder(GroupConversationActivity.this);
                builder.setMessage(R.string.announce_leave_group)
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener).show();
                return true;
            default:
                return false;
        }

    }
    public void showPopup(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.setOnMenuItemClickListener(this);

        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.chat_menu, popup.getMenu());
        popup.show();
    }

    @Override
    public void onComplete(String name) {
        if (name!= null){
            if (tvName!=null)
                tvName.setText(name);
        if (groupChatData!=null)
            groupChatData.setName(name);
        }
    }
}

