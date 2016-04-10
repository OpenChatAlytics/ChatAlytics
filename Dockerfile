FROM maven:3.3.3-jdk-8

ENV CHATALYTICSDIR /opt/chatalytics
ENV DATABASEDIR /mnt/

RUN mkdir -p ${CHATALYTICSDIR}
RUN mkdir -p ${DATABASEDIR}

COPY . ${CHATALYTICSDIR}
WORKDIR ${CHATALYTICSDIR}

# Build ChatAlytics

RUN mvn clean package -Dmaven.test.skip=true

RUN cp web/target/chatalytics-web-0.3-with-dependencies.jar ${CHATALYTICSDIR}
RUN cp compute/target/chatalytics-compute-0.3-with-dependencies.jar ${CHATALYTICSDIR}

# Run ChatAlytics
CMD ./bin/start-web-compute.sh
