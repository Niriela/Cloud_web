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
import java.util.List;
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
                    "libelle", statutsUser.getLibelle()
            ));
        }

        for (UserType userType : userTypeRepository.findAll()) {
            DocumentReference ref = firestore.collection("user_type")
                    .document(String.valueOf(userType.getId()));
            batch.set(ref, Map.of(
                    "id", userType.getId(),
                    "libelle", userType.getLibelle()
            ));
        }

        for (ReglesGestion regle : reglesGestionRepository.findAll()) {
            DocumentReference ref = firestore.collection("regles_gestion")
                    .document(String.valueOf(regle.getId()));
            batch.set(ref, Map.of(
                    "id", regle.getId(),
                    "libelle", regle.getLibelle(),
                    "valeur", regle.getValeur()
            ));
        }

        for (TypeSignalement type : typeSignalementRepository.findAll()) {
            DocumentReference ref = firestore.collection("type_signalements")
                    .document(String.valueOf(type.getId()));
            batch.set(ref, Map.of(
                    "id", type.getId(),
                    "libelle", type.getLibelle()
            ));
        }

        for (Statuts statut : statutsRepository.findAll()) {
            DocumentReference ref = firestore.collection("statuts")
                    .document(String.valueOf(statut.getId()));
            batch.set(ref, Map.of(
                    "id", statut.getId(),
                    "libelle", statut.getLibelle()
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
                    "active", entreprise.getActive()
            ));
        }

        for (Point point : pointRepository.findAll()) {
            DocumentReference ref = firestore.collection("points")
                    .document(String.valueOf(point.getId()));
            batch.set(ref, Map.of(
                    "id", point.getId(),
                    "latitude", point.getLatitude(),
                    "longitude", point.getLongitude()
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
            batch.set(ref, data);
        }

        for (User user : userRepository.findAll()) {
            DocumentReference ref = firestore.collection("users")
                    .document(String.valueOf(user.getId()));
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId());
            data.put("email", user.getEmail());
            data.put("password", user.getPassword());
            data.put("first_name", user.getFirstName());
            data.put("last_name", user.getLastName());
            data.put("date", user.getDate() != null ? user.getDate().toString() : null);
            data.put("failed_login_attempts", user.getFailedLoginAttempts());
            data.put("statuts_user_id",
                    user.getStatutsUser() != null ? user.getStatutsUser().getId() : null);
            data.put("user_type_id",
                    user.getUserType() != null ? user.getUserType().getId() : null);
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
        pushAllToFirebase();
        pullAllFromFirebase();
    }

    private void syncStatutsUser() throws ExecutionException, InterruptedException {
        QuerySnapshot snapshot = firestore.collection("statuts_user").get().get();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Long id = getLong(doc, "id");
            if (id == null) {
                id = Long.parseLong(doc.getId());
            }
            StatutsUser statutsUser = new StatutsUser();
            statutsUser.setId(id);
            statutsUser.setLibelle(doc.getString("libelle"));
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
            UserType userType = new UserType();
            userType.setId(id);
            userType.setLibelle(doc.getString("libelle"));
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
            ReglesGestion regle = new ReglesGestion();
            regle.setId(id);
            regle.setLibelle(doc.getString("libelle"));
            regle.setValeur(doc.getString("valeur"));
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
            TypeSignalement type = new TypeSignalement();
            type.setId(id);
            type.setLibelle(doc.getString("libelle"));
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
            Statuts statut = new Statuts();
            statut.setId(id);
            statut.setLibelle(doc.getString("libelle"));
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
            Entreprise entreprise = new Entreprise();
            entreprise.setId(id);
            entreprise.setName(doc.getString("name"));
            entreprise.setAddress(doc.getString("address"));
            entreprise.setPhone(doc.getString("phone"));
            Boolean active = doc.getBoolean("active");
            entreprise.setActive(active);
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
            Point point = new Point();
            point.setId(id);
            point.setLatitude(getDouble(doc, "latitude"));
            point.setLongitude(getDouble(doc, "longitude"));
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
            User user = new User();
            user.setId(id);
            user.setEmail(doc.getString("email"));
            user.setPassword(doc.getString("password"));
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
}
