package com.cloudweb.config;

import com.cloudweb.entity.ReglesGestion;
import com.cloudweb.entity.Statuts;
import com.cloudweb.entity.StatutsPourcentage;
import com.cloudweb.entity.StatutsUser;
import com.cloudweb.entity.TypeSignalement;
import com.cloudweb.entity.User;
import com.cloudweb.entity.UserType;
import com.cloudweb.repository.ReglesGestionRepository;
import com.cloudweb.repository.StatutsRepository;
import com.cloudweb.repository.StatutsPourcentageRepository;
import com.cloudweb.repository.StatutsUserRepository;
import com.cloudweb.repository.TypeSignalementRepository;
import com.cloudweb.repository.UserRepository;
import com.cloudweb.repository.UserTypeRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseBootstrapRunner implements ApplicationRunner {

    private final StatutsUserRepository statutsUserRepository;
    private final UserTypeRepository userTypeRepository;
    private final ReglesGestionRepository reglesGestionRepository;
    private final StatutsRepository statutsRepository;
    private final StatutsPourcentageRepository statutsPourcentageRepository;
    private final TypeSignalementRepository typeSignalementRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.bootstrap.mode:seed}")
    private String bootstrapMode;

    @Value("${app.bootstrap.seed-default-manager:true}")
    private boolean seedDefaultManager;

    @Value("${app.bootstrap.manager-email:manager@gmail.com}")
    private String managerEmail;

    @Value("${app.bootstrap.manager-password:123456}")
    private String managerPassword;

    @Value("${app.bootstrap.manager-first-name:Manager}")
    private String managerFirstName;

    @Value("${app.bootstrap.manager-last-name:Admin}")
    private String managerLastName;

    @Override
    public void run(ApplicationArguments args) {
        String mode = normalizeMode(bootstrapMode);
        log.info("Database bootstrap mode: {}", mode);

        if ("none".equals(mode)) {
            return;
        }

        seedBaselineData();
    }

    private void seedBaselineData() {
        StatutsUser actif = ensureStatutsUser("Actif");
        ensureStatutsUser("Bloque");
        ensureStatutsUser("Banni");

        ensureUserType("Visiteur");
        ensureUserType("Utilisateur");
        UserType managerType = ensureUserType("Manager");

        ensureRegle("Duree_vie_session", "30");
        ensureRegle("Nombre_tentative_connexion", "3");

        Statuts nouveau = ensureStatut("Nouveau");
        Statuts enCours = ensureStatut("En cours");
        Statuts termine = ensureStatut("Terminé");
        Statuts annule = ensureStatut("Annulé");
        ensureStatutPourcentage(nouveau, 0);
        ensureStatutPourcentage(enCours, 50);
        ensureStatutPourcentage(termine, 100);
        ensureStatutPourcentage(annule, 0);

        ensureTypeSignalement("En construction");
        ensureTypeSignalement("Accident");
        ensureTypeSignalement("Nid de poule");
        ensureTypeSignalement("Réparé");
        ensureTypeSignalement("Abîmé");
        ensureTypeSignalement("Alerte");
        ensureTypeSignalement("Zone rouge");
        ensureTypeSignalement("Fuite / eau");
        ensureTypeSignalement("EFT");

        if (seedDefaultManager) {
            ensureDefaultManager(actif, managerType);
        }
    }

    private void ensureDefaultManager(StatutsUser actif, UserType managerType) {
        String email = normalizeEmail(managerEmail);
        if (email.isBlank()) {
            log.warn("Default manager email is blank, skipping manager seed.");
            return;
        }
        if (userRepository.existsByEmail(email)) {
            return;
        }

        String password = managerPassword == null || managerPassword.isBlank() ? "123456" : managerPassword;
        String firstName = managerFirstName == null || managerFirstName.isBlank() ? "Manager" : managerFirstName;
        String lastName = managerLastName == null || managerLastName.isBlank() ? "Admin" : managerLastName;

        User manager = User.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .firstName(firstName)
                .lastName(lastName)
                .failedLoginAttempts(0)
                .statutsUser(actif)
                .userType(managerType)
                .build();
        userRepository.save(manager);

        log.info("Default manager user created with email {}", email);
    }

    private StatutsUser ensureStatutsUser(String libelle) {
        return statutsUserRepository.findByLibelle(libelle)
                .orElseGet(() -> statutsUserRepository.save(StatutsUser.builder().libelle(libelle).build()));
    }

    private UserType ensureUserType(String libelle) {
        return userTypeRepository.findByLibelle(libelle)
                .orElseGet(() -> userTypeRepository.save(UserType.builder().libelle(libelle).build()));
    }

    private ReglesGestion ensureRegle(String libelle, String valeur) {
        return reglesGestionRepository.findByLibelle(libelle)
                .orElseGet(() -> reglesGestionRepository.save(ReglesGestion.builder()
                        .libelle(libelle)
                        .valeur(valeur)
                        .build()));
    }

    private Statuts ensureStatut(String libelle) {
        return statutsRepository.findByLibelleIgnoreCase(libelle)
                .orElseGet(() -> statutsRepository.save(Statuts.builder().libelle(libelle).build()));
    }

    private void ensureStatutPourcentage(Statuts statut, int pourcentage) {
        if (statut == null || statut.getId() == null) {
            return;
        }
        StatutsPourcentage entity = statutsPourcentageRepository.findByStatutsId(statut.getId())
                .orElseGet(() -> StatutsPourcentage.builder().statuts(statut).build());
        entity.setPourcentage(pourcentage);
        statutsPourcentageRepository.save(entity);
    }

    private TypeSignalement ensureTypeSignalement(String libelle) {
        return typeSignalementRepository.findByLibelleIgnoreCase(libelle)
                .orElseGet(() -> typeSignalementRepository.save(TypeSignalement.builder().libelle(libelle).build()));
    }

    private String normalizeMode(String value) {
        if (value == null || value.isBlank()) {
            return "seed";
        }
        String mode = value.trim().toLowerCase(Locale.ROOT);
        if ("firestore".equals(mode)) {
            log.warn("APP_BOOTSTRAP_MODE=firestore is disabled. Falling back to seed mode to keep sync manual.");
            return "seed";
        }
        return mode;
    }

    private String normalizeEmail(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
