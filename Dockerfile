FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY . .
RUN chmod +x mvnw
RUN ./mvnw -DskipTests package
RUN JAR_FILE=$(find target -maxdepth 1 -name "*.jar" ! -name "*.original" | head -n 1) && \
    cp "$JAR_FILE" app.jar

FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /workspace/app.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]