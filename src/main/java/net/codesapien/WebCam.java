package net.codesapien;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.component.ClientCallable;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.function.Consumer;

public class WebCam extends Composite<VerticalLayout> {

    private final Button captureButton = new Button("Capture");
    private final Image photoPreview = new Image();
    private final Video videoPreview = new Video();

    private byte[] photoBytes;
    private byte[] videoBytes;

    private Consumer<byte[]> onPhotoCaptured;
    private Consumer<byte[]> onVideoRecorded;

    public WebCam() {
        VerticalLayout layout = getContent();
        layout.setSpacing(true);
        layout.setPadding(true);

        photoPreview.setAlt("Photo preview");
        photoPreview.setWidth("320px");
        photoPreview.setHeight("240px");

        videoPreview.setWidth("320px");
        videoPreview.setHeight("240px");
        videoPreview.setAutoplay(true);
        videoPreview.setControls(true);

        layout.add(captureButton, photoPreview, videoPreview);

        videoPreview.setVisible(false);

        // JavaScript for click/hold logic
        captureButton.getElement().executeJs(
                "var button = this;" +
                        "var pressTimer;" +
                        "var isRecording = false;" +
                        "button.addEventListener('mousedown', function(e) {" +
                        "  pressTimer = window.setTimeout(function() {" +
                        "    isRecording = true;" +
                        "    button.$server.startRecording();" +
                        "  }, 2000);" +
                        "});" +
                        "button.addEventListener('mouseup', function(e) {" +
                        "  clearTimeout(pressTimer);" +
                        "  if (isRecording) {" +
                        "    isRecording = false;" +
                        "    button.$server.stopRecording();" +
                        "  } else {" +
                        "    button.$server.capturePhoto();" +
                        "  }" +
                        "});" +
                        "button.addEventListener('mouseleave', function(e) {" +
                        "  clearTimeout(pressTimer);" +
                        "  if (isRecording) {" +
                        "    isRecording = false;" +
                        "    button.$server.stopRecording();" +
                        "  }" +
                        "});"
        );
    }

    public void setOnPhotoCaptured(Consumer<byte[]> onPhotoCaptured) {
        this.onPhotoCaptured = onPhotoCaptured;
    }

    public void setOnVideoRecorded(Consumer<byte[]> onVideoRecorded) {
        this.onVideoRecorded = onVideoRecorded;
    }

    @ClientCallable
    public void capturePhoto() {
        getElement().executeJs(
                "navigator.mediaDevices.getUserMedia({video:true}).then(stream => {" +
                        "  const video = document.createElement('video');" +
                        "  video.srcObject = stream;" +
                        "  video.play();" +
                        "  video.onloadedmetadata = () => {" +
                        "    const canvas = document.createElement('canvas');" +
                        "    canvas.width = video.videoWidth;" +
                        "    canvas.height = video.videoHeight;" +
                        "    canvas.getContext('2d').drawImage(video, 0, 0);" +
                        "    stream.getTracks().forEach(track => track.stop());" +
                        "    const dataUrl = canvas.toDataURL('image/png');" +
                        "    $0.$server.receivePhoto(dataUrl);" +
                        "  };" +
                        "});", getElement()
        );
    }

    @ClientCallable
    public void startRecording() {
        getElement().executeJs(
                "navigator.mediaDevices.getUserMedia({video:true, audio:true}).then(stream => {" +
                        "  window.mediaRecorder = new MediaRecorder(stream);" +
                        "  window.chunks = [];" +
                        "  window.mediaRecorder.ondataavailable = e => window.chunks.push(e.data);" +
                        "  window.mediaRecorder.onstop = e => {" +
                        "    const blob = new Blob(window.chunks, {type: 'video/webm'});" +
                        "    const reader = new FileReader();" +
                        "    reader.onloadend = () => {" +
                        "      const base64data = reader.result;" +
                        "      $0.$server.receiveVideo(base64data);" +
                        "    };" +
                        "    reader.readAsDataURL(blob);" +
                        "  };" +
                        "  window.mediaRecorder.start();" +
                        "  window.stream = stream;" +
                        "});", getElement()
        );
    }

    @ClientCallable
    public void stopRecording() {
        getElement().executeJs(
                "if(window.mediaRecorder && window.mediaRecorder.state !== 'inactive') {" +
                        "  window.mediaRecorder.stop();" +
                        "  window.stream.getTracks().forEach(track => track.stop());" +
                        "}"
        );
    }

    @ClientCallable
    public void receivePhoto(String dataUrl) {
        if (dataUrl != null && dataUrl.startsWith("image/png;base64,")) {
            String base64 = dataUrl.substring("image/png;base64,".length());
            photoBytes = Base64.getDecoder().decode(base64);
            StreamResource resource = new StreamResource("photo.png", () -> new ByteArrayInputStream(photoBytes));
            photoPreview.setSrc(resource);
            videoPreview.setVisible(false);
            photoPreview.setVisible(true);
            if (onPhotoCaptured != null) {
                onPhotoCaptured.accept(photoBytes);
            }
        }
    }

    @ClientCallable
    public void receiveVideo(String dataUrl) {
        if (dataUrl != null && dataUrl.startsWith("video/webm;base64,")) {
            String base64 = dataUrl.substring("video/webm;base64,".length());
            videoBytes = Base64.getDecoder().decode(base64);
            StreamResource resource = new StreamResource("video.webm", () -> new ByteArrayInputStream(videoBytes));
            videoPreview.setSrc(resource);
            videoPreview.setVisible(true);
            photoPreview.setVisible(false);
            if (onVideoRecorded != null) {
                onVideoRecorded.accept(videoBytes);
            }
        }
    }

    public byte[] getPhotoBytes() {
        return photoBytes;
    }

    public byte[] getVideoBytes() {
        return videoBytes;
    }
}
