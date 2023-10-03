# The `universal` lib

To publish (note this should only be run in the build pipeline)

```bash
cd deploy-service/universal && mvn deploy
```

To consume

In pom.xml, add

```xml
<repositories>
    <repository>
        <id>snapshots</id>
        <name>maven-snapshots</name>
        <url>https://artifacts-prod-use1.pinadmin.com/artifactory/maven-private-prod-sox-local</url>
        <releases>
            <enabled>true</enabled>
        </releases>
        <snapshots>
            <enabled>true</enabled>
        </snapshots>
    </repository>
</repositories>

...

<dependencies>
    <dependency>
        <groupId>com.pinterest.teletraan</groupId>
        <artifactId>universal</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```