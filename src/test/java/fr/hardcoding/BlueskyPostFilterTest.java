package fr.hardcoding;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Unit tests for the post filtering logic in BlueskyRaffle.
 * Tests hasImage() and isFromThisWeek() with synthetic BlueskyPost objects.
 */
@QuarkusTest
public class BlueskyPostFilterTest {

    @Inject
    BlueskyRaffle raffle;

    // ── hasImage() tests ──────────────────────────────────────────────────────

    @Test
    public void testHasImage_imagesView() {
        BlueskyPost post = postWithEmbed("app.bsky.embed.images#view", null);
        Assertions.assertTrue(raffle.hasImage(post), "Should detect app.bsky.embed.images#view as having an image");
    }

    @Test
    public void testHasImage_recordWithMediaContainingImages() {
        BlueskyPost post = postWithEmbed("app.bsky.embed.recordWithMedia#view", "app.bsky.embed.images#view");
        Assertions.assertTrue(raffle.hasImage(post), "Should detect recordWithMedia#view with images media as having an image");
    }

    @Test
    public void testHasImage_recordWithMediaContainingExternal() {
        BlueskyPost post = postWithEmbed("app.bsky.embed.recordWithMedia#view", "app.bsky.embed.external#view");
        Assertions.assertFalse(raffle.hasImage(post), "recordWithMedia#view with external media should NOT have an image");
    }

    @Test
    public void testHasImage_externalView() {
        BlueskyPost post = postWithEmbed("app.bsky.embed.external#view", null);
        Assertions.assertFalse(raffle.hasImage(post), "External embed should NOT be detected as having an image");
    }

    @Test
    public void testHasImage_noEmbed() {
        BlueskyPost post = new BlueskyPost();
        post.author = author("handle.bsky.social");
        post.record = record(Instant.now().toString());
        Assertions.assertFalse(raffle.hasImage(post), "Post with no embed should NOT have an image");
    }

    @Test
    public void testHasImage_nullType() {
        BlueskyPost post = new BlueskyPost();
        post.author = author("handle.bsky.social");
        post.record = record(Instant.now().toString());
        post.embed = new BlueskyPost.Embed();
        // $type is null
        Assertions.assertFalse(raffle.hasImage(post), "Post with null embed type should NOT have an image");
    }

    // ── isFromThisWeek() tests ─────────────────────────────────────────────────

    @Test
    public void testIsFromThisWeek_recentPost() {
        BlueskyPost post = new BlueskyPost();
        post.author = author("handle.bsky.social");
        post.record = record(Instant.now().minus(1, ChronoUnit.DAYS).toString());
        Assertions.assertTrue(raffle.isFromThisWeek(post), "Post from 1 day ago should be within this week");
    }

    @Test
    public void testIsFromThisWeek_oldPost() {
        BlueskyPost post = new BlueskyPost();
        post.author = author("handle.bsky.social");
        post.record = record(Instant.now().minus(8, ChronoUnit.DAYS).toString());
        Assertions.assertFalse(raffle.isFromThisWeek(post), "Post from 8 days ago should NOT be within this week");
    }

    @Test
    public void testIsFromThisWeek_exactlyOneWeekOld() {
        BlueskyPost post = new BlueskyPost();
        post.author = author("handle.bsky.social");
        // Exactly 7 days - 1 second should be within the week
        post.record = record(Instant.now().minus(7, ChronoUnit.DAYS).plusSeconds(1).toString());
        Assertions.assertTrue(raffle.isFromThisWeek(post), "Post just under 7 days old should be within this week");
    }

    @Test
    public void testIsFromThisWeek_fallbackToIndexedAt() {
        BlueskyPost post = new BlueskyPost();
        post.author = author("handle.bsky.social");
        post.record = new BlueskyPost.Record(); // no createdAt
        post.indexedAt = Instant.now().minus(2, ChronoUnit.DAYS).toString();
        Assertions.assertTrue(raffle.isFromThisWeek(post), "Should use indexedAt when record.createdAt is null");
    }

    // ── Helper methods ─────────────────────────────────────────────────────────

    private BlueskyPost postWithEmbed(String embedType, String mediaType) {
        BlueskyPost post = new BlueskyPost();
        post.author = author("testuser.bsky.social");
        post.record = record(Instant.now().minus(1, ChronoUnit.DAYS).toString());
        post.uri = "at://did:plc:test/app.bsky.feed.post/testrkey";

        BlueskyPost.Embed embed = new BlueskyPost.Embed();
        embed.$type = embedType;
        if (mediaType != null) {
            BlueskyPost.Embed media = new BlueskyPost.Embed();
            media.$type = mediaType;
            embed.media = media;
        }
        post.embed = embed;
        return post;
    }

    private BlueskyPost.Author author(String handle) {
        BlueskyPost.Author a = new BlueskyPost.Author();
        a.handle = handle;
        a.did = "did:plc:test";
        return a;
    }

    private BlueskyPost.Record record(String createdAt) {
        BlueskyPost.Record r = new BlueskyPost.Record();
        r.createdAt = createdAt;
        r.text = "parisjug test post";
        return r;
    }
}
