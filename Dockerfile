# stage 1: build tool

FROM alpine:3.21 AS build

# copy source code
COPY ./src ./OntoMutate/src
COPY ./build.gradle /OntoMutate/build.gradle

# install java and gradle
RUN apk add openjdk17 gradle

# set java version
ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
ENV PATH=$JAVA_HOME/bin:$PATH

# init gradle repository
WORKDIR "/OntoMutate"
RUN gradle wrapper

# build gradle repo, i.e., produce jar
RUN ./gradlew build



# stage 2: container to run jar
FROM alpine:3.21

# copy jar
COPY --from=build /OntoMutate/build/libs/OntoMutate-0.1.jar /OntoMutate/build/libs/OntoMutate-0.1.jar

# copy scripts
COPY ./scriptsDocker/test_system.sh  /OntoMutate/test_system.sh
COPY ./examples/miniPipes.ttl /OntoMutate/examples/miniPipes.ttl
COPY ./scriptsDocker/mutate.sh  /OntoMutate/mutate.sh


# add java JRE
RUN apk add openjdk17-jre

WORKDIR "/OntoMutate"