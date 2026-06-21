package com.example.week1task1;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "RealtimeDBApp";

    public static class Note {
        public String id = ""; 
        public String title = "";
        public String body = "";
        public String date = "";
        public String userId = "";
        public int color;

        public Note() {}

        public Note(String id, String title, String body, String date, int color, String userId) {
            this.id = (id != null) ? id : "";
            this.title = (title != null) ? title : "";
            this.body = (body != null) ? body : "";
            this.date = (date != null) ? date : "";
            this.color = color;
            this.userId = (userId != null) ? userId : "";
        }
    }

    private DatabaseReference userNotesRef;
    private ValueEventListener notesListener;
    private String currentUserId;
    private NotesDatabaseHelper dbHelper;
    private List<Note> notesList = new ArrayList<>();
    private List<Note> filteredList = new ArrayList<>();
    private Note currentNote = null; 

    private View screenHome, screenAdd, screenView, screenSettings;
    private RecyclerView recyclerView;
    private NoteAdapter adapter;
    private LinearLayout emptyState;
    private ImageView emptyStateIcon;
    private TextView tvEmptyTitle, tvEmptyDesc;
    private TextView tvOfflineIndicator;
    private EditText inputTitle, inputBody, inputDate;
    private TextView displayTitle, displayDate, displayBody;
    private View displayTagColor;
    private TextView footerPageName, tvUserEmail, tvAppVersion;
    private EditText searchBar;
    private ImageButton btnSettings, btnBackFromSettings;
    private SwitchMaterial switchTheme;
    private SwipeRefreshLayout swipeRefreshLayout;
    
    private int selectedColor;
    private boolean isGridView = false;
    private FirebaseAuth mAuth;
    private SharedPreferences themePrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        themePrefs = getSharedPreferences("ThemePrefs", MODE_PRIVATE);
        boolean isDarkMode = themePrefs.getBoolean("isDarkMode", false);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

        super.onCreate(savedInstanceState);
        
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            goToLogin();
            return;
        }
        
        currentUserId = currentUser.getUid();
        
        try {
            FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        } catch (Exception ignored) {}
        
        userNotesRef = FirebaseDatabase.getInstance("https://zaigham-3653c-default-rtdb.firebaseio.com/")
                .getReference("notes").child(currentUserId);
        userNotesRef.keepSynced(true);
        
        dbHelper = new NotesDatabaseHelper(this);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        selectedColor = getResources().getColor(R.color.tag_blue);

        initViews();
        setupRecyclerView();
        setupListeners();
        setupRealTimeSync();
        setupNetworkListener();
        
        tvUserEmail.setText(currentUser.getEmail());
        setAppVersion();
        switchTheme.setChecked(isDarkMode);
        
        migrateLocalNotes();
    }

    private void setAppVersion() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            tvAppVersion.setText("Version " + pInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            tvAppVersion.setText("Version 1.0.0");
        }
    }

    private void goToLogin() {
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    private void migrateLocalNotes() {
        List<Note> localNotes = dbHelper.getAllNotes();
        if (localNotes.isEmpty()) return;

        for (Note localNote : localNotes) {
            DatabaseReference newRef = userNotesRef.push();
            Note firebaseNote = new Note(newRef.getKey(), localNote.title, localNote.body, localNote.date, localNote.color, currentUserId);
            newRef.setValue(firebaseNote);
        }
        dbHelper.clearAllNotes();
    }

    private void setupRealTimeSync() {
        if (notesListener != null) userNotesRef.removeEventListener(notesListener);

        notesListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notesList.clear();
                for (DataSnapshot postSnapshot : snapshot.getChildren()) {
                    Note note = postSnapshot.getValue(Note.class);
                    if (note != null) {
                        note.id = postSnapshot.getKey(); 
                        notesList.add(note);
                    }
                }
                filterNotes(searchBar.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                    Toast.makeText(MainActivity.this, "Cloud Sync Error: Permission Denied", Toast.LENGTH_LONG).show();
                }
                Log.e(TAG, "Database error: " + error.getMessage());
            }
        };
        userNotesRef.addValueEventListener(notesListener);
    }

    private void saveNote() {
        String title = inputTitle.getText().toString().trim();
        String body = inputBody.getText().toString().trim();
        String date = inputDate.getText().toString().trim();
        
        if (title.isEmpty() && body.isEmpty()) {
            changeScreenVisibility(screenHome, "HOME");
            return;
        }

        findViewById(R.id.btn_save_note).setEnabled(false);

        if (currentNote == null) {
            DatabaseReference newRef = userNotesRef.push();
            Note note = new Note(newRef.getKey(), title, body, date, selectedColor, currentUserId);
            newRef.setValue(note).addOnCompleteListener(task -> {
                findViewById(R.id.btn_save_note).setEnabled(true);
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Note saved", Toast.LENGTH_SHORT).show();
                    changeScreenVisibility(screenHome, "HOME");
                }
            });
        } else {
            Note note = new Note(currentNote.id, title, body, date, selectedColor, currentUserId);
            userNotesRef.child(currentNote.id).setValue(note).addOnCompleteListener(task -> {
                findViewById(R.id.btn_save_note).setEnabled(true);
                if (task.isSuccessful()) {
                    changeScreenVisibility(screenHome, "HOME");
                }
            });
        }
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Delete this note from the cloud?")
                .setPositiveButton("Delete", (d, w) -> {
                    if (currentNote != null) {
                        userNotesRef.child(currentNote.id).removeValue();
                        changeScreenVisibility(screenHome, "HOME");
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void setupNetworkListener() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build();

        cm.registerNetworkCallback(request, new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> tvOfflineIndicator.setVisibility(View.GONE));
            }
            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> tvOfflineIndicator.setVisibility(View.VISIBLE));
            }
        });
        
        NetworkCapabilities caps = cm.getNetworkCapabilities(cm.getActiveNetwork());
        boolean online = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
        tvOfflineIndicator.setVisibility(online ? View.GONE : View.VISIBLE);
    }

    private void initViews() {
        screenHome = findViewById(R.id.screen_home);
        screenAdd = findViewById(R.id.screen_add);
        screenView = findViewById(R.id.screen_view);
        screenSettings = findViewById(R.id.screen_settings);
        
        recyclerView = findViewById(R.id.notes_recycler_view);
        emptyState = findViewById(R.id.empty_state);
        emptyStateIcon = findViewById(R.id.empty_state_icon);
        tvEmptyTitle = findViewById(R.id.tv_empty_title);
        tvEmptyDesc = findViewById(R.id.tv_empty_desc);

        inputTitle = findViewById(R.id.input_title);
        inputBody = findViewById(R.id.input_body);
        inputDate = findViewById(R.id.input_date);
        displayTitle = findViewById(R.id.display_title);
        displayDate = findViewById(R.id.display_date);
        displayBody = findViewById(R.id.display_body);
        displayTagColor = findViewById(R.id.display_tag_color);
        footerPageName = findViewById(R.id.footer_page_name);
        searchBar = findViewById(R.id.search_bar);
        tvUserEmail = findViewById(R.id.tv_user_email);
        tvAppVersion = findViewById(R.id.tv_app_version);
        btnSettings = findViewById(R.id.btn_settings);
        btnBackFromSettings = findViewById(R.id.btn_back_from_settings);
        switchTheme = findViewById(R.id.switch_theme);
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        tvOfflineIndicator = findViewById(R.id.tv_offline_indicator);
    }

    private void setupListeners() {
        findViewById(R.id.btn_goto_add).setOnClickListener(v -> {
            currentNote = null;
            clearFormInputs();
            selectedColor = getResources().getColor(R.color.tag_blue);
            inputDate.setText(new SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(new Date()));
            changeScreenVisibility(screenAdd, "CREATE NOTE");
        });

        findViewById(R.id.btn_cancel_add).setOnClickListener(v -> changeScreenVisibility(screenHome, "HOME"));
        findViewById(R.id.btn_back_home).setOnClickListener(v -> changeScreenVisibility(screenHome, "HOME"));
        findViewById(R.id.btn_save_note).setOnClickListener(v -> saveNote());
        
        findViewById(R.id.btn_edit_note).setOnClickListener(v -> {
            if (currentNote != null) {
                inputTitle.setText(currentNote.title);
                inputBody.setText(currentNote.body);
                inputDate.setText(currentNote.date);
                selectedColor = currentNote.color;
                changeScreenVisibility(screenAdd, "EDIT NOTE");
            }
        });

        findViewById(R.id.btn_delete_note).setOnClickListener(v -> showDeleteConfirmation());
        findViewById(R.id.btn_toggle_view).setOnClickListener(v -> toggleLayoutView());
        
        btnSettings.setOnClickListener(v -> changeScreenVisibility(screenSettings, "SETTINGS"));
        btnBackFromSettings.setOnClickListener(v -> changeScreenVisibility(screenHome, "HOME"));
        
        findViewById(R.id.btn_logout_settings).setOnClickListener(v -> {
            mAuth.signOut();
            goToLogin();
        });

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            themePrefs.edit().putBoolean("isDarkMode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(isChecked ? 
                    AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { filterNotes(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });

        searchBar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                hideKeyboard();
                return true;
            }
            return false;
        });

        swipeRefreshLayout.setOnRefreshListener(() -> {
            swipeRefreshLayout.setRefreshing(false);
        });

        setupColorPickers();
    }

    private void hideKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    private void setupRecyclerView() {
        adapter = new NoteAdapter(filteredList, note -> {
            currentNote = note;
            displayTitle.setText(note.title == null || note.title.isEmpty() ? "Untitled" : note.title);
            displayDate.setText(note.date);
            displayBody.setText(note.body == null || note.body.isEmpty() ? "" : note.body);
            displayTagColor.setBackgroundColor(note.color);
            changeScreenVisibility(screenView, "VIEW NOTE");
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void filterNotes(String query) {
        String lowerCaseQuery = (query != null) ? query.toLowerCase().trim() : "";
        
        filteredList.clear();
        for (Note note : notesList) {
            if (note == null) continue;
            
            String title = (note.title != null) ? note.title.toLowerCase() : "";
            String body = (note.body != null) ? note.body.toLowerCase() : "";
            
            if (title.contains(lowerCaseQuery) || body.contains(lowerCaseQuery)) {
                filteredList.add(note);
            }
        }
        
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        
        updateEmptyState(lowerCaseQuery);
    }

    private void updateEmptyState(String query) {
        boolean isEmpty = filteredList.isEmpty();
        recyclerView.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        
        if (isEmpty) {
            if (!query.isEmpty()) {
                tvEmptyTitle.setText("No results found");
                tvEmptyDesc.setText("Try searching for something else");
                emptyStateIcon.setImageResource(android.R.drawable.ic_menu_search);
            } else {
                tvEmptyTitle.setText("No notes yet");
                tvEmptyDesc.setText("Tap the + button to create your first note");
                emptyStateIcon.setImageResource(android.R.drawable.ic_menu_edit);
            }
        }
    }

    private void toggleLayoutView() {
        isGridView = !isGridView;
        recyclerView.setLayoutManager(isGridView ? new GridLayoutManager(this, 2) : new LinearLayoutManager(this));
        ((ImageButton)findViewById(R.id.btn_toggle_view)).setImageResource(isGridView ? 
                android.R.drawable.ic_menu_sort_by_size : android.R.drawable.ic_dialog_dialer);
    }

    private void changeScreenVisibility(View activeScreen, String pageName) {
        screenHome.setVisibility(View.GONE);
        screenAdd.setVisibility(View.GONE);
        screenView.setVisibility(View.GONE);
        screenSettings.setVisibility(View.GONE);
        activeScreen.setVisibility(View.VISIBLE);
        footerPageName.setText(pageName);
        if (activeScreen == screenHome) {
            filterNotes(searchBar.getText().toString());
        }
    }

    private void setupColorPickers() {
        findViewById(R.id.color_blue).setOnClickListener(v -> selectedColor = getResources().getColor(R.color.tag_blue));
        findViewById(R.id.color_green).setOnClickListener(v -> selectedColor = getResources().getColor(R.color.tag_green));
        findViewById(R.id.color_yellow).setOnClickListener(v -> selectedColor = getResources().getColor(R.color.tag_yellow));
        findViewById(R.id.color_red).setOnClickListener(v -> selectedColor = getResources().getColor(R.color.tag_red));
        findViewById(R.id.color_purple).setOnClickListener(v -> selectedColor = getResources().getColor(R.color.tag_purple));
        findViewById(R.id.color_gray).setOnClickListener(v -> selectedColor = getResources().getColor(R.color.tag_gray));
    }

    private void clearFormInputs() {
        inputTitle.setText(""); inputBody.setText(""); inputDate.setText("");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (notesListener != null && userNotesRef != null) {
            userNotesRef.removeEventListener(notesListener);
        }
    }
}
