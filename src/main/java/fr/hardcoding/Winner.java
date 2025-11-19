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
        if (post.record != null && post.record.embed != null && 
            post.record.embed.images != null && post.record.embed.images.length > 0) {
            winner.imageUrl = post.record.embed.images[0].fullsize;
        }
        
        return winner;
    }

    public String toString() {
        return this.name + " (@" + this.screenName + "): " + this.postUrl;
    }
}