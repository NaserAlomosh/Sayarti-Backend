package com.sayarti.backend.auth.service;
import com.sayarti.backend.auth.dto.*;
import com.sayarti.backend.common.exception.*;
import com.sayarti.backend.security.jwt.JwtService;
import com.sayarti.backend.user.dto.UserResponse;
import com.sayarti.backend.user.entity.User;
import com.sayarti.backend.user.repository.UserRepository;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class AuthService {
 private final UserRepository users;private final PasswordEncoder encoder;private final JwtService jwt;private final RefreshTokenService refreshTokens;
 public AuthService(UserRepository users,PasswordEncoder encoder,JwtService jwt,RefreshTokenService refreshTokens){this.users=users;this.encoder=encoder;this.jwt=jwt;this.refreshTokens=refreshTokens;}
 @Transactional public AuthResponse register(RegisterRequest r){String email=normalize(r.email());if(users.existsByEmailIgnoreCase(email))throw conflict();User user=new User(r.firstName().trim(),r.lastName().trim(),email,encoder.encode(r.password()));try{users.saveAndFlush(user);}catch(DataIntegrityViolationException e){throw conflict();}return tokens(user);}
 @Transactional public AuthResponse login(LoginRequest r){User user=users.findByEmailIgnoreCaseAndDeletedAtIsNull(normalize(r.email())).orElseThrow(this::invalidCredentials);if(!encoder.matches(r.password(),user.getPasswordHash()))throw invalidCredentials();return tokens(user);}
 @Transactional public AuthResponse refresh(RefreshRequest r){var rotation=refreshTokens.rotate(r.refreshToken());return response(rotation.user(),rotation.refreshToken());}
 @Transactional public void logout(RefreshRequest r){refreshTokens.revoke(r.refreshToken());}
 private AuthResponse tokens(User user){return response(user,refreshTokens.issue(user).raw());}
 private AuthResponse response(User u,String refresh){return new AuthResponse(jwt.generateAccessToken(u),refresh,"Bearer",jwt.accessExpirationSeconds(),UserResponse.from(u));}
 private String normalize(String email){return email.trim().toLowerCase(Locale.ROOT);}
 private ApiException invalidCredentials(){return new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS,HttpStatus.UNAUTHORIZED,"Email or password is incorrect");}
 private ApiException conflict(){return new ApiException(ErrorCode.AUTH_EMAIL_ALREADY_EXISTS,HttpStatus.CONFLICT,"An account with this email already exists");}
}
