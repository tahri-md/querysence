FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar /app/app.jar
EXPOSE 8081
ENV PORT=8081
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
