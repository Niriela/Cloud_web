package com.cloudweb.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
public class FirebaseConfig {

    @Value("${firebase.service-account}")
    private String serviceAccountPath;

    @Value("${app.firebase.project-id:}")
    private String projectId;

    @Value("${app.firebase.private-key-id:}")
    private String privateKeyId;

    @Value("${app.firebase.private-key:}")
    private String privateKey;

    @Value("${app.firebase.client-email:}")
    private String clientEmail;

    @Value("${app.firebase.client-id:}")
    private String clientId;

    @Value("${app.firebase.client-x509-cert-url:}")
    private String clientX509CertUrl;

    @Bean
    @ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
    public Firestore firestore() throws IOException {
        if (FirebaseApp.getApps().isEmpty()) {
            try (InputStream serviceAccount = resolveServiceAccountStream()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                        .build();
                FirebaseApp.initializeApp(options);
            }
        }
        return FirestoreClient.getFirestore();
    }

    private InputStream resolveServiceAccountStream() throws IOException {
        if (serviceAccountPath != null && !serviceAccountPath.isBlank()) {
            Path filePath = Path.of(serviceAccountPath);
            if (Files.exists(filePath)) {
                return new FileInputStream(filePath.toFile());
            }
        }

        if (hasInlineCredentials()) {
            String normalizedPrivateKey = privateKey.replace("\\n", "\n");
            String json = """
                    {
                      "type": "service_account",
                      "project_id": "%s",
                      "private_key_id": "%s",
                      "private_key": "%s",
                      "client_email": "%s",
                      "client_id": "%s",
                      "auth_uri": "https://accounts.google.com/o/oauth2/auth",
                      "token_uri": "https://oauth2.googleapis.com/token",
                      "auth_provider_x509_cert_url": "https://www.googleapis.com/oauth2/v1/certs",
                      "client_x509_cert_url": "%s"
                    }
                    """.formatted(
                    escapeJson(projectId),
                    escapeJson(privateKeyId),
                    escapeJson(normalizedPrivateKey),
                    escapeJson(clientEmail),
                    escapeJson(clientId),
                    escapeJson(clientX509CertUrl)
            );
            return new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));
        }

        throw new IllegalStateException(
                "Firebase enabled but no valid service account source found. Provide FIREBASE_SERVICE_ACCOUNT file or FIREBASE_* credentials.");
    }

    private boolean hasInlineCredentials() {
        return isFilled(projectId)
                && isFilled(privateKeyId)
                && isFilled(privateKey)
                && isFilled(clientEmail)
                && isFilled(clientId)
                && isFilled(clientX509CertUrl);
    }

    private boolean isFilled(String value) {
        return value != null && !value.isBlank();
    }

    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
