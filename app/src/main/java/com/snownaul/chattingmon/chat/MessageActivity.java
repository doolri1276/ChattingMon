package com.snownaul.chattingmon.chat;

import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.snownaul.chattingmon.R;
import com.snownaul.chattingmon.model.ChatModel;
import com.snownaul.chattingmon.model.NotificationModel;
import com.snownaul.chattingmon.model.UserModel;

import org.w3c.dom.Text;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MessageActivity extends AppCompatActivity {

    private String destinationUid;
    private Button button;
    private EditText editText;

    private String uid;
    private String chatRoomUid;

    private RecyclerView recyclerView;

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm");

    private UserModel destinationUserModel;
    private DatabaseReference databaseReference;
    private ValueEventListener valueEventListener;

    int peopleCount =0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        uid=FirebaseAuth.getInstance().getCurrentUser().getUid();//채팅을 요구하는 아이디 즉 단말기에 로그인된 UID
        destinationUid=getIntent().getStringExtra( "destinationUid");//채팅을 당하는 아이디

        button=findViewById(R.id.messageActivity_button);
        editText=findViewById(R.id.messageActivity_editText);

        recyclerView=findViewById(R.id.messageActivity_recyclerview);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                ChatModel chatModel=new ChatModel();
                chatModel.users.put(uid,true);
                chatModel.users.put(destinationUid,true);

                if(chatRoomUid==null){
                    button.setEnabled(false);
                    FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel).addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            checkChatRoom();
                        }
                    });

                }else{
                    ChatModel.Comment comment=new ChatModel.Comment();
                    comment.uid=uid;
                    comment.message=editText.getText().toString();
                    comment.timestamp = ServerValue.TIMESTAMP;

                    FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments").push().setValue(comment).addOnCompleteListener(new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            sendGcm();
                            editText.setText("");
                        }
                    });

                }




            }
        });

        checkChatRoom();
    }

    void sendGcm(){
        Log.i("MyTag","GCM ==================================================입장");
        Gson gson = new Gson();

        String userName = FirebaseAuth.getInstance().getCurrentUser().getDisplayName();
        Log.i("MyTag","GCM DisplayName : "+userName);

        NotificationModel notificationModel = new NotificationModel();
        notificationModel.to = destinationUserModel.pushToken;
        notificationModel.notification.title = userName;
        notificationModel.notification.text = editText.getText().toString();
        notificationModel.data.title = userName;
        notificationModel.data.text = editText.getText().toString();

        Log.i("MyTag","GCM notification text"+notificationModel.notification.text);
        Log.i("MyTag","GCM data text"+notificationModel.data.text);

        RequestBody requestBody = RequestBody.create(MediaType.parse("application/json; charset=utf8"),gson.toJson(notificationModel));

        Request request = new Request.Builder()
                .header("Content-Type","application/json")
                .addHeader("Authorization","key=AIzaSyA4lOJ6UlZQdYwTqbOn1Q_v6kaS35NBByo")
                .url("https://gcm-http.googleapis.com/gcm/send")
                .post(requestBody)
                .build();

        OkHttpClient okHttpClient = new OkHttpClient();
        okHttpClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("MyTag","GCM Okhttp 에러낫다아아앙~! : "+e.toString());

            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                Log.i("MyTag","GCM Okhttp response : "+response.toString());

            }
        });
    }

    void checkChatRoom(){
        FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot item:dataSnapshot.getChildren()){

                    ChatModel chatModel=item.getValue(ChatModel.class);

                    if(chatModel.users.containsKey(destinationUid)&&chatModel.users.size()==2){
                        chatRoomUid=item.getKey();
                        button.setEnabled(true);
                        recyclerView.setLayoutManager(new LinearLayoutManager(MessageActivity.this));
                        recyclerView.setAdapter(new RecyclerViewAdapter());
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        List<ChatModel.Comment> comments;


        public RecyclerViewAdapter() {
            comments=new ArrayList<>();

            FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    destinationUserModel =dataSnapshot.getValue(UserModel.class);
                    getMessageList();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }

        void getMessageList(){
            Log.i("MyTag","getMessageList()로 들어옴");
            databaseReference=FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments");
            valueEventListener=new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    comments.clear();

                    Map<String, Object> readUsersMap = new HashMap<>();



                    Log.i("MyTag","data change한 곳으로 들어옴. 갯수 : "+dataSnapshot.getChildrenCount());



                    for(DataSnapshot item : dataSnapshot.getChildren()){

                        String key = item.getKey();

                        ChatModel.Comment comment_origin=item.getValue(ChatModel.Comment.class);
                        ChatModel.Comment comment_modify=item.getValue(ChatModel.Comment.class);

                        comment_modify.readUsers.put(uid,true);

                        readUsersMap.put(key, comment_modify);
                        comments.add(comment_origin);
                        Log.i("MyTag","data 추가함!!" + comment_modify.message);
                    }

                    if(comments.size()!=0&&!comments.get(comments.size()-1).readUsers.containsKey(uid)){
                        FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("comments")
                                .updateChildren(readUsersMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //메세지가 갱신됨
                                notifyDataSetChanged();

                                recyclerView.scrollToPosition(comments.size()-1);
                            }
                        });
                    }else{
                        notifyDataSetChanged();
                        recyclerView.scrollToPosition(comments.size()-1);
                    }




                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };

            databaseReference.addValueEventListener(valueEventListener);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message,parent,false);
            return new MessageViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            MessageViewHolder messageViewHolder=((MessageViewHolder)holder);

            Log.i("MyTag","bind하러 입장..");

            if(comments.get(position).uid.equals(uid)){//내가 보낸 메세지
                Log.i("MyTag","내가 쓴 메세지임"+comments.get(position).message);
                messageViewHolder.textView_message.setText(comments.get(position).message);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.rightbubble);
                messageViewHolder.linearLayout_destination.setVisibility(View.INVISIBLE);
//                messageViewHolder.textView_message.setTextSize(25);
                messageViewHolder.linearLayout_main.setGravity(Gravity.RIGHT);
                setReadcounter(position,messageViewHolder.textView_readCounter_left);

            }else{//상대방이 보낸 말풍선
                Log.i("MyTag","상대가쓴메세지임 : "+comments.get(position).message);
                Glide.with(holder.itemView.getContext())
                        .load(destinationUserModel.profileImageUrl)
                        .apply(new RequestOptions().circleCrop())
                        .into(messageViewHolder.imageView_profile);

                messageViewHolder.textView_name.setText(destinationUserModel.userName);
                messageViewHolder.linearLayout_destination.setVisibility(View.VISIBLE);
                messageViewHolder.textView_message.setBackgroundResource(R.drawable.leftbubble);
                messageViewHolder.textView_message.setText(comments.get(position).message);
//                messageViewHolder.textView_message.setTextSize(10);
                messageViewHolder.linearLayout_main.setGravity(Gravity.LEFT);
                setReadcounter(position,messageViewHolder.getTextView_readCounter_right);

            }

            long unixTime = (long)comments.get(position).timestamp;
            Date date = new Date(unixTime);
            simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
            String time = simpleDateFormat.format(date);
            messageViewHolder.textView_timestamp.setText(time);


        }

        void setReadcounter(final int position, final TextView textView){
            if(peopleCount==0){
                FirebaseDatabase.getInstance().getReference().child("chatrooms").child(chatRoomUid).child("users").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Map<String, Boolean> users = (Map<String, Boolean>) dataSnapshot.getValue();
                        peopleCount = users.size();
                        int count = peopleCount-comments.get(position).readUsers.size();
                        if(count>0){
                            textView.setVisibility(View.VISIBLE);
                            textView.setText(String.valueOf(count));
                        }else{
                            textView.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            }else{
                int count = peopleCount-comments.get(position).readUsers.size();
                if(count>0){
                    textView.setVisibility(View.VISIBLE);
                    textView.setText(String.valueOf(count));
                }else{
                    textView.setVisibility(View.INVISIBLE);
                }
            }


        }

        @Override
        public int getItemCount() {
            return comments.size();
        }

        private class MessageViewHolder extends RecyclerView.ViewHolder {
            public TextView textView_message;
            public TextView textView_name;
            public ImageView imageView_profile;
            public LinearLayout linearLayout_destination;
            public LinearLayout linearLayout_main;
            public TextView textView_timestamp;
            public TextView textView_readCounter_left;
            public TextView getTextView_readCounter_right;

            public MessageViewHolder(View view) {
                super(view);
                textView_message=view.findViewById(R.id.messageItem_textView_message);
                textView_name=view.findViewById(R.id.messageItem_textView_name);
                imageView_profile=view.findViewById(R.id.messageItem_imageview_profile);
                linearLayout_destination=view.findViewById(R.id.messageItem_linearlayout_destination);
                linearLayout_main=view.findViewById(R.id.messageItem_linearlayout_main);
                textView_timestamp=view.findViewById(R.id.messageItem_textView_timestamp);
                textView_readCounter_left = view.findViewById(R.id.messageItem_textview_readCounter_left);
                getTextView_readCounter_right = view.findViewById(R.id.messageItem_textview_readCounter_right);
            }
        }
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        if(valueEventListener!=null){
            databaseReference.removeEventListener(valueEventListener);
        }

        finish();

        overridePendingTransition(R.anim.fromleft,R.anim.toright);
    }
}
