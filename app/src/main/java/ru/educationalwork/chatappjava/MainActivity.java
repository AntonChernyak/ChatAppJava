package ru.educationalwork.chatappjava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private RecyclerView recyclerViewMessages;
    private MessagesAdapter messagesAdapter;
    private EditText editTextMessage;
    private ImageView imageViewSendMessage;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private String author;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        messagesAdapter = new MessagesAdapter();
        editTextMessage = findViewById(R.id.editTextMessage);
        imageViewSendMessage = findViewById(R.id.imageViewSendMessage);

        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messagesAdapter);

        author = "Anton";

        imageViewSendMessage.setOnClickListener(v -> sendMessage());

        db.collection("messages")
                .orderBy("date")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<Message> messages = value.toObjects(Message.class);
                        messagesAdapter.setMessageList(messages);
                    }
                });
    }

    private void sendMessage() {
        String textOfMessage = editTextMessage.getText().toString().trim();
        if (!textOfMessage.isEmpty()) {

            recyclerViewMessages.scrollToPosition(messagesAdapter.getItemCount() - 1);

            db.collection("messages")
                    .add(new Message(author, textOfMessage, System.currentTimeMillis()))
                    .addOnSuccessListener(documentReference -> editTextMessage.setText(""))
                    .addOnFailureListener(e ->
                            Toast.makeText(MainActivity.this, getResources().getString(R.string.message_dont_send), Toast.LENGTH_SHORT).show());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null)
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            Toast.makeText(this, "Logged", Toast.LENGTH_SHORT).show();
        } else {
            signOutAction();
        }
    }

    // Меню
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.itemSignOut){
            mAuth.signOut();
            signOutAction();
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOutAction(){
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }
}