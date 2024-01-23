FROM openjdk:17
WORKDIR /app
COPY ./target/notifier-0.0.1-SNAPSHOT.jar /app

CMD ["java", "-jar", "notifier-0.0.1-SNAPSHOT.jar"]