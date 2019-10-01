FROM adoptopenjdk/openjdk11:alpine-slim
ARG BMRG_TAG
ENV JAVA_OPTS=""
ENV BMRG_HOME=/opt/boomerang
ENV BMRG_SVC=service-controller-$BMRG_TAG

WORKDIR $BMRG_HOME
ADD target/$BMRG_SVC.jar service.jar
RUN sh -c 'touch /service.jar'

RUN adduser -u 2000 -G root -D bmrguser \
&& chown -R 2000:0 $BMRG_HOME \
&& chmod -R u+x $BMRG_HOME/service.jar
USER 2000

EXPOSE 8080
ENTRYPOINT [ "sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar ./service.jar" ]
