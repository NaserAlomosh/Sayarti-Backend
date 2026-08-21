package com.sayarti.backend.security.oauth;

public record GoogleIdentity(String subject, String email, String givenName, String familyName) {}
