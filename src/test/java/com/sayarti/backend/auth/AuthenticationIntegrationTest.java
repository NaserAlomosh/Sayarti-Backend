package com.sayarti.backend.auth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.user.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AuthenticationIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users; @Autowired RefreshTokenRepository tokens; @Autowired PasswordEncoder encoder;
 @BeforeEach void clear(){tokens.deleteAll();users.deleteAll();}
 private JsonNode register(String email)throws Exception{return body(mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
 {"firstName":"Sara","lastName":"Ali","email":"%s","password":"StrongPass1"}
 """.formatted(email))).andExpect(status().isCreated()).andExpect(jsonPath("$.success").value(true)).andReturn());}
 private JsonNode body(MvcResult r)throws Exception{return json.readTree(r.getResponse().getContentAsString());}
 @Test void registersAndHashesPassword()throws Exception{JsonNode response=register("Sara@Example.com");assertThat(response.at("/data/user/email").asText()).isEqualTo("sara@example.com");var user=users.findByEmailIgnoreCaseAndDeletedAtIsNull("sara@example.com").orElseThrow();assertThat(user.getPasswordHash()).doesNotContain("StrongPass1");assertThat(encoder.matches("StrongPass1",user.getPasswordHash())).isTrue();}
 @Test void rejectsDuplicateEmail()throws Exception{register("same@example.com");mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""{"firstName":"A","lastName":"B","email":"SAME@example.com","password":"StrongPass1"}""")).andExpect(status().isConflict()).andExpect(jsonPath("$.error.code").value("AUTH_EMAIL_ALREADY_EXISTS"));}
 @Test void validatesRegistrationDto()throws Exception{mvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content("""{"firstName":"","lastName":"B","email":"bad","password":"weak"}""")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));}
 @Test void logsInAndRejectsBadCredentials()throws Exception{register("login@example.com");mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""{"email":"login@example.com","password":"StrongPass1"}""")).andExpect(status().isOk()).andExpect(jsonPath("$.data.accessToken").isNotEmpty()).andExpect(jsonPath("$.data.refreshToken").isNotEmpty());mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""{"email":"login@example.com","password":"wrong"}""")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("AUTH_INVALID_CREDENTIALS"));}
 @Test void protectsCurrentUserAndAcceptsValidJwt()throws Exception{mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());String access=register("me@example.com").at("/data/accessToken").asText();mvc.perform(get("/api/v1/users/me").header("Authorization","Bearer "+access)).andExpect(status().isOk()).andExpect(jsonPath("$.data.email").value("me@example.com"));mvc.perform(get("/api/v1/users/me").header("Authorization","Bearer broken.token")).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("AUTH_INVALID_TOKEN"));}
 @Test void rotatesRefreshTokenAndRejectsReuse()throws Exception{String old=register("rotate@example.com").at("/data/refreshToken").asText();JsonNode refreshed=body(mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("refreshToken",old)))).andExpect(status().isOk()).andReturn());String next=refreshed.at("/data/refreshToken").asText();assertThat(next).isNotEqualTo(old);mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("refreshToken",old)))).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.error.code").value("AUTH_REFRESH_TOKEN_REUSED"));}
 @Test void logoutRevokesRefreshButAccessRemainsUntilExpiry()throws Exception{JsonNode session=register("logout@example.com");String refresh=session.at("/data/refreshToken").asText(),access=session.at("/data/accessToken").asText();mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("refreshToken",refresh)))).andExpect(status().isOk());mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("refreshToken",refresh)))).andExpect(status().isUnauthorized());mvc.perform(get("/api/v1/users/me").header("Authorization","Bearer "+access)).andExpect(status().isOk());}
}
