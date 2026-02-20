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

        // Extract post text
        if (post.record != null && post.record.text != null) {
            winner.postText = post.record.text;
        }
        
        // Extract first image if available
        // Try top-level view embed first (has thumb/fullsize URLs directly)
        BlueskyPost.Image firstImage = findFirstImage(post);
        if (firstImage != null) {
            String imageUrl = firstImage.fullsize != null ? firstImage.fullsize : firstImage.thumb;
            if (imageUrl == null && firstImage.image != null && firstImage.image.ref != null) {
                String did = post.author.did;
                String cid = firstImage.image.ref.$link != null ? firstImage.image.ref.$link :
                        (firstImage.image.ref.link != null ? firstImage.image.ref.link : firstImage.image.ref.cid);
                if (cid != null && !cid.isEmpty()) {
                    imageUrl = "https://bsky.social/xrpc/com.atproto.sync.getBlob?did=" + did + "&cid=" + cid;
                }
            }
            winner.imageUrl = imageUrl;
        } else {
            LOGGER.warning("No images found for post: " + post.uri);
        }
        
        return winner;
    }

    private static BlueskyPost.Image findFirstImage(BlueskyPost post) {
        if (post.embed != null) {
            if (post.embed.images != null && post.embed.images.length > 0) {
                return post.embed.images[0];
            }
            if (post.embed.media != null && post.embed.media.images != null && post.embed.media.images.length > 0) {
                return post.embed.media.images[0];
            }
        }
        if (post.record != null && post.record.embed != null &&
                post.record.embed.images != null && post.record.embed.images.length > 0) {
            return post.record.embed.images[0];
        }
        return null;
    }

    public String toString() {
        return this.name + " (@" + this.screenName + "): " + this.postUrl;
    }
}