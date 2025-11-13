FROM maven:3.9.11-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
COPY src ./src
RUN mvn -B -DskipTests package \
    && ls -la target \
    && cp target/*.jar target/app.jar

FROM eclipse-temurin:25-jdk AS runtime
WORKDIR /app
COPY --from=build /workspace/target/app.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]