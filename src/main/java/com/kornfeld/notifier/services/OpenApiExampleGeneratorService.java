package com.kornfeld.notifier.services;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;

@Service
public class OpenApiExampleGeneratorService {
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

                count = processOperation("GET", path, pathItem.getGet(), baseUrl, samples, count, targetEndpoints, openAPI);
                count = processOperation("POST", path, pathItem.getPost(), baseUrl, samples, count, targetEndpoints, openAPI);
                count = processOperation("PUT", path, pathItem.getPut(), baseUrl, samples, count, targetEndpoints, openAPI);
                count = processOperation("PATCH", path, pathItem.getPatch(), baseUrl, samples, count, targetEndpoints, openAPI);
                count = processOperation("DELETE", path, pathItem.getDelete(), baseUrl, samples, count, targetEndpoints, openAPI);

                /*if (pathItem.getGet() != null && targetEndpoints.contains("GET " + path)) {
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
                }*/

            }

            if (samples.isEmpty()) return "No samples found\n";
            return samples.toString();

        } catch (Exception e) {
            System.err.println("Error generating API usage examples: " + e.getMessage());
            return "Error occurred while generating API examples.\n";
        }
    }

    // TODO requestbody & proper accept
    private int processOperation(
            String method,
            String rawPath,
            Operation operation,
            String baseUrl,
            StringBuilder samples,
            int count,
            Set<String> targetEndpoints,
            OpenAPI openAPI
    ) {
        if (operation == null || count >= SAMPLE_LIMIT) return count;

        String endpointKey = method + " " + rawPath;
        if (!targetEndpoints.contains(endpointKey)) return count;

        String resolvedPath = rawPath.replaceAll("\\{([^}]+)}", "123");

        StringBuilder query = new StringBuilder();
        if (operation.getParameters() != null) {
            for (Parameter p : operation.getParameters()) {
                if ("query".equals(p.getIn())) {
                    query.append(query.isEmpty() ? "?" : "&");
                    query.append(p.getName()).append("=").append("value");
                }
            }
        }

        String fullUrl = baseUrl + resolvedPath + query;

        samples.append("**``").append(method).append("`` ").append(rawPath).append("**\n");
        samples.append("```bash\n");
        samples.append("curl -X ").append(method).append(" \"").append(fullUrl).append("\"");
        samples.append(" \\\n  -H \"accept: application/json\"");

        if (operation.getRequestBody() != null &&
                ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {

            String exampleJson = "{ }";

            samples.append(" \\\n  -H \"Content-Type: application/json\"");
            samples.append(" \\\n  -d '").append(exampleJson).append("'");
        }

        samples.append("\n```\n\n");

        return count + 1;
    }

}

// TODO ggf möglichkeit zum auf/zuklappen der api usage examples, ggf. kopiermöglichkeit und execute button
