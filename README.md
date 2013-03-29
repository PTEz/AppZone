# NBU AppZone server

## Description
AppZone provides a simple interface for publishing Android and iOS apps through a simple web interface. Apps can be published on AppZone using simple REST commands or with the jenkins plugin.

For an overview look at [overview.png](overview.png).

## Requirements

* mongodb

## Simple build & run everything
    ./run.sh start

Open your browser at [http://localhost:8080](http://localhost:8080).

To configure domain, ports and mongodb edit run.properties.

## Run the nicer way

### Run API
First you will need a tomcat or jetty server running. The API

    cd server-api/
    ./sbt package
    cp target/.../...war <your_tomcat>/webapps/appzone.war
    
To configure mongodb, either edit the source, or create a default.props file in src/main/resources/props .

### Run WEB
Edit the SERVER variable at the top of server-web/js/appzone.js to point to your running API. Then serve server-web as a static website. Preferably through nginx. Otherwise you can run:

	cd server-web/
	python -m SimpleHTTPServer
	# or: sudo python -m SimpleHTTPServer 80

## Build & run in sbt
    cd server-api/
    ./sbt
    > container:start
    > ~ ;copy-resources;aux-compile

Now open the site's [root page](http://localhost:8080/) in your browser.

## Jenkins plugin
Running the following command creates a .hpi file, that can be installed in jenkins in the plugin manager (advanced tab).

    cd jenkins-plugin
    mvn package