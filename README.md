# NBU AppZone server

## Setting up local dev environment
Install [vagrant](http://vagrantup.com/)

    vagrant box add lucid32 http://files.vagrantup.com/lucid32.box
    vagrant init lucid32
    # in Vagrantfile add line (search for config.vm.forward_port)
    # config.vm.forward_port 8080, 8080
    vagrant up

    vagrant ssh
    
    # now in the vagrant box
    sudo ./postinstall.sh
    
    # scalatra
    wget http://apt.typesafe.com/repo-deb-build-0002.deb
    sudo dpkg -i repo-deb-build-0002.deb
    sudo apt-get update
    sudo apt-get install typesafe-stack
    
    # mongodb
    sudo apt-key adv --keyserver keyserver.ubuntu.com --recv 7F0CEB10
    echo "deb http://downloads-distro.mongodb.org/repo/ubuntu-upstart dist 10gen" > 10gen.list
    mv 10gen.list /etc/apt/sources.list.d/
    sudo apt-get update
    sudo apt-get install mongodb-10gen

## Build & run
    cd /vagrant
    ./sbt
    > container:start

Now open the site's [root page](http://localhost:8080/) in your browser.