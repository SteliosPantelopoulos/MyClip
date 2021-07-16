# MyClip

MyClip is an Android application written in Java that exchanges videos between two users on the standards of well-known social media (Instagram, TikTok).
It is an application in which every user can create a channel, upload videos, subscribe to other channels and hashtags and see
videos that contain topics to which they are subscribed.

The application has 3 main tabs:

- The first one called "Subscriptions" where all the videos of the subscribed topics are shown.

  ![Subscriptions Tab](/assetsREADME/subscriptionsTab.png)

- The second one called "Search" where the user can search other channels or hashtags.

  ![Search Tab](/assetsREADME/searchTab.png)

- The third one called "Upload" where the user can select to record a video or choose one from the gallery and then the app will redirect him in the appropriate android activity.

  ![Upload Tab](/assetsREADME/uploadTab.png)

## Design Details

This application is a distributed system based on the idea of Node.
From the Server Side there are Brokers who are responsible for storing Topics (Hashtags and Channels)
as well as for the distribution of videos to subscribers.
From the Client Side there are Consumers & Publishers where the former are responsible for viewing videos of subscriptions while the latter for promoting a new video
to Brokers.

When uploading the video, the Publisher connects to a random Broker and asks for the right Broker based on the hashed value of the topic he wants to post. He is then redirected
to the right Broker where he sends the video metadata.
Similarly, when deleting a video, the same procedure is followed.
When a consumer opens the tab with his subscriptions, he connects with the right Brokers and send them a request to receive the videos that contain topics in which he is subscribed.
He then receives the video metadata, saves it on the device and plays the video in the application.

Distributed system is implemented with the use of ServerSockets and SHA-1 Hash Function.

## How to setup the application

- Run 3 times the Server.java file giving for each Broker the values: 
  - 1, 127.0.0.1, 1500
  - 2, 127.0.0.1, 2000
  - 3, 127.0.0.1, 2500
  
- Run the application in a simulator.
