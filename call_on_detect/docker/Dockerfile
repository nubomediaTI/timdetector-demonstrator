FROM nubomedia/apps-baseimage:src

MAINTAINER Nubomedia

ADD call_on_detect/keystore.jks /
ADD . /home/nubomedia

RUN sudo chown -R nubomedia /home/nubomedia
RUN cd /home/nubomedia && mvn compile

EXPOSE 8450

ENTRYPOINT cd /home/nubomedia/call_on_detect && mvn exec:java
