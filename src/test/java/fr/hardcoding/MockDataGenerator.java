package fr.hardcoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
        saveMock("create-session-response.json", sessionResponse);

        // Extract access token from session response
        String accessToken = extractAccessToken(sessionResponse);

        // 2. Generate searchPosts response with images
        String searchWithImagesResponse = generateSearchPostsMock(accessToken, "parisjug", true);
        saveMock("search-response-with-images.json", searchWithImagesResponse);

        // 3. Generate searchPosts response without images (if available)
        String searchNoImagesResponse = generateSearchPostsMock(accessToken, "parisjug test", false);
        saveMock("search-response-no-images.json", searchNoImagesResponse);

        // 4. Generate oEmbed response
        String oembedResponse = generateOEmbedMock();
        saveMock("oembed-response.json", oembedResponse);

        System.out.println("\n=== Mock Generation Complete ===");
        System.out.println("Mock files saved to: " + MOCK_DIR);
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
}
