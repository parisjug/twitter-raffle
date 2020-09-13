# Paris JUG Twitter raffle website

## Package

Compile project with Maven:

```bash
mvn package
```

## Run

Run providing Twitter client credentials:

```bash
java -Dtwitter4j.oauth.consumerKey=<consumerKey> -Dtwitter4j.oauth.consumerSecret=<consumerSecret> -Dtwitter4j.oauth.accessToken=<accessToken> -Dtwitter4j.oauth.accessTokenSecret=<accessTokenSecret> -jar target/twitter-raffle-1.0.0-SNAPSHOT-runner.jar
```
