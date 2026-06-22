package com.trillionloans.customer_portal.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenUtil {

  @Value("${jwt-secret-key}")
  private String secretKey;

  private Key key;

  @PostConstruct
  public void init() {
    this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secretKey));
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    final Claims claims = extractAllClaims(token);
    return claimsResolver.apply(claims);
  }

  private Claims extractAllClaims(String token) {
    Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    return claims;
  }

  public String extractMobileNumber(String token) {
    return extractClaim(token, (claims) -> claims.get("mobileNumber", String.class));
  }

  public String extractDOB(String token) {
    return extractClaim(token, (claims) -> claims.get("dateOfBirth", String.class));
  }

  public String extractPanLast4Digits(String token) {
    return extractClaim(token, (claims) -> claims.get("panLast4Digits", String.class));
  }
}
