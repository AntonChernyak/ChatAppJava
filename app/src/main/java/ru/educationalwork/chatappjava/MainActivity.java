package ru.educationalwork.chatappjava;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 100; // request code
    private static final int RC_GET_IMAGE = 200;

    private RecyclerView recyclerViewMessages;
    private MessagesAdapter messagesAdapter;
    private EditText editTextMessage;
    private ImageView imageViewSendMessage;
    private ImageView imageViewAddImage;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private FirebaseStorage storage;
    private StorageReference reference;

    private String author;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
        storage = FirebaseStorage.getInstance();

        reference = storage.getReference(); // ссылка на общее хранилище

        recyclerViewMessages = findViewById(R.id.recyclerViewMessages);
        messagesAdapter = new MessagesAdapter();
        editTextMessage = findViewById(R.id.editTextMessage);
        imageViewSendMessage = findViewById(R.id.imageViewSendMessage);
        imageViewAddImage = findViewById(R.id.imageViewAddImage);

        recyclerViewMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewMessages.setAdapter(messagesAdapter);

        author = "Anton";

        imageViewSendMessage.setOnClickListener(v -> sendMessage(editTextMessage.getText().toString().trim(), null));
        imageViewAddImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true); // только с локального хранилища
            startActivityForResult(intent, RC_GET_IMAGE);
        });
    }

    private void sendMessage(String textOfMessage, String urlToImage) {

        Message message = null;
        if (textOfMessage != null && !textOfMessage.isEmpty()) {
            message = new Message(author, textOfMessage, System.currentTimeMillis(), null);
        } else if (urlToImage != null && !urlToImage.isEmpty()) {
            message = new Message(author, null, System.currentTimeMillis(), urlToImage);
        }

        db.collection("messages")
                .add(message)
                .addOnSuccessListener(documentReference -> {
                    editTextMessage.setText("");
                    recyclerViewMessages.scrollToPosition(messagesAdapter.getItemCount() - 1);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.message_dont_send), Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onResume() {
        db.collection("messages")
                .orderBy("date")
                .addSnapshotListener((value, error) -> {
                    if (value != null) {
                        List<Message> messages = value.toObjects(Message.class);
                        messagesAdapter.setMessageList(messages);
                        recyclerViewMessages.scrollToPosition(messagesAdapter.getItemCount() - 1);
                    }
                });
        super.onResume();
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
        if (item.getItemId() == R.id.itemSignOut) {
            mAuth.signOut();
            signOutAction();
        }
        return super.onOptionsItemSelected(item);
    }

    private void signOutAction() {
        AuthUI.getInstance().signOut(this).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // отправим пользователя на стр регистрации

                // Choose authentication providers
                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.EmailBuilder().build(),
                        new AuthUI.IdpConfig.GoogleBuilder().build());

                // Create and launch sign-in intent
                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .build(),
                        RC_SIGN_IN);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GET_IMAGE && resultCode == RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData(); // адрес изображения на телефоне
                if (uri != null) {
                    StorageReference referenceImages = reference.child("images/" + uri.getLastPathSegment()); // ссылка на директорию images в общем хранилще
                    referenceImages.putFile(uri).continueWithTask(task -> {
                        if (!task.isSuccessful()) {
                            throw task.getException();
                        }
                        return referenceImages.getDownloadUrl();
                    }).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Uri downloadUri = task.getResult(); // адрес изображения в Firebase
                            if (downloadUri != null) {
                                sendMessage(null, String.valueOf(downloadUri));
                            }
                        } else {
                            // Fail
                        }
                    });

                }
            }
        }

        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                // Successfully signed in
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    Toast.makeText(this, user.getEmail(), Toast.LENGTH_SHORT).show();
                    author = user.getEmail();
                }
            } else {
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                if (response != null) {
                    Toast.makeText(this, "Error: " + response.getError(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Error", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}