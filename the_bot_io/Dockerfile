#FROM eclipse-temurin:17-jdk-alpine
FROM openjdk:21-jdk
VOLUME /tmp
ENV TZ=Europe/Helsinki
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]

