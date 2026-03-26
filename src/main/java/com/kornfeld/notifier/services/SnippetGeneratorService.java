package com.kornfeld.notifier.services;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SnippetGeneratorService {
    public String generateCurlExamples(String openApiJson) {
        if (openApiJson == null || openApiJson.isEmpty()) {
            return "No OpenAPI spec provided to generate API usage examples\n";
        }

        try {
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(openApiJson, null, null).getOpenAPI();

            if (openAPI == null || openAPI.getPaths() == null) {
                return "Could not parse OpenAPI description\n";
            }

            StringBuilder samples = new StringBuilder();

            String baseUrl = (openAPI.getServers() != null && !openAPI.getServers().isEmpty()) ? openAPI.getServers().get(0).getUrl() : "http://api.example.com";

            int count = 0;

            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                if (count >= 5) break; // TODO const

                String path = entry.getKey();
                PathItem pathItem = entry.getValue();

                if (pathItem.getGet() != null) {
                    samples.append("**GET ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X GET \"").append(baseUrl).append(path).append("\"\n```\n\n");
                    count++;
                }

                if (pathItem.getPost() != null) {
                    samples.append("**POST ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X POST \"").append(baseUrl).append(path)
                            .append("\" \\\n  -H \"Content-Type: application/json\" \\\n  -d '{}'\n```\n\n");
                    count++;
                }

                if (pathItem.getPut() != null) {
                    samples.append("**PUT ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X PUT \"").append(baseUrl).append(path)
                            .append("\" \\\n  -H \"Content-Type: application/json\" \\\n  -d '{}'\n```\n\n");
                    count++;
                }
                if (pathItem.getPatch() != null) {
                    samples.append("**PATCH ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X PATCH \"").append(baseUrl).append(path)
                            .append("\" \\\n  -H \"Content-Type: application/json\" \\\n  -d '{}'\n```\n\n");
                    count++;
                }
                if (pathItem.getDelete() != null) {
                    samples.append("**DELETE ").append(path).append("**\n");
                    samples.append("```bash\ncurl -X DELETE \"").append(baseUrl).append(path).append("\"\n```\n\n");
                    count++;
                }

            }
            return samples.toString();
        } catch (Exception e) {
            System.err.println("Error generating API usage examples: " + e.getMessage());
            return "Error occurred while generating API examples.\n";
        }
    }
}

// TODO ggf möglichkeit zum auf/zuklappen der api usage examples, ggf. kopiermöglichkeit und execute button, nur anzeigen von beispielen aus whats new/changed zb
