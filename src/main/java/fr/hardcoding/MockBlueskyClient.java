package fr.hardcoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mock implementation of BlueskyClient that returns pre-recorded responses from mock files.
 * This allows testing and UI development without requiring real Bluesky credentials.
 */
@ApplicationScoped
public class MockBlueskyClient implements BlueskyClient {
    private static final Logger LOGGER = Logger.getLogger(MockBlueskyClient.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @ConfigProperty(name = "bluesky.mock.enabled", defaultValue = "false")
    boolean mockEnabled;

    @Override
    public SessionResponse createSession(SessionRequest request) {
        if (!mockEnabled) {
            throw new UnsupportedOperationException("Mock client should only be called when mock mode is enabled");
        }
        
        LOGGER.info("MockBlueskyClient: Returning mock session response");
        try {
            return loadMockResponse("bluesky-mocks/create-session-response.json", SessionResponse.class);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock session response", e);
            throw new RuntimeException("Failed to load mock session response", e);
        }
    }

    @Override
    public SearchResponse searchPosts(String authorization, String query, Integer limit, String cursor) {
        if (!mockEnabled) {
            throw new UnsupportedOperationException("Mock client should only be called when mock mode is enabled");
        }
        
        LOGGER.info(String.format("MockBlueskyClient: Returning mock search response for query: %s", query));
        try {
            // Return the search response with images by default
            return loadMockResponse("bluesky-mocks/search-response-with-images.json", SearchResponse.class);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock search response", e);
            throw new RuntimeException("Failed to load mock search response", e);
        }
    }

    private <T> T loadMockResponse(String resourcePath, Class<T> responseClass) throws IOException {
        // Try to load from test resources first (for runtime), then from classpath
        InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (is == null) {
            throw new IOException("Mock file not found: " + resourcePath);
        }
        
        try {
            return mapper.readValue(is, responseClass);
        } finally {
            is.close();
        }
    }
}
