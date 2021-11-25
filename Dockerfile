FROM adoptopenjdk/openjdk11:jre-nightly

WORKDIR /app
ADD sunflower-core/build/libs/sunflower*.jar /app/app.jar

ENV SUNFLOWER_DATABASE_DIRECTORY /var/lib/psc
ENTRYPOINT java
CMD ["-jar", "app.jar"]
