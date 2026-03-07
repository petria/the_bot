FROM amazoncorretto:25
VOLUME /tmp
ENV TZ=Europe/Helsinki

# Copy the application JAR and the certificate
COPY target/*.jar app.jar
COPY ymparisto.crt ymparisto.crt

# Import the certificate into the default Java truststore
RUN keytool -importcert \
    -trustcacerts \
    -alias ymparisto \
    -file ymparisto.crt \
    -keystore /usr/lib/jvm/java-25-amazon-corretto/lib/security/cacerts \
    -storepass changeit \
    -noprompt


ENTRYPOINT ["java", "-jar", "/app.jar"]
