# Bluesky Mock Data Directory

This directory contains mock API responses for testing.

**These files are NOT committed to git** - they contain real API responses from your account.

## Generate Mock Data

Run the mock data generator to create these files:

```shell
mvn test -Dtest=MockDataGenerator \
  -Dbluesky.identifier=<your-handle-or-email> \
  -Dbluesky.password=<your-app-password>
```

## Expected Files

After running the generator, you should have:
- `create-session-response.json`
- `search-response-with-images.json`
- `search-response-no-images.json`
- `oembed-response.json`

## Example Files

The `.gitkeep-example-*.json` files show the structure but contain placeholder data.
