package fr.hardcoding;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Winner {
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
        
        // Extract post text
        if (post.record != null && post.record.text != null) {
            winner.postText = post.record.text;
        }
        
        // Extract first image if available
        // Try record.embed first (this is where images should be)
        if (post.record != null && post.record.embed != null && 
            post.record.embed.images != null && post.record.embed.images.length > 0) {
            String imageUrl = post.record.embed.images[0].fullsize;
            // If the URL is not complete, try thumb as fallback
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = post.record.embed.images[0].thumb;
            }
            winner.imageUrl = imageUrl;
        }
        // Fallback to top-level embed if record.embed doesn't have images
        else if (post.embed != null && post.embed.images != null && post.embed.images.length > 0) {
            String imageUrl = post.embed.images[0].fullsize;
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = post.embed.images[0].thumb;
            }
            winner.imageUrl = imageUrl;
        }
        
        return winner;
    }

    public String toString() {
        return this.name + " (@" + this.screenName + "): " + this.postUrl;
    }
}