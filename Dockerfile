FROM ubuntu:22.04 AS build
WORKDIR /app

RUN apt-get update && apt-get install -y \
    openjdk-25-jdk \
    maven \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM openjdk:25-jdk-slim 
WORKDIR /app
ENV PORT=8080
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]