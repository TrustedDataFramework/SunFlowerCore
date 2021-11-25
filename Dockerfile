FROM adoptopenjdk/openjdk11:jre-nightly

WORKDIR /app
ADD sunflower-core/build/libs/sunflower*.jar /app/app.jar

ENTRYPOINT java
CMD ["-jar", "app.jar"]
