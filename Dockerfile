FROM nubomedia/apps-baseimage:v1

MAINTAINER Nubomedia

ADD call_on_detect/keystore.jks /
ADD . /home/nubomedia

RUN cd /home/nubomedia && mvn compile


ENTRYPOINT cd /home/nubomedia/call_on_detect && mvn exec:java
