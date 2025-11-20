# Bluesky Mock Data Directory

This directory contains mock API responses for testing.

## Data Sanitization

**Mock files are safe to commit to git** - the generator automatically sanitizes sensitive data:
- Authentication tokens (accessJwt, refreshJwt) → placeholders
- User emails → `test@example.com`
- User handles → anonymized (e.g., `testuser123.bsky.social`)
- Personal mentions (@username) → `@testuser`
- Avatar URLs → placeholder URLs
- DIDs are preserved (needed for blob URL construction in tests)

## Generate Mock Data

Run the mock data generator to create these files:

```shell
mvn test -Dtest=MockDataGenerator \
  -Dbluesky.identifier=<your-handle-or-email> \
  -Dbluesky.password=<your-app-password>
```

## Expected Files

After running the generator, you should have:
- `create-session-response.json` - Sanitized authentication response
- `search-response-with-images.json` - Sanitized search results with images
- `search-response-no-images.json` - Sanitized search results without images
- `oembed-response.json` - Sanitized oEmbed response

## Example Files

The `.gitkeep-example-*.json` files show the structure but contain placeholder data.
They help demonstrate the expected format even before you generate real mocks.
