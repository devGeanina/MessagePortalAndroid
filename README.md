

## MessagePortal for Android
SMS Gateway application allowing users to send SMS messages through its server listening on a particular address and port. The Android application is part of a multi-device SMS gateway service that can be used in conjucture with its twin application for desktop (Windows only supported). The twin desktop application allows the user to send the desired SMS through its interface instead of using a browser to make the calls, and allows the user to have more options using it. However, this application can be used standalone as well, by making the calls using a regular browser and the URI parameters mentioned in the application info, such as the authentication key, receiving SMS number and the SMS body.

## Pre-requisites
Android version supported is min 26.
The Android device used needs to have access to Internet (mobile or wifi). Write down the device's IP generated by your mobile/wifi provider that will be used to make the browser calls or for the settings of the twin desktop application.

## Getting Started
* Import the project into Android Studio or IntelliJ and run on one of the supported platforms.
* Provide Internet access for the device used and turn on the server on the main page of the application. Happy texting!

### Settings configuration
The application allows the user to change the following settings:
* Port and server address to listen on for the incoming SMS to send further.
* Setting the invoice day and the number of SMS to send until the next invoice day, based on each user's provider and monthly subscription, or if there is no limit to sending the SMS messages.
* Play a ringtone sound or not when sending the SMS.
* The application contains information about the number of SMS left (in case the "Unlimited" option is off), the URI format for the http requests to the server, as well as the generated authentication key needed when creating the URIs to ensure extra security when using the application.
* For an Android device that is dual SIM the interface of the application also allows the user to select which SIM to use for forwarding the SMS. 
 

## Built With
* NanoHTTPD - light-weight HTTP server listening for the incoming requests.
* SQLiteOpenHelper - light-weight database to store the application information.
* Gson - used to parse information.
* Material icons - to set the menu and recycler view icons.
* Butterknife - to bind Android views.
* JUnit & Mockito - test coverage.

## Author
* **GeaninaC**


https://user-images.githubusercontent.com/35954631/115345927-29aa1980-a1b8-11eb-8045-82ddb60e1ec9.mp4




