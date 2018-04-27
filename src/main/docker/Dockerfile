FROM openjdk:jre-alpine

#OSS/BSS Api
ADD maven/queue-worker.jar /opt/queue-worker.jar

RUN chmod +x /opt/queue-worker.jar

CMD ["java", "-jar", "/opt/queue-worker.jar"]