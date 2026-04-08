package com.example.a8thang4;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyTicketsActivity extends AppCompatActivity {

    private RecyclerView rvMyTickets;
    private TicketAdapter adapter;
    private List<Ticket> ticketList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_tickets);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        
        rvMyTickets = findViewById(R.id.rvMyTickets);
        ticketList = new ArrayList<>();
        adapter = new TicketAdapter(ticketList);

        rvMyTickets.setLayoutManager(new LinearLayoutManager(this));
        rvMyTickets.setAdapter(adapter);

        loadMyTickets();
    }

    private void loadMyTickets() {
        if (mAuth.getCurrentUser() == null) return;
        
        String userId = mAuth.getCurrentUser().getUid();

        // Lấy danh sách vé đơn giản (bỏ orderBy để tránh lỗi Index)
        db.collection("tickets")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        ticketList.clear();
                        for (DocumentSnapshot document : task.getResult()) {
                            Ticket ticket = document.toObject(Ticket.class);
                            if (ticket != null) {
                                ticket.setId(document.getId());
                                ticketList.add(ticket);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (ticketList.isEmpty()) {
                            Toast.makeText(this, "Bạn chưa có vé nào!", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "Lỗi tải vé: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }
}
