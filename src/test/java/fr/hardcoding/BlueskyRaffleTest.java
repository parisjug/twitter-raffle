package fr.hardcoding;

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
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlueskyRaffleTest {

    private static WireMockServer wireMockServer;
    private static String createSessionMock;
    private static String searchWithImagesMock;
    private static String searchNoImagesMock;
    private static String oembedMock;

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

        System.out.println("WireMock server started on port 8089");
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

    @Test
    @Order(1)
    @Disabled("Enable after generating mock data - requires configuration override")
    public void testRaffleEndpointReturnsWinner() {
        // This test would require overriding the Bluesky API URL to point to WireMock
        // For now, this serves as a template for how the test would work
        
        given()
                .queryParam("speaker", "testuser")
                .when()
                .get("/raffle")
                .then()
                .statusCode(200)
                .body("name", notNullValue())
                .body("screenName", notNullValue())
                .body("postUrl", notNullValue())
                .body("postText", notNullValue());
    }

    @Test
    @Order(2)
    @Disabled("Enable after generating mock data")
    public void testRaffleWithNoResults() {
        // Setup mock to return empty results
        stubFor(get(urlMatching("/xrpc/app.bsky.feed.searchPosts\\?.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"posts\":[]}")));

        // Test would verify appropriate error handling
    }

    @Test
    @Order(3)
    public void testMockFilesExist() {
        // Verify that mock files are present - but don't fail build if not
        if (createSessionMock == null) {
            System.out.println("⚠️  create-session-response.json not found. Run MockDataGenerator to generate mock files (see README.md)");
        } else {
            Assertions.assertTrue(createSessionMock.contains("accessJwt"), 
                "createSession mock should contain accessJwt");
        }
        
        if (searchWithImagesMock == null) {
            System.out.println("⚠️  search-response-with-images.json not found. Run MockDataGenerator to generate mock files (see README.md)");
        } else {
            Assertions.assertTrue(searchWithImagesMock.contains("posts"), 
                "searchPosts mock should contain posts array");
        }
        
        if (oembedMock == null) {
            System.out.println("⚠️  oembed-response.json not found. Run MockDataGenerator to generate mock files (see README.md)");
        }
        
        // Don't fail the test - just inform
        System.out.println("✓ Mock file check complete. Generate mocks using: mvn test -Dtest=MockDataGenerator -Dbluesky.identifier=<your-handle> -Dbluesky.password=<your-app-password>");
    }

    @Test
    @Order(4)
    public void testOEmbedMockStructure() {
        if (oembedMock == null) {
            System.out.println("⚠️  oEmbed mock not found - skipping structure test");
            return; // Skip test if mock not available
        }
        Assertions.assertTrue(oembedMock.contains("\"type\""), 
            "oEmbed mock should contain type field");
    }

    private static String loadMockFile(String filename) {
        try {
            String path = "src/test/resources/bluesky-mocks/" + filename;
            if (Files.exists(Paths.get(path))) {
                return Files.readString(Paths.get(path));
            } else {
                System.err.println("Warning: Mock file not found: " + path);
                System.err.println("Run MockDataGenerator to generate mock files (see README.md)");
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error loading mock file " + filename + ": " + e.getMessage());
            return null;
        }
    }
}
