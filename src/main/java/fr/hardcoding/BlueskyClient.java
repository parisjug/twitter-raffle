package fr.hardcoding;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;

@Path("/xrpc")
@RegisterRestClient(configKey = "bluesky-api")
public interface BlueskyClient {

    @POST
    @Path("/com.atproto.server.createSession")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    SessionResponse createSession(SessionRequest request);

    @GET
    @Path("/app.bsky.feed.searchPosts")
    @Produces(MediaType.APPLICATION_JSON)
    SearchResponse searchPosts(
            @HeaderParam("Authorization") String authorization,
            @QueryParam("q") String query,
            @QueryParam("limit") Integer limit,
            @QueryParam("cursor") String cursor);

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    class SessionRequest {
        public String identifier;
        public String password;

        public SessionRequest() {}

        public SessionRequest(String identifier, String password) {
            this.identifier = identifier;
            this.password = password;
        }
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    class SessionResponse {
        public String did;
        public String handle;
        public String email;
        public String accessJwt;
        public String refreshJwt;
    }

    @RegisterForReflection
    @JsonIgnoreProperties(ignoreUnknown = true)
    class SearchResponse {
        public String cursor;
        public BlueskyPost[] posts;
    }
}
