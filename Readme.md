#Call on detect Nubomedia demonstrator

#Call on detect Nubomedia demonstrator

Prerequisites
-------------------------
To run properly this demo you need to have an installed and working instance of the following components:
* KMS (v6.5.0)
* The [nubomedia face detector plugin] installed on the KMS
* Asterisk or other PBX 

Installation instructions
-------------------------

Be sure to have installed [Java8] [Maven] and [Bower] in your system:

```bash
git clone https://github.com/nubomediaTI/timdetector-demonstrator.git
cd timdetector-demonstrator
```

##COD configuration

Get the application.properties template and make a copy:

```bash
cp call_on_detect/src/main/resources/application.properties .
```

Open it with your favourite text editor. You will see the following properties:
```
server.port: 8443                           #the port on which the server will listen
server.ssl.key-store: keystore.jks          #keystore file
server.ssl.key-store-password: kurento
server.ssl.keyStoreType: JKS
server.ssl.keyAlias: kurento-selfsigned

serverEvents.timer.milliseconds=6000        #interval from a face recognition event and another from the server

#stun server address
sip.stunServer=stun.l.google.com:19302
#asterisk host address
sip.host=showdemos2.ddns.net                #the host address of the PBX to which the client will connect

#interface from which take ip address (if more than one interface connected)
#sip.listenOnInterface=
```


```bash
mvn clean package -DskipTests
cd call_on_detect
java -jar target/call_on_detect-6.1.0.jar
```

Pay Attention: if you change the location of the jar file be sure that the file keystore.jks is in the same folder 
you launch the application.



[Bower]: http://bower.io
[Maven]: https://maven.apache.org/download.cgi
[Java8]: http://www.oracle.com/technetwork/java/javase/downloads/index-jsp-138363.html
[nubomedia face detector plugin]: http://nubomedia-vca.readthedocs.io/en/latest/face_detector.html
