package com.sayarti.backend.auth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.*;
import com.sayarti.backend.auth.repository.RefreshTokenRepository;
import com.sayarti.backend.user.repository.UserRepository;
import com.sayarti.backend.security.oauth.*;
import com.sayarti.backend.user.entity.User;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AuthenticationIntegrationTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users; @Autowired RefreshTokenRepository tokens; @Autowired PasswordEncoder encoder;
 @MockitoBean GoogleTokenVerifier googleTokens;
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
 @Test void createsGoogleUserAndMapsStandardAuthenticationResponse()throws Exception{
  org.mockito.Mockito.when(googleTokens.verify("valid-google-token")).thenReturn(new GoogleIdentity("google-123","Google.User@Example.com","Google","User"));
  JsonNode response=google("valid-google-token",status().isOk());
  assertThat(response.at("/data/accessToken").asText()).isNotBlank();assertThat(response.at("/data/refreshToken").asText()).isNotBlank();
  assertThat(response.at("/data/user/email").asText()).isEqualTo("google.user@example.com");assertThat(response.at("/data/user/authProvider").asText()).isEqualTo("GOOGLE");
  assertThat(tokens.count()).isEqualTo(1);assertThat(users.findByGoogleSubjectAndDeletedAtIsNull("google-123")).isPresent();
 }
 @Test void logsInExistingGoogleUserWithoutCreatingAnother()throws Exception{
  users.saveAndFlush(User.google("Existing","Google","existing@example.com","existing-subject"));
  org.mockito.Mockito.when(googleTokens.verify("existing-token")).thenReturn(new GoogleIdentity("existing-subject","existing@example.com","Ignored","Profile"));
  google("existing-token",status().isOk());assertThat(users.count()).isEqualTo(1);assertThat(tokens.count()).isEqualTo(1);
 }
 @Test void doesNotAutomaticallyLinkExistingLocalAccount()throws Exception{
  register("local@example.com");tokens.deleteAll();
  org.mockito.Mockito.when(googleTokens.verify("same-email-token")).thenReturn(new GoogleIdentity("new-subject","LOCAL@example.com","Untrusted","Name"));
  JsonNode response=google("same-email-token",status().isConflict());assertThat(response.at("/error/code").asText()).isEqualTo("AUTH_ACCOUNT_LINKING_REQUIRED");
  assertThat(users.findByEmailIgnoreCaseAndDeletedAtIsNull("local@example.com").orElseThrow().getAuthProvider().name()).isEqualTo("LOCAL");
 }
 @Test void rejectsInvalidGoogleToken()throws Exception{
  org.mockito.Mockito.when(googleTokens.verify("invalid-token")).thenThrow(new GoogleAuthenticationException("Google ID token is invalid or expired"));
  JsonNode response=google("invalid-token",status().isUnauthorized());assertThat(response.at("/error/code").asText()).isEqualTo("AUTH_GOOGLE_LOGIN_FAILED");assertThat(tokens.count()).isZero();
 }
 private JsonNode google(String token,org.springframework.test.web.servlet.ResultMatcher expected)throws Exception{return body(mvc.perform(post("/api/v1/auth/google").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("idToken",token)))).andExpect(expected).andReturn());}
}
