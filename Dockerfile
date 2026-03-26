FROM maven:3.9.6-openjdk-25-slim AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM openjdk:25-slim
WORKDIR /app
ENV PORT=8080
ENV JAVA_OPTS="-Djava.security.egd=file:/dev/./urandom"
COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]