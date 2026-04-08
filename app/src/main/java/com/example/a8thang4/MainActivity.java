package com.example.a8thang4;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvMovies;
    private MovieAdapter adapter;
    private List<Movie> movieList;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FloatingActionButton btnLogout;
    private ExtendedFloatingActionButton btnViewTickets;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
            return;
        }

        db = FirebaseFirestore.getInstance();
        rvMovies = findViewById(R.id.rvMovies);
        btnLogout = findViewById(R.id.btnLogout);
        btnViewTickets = findViewById(R.id.btnViewTickets);

        movieList = new ArrayList<>();
        adapter = new MovieAdapter(movieList, movie -> showBookingDialog(movie));

        rvMovies.setLayoutManager(new LinearLayoutManager(this));
        rvMovies.setAdapter(adapter);

        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        btnViewTickets.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, MyTicketsActivity.class));
        });

        requestNotificationPermission();
        loadMovies();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
    }

    private void loadMovies() {
        db.collection("movies").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                movieList.clear();
                for (DocumentSnapshot document : task.getResult()) {
                    Movie movie = document.toObject(Movie.class);
                    if (movie != null) {
                        movie.setId(document.getId());
                        movieList.add(movie);
                    }
                }
                if (movieList.isEmpty()) {
                    addSampleData();
                }
                adapter.notifyDataSetChanged();
            } else {
                Toast.makeText(MainActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addSampleData() {
        String[] titles = {"Avengers: Endgame", "Interstellar", "Inception", "Joker"};
        String[] genres = {"Action", "Sci-Fi", "Sci-Fi", "Drama"};
        String[] durations = {"181 mins", "169 mins", "148 mins", "122 mins"};

        for (int i = 0; i < titles.length; i++) {
            Movie movie = new Movie("", titles[i], genres[i], durations[i], "", "Description for " + titles[i]);
            db.collection("movies").add(movie);
        }

        // Add sample theaters
        Theater theater = new Theater("", "CGV Cinema", "Vincom Center");
        db.collection("theaters").add(theater);
        loadMovies();
    }

    private void showBookingDialog(Movie movie) {
        String[] showtimes = {"10:00 AM", "01:30 PM", "04:45 PM", "08:00 PM"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn suất chiếu cho " + movie.getTitle());
        builder.setItems(showtimes, (dialog, which) -> {
            String selectedTime = showtimes[which];
            bookTicket(movie, selectedTime);
        });
        builder.show();
    }

    private void bookTicket(Movie movie, String showtime) {
        String userId = mAuth.getCurrentUser().getUid();
        Map<String, Object> ticket = new HashMap<>();
        ticket.put("userId", userId);
        ticket.put("movieId", movie.getId());
        ticket.put("movieTitle", movie.getTitle());
        ticket.put("showtime", showtime);
        ticket.put("bookingDate", System.currentTimeMillis());
        ticket.put("status", "Confirmed");

        db.collection("tickets").add(ticket)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(MainActivity.this, "Đặt vé thành công: " + movie.getTitle() + " lúc " + showtime, Toast.LENGTH_LONG).show();
                    
                    // Gửi thông báo nhắc nhở lên thanh thông báo
                    NotificationHelper.showNotification(MainActivity.this, 
                        "Nhắc lịch xem phim", 
                        "Bạn đã đặt vé phim " + movie.getTitle() + " vào lúc " + showtime + ". Đừng quên nhé!");
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(MainActivity.this, "Lỗi đặt vé: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
