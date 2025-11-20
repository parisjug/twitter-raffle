# Paris JUG Bluesky raffle website

This website is the Bluesky raffle to gift our sponsor prizes.

<img src="resources/home.png" raw="true" alt="homepage">
<img src="resources/winner.png" raw="true" alt="showing a winner">

## Package

The application can be packaged as a jar application, the classic mode, or as a native application, the native mode.

### Classic Mode

Compile project with Maven:

```shell
mvn package
```

### Native Mode

Compile project with Maven using native profile:

```shell
mvn package -Pnative
```

## Run

Run providing Bluesky client credentials using CLI arguments:

```shell
# Run classic mode (Quarkus 3.x uses quarkus-run.jar)
java -Dbluesky.identifier=<your-handle-or-email> -Dbluesky.password=<your-app-password> -jar target/quarkus-app/quarkus-run.jar

# Run native mode
target/bluesky-raffle-1.0.0-SNAPSHOT-runner -Dbluesky.identifier=<your-handle-or-email> -Dbluesky.password=<your-app-password>
```

Run providing Bluesky client credentials using environment variables:

```shell
export bluesky.identifier=<your-handle-or-email>
export bluesky.password=<your-app-password>

# Run classic mode
java -jar target/quarkus-app/quarkus-run.jar

# Run native mode
target/bluesky-raffle-1.0.0-SNAPSHOT-runner
```

**Note:** You need to generate an app password from your Bluesky account settings to use with this application.

## Testing

### Generating Mock Data for Tests

The project uses WireMock to mock Bluesky API and oEmbed API responses in tests. To generate fresh mock data:

1. **Set your Bluesky credentials** (using environment variables or system properties)

2. **Run the mock data generator:**
   ```shell
   mvn test -Dtest=MockDataGenerator \
     -Dbluesky.identifier=<your-handle-or-email> \
     -Dbluesky.password=<your-app-password>
   ```

3. **Mock files will be generated in:** `src/test/resources/bluesky-mocks/`
   - `create-session-response.json` - Bluesky authentication response
   - `search-response-with-images.json` - Search results with image posts
   - `search-response-no-images.json` - Search results without images
   - `oembed-response.json` - oEmbed API response

4. **Sensitive data is automatically sanitized:**
   - Authentication tokens (`accessJwt`, `refreshJwt`) are replaced with placeholders
   - User emails are replaced with `test@example.com`
   - User handles are anonymized (e.g., `testuser123.bsky.social`)
   - Personal mentions (@username) are replaced with generic names
   - Avatar URLs are replaced with placeholders
   - DIDs are preserved (needed for blob URL construction in tests)

5. **Run the tests:**
   ```shell
   mvn test
   ```

**Note:** After sanitization, mock files are safe to commit to git without exposing personal credentials or information.

### What Gets Mocked

The test infrastructure mocks:
- **Bluesky API**: 
  - `POST /xrpc/com.atproto.server.createSession` - Authentication
  - `GET /xrpc/app.bsky.feed.searchPosts` - Post search
- **oEmbed API**:
  - `GET https://embed.bsky.app/oembed` - Post embedding

### Updating Mock Data

To update mock data with current API responses:

1. Delete existing mock files from `src/test/resources/bluesky-mocks/`
2. Re-run the mock generator as shown above
3. Commit the updated mock files if API response structure has changed

This ensures tests remain stable while allowing periodic updates to reflect API changes.

