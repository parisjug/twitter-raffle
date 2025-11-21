package fr.hardcoding;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;
import static jakarta.ws.rs.core.Response.Status.SERVICE_UNAVAILABLE;

@Path("/")
public class BlueskyRaffle {
    private static final Logger LOGGER = Logger.getLogger(BlueskyRaffle.class.getName());
    private static final int WINNER_COUNT = 10;
    private static final int MAX_RESULT = 100;
    private static final int SEARCH_LIMIT = 25;

    private String accessToken = null;

    @ConfigProperty(name = "bluesky.identifier")
    Optional<String> identifier;

    @ConfigProperty(name = "bluesky.password")
    Optional<String> password;

    @ConfigProperty(name = "bluesky.mock.enabled", defaultValue = "false")
    boolean mockEnabled;

    @Inject
    @RestClient
    BlueskyClient blueskyClient;

    @Inject
    MockBlueskyClient mockBlueskyClient;

    @Inject
    @RestClient
    OEmbedClient oembedClient;

    @Inject
    MockOEmbedClient mockOEmbedClient;

    private BlueskyClient getClient() {
        return mockEnabled ? mockBlueskyClient : blueskyClient;
    }

    private OEmbedClient getOEmbedClient() {
        return mockEnabled ? mockOEmbedClient : oembedClient;
    }

    private String getAccessToken() {
        if (mockEnabled) {
            // In mock mode, return a placeholder token
            LOGGER.info("Using mock mode - no authentication required");
            return "Bearer mock-token";
        }
        
        if (accessToken == null) {
            if (identifier.isPresent() && password.isPresent()) {
                try {
                    BlueskyClient.SessionRequest request = new BlueskyClient.SessionRequest(
                            identifier.get(), password.get());
                    BlueskyClient.SessionResponse session = getClient().createSession(request);
                    accessToken = "Bearer " + session.accessJwt;
                    LOGGER.info("Successfully authenticated with Bluesky as " + session.handle);
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Failed to authenticate with Bluesky", e);
                    throw new RuntimeException("Failed to authenticate with Bluesky", e);
                }
            } else {
                throw new RuntimeException("Bluesky credentials not configured");
            }
        }
        return accessToken;
    }

    @Path("/raffle")
    @GET
    @Produces(APPLICATION_JSON)
    public Response hello(@QueryParam("speaker") String speaker) {
        try {
            List<Winner> winners = performRaffle(speaker);
            return Response.ok(winners).build();
        } catch (Exception exception) {
            LOGGER.log(Level.SEVERE, "Failed to query posts", exception);
            return Response.status(SERVICE_UNAVAILABLE).build();
        }
    }

    @Path("/embed")
    @GET
    @Produces(APPLICATION_JSON)
    public Post getEmbed(@QueryParam("url") String url) {
        try {
            // Call Bluesky's official oEmbed API
            OEmbedClient.OEmbedResponse oembedResponse = getOEmbedClient().getOEmbed(url);
            
            // Convert to Post format
            Post post = new Post();
            post.url = url;
            post.author_name = oembedResponse.author_name;
            post.author_url = oembedResponse.author_url;
            post.html = oembedResponse.html;
            
            LOGGER.info("Successfully fetched oEmbed for URL: " + url);
            return post;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to fetch oEmbed for URL: " + url, e);
            
            // Fallback to simple embed if oEmbed API fails
            Post post = new Post();
            post.url = url;
            
            try {
                String[] parts = url.split("/");
                if (parts.length >= 6) {
                    String handle = parts[4];
                    post.author_name = handle;
                    post.author_url = "https://bsky.app/profile/" + handle;
                    post.html = "<div class='bluesky-embed'><a href='" + url + "' target='_blank'>View post on Bluesky</a></div>";
                }
            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Failed to parse Bluesky URL: " + url, ex);
            }
            
            return post;
        }
    }

    private List<Winner> performRaffle(String speaker) {
        String query = getQuery(speaker);
        Predicate<BlueskyPost> filter = getPostFilter(speaker);
        Map<String, List<BlueskyPost>> userPosts = performQuery(query, filter);

        Random rand = new Random(System.currentTimeMillis());
        List<String> users = new LinkedList<>(userPosts.keySet());
        Set<String> winningUsers = new HashSet<>();

        while (winningUsers.size() < Math.min(WINNER_COUNT, userPosts.size())) {
            String winner = users.remove(rand.nextInt(users.size()));
            winningUsers.add(winner);
        }

        return winningUsers.stream()
                .map(userPosts::get)
                .map(list -> list.get(0))
                .map(Winner::fromBlueskyPost)
                .collect(Collectors.toList());
    }

    private String getQuery(String speaker) {
        // Must mention ParisJUG and the given speaker
        return "parisjug " + speaker;
    }

    private Predicate<BlueskyPost> getPostFilter(String speaker) {
        return post -> {
            // Filter out posts without images
            if (post.record == null || post.record.embed == null || 
                post.record.embed.images == null || post.record.embed.images.length == 0) {
                LOGGER.info("Filtering post without images: " + post.uri);
                return false;
            }

            // Remove parisjug, given speaker, white spaces, and check if remains few text
            String text = post.record.text;
            String originalText = text;
            text = text.toLowerCase();
            text = text.replace("parisjug", "");
            for (String part : speaker.toLowerCase().split(" ")) {
                text = text.replace(part, "");
            }
            // Remove URLs (Bluesky uses different URL format)
            text = text.replaceAll("https?://[^\\s]+", "");
            text = text.replaceAll("[ \n]", "");
            if (text.length() <= 5) {
                LOGGER.info("Filtering post with too little content: " + originalText);
                return false;
            }
            return true;
        };
    }

    private Map<String, List<BlueskyPost>> performQuery(String query, Predicate<BlueskyPost> filter) {
        Map<String, List<BlueskyPost>> userPosts = new HashMap<>();
        String cursor = null;
        int totalFetched = 0;

        try {
            do {
                BlueskyClient.SearchResponse result = getClient().searchPosts(
                        getAccessToken(), query, SEARCH_LIMIT, cursor);
                
                LOGGER.info(String.format("Fetched %d posts, total users so far: %d", 
                        result.posts != null ? result.posts.length : 0, userPosts.size()));

                if (result.posts != null) {
                    for (BlueskyPost post : result.posts) {
                        if (filter.test(post)) {
                            String handle = post.author.handle;
                            userPosts.computeIfAbsent(handle, __ -> new ArrayList<>()).add(post);
                        }
                    }
                    totalFetched += result.posts.length;
                }

                cursor = result.cursor;
                
                // Stop if we have enough users or fetched enough posts
                if (userPosts.size() >= MAX_RESULT || totalFetched >= MAX_RESULT * 2) {
                    break;
                }
                
            } while (cursor != null && !cursor.isEmpty());
            
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during search", e);
        }

        return userPosts;
    }
}