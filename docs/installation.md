# Installation
There are two options to install our tool. 
1. You can use docker.
2. You can run everything locally.
## Usage using Docker
- build the docker image using the provided docker file
```
docker build -t onto-mutate .
```
- create a new container, where you can run the scripts in
```
docker run -it onto-mutate
```
- You can run the scripts in the container now

## Usage without Docker
### Requirements
 - Java JRE and JDK
 - gradle
### Setup
Build the project with gradle using
```
./gradlew build 
```
- You are now able to run the scripts from your local folder.
