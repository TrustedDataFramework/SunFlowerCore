FROM adoptopenjdk/openjdk11:jre-nightly

WORKDIR /app
ADD sunflower-core/build/libs/sunflower*.jar /app/app.jar
ADD sunflower-core/src/main/resources/code.json /app/code.json
ADD sunflower-core/src/main/resources/abi.json /app/abi.json
RUN mkdir -p /var/lib/psc
ENV SUNFLOWER_DATABASE_DIRECTORY /var/lib/psc
ENV SUNFLOWER_CONSENSUS_ABI /app/abi.json
ENV SUNFLOWER_CONSENSUS_CODE /app/code.json
CMD ["java", "-jar", "app.jar"]
