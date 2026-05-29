package com.example.tocadospeludos;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!UserStorage.isLoggedIn(this)) {
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView welcomeText = findViewById(R.id.welcomeText);
        String userName = UserStorage.getCurrentUserName(this);
        if (userName != null) {
            welcomeText.setText("Olá, " + userName + "! Bem-vindo ao seu painel");
        }

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);
        View homeContent = findViewById(R.id.homeContent);
        View profileContent = findViewById(R.id.profileContent);
        View settingsContent = findViewById(R.id.settingsContent);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            homeContent.setVisibility(itemId == R.id.nav_home ? View.VISIBLE : View.GONE);
            profileContent.setVisibility(itemId == R.id.nav_profile ? View.VISIBLE : View.GONE);
            settingsContent.setVisibility(itemId == R.id.nav_settings ? View.VISIBLE : View.GONE);
            return true;
        });
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        TextView documentStatus = findViewById(R.id.tvDocumentStatus);
        boolean docsComplete = UserStorage.hasUserDocuments(this, UserStorage.getCurrentUserEmail(this));
        documentStatus.setText(docsComplete ? "Status dos documentos: concluído" : "Status dos documentos: pendente");

        Button completeButton = findViewById(R.id.btnCompleteAccount);
        completeButton.setOnClickListener(v -> bottomNavigation.setSelectedItemId(R.id.nav_profile));

        Button fillDocsButton = findViewById(R.id.btnFillDocuments);
        fillDocsButton.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, DocumentActivity.class));
        });

        Button logoutButton = findViewById(R.id.btnLogout);
        logoutButton.setOnClickListener(v -> {
            UserStorage.logout(this);
            Toast.makeText(this, "Desconectando...", Toast.LENGTH_SHORT).show();
            finish();
        });

        Button deleteButton = findViewById(R.id.btnDeleteAccount);
        deleteButton.setOnClickListener(v -> {
            String currentEmail = UserStorage.getCurrentUserEmail(this);
            if (UserStorage.deleteAccount(this, currentEmail)) {
                Toast.makeText(this, "Conta deletada", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, "Falha ao deletar conta", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        TextView documentStatus = findViewById(R.id.tvDocumentStatus);
        boolean docsComplete = UserStorage.hasUserDocuments(this, UserStorage.getCurrentUserEmail(this));
        documentStatus.setText(docsComplete ? "Status dos documentos: concluído" : "Status dos documentos: pendente");
    }
}
