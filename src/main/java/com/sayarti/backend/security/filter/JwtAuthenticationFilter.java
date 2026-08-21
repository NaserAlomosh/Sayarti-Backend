package com.sayarti.backend.security.filter;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sayarti.backend.common.exception.ErrorCode;
import com.sayarti.backend.common.response.ErrorResponse;
import com.sayarti.backend.security.jwt.*;
import com.sayarti.backend.user.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
 private final JwtService jwtService; private final UserRepository users; private final ObjectMapper mapper;
 public JwtAuthenticationFilter(JwtService jwtService,UserRepository users,ObjectMapper mapper){this.jwtService=jwtService;this.users=users;this.mapper=mapper;}
 @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain)throws ServletException,IOException{
  String header=req.getHeader("Authorization");
  if(header==null||!header.startsWith("Bearer ")){chain.doFilter(req,res);return;}
  try {var id=jwtService.parseUserId(header.substring(7)); var user=users.findByIdAndDeletedAtIsNull(id).orElseThrow();
   var principal=new AuthenticatedUser(user.getId(),user.getEmail());
   SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,java.util.List.of())); chain.doFilter(req,res);
  } catch(ExpiredJwtException e){write(res,ErrorCode.AUTH_TOKEN_EXPIRED,"Access token has expired");}
    catch(Exception e){write(res,ErrorCode.AUTH_INVALID_TOKEN,"Access token is invalid");}
 }
 private void write(HttpServletResponse res,ErrorCode code,String message)throws IOException{SecurityContextHolder.clearContext();res.setStatus(401);res.setContentType(MediaType.APPLICATION_JSON_VALUE);mapper.writeValue(res.getOutputStream(),ErrorResponse.of(code.name(),message));}
}
