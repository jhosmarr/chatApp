package com.example.chatapp;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;

public class RequestsFragment extends Fragment {

    private View RequestsFragmentView;
    private RecyclerView myRequestsList;
    private TextView noRequestsText;

    private DatabaseReference chatRequestsRef, usersRef;
    private FirebaseAuth mAuth;
    private String currentUserID;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        RequestsFragmentView = inflater.inflate(R.layout.fragment_requests, container, false);

        myRequestsList = RequestsFragmentView.findViewById(R.id.requests_list);
        noRequestsText = RequestsFragmentView.findViewById(R.id.no_requests_text);

        myRequestsList.setLayoutManager(new LinearLayoutManager(getContext()));

        mAuth = FirebaseAuth.getInstance();
        currentUserID = mAuth.getCurrentUser().getUid();
        Log.d("DEBUG", "Usuario actual: " + currentUserID);

        chatRequestsRef = FirebaseDatabase.getInstance().getReference().child("ChatRequests").child(currentUserID);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return RequestsFragmentView;
    }

    @Override
    public void onStart() {
        super.onStart();

        Query query = chatRequestsRef.orderByChild("request_type").equalTo("received");

        FirebaseRecyclerOptions<ChatRequest> options =
                new FirebaseRecyclerOptions.Builder<ChatRequest>()
                        .setQuery(query, ChatRequest.class)
                        .build();

        FirebaseRecyclerAdapter<ChatRequest, RequestsViewHolder> adapter =
                new FirebaseRecyclerAdapter<ChatRequest, RequestsViewHolder>(options) {

                    @Override
                    protected void onBindViewHolder(@NonNull RequestsViewHolder holder, int position, @NonNull ChatRequest model) {
                        String userId = getRef(position).getKey();

                        if ("received".equals(model.getRequest_type())) {
                            usersRef.child(userId).get().addOnSuccessListener(dataSnapshot -> {
                                if (dataSnapshot.exists()) {
                                    String name = dataSnapshot.child("name").getValue(String.class);
                                    String status = dataSnapshot.child("status").getValue(String.class);

                                    holder.userName.setText(name);
                                    holder.userStatus.setText(status);

                                    // Mostrar botones
                                    holder.acceptButton.setVisibility(View.VISIBLE);
                                    holder.cancelButton.setVisibility(View.VISIBLE);

                                    // Listener para aceptar solicitud con diálogo
                                    holder.acceptButton.setOnClickListener(v -> {
                                        new AlertDialog.Builder(requireContext())
                                                .setTitle("Aceptar solicitud")
                                                .setMessage("¿Deseas aceptar la solicitud de " + name + "?")
                                                .setPositiveButton("Aceptar", (dialog, which) -> {
                                                    DatabaseReference contactsRef = FirebaseDatabase.getInstance().getReference().child("Contacts");

                                                    contactsRef.child(currentUserID).child(userId).setValue("Saved").addOnCompleteListener(task1 -> {
                                                        if (task1.isSuccessful()) {
                                                            contactsRef.child(userId).child(currentUserID).setValue("Saved").addOnCompleteListener(task2 -> {
                                                                if (task2.isSuccessful()) {
                                                                    chatRequestsRef.child(userId).removeValue().addOnCompleteListener(task3 -> {
                                                                        if (task3.isSuccessful()) {
                                                                            DatabaseReference otherUserRequestsRef = FirebaseDatabase.getInstance()
                                                                                    .getReference().child("ChatRequests").child(userId).child(currentUserID);
                                                                            otherUserRequestsRef.removeValue().addOnCompleteListener(task4 -> {
                                                                                if (task4.isSuccessful()) {
                                                                                    Log.d("DEBUG", "Solicitud aceptada y contactos actualizados correctamente");
                                                                                } else {
                                                                                    Log.e("DEBUG", "Error al eliminar solicitud del otro usuario");
                                                                                }
                                                                            });
                                                                        } else {
                                                                            Log.e("DEBUG", "Error al eliminar solicitud local");
                                                                        }
                                                                    });
                                                                } else {
                                                                    Log.e("DEBUG", "Error al guardar contacto en userId");
                                                                }
                                                            });
                                                        } else {
                                                            Log.e("DEBUG", "Error al guardar contacto en currentUserID");
                                                        }
                                                    });
                                                })
                                                .setNegativeButton("Cancelar", null)
                                                .show();
                                    });

                                    // Listener para cancelar solicitud con diálogo
                                    holder.cancelButton.setOnClickListener(v -> {
                                        new AlertDialog.Builder(requireContext())
                                                .setTitle("Cancelar solicitud")
                                                .setMessage("¿Estás seguro que deseas cancelar la solicitud de " + name + "?")
                                                .setPositiveButton("Sí", (dialog, which) -> {
                                                    chatRequestsRef.child(userId).removeValue()
                                                            .addOnSuccessListener(aVoid -> Log.d("DEBUG", "Solicitud cancelada correctamente"))
                                                            .addOnFailureListener(e -> Log.e("DEBUG", "Error al cancelar solicitud", e));
                                                })
                                                .setNegativeButton("No", null)
                                                .show();
                                    });
                                }
                            });
                        } else {
                            // Si no es recibido, ocultar ítem
                            holder.itemView.setVisibility(View.GONE);
                            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 0));
                        }
                    }

                    @NonNull
                    @Override
                    public RequestsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                        View view = LayoutInflater.from(parent.getContext())
                                .inflate(R.layout.users_display_layout, parent, false);
                        return new RequestsViewHolder(view);
                    }

                    @Override
                    public void onDataChanged() {
                        super.onDataChanged();
                        if (getItemCount() == 0) {
                            noRequestsText.setVisibility(View.VISIBLE);
                            myRequestsList.setVisibility(View.GONE);
                        } else {
                            noRequestsText.setVisibility(View.GONE);
                            myRequestsList.setVisibility(View.VISIBLE);
                        }
                    }
                };

        myRequestsList.setAdapter(adapter);
        adapter.startListening();
    }

    public static class RequestsViewHolder extends RecyclerView.ViewHolder {
        TextView userName, userStatus;
        Button acceptButton, cancelButton;

        public RequestsViewHolder(@NonNull View itemView) {
            super(itemView);
            userName = itemView.findViewById(R.id.users_profile_name);
            userStatus = itemView.findViewById(R.id.user_status);
            acceptButton = itemView.findViewById(R.id.request_accept_btn);
            cancelButton = itemView.findViewById(R.id.request_cancel_btn);
        }
    }
}
