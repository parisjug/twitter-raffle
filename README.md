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
# Run classic mode
java -Dbluesky.identifier=<your-handle-or-email> -Dbluesky.password=<your-app-password> -jar target/bluesky-raffle-1.0.0-SNAPSHOT-runner.jar

# Run native mode
target/bluesky-raffle-1.0.0-SNAPSHOT-runner -Dbluesky.identifier=<your-handle-or-email> -Dbluesky.password=<your-app-password>
```

Run providing Bluesky client credentials using environment variables:

```shell
export bluesky.identifier=<your-handle-or-email>
export bluesky.password=<your-app-password>

# Run classic mode
java -jar target/bluesky-raffle-1.0.0-SNAPSHOT-runner.jar

# Run native mode
target/bluesky-raffle-1.0.0-SNAPSHOT-runner
```

**Note:** You need to generate an app password from your Bluesky account settings to use with this application.
