package com.jigmnnaga.global.jwt;

import com.jigmnnaga.application.exception.BusinessException;
import com.jigmnnaga.application.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@EnableConfigurationProperties(JwtProperties.class)
public class JwtProvider {

    private static final String TOKEN_TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";
    private static final String REFRESH_TOKEN_TYPE = "refresh";

    private final SecretKey key;
    private final long accessExpiration;
    private final long refreshExpiration;

    public JwtProvider(JwtProperties jwtProperties) {
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessExpiration = jwtProperties.accessExpiration();
        this.refreshExpiration = jwtProperties.refreshExpiration();
    }

    public String createAccessToken(Long memberId) {
        return createToken(memberId, ACCESS_TOKEN_TYPE, accessExpiration);
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, REFRESH_TOKEN_TYPE, refreshExpiration);
    }

    public Long getMemberIdFromAccessToken(String token) {
        return parseMemberId(token, ACCESS_TOKEN_TYPE, ErrorCode.EXPIRED_ACCESS_TOKEN, ErrorCode.INVALID_ACCESS_TOKEN);
    }

    public Long getMemberIdFromRefreshToken(String token) {
        return parseMemberId(token, REFRESH_TOKEN_TYPE, ErrorCode.EXPIRED_REFRESH_TOKEN, ErrorCode.INVALID_REFRESH_TOKEN);
    }

    private Long parseMemberId(String token, String expectedType, ErrorCode expiredErrorCode,
                               ErrorCode invalidErrorCode) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!expectedType.equals(claims.get(TOKEN_TYPE_CLAIM, String.class))) {
                throw new BusinessException(invalidErrorCode);
            }
            return Long.valueOf(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new BusinessException(expiredErrorCode);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(invalidErrorCode);
        }
    }

    private String createToken(Long memberId, String type, long expiration) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(memberId))
                .claim(TOKEN_TYPE_CLAIM, type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }
}
