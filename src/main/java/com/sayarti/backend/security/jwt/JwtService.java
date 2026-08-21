package com.sayarti.backend.security.jwt;
import com.sayarti.backend.user.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;
@Service
public class JwtService {
 private final JwtProperties properties; private final SecretKey key;
 public JwtService(JwtProperties properties){this.properties=properties; if(properties.accessSecret()==null || properties.accessSecret().getBytes(StandardCharsets.UTF_8).length<32) throw new IllegalStateException("JWT_ACCESS_SECRET must contain at least 32 bytes"); this.key=Keys.hmacShaKeyFor(properties.accessSecret().getBytes(StandardCharsets.UTF_8));}
 public String generateAccessToken(User user){Instant now=Instant.now();return Jwts.builder().subject(user.getId().toString()).claim("email",user.getEmail()).issuedAt(Date.from(now)).expiration(Date.from(now.plusMillis(properties.accessExpiration()))).signWith(key).compact();}
 public UUID parseUserId(String token){return UUID.fromString(Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());}
 public long accessExpirationSeconds(){return properties.accessExpiration()/1000;}
}
