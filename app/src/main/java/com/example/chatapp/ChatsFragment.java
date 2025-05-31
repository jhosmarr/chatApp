package com.example.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class ChatsFragment extends Fragment {

    private View PrivateChatsView;
    private RecyclerView chatList;

    private DatabaseReference ChatsRef, UsersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    public ChatsFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        PrivateChatsView = inflater.inflate(R.layout.chats_fragment, container, false);

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();

        ChatsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserID);
        UsersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        chatList = PrivateChatsView.findViewById(R.id.chat_list);
        chatList.setLayoutManager(new LinearLayoutManager(getContext()));

        return PrivateChatsView;
    }

    @Override
    public void onStart() {
        super.onStart();

        ChatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> userIdList = new ArrayList<>();

                for (DataSnapshot contactSnapshot : snapshot.getChildren()) {
                    String userId = contactSnapshot.getKey();

                    final String[] retImage = {"default_image"};
                    userIdList.add(userId);
                }

                ChatsAdapter adapter = new ChatsAdapter(userIdList);
                chatList.setAdapter(adapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Manejo de error si lo deseas
            }
        });
    }

    private class ChatsAdapter extends RecyclerView.Adapter<ChatsViewHolder> {

        private List<String> userIds;

        public ChatsAdapter(List<String> userIds) {
            this.userIds = userIds;
        }

        @NonNull
        @Override
        public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.users_display_layout, parent, false);
            return new ChatsViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ChatsViewHolder holder, int position) {
            String userId = userIds.get(position);

            final String[] retImage = {"default_image"};

            UsersRef.child(userId).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot)
                {
                    if(dataSnapshot.hasChild("image"))
                    {
                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.hasChild("image")) {
                                retImage[0] = dataSnapshot.child("image").getValue(String.class);
                                Picasso.get().load(retImage[0]).into(holder.profileImage);
                            }

                            String name = dataSnapshot.child("name").getValue(String.class);
                            String status = dataSnapshot.child("status").getValue(String.class);

                            holder.userName.setText(name);

                            if (dataSnapshot.child("userState").hasChild("state"))
                            {
                                String state = dataSnapshot.child("userState").child("state").getValue().toString();
                                String date = dataSnapshot.child("userState").child("date").getValue().toString();
                                String time = dataSnapshot.child("userState").child("time").getValue().toString();

                                if(state.equals("en linea"))
                                {
                                    holder.userStatus.setText("en linea");
                                }
                                else if(state.equals("fuera de linea"))
                                {
                                    holder.userStatus.setText("Last Seen: " + date + " " + time);
                                }
                            }
                            else
                            {
                                holder.userStatus.setText("fuera de línea");
                            }


                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent chatIntent  = new Intent(getContext(), ChatActivity.class);
                                    chatIntent.putExtra("visit_user_id", userId);
                                    chatIntent.putExtra("visit_user_name", name);
                                    chatIntent.putExtra("visit_image", retImage[0]);
                                    startActivity(chatIntent);
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }

        @Override
        public int getItemCount() {
            return userIds.size();
        }
    }

    public static class ChatsViewHolder extends RecyclerView.ViewHolder {
        CircleImageView profileImage;
        TextView userStatus, userName;

        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.users_profile_image);
            userStatus = itemView.findViewById(R.id.user_status);
            userName = itemView.findViewById(R.id.users_profile_name);
        }
    }
}
