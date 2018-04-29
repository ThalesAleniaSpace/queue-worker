FROM centos

ENV PATH="${PATH}:/usr/local/go/bin"
ADD maven/queue-worker.jar /opt/queue-worker.jar
ADD maven/go1.10.1.linux-amd64.tar.gz /usr/local/

ENV GOPATH="/root/go"

RUN yum install -y java-1.8.0-openjdk \
                   yum-utils \
                   device-mapper-persistent-data \
                   lvm2 &&\
    yum-config-manager --add-repo https://download.docker.com/linux/centos/docker-ce.repo &&\
    yum install -y docker-ce git &&\
    yum clean all

RUN go get -d -v github.com/alexellis/jaas &&\
    cd $GOPATH/src/github.com/alexellis/jaas &&\
    go install && chmod +x /opt/queue-worker.jar

ENV PATH="${PATH}:${GOPATH}/bin"

CMD ["java", "-jar", "/opt/queue-worker.jar"]