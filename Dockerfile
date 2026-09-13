FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app
COPY gradlew .
COPY gradle/ gradle/
COPY build.gradle .
COPY settings.gradle .

RUN chmod +x gradlew

RUN ./gradlew dependencies --no-daemon || true

COPY src/ src/

RUN ./gradlew bootJar -x test --no-daemon


FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/build/libs/*.jar app.jar

RUN chown spring:spring app.jar

USER spring

EXPOSE 8086

ENV JAVA_OPTS="-Xms256m -Xmx512m"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]