package fr.hardcoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mock implementation of OEmbedClient that returns pre-recorded responses from mock files.
 * This allows testing and UI development without requiring real API calls.
 */
@ApplicationScoped
public class MockOEmbedClient implements OEmbedClient {
    private static final Logger LOGGER = Logger.getLogger(MockOEmbedClient.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @ConfigProperty(name = "bluesky.mock.enabled", defaultValue = "false")
    boolean mockEnabled;
    
    @ConfigProperty(name = "bluesky.mock.oembed.file", defaultValue = "src/test/resources/bluesky-mocks/oembed-response.json")
    String oembedMockFile;

    @Override
    public OEmbedResponse getOEmbed(String url) {
        if (!mockEnabled) {
            throw new UnsupportedOperationException("Mock client should only be called when mock mode is enabled");
        }
        
        LOGGER.info("MockOEmbedClient: Returning mock oEmbed response for URL: " + url + " from: " + oembedMockFile);
        try {
            return loadMockFromFile(oembedMockFile, OEmbedResponse.class);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load mock oEmbed response from: " + oembedMockFile, e);
            throw new RuntimeException("Failed to load mock oEmbed response", e);
        }
    }

    private <T> T loadMockFromFile(String filePath, Class<T> responseClass) throws IOException {
        File file = new File(filePath);
        if (!file.exists()) {
            throw new IOException("Mock file not found: " + filePath);
        }
        
        try (InputStream is = new FileInputStream(file)) {
            return mapper.readValue(is, responseClass);
        }
    }
}
