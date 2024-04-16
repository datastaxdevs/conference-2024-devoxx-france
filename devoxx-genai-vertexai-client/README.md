# langchain4j-astradb-demo

Sample Codes to use VertexAI directly from Java using the Google Libraries

## Setup

gcloud config set project devoxxfrance
gcloud auth login
gcloud auth application-default login

## Build dependencies

Run from the parent folder

```
mvn clean
```

## Run VertexAI chat with gemini-pro

```
mvn test -Dtest=Demo01_VertexClientChat#testChat
```

## Run VertexAI image recognition with gemini-pro-vision

```
mvn test -Dtest=Demo02_VertexClientVisionPro#testVision 
```

To try an other picture replace ```img1.png``` in the ```/resources``` folder
