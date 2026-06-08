FROM amazoncorretto:25
VOLUME /tmp
ENV TZ=Europe/Helsinki
COPY target/*.jar app.jar
ENTRYPOINT ["java","-jar","/app.jar"]
