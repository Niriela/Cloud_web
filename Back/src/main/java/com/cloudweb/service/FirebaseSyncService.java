package com.cloudweb.service;

import com.cloudweb.entity.Entreprise;
import com.cloudweb.entity.HistoriqueSignalements;
import com.cloudweb.entity.HistoriqueUsers;
import com.cloudweb.entity.PhotoSignalement;
import com.cloudweb.entity.Point;
import com.cloudweb.entity.ReglesGestion;
import com.cloudweb.entity.Signalements;
import com.cloudweb.entity.Statuts;
import com.cloudweb.entity.StatutsUser;
import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.entity.User;
import com.cloudweb.entity.UserType;
import com.cloudweb.repository.EntrepriseRepository;
import com.cloudweb.repository.HistoriqueSignalementsRepository;
import com.cloudweb.repository.HistoriqueUsersRepository;
import com.cloudweb.repository.PhotoSignalementRepository;
import com.cloudweb.repository.PointRepository;
import com.cloudweb.repository.ReglesGestionRepository;
import com.cloudweb.repository.SignalementsRepository;
import com.cloudweb.repository.StatutsRepository;
import com.cloudweb.repository.StatutsUserRepository;
import com.cloudweb.repository.TypeSignalementRepository;
import com.cloudweb.repository.UserRepository;
import com.cloudweb.repository.UserTypeRepository;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import com.google.cloud.Timestamp;
import com.google.firebase.auth.AuthErrorCode;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.FirebaseApp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@ConditionalOnProperty(prefix = "app.firebase", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class FirebaseSyncService {

    private final Firestore firestore;
    private final TypeSignalementRepository typeSignalementRepository;
    private final StatutsRepository statutsRepository;
    private final EntrepriseRepository entrepriseRepository;
    private final PointRepository pointRepository;
    private final SignalementsRepository signalementsRepository;
    private final UserRepository userRepository;
    private final StatutsUserRepository statutsUserRepository;
    private final UserTypeRepository userTypeRepository;
    private final ReglesGestionRepository reglesGestionRepository;
    private final HistoriqueSignalementsRepository historiqueSignalementsRepository;
    private final HistoriqueUsersRepository historiqueUsersRepository;
    private final PhotoSignalementRepository photoSignalementRepository;
    private final JdbcTemplate jdbcTemplate;

    private Map<String, DocumentReference> remotePointByCoord = new HashMap<>();
    private Map<Long, DocumentReference> remotePointById = new HashMap<>();
    private Map<String, DocumentReference> remoteSignalementByKey = new HashMap<>();
    private Map<Long, DocumentReference> remoteSignalementById = new HashMap<>();
    private Map<Long, DocumentReference> remoteUserById = new HashMap<>();
    private Map<String, DocumentReference> remoteUserByEmail = new HashMap<>();
    private Map<String, DocumentReference> remoteUserByFirebaseId = new HashMap<>();
    private Map<String, RemoteUserSnapshot> remoteUserByDocPath = new HashMap<>();

    private record RemoteUserSnapshot(String email, String password, String firebaseId) {
    }

    public Map<String, Object> firebaseHealthCheck() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("checkedAtUtc", Instant.now().toString());
        report.put("javaTimeZone", java.time.ZoneId.systemDefault().toString());

        report.put("firebaseAppsInitialized", !FirebaseApp.getApps().isEmpty());
        report.put("firebaseAppsCount", FirebaseApp.getApps().size());

        Map<String, Object> dns = new LinkedHashMap<>();
        try {
            InetAddress[] addresses = InetAddress.getAllByName("oauth2.googleapis.com");
            String[] resolved = new String[addresses.length];
            for (int i = 0; i < addresses.length; i++) {
                resolved[i] = addresses[i].getHostAddress();
            }
            dns.put("ok", true);
            dns.put("resolvedIps", resolved);
        } catch (Exception ex) {
            dns.put("ok", false);
            dns.put("error", ex.getMessage());
        }
        report.put("dnsOauth2Googleapis", dns);

        Map<String, Object> firestoreCheck = new LinkedHashMap<>();
        try {
            firestore.collection("users").limit(1).get().get();
            firestoreCheck.put("ok", true);
        } catch (Exception ex) {
            firestoreCheck.put("ok", false);
            firestoreCheck.put("error", flattenExceptionMessage(ex));
        }
        report.put("firestore", firestoreCheck);

        Map<String, Object> authCheck = new LinkedHashMap<>();
        try {
            String probeUid = "healthcheck-nonexistent-uid-" + Instant.now().toEpochMilli();
            FirebaseAuth.getInstance().getUser(probeUid);
            authCheck.put("ok", true);
            authCheck.put("note", "Unexpectedly found probe uid");
        } catch (FirebaseAuthException ex) {
            if (isUserNotFound(ex)) {
                authCheck.put("ok", true);
                authCheck.put("note", "Auth reachable (probe uid not found as expected)");
            } else {
                authCheck.put("ok", false);
                authCheck.put("error", flattenExceptionMessage(ex));
                if (ex.getAuthErrorCode() != null) {
                    authCheck.put("authErrorCode", ex.getAuthErrorCode().name());
                }
            }
        } catch (Exception ex) {
            authCheck.put("ok", false);
            authCheck.put("error", flattenExceptionMessage(ex));
        }
        report.put("firebaseAuth", authCheck);

        boolean firestoreOk = Boolean.TRUE.equals(firestoreCheck.get("ok"));
        boolean authOk = Boolean.TRUE.equals(authCheck.get("ok"));
        boolean dnsOk = Boolean.TRUE.equals(dns.get("ok"));
        boolean appOk = Boolean.TRUE.equals(report.get("firebaseAppsInitialized"));
        report.put("overallOk", firestoreOk && authOk && dnsOk && appOk);

        return report;
    }

    public Map<String, Long> getLocalCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("statuts_user", statutsUserRepository.count());
        counts.put("user_type", userTypeRepository.count());
        counts.put("regles_gestion", reglesGestionRepository.count());
        counts.put("type_signalements", typeSignalementRepository.count());
        counts.put("statuts", statutsRepository.count());
        counts.put("entreprises", entrepriseRepository.count());
        counts.put("points", pointRepository.count());
        counts.put("users", userRepository.count());
        counts.put("signalements", signalementsRepository.count());
        counts.put("historique_signalements", historiqueSignalementsRepository.count());
        counts.put("historique_users", historiqueUsersRepository.count());
        return counts;
    }

    public Map<String, Long> getRemoteCounts() {
        Map<String, Long> counts = new HashMap<>();
        counts.put("statuts_user", countCollection("statuts_user"));
        counts.put("user_type", countCollection("user_type"));
        counts.put("regles_gestion", countCollection("regles_gestion"));
        counts.put("type_signalements", countCollection("type_signalements"));
        counts.put("statuts", countCollection("statuts"));
        counts.put("entreprises", countCollection("entreprises"));
        counts.put("points", countCollection("points"));
        counts.put("users", countCollection("users"));
        counts.put("signalements", countCollection("signalements"));
        counts.put("historique_signalements", countCollection("historique_signalements"));
        counts.put("historique_users", countCollection("historique_users"));
        return counts;
    }

    public void pushAllToFirebase() {
        WriteBatch batch = firestore.batch();
        prepareRemoteIndexes(batch);

        for (StatutsUser statutsUser : statutsUserRepository.findAll()) {
            if (!shouldPush("statuts_user", statutsUser.getId(), statutsUser.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemoteDoc("statuts_user", statutsUser.getId(), batch);
            batch.set(ref, Map.of(
                    "id", statutsUser.getId(),
                    "libelle", statutsUser.getLibelle(),
                    "updated_at", formatDate(statutsUser.getUpdatedAt())
            ));
        }

        for (UserType userType : userTypeRepository.findAll()) {
            if (!shouldPush("user_type", userType.getId(), userType.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemoteDoc("user_type", userType.getId(), batch);
            batch.set(ref, Map.of(
                    "id", userType.getId(),
                    "libelle", userType.getLibelle(),
                    "updated_at", formatDate(userType.getUpdatedAt())
            ));
        }

        for (ReglesGestion regle : reglesGestionRepository.findAll()) {
            LocalDateTime regleUpdatedAt = regle.getUpdatedAt();
            if (!shouldPush("regles_gestion", regle.getId(), regleUpdatedAt)) {
                continue;
            }
            DocumentReference ref = resolveRemoteDoc("regles_gestion", regle.getId(), batch);
            batch.set(ref, Map.of(
                    "id", regle.getId(),
                    "libelle", regle.getLibelle(),
                    "valeur", regle.getValeur(),
                    "updated_at", formatDate(regleUpdatedAt != null ? regleUpdatedAt : LocalDateTime.now())
            ));
        }

        for (TypeSignalement type : typeSignalementRepository.findAll()) {
            if (!shouldPush("type_signalements", type.getId(), type.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemoteDoc("type_signalements", type.getId(), batch);
            batch.set(ref, Map.of(
                    "id", type.getId(),
                    "libelle", type.getLibelle(),
                    "updated_at", formatDate(type.getUpdatedAt())
            ));
        }

        for (Statuts statut : statutsRepository.findAll()) {
            if (!shouldPush("statuts", statut.getId(), statut.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemoteDoc("statuts", statut.getId(), batch);
            batch.set(ref, Map.of(
                    "id", statut.getId(),
                    "libelle", statut.getLibelle(),
                    "updated_at", formatDate(statut.getUpdatedAt())
            ));
        }

        for (Entreprise entreprise : entrepriseRepository.findAll()) {
            if (!shouldPush("entreprises", entreprise.getId(), entreprise.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemoteDoc("entreprises", entreprise.getId(), batch);
            batch.set(ref, Map.of(
                    "id", entreprise.getId(),
                    "name", entreprise.getName(),
                    "address", entreprise.getAddress(),
                    "phone", entreprise.getPhone(),
                    "active", entreprise.getActive(),
                    "updated_at", formatDate(entreprise.getUpdatedAt())
            ));
        }

        for (Point point : pointRepository.findAll()) {
            if (!shouldPush("points", point.getId(), point.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemotePointDoc(point, batch);
            batch.set(ref, Map.of(
                    "id", point.getId(),
                    "latitude", point.getLatitude(),
                    "longitude", point.getLongitude(),
                    "updated_at", formatDate(point.getUpdatedAt())
            ));
        }

        for (Signalements signalement : signalementsRepository.findAll()) {
            if (!shouldPush("signalements", signalement.getId(), signalement.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemoteSignalementDoc(signalement, batch);
            List<String> photos = photoSignalementRepository.findBySignalementsIdOrderByIdAsc(signalement.getId())
                    .stream()
                    .map(PhotoSignalement::getUrl)
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(url -> !url.isBlank())
                    .toList();
            Map<String, Object> data = new HashMap<>();
            data.put("id", signalement.getId());
            data.put("user_id", signalement.getUser() != null ? signalement.getUser().getId() : null);
            data.put("point_id", signalement.getPoint() != null ? signalement.getPoint().getId() : null);
            data.put("type_signalement_id",
                    signalement.getTypeSignalement() != null ? signalement.getTypeSignalement().getId() : null);
            data.put("date", signalement.getDate() != null ? signalement.getDate().toString() : null);
            data.put("surface", signalement.getSurface());
            data.put("budget", signalement.getBudget());
            data.put("description", signalement.getDescription());
            data.put("statuts_id", signalement.getStatuts() != null ? signalement.getStatuts().getId() : null);
            data.put("entreprise_id",
                    signalement.getEntreprise() != null ? signalement.getEntreprise().getId() : null);
            data.put("photos", photos);
            data.put("updated_at", formatDate(signalement.getUpdatedAt()));
            batch.set(ref, data);
        }

        for (User user : userRepository.findAll()) {
            DocumentReference ref = resolveRemoteUserDoc(user);
            RemoteUserSnapshot remote = remoteUserByDocPath.get(ref.getPath());
            boolean emailChanged = isDifferentEmail(remote != null ? remote.email() : null, user.getEmail());
            boolean passwordChanged = remote == null || !Objects.equals(remote.password(), user.getPassword());
            boolean firebaseIdChanged =
                    remote == null || !Objects.equals(blankToNull(remote.firebaseId()), blankToNull(user.getFirebaseId()));

            User syncedUser = ensureFirebaseAuthUserSynced(user, emailChanged, passwordChanged, firebaseIdChanged);
            if (!shouldPush("users", syncedUser.getId(), syncedUser.getUpdatedAt())
                    && !emailChanged
                    && !passwordChanged
                    && !firebaseIdChanged) {
                continue;
            }

            ref = resolveRemoteUserDoc(syncedUser);
            Map<String, Object> data = new HashMap<>();
            data.put("id", syncedUser.getId());
            data.put("email", syncedUser.getEmail());
            data.put("password", syncedUser.getPassword());
            data.put("firebase_id", syncedUser.getFirebaseId());
            data.put("first_name", syncedUser.getFirstName());
            data.put("last_name", syncedUser.getLastName());
            data.put("date", syncedUser.getDate() != null ? syncedUser.getDate().toString() : null);
            data.put("failed_login_attempts", syncedUser.getFailedLoginAttempts());
            data.put("statuts_user_id",
                    syncedUser.getStatutsUser() != null ? syncedUser.getStatutsUser().getId() : null);
            data.put("user_type_id",
                    syncedUser.getUserType() != null ? syncedUser.getUserType().getId() : null);
            data.put("updated_at", formatDate(syncedUser.getUpdatedAt()));
            batch.set(ref, data);

            indexRemoteUserRef(
                    ref,
                    syncedUser.getId(),
                    syncedUser.getEmail(),
                    syncedUser.getFirebaseId(),
                    syncedUser.getPassword()
            );
        }

        for (HistoriqueSignalements historique : historiqueSignalementsRepository.findAll()) {
            if (!shouldPush("historique_signalements", historique.getId(), historique.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemoteDoc("historique_signalements", historique.getId(), batch);
            Map<String, Object> data = new HashMap<>();
            data.put("id", historique.getId());
            data.put("signalements_id",
                    historique.getSignalements() != null ? historique.getSignalements().getId() : null);
            data.put("statuts_id",
                    historique.getStatuts() != null ? historique.getStatuts().getId() : null);
            data.put("date", historique.getDate() != null ? historique.getDate().toString() : null);
            data.put("updated_at", formatDate(historique.getUpdatedAt()));
            batch.set(ref, data);
        }

        for (HistoriqueUsers historique : historiqueUsersRepository.findAll()) {
            if (!shouldPush("historique_users", historique.getId(), historique.getUpdatedAt())) {
                continue;
            }
            DocumentReference ref = resolveRemoteDoc("historique_users", historique.getId(), batch);
            Map<String, Object> data = new HashMap<>();
            data.put("id", historique.getId());
            data.put("user_id", historique.getUser() != null ? historique.getUser().getId() : null);
            data.put("statuts_user_id",
                    historique.getStatutsUser() != null ? historique.getStatutsUser().getId() : null);
            data.put("date", historique.getDate() != null ? historique.getDate().toString() : null);
            data.put("updated_at", formatDate(historique.getUpdatedAt()));
            batch.set(ref, data);
        }

        try {
            batch.commit().get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw syncFailure("Firebase push failed", ex);
        } catch (ExecutionException ex) {
            throw syncFailure("Firebase push failed", ex);
        }
    }

    @org.springframework.scheduling.annotation.Async
    public void deleteSignalementFromFirestore(Signalements signalement) {
        if (signalement == null) {
            return;
        }
        WriteBatch batch = firestore.batch();
        boolean deleted = deleteRemoteById("signalements", signalement.getId(), batch);
        if (!deleted) {
            deleteRemoteSignalementByNaturalKey(
                    signalement.getPoint() != null ? signalement.getPoint().getId() : null,
                    signalement.getTypeSignalement() != null ? signalement.getTypeSignalement().getId() : null,
                    signalement.getDate() != null ? signalement.getDate().toString() : null,
                    signalement.getUser() != null ? signalement.getUser().getId() : null,
                    batch
            );
        }
        try {
            batch.commit().get();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            log.warn("Firebase delete interrupted", ex);
        } catch (ExecutionException ex) {
            log.warn("Firebase delete failed", ex);
        }
    }

    public void pullAllFromFirebase() {
        try {
            syncStatutsUser();
            syncUserTypes();
            syncReglesGestion();
            syncTypeSignalements();
            syncStatuts();
            syncEntreprises();
            syncPoints();
            syncUsers();
            syncSignalements();
            syncHistoriqueSignalements();
            syncHistoriqueUsers();
            alignPostgresIdSequences();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw syncFailure("Firebase pull failed", ex);
        } catch (ExecutionException ex) {
            throw syncFailure("Firebase pull failed", ex);
        }
    }

    public void mergeSignalementsFromFirebase() {
        try {
            syncPoints();
            syncSignalements();
            alignPostgresIdSequences();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw syncFailure("Signalements merge from Firebase failed", ex);
        } catch (ExecutionException ex) {
            throw syncFailure("Signalements merge from Firebase failed", ex);
        }
    }

    public void mergeUsersFromFirebase() {
        try {
            syncStatutsUser();
            syncUserTypes();
            syncUsers();
            alignPostgresIdSequences();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw syncFailure("Users merge from Firebase failed", ex);
        } catch (ExecutionException ex) {
            throw syncFailure("Users merge from Firebase failed", ex);
        }
    }

    @Async
    public void refreshAsync() {
        pushAllToFirebase();
    }

    private void syncStatutsUser() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("statuts_user").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                StatutsUser existing = statutsUserRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                StatutsUser statutsUser = existing != null ? existing : new StatutsUser();
                statutsUser.setId(id);
                String libelle = doc.getString("libelle");
                if (libelle != null) {
                    statutsUser.setLibelle(libelle);
                }
                statutsUser.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                statutsUserRepository.save(statutsUser);
            } catch (Exception ex) {
                log.warn("Skipping statuts_user {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncUserTypes() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("user_type").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                UserType existing = userTypeRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                UserType userType = existing != null ? existing : new UserType();
                userType.setId(id);
                String libelle = doc.getString("libelle");
                if (libelle != null) {
                    userType.setLibelle(libelle);
                }
                userType.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                userTypeRepository.save(userType);
            } catch (Exception ex) {
                log.warn("Skipping user_type {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncReglesGestion() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("regles_gestion").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                ReglesGestion existing = reglesGestionRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                ReglesGestion regle = existing != null ? existing : new ReglesGestion();
                regle.setId(id);
                String libelle = doc.getString("libelle");
                if (libelle != null) {
                    regle.setLibelle(libelle);
                }
                String valeur = doc.getString("valeur");
                if (valeur != null) {
                    regle.setValeur(valeur);
                }
                regle.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                reglesGestionRepository.save(regle);
            } catch (Exception ex) {
                log.warn("Skipping regles_gestion {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncTypeSignalements() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("type_signalements").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                TypeSignalement existing = typeSignalementRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                TypeSignalement type = existing != null ? existing : new TypeSignalement();
                type.setId(id);
                String libelle = doc.getString("libelle");
                if (libelle != null) {
                    type.setLibelle(libelle);
                }
                type.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                typeSignalementRepository.save(type);
            } catch (Exception ex) {
                log.warn("Skipping type_signalements {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncStatuts() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("statuts").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                Statuts existing = statutsRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                Statuts statut = existing != null ? existing : new Statuts();
                statut.setId(id);
                String libelle = doc.getString("libelle");
                if (libelle != null) {
                    statut.setLibelle(libelle);
                }
                statut.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                statutsRepository.save(statut);
            } catch (Exception ex) {
                log.warn("Skipping statuts {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncEntreprises() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("entreprises").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                Entreprise existing = entrepriseRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                Entreprise entreprise = existing != null ? existing : new Entreprise();
                entreprise.setId(id);
                String name = doc.getString("name");
                if (name != null) {
                    entreprise.setName(name);
                }
                String address = doc.getString("address");
                if (address != null) {
                    entreprise.setAddress(address);
                }
                String phone = doc.getString("phone");
                if (phone != null) {
                    entreprise.setPhone(phone);
                }
                Boolean active = doc.getBoolean("active");
                if (active != null) {
                    entreprise.setActive(active);
                }
                entreprise.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                entrepriseRepository.save(entreprise);
            } catch (Exception ex) {
                log.warn("Skipping entreprises {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncPoints() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("points").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                Double latitude = normalizeCoordinate(getDouble(doc, "latitude"));
                Double longitude = normalizeCoordinate(getDouble(doc, "longitude"));

                Point existing = null;
                if (id != null) {
                    existing = pointRepository.findById(id).orElse(null);
                }
                if (existing == null && latitude != null && longitude != null) {
                    existing = findPointByCoordinates(latitude, longitude).orElse(null);
                }

                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }

                Point point = existing != null ? existing : new Point();
                if (existing == null && id != null) {
                    point.setId(id);
                }
                if (latitude != null) {
                    point.setLatitude(latitude);
                }
                if (longitude != null) {
                    point.setLongitude(longitude);
                }
                point.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                pointRepository.save(point);
            } catch (Exception ex) {
                log.warn("Skipping points {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncUsers() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("users").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                User existing = userRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                User user = existing != null ? existing : new User();
                user.setId(id);
                String email = getString(doc, "email");
                String password = getString(doc, "password");
                String firebaseId = getString(doc, "firebase_id", "firebaseId");
                String firstName = getString(doc, "first_name", "firstName");
                String lastName = getString(doc, "last_name", "lastName");
                if (email == null || password == null || firstName == null || lastName == null) {
                    log.warn("Skipping user {} due to missing required fields", doc.getId());
                    continue;
                }
                user.setEmail(email);
                user.setPassword(password);
                user.setFirebaseId(firebaseId);
                user.setFirstName(firstName);
                user.setLastName(lastName);

                LocalDateTime createdAt = getDate(doc, "created_at");
                if (createdAt == null) {
                    createdAt = getDate(doc, "date");
                }
                if (createdAt != null) {
                    user.setDate(createdAt);
                }

                Long statutsUserId = getLong(doc, "statuts_user_id");
                if (statutsUserId != null) {
                    StatutsUser statutsUser = statutsUserRepository.findById(statutsUserId).orElse(null);
                    if (statutsUser != null) {
                        user.setStatutsUser(statutsUser);
                    }
                }

                Long userTypeId = getLong(doc, "user_type_id");
                if (userTypeId != null) {
                    UserType userType = userTypeRepository.findById(userTypeId).orElse(null);
                    if (userType != null) {
                        user.setUserType(userType);
                    }
                }

                Long failedAttempts = getLong(doc, "failed_login_attempts");
                if (failedAttempts != null) {
                    user.setFailedLoginAttempts(failedAttempts.intValue());
                }
                user.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());

                userRepository.save(user);
            } catch (Exception ex) {
                log.warn("Skipping users {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncSignalements() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("signalements").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");

                Double surface = getDouble(doc, "surface");
                Double budget = getDouble(doc, "budget");
                String description = getString(doc, "description");

                LocalDateTime createdAt = getDate(doc, "created_at");
                if (createdAt == null) {
                    createdAt = getDate(doc, "date");
                }

                User resolvedUser = null;
                Long userId = getLong(doc, "user_id");
                if (userId != null) {
                    resolvedUser = userRepository.findById(userId).orElse(null);
                } else {
                    String firebaseId = getString(doc, "userId", "user_id");
                    if (firebaseId != null) {
                        resolvedUser = userRepository.findByFirebaseId(firebaseId).orElse(null);
                    } else {
                        String email = getString(doc, "userEmail", "user_email");
                        if (email != null) {
                            resolvedUser = userRepository.findByEmail(email).orElse(null);
                        }
                    }
                }

                Point resolvedPoint = null;
                Long pointId = getLong(doc, "point_id");
                if (pointId != null) {
                    resolvedPoint = pointRepository.findById(pointId).orElse(null);
                } else {
                    Double latitude = normalizeCoordinate(getDouble(doc, "latitude"));
                    Double longitude = normalizeCoordinate(getDouble(doc, "longitude"));
                    if (latitude != null && longitude != null) {
                        resolvedPoint = findPointByCoordinates(latitude, longitude)
                                .orElseGet(() -> {
                                    Point created = new Point();
                                    created.setLatitude(latitude);
                                    created.setLongitude(longitude);
                                    created.setUpdatedAt(LocalDateTime.now());
                                    return pointRepository.save(created);
                                });
                    }
                }

                TypeSignalement resolvedType = null;
                Long typeId = getLong(doc, "type_signalement_id");
                if (typeId != null) {
                    resolvedType = typeSignalementRepository.findById(typeId).orElse(null);
                } else {
                    String title = getString(doc, "title", "type_signalement");
                    if (title != null) {
                        resolvedType = typeSignalementRepository.findByLibelleIgnoreCase(title).orElse(null);
                    }
                }

                Statuts resolvedStatut = null;
                Long statutId = getLong(doc, "statuts_id");
                if (statutId != null) {
                    resolvedStatut = statutsRepository.findById(statutId).orElse(null);
                } else {
                    String status = getString(doc, "status", "statut");
                    if (status != null) {
                        resolvedStatut = statutsRepository.findByLibelleIgnoreCase(status).orElse(null);
                    }
                }

                Entreprise resolvedEntreprise = null;
                Long entrepriseId = getLong(doc, "entreprise_id");
                if (entrepriseId != null) {
                    resolvedEntreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
                } else {
                    String entrepriseName = getString(doc, "entreprise", "entreprise_name", "entrepriseName");
                    if (entrepriseName != null) {
                        resolvedEntreprise = entrepriseRepository.findByNameIgnoreCase(entrepriseName).orElse(null);
                    }
                }

                Signalements existing = null;
                if (id != null) {
                    existing = signalementsRepository.findById(id).orElse(null);
                }
                if (existing == null && resolvedPoint != null && resolvedType != null && createdAt != null) {
                    if (resolvedUser != null) {
                        existing = signalementsRepository
                                .findFirstByPointAndTypeSignalementAndUserAndDate(
                                        resolvedPoint,
                                        resolvedType,
                                        resolvedUser,
                                        createdAt)
                                .orElse(null);
                    }
                    if (existing == null) {
                        existing = signalementsRepository
                                .findFirstByPointAndTypeSignalementAndDate(
                                        resolvedPoint,
                                        resolvedType,
                                        createdAt)
                                .orElse(null);
                    }
                }

                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    if (existing != null) {
                        syncSignalementPhotosFromFirestore(existing, doc, remoteUpdatedAt);
                    }
                    continue;
                }

                Signalements signalement = existing != null ? existing : new Signalements();
                if (existing == null && id != null) {
                    signalement.setId(id);
                }
                if (surface != null) {
                    signalement.setSurface(surface);
                }
                if (budget != null) {
                    signalement.setBudget(budget);
                }
                if (description != null) {
                    signalement.setDescription(description);
                }
                if (createdAt != null) {
                    signalement.setDate(createdAt);
                }
                if (resolvedUser != null) {
                    signalement.setUser(resolvedUser);
                }
                if (resolvedPoint != null) {
                    signalement.setPoint(resolvedPoint);
                }
                if (resolvedType != null) {
                    signalement.setTypeSignalement(resolvedType);
                }
                if (resolvedStatut != null) {
                    signalement.setStatuts(resolvedStatut);
                }
                if (resolvedEntreprise != null) {
                    signalement.setEntreprise(resolvedEntreprise);
                }

                signalement.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                Signalements persistedSignalement;
                if (existing == null && id != null) {
                    upsertSignalementWithExplicitId(signalement, id);
                    persistedSignalement = signalementsRepository.findById(id).orElse(signalement);
                } else {
                    persistedSignalement = signalementsRepository.save(signalement);
                }
                syncSignalementPhotosFromFirestore(persistedSignalement, doc, remoteUpdatedAt);
            } catch (Exception ex) {
                log.warn("Skipping signalements {} due to error", doc.getId(), ex);
            }
        }
    }

    private void upsertSignalementWithExplicitId(Signalements signalement, Long id) {
        if (signalement == null || id == null) {
            return;
        }
        jdbcTemplate.update(
                """
                        insert into signalements (
                            id,
                            user_id,
                            point_id,
                            type_signalement_id,
                            date,
                            surface,
                            budget,
                            description,
                            updated_at,
                            statuts_id,
                            entreprise_id
                        ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on conflict (id) do update set
                            user_id = excluded.user_id,
                            point_id = excluded.point_id,
                            type_signalement_id = excluded.type_signalement_id,
                            date = excluded.date,
                            surface = excluded.surface,
                            budget = excluded.budget,
                            description = excluded.description,
                            updated_at = excluded.updated_at,
                            statuts_id = excluded.statuts_id,
                            entreprise_id = excluded.entreprise_id
                        """,
                id,
                signalement.getUser() != null ? signalement.getUser().getId() : null,
                signalement.getPoint() != null ? signalement.getPoint().getId() : null,
                signalement.getTypeSignalement() != null ? signalement.getTypeSignalement().getId() : null,
                signalement.getDate(),
                signalement.getSurface(),
                signalement.getBudget(),
                signalement.getDescription(),
                signalement.getUpdatedAt(),
                signalement.getStatuts() != null ? signalement.getStatuts().getId() : null,
                signalement.getEntreprise() != null ? signalement.getEntreprise().getId() : null
        );
    }

    private void syncHistoriqueSignalements() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("historique_signalements").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                HistoriqueSignalements existing = historiqueSignalementsRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                HistoriqueSignalements historique = existing != null ? existing : new HistoriqueSignalements();
                historique.setId(id);

                Long signalementId = getLong(doc, "signalements_id");
                if (signalementId != null) {
                    Signalements signalement = signalementsRepository.findById(signalementId).orElse(null);
                    if (signalement != null) {
                        historique.setSignalements(signalement);
                    }
                }

                Long statutId = getLong(doc, "statuts_id");
                if (statutId != null) {
                    Statuts statut = statutsRepository.findById(statutId).orElse(null);
                    if (statut != null) {
                        historique.setStatuts(statut);
                    }
                }

                LocalDateTime dateValue = getDate(doc, "date");
                if (dateValue != null) {
                    historique.setDate(dateValue);
                }

                historique.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                historiqueSignalementsRepository.save(historique);
            } catch (Exception ex) {
                log.warn("Skipping historique_signalements {} due to error", doc.getId(), ex);
            }
        }
    }

    private void syncHistoriqueUsers() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("historique_users").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            try {
                Long id = resolveId(doc);
                if (id == null) {
                    continue;
                }
                HistoriqueUsers existing = historiqueUsersRepository.findById(id).orElse(null);
                LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
                if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                    continue;
                }
                HistoriqueUsers historique = existing != null ? existing : new HistoriqueUsers();
                historique.setId(id);

                Long userId = getLong(doc, "user_id");
                if (userId != null) {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        historique.setUser(user);
                    }
                }

                Long statutsUserId = getLong(doc, "statuts_user_id");
                if (statutsUserId != null) {
                    StatutsUser statutsUser = statutsUserRepository.findById(statutsUserId).orElse(null);
                    if (statutsUser != null) {
                        historique.setStatutsUser(statutsUser);
                    }
                }

                LocalDateTime dateValue = getDate(doc, "date");
                if (dateValue != null) {
                    historique.setDate(dateValue);
                }

                historique.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
                historiqueUsersRepository.save(historique);
            } catch (Exception ex) {
                log.warn("Skipping historique_users {} due to error", doc.getId(), ex);
            }
        }
    }

    private Double normalizeCoordinate(Double value) {
        if (value == null) {
            return null;
        }
        double scale = 1_000_000d;
        return Math.round(value * scale) / scale;
    }

    private Optional<Point> findPointByCoordinates(Double latitude, Double longitude) {
        Optional<Point> exact = pointRepository.findByLatitudeAndLongitude(latitude, longitude);
        if (exact.isPresent()) {
            return exact;
        }
        double epsilon = 1e-6;
        return pointRepository.findFirstByLatitudeBetweenAndLongitudeBetween(
                latitude - epsilon,
                latitude + epsilon,
                longitude - epsilon,
                longitude + epsilon
        );
    }

    private Long resolveId(DocumentSnapshot doc) {
        Long id = getLong(doc, "id");
        if (id != null) {
            return id;
        }
        String docId = doc.getId();
        try {
            return Long.parseLong(docId);
        } catch (NumberFormatException ex) {
            log.warn("Skipping document with non-numeric id: {}", docId);
            return null;
        }
    }

    private long countCollection(String name) {
        try {
            return firestore.collection(name).get().get().size();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return 0L;
        } catch (ExecutionException ex) {
            log.warn("Unable to count Firestore collection {}", name, ex);
            return 0L;
        }
    }

    private DocumentReference resolveRemoteDoc(String collection, Long id, WriteBatch batch) {
        if (id == null) {
            return firestore.collection(collection).document();
        }
        try {
            QuerySnapshot snapshot = firestore.collection(collection)
                    .whereEqualTo("id", id)
                    .get()
                    .get();
            var docs = snapshot.getDocuments();
            if (!docs.isEmpty()) {
                DocumentReference keep = docs.get(0).getReference();
                for (int i = 1; i < docs.size(); i++) {
                    batch.delete(docs.get(i).getReference());
                }
                return keep;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to resolve remote doc by numeric id for {}:{}", collection, id, ex);
        }
        try {
            QuerySnapshot snapshot = firestore.collection(collection)
                    .whereEqualTo("id", String.valueOf(id))
                    .get()
                    .get();
            var docs = snapshot.getDocuments();
            if (!docs.isEmpty()) {
                DocumentReference keep = docs.get(0).getReference();
                for (int i = 1; i < docs.size(); i++) {
                    batch.delete(docs.get(i).getReference());
                }
                return keep;
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to resolve remote doc by string id for {}:{}", collection, id, ex);
        }
        return firestore.collection(collection).document(String.valueOf(id));
    }

    private DocumentReference resolveRemotePointDoc(Point point, WriteBatch batch) {
        if (point == null) {
            return firestore.collection("points").document();
        }
        if (point.getId() != null) {
            DocumentReference byId = remotePointById.get(point.getId());
            if (byId != null) {
                return byId;
            }
        }
        Double latitude = normalizeCoordinate(point.getLatitude());
        Double longitude = normalizeCoordinate(point.getLongitude());
        if (latitude != null && longitude != null) {
            String key = coordinateKey(latitude, longitude);
            DocumentReference byCoords = remotePointByCoord.get(key);
            if (byCoords != null) {
                return byCoords;
            }
        }
        if (point.getId() != null) {
            return firestore.collection("points").document(String.valueOf(point.getId()));
        }
        return firestore.collection("points").document();
    }

    private DocumentReference resolveRemoteSignalementDoc(Signalements signalement, WriteBatch batch) {
        if (signalement == null) {
            return firestore.collection("signalements").document();
        }
        if (signalement.getId() != null) {
            DocumentReference byId = remoteSignalementById.get(signalement.getId());
            if (byId != null) {
                return byId;
            }
        }
        String key = signalementKey(
                signalement.getPoint() != null ? signalement.getPoint().getId() : null,
                signalement.getTypeSignalement() != null ? signalement.getTypeSignalement().getId() : null,
                signalement.getDate() != null ? signalement.getDate().toString() : null,
                signalement.getUser() != null ? signalement.getUser().getId() : null
        );
        DocumentReference byNaturalKey = key != null ? remoteSignalementByKey.get(key) : null;
        if (byNaturalKey != null) {
            return byNaturalKey;
        }
        if (signalement.getId() != null) {
            return firestore.collection("signalements").document(String.valueOf(signalement.getId()));
        }
        return firestore.collection("signalements").document();
    }

    private DocumentReference resolveRemoteUserDoc(User user) {
        if (user == null) {
            return firestore.collection("users").document();
        }
        if (user.getId() != null) {
            DocumentReference byId = remoteUserById.get(user.getId());
            if (byId != null) {
                return byId;
            }
        }
        String firebaseId = blankToNull(user.getFirebaseId());
        if (firebaseId != null) {
            DocumentReference byFirebaseId = remoteUserByFirebaseId.get(firebaseId);
            if (byFirebaseId != null) {
                return byFirebaseId;
            }
        }
        String email = normalizeEmail(user.getEmail());
        if (email != null) {
            DocumentReference byEmail = remoteUserByEmail.get(email);
            if (byEmail != null) {
                return byEmail;
            }
        }
        if (user.getId() != null) {
            return firestore.collection("users").document(String.valueOf(user.getId()));
        }
        return firestore.collection("users").document();
    }

    private void indexRemoteUserRef(
            DocumentReference ref,
            Long id,
            String email,
            String firebaseId,
            String password
    ) {
        if (ref == null) {
            return;
        }
        if (id != null) {
            remoteUserById.put(id, ref);
        }
        String normalizedEmail = normalizeEmail(email);
        if (normalizedEmail != null) {
            remoteUserByEmail.put(normalizedEmail, ref);
        }
        String normalizedFirebaseId = blankToNull(firebaseId);
        if (normalizedFirebaseId != null) {
            remoteUserByFirebaseId.put(normalizedFirebaseId, ref);
        }
        remoteUserByDocPath.put(
                ref.getPath(),
                new RemoteUserSnapshot(normalizedEmail, password, normalizedFirebaseId)
        );
    }

    private User ensureFirebaseAuthUserSynced(
            User user,
            boolean emailChanged,
            boolean passwordChanged,
            boolean firebaseIdChanged
    ) {
        if (user == null) {
            return null;
        }
        String email = normalizeEmail(user.getEmail());
        String password = user.getPassword();
        if (email == null || password == null || password.isBlank()) {
            throw new RuntimeException("Invalid user data for Firebase sync (email/password required)");
        }

        FirebaseAuth auth = FirebaseAuth.getInstance();
        UserRecord byUid = findFirebaseUserByUid(blankToNull(user.getFirebaseId()));
        UserRecord byEmail = findFirebaseUserByEmail(email);

        UserRecord target = byUid != null ? byUid : byEmail;
        if (target == null) {
            try {
                UserRecord created = auth.createUser(new UserRecord.CreateRequest()
                        .setEmail(email)
                        .setPassword(password)
                        .setDisplayName(buildDisplayName(user)));
                user.setFirebaseId(created.getUid());
                return userRepository.save(user);
            } catch (FirebaseAuthException ex) {
                throw new RuntimeException("Firebase Auth user creation failed: " + ex.getMessage(), ex);
            }
        }

        if (byUid != null && byEmail != null && !byUid.getUid().equals(byEmail.getUid())) {
            throw new RuntimeException("Firebase Auth conflict: email already attached to another uid for " + email);
        }

        boolean localUpdated = false;
        if (!Objects.equals(blankToNull(user.getFirebaseId()), target.getUid())) {
            user.setFirebaseId(target.getUid());
            localUpdated = true;
        }

        UserRecord.UpdateRequest update = new UserRecord.UpdateRequest(target.getUid());
        boolean authNeedsUpdate = false;

        String currentEmail = normalizeEmail(target.getEmail());
        if ((emailChanged || firebaseIdChanged) && !Objects.equals(currentEmail, email)) {
            update.setEmail(email);
            authNeedsUpdate = true;
        }
        if (passwordChanged) {
            update.setPassword(password);
            authNeedsUpdate = true;
        }
        String displayName = buildDisplayName(user);
        if (!Objects.equals(blankToNull(target.getDisplayName()), displayName)) {
            update.setDisplayName(displayName);
            authNeedsUpdate = true;
        }

        if (authNeedsUpdate) {
            try {
                auth.updateUser(update);
            } catch (FirebaseAuthException ex) {
                throw new RuntimeException("Firebase Auth user update failed: " + ex.getMessage(), ex);
            }
        }

        if (localUpdated) {
            return userRepository.save(user);
        }
        return user;
    }

    private UserRecord findFirebaseUserByUid(String uid) {
        if (uid == null) {
            return null;
        }
        try {
            return FirebaseAuth.getInstance().getUser(uid);
        } catch (FirebaseAuthException ex) {
            if (isUserNotFound(ex)) {
                return null;
            }
            throw new RuntimeException("Firebase Auth lookup by uid failed: " + ex.getMessage(), ex);
        }
    }

    private UserRecord findFirebaseUserByEmail(String email) {
        if (email == null) {
            return null;
        }
        try {
            return FirebaseAuth.getInstance().getUserByEmail(email);
        } catch (FirebaseAuthException ex) {
            if (isUserNotFound(ex)) {
                return null;
            }
            throw new RuntimeException("Firebase Auth lookup by email failed: " + ex.getMessage(), ex);
        }
    }

    private String buildDisplayName(User user) {
        String first = blankToNull(user != null ? user.getFirstName() : null);
        String last = blankToNull(user != null ? user.getLastName() : null);
        if (first == null && last == null) {
            return null;
        }
        if (first == null) {
            return last;
        }
        if (last == null) {
            return first;
        }
        return first + " " + last;
    }

    private boolean isUserNotFound(FirebaseAuthException ex) {
        if (ex == null) {
            return false;
        }
        if (ex.getAuthErrorCode() == AuthErrorCode.USER_NOT_FOUND) {
            return true;
        }
        String message = ex.getMessage();
        if (message == null) {
            return false;
        }
        String normalized = message.toLowerCase();
        return normalized.contains("no user record") || normalized.contains("user not found");
    }

    private void prepareRemoteIndexes(WriteBatch batch) {
        remotePointByCoord = new HashMap<>();
        remotePointById = new HashMap<>();
        remoteSignalementByKey = new HashMap<>();
        remoteSignalementById = new HashMap<>();
        remoteUserById = new HashMap<>();
        remoteUserByEmail = new HashMap<>();
        remoteUserByFirebaseId = new HashMap<>();
        remoteUserByDocPath = new HashMap<>();

        try {
            QuerySnapshot points = firestore.collection("points").get().get();
            for (DocumentSnapshot doc : points.getDocuments()) {
                Long id = resolveId(doc);
                if (id != null) {
                    DocumentReference existing = remotePointById.putIfAbsent(id, doc.getReference());
                    if (existing != null) {
                        batch.delete(doc.getReference());
                    }
                }
                Double lat = normalizeCoordinate(getDouble(doc, "latitude"));
                Double lon = normalizeCoordinate(getDouble(doc, "longitude"));
                if (lat != null && lon != null) {
                    String key = coordinateKey(lat, lon);
                    DocumentReference existing = remotePointByCoord.putIfAbsent(key, doc.getReference());
                    if (existing != null) {
                        batch.delete(doc.getReference());
                    }
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to index remote points", ex);
        }

        try {
            QuerySnapshot signalements = firestore.collection("signalements").get().get();
            for (DocumentSnapshot doc : signalements.getDocuments()) {
                Long id = resolveId(doc);
                if (id != null) {
                    DocumentReference existing = remoteSignalementById.putIfAbsent(id, doc.getReference());
                    if (existing != null) {
                        batch.delete(doc.getReference());
                    }
                }
                LocalDateTime dateValue = getDate(doc, "date");
                if (dateValue == null) {
                    dateValue = getDate(doc, "created_at");
                }
                String key = signalementKey(
                        getLong(doc, "point_id"),
                        getLong(doc, "type_signalement_id"),
                        dateValue != null ? dateValue.toString() : null,
                        getLong(doc, "user_id")
                );
                if (key != null) {
                    DocumentReference existing = remoteSignalementByKey.putIfAbsent(key, doc.getReference());
                    if (existing != null) {
                        batch.delete(doc.getReference());
                    }
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to index remote signalements", ex);
        }

        try {
            QuerySnapshot users = firestore.collection("users").get().get();
            for (DocumentSnapshot doc : users.getDocuments()) {
                DocumentReference ref = doc.getReference();
                Long id = getLong(doc, "id");
                String email = normalizeEmail(getString(doc, "email"));
                String firebaseId = blankToNull(getString(doc, "firebase_id", "firebaseId"));
                String password = getString(doc, "password");

                boolean duplicate = false;

                if (id != null) {
                    DocumentReference existing = remoteUserById.putIfAbsent(id, ref);
                    if (existing != null && !existing.getPath().equals(ref.getPath())) {
                        duplicate = true;
                    }
                }
                if (!duplicate && email != null) {
                    DocumentReference existing = remoteUserByEmail.putIfAbsent(email, ref);
                    if (existing != null && !existing.getPath().equals(ref.getPath())) {
                        duplicate = true;
                    }
                }
                if (!duplicate && firebaseId != null) {
                    DocumentReference existing = remoteUserByFirebaseId.putIfAbsent(firebaseId, ref);
                    if (existing != null && !existing.getPath().equals(ref.getPath())) {
                        duplicate = true;
                    }
                }

                if (duplicate) {
                    batch.delete(ref);
                    continue;
                }

                remoteUserByDocPath.put(ref.getPath(), new RemoteUserSnapshot(email, password, firebaseId));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to index remote users", ex);
        }
    }

    private boolean deleteRemoteById(String collection, Long id, WriteBatch batch) {
        if (id == null) {
            return false;
        }
        boolean deleted = false;
        try {
            QuerySnapshot snapshot = firestore.collection(collection)
                    .whereEqualTo("id", id)
                    .get()
                    .get();
            deleted = deleteAllDocs(snapshot, batch) || deleted;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to delete remote docs by numeric id for {}:{}", collection, id, ex);
        }
        try {
            QuerySnapshot snapshot = firestore.collection(collection)
                    .whereEqualTo("id", String.valueOf(id))
                    .get()
                    .get();
            deleted = deleteAllDocs(snapshot, batch) || deleted;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to delete remote docs by string id for {}:{}", collection, id, ex);
        }
        return deleted;
    }

    private boolean deleteRemoteSignalementByNaturalKey(
            Long pointId,
            Long typeId,
            String date,
            Long userId,
            WriteBatch batch
    ) {
        if (pointId == null || typeId == null || date == null) {
            return false;
        }
        try {
            var query = firestore.collection("signalements")
                    .whereEqualTo("point_id", pointId)
                    .whereEqualTo("type_signalement_id", typeId)
                    .whereEqualTo("date", date);
            if (userId != null) {
                query = query.whereEqualTo("user_id", userId);
            }
            QuerySnapshot snapshot = query.get().get();
            return deleteAllDocs(snapshot, batch);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to delete remote signalement by natural key point={}, type={}, user={}",
                    pointId, typeId, userId, ex);
        }
        return false;
    }

    private boolean deleteAllDocs(QuerySnapshot snapshot, WriteBatch batch) {
        if (snapshot == null) {
            return false;
        }
        boolean deleted = false;
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            batch.delete(doc.getReference());
            deleted = true;
        }
        return deleted;
    }

    private String coordinateKey(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }
        return latitude + "," + longitude;
    }

    private String signalementKey(Long pointId, Long typeId, String date, Long userId) {
        if (pointId == null || typeId == null || date == null) {
            return null;
        }
        String base = pointId + "|" + typeId + "|" + date;
        return userId != null ? base + "|" + userId : base;
    }

    private String normalizeEmail(String email) {
        String value = blankToNull(email);
        return value != null ? value.toLowerCase() : null;
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isDifferentEmail(String left, String right) {
        return !Objects.equals(normalizeEmail(left), normalizeEmail(right));
    }

    private String flattenExceptionMessage(Throwable throwable) {
        if (throwable == null) {
            return "Unknown error";
        }
        StringBuilder builder = new StringBuilder();
        Throwable current = throwable;
        int depth = 0;
        while (current != null && depth < 5) {
            String part = current.getClass().getSimpleName();
            String message = current.getMessage();
            if (message != null && !message.isBlank()) {
                part += ": " + message;
            }
            if (builder.length() > 0) {
                builder.append(" | cause: ");
            }
            builder.append(part);
            current = current.getCause();
            depth++;
        }
        return builder.toString();
    }

    private RuntimeException syncFailure(String prefix, Exception ex) {
        String message = ex != null ? ex.getMessage() : null;
        if ((message == null || message.isBlank()) && ex != null && ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }
        if (message == null || message.isBlank()) {
            message = ex != null ? ex.getClass().getSimpleName() : "Unknown error";
        }
        String normalized = message.toLowerCase();
        if (normalized.contains("credentials failed to obtain metadata")
                || normalized.contains("unable to load the default credentials")
                || normalized.contains("statusruntimeexception: unavailable")) {
            message = message
                    + " | Firebase credentials/token unavailable. "
                    + "Verify service-account JSON, internet egress from backend, DNS/TLS access to oauth2.googleapis.com, and system clock.";
        }
        return new RuntimeException(prefix + ": " + message, ex);
    }

    private boolean shouldPush(String collection, Long id, LocalDateTime localUpdatedAt) {
        LocalDateTime remoteUpdatedAt = getRemoteUpdatedAt(collection, id);
        if (localUpdatedAt == null) {
            return remoteUpdatedAt == null;
        }
        if (remoteUpdatedAt == null) {
            return true;
        }
        return localUpdatedAt.isAfter(remoteUpdatedAt);
    }

    private LocalDateTime getRemoteUpdatedAt(String collection, Long id) {
        if (id == null) {
            return null;
        }
        try {
            QuerySnapshot snapshot = firestore.collection(collection)
                    .whereEqualTo("id", id)
                    .get()
                    .get();
            var docs = snapshot.getDocuments();
            if (!docs.isEmpty()) {
                return resolveUpdatedAtFromDoc(docs.get(0));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to fetch remote updated_at for {}:{}", collection, id, ex);
        }
        try {
            QuerySnapshot snapshot = firestore.collection(collection)
                    .whereEqualTo("id", String.valueOf(id))
                    .get()
                    .get();
            var docs = snapshot.getDocuments();
            if (!docs.isEmpty()) {
                return resolveUpdatedAtFromDoc(docs.get(0));
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException ex) {
            log.warn("Failed to fetch remote updated_at for {}:{} (string id)", collection, id, ex);
        }
        return null;
    }

    private LocalDateTime resolveUpdatedAtFromDoc(DocumentSnapshot doc) {
        LocalDateTime value = getDate(doc, "updated_at");
        if (value != null) {
            return value;
        }
        return getDate(doc, "updatedAt");
    }

    private Long getLong(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        return parseLongValue(value);
    }

    private Long parseLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            String normalized = text.trim();
            if (normalized.isEmpty()) {
                return null;
            }
            try {
                return Long.parseLong(normalized);
            } catch (NumberFormatException ex) {
                try {
                    BigDecimal decimal = new BigDecimal(normalized);
                    return decimal.stripTrailingZeros().longValueExact();
                } catch (Exception ignored) {
                    return null;
                }
            }
        }
        return null;
    }

    private Double getDouble(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private LocalDateTime getDate(DocumentSnapshot doc, String field) {
        LocalDateTime value = getDateValue(doc, field);
        if (value != null) {
            return value;
        }
        String alternate = toCamelCase(field);
        if (alternate != null && !alternate.equals(field)) {
            return getDateValue(doc, alternate);
        }
        return null;
    }

    private LocalDateTime getDateValue(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Timestamp timestamp) {
            return LocalDateTime.ofInstant(timestamp.toDate().toInstant(), java.time.ZoneOffset.UTC);
        }
        if (value instanceof java.util.Date date) {
            return LocalDateTime.ofInstant(date.toInstant(), java.time.ZoneOffset.UTC);
        }
        if (value instanceof String text) {
            return parseDateString(text);
        }
        return null;
    }

    private LocalDateTime parseDateString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            try {
                return LocalDateTime.ofInstant(java.time.Instant.parse(value), java.time.ZoneOffset.UTC);
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    private String getString(DocumentSnapshot doc, String... fields) {
        for (String field : fields) {
            String value = doc.getString(field);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private String toCamelCase(String value) {
        if (value == null || !value.contains("_")) {
            return value;
        }
        String[] parts = value.split("_");
        if (parts.length == 0) {
            return value;
        }
        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0)));
            if (parts[i].length() > 1) {
                builder.append(parts[i].substring(1));
            }
        }
        return builder.toString();
    }

    private String formatDate(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }

    private boolean shouldOverwrite(LocalDateTime localUpdatedAt, LocalDateTime remoteUpdatedAt) {
        if (localUpdatedAt == null) {
            return true;
        }
        if (remoteUpdatedAt == null) {
            return false;
        }
        return remoteUpdatedAt.isAfter(localUpdatedAt);
    }

    private void syncSignalementPhotosFromFirestore(
            Signalements signalement,
            DocumentSnapshot doc,
            LocalDateTime remoteUpdatedAt
    ) {
        if (signalement == null || signalement.getId() == null || doc == null) {
            return;
        }
        Object rawPhotos = doc.get("photos");
        if (rawPhotos == null) {
            return;
        }
        if (!(rawPhotos instanceof List<?> photoValues)) {
            log.warn("Signalement {} has invalid photos format in Firestore doc {}", signalement.getId(), doc.getId());
            return;
        }

        List<String> normalizedRemoteUrls = normalizePhotoUrls(photoValues);
        List<String> localUrls = photoSignalementRepository.findBySignalementsIdOrderByIdAsc(signalement.getId())
                .stream()
                .map(PhotoSignalement::getUrl)
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(url -> !url.isBlank())
                .toList();
        if (localUrls.equals(normalizedRemoteUrls)) {
            return;
        }

        photoSignalementRepository.deleteBySignalementsId(signalement.getId());
        LocalDateTime photoUpdatedAt = remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now();
        for (String url : normalizedRemoteUrls) {
            photoSignalementRepository.save(PhotoSignalement.builder()
                    .signalements(signalement)
                    .url(url)
                    .updatedAt(photoUpdatedAt)
                    .build());
        }
    }

    private List<String> normalizePhotoUrls(List<?> photoValues) {
        List<String> normalizedUrls = new ArrayList<>();
        for (Object photoValue : photoValues) {
            if (photoValue == null) {
                continue;
            }
            String url = photoValue.toString().trim();
            if (url.isBlank() || normalizedUrls.contains(url)) {
                continue;
            }
            normalizedUrls.add(url);
        }
        return normalizedUrls;
    }

    private void alignPostgresIdSequences() {
        try {
            String schema = jdbcTemplate.queryForObject("select current_schema()", String.class);
            if (schema == null || schema.isBlank()) {
                schema = "public";
            }

            var tables = jdbcTemplate.queryForList(
                    """
                            select table_name
                            from information_schema.columns
                            where table_schema = ?
                              and column_name = 'id'
                            order by table_name
                            """,
                    String.class,
                    schema
            );

            for (String table : tables) {
                if (table == null || !table.matches("[a-zA-Z0-9_]+")) {
                    continue;
                }
                String qualifiedTable = schema + "." + table;
                String sequence = jdbcTemplate.queryForObject(
                        "select pg_get_serial_sequence(?, 'id')",
                        String.class,
                        qualifiedTable
                );
                if (sequence == null || sequence.isBlank()) {
                    continue;
                }

                Long nextValue = jdbcTemplate.queryForObject(
                        "select coalesce(max(id), 0) + 1 from "
                                + quoteIdentifier(schema) + "." + quoteIdentifier(table),
                        Long.class
                );
                if (nextValue == null || nextValue < 1L) {
                    nextValue = 1L;
                }

                jdbcTemplate.update("select setval(?::regclass, ?, false)", sequence, nextValue);
            }
        } catch (Exception ex) {
            log.warn("Unable to align PostgreSQL id sequences after Firebase sync", ex);
        }
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
