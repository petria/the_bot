FROM amazoncorretto:25
VOLUME /tmp
ENV TZ=Europe/Helsinki

COPY target/*.jar app.jar
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

ENTRYPOINT ["/docker-entrypoint.sh"]
