package com.cloudweb.service;

import com.cloudweb.entity.Entreprise;
import com.cloudweb.entity.HistoriqueSignalements;
import com.cloudweb.entity.HistoriqueUsers;
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
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
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

    public void pushAllToFirebase() {
        WriteBatch batch = firestore.batch();

        for (StatutsUser statutsUser : statutsUserRepository.findAll()) {
            DocumentReference ref = firestore.collection("statuts_user")
                    .document(String.valueOf(statutsUser.getId()));
            batch.set(ref, Map.of(
                    "id", statutsUser.getId(),
                    "libelle", statutsUser.getLibelle(),
                    "updated_at", formatDate(statutsUser.getUpdatedAt())
            ));
        }

        for (UserType userType : userTypeRepository.findAll()) {
            DocumentReference ref = firestore.collection("user_type")
                    .document(String.valueOf(userType.getId()));
            batch.set(ref, Map.of(
                    "id", userType.getId(),
                    "libelle", userType.getLibelle(),
                    "updated_at", formatDate(userType.getUpdatedAt())
            ));
        }

        for (ReglesGestion regle : reglesGestionRepository.findAll()) {
            DocumentReference ref = firestore.collection("regles_gestion")
                    .document(String.valueOf(regle.getId()));
            batch.set(ref, Map.of(
                    "id", regle.getId(),
                    "libelle", regle.getLibelle(),
                    "valeur", regle.getValeur(),
                    "updated_at", formatDate(regle.getUpdatedAt())
            ));
        }

        for (TypeSignalement type : typeSignalementRepository.findAll()) {
            DocumentReference ref = firestore.collection("type_signalements")
                    .document(String.valueOf(type.getId()));
            batch.set(ref, Map.of(
                    "id", type.getId(),
                    "libelle", type.getLibelle(),
                    "updated_at", formatDate(type.getUpdatedAt())
            ));
        }

        for (Statuts statut : statutsRepository.findAll()) {
            DocumentReference ref = firestore.collection("statuts")
                    .document(String.valueOf(statut.getId()));
            batch.set(ref, Map.of(
                    "id", statut.getId(),
                    "libelle", statut.getLibelle(),
                    "updated_at", formatDate(statut.getUpdatedAt())
            ));
        }

        for (Entreprise entreprise : entrepriseRepository.findAll()) {
            DocumentReference ref = firestore.collection("entreprises")
                    .document(String.valueOf(entreprise.getId()));
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
            DocumentReference ref = firestore.collection("points")
                    .document(String.valueOf(point.getId()));
            batch.set(ref, Map.of(
                    "id", point.getId(),
                    "latitude", point.getLatitude(),
                    "longitude", point.getLongitude(),
                    "updated_at", formatDate(point.getUpdatedAt())
            ));
        }

        for (Signalements signalement : signalementsRepository.findAll()) {
            DocumentReference ref = firestore.collection("signalements")
                    .document(String.valueOf(signalement.getId()));
            Map<String, Object> data = new HashMap<>();
            data.put("id", signalement.getId());
            data.put("user_id", signalement.getUser() != null ? signalement.getUser().getId() : null);
            data.put("point_id", signalement.getPoint() != null ? signalement.getPoint().getId() : null);
            data.put("type_signalement_id",
                    signalement.getTypeSignalement() != null ? signalement.getTypeSignalement().getId() : null);
            data.put("date", signalement.getDate() != null ? signalement.getDate().toString() : null);
            data.put("surface", signalement.getSurface());
            data.put("budget", signalement.getBudget());
            data.put("statuts_id", signalement.getStatuts() != null ? signalement.getStatuts().getId() : null);
            data.put("entreprise_id",
                    signalement.getEntreprise() != null ? signalement.getEntreprise().getId() : null);
            data.put("updated_at", formatDate(signalement.getUpdatedAt()));
            batch.set(ref, data);
        }

        for (User user : userRepository.findAll()) {
            DocumentReference ref = firestore.collection("users")
                    .document(String.valueOf(user.getId()));
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("email", user.getEmail());
            data.put("password", user.getPassword());
            data.put("firebase_id", user.getFirebaseId());
            data.put("first_name", user.getFirstName());
            data.put("last_name", user.getLastName());
            data.put("date", user.getDate() != null ? user.getDate().toString() : null);
            data.put("failed_login_attempts", user.getFailedLoginAttempts());
            data.put("statuts_user_id",
                    user.getStatutsUser() != null ? user.getStatutsUser().getId() : null);
            data.put("user_type_id",
                    user.getUserType() != null ? user.getUserType().getId() : null);
            data.put("updated_at", formatDate(user.getUpdatedAt()));
            batch.set(ref, data);
        }

        for (HistoriqueSignalements historique : historiqueSignalementsRepository.findAll()) {
            DocumentReference ref = firestore.collection("historique_signalements")
                    .document(String.valueOf(historique.getId()));
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
            DocumentReference ref = firestore.collection("historique_users")
                    .document(String.valueOf(historique.getId()));
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
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firebase push failed: " + ex.getMessage(), ex);
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
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Firebase pull failed: " + ex.getMessage(), ex);
        }
    }

    @Async
    public void refreshAsync() {
        pullAllFromFirebase();
        pushAllToFirebase();
    }

    private void syncStatutsUser() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("statuts_user").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            StatutsUser existing = statutsUserRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            StatutsUser statutsUser = new StatutsUser();
            statutsUser.setId(id);
            statutsUser.setLibelle(doc.getString("libelle"));
            statutsUser.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            statutsUserRepository.save(statutsUser);
        }
    }

    private void syncUserTypes() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("user_type").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            UserType existing = userTypeRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            UserType userType = new UserType();
            userType.setId(id);
            userType.setLibelle(doc.getString("libelle"));
            userType.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            userTypeRepository.save(userType);
        }
    }

    private void syncReglesGestion() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("regles_gestion").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            ReglesGestion existing = reglesGestionRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            ReglesGestion regle = new ReglesGestion();
            regle.setId(id);
            regle.setLibelle(doc.getString("libelle"));
            regle.setValeur(doc.getString("valeur"));
            regle.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            reglesGestionRepository.save(regle);
        }
    }

    private void syncTypeSignalements() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("type_signalements").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            TypeSignalement existing = typeSignalementRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            TypeSignalement type = new TypeSignalement();
            type.setId(id);
            type.setLibelle(doc.getString("libelle"));
            type.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            typeSignalementRepository.save(type);
        }
    }

    private void syncStatuts() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("statuts").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            Statuts existing = statutsRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            Statuts statut = new Statuts();
            statut.setId(id);
            statut.setLibelle(doc.getString("libelle"));
            statut.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            statutsRepository.save(statut);
        }
    }

    private void syncEntreprises() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("entreprises").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            Entreprise existing = entrepriseRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            Entreprise entreprise = new Entreprise();
            entreprise.setId(id);
            entreprise.setName(doc.getString("name"));
            entreprise.setAddress(doc.getString("address"));
            entreprise.setPhone(doc.getString("phone"));
            Boolean active = doc.getBoolean("active");
            entreprise.setActive(active);
            entreprise.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            entrepriseRepository.save(entreprise);
        }
    }

    private void syncPoints() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("points").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            Point existing = pointRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            Point point = new Point();
            point.setId(id);
            point.setLatitude(getDouble(doc, "latitude"));
            point.setLongitude(getDouble(doc, "longitude"));
            point.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            pointRepository.save(point);
        }
    }

    private void syncUsers() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("users").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            User existing = userRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            User user = new User();
            user.setId(id);
            user.setEmail(doc.getString("email"));
            user.setPassword(doc.getString("password"));
            user.setFirebaseId(doc.getString("firebase_id"));
            user.setFirstName(doc.getString("first_name"));
            user.setLastName(doc.getString("last_name"));

            String dateValue = doc.getString("date");
            if (dateValue != null) {
                user.setDate(LocalDateTime.parse(dateValue));
            }

            Long statutsUserId = getLong(doc, "statuts_user_id");
            if (statutsUserId != null) {
                StatutsUser statutsUser = statutsUserRepository.findById(statutsUserId).orElse(null);
                user.setStatutsUser(statutsUser);
            }

            Long userTypeId = getLong(doc, "user_type_id");
            if (userTypeId != null) {
                UserType userType = userTypeRepository.findById(userTypeId).orElse(null);
                user.setUserType(userType);
            }

            Long failedAttempts = getLong(doc, "failed_login_attempts");
            user.setFailedLoginAttempts(failedAttempts != null ? failedAttempts.intValue() : 0);
            user.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());

            userRepository.save(user);
        }
    }

    private void syncSignalements() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("signalements").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            Signalements existing = signalementsRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            Signalements signalement = new Signalements();
            signalement.setId(id);
            signalement.setSurface(getDouble(doc, "surface"));
            signalement.setBudget(getDouble(doc, "budget"));

            String dateValue = doc.getString("date");
            if (dateValue != null) {
                signalement.setDate(LocalDateTime.parse(dateValue));
            }

            Long userId = getLong(doc, "user_id");
            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                signalement.setUser(user);
            }

            Long pointId = getLong(doc, "point_id");
            if (pointId != null) {
                Point point = pointRepository.findById(pointId).orElse(null);
                signalement.setPoint(point);
            }

            Long typeId = getLong(doc, "type_signalement_id");
            if (typeId != null) {
                TypeSignalement type = typeSignalementRepository.findById(typeId).orElse(null);
                signalement.setTypeSignalement(type);
            }

            Long statutId = getLong(doc, "statuts_id");
            if (statutId != null) {
                Statuts statut = statutsRepository.findById(statutId).orElse(null);
                signalement.setStatuts(statut);
            }

            Long entrepriseId = getLong(doc, "entreprise_id");
            if (entrepriseId != null) {
                Entreprise entreprise = entrepriseRepository.findById(entrepriseId).orElse(null);
                signalement.setEntreprise(entreprise);
            }

            signalement.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            signalementsRepository.save(signalement);
        }
    }

    private void syncHistoriqueSignalements() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("historique_signalements").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            HistoriqueSignalements existing = historiqueSignalementsRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            HistoriqueSignalements historique = new HistoriqueSignalements();
            historique.setId(id);

            Long signalementId = getLong(doc, "signalements_id");
            if (signalementId != null) {
                Signalements signalement = signalementsRepository.findById(signalementId).orElse(null);
                historique.setSignalements(signalement);
            }

            Long statutId = getLong(doc, "statuts_id");
            if (statutId != null) {
                Statuts statut = statutsRepository.findById(statutId).orElse(null);
                historique.setStatuts(statut);
            }

            String dateValue = doc.getString("date");
            if (dateValue != null) {
                historique.setDate(LocalDateTime.parse(dateValue));
            }

            historique.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            historiqueSignalementsRepository.save(historique);
        }
    }

    private void syncHistoriqueUsers() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("historique_users").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            HistoriqueUsers existing = historiqueUsersRepository.findById(id).orElse(null);
            LocalDateTime remoteUpdatedAt = getDate(doc, "updated_at");
            if (!shouldOverwrite(existing != null ? existing.getUpdatedAt() : null, remoteUpdatedAt)) {
                continue;
            }
            HistoriqueUsers historique = new HistoriqueUsers();
            historique.setId(id);

            Long userId = getLong(doc, "user_id");
            if (userId != null) {
                User user = userRepository.findById(userId).orElse(null);
                historique.setUser(user);
            }

            Long statutsUserId = getLong(doc, "statuts_user_id");
            if (statutsUserId != null) {
                StatutsUser statutsUser = statutsUserRepository.findById(statutsUserId).orElse(null);
                historique.setStatutsUser(statutsUser);
            }

            String dateValue = doc.getString("date");
            if (dateValue != null) {
                historique.setDate(LocalDateTime.parse(dateValue));
            }

            historique.setUpdatedAt(remoteUpdatedAt != null ? remoteUpdatedAt : LocalDateTime.now());
            historiqueUsersRepository.save(historique);
        }
    }

    private Long getLong(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                return null;
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
        String value = doc.getString(field);
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private String formatDate(LocalDateTime value) {
        return value != null ? value.toString() : null;
    }

    private boolean shouldOverwrite(LocalDateTime localUpdatedAt, LocalDateTime remoteUpdatedAt) {
        if (remoteUpdatedAt == null) {
            return false;
        }
        if (localUpdatedAt == null) {
            return true;
        }
        return remoteUpdatedAt.isAfter(localUpdatedAt);
    }
}
