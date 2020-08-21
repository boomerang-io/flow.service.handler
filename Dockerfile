FROM adoptopenjdk/openjdk11-openj9:jdk-11.0.5_10_openj9-0.17.0-alpine-slim
ARG BMRG_TAG
ENV JAVA_OPTS=""
ENV BMRG_HOME=/opt/boomerang
ENV BMRG_SVC=service-controller-$BMRG_TAG

RUN apk del --purge --no-cache wget; \
    rm -rf /var/cache/apk/*;

WORKDIR $BMRG_HOME
ADD target/$BMRG_SVC.jar service.jar
RUN sh -c 'touch /service.jar'

RUN cp $JAVA_HOME/lib/security/cacerts .

# Create user, chown, and chmod. 
# OpenShift requires that a numeric user is used in the USER declaration instead of the user name
RUN chmod -R u+x $BMRG_HOME \
    && chgrp -R 0 $BMRG_HOME \
    && chmod -R g=u $BMRG_HOME
USER 2000

EXPOSE 8080
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar ./service.jar" ]
