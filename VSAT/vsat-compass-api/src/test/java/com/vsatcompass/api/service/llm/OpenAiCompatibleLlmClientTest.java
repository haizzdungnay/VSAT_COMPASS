package com.vsatcompass.api.service.llm;

import com.sun.net.httpserver.HttpServer;
import com.vsatcompass.api.exception.AppException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("OpenAI-compatible LLM client")
class OpenAiCompatibleLlmClientTest {

    private HttpServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("complete parses text/event-stream chunks from router")
    void complete_streamingResponse_success() {
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = ("data: {\"choices\":[{\"delta\":{\"role\":\"assistant\"}}]}\n\n"
                    + "data: {\"choices\":[{\"delta\":{\"content\":\"pong\"}}]}\n\n"
                    + "data: [DONE]\n").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/event-stream");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(baseUrl(), "test-key", "test-model");

        assertThat(client.complete("ping")).isEqualTo("pong");
    }

    @Test
    @DisplayName("complete maps provider non-2xx responses to 503")
    void complete_providerError_serviceUnavailable() {
        server.createContext("/v1/chat/completions", exchange -> {
            byte[] body = "{\"error\":{\"message\":\"model not found\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(404, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });

        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(baseUrl(), "test-key", "bad-model");

        assertThatThrownBy(() -> client.complete("ping"))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appException = (AppException) ex;
                    assertThat(appException.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                    assertThat(appException.getMessage()).isEqualTo("AI provider loi: 404");
                });
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/v1";
    }
}
