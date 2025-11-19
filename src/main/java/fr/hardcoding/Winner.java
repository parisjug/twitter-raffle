package fr.hardcoding;

import io.quarkus.runtime.annotations.RegisterForReflection;
import java.util.logging.Logger;

@RegisterForReflection
public class Winner {
    private static final Logger LOGGER = Logger.getLogger(Winner.class.getName());
    
    public String name;
    public String screenName;
    public String postUrl;
    public String postText;
    public String imageUrl;

    public static Winner fromBlueskyPost(BlueskyPost post) {
        Winner winner = new Winner();
        winner.name = post.author.displayName != null ? post.author.displayName : post.author.handle;
        winner.screenName = post.author.handle;
        // Extract post ID from URI: at://did:plc:xxx/app.bsky.feed.post/yyy
        String postId = post.uri.substring(post.uri.lastIndexOf('/') + 1);
        winner.postUrl = "https://bsky.app/profile/" + winner.screenName + "/post/" + postId;
        
        LOGGER.info("Processing post for winner: " + winner.screenName);
        LOGGER.info("Post URI: " + post.uri);
        
        // Extract post text
        if (post.record != null && post.record.text != null) {
            winner.postText = post.record.text;
        }
        
        // Debug logging for embed structure
        LOGGER.info("Post record: " + (post.record != null ? "present" : "null"));
        LOGGER.info("Post record.embed: " + (post.record != null && post.record.embed != null ? "present" : "null"));
        LOGGER.info("Post record.embed.images: " + (post.record != null && post.record.embed != null && post.record.embed.images != null ? "array length=" + post.record.embed.images.length : "null"));
        LOGGER.info("Post top-level embed: " + (post.embed != null ? "present" : "null"));
        LOGGER.info("Post top-level embed.images: " + (post.embed != null && post.embed.images != null ? "array length=" + post.embed.images.length : "null"));
        
        // Extract first image if available
        // Try record.embed first (this is where images should be)
        if (post.record != null && post.record.embed != null && 
            post.record.embed.images != null && post.record.embed.images.length > 0) {
            String imageUrl = post.record.embed.images[0].fullsize;
            LOGGER.info("Found image in record.embed.images[0].fullsize: " + imageUrl);
            // If the URL is not complete, try thumb as fallback
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = post.record.embed.images[0].thumb;
                LOGGER.info("Fallback to record.embed.images[0].thumb: " + imageUrl);
            }
            winner.imageUrl = imageUrl;
        }
        // Fallback to top-level embed if record.embed doesn't have images
        else if (post.embed != null && post.embed.images != null && post.embed.images.length > 0) {
            String imageUrl = post.embed.images[0].fullsize;
            LOGGER.info("Found image in top-level embed.images[0].fullsize: " + imageUrl);
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = post.embed.images[0].thumb;
                LOGGER.info("Fallback to top-level embed.images[0].thumb: " + imageUrl);
            }
            winner.imageUrl = imageUrl;
        } else {
            LOGGER.warning("No images found in either record.embed or top-level embed for post: " + post.uri);
        }
        
        LOGGER.info("Final imageUrl for winner: " + winner.imageUrl);
        
        return winner;
    }

    public String toString() {
        return this.name + " (@" + this.screenName + "): " + this.postUrl;
    }
}