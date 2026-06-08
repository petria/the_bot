FROM amazoncorretto:26
VOLUME /tmp
ENV TZ=Europe/Helsinki
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
