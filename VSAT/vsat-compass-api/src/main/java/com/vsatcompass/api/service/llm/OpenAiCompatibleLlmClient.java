package com.vsatcompass.api.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vsatcompass.api.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@Slf4j
@Component
public class OpenAiCompatibleLlmClient implements LlmClient {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);

    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(
            @Value("${LLM_BASE_URL:}") String baseUrl,
            @Value("${LLM_API_KEY:}") String apiKey,
            @Value("${LLM_MODEL:gemini/gemini-2.5-flash-lite}") String model
    ) {
        this.baseUrl = trimSlash(baseUrl);
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String complete(String prompt) {
        if (baseUrl.isBlank() || apiKey.isBlank() || model == null || model.isBlank()) {
            throw AppException.serviceUnavailable("LLM chua duoc cau hinh");
        }

        ChatRequest request = new ChatRequest(model, List.of(
                new ChatMessage("system", "Ban la tro ly giai thich bai thi V-SAT ngan gon, de hieu, khong cham diem."),
                new ChatMessage("user", prompt)
        ));

        String responseBody;
        try {
            responseBody = createRestClient()
                    .post()
                    .uri("/chat/completions")
                    .body(request)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException ex) {
            log.warn("AI provider HTTP error status={} body={}",
                    ex.getStatusCode().value(), ex.getResponseBodyAsString());
            throw AppException.serviceUnavailable("AI provider loi: " + ex.getStatusCode().value());
        } catch (ResourceAccessException ex) {
            log.warn("AI provider connection failed: {}", ex.getMessage());
            throw AppException.serviceUnavailable("Khong ket noi duoc AI provider");
        } catch (RestClientException ex) {
            log.error("AI provider request failed", ex);
            throw AppException.serviceUnavailable("AI provider loi khi xu ly yeu cau");
        }

        String content = extractContent(responseBody);
        if (content.isBlank()) {
            throw AppException.serviceUnavailable("LLM khong tra ve giai thich hop le");
        }
        return content.trim();
    }

    private RestClient createRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();
    }

    private String extractContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        String trimmed = responseBody.trim();
        try {
            if (trimmed.startsWith("data:") || trimmed.contains("\ndata:")) {
                return extractStreamingContent(trimmed);
            }
            return extractJsonContent(objectMapper.readTree(trimmed));
        } catch (IOException ex) {
            log.warn("AI provider returned invalid response body: {}", ex.getMessage());
            throw AppException.serviceUnavailable("AI provider tra ve phan hoi khong hop le");
        }
    }

    private String extractStreamingContent(String responseBody) throws IOException {
        StringBuilder content = new StringBuilder();
        for (String line : responseBody.split("\\R")) {
            String trimmedLine = line.trim();
            if (!trimmedLine.startsWith("data:")) {
                continue;
            }
            String payload = trimmedLine.substring("data:".length()).trim();
            if (payload.isBlank() || "[DONE]".equals(payload)) {
                continue;
            }
            content.append(extractJsonContent(objectMapper.readTree(payload)));
        }
        return content.toString();
    }

    private static String extractJsonContent(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (!choices.isArray() || choices.isEmpty()) {
            return "";
        }
        JsonNode choice = choices.get(0);
        JsonNode deltaContent = choice.path("delta").path("content");
        if (deltaContent.isTextual()) {
            return deltaContent.asText();
        }
        JsonNode messageContent = choice.path("message").path("content");
        return messageContent.isTextual() ? messageContent.asText() : "";
    }

    private static String trimSlash(String value) {
        if (value == null) return "";
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private record ChatRequest(String model, List<ChatMessage> messages) {}

    private record ChatMessage(String role, String content) {}
}
