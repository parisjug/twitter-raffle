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
        
        // Test filtering posts using the new $type-based image detection (matching reference app)
        List<BlueskyPost> postsWithImages = Stream.of(searchResponse.posts)
            .filter(post -> {
                BlueskyPost.Embed embed = post.embed;
                if (embed == null) return false;
                String type = embed.$type != null ? embed.$type : "";
                if ("app.bsky.embed.images#view".equals(type)) return true;
                if ("app.bsky.embed.recordWithMedia#view".equals(type) && embed.media != null) {
                    return "app.bsky.embed.images#view".equals(embed.media.$type);
                }
                return false;
            })
            .collect(Collectors.toList());
        
        Assertions.assertTrue(postsWithImages.size() > 0, "Should find some posts with images");
        
        // All detected posts should have the correct $type
        for (BlueskyPost post : postsWithImages) {
            String type = post.embed != null ? post.embed.$type : null;
            boolean isDirectImage = "app.bsky.embed.images#view".equals(type);
            boolean isRecordWithMediaImage = "app.bsky.embed.recordWithMedia#view".equals(type)
                    && post.embed.media != null
                    && "app.bsky.embed.images#view".equals(post.embed.media.$type);
            Assertions.assertTrue(isDirectImage || isRecordWithMediaImage,
                    "Post should have image embed type, got: " + type);
        }
        
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
