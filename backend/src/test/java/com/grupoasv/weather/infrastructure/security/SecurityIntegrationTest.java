package com.grupoasv.weather.infrastructure.security;

import com.grupoasv.weather.domain.model.Municipio;
import com.grupoasv.weather.domain.port.out.WeatherExternalPort;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba la cadena de seguridad completa (login, emisión y validación del JWT, CORS) con la aplicación real.
 * Solo se simula el puerto de salida hacia AEMET.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:test-secrets.properties")
class SecurityIntegrationTest {

    private static final String LOGIN_URL = "/api/v1/auth/login";
    private static final String PROTECTED_URL = "/api/v1/weather/municipalities?prefix=Ag";
    private static final String ISSUER = "asv-weather-api";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @MockitoBean
    private WeatherExternalPort weatherExternalPort;

    @Test
    @DisplayName("Login - should return a Bearer token with valid credentials")
    void login_ShouldReturnBearerTokenWithValidCredentials() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("test-user", "test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.accessToken", not(emptyString())));
    }

    @Test
    @DisplayName("Login - should return 401 with the same message for wrong password and unknown user")
    void login_ShouldReturnUnauthorizedWithoutRevealingWhichFieldIsWrong() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("test-user", "wrong-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"));

        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("unknown-user", "test-password")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Usuario o contraseña incorrectos"));
    }

    @Test
    @DisplayName("Login - should return 400 when fields are blank")
    void login_ShouldReturnBadRequestWhenFieldsAreBlank() throws Exception {
        mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Protected endpoint - should return 401 without token")
    void protectedEndpoint_ShouldReturnUnauthorizedWithoutToken() throws Exception {
        mockMvc.perform(get(PROTECTED_URL))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, containsString("Bearer")));
    }

    @Test
    @DisplayName("Protected endpoint - should return 200 with the token obtained from login")
    void protectedEndpoint_ShouldReturnOkWithTokenFromLogin() throws Exception {
        when(weatherExternalPort.fetchAllMunicipalities()).thenReturn(List.of(new Municipio("03002", "Agost")));
        String loginResponse = mockMvc.perform(post(LOGIN_URL).contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("test-user", "test-password")))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginResponse, "$.accessToken");

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Agost"));
    }

    @Test
    @DisplayName("Protected endpoint - should return 401 with an expired token")
    void protectedEndpoint_ShouldReturnUnauthorizedWithExpiredToken() throws Exception {
        Instant twoHoursAgo = Instant.now().minus(2, ChronoUnit.HOURS);
        String expiredToken = sign(jwtEncoder, ISSUER, twoHoursAgo, twoHoursAgo.plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected endpoint - should return 401 with a token signed with another key (forged)")
    void protectedEndpoint_ShouldReturnUnauthorizedWithForgedToken() throws Exception {
        byte[] otherKey = "otra-clave-distinta-de-al-menos-32-bytes".getBytes(StandardCharsets.UTF_8);
        JwtEncoder attackerEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(otherKey, "HmacSHA256")));
        String forgedToken = sign(attackerEncoder, ISSUER, Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + forgedToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Protected endpoint - should return 401 with a token from another issuer")
    void protectedEndpoint_ShouldReturnUnauthorizedWithForeignIssuer() throws Exception {
        String foreignToken = sign(jwtEncoder, "otro-emisor", Instant.now(), Instant.now().plus(1, ChronoUnit.HOURS));

        mockMvc.perform(get(PROTECTED_URL).header(HttpHeaders.AUTHORIZATION, "Bearer " + foreignToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("CORS - should allow the frontend origin")
    void cors_ShouldAllowFrontendOrigin() throws Exception {
        mockMvc.perform(options(PROTECTED_URL)
                        .header(HttpHeaders.ORIGIN, "http://localhost:4200")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:4200"));
    }

    @Test
    @DisplayName("CORS - should reject unknown origins")
    void cors_ShouldRejectUnknownOrigins() throws Exception {
        mockMvc.perform(options(PROTECTED_URL)
                        .header(HttpHeaders.ORIGIN, "https://sitio-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    private static String credentials(String username, String password) {
        return """
                {"username": "%s", "password": "%s"}
                """.formatted(username, password);
    }

    private static String sign(JwtEncoder encoder, String issuer, Instant issuedAt, Instant expiresAt) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("test-user")
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
