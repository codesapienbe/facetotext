package net.codesapien;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.MimeTypeUtils;

import java.io.ByteArrayInputStream;
import java.util.Base64;
import java.util.List;

@Route
public class MainView extends VerticalLayout {

    private static final String PROMPT = "Describe what you see in the picture?";

    private byte[] capturedPhotoBytes = null;
    private final Image previewImage;
    private final Button captureButton;
    private final Button describeButton;
    private final Element videoElement;
    private final OllamaChatModel chatModel;

    public MainView(@Autowired OllamaChatModel chatModel) {
        this.chatModel = chatModel;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Webcam video element (live preview)
        videoElement = new Element("video");
        videoElement.setAttribute("autoplay", true);
        videoElement.setAttribute("playsinline", true);
        videoElement.getStyle().set("width", "640px");
        videoElement.getStyle().set("height", "480px");
        videoElement.getStyle().set("border", "1px solid #ccc");

        // Add video to a Vaadin container for JS interop
        VerticalLayout webcamLayout = new VerticalLayout();
        webcamLayout.setPadding(false);
        webcamLayout.setSpacing(false);
        webcamLayout.setSizeFull();
        webcamLayout.getElement().appendChild(videoElement);

        // Start webcam when attached
        webcamLayout.getElement().executeJs(
                "const video = this.querySelector('video');" +
                        "navigator.mediaDevices.getUserMedia({video:true}).then(stream => {" +
                        "  video.srcObject = stream;" +
                        "});"
        );

        // Image preview
        previewImage = new Image();
        previewImage.setAlt("Webcam preview");
        previewImage.setWidth("640px");
        previewImage.setHeight("480px");
        previewImage.getStyle().set("border", "1px solid #ccc");

        // Layout split: left (webcam), right (captured image)
        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setWidthFull();
        splitLayout.setHeight(500, Unit.PIXELS);
        splitLayout.addToPrimary(webcamLayout);
        splitLayout.addToSecondary(previewImage);
        splitLayout.setSplitterPosition(50);

        // Step 1: Button to capture photo
        captureButton = new Button("Capture Photo", onCaptureEvent(webcamLayout));

        // Step 2: Button to describe, enabled only after photo is captured
        describeButton = new Button("Start Describing", onDescribeEvent(chatModel));
        describeButton.setEnabled(false);

        add(splitLayout, captureButton, describeButton);
    }

    private @NotNull ComponentEventListener<ClickEvent<Button>> onDescribeEvent(OllamaChatModel chatModel) {
        return event -> {
            describeButton.setEnabled(false);
            if (capturedPhotoBytes == null) {
                Notification.show("Please capture a photo first.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                captureButton.setEnabled(true);
                return;
            }
            try {
                Media media = Media.builder()
                        .mimeType(MimeTypeUtils.IMAGE_PNG)
                        .data(capturedPhotoBytes)
                        .build();

                UserMessage userMessage = UserMessage.builder()
                        .text(PROMPT)
                        .media(List.of(media))
                        .build();

                ChatResponse response = chatModel.call(
                        new Prompt(userMessage, OllamaOptions.builder().model(OllamaModel.LLAVA).build())
                );

                String resultText = response.getResult().getOutput().getText();
                Notification.show(resultText, 8000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_PRIMARY);

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                captureButton.setEnabled(true);
            }
        };
    }

    private @NotNull ComponentEventListener<ClickEvent<Button>> onCaptureEvent(VerticalLayout webcamLayout) {
        return event -> {
            captureButton.setEnabled(false);
            describeButton.setEnabled(false);
            // Capture photo from webcam using JS interop
            webcamLayout.getElement().executeJs(
                    "const video = this.querySelector('video');" +
                            "const canvas = document.createElement('canvas');" +
                            "canvas.width = video.videoWidth || 640;" +
                            "canvas.height = video.videoHeight || 480;" +
                            "canvas.getContext('2d').drawImage(video, 0, 0);" +
                            "const dataUrl = canvas.toDataURL('image/png');" +
                            "$0.$server.receivePhoto(dataUrl);",
                    this // $0 refers to this Java component (MainView)
            );
            Notification.show("Capturing photo...", 2000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        };
    }

    @ClientCallable
    public void receivePhoto(String dataUrl) {
        try {
            // Accept both "image/png;base64," and "image/png;base64," as prefix
            String prefix = "image/png;base64,";
            if (dataUrl != null && dataUrl.contains("base64,")) {
                int idx = dataUrl.indexOf("base64,") + "base64,".length();
                String base64 = dataUrl.substring(idx);
                capturedPhotoBytes = Base64.getDecoder().decode(base64);
                StreamResource resource = new StreamResource("preview.png", () -> new ByteArrayInputStream(capturedPhotoBytes));
                previewImage.setSrc(resource);
                Notification.show("Photo captured. You can now start describing.", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                // Enable the describe button
                describeButton.setEnabled(true);
            }
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            captureButton.setEnabled(true);
        }
    }
}
