FROM openjdk:17-jdk-slim

ENV TZ=Asia/Seoul

RUN apt-get update && apt-get install -y telnet netcat

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

COPY src/main/resources/application.yml /config/application.yml
