package br.com.fiap.medistockbackend.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:tratamento-erros-test?mode=memory&cache=shared",
                "medistock.jwt.secret=ZmFrZS10ZXN0LXNlY3JldC1rZXktd2l0aC1hdC1sZWFzdC0zMi1ieXRlcw==",
                "medistock.gemini.api-key="
        })
class TratamentoErrosApiTest {

    private static final Pattern TOKEN = Pattern.compile("\"token\":\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Value("${local.server.port}")
    private int porta;

    private String token;

    @BeforeEach
    void autenticar() throws Exception {
        String email = "teste." + UUID.randomUUID().toString().substring(0, 8) + "@fiap.com.br";
        HttpResponse<String> resposta = enviar("POST", "/api/auth/registrar", "application/json", """
                {"primeiroNome":"Teste","ultimoNome":"Erros","emailInstitucional":"%s","senha":"senha12345"}"""
                .formatted(email), false);
        Matcher matcher = TOKEN.matcher(resposta.body());
        assertTrue(matcher.find(), resposta.body());
        token = matcher.group(1);
    }

    @Test
    void deveRetornar400ParaJsonMalformado() throws Exception {
        assertStatus(400, enviar("POST", "/api/estoque", "application/json", "{nome:", true));
    }

    @Test
    void deveRetornar400ParaCorpoAusente() throws Exception {
        assertStatus(400, enviar("POST", "/api/auth/registrar", "application/json", "", false));
    }

    @Test
    void deveRetornar404ParaRotaInexistente() throws Exception {
        assertStatus(404, enviar("GET", "/api/rota-inexistente", null, null, true));
    }

    @Test
    void deveRetornar405ParaMetodoNaoSuportado() throws Exception {
        assertStatus(405, enviar("DELETE", "/api/alertas", null, null, true));
    }

    @Test
    void deveRetornar415ParaContentTypeNaoSuportado() throws Exception {
        assertStatus(415, enviar("POST", "/api/estoque", "text/plain", "x", true));
    }

    private void assertStatus(int esperado, HttpResponse<String> resposta) {
        assertEquals(esperado, resposta.statusCode(), resposta.body());
        assertTrue(resposta.body().contains("\"status\":" + esperado), resposta.body());
    }

    private HttpResponse<String> enviar(String metodo, String caminho, String contentType, String corpo,
                                        boolean autenticado) throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create("http://localhost:" + porta + caminho))
                .method(metodo, corpo == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(corpo));
        if (contentType != null) {
            requisicao.header("Content-Type", contentType);
        }
        if (autenticado) {
            requisicao.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
    }
}
