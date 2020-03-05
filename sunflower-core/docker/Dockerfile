FROM wisdomchain/server_jre8
COPY build/sunflower*.jar /app.jar

ENTRYPOINT ["usr/bin/env", "java", "-jar", "/app.jar" ]

EXPOSE 8080
EXPOSE 8081