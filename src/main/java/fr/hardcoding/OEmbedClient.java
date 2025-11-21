package fr.hardcoding;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterRestClient(configKey = "oembed-api")
public interface OEmbedClient {

    @GET
    @Path("/oembed")
    @Produces(MediaType.APPLICATION_JSON)
    OEmbedResponse getOEmbed(@QueryParam("url") String url);

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    class OEmbedResponse {
        public String type;
        public String version;
        public String author_name;
        public String author_url;
        public String provider_name;
        public String provider_url;
        public Integer cache_age;
        public Integer width;
        public Integer height;
        public String html;
    }
}
