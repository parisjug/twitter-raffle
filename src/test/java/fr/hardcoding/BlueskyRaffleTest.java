package fr.hardcoding;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.RestAssured;
import org.junit.jupiter.api.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;

/**
 * Simple high-level tests to validate BlueskyRaffle code works with mock data.
 * 
 * Generate mock files first (see README.md):
 * mvn test -Dtest=MockDataGenerator -Dbluesky.identifier=<your-handle> -Dbluesky.password=<your-app-password>
 */
@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BlueskyRaffleTest {

    private static ObjectMapper objectMapper = new ObjectMapper();
    private static BlueskyClient.SearchResponse searchResponse;
    private static boolean mocksAvailable = false;

    @BeforeAll
    public static void loadMocks() {
        try {
            String searchJson = Files.readString(
                Paths.get("src/test/resources/bluesky-mocks/search-response-with-images.json"));
            
            searchResponse = objectMapper.readValue(searchJson, BlueskyClient.SearchResponse.class);
            mocksAvailable = true;
            
            System.out.println("✓ Loaded " + searchResponse.posts.length + " posts from mock data");
        } catch (Exception e) {
            System.out.println("⚠️  Mock files not found. Generate them with:");
            System.out.println("   mvn test -Dtest=MockDataGenerator -Dbluesky.identifier=<handle> -Dbluesky.password=<password>");
        }
    }

    @Test
    @Order(1)
    public void testCanLoadAndParseMockData() {
        Assumptions.assumeTrue(mocksAvailable, "Mock files not available");
        
        // Simply verify we can load and parse the mock search response
        Assertions.assertNotNull(searchResponse);
        Assertions.assertNotNull(searchResponse.posts);
        Assertions.assertTrue(searchResponse.posts.length > 0, "Should have posts in mock data");
        
        System.out.println("✓ Successfully parsed " + searchResponse.posts.length + " posts from mock");
    }

    @Test
    @Order(2)
    public void testWinnerCreationFromPost() {
        Assumptions.assumeTrue(mocksAvailable, "Mock files not available");
        
        // Test creating a Winner object from a mock post
        BlueskyPost post = searchResponse.posts[0];
        Winner winner = Winner.fromBlueskyPost(post);
        
        // Verify Winner object was created with expected data
        Assertions.assertNotNull(winner);
        Assertions.assertNotNull(winner.screenName, "Winner should have screenName");
        Assertions.assertNotNull(winner.postUrl, "Winner should have postUrl");
        
        System.out.println("✓ Created Winner from post: " + winner.screenName);
    }

    @Test
    @Order(3)
    public void testPostFiltering() {
        Assumptions.assumeTrue(mocksAvailable, "Mock files not available");
        
        // Test filtering posts (basic functionality that BlueskyRaffle uses)
        List<BlueskyPost> postsWithImages = Stream.of(searchResponse.posts)
            .filter(post -> {
                // Has images in embed
                if (post.record != null && post.record.embed != null && post.record.embed.images != null && post.record.embed.images.length > 0) {
                    return true;
                }
                if (post.embed != null && post.embed.images != null && post.embed.images.length > 0) {
                    return true;
                }
                return false;
            })
            .collect(Collectors.toList());
        
        Assertions.assertTrue(postsWithImages.size() > 0, "Should find some posts with images");
        
        System.out.println("✓ Found " + postsWithImages.size() + " posts with images out of " + searchResponse.posts.length + " total");
    }

    @Test
    @Order(4)
    public void testOEmbedEndpoint() {
        Assumptions.assumeTrue(mocksAvailable, "Mock files not available");
        
        // Test the /embed endpoint with mock mode
        String testPostUrl = "https://bsky.app/profile/testuser.bsky.social/post/3m5ypww5hvs2j";
        
        given()
            .queryParam("url", testPostUrl)
        .when()
            .get("/embed")
        .then()
            .statusCode(200)
            .body("url", equalTo(testPostUrl))
            .body("author_name", notNullValue())
            .body("html", notNullValue())
            .body("html", containsString("blockquote"));
        
        System.out.println("✓ OEmbed endpoint works correctly");
    }
}
