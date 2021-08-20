FROM adoptopenjdk:11.0.11_9-jre-hotspot

RUN mkdir /app
WORKDIR /app

ADD build/sunflower*.jar /app/app.jar

ENTRYPOINT java -jar ./app.jar

EXPOSE 7010
EXPOSE 7000
EXPOSE 10004