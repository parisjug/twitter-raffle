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
            BlueskyPost.Image firstImage = post.record.embed.images[0];
            LOGGER.info("Image object fields - fullsize: " + firstImage.fullsize + ", thumb: " + firstImage.thumb + 
                       ", alt: " + firstImage.alt + ", image: " + (firstImage.image != null ? "present" : "null"));
            
            String imageUrl = firstImage.fullsize;
            LOGGER.info("Found image in record.embed.images[0].fullsize: " + imageUrl);
            // If the URL is not complete, try thumb as fallback
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = firstImage.thumb;
                LOGGER.info("Fallback to record.embed.images[0].thumb: " + imageUrl);
            }
            // If still no URL, try to construct from blob reference
            if ((imageUrl == null || imageUrl.isEmpty()) && firstImage.image != null) {
                LOGGER.info("Image blob present - type: " + firstImage.image.$type + ", mimeType: " + firstImage.image.mimeType + 
                           ", size: " + firstImage.image.size + ", ref: " + (firstImage.image.ref != null ? "present" : "null"));
                
                if (firstImage.image.ref != null) {
                    LOGGER.info("BlobRef fields - $link: " + firstImage.image.ref.$link + ", link: " + firstImage.image.ref.link + 
                               ", cid: " + firstImage.image.ref.cid);
                    
                    // Construct URL from blob: https://bsky.social/xrpc/com.atproto.sync.getBlob?did={did}&cid={cid}
                    String did = post.author.did;
                    String cid = firstImage.image.ref.$link != null ? firstImage.image.ref.$link : 
                                (firstImage.image.ref.link != null ? firstImage.image.ref.link : firstImage.image.ref.cid);
                    
                    if (cid != null && !cid.isEmpty()) {
                        imageUrl = "https://bsky.social/xrpc/com.atproto.sync.getBlob?did=" + did + "&cid=" + cid;
                        LOGGER.info("Constructed image URL from blob reference: " + imageUrl);
                    } else {
                        LOGGER.warning("All CID fields are null in blob reference");
                    }
                } else {
                    LOGGER.warning("Image blob ref is null");
                }
            }
            winner.imageUrl = imageUrl;
        }
        // Fallback to top-level embed if record.embed doesn't have images
        else if (post.embed != null && post.embed.images != null && post.embed.images.length > 0) {
            BlueskyPost.Image firstImage = post.embed.images[0];
            LOGGER.info("Top-level image object fields - fullsize: " + firstImage.fullsize + ", thumb: " + firstImage.thumb + 
                       ", alt: " + firstImage.alt + ", image: " + (firstImage.image != null ? "present" : "null"));
            
            String imageUrl = firstImage.fullsize;
            LOGGER.info("Found image in top-level embed.images[0].fullsize: " + imageUrl);
            if (imageUrl == null || imageUrl.isEmpty()) {
                imageUrl = firstImage.thumb;
                LOGGER.info("Fallback to top-level embed.images[0].thumb: " + imageUrl);
            }
            // If still no URL, try to construct from blob reference
            if ((imageUrl == null || imageUrl.isEmpty()) && firstImage.image != null) {
                LOGGER.info("Top-level image blob present - type: " + firstImage.image.$type + ", mimeType: " + firstImage.image.mimeType + 
                           ", size: " + firstImage.image.size + ", ref: " + (firstImage.image.ref != null ? "present" : "null"));
                
                if (firstImage.image.ref != null) {
                    LOGGER.info("Top-level BlobRef fields - $link: " + firstImage.image.ref.$link + ", link: " + firstImage.image.ref.link + 
                               ", cid: " + firstImage.image.ref.cid);
                    
                    String did = post.author.did;
                    String cid = firstImage.image.ref.$link != null ? firstImage.image.ref.$link : 
                                (firstImage.image.ref.link != null ? firstImage.image.ref.link : firstImage.image.ref.cid);
                    
                    if (cid != null && !cid.isEmpty()) {
                        imageUrl = "https://bsky.social/xrpc/com.atproto.sync.getBlob?did=" + did + "&cid=" + cid;
                        LOGGER.info("Constructed image URL from blob reference: " + imageUrl);
                    } else {
                        LOGGER.warning("All CID fields are null in top-level blob reference");
                    }
                } else {
                    LOGGER.warning("Top-level image blob ref is null");
                }
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