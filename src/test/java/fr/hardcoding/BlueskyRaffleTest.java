package fr.hardcoding;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

/**
 * Integration tests for Bluesky Raffle using WireMock to mock external APIs.
 * 
 * Prerequisites:
 * 1. Generate mock data files by running MockDataGenerator (see README.md)
 * 2. Mock files should exist in src/test/resources/bluesky-mocks/
 * 
 * These tests use the actual generated mock data to validate functionality.
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlueskyRaffleTest {

    private static WireMockServer wireMockServer;
    private static String createSessionMock;
    private static String searchWithImagesMock;
    private static String searchNoImagesMock;
    private static String oembedMock;
    private static boolean mocksAvailable = false;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeAll
    public static void setupWireMock() throws IOException {
        // Start WireMock server
        wireMockServer = new WireMockServer(options()
                .port(8089)
                .bindAddress("localhost"));
        wireMockServer.start();
        WireMock.configureFor("localhost", 8089);

        // Load mock data files
        createSessionMock = loadMockFile("create-session-response.json");
        searchWithImagesMock = loadMockFile("search-response-with-images.json");
        searchNoImagesMock = loadMockFile("search-response-no-images.json");
        oembedMock = loadMockFile("oembed-response.json");

        mocksAvailable = (createSessionMock != null && searchWithImagesMock != null && 
                         searchNoImagesMock != null && oembedMock != null);

        System.out.println("WireMock server started on port 8089");
        System.out.println("Mock files available: " + mocksAvailable);
    }

    @AfterAll
    public static void teardownWireMock() {
        if (wireMockServer != null) {
            wireMockServer.stop();
            System.out.println("WireMock server stopped");
        }
    }

    @BeforeEach
    public void setupMocks() {
        if (!mocksAvailable) {
            return; // Skip mock setup if files aren't available
        }

        // Reset WireMock before each test
        wireMockServer.resetAll();

        // Setup createSession endpoint mock
        stubFor(post(urlEqualTo("/xrpc/com.atproto.server.createSession"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(createSessionMock)));

        // Setup searchPosts endpoint mock (with images)
        stubFor(get(urlMatching("/xrpc/app.bsky.feed.searchPosts\\?.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(searchWithImagesMock)));

        // Setup oEmbed endpoint mock
        stubFor(get(urlMatching("/oembed\\?.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(oembedMock)));
    }

    // ========== Mock File Structure Tests ==========

    @Test
    @Order(1)
    public void testMockFilesExist() {
        System.out.println("\n=== Testing Mock Files Availability ===");
        
        if (createSessionMock == null) {
            System.out.println("⚠️  create-session-response.json not found");
        } else {
            System.out.println("✓ create-session-response.json found");
            Assertions.assertTrue(createSessionMock.contains("accessJwt"), 
                "createSession mock should contain accessJwt");
        }
        
        if (searchWithImagesMock == null) {
            System.out.println("⚠️  search-response-with-images.json not found");
        } else {
            System.out.println("✓ search-response-with-images.json found");
            Assertions.assertTrue(searchWithImagesMock.contains("posts"), 
                "searchPosts mock should contain posts array");
        }
        
        if (searchNoImagesMock == null) {
            System.out.println("⚠️  search-response-no-images.json not found");
        } else {
            System.out.println("✓ search-response-no-images.json found");
        }
        
        if (oembedMock == null) {
            System.out.println("⚠️  oembed-response.json not found");
        } else {
            System.out.println("✓ oembed-response.json found");
        }
        
        if (!mocksAvailable) {
            System.out.println("\nℹ️  Generate mocks using: mvn test -Dtest=MockDataGenerator -Dbluesky.identifier=<your-handle> -Dbluesky.password=<your-app-password>");
        }
    }

    @Test
    @Order(2)
    public void testCreateSessionMockStructure() throws Exception {
        if (createSessionMock == null) {
            System.out.println("⚠️  Skipping test - create-session mock not available");
            return;
        }

        System.out.println("\n=== Testing Create Session Mock Structure ===");
        JsonNode session = objectMapper.readTree(createSessionMock);
        
        // Verify required fields exist
        Assertions.assertNotNull(session.get("did"), "Session should contain did");
        Assertions.assertNotNull(session.get("handle"), "Session should contain handle");
        Assertions.assertNotNull(session.get("email"), "Session should contain email");
        Assertions.assertNotNull(session.get("accessJwt"), "Session should contain accessJwt");
        Assertions.assertNotNull(session.get("refreshJwt"), "Session should contain refreshJwt");
        
        // Verify data is sanitized
        String accessJwt = session.get("accessJwt").asText();
        Assertions.assertTrue(accessJwt.contains("sanitized"), 
            "AccessJwt should be sanitized");
        
        String email = session.get("email").asText();
        Assertions.assertEquals("test@example.com", email, 
            "Email should be sanitized to test@example.com");
        
        System.out.println("✓ Create session mock structure valid");
        System.out.println("✓ Sensitive data properly sanitized");
    }

    @Test
    @Order(3)
    public void testSearchWithImagesMockStructure() throws Exception {
        if (searchWithImagesMock == null) {
            System.out.println("⚠️  Skipping test - search-with-images mock not available");
            return;
        }

        System.out.println("\n=== Testing Search With Images Mock Structure ===");
        JsonNode searchResponse = objectMapper.readTree(searchWithImagesMock);
        
        // Verify posts array exists and has content
        JsonNode posts = searchResponse.get("posts");
        Assertions.assertNotNull(posts, "Response should contain posts array");
        Assertions.assertTrue(posts.isArray(), "Posts should be an array");
        Assertions.assertTrue(posts.size() > 0, "Posts array should not be empty");
        
        // Verify first post structure
        JsonNode firstPost = posts.get(0);
        Assertions.assertNotNull(firstPost.get("uri"), "Post should have uri");
        Assertions.assertNotNull(firstPost.get("cid"), "Post should have cid");
        Assertions.assertNotNull(firstPost.get("author"), "Post should have author");
        Assertions.assertNotNull(firstPost.get("record"), "Post should have record");
        
        // Verify author structure
        JsonNode author = firstPost.get("author");
        Assertions.assertNotNull(author.get("did"), "Author should have did");
        Assertions.assertNotNull(author.get("handle"), "Author should have handle");
        Assertions.assertNotNull(author.get("displayName"), "Author should have displayName");
        
        // Verify author data is sanitized
        String displayName = author.get("displayName").asText();
        Assertions.assertTrue(displayName.startsWith("Test User"), 
            "Display name should be sanitized");
        
        // Verify record structure with images
        JsonNode record = firstPost.get("record");
        Assertions.assertNotNull(record.get("text"), "Record should have text");
        Assertions.assertNotNull(record.get("embed"), "Record should have embed");
        
        JsonNode embed = record.get("embed");
        JsonNode images = embed.get("images");
        Assertions.assertNotNull(images, "Embed should have images");
        Assertions.assertTrue(images.isArray(), "Images should be an array");
        Assertions.assertTrue(images.size() > 0, "Images array should not be empty");
        
        // Verify image structure with blob reference
        JsonNode firstImage = images.get(0);
        JsonNode imageBlob = firstImage.get("image");
        Assertions.assertNotNull(imageBlob, "Image should have blob reference");
        Assertions.assertNotNull(imageBlob.get("ref"), "Image blob should have ref");
        
        JsonNode blobRef = imageBlob.get("ref");
        Assertions.assertNotNull(blobRef.get("$link"), "Blob ref should have $link (CID)");
        
        System.out.println("✓ Search response structure valid");
        System.out.println("✓ Found " + posts.size() + " posts with images");
        System.out.println("✓ Image blob references properly structured");
    }

    @Test
    @Order(4)
    public void testSearchNoImagesMockStructure() throws Exception {
        if (searchNoImagesMock == null) {
            System.out.println("⚠️  Skipping test - search-no-images mock not available");
            return;
        }

        System.out.println("\n=== Testing Search No Images Mock Structure ===");
        JsonNode searchResponse = objectMapper.readTree(searchNoImagesMock);
        
        // Verify posts array exists
        JsonNode posts = searchResponse.get("posts");
        Assertions.assertNotNull(posts, "Response should contain posts array");
        Assertions.assertTrue(posts.isArray(), "Posts should be an array");
        
        // If there are posts, verify they don't have images or have empty image arrays
        if (posts.size() > 0) {
            boolean hasPostWithoutImages = false;
            for (JsonNode post : posts) {
                JsonNode record = post.get("record");
                if (record != null) {
                    JsonNode embed = record.get("embed");
                    if (embed == null || embed.get("images") == null || 
                        embed.get("images").size() == 0) {
                        hasPostWithoutImages = true;
                        break;
                    }
                }
            }
            Assertions.assertTrue(hasPostWithoutImages, 
                "Search no-images mock should contain posts without images");
        }
        
        System.out.println("✓ Search no-images mock structure valid");
    }

    @Test
    @Order(5)
    public void testOEmbedMockStructure() throws Exception {
        if (oembedMock == null) {
            System.out.println("⚠️  Skipping test - oembed mock not available");
            return;
        }

        System.out.println("\n=== Testing oEmbed Mock Structure ===");
        JsonNode oembed = objectMapper.readTree(oembedMock);
        
        // Verify oEmbed standard fields
        Assertions.assertNotNull(oembed.get("type"), "oEmbed should have type");
        Assertions.assertNotNull(oembed.get("version"), "oEmbed should have version");
        Assertions.assertNotNull(oembed.get("author_name"), "oEmbed should have author_name");
        Assertions.assertNotNull(oembed.get("author_url"), "oEmbed should have author_url");
        Assertions.assertNotNull(oembed.get("html"), "oEmbed should have html");
        
        String type = oembed.get("type").asText();
        Assertions.assertEquals("rich", type, "oEmbed type should be 'rich'");
        
        String version = oembed.get("version").asText();
        Assertions.assertEquals("1.0", version, "oEmbed version should be '1.0'");
        
        // Verify HTML contains sanitized data
        String html = oembed.get("html").asText();
        Assertions.assertTrue(html.contains("testuser") || html.contains("Test User"), 
            "HTML should contain sanitized user reference");
        Assertions.assertTrue(html.contains("#parisjug"), 
            "HTML should preserve hashtags");
        
        System.out.println("✓ oEmbed structure valid");
        System.out.println("✓ Author data properly sanitized");
    }

    // ========== Integration Tests ==========

    @Test
    @Order(6)
    public void testEmbedEndpointWithValidUrl() {
        System.out.println("\n=== Testing /embed Endpoint ===");
        
        String testUrl = "https://bsky.app/profile/testuser.bsky.social/post/3m5ypww5hvs2j";
        
        given()
                .queryParam("url", testUrl)
                .when()
                .get("/embed")
                .then()
                .statusCode(200)
                .body("url", org.hamcrest.Matchers.equalTo(testUrl))
                .body("author_name", org.hamcrest.Matchers.equalTo("testuser.bsky.social"))
                .body("author_url", org.hamcrest.Matchers.equalTo("https://bsky.app/profile/testuser.bsky.social"))
                .body("html", org.hamcrest.Matchers.containsString("bluesky-embed"));
        
        System.out.println("✓ Embed endpoint working correctly");
    }

    @Test
    @Order(7)
    public void testEmbedEndpointWithInvalidUrl() {
        System.out.println("\n=== Testing /embed Endpoint with Invalid URL ===");
        
        String invalidUrl = "https://invalid-url";
        
        given()
                .queryParam("url", invalidUrl)
                .when()
                .get("/embed")
                .then()
                .statusCode(200)
                .body("url", org.hamcrest.Matchers.equalTo(invalidUrl));
        
        System.out.println("✓ Embed endpoint handles invalid URLs gracefully");
    }

    @Test
    @Order(8)
    public void testPostFilteringLogic() throws Exception {
        if (searchWithImagesMock == null) {
            System.out.println("⚠️  Skipping test - search mock not available");
            return;
        }

        System.out.println("\n=== Testing Post Filtering Logic ===");
        JsonNode searchResponse = objectMapper.readTree(searchWithImagesMock);
        JsonNode posts = searchResponse.get("posts");
        
        int postsWithImages = 0;
        int postsWithText = 0;
        int postsWithHashtag = 0;
        
        for (JsonNode post : posts) {
            JsonNode record = post.get("record");
            if (record != null) {
                // Check for images
                JsonNode embed = record.get("embed");
                if (embed != null && embed.get("images") != null && 
                    embed.get("images").size() > 0) {
                    postsWithImages++;
                }
                
                // Check for text content
                JsonNode text = record.get("text");
                if (text != null && text.asText().length() > 10) {
                    postsWithText++;
                }
                
                // Check for parisjug hashtag
                if (text != null && text.asText().toLowerCase().contains("parisjug")) {
                    postsWithHashtag++;
                }
            }
        }
        
        System.out.println("Posts with images: " + postsWithImages);
        System.out.println("Posts with substantial text: " + postsWithText);
        System.out.println("Posts with #parisjug: " + postsWithHashtag);
        
        Assertions.assertTrue(postsWithImages > 0, 
            "Mock should contain posts with images");
        Assertions.assertTrue(postsWithHashtag > 0, 
            "Mock should contain posts mentioning parisjug");
        
        System.out.println("✓ Post filtering criteria validated");
    }

    @Test
    @Order(9)
    public void testBlobReferenceExtraction() throws Exception {
        if (searchWithImagesMock == null) {
            System.out.println("⚠️  Skipping test - search mock not available");
            return;
        }

        System.out.println("\n=== Testing Blob Reference Extraction ===");
        JsonNode searchResponse = objectMapper.readTree(searchWithImagesMock);
        JsonNode posts = searchResponse.get("posts");
        
        boolean foundValidBlobRef = false;
        String sampleCid = null;
        
        for (JsonNode post : posts) {
            JsonNode record = post.get("record");
            if (record != null && record.get("embed") != null) {
                JsonNode images = record.get("embed").get("images");
                if (images != null && images.size() > 0) {
                    JsonNode firstImage = images.get(0);
                    JsonNode imageBlob = firstImage.get("image");
                    if (imageBlob != null && imageBlob.get("ref") != null) {
                        JsonNode blobRef = imageBlob.get("ref");
                        JsonNode link = blobRef.get("$link");
                        if (link != null && !link.asText().isEmpty()) {
                            foundValidBlobRef = true;
                            sampleCid = link.asText();
                            break;
                        }
                    }
                }
            }
        }
        
        Assertions.assertTrue(foundValidBlobRef, 
            "Should find at least one valid blob reference with $link");
        Assertions.assertNotNull(sampleCid, "Should extract CID from blob reference");
        Assertions.assertTrue(sampleCid.startsWith("bafkrei"), 
            "CID should start with 'bafkrei' (IPFS CIDv1)");
        
        System.out.println("✓ Found valid blob reference");
        System.out.println("Sample CID: " + sampleCid);
    }

    @Test
    @Order(10)
    public void testDataSanitization() throws Exception {
        if (createSessionMock == null || searchWithImagesMock == null) {
            System.out.println("⚠️  Skipping test - mocks not available");
            return;
        }

        System.out.println("\n=== Testing Data Sanitization ===");
        
        // Check create session sanitization
        JsonNode session = objectMapper.readTree(createSessionMock);
        String email = session.get("email").asText();
        String handle = session.get("handle").asText();
        String accessJwt = session.get("accessJwt").asText();
        
        Assertions.assertTrue(email.equals("test@example.com"), 
            "Email should be sanitized");
        Assertions.assertTrue(handle.contains("testuser"), 
            "Handle should be sanitized");
        Assertions.assertTrue(accessJwt.contains("sanitized"), 
            "Access token should be sanitized");
        
        // Check search response sanitization
        JsonNode searchResponse = objectMapper.readTree(searchWithImagesMock);
        JsonNode posts = searchResponse.get("posts");
        JsonNode firstPost = posts.get(0);
        JsonNode author = firstPost.get("author");
        
        String displayName = author.get("displayName").asText();
        String authorHandle = author.get("handle").asText();
        
        Assertions.assertTrue(displayName.startsWith("Test User"), 
            "Display name should be sanitized");
        Assertions.assertTrue(authorHandle.contains("testuser"), 
            "Author handle should be sanitized");
        
        System.out.println("✓ All sensitive data properly sanitized");
        System.out.println("✓ Mock files safe for git commits");
    }

    private static String loadMockFile(String filename) {
        try {
            String path = "src/test/resources/bluesky-mocks/" + filename;
            if (Files.exists(Paths.get(path))) {
                return Files.readString(Paths.get(path));
            } else {
                System.err.println("Warning: Mock file not found: " + path);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error loading mock file " + filename + ": " + e.getMessage());
            return null;
        }
    }
}
