package fr.hardcoding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlueskyPost {
    public String uri;
    public String cid;
    public Author author;
    public Record record;
    public Embed embed;
    public String indexedAt;
    public Integer likeCount;
    public Integer repostCount;
    public Integer replyCount;

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
        @JsonProperty("$type")
        public String $type;
        public Image[] images;
        public Embed media;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        public String thumb;
        public String fullsize;
        public String alt;
        // AT Protocol also uses these fields
        public ImageBlob image;
    }
    
    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ImageBlob {
        public String $type;  // AT Protocol uses $type
        public BlobRef ref;
        public String mimeType;
        public Integer size;
    }
    
    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class BlobRef {
        public String $link;  // AT Protocol might use $link
        public String link;   // Or just link
        public String cid;    // Or cid directly
    }
}
