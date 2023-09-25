FROM openjdk:17-jdk-slim

ENV TZ=Asia/Seoul

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

COPY src/main/resources/application.yml /config/application.yml

CMD ["java", "-jar", "app.jar", "--spring.config.location=file:/config/application.yml"]