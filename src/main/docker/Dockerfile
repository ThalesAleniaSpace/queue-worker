FROM centos

ADD maven/queue-worker.jar /opt/queue-worker.jar

RUN yum install -y java-1.8.0-openjdk \
                   yum-utils \
                   device-mapper-persistent-data \
                   lvm2 &&\
    yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo &&\
    yum install -y docker-ce git &&\
    yum clean all

RUN chmod +x /opt/queue-worker.jar

CMD ["java", "-jar", "/opt/queue-worker.jar"]
