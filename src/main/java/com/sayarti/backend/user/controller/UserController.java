package com.sayarti.backend.user.controller;
import com.sayarti.backend.common.response.ApiResponse;
import com.sayarti.backend.security.jwt.AuthenticatedUser;
import com.sayarti.backend.user.dto.UserResponse;
import com.sayarti.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/users") @Tag(name="Users",description="Authenticated user profile") @SecurityRequirement(name="bearerAuth")
public class UserController {private final UserService service;public UserController(UserService service){this.service=service;}
 @GetMapping("/me") @Operation(summary="Get the current authenticated user") @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200",description="Current user"),@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401",description="Authentication required")})
 public ApiResponse<UserResponse> me(@AuthenticationPrincipal AuthenticatedUser user){return ApiResponse.success(service.current(user));}}
