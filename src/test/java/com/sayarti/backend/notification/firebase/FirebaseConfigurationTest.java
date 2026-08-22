package com.sayarti.backend.notification.firebase;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.firebase.FirebaseApp;
import java.security.KeyPairGenerator;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class FirebaseConfigurationTest {
    @Test
    void constructsFirebaseAppFromValidConfigurationAndEscapedNewlines() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        String encodedKey = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(generator.generateKeyPair().getPrivate().getEncoded());
        String privateKey = "-----BEGIN PRIVATE KEY-----\\n"
                + encodedKey.replace("\n", "\\n")
                + "\\n-----END PRIVATE KEY-----\\n";
        FirebaseProperties properties = new FirebaseProperties("test-project",
                "firebase@test-project.iam.gserviceaccount.com", privateKey);

        FirebaseApp app = new FirebaseConfiguration().firebaseApp(properties);
        try {
            assertThat(app.getOptions().getProjectId()).isEqualTo("test-project");
            assertThat(properties.normalizedPrivateKey()).contains("\n");
        } finally {
            app.delete();
        }
    }
}
