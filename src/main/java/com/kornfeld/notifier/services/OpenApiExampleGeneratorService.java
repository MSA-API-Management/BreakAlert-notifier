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

import java.util.*;

@Service
public class OpenApiExampleGeneratorService {
    private static final int SAMPLE_LIMIT = -1; // usage example limitation. -1 for no limitation
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * Generates curl examples for selected endpoints from an OpenAPI specification.
     *
     * @param openApiJson OpenAPI specification as JSON
     * @param address base address of the API
     * @param targetEndpoints endpoints to include (method + path)
     * @return a map containing the endpoint identifier (e.g., "GET /path") as the key and the generated curl command as the value. Returns an empty map if no endpoints match or an error occurs.
     */
    public Map<String, String> generateCurlExamples(String openApiJson, String address, Set<String> targetEndpoints) {
        if (openApiJson == null || openApiJson.isEmpty() || targetEndpoints == null || targetEndpoints.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            OpenAPI openAPI = new OpenAPIV3Parser().readContents(openApiJson, null, null).getOpenAPI();
            if (openAPI == null || openAPI.getPaths() == null) return Collections.emptyMap();

            String baseUrl = generateBaseUrl(address);

            Map<String, String> examples = new LinkedHashMap<>();
            int count = 0;

            for (Map.Entry<String, PathItem> entry : openAPI.getPaths().entrySet()) {
                if (SAMPLE_LIMIT != -1 && count >= SAMPLE_LIMIT) break;

                String path = entry.getKey();
                if (!path.startsWith("/")) path = "/" + path;
                PathItem pathItem = entry.getValue();

                count = processOperation("GET", path, pathItem.getGet(), baseUrl, examples, count, targetEndpoints, openAPI);
                count = processOperation("POST", path, pathItem.getPost(), baseUrl, examples, count, targetEndpoints, openAPI);
                count = processOperation("PUT", path, pathItem.getPut(), baseUrl, examples, count, targetEndpoints, openAPI);
                count = processOperation("PATCH", path, pathItem.getPatch(), baseUrl, examples, count, targetEndpoints, openAPI);
                count = processOperation("DELETE", path, pathItem.getDelete(), baseUrl, examples, count, targetEndpoints, openAPI);
            }
            return examples;

        } catch (Exception e) {
            System.err.println("Error generating API usage examples: " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * Generates a raw curl example for a matching operation and adds it to the provided map.
     *
     * @param method HTTP method (e.g., "GET", "POST")
     * @param rawPath API endpoint path (e.g., "/users/{id}")
     * @param operation OpenAPI Operation object containing request details
     * @param baseUrl base URL of the API
     * @param examples map to store the generated curl commands
     * @param count current number of generated examples
     * @param targetEndpoints set of target endpoints to filter by
     * @param openAPI parsed OpenAPI specification for resolving references (like request bodies)
     * @return updated sample count
     */
    private int processOperation(String method, String rawPath, Operation operation, String baseUrl,
                                 Map<String, String> examples, int count, Set<String> targetEndpoints, OpenAPI openAPI) {
        if (operation == null) return count;

        String endpointKey = method + " " + rawPath;
        if (!targetEndpoints.contains(endpointKey)) return count;

        String resolvedPath = rawPath.replaceAll("\\{([^}]+)}", "123");

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
        StringBuilder curlCmd = new StringBuilder();

        curlCmd.append("curl -X ").append(method).append(" \"").append(fullUrl).append("\"");

        String acceptHeader = extractResponseAcceptHeader(operation);
        curlCmd.append(" \\\n  -H \"accept: ").append(acceptHeader).append("\"");

        if (operation.getRequestBody() != null && (method.equals("POST") || method.equals("PUT") || method.equals("PATCH"))) {
            String requestBodyExample = extractSampleJson(operation.getRequestBody(), openAPI);
            curlCmd.append(" \\\n  -H \"Content-Type: application/json\"")
                    .append(" \\\n  -d '").append(requestBodyExample).append("'");
        }

        examples.put(endpointKey, curlCmd.toString());

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