package com.sayarti.backend.security.jwt;
import static org.assertj.core.api.Assertions.*;

import com.sayarti.backend.AbstractIntegrationTest;
import com.sayarti.backend.user.entity.User;
import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.Test;
class JwtServiceTest extends AbstractIntegrationTest {
 private static final String SECRET="a-unit-test-secret-that-is-definitely-long-enough";
 @Test void generatesAndValidatesAccessToken(){JwtService service=new JwtService(new JwtProperties(SECRET,60_000,1));User user=new User("A","B","a@example.com","hash");assertThat(service.parseUserId(service.generateAccessToken(user))).isEqualTo(user.getId());}
 @Test void rejectsExpiredAccessToken()throws Exception{JwtService service=new JwtService(new JwtProperties(SECRET,1,1));String token=service.generateAccessToken(new User("A","B","a@example.com","hash"));Thread.sleep(10);assertThatThrownBy(()->service.parseUserId(token)).isInstanceOf(ExpiredJwtException.class);}
}
