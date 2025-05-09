# stage 1: build tool

FROM alpine:3.21 AS build

# copy source code
COPY ./src ./RDFmutate/src
COPY ./build.gradle /RDFmutate/build.gradle
COPY ./examples/miniPipes.ttl /RDFmutate/examples/miniPipes.ttl

# install java and gradle
RUN apk add openjdk17 gradle

# set java version
ENV JAVA_HOME="/usr/lib/jvm/java-17-openjdk"
ENV PATH=$JAVA_HOME/bin:$PATH

# init gradle repository
WORKDIR "/RDFmutate"
RUN gradle wrapper

# build gradle repo, i.e., produce jar
RUN ./gradlew shadowJar



# stage 2: container to run jar
FROM alpine:3.21

# copy jar
COPY --from=build /RDFmutate/build/libs/rdfmutate-1.0.jar /RDFmutate/build/libs/rdfmutate-1.0.jar

# copy scripts
COPY ./scriptsDocker/test_system.sh  /RDFmutate/test_system.sh
COPY ./examples/miniPipes.ttl /RDFmutate/examples/miniPipes.ttl
COPY ./scriptsDocker/mutate.sh  /RDFmutate/mutate.sh


# add java JRE
RUN apk add openjdk17-jre

WORKDIR "/RDFmutate"