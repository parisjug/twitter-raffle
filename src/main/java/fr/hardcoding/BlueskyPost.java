package fr.hardcoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlueskyPost {
    public String uri;
    public String cid;
    public Author author;
    public Record record;
    public Embed embed;
    public long indexedAt;
    public long likeCount;
    public long repostCount;
    public long replyCount;

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Author {
        public String did;
        public String handle;
        public String displayName;
        public String avatar;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Record {
        public String text;
        public String createdAt;
        public Embed embed;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Embed {
        public String type;
        public Image[] images;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        public String thumb;
        public String fullsize;
        public String alt;
    }
}
