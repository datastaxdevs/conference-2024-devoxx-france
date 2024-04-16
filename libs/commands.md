
Jars to install (until it is merged into the main repo):

```console
mvn install:install-file \
 -Dfile=langchain4j-cassandra-0.29.1.jar \
 -DgroupId=dev.langchain4j \
 -DartifactId=langchain4j-cassandra \
 -Dversion=0.29.1 \
 -Dpackaging=jar
```

```console
mvn install:install-file \
  -Dfile=langchain4j-astra-0.29.1.jar \
  -DgroupId=dev.langchain4j \
  -DartifactId=langchain4j-astradb \
  -Dversion=0.29.1 \
  -Dpackaging=jar
```
