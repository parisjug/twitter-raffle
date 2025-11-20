package fr.hardcoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Mock Data Generator for Bluesky API responses.
 * 
 * This test is @Disabled by default. To generate mock data:
 * 1. Set environment variables: bluesky.identifier and bluesky.password
 * 2. Remove @Disabled annotation
 * 3. Run: mvn test -Dtest=MockDataGenerator
 * 4. Mock files will be saved to src/test/resources/bluesky-mocks/
 */
@Disabled("Manual test - only run when you want to regenerate mock data")
public class MockDataGenerator {

    private static final String MOCK_DIR = "src/test/resources/bluesky-mocks/";
    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    public void generateAllMocks() throws Exception {
        System.out.println("=== Generating Bluesky API Mock Data ===");
        
        String identifier = System.getProperty("bluesky.identifier");
        String password = System.getProperty("bluesky.password");
        
        if (identifier == null || password == null) {
            throw new IllegalStateException(
                "Please set bluesky.identifier and bluesky.password system properties:\n" +
                "mvn test -Dtest=MockDataGenerator -Dbluesky.identifier=<your-handle> -Dbluesky.password=<your-app-password>"
            );
        }

        // Create mock directory if it doesn't exist
        Files.createDirectories(Paths.get(MOCK_DIR));

        // 1. Generate createSession response
        String sessionResponse = generateCreateSessionMock(identifier, password);
        String sanitizedSession = sanitizeSessionResponse(sessionResponse);
        saveMock("create-session-response.json", sanitizedSession);

        // Extract access token from session response
        String accessToken = extractAccessToken(sessionResponse);

        // 2. Generate searchPosts response with images
        String searchWithImagesResponse = generateSearchPostsMock(accessToken, "parisjug", true);
        String sanitizedSearchWithImages = sanitizeSearchResponse(searchWithImagesResponse);
        saveMock("search-response-with-images.json", sanitizedSearchWithImages);

        // 3. Generate searchPosts response without images (if available)
        String searchNoImagesResponse = generateSearchPostsMock(accessToken, "parisjug test", false);
        String sanitizedSearchNoImages = sanitizeSearchResponse(searchNoImagesResponse);
        saveMock("search-response-no-images.json", sanitizedSearchNoImages);

        // 4. Generate oEmbed response
        String oembedResponse = generateOEmbedMock();
        String sanitizedOembed = sanitizeOEmbedResponse(oembedResponse);
        saveMock("oembed-response.json", sanitizedOembed);

        System.out.println("\n=== Mock Generation Complete ===");
        System.out.println("Mock files saved to: " + MOCK_DIR);
        System.out.println("Sensitive data has been sanitized from mock files.");
        System.out.println("You can now run the tests with: mvn test");
    }

    private String generateCreateSessionMock(String identifier, String password) throws IOException, InterruptedException {
        System.out.println("\nGenerating createSession mock...");
        
        HttpClient client = HttpClient.newHttpClient();
        String requestBody = String.format("{\"identifier\":\"%s\",\"password\":\"%s\"}", identifier, password);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://bsky.social/xrpc/com.atproto.server.createSession"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to create session: " + response.statusCode() + " - " + response.body());
        }
        
        System.out.println("✓ createSession mock generated");
        return response.body();
    }

    private String generateSearchPostsMock(String accessToken, String query, boolean expectImages) 
            throws IOException, InterruptedException {
        System.out.println("\nGenerating searchPosts mock (query: " + query + ")...");
        
        HttpClient client = HttpClient.newHttpClient();
        String url = "https://bsky.social/xrpc/app.bsky.feed.searchPosts?q=" + 
                     java.net.URLEncoder.encode(query, "UTF-8") + "&limit=25";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to search posts: " + response.statusCode() + " - " + response.body());
        }
        
        System.out.println("✓ searchPosts mock generated");
        return response.body();
    }

    private String generateOEmbedMock() throws IOException, InterruptedException {
        System.out.println("\nGenerating oEmbed mock...");
        
        HttpClient client = HttpClient.newHttpClient();
        // Use a well-known Bluesky post for consistent oEmbed response
        String testPostUrl = "https://bsky.app/profile/sunix.org/post/3m5ypww5hvs2j";
        String url = "https://embed.bsky.app/oembed?url=" + 
                     java.net.URLEncoder.encode(testPostUrl, "UTF-8");
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 200) {
            System.out.println("Warning: Failed to fetch oEmbed (status " + response.statusCode() + 
                             "), creating placeholder mock");
            // Create a placeholder oEmbed response
            return "{\"type\":\"rich\",\"version\":\"1.0\",\"author_name\":\"Test User\"," +
                   "\"author_url\":\"https://bsky.app/profile/test.bsky.social\"," +
                   "\"html\":\"<blockquote>Test post content</blockquote>\"}";
        }
        
        System.out.println("✓ oEmbed mock generated");
        return response.body();
    }

    private String extractAccessToken(String sessionResponse) throws IOException {
        // Simple JSON parsing to extract accessJwt
        return mapper.readTree(sessionResponse).get("accessJwt").asText();
    }

    private void saveMock(String filename, String content) throws IOException {
        Path path = Paths.get(MOCK_DIR + filename);
        // Pretty-print JSON before saving
        try {
            Object json = mapper.readValue(content, Object.class);
            String prettyJson = mapper.writeValueAsString(json);
            Files.writeString(path, prettyJson);
            System.out.println("  Saved: " + filename);
        } catch (Exception e) {
            // If not valid JSON, save as-is
            Files.writeString(path, content);
            System.out.println("  Saved: " + filename + " (raw)");
        }
    }

    /**
     * Sanitize sensitive data from createSession response.
     * Replaces sensitive fields with placeholder values safe for committing to git.
     */
    private String sanitizeSessionResponse(String sessionResponse) throws IOException {
        System.out.println("  Sanitizing session response...");
        ObjectNode root = (ObjectNode) mapper.readTree(sessionResponse);
        
        // Replace sensitive authentication tokens
        if (root.has("accessJwt")) {
            root.put("accessJwt", "sanitized-access-token-for-testing");
        }
        if (root.has("refreshJwt")) {
            root.put("refreshJwt", "sanitized-refresh-token-for-testing");
        }
        
        // Replace user email
        if (root.has("email")) {
            root.put("email", "test@example.com");
        }
        
        // Replace user handle (keep it realistic but generic)
        if (root.has("handle")) {
            root.put("handle", "testuser.bsky.social");
        }
        
        // Keep did as it's needed for testing but not sensitive
        // Keep didDoc as it's public information
        
        System.out.println("  ✓ Sensitive data sanitized");
        return mapper.writeValueAsString(root);
    }

    /**
     * Sanitize sensitive data from searchPosts response.
     * Removes personal information while keeping structure for testing.
     */
    private String sanitizeSearchResponse(String searchResponse) throws IOException {
        System.out.println("  Sanitizing search response...");
        ObjectNode root = (ObjectNode) mapper.readTree(searchResponse);
        
        // Sanitize each post in the response
        if (root.has("posts")) {
            ArrayNode posts = (ArrayNode) root.get("posts");
            for (int i = 0; i < posts.size(); i++) {
                JsonNode post = posts.get(i);
                if (post.isObject()) {
                    sanitizePost((ObjectNode) post);
                }
            }
        }
        
        System.out.println("  ✓ Search response sanitized");
        return mapper.writeValueAsString(root);
    }

    /**
     * Sanitize individual post data.
     */
    private void sanitizePost(ObjectNode post) {
        // Sanitize author information
        if (post.has("author")) {
            ObjectNode author = (ObjectNode) post.get("author");
            if (author.has("displayName")) {
                author.put("displayName", "Test User " + Math.abs(author.hashCode()) % 100);
            }
            if (author.has("handle")) {
                String originalHandle = author.get("handle").asText();
                // Keep domain but sanitize username
                author.put("handle", "testuser" + Math.abs(originalHandle.hashCode()) % 1000 + ".bsky.social");
            }
            if (author.has("avatar")) {
                // Keep avatar structure but use placeholder
                author.put("avatar", "https://cdn.bsky.app/img/avatar/plain/test/placeholder");
            }
            // Keep did as it's needed for blob URL construction
        }
        
        // Sanitize post text - keep structure but remove personal mentions
        if (post.has("record")) {
            ObjectNode record = (ObjectNode) post.get("record");
            if (record.has("text")) {
                String text = record.get("text").asText();
                // Remove @mentions but keep hashtags for testing
                String sanitized = text.replaceAll("@[a-zA-Z0-9._-]+", "@testuser");
                record.put("text", sanitized);
            }
        }
    }

    /**
     * Sanitize oEmbed response.
     * Removes personal information from embedded HTML.
     */
    private String sanitizeOEmbedResponse(String oembedResponse) throws IOException {
        System.out.println("  Sanitizing oEmbed response...");
        ObjectNode root = (ObjectNode) mapper.readTree(oembedResponse);
        
        // Sanitize author information
        if (root.has("author_name")) {
            root.put("author_name", "Test User");
        }
        if (root.has("author_url")) {
            root.put("author_url", "https://bsky.app/profile/testuser.bsky.social");
        }
        
        // Sanitize embedded HTML
        if (root.has("html")) {
            String html = root.get("html").asText();
            // Remove personal identifiers from HTML while keeping structure
            String sanitized = html
                .replaceAll("@[a-zA-Z0-9._-]+", "@testuser")
                .replaceAll("(https://bsky\\.app/profile/)[a-zA-Z0-9._-]+", "$1testuser.bsky.social");
            root.put("html", sanitized);
        }
        
        System.out.println("  ✓ oEmbed response sanitized");
        return mapper.writeValueAsString(root);
    }
}
