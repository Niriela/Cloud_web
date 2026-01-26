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
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
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

        for (StatutsUser statutsUser : statutsUserRepository.findAll()) {
            DocumentReference ref = resolveRemoteDoc("statuts_user", statutsUser.getId(), batch);
            batch.set(ref, Map.of(
                    "id", statutsUser.getId(),
                    "libelle", statutsUser.getLibelle(),
                    "updated_at", formatDate(statutsUser.getUpdatedAt())
            ));
        }

        for (UserType userType : userTypeRepository.findAll()) {
            DocumentReference ref = resolveRemoteDoc("user_type", userType.getId(), batch);
            batch.set(ref, Map.of(
                    "id", userType.getId(),
                    "libelle", userType.getLibelle(),
                    "updated_at", formatDate(userType.getUpdatedAt())
            ));
        }

        for (ReglesGestion regle : reglesGestionRepository.findAll()) {
            DocumentReference ref = resolveRemoteDoc("regles_gestion", regle.getId(), batch);
            batch.set(ref, Map.of(
                    "id", regle.getId(),
                    "libelle", regle.getLibelle(),
                    "valeur", regle.getValeur(),
                    "updated_at", formatDate(regle.getUpdatedAt())
            ));
        }

        for (TypeSignalement type : typeSignalementRepository.findAll()) {
            DocumentReference ref = resolveRemoteDoc("type_signalements", type.getId(), batch);
            batch.set(ref, Map.of(
                    "id", type.getId(),
                    "libelle", type.getLibelle(),
                    "updated_at", formatDate(type.getUpdatedAt())
            ));
        }

        for (Statuts statut : statutsRepository.findAll()) {
            DocumentReference ref = resolveRemoteDoc("statuts", statut.getId(), batch);
            batch.set(ref, Map.of(
                    "id", statut.getId(),
                    "libelle", statut.getLibelle(),
                    "updated_at", formatDate(statut.getUpdatedAt())
            ));
        }

        for (Entreprise entreprise : entrepriseRepository.findAll()) {
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
            DocumentReference ref = resolveRemoteDoc("points", point.getId(), batch);
            batch.set(ref, Map.of(
                    "id", point.getId(),
                    "latitude", point.getLatitude(),
                    "longitude", point.getLongitude(),
                    "updated_at", formatDate(point.getUpdatedAt())
            ));
        }

        for (Signalements signalement : signalementsRepository.findAll()) {
            DocumentReference ref = resolveRemoteDoc("signalements", signalement.getId(), batch);
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
            DocumentReference ref = resolveRemoteDoc("users", user.getId(), batch);
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
                signalementsRepository.save(signalement);
            } catch (Exception ex) {
                log.warn("Skipping signalements {} due to error", doc.getId(), ex);
            }
        }
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
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
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
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
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
        } catch (InterruptedException | ExecutionException ex) {
            Thread.currentThread().interrupt();
        }
        return firestore.collection(collection).document(String.valueOf(id));
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
}
