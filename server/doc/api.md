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
    
## GET /app/:id/android
Returns the .apk for android

## POST /app/:id/android
Parameters:

* apk: the android apk file
* version: the string representation of current version

## GET /app/:id/ios
forwards to the correct ```itms-services://?action=download-manifest?url=...``` 
url.

## POST /app/:id/ios
Parameters:

* ipa: the ipa file
* manifest: the manifest file
* version: the string representation of current version

## GET /app/:id/feedback
Returns the feedback for given app

## POST /app/:id/android/feedback
Post a feedback for the android app with given id

## POST /app/:id/ios/feedback
Post a feedback for the ios app with given id