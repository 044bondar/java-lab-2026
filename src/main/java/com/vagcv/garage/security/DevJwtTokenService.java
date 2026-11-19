package com.vagcv.garage.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DevJwtTokenService {

    @Value("${app.security.local-jwt-secret}")
    private String secret;

    public String generateToken(String username, List<String> roles) {
        try {
            String headerJson = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";
            long now = Instant.now().getEpochSecond();
            long exp = now + 3600;
            String rolesJson = roles.stream()
                    .map(r -> "\"" + r + "\"")
                    .collect(Collectors.joining(","));
            String payloadJson = String.format(
                    "{\"sub\":\"%s\",\"preferred_username\":\"%s\",\"roles\":[%s],\"iat\":%d,\"exp\":%d}",
                    username, username, rolesJson, now, exp
            );

            String encodedHeader = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
            String signingInput = encodedHeader + "." + encodedPayload;

            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String signature = base64Url(mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8)));

            return signingInput + "." + signature;
        } catch (Exception e) {
            throw new IllegalStateException("Не вдалося згенерувати JWT токен", e);
        }
    }

    private String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
