package fr.hardcoding;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Post {
    public String url;
    public String author_name;
    public String author_url;
    public String html;
    public String text;
    public String image;
}
