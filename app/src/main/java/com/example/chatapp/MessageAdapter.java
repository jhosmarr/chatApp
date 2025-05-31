package com.example.chatapp;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast; // Importar para mostrar mensajes Toast

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    private List<Messages> userMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference usersRef;
    // No necesitamos messagesRef aquí si delegamos la eliminación a ChatActivity

    public MessageAdapter(List<Messages> userMessagesList) {
        this.userMessagesList = userMessagesList;
    }

    public class MessageViewHolder extends RecyclerView.ViewHolder {
        public TextView senderMessageText, receiverMessageText;
        public CircleImageView receiverProfileImage;
        public ImageView messageSenderPicture, messageReceiverPicture;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            senderMessageText = itemView.findViewById(R.id.sender_messsage_text);
            receiverMessageText = itemView.findViewById(R.id.receiver_message_text);
            receiverProfileImage = itemView.findViewById(R.id.message_profile_image);
            messageReceiverPicture = itemView.findViewById(R.id.message_receiver_image_view);
            messageSenderPicture = itemView.findViewById(R.id.message_sender_image_view);
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.custom_messages_layout, parent, false);
        mAuth = FirebaseAuth.getInstance();
        // messagesRef no es necesario inicializarlo aquí si la lógica de eliminación se mueve a ChatActivity
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessageViewHolder holder, @SuppressLint("RecyclerView") final int position) {
        if (mAuth.getCurrentUser() == null) return;

        String messageSenderId = mAuth.getCurrentUser().getUid();
        Messages messages = userMessagesList.get(position);

        String fromUserID = messages.getFrom();
        String fromMessageType = messages.getType();

        usersRef = FirebaseDatabase.getInstance().getReference().child("User").child(fromUserID);

        usersRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.hasChild("image")) {
                    String receiverImage = snapshot.child("image").getValue().toString();
                    Picasso.get()
                            .load(receiverImage)
                            .placeholder(R.drawable.profile_image)
                            .into(holder.receiverProfileImage);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });

        holder.receiverMessageText.setVisibility(View.GONE);
        holder.receiverProfileImage.setVisibility(View.GONE);
        holder.senderMessageText.setVisibility(View.GONE);
        holder.messageSenderPicture.setVisibility(View.GONE);
        holder.messageReceiverPicture.setVisibility(View.GONE);

        if (fromMessageType.equals("text")) {
            if (fromUserID.equals(messageSenderId))
            {
                holder.senderMessageText.setVisibility(View.VISIBLE);
                holder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                holder.senderMessageText.setTextColor(Color.BLACK);
                holder.senderMessageText.setText(messages.getMessage() + "\n \n" + messages.getTime() + " - " + messages.getDate());
            } else {
                holder.receiverMessageText.setVisibility(View.VISIBLE);
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                holder.receiverMessageText.setTextColor(Color.BLACK);
                holder.receiverMessageText.setText(messages.getMessage() + "\n \n" + messages.getTime() + " - " + messages.getDate());
            }
        } else if (fromMessageType.equals("image")) {
            if (fromUserID.equals(messageSenderId)) {
                holder.messageSenderPicture.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageSenderPicture);
            } else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                Picasso.get().load(messages.getMessage()).into(holder.messageReceiverPicture);
            }
        } else if (fromMessageType.equals("pdf") || fromMessageType.equals("docx")) {
            if (fromUserID.equals(messageSenderId)) {
                holder.messageSenderPicture.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load("https://firebasestorage.googleapis.com/v0/b/chatapp-b40c2.firebasestorage.app/o/Image%20Files%2Ffile.png?alt=media&token=0137dfe5-7a40-4472-8f98-db24d8d96292")
                        .into(holder.messageSenderPicture);
            } else {
                holder.receiverProfileImage.setVisibility(View.VISIBLE);
                holder.messageReceiverPicture.setVisibility(View.VISIBLE);
                Picasso.get()
                        .load("https://firebasestorage.googleapis.com/v0/b/chatapp-b40c2.firebasestorage.app/o/Image%20Files%2Ffile.png?alt=media&token=0137dfe5-7a40-4472-8f98-db24d8d96292")
                        .into(holder.messageReceiverPicture);
            }
        }

        // Manejo de clics para el emisor
        if (fromUserID.equals(messageSenderId)) {
            holder.itemView.setOnClickListener(v -> {
                if (messages.getType().equals("image")) {
                    // Opciones para el emisor
                    CharSequence[] options = new CharSequence[]{"Delete for me", "Delete for Everyone", "View This Image", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if (which == 0) { // Delete for me
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForMe(messages.getMessageID(), position);
                        } else if (which == 1) { // Delete for Everyone
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForEveryone(messages.getMessageID(), position);
                        } else if (which == 2) { // View This Image
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(messages.getMessage()));
                            holder.itemView.getContext().startActivity(intent);
                        }
                    });
                    builder.show();
                } else if (messages.getType().equals("text")) {
                    // Opciones para el emisor
                    CharSequence[] options = new CharSequence[]{"Delete for me", "Delete for Everyone", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if (which == 0) { // Delete for me
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForMe(messages.getMessageID(), position);
                        } else if (which == 1) { // Delete for Everyone
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForEveryone(messages.getMessageID(), position);
                        }
                    });
                    builder.show();
                } else if (messages.getType().equals("pdf") || messages.getType().equals("docx")) {
                    // Opciones para el emisor
                    CharSequence[] options = new CharSequence[]{"Delete for me", "Delete for Everyone", "View This Document", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if (which == 0) { // Delete for me
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForMe(messages.getMessageID(), position);
                        } else if (which == 1) { // Delete for Everyone
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForEveryone(messages.getMessageID(), position);
                        } else if (which == 2) { // View This Document
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(messages.getMessage()));
                            holder.itemView.getContext().startActivity(intent);
                        }
                    });
                    builder.show();
                }
            });
        } else { // Manejo de clics para el receptor (mensajes recibidos)
            holder.itemView.setOnClickListener(v -> {
                if (messages.getType().equals("image")) {
                    // Opciones para el receptor (solo eliminar para sí mismo y ver imagen)
                    CharSequence[] options = new CharSequence[]{"Delete for me", "View This Image", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if (which == 0) { // Delete for me
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForMe(messages.getMessageID(), position);
                        } else if (which == 1) { // View This Image
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(messages.getMessage()));
                            holder.itemView.getContext().startActivity(intent);
                        }
                    });
                    builder.show();
                } else if (messages.getType().equals("text")) {
                    // Opciones para el receptor (solo eliminar para sí mismo)
                    CharSequence[] options = new CharSequence[]{"Delete for me", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if (which == 0) { // Delete for me
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForMe(messages.getMessageID(), position);
                        }
                    });
                    builder.show();
                } else if (messages.getType().equals("pdf") || messages.getType().equals("docx")) {
                    // Opciones para el receptor (solo eliminar para sí mismo y ver documento)
                    CharSequence[] options = new CharSequence[]{"Delete for me", "View This Document", "Cancel"};
                    AlertDialog.Builder builder = new AlertDialog.Builder(holder.itemView.getContext());
                    builder.setTitle("Delete Message?");
                    builder.setItems(options, (dialog, which) -> {
                        if (which == 0) { // Delete for me
                            ((ChatActivity)holder.itemView.getContext()).deleteMessageForMe(messages.getMessageID(), position);
                        } else if (which == 1) { // View This Document
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(messages.getMessage()));
                            holder.itemView.getContext().startActivity(intent);
                        }
                    });
                    builder.show();
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return userMessagesList.size();
    }
}