package br.com.fiap.medistockbackend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Slf4j
@Service
public class GeminiClient {

    private static final String HEADER_API_KEY = "x-goog-api-key";

    private final String url;
    private final String apiKey;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiClient(@Value("${medistock.gemini.url}") String url,
                        @Value("${medistock.gemini.api-key:}") String apiKey,
                        @Value("${medistock.gemini.timeout:10s}") Duration timeout) {
        this.url = url;
        this.apiKey = apiKey;

        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(timeout);
        this.restClient = RestClient.builder().requestFactory(requestFactory).build();
    }

    public boolean configurado() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String gerarTexto(String prompt) {
        if (!configurado()) {
            return null;
        }

        try {
            String corpoRequisicao = objectMapper.writeValueAsString(
                    new GeminiRequest(new Content[]{new Content(new Part[]{new Part(prompt)})}));

            String resposta = restClient.post()
                    .uri(url)
                    .header(HEADER_API_KEY, apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpoRequisicao)
                    .retrieve()
                    .body(String.class);

            return extrairTexto(resposta);
        } catch (Exception ex) {
            log.warn("Falha ao consultar o Gemini; usando a justificativa padrao: {}", ex.getMessage());
            return null;
        }
    }

    private String extrairTexto(String jsonResposta) throws Exception {
        JsonNode texto = objectMapper.readTree(jsonResposta)
                .path("candidates").path(0)
                .path("content").path("parts").path(0)
                .path("text");
        return texto.isMissingNode() ? null : texto.asText();
    }

    private record GeminiRequest(Content[] contents) {}
    private record Content(Part[] parts) {}
    private record Part(String text) {}
}
