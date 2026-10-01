package br.com.fiap.medistockbackend.security;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:seguranca-test?mode=memory&cache=shared",
                "medistock.jwt.secret=ZmFrZS10ZXN0LXNlY3JldC1rZXktd2l0aC1hdC1sZWFzdC0zMi1ieXRlcw==",
                "medistock.gemini.api-key="
        })
class SegurancaApiTest {

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int porta;

    @Test
    void deveRetornar401SemToken() throws Exception {
        HttpResponse<String> resposta = get("/api/hospitais", null);

        assertEquals(401, resposta.statusCode());
        assertTrue(resposta.body().contains("\"status\":401"));
    }

    @Test
    void deveRetornar401ComTokenInvalido() throws Exception {
        HttpResponse<String> resposta = get("/api/hospitais", "Bearer token-invalido");

        assertEquals(401, resposta.statusCode());
    }

    @Test
    void deveManterRotasPublicasAcessiveis() throws Exception {
        HttpResponse<String> resposta = get("/v3/api-docs", null);

        assertEquals(200, resposta.statusCode());
    }

    private HttpResponse<String> get(String caminho, String authorization) throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho)).GET();
        if (authorization != null) {
            requisicao.header("Authorization", authorization);
        }
        return httpClient.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }
}
