package br.com.fiap.medistockbackend.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class GeminiClientTest {

    private HttpServer servidor;
    private final AtomicReference<String> chaveRecebida = new AtomicReference<>();
    private final AtomicReference<String> queryRecebida = new AtomicReference<>();

    @BeforeEach
    void iniciarServidor() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/ok", troca -> {
            chaveRecebida.set(troca.getRequestHeaders().getFirst("x-goog-api-key"));
            queryRecebida.set(troca.getRequestURI().getQuery());
            byte[] corpo = """
                    {"candidates":[{"content":{"parts":[{"text":"Justificativa gerada"}]}}]}"""
                    .getBytes(StandardCharsets.UTF_8);
            troca.sendResponseHeaders(200, corpo.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(corpo);
            }
        });
        servidor.createContext("/lento", troca -> {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ignorada) {
                Thread.currentThread().interrupt();
            }
            troca.sendResponseHeaders(200, -1);
            troca.close();
        });
        servidor.start();
    }

    @AfterEach
    void pararServidor() {
        servidor.stop(0);
    }

    @Test
    void deveEnviarChaveNoHeaderELerOTexto() {
        GeminiClient cliente = new GeminiClient(url("/ok"), "chave-teste", Duration.ofSeconds(5));

        assertEquals("Justificativa gerada", cliente.gerarTexto("prompt"));
        assertEquals("chave-teste", chaveRecebida.get());
        assertNull(queryRecebida.get());
    }

    @Test
    void deveRetornarNuloQuandoExcedeOTimeout() {
        GeminiClient cliente = new GeminiClient(url("/lento"), "chave-teste", Duration.ofMillis(300));

        assertNull(cliente.gerarTexto("prompt"));
    }

    @Test
    void naoDeveChamarAApiSemChave() {
        GeminiClient cliente = new GeminiClient(url("/ok"), "", Duration.ofSeconds(5));

        assertFalse(cliente.configurado());
        assertNull(cliente.gerarTexto("prompt"));
        assertNull(chaveRecebida.get());
    }

    private String url(String caminho) {
        return "http://localhost:" + servidor.getAddress().getPort() + caminho;
    }
}
