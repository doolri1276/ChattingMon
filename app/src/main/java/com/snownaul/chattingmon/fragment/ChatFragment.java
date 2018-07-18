package com.snownaul.chattingmon.fragment;

import android.app.ActivityOptions;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.snownaul.chattingmon.R;
import com.snownaul.chattingmon.chat.GroupMessageActivity;
import com.snownaul.chattingmon.chat.MessageActivity;
import com.snownaul.chattingmon.model.ChatModel;
import com.snownaul.chattingmon.model.UserModel;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

/**
 * Created by alfo6-11 on 2018-07-04.
 */

public class ChatFragment extends Fragment {

    private SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd hh:mm");

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view=inflater.inflate(R.layout.fragment_chat,container,false);

        RecyclerView recyclerView=view.findViewById(R.id.chatfragment_recyclerview);
        recyclerView.setAdapter(new ChatRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(inflater.getContext()));


        return view;
    }

    class ChatRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        private List<ChatModel> chatModels = new ArrayList<>();
        private List<String> keys=new ArrayList<>();
        private String uid;
        private ArrayList<String> destinationUsers=new ArrayList<>();

        public ChatRecyclerViewAdapter() {

            uid= FirebaseAuth.getInstance().getCurrentUser().getUid();

            FirebaseDatabase.getInstance().getReference().child("chatrooms").orderByChild("users/"+uid).equalTo(true).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    chatModels.clear();

                    for(DataSnapshot item : dataSnapshot.getChildren()){
                        chatModels.add(item.getValue(ChatModel.class));
                        keys.add(item.getKey());
                    }

                    notifyDataSetChanged();
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view=LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat,parent,false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

            final CustomViewHolder customViewHolder=((CustomViewHolder)holder);
            String destinationUid = null;

            //챗방에 있는 유저를 일일이 체크
            Log.i("MyTag","새로운 챗방의 유저 수 : "+chatModels.get(position).users.size());
            for(String user : chatModels.get(position).users.keySet()){
                final StringBuffer userName=new StringBuffer();

                userName.append("");
                if(!user.equals(uid)){//내가 아닌사람 뽑아옴
                    destinationUid=user;
                    Log.i("MyTag","챗방에 있는유저 일일이 체크 position : "+position+", uid : "+destinationUid);

                    destinationUsers.add(destinationUid);


                    FirebaseDatabase.getInstance().getReference().child("users").child(destinationUid).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot dataSnapshot) {
                            UserModel userModel=dataSnapshot.getValue(UserModel.class);

                            Log.i("MyTag",position+"유저 이름!!!! "+userModel.userName);

                            Glide.with(customViewHolder.itemView.getContext())
                                    .load(userModel.profileImageUrl)
                                    .apply(new RequestOptions().circleCrop())
                                    .into(customViewHolder.imageView);

                            if(!(userName.toString().length()==0))userName.append(", ");

                            userName.append(userModel.userName);

                            customViewHolder.textView_title.setText(userModel.userName);

                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {

                        }
                    });

                }

            }

            //메세지를 내림 차순으로 정렬 후 마지막 메세지의 키값을 가져옴.
            Map<String, ChatModel.Comment> commentMap=new TreeMap<>(Collections.reverseOrder());
            commentMap.putAll(chatModels.get(position).comments);
            if(commentMap.keySet().toArray().length>0) {


                String lastMessageKey = (String) commentMap.keySet().toArray()[0];
                customViewHolder.textView_last_message.setText(chatModels.get(position).comments.get(lastMessageKey).message);

                //TimeStamp
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Seoul"));
                long unixTime = (long) chatModels.get(position).comments.get(lastMessageKey).timestamp;
                Date date = new Date(unixTime);
                customViewHolder.textView_timeStamp.setText(simpleDateFormat.format(date));


            }

            customViewHolder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("MyTag","클릭되었다. "+keys.get(position)+", 유저수 : "+chatModels.get(position).users.size());
                    Intent intent=null;
                    if(chatModels.get(position).users.size()>2){
                        Log.i("MyTag","들어간다 그룹챗으로..");
                        intent = new Intent(v.getContext(), GroupMessageActivity.class);
                        intent.putExtra("destinationRoom",keys.get(position));
                    }else{
                        Log.i("MyTag","개인챗 꼬우꼬우!");
                        intent = new Intent(v.getContext(), MessageActivity.class);
                        intent.putExtra("destinationUid", destinationUsers.get(position));
                    }


                    ActivityOptions activityOptions = ActivityOptions.makeCustomAnimation(v.getContext(), R.anim.fromright, R.anim.toleft);
                    startActivity(intent, activityOptions.toBundle());
                }
            });

        }

        @Override
        public int getItemCount() {
            return chatModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {

            public ImageView imageView;
            public TextView textView_title;
            public TextView textView_last_message;
            public TextView textView_timeStamp;

            public CustomViewHolder(View view) {
                super(view);

                imageView=view.findViewById(R.id.chatitem_imageview);
                textView_title=view.findViewById(R.id.chatitem_textview_title);
                textView_last_message=view.findViewById(R.id.chatitem_textview_lastMessage);
                textView_timeStamp=view.findViewById(R.id.chatitem_textview_timestamp);
            }
        }
    }
}
