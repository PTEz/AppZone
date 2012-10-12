# API Documentation

## GET /apps
    [{
      "id": "test",
      "name": "Test Application"
    },{
      "id": "nbugap",
      "name": "NBUGap for iOS/Android"
    }]

## GET /app/:id
    {
      "id": "test",
      "name": "Test Application"
    }

## POST /app
Possible parameters:

* type: web, ios or android
* stage: stable, develop

Optional:

* apk: if android, send the apk file
* api: if ios, send the .api file
* web: if web, send the url of the webapp

Response:

    {
      "id": "test",
      "name": "Test Application"
    }