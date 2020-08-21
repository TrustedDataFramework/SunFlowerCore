FROM registry.cn-hangzhou.aliyuncs.com/wisdomchain/server_jre8

RUN mkdir /app
WORKDIR /app

ADD build/sunflower*.jar /app/app.jar

ENTRYPOINT java -jar ./app.jar

EXPOSE 7010
EXPOSE 7000
EXPOSE 10004