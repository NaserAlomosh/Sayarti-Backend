package com.sayarti.backend.auth.service;
import java.security.SecureRandom;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
@Service
public class OtpSecurityService {
    private final SecureRandom random = new SecureRandom();
    private final PasswordEncoder encoder;
    public OtpSecurityService(PasswordEncoder encoder) { this.encoder = encoder; }
    public String generate() { return "%06d".formatted(random.nextInt(1_000_000)); }
    public String hash(String otp) { return encoder.encode(otp); }
    public boolean matches(String otp, String hash) { return encoder.matches(otp, hash); }
}
