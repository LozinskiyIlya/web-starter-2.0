package com.starter.web.service.auth;

import com.starter.web.configuration.auth.JwtProperties;
import io.jsonwebtoken.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/**
 * @author ilya
 * @date 08.11.2021
 */

@Service
@Log
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties jwtProperties;

    public String generateToken(String login) {
        Date date = Date.from(LocalDate.now().plusDays(181).atStartOfDay(ZoneId.systemDefault()).toInstant());
        return Jwts.builder()
                .setSubject(login)
                .setExpiration(date)
                .signWith(SignatureAlgorithm.HS512, jwtProperties.getSecret())
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token);
            return true;
        } catch (ExpiredJwtException expEx) {
            log.severe("Token expired");
        } catch (UnsupportedJwtException unsEx) {
            log.severe("Unsupported jwt");
        } catch (MalformedJwtException mjEx) {
            log.severe("Malformed jwt");
        } catch (SignatureException sEx) {
            log.severe("Invalid signature");
        } catch (Exception e) {
            log.severe("invalid token");
        }
        return false;
    }

    public String getLoginFromToken(String token) {
        Claims claims = Jwts.parser().setSigningKey(jwtProperties.getSecret()).parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
}
