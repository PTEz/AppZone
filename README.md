# NBU AppZone server

## Requirements

* mongodb

##Install in tomcat

    cd server
    ./sbt package
    cp target/.../...war <your_tomcat>/webapps/appzone.war

## Build & run
    cd server
    ./sbt
    > container:start
    > ~ ;copy-resources;aux-compile

Now open the site's [root page](http://localhost:8080/) in your browser.

## Jenkins plugin
Running the following command creates a .hpi file, that can be installed in jenkins in the plugin manager (advanced tab).

    cd jenkins-android-plugin
    mvn package