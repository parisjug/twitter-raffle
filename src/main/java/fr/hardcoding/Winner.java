package fr.hardcoding;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Winner {
    public String name;
    public String screenName;
    public String postUrl;

    public static Winner fromBlueskyPost(BlueskyPost post) {
        Winner winner = new Winner();
        winner.name = post.author.displayName != null ? post.author.displayName : post.author.handle;
        winner.screenName = post.author.handle;
        // Extract post ID from URI: at://did:plc:xxx/app.bsky.feed.post/yyy
        String postId = post.uri.substring(post.uri.lastIndexOf('/') + 1);
        winner.postUrl = "https://bsky.app/profile/" + winner.screenName + "/post/" + postId;
        return winner;
    }

    public String toString() {
        return this.name + " (@" + this.screenName + "): " + this.postUrl;
    }
}