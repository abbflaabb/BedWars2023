# BedWars2023 API on GitHub Packages

Coordinates: `com.tomkeuper.bedwars:bedwars-api:5.0.1`

Repository: `https://maven.pkg.github.com/abbflaabb/BedWars2023`

This package is for addon developers. Install the BedWars plugin and TAB on the
server separately. Do not shade the API into your addon; use `compileOnly` or
Maven's `provided` scope and add `depend: [BedWars2023]` to your `plugin.yml`.
Your project must also supply its Bukkit/Spigot/Paper API dependency.

GitHub's Maven registry requires authentication even for public packages. Set
`GITHUB_ACTOR` to your GitHub username and `GITHUB_TOKEN` to a personal access
token (classic) with `read:packages`. Keep credentials out of source control.
See [GitHub's registry documentation](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry).

## Gradle (Groovy)

```groovy
repositories {
    maven {
        url = uri('https://maven.pkg.github.com/abbflaabb/BedWars2023')
        credentials {
            username = System.getenv('GITHUB_ACTOR')
            password = System.getenv('GITHUB_TOKEN')
        }
        content { includeGroup 'com.tomkeuper.bedwars' }
    }
}
dependencies {
    compileOnly 'com.tomkeuper.bedwars:bedwars-api:5.0.1'
}
```

## Maven

Add to your `pom.xml`:

```xml
<repositories>
  <repository>
    <id>bedwars-github</id>
    <url>https://maven.pkg.github.com/abbflaabb/BedWars2023</url>
  </repository>
</repositories>
<dependencies>
  <dependency>
    <groupId>com.tomkeuper.bedwars</groupId>
    <artifactId>bedwars-api</artifactId>
    <version>5.0.1</version>
    <scope>provided</scope>
  </dependency>
</dependencies>
```

Add credentials to your local `~/.m2/settings.xml`:

```xml
<settings>
  <servers>
    <server>
      <id>bedwars-github</id>
      <username>${env.GITHUB_ACTOR}</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
```

## Publishing a future version

Build with JDK 21:

```text
./gradlew :bedwars-api:build :bedwars-api:generatePomFileForMavenJavaPublication
```

Before publishing the GitHub release, attach these assets, replacing `VERSION`
with the value of `pluginVersion` in `gradle.properties`:

- `bedwars-api-VERSION.jar`
- `bedwars-api-VERSION-sources.jar`
- `bedwars-api-VERSION-javadoc.jar`
- `bedwars-api-VERSION.pom` from `bedwars-api/build/publications/mavenJava/pom-default.xml`
- `bedwars-api-VERSION.sha256`, containing SHA-256 hashes and filenames for all four files

The **Publish BedWars API to GitHub Packages** workflow downloads the release's
artifacts, verifies checksums and Maven coordinates, then publishes with the
repository's temporary `GITHUB_TOKEN`. It can also be started manually for an
existing release. Source, binary and documentation must come from the same
release source. Do not overwrite an already published version.

Maintainers with a classic token granting `write:packages` can alternatively
run `:bedwars-api:publishMavenJavaPublicationToGitHubPackagesRepository` locally.
