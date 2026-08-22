package com.sayarti.backend.notification.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.mock.env.MockEnvironment;

class FirebaseConfigurationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void constructsFirebaseAppAndMessagingFromServiceAccountFile() throws Exception {
        Path credentialFile = writeValidServiceAccount();
        boolean appAlreadyExisted = FirebaseApp.getApps().stream()
                .anyMatch(candidate -> FirebaseConfiguration.APP_NAME.equals(candidate.getName()));

        FirebaseApp app = new FirebaseConfiguration()
                .firebaseApp(new FirebaseProperties(credentialFile.toString()));
        try {
            assertThat(app.getOptions().getProjectId()).isEqualTo("test-project");
            assertThat(new FirebaseConfiguration().firebaseMessaging(app))
                    .isInstanceOf(FirebaseMessaging.class);
            assertThat(new FirebaseConfiguration()
                    .firebaseApp(new FirebaseProperties(temporaryDirectory
                            .resolve("not-needed-when-app-exists.json").toString())))
                    .isSameAs(app);
        } finally {
            if (!appAlreadyExisted) {
                app.delete();
            }
        }
    }

    @Test
    void rejectsMissingBlankAndNonexistentPathsWithoutExposingCredentialData() {
        FirebaseConfiguration configuration = new FirebaseConfiguration();

        assertThatThrownBy(() -> configuration.firebaseApp(new FirebaseProperties(null)))
                .isInstanceOf(IOException.class)
                .hasMessage("Firebase service-account path is not configured");
        assertThatThrownBy(() -> configuration.firebaseApp(new FirebaseProperties("  ")))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> configuration.firebaseApp(
                new FirebaseProperties(temporaryDirectory.resolve("missing.json").toString())))
                .isInstanceOf(IOException.class);
    }

    @Test
    void rejectsUnreadableAndInvalidCredentialFiles() throws Exception {
        Path invalid = temporaryDirectory.resolve("invalid.json");
        Files.writeString(invalid, "not a credential");

        assertThatThrownBy(() -> new FirebaseConfiguration()
                .firebaseApp(new FirebaseProperties(temporaryDirectory.toString())))
                .isInstanceOf(IOException.class);
        assertThatThrownBy(() -> new FirebaseConfiguration()
                .firebaseApp(new FirebaseProperties(invalid.toString())))
                .isInstanceOf(IOException.class);
    }

    @Test
    void conditionEnablesFirebaseOnlyForReadableValidServiceAccount() throws Exception {
        FirebaseConfiguredCondition condition = new FirebaseConfiguredCondition();

        assertThat(matches(condition, null)).isFalse();
        assertThat(matches(condition, temporaryDirectory.resolve("missing.json").toString()))
                .isFalse();
        assertThat(matches(condition, temporaryDirectory.toString())).isFalse();
        Path invalid = temporaryDirectory.resolve("invalid.json");
        Files.writeString(invalid, "{}");
        assertThat(matches(condition, invalid.toString())).isFalse();
        assertThat(matches(condition, writeValidServiceAccount().toString())).isTrue();
    }

    @Test
    void conditionCanDisableFirebaseDespiteAValidCredentialPath() throws Exception {
        FirebaseConfiguredCondition condition = new FirebaseConfiguredCondition();
        MockEnvironment environment = new MockEnvironment()
                .withProperty("sayarti.firebase.enabled", "false")
                .withProperty("sayarti.firebase.service-account-path",
                        writeValidServiceAccount().toString());
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);

        assertThat(condition.matches(context, null)).isFalse();
    }

    private boolean matches(FirebaseConfiguredCondition condition, String path) {
        MockEnvironment environment = new MockEnvironment();
        if (path != null) {
            environment.setProperty("sayarti.firebase.service-account-path", path);
        }
        ConditionContext context = mock(ConditionContext.class);
        when(context.getEnvironment()).thenReturn(environment);
        return condition.matches(context, null);
    }

    private Path writeValidServiceAccount() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String encodedKey = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        String privateKey = "-----BEGIN PRIVATE KEY-----\n" + encodedKey
                + "\n-----END PRIVATE KEY-----\n";
        String json = """
                {"type":"service_account","project_id":"test-project",
                 "private_key_id":"test-key","private_key":"%s",
                 "client_email":"firebase@test-project.iam.gserviceaccount.com",
                 "client_id":"123456789","token_uri":"https://oauth2.googleapis.com/token"}
                """.formatted(privateKey.replace("\n", "\\n"));
        Path file = temporaryDirectory.resolve("generated-service-account.json");
        Files.writeString(file, json);
        return file;
    }
}
