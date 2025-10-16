#FROM eclipse-temurin:17-jdk-alpine

#FROM openjdk:21-jdk
#VOLUME /tmp
#ENV TZ=Europe/Helsinki
#COPY target/*.jar app.jar
#ENTRYPOINT ["java", "-Xmx6000M", "-jar","/app.jar"]

FROM openjdk:21-jdk-slim
VOLUME /tmp
ENV TZ=Europe/Helsinki

#WORKDIR /app

# Copy the application JAR and the certificate
COPY target/*.jar app.jar
COPY ymparisto.crt ymparisto.crt

# Import the certificate into the default Java truststore
RUN keytool -importcert \
    -trustcacerts \
    -alias ymparisto \
    -file ymparisto.crt \
    -keystore /usr/local/openjdk-21/lib/security/cacerts \
    -storepass changeit \
    -noprompt

# Create non-root user (optional but recommended)
#RUN useradd -ms /bin/bash appuser
#USER appuser

#EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app.jar"]
