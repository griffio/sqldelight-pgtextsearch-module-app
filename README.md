# SqlDelight 2.3.x Postgresql pg_textsearch module support 

https://github.com/cashapp/sqldelight

**Experimental**

Use with SqlDelight `2.3.x` or higher   

---

SqlDelight pg_textsearch Module support

https://github.com/timescale/pg_textsearch

## Usage

Instead of a new dialect or adding PostgreSql extensions into the core PostgreSql grammar e.g. https://postgis.net/ and https://github.com/pgvector/pgvector

Use a custom SqlDelight module to implement the type resolver for pgtextsearch operations

```kotlin
sqldelight {
    databases {
        create("Sample") {
            deriveSchemaFromMigrations.set(true)
            migrationOutputDirectory = file("$buildDir/generated/migrations")
            migrationOutputFileFormat = ".sql"
            packageName.set("griffio.queries")
            dialect(libs.sqldelight.postgresql.dialect)
            module(project(":pgtextsearch")) // module can be local project
            // or external dependency module("io.github.griffio:sqldelight-pgtextsearch-module:0.0.1")
        }
    }
}
```

`pgtextsearch-module` published in Maven Central https://central.sonatype.com/artifact/io.github.griffio/sqldelight-pgtextsearch/versions

`io.github.griffio:sqldelight-pgtextsearch:0.0.1`

```sql
CREATE EXTENSION IF NOT EXISTS pg_textsearch;

CREATE TABLE documents (
    id BIGSERIAL PRIMARY KEY,
    content TEXT
);

INSERT INTO documents (content) VALUES
    ('PostgreSQL is a powerful database system'),
    ('BM25 is an effective ranking function'),
    ('Full text search with custom scoring');

CREATE INDEX docs_idx ON documents USING bm25(content) WITH (text_config='english');
```

Implicit index name with text

```sql
select:
SELECT * FROM documents
ORDER BY content <@> 'database system'
LIMIT 5;
```

Bind parameters need to use `to_bm25query` and explicit index name

```sql
topk:
SELECT * FROM documents
ORDER BY content <@> to_bm25query(:query, 'docs_idx')
LIMIT 5;
```

```sql
score:
SELECT * FROM documents
WHERE content <@> to_bm25query(:query, 'docs_idx') < -1.0;
```
---

```shell
docker run -d --name timescaledb \
    -p 6543:5432 \
    -e POSTGRES_PASSWORD=password \
    timescale/timescaledb-ha:pg18
```

```shell
./gradlew build &&
./gradlew flywayMigrate
```
