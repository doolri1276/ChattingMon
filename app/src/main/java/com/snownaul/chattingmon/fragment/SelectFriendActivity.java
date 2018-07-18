package com.snownaul.chattingmon.fragment;

import android.app.ActivityOptions;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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

import java.util.ArrayList;
import java.util.List;

public class SelectFriendActivity extends AppCompatActivity {

    ChatModel chatModel = new ChatModel();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_friend);

        RecyclerView recyclerView = findViewById(R.id.selectFriendActivity_recyclerview);
        recyclerView.setAdapter(new SelectFriendRecyclerViewAdapter());
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        Button button = findViewById(R.id.selectFriendActivity_button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                chatModel.users.put(myUid, true);

                FirebaseDatabase.getInstance().getReference().child("chatrooms").push().setValue(chatModel).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        finish();
                    }
                });
            }
        });

    }

    class SelectFriendRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

        List<UserModel> userModels;

        public SelectFriendRecyclerViewAdapter(){
            userModels=new ArrayList<>();
            final String myUid= FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseDatabase.getInstance().getReference().child("users").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    userModels.clear();

                    for(DataSnapshot snapshot: dataSnapshot.getChildren()){
                        UserModel userModel=snapshot.getValue(UserModel.class);

                        if(userModel.uid.equals(myUid)) continue;
                        userModels.add(userModel);
                    }
                    notifyDataSetChanged();//꼭 넣어줘야 한다.

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });

        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.item_friend_select, parent, false);
            return new CustomViewHolder(view);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {

            Glide.with(holder.itemView.getContext())
                    .load(userModels.get(position).profileImageUrl)
                    .apply(new RequestOptions().circleCrop())
                    .into(((CustomViewHolder)holder).imageView);

            ((CustomViewHolder)holder).textView.setText(userModels.get(position).userName);

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(v.getContext(), MessageActivity.class);
                    intent.putExtra("destinationUid",userModels.get(position).uid);
                    ActivityOptions activityOptions= ActivityOptions.makeCustomAnimation(v.getContext(),R.anim.fromright,R.anim.toleft);
                    startActivity(intent,activityOptions.toBundle());
                }
            });

            if(userModels.get(position).comment!=null){
                ((CustomViewHolder)holder).textView_comment.setText(userModels.get(position).comment);
            }

            ((CustomViewHolder) holder).checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){//체크 된 상태임

                        chatModel.users.put(userModels.get(position).uid, true);

                    }else{//체크 취소 상태
                        chatModel.users.remove(userModels.get(position));
                    }
                }
            });


        }

        @Override
        public int getItemCount() {
            return userModels.size();
        }

        private class CustomViewHolder extends RecyclerView.ViewHolder {
            public ImageView imageView;
            public TextView textView;
            public TextView textView_comment;
            public CheckBox checkBox;

            public CustomViewHolder(View view) {
                super(view);
                imageView=view.findViewById(R.id.frienditem_imageview);
                textView=view.findViewById(R.id.frienditem_textview);
                textView_comment=view.findViewById(R.id.frienditem_textview_comment);
                checkBox = view.findViewById(R.id.frienditem_checkbox);

            }
        }
    }
}
