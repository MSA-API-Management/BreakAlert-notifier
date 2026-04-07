package com.kornfeld.notifier.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Service
public class OpenApiExampleGeneratorService {
    private static final int SAMPLE_LIMIT = -1; // usage example limitation. -1 for no limitation
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Generates curl examples for selected endpoints from an OpenAPI specification
     * @param openApiJson OpenAPI specification as JSON
     * @param address base address of the API
     * @param targetEndpoints endpoints to include (method + path)
     * @return generated curl examples or an error/message string
     */
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

            String baseUrl = generateBaseUrl(address);

            StringBuilder samples = new StringBuilder();

            int count = 0;
            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                if (SAMPLE_LIMIT != -1 && count >= SAMPLE_LIMIT) break;

                String path = entry.getKey();
                if (!path.startsWith("/")) path = "/" + path;

                PathItem pathItem = entry.getValue();

                count = processOperation("GET", path, pathItem.getGet(), baseUrl, samples, count, targetEndpoints, openAPI);
                count = processOperation("POST", path, pathItem.getPost(), baseUrl, samples, count, targetEndpoints, openAPI);
                count = processOperation("PUT", path, pathItem.getPut(), baseUrl, samples, count, targetEndpoints, openAPI);
                count = processOperation("PATCH", path, pathItem.getPatch(), baseUrl, samples, count, targetEndpoints, openAPI);
                count = processOperation("DELETE", path, pathItem.getDelete(), baseUrl, samples, count, targetEndpoints, openAPI);
            }

            if (samples.isEmpty()) return "No samples found\n";
            return samples.toString();

        } catch (Exception e) {
            System.err.println("Error generating API usage examples: " + e.getMessage());
            return "Error occurred while generating API examples.\n";
        }
    }

    /**
     * Generates a curl example in markdown format for a matching operation
     * @param method
     * @param rawPath
     * @param operation
     * @param baseUrl
     * @param samples
     * @param count
     * @param targetEndpoints
     * @param openAPI
     * @return updated sample count
     */
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

        String resolvedPath = rawPath.replaceAll("\\{([^}]+)}", "123"); // replaces all path variables with sample input

        // Replaces query params with sample input
        StringBuilder query = new StringBuilder();
        if (operation.getParameters() != null) {
            for (Parameter p : operation.getParameters()) {
                if (p.getIn().equals("query")) {
                    query.append(query.isEmpty() ? "?" : "&");
                    query.append(p.getName()).append("=").append("value");
                }
            }
        }

        String fullUrl = baseUrl + resolvedPath + query;

        samples.append("**``").append(method).append("`` ").append(rawPath).append("**\n");
        samples.append("```bash\n");
        samples.append("curl -X ").append(method).append(" \"").append(fullUrl).append("\"");

        String acceptHeader = extractResponseAcceptHeader(operation);
        samples.append(" \\\n  -H \"accept: ").append(acceptHeader).append("\"");

        if (operation.getRequestBody() != null) {
            if (method.equals("POST") || method.equals("PUT") || method.equals("PATCH")) {
                String requestBodyExample = extractSampleJson(operation.getRequestBody(), openAPI);

                samples.append(" \\\n  -H \"Content-Type: application/json\"")
                        .append(" \\\n  -d '")
                        .append(requestBodyExample)
                        .append("'");
            }
        }

        samples.append("\n```\n\n");

        return count + 1;
    }

    /**
     * Creates the base url. The base url is a combination of the provided address and URL protocol
     * @param address Provider subscription address
     * @return normalized url or fallback sample url
     */
    private String generateBaseUrl(String address) {
        String baseUrl = address;
        if (baseUrl != null && !baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
            baseUrl = "https://" + baseUrl;
        } else if (baseUrl == null) {
            baseUrl = "http://api.example.com";
        }

        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }

    /**
     * Extracts response Accept header values from successful (2xx) responses.
     * @param operation OpenAPI operation
     * @return a comma-separated list of media types or a fallback if none found
     */
    private String extractResponseAcceptHeader(Operation operation) {
        String acceptHeader = "*/*";
        if (operation.getResponses() != null) {
            for (Map.Entry<String, ApiResponse> response : operation.getResponses().entrySet()) {
                if (response.getKey().startsWith("2") && response.getValue().getContent() != null) {
                    Set<String> mediaTypes = response.getValue().getContent().keySet();
                    if (!mediaTypes.isEmpty()) {
                        acceptHeader = String.join(",", mediaTypes);
                        break;
                    }
                }
            }
        }
        return acceptHeader;
    }

    /**
     * Generates a sample JSON string from the given request body schema
     * @param requestBody Request body definition
     * @param openAPI OpenAPI specification
     * @return JSON sample string or "{}" if unavailable or error
     */
    private String extractSampleJson(RequestBody requestBody,
                                     OpenAPI openAPI) {
        if (requestBody.getContent() == null) return "{}";

        MediaType mediaType = requestBody.getContent().get("application/json");
        if (mediaType == null) return "{}";

        Object exampleObj = null;
        if (mediaType.getSchema() != null) exampleObj = buildMockData(mediaType.getSchema(), openAPI);
        if (exampleObj == null) return "{}";

        try {
            return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(exampleObj);
        } catch (Exception e) {
            System.out.println("NOTIFICATION - Error occurred while creating API usage example: " + e.getMessage());
            return "{}";
        }
    }

    /**
     * Recursively builds mock data for a given schema, resolving references,
     * objects, arrays, and primitive types.
     * @param schema Schema to process
     * @param openAPI OpenAPI specification
     * @return a mock object matching the schema or null if schema is null
     */
    private Object buildMockData(Schema<?> schema,
                                 OpenAPI openAPI) {
        if (schema == null) return null;

        String schemaRef = schema.get$ref();
        if (schemaRef != null) {
            String refName = schemaRef.substring(schemaRef.lastIndexOf('/') + 1);

            if (openAPI.getComponents() != null && openAPI.getComponents().getSchemas() != null) {
                Schema<?> refSchema = openAPI.getComponents().getSchemas().get(refName);
                return buildMockData(refSchema, openAPI);
            }
        }

        if (schema.getProperties() != null) {
            Map<String, Object> map = new LinkedHashMap<>();
            for (Map.Entry<String, Schema> entry : schema.getProperties().entrySet()) {
                map.put(entry.getKey(), buildMockData(entry.getValue(), openAPI));
            }
            return map;
        }

        if (schema.getType().equals("array") && schema.getItems() != null) {
            return Collections.singletonList(buildMockData(schema.getItems(), openAPI));
        }

        String type = schema.getType();
        if (type.equals("boolean")) return true;
        if (type.equals("integer") || type.equals("number")) return 1;
        if (type.equals("string") && schema.getMaxLength() != null && schema.getMaxLength() == 1) return "c";

        return "string";
    }

}