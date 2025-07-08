# FaceToText

> Vaadin Flow & Spring Boot Vision App

This application lets you capture a live photo using your webcam and get an AI-powered description of what’s in the picture.

## Features

- \*Live Webcam Preview\*: See your webcam feed directly in the browser.
- \*Photo Capture\*: Click the \`Capture Photo\` button to take a snapshot.
- \*Image Description\*: After capturing, click \`Start Describing\` to send your snapshot to an AI model that describes the image.
- \*Real-Time Notifications\*: Get immediate feedback and results through on-screen notifications.

## How It Works

1. **Live Stream**  
   When you open the app, your browser will ask for permission to access the webcam. Once allowed, you will see a video preview of your feed.

2. **Capture Photo**  
   Click the \`Capture Photo\` button to capture a still image from the live feed. The captured photo will appear on the right side of the screen.

3. **Generate Description**  
   With a photo captured, click the \`Start Describing\` button. The app sends the image to an AI chat model to generate a description of the picture. The description is then displayed as a notification.

## Running the Application

- Import the project as a Maven project.
- Run the application using:
  \`\`\`
  mvn spring-boot:run
  \`\`\`
- Open [http://localhost:8080/](http://localhost:8080/) in your browser.

## Requirements

- A working webcam.
- Browser permissions to access the webcam.
- Internet connection if additional AI service dependencies are used.

Enjoy capturing memories and discovering what the AI sees in your photos!