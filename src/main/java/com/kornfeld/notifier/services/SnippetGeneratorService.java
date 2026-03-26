package com.kornfeld.notifier.services;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class SnippetGeneratorService {
    private static final int SAMPLE_LIMIT = 5;

    public String generateCurlExamples(String openApiJson,
                                       String address,
                                       Set<String> targetEndpoints) {
        if (openApiJson == null || openApiJson.isEmpty()) {
            return "No OpenAPI spec provided to generate API usage examples\n";
        }

        if (targetEndpoints == null || targetEndpoints.isEmpty()) {
            return "No new or changed endpoints detected.\n";
        }

        try {
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(openApiJson, null, null).getOpenAPI();

            if (openAPI == null || openAPI.getPaths() == null) {
                return "Could not parse OpenAPI description\n";
            }

            String baseUrl = address;
            if (baseUrl != null && !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
                baseUrl = "https://" + baseUrl;
            } else if (baseUrl == null) {
                baseUrl = "http://api.example.com";
            }

            if (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }

            StringBuilder samples = new StringBuilder();

            int count = 0;

            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                if (count >= SAMPLE_LIMIT) break;

                String path = entry.getKey();
                if (!path.startsWith("/")) path = "/" + path;

                String fullUrl = baseUrl + path;
                PathItem pathItem = entry.getValue();

                if (pathItem.getGet() != null && targetEndpoints.contains("GET " + path)) {
                    samples.append("**``GET`` ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X GET \"").append(fullUrl).append("\"\n```\n\n");
                    count++;
                }

                if (pathItem.getPost() != null && targetEndpoints.contains("POST " + path)) {
                    samples.append("**``POST`` ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X POST \"").append(fullUrl)
                            .append("\" \\\n  -H \"Content-Type: application/json\" \\\n  -d '{}'\n```\n\n");
                    count++;
                }

                if (pathItem.getPut() != null && targetEndpoints.contains("PUT " + path)) {
                    samples.append("**``PUT`` ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X PUT \"").append(fullUrl)
                            .append("\" \\\n  -H \"Content-Type: application/json\" \\\n  -d '{}'\n```\n\n");
                    count++;
                }
                if (pathItem.getPatch() != null && targetEndpoints.contains("PATCH " + path)) {
                    samples.append("**``PATCH`` ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X PATCH \"").append(fullUrl)
                            .append("\" \\\n  -H \"Content-Type: application/json\" \\\n  -d '{}'\n```\n\n");
                    count++;
                }
                if (pathItem.getDelete() != null && targetEndpoints.contains("DELETE " + path)) {
                    samples.append("**``DELETE`` ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X DELETE \"").append(fullUrl).append("\"\n```\n\n");
                    count++;
                }

            }

            if (samples.isEmpty()) return "No samples found\n";
            return samples.toString();

        } catch (Exception e) {
            System.err.println("Error generating API usage examples: " + e.getMessage());
            return "Error occurred while generating API examples.\n";
        }
    }
}

// TODO ggf möglichkeit zum auf/zuklappen der api usage examples, ggf. kopiermöglichkeit und execute button, nur anzeigen von beispielen aus whats new/changed zb
