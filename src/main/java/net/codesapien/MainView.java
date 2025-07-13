package net.codesapien;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

        // Main container styling with glassmorphic effects
        setSizeFull();
        getStyle()
            .set("background", "rgba(255, 255, 255, 0.25)")
            .set("backdrop-filter", "blur(15px)")
            .set("border-radius", "16px")
            .set("border", "1px solid rgba(255,255,255,0.3)")
            .set("padding", "1rem");

        // Webcam video element (live preview)
        videoElement = new Element("video");
        videoElement.setAttribute("autoplay", true);
        videoElement.setAttribute("playsinline", true);
        videoElement.getStyle().set("width", "100%");
        videoElement.getStyle().set("height", "auto");
        videoElement.getStyle().set("border", "1px solid #ccc");

        // Add video to webcam container
        VerticalLayout webcamLayout = new VerticalLayout();
        webcamLayout.setPadding(false);
        webcamLayout.setSpacing(false);
        webcamLayout.setSizeFull();
        webcamLayout.getElement().appendChild(videoElement);
        webcamLayout.getElement().executeJs(
                "const video = this.querySelector('video');" +
                "navigator.mediaDevices.getUserMedia({video:true}).then(stream => {" +
                "  video.srcObject = stream;" +
                "});"
        );

        // Image preview with responsive settings and glass effect
        previewImage = new Image();
        previewImage.setAlt("Webcam preview");
        previewImage.setWidth("100%");
        previewImage.getStyle().set("height", "auto");
        previewImage.getStyle().set("border", "1px solid #ccc");
        previewImage.getStyle().set("background", "rgba(255, 255, 255, 0.15)");
        previewImage.getStyle().set("backdrop-filter", "blur(10px)");

        // Create overlay containers for webcam and preview
        Div webcamContainer = new Div();
        webcamContainer.getStyle()
            .set("position", "relative")
            .set("width", "95vw")
            .set("height", "95vh")
            .set("flex", "1 1 50%")
            .set("min-width", "300px");
        webcamContainer.addClassName("responsive-container");
        webcamContainer.add(webcamLayout);

        Div previewContainer = new Div();
        previewContainer.getStyle()
            .set("position", "relative")
            .set("width", "95vw")
            .set("height", "95vh")
            .set("flex", "1 1 50%")
            .set("min-width", "300px");
        previewContainer.addClassName("responsive-container");
        previewContainer.add(previewImage);

        // Create flex container with two equal areas & enable wrapping for responsiveness.
        // Added a custom class for media queries.
        HorizontalLayout flexLayout = new HorizontalLayout();
        flexLayout.addClassName("responsive-layout");
        flexLayout.setWidthFull();
        flexLayout.setHeightFull();
        flexLayout.setSpacing(false);
        flexLayout.getStyle()
            .set("display", "flex")
            .set("justify-content", "space-between")
            .set("flex-wrap", "wrap");
        flexLayout.addAndExpand(webcamContainer, previewContainer);

        // Capture button overlays the webcam container, sticking to the bottom center
        captureButton = new Button("Capture Photo", onCaptureEvent(webcamContainer));
        captureButton.getStyle()
            .set("position", "absolute")
            .set("bottom", "10px")
            .set("left", "50%")
            .set("transform", "translateX(-50%)");
        captureButton.addClassName("responsive-button");

        // Describe button overlays the preview container, sticking to the bottom center
        describeButton = new Button("Start Describing", onDescribeEvent(chatModel));
        describeButton.getStyle()
            .set("position", "absolute")
            .set("bottom", "10px")
            .set("left", "50%")
            .set("transform", "translateX(-50%)");
        describeButton.setEnabled(false);
        describeButton.addClassName("responsive-button");

        // Add buttons to their containers
        webcamContainer.getElement().appendChild(captureButton.getElement());
        previewContainer.getElement().appendChild(describeButton.getElement());

        // Inject custom CSS via JS for mobile devices, using only the current class.
        getUI().ifPresent(ui ->
            ui.getPage().executeJs(
                "var style = document.createElement('style');" +
                "style.innerHTML = '@media (max-width: 600px) {" +
                "  .responsive-layout { flex-direction: column !important; justify-content: flex-start !important; align-items: stretch !important; }" +
                "  .responsive-container { flex: 1 1 100% !important; height: 100% !important; }" +
                "  .responsive-button { position: static !important; transform: none !important; margin-top: 1rem !important; }" +
                "}' + " +
                "document.head.appendChild(style);"
            )
        );

        // Add the flex layout to the main view
        add(flexLayout);
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

    private @NotNull ComponentEventListener<ClickEvent<Button>> onCaptureEvent(Div webcamContainer) {
        return event -> {
            captureButton.setEnabled(false);
            describeButton.setEnabled(false);
            // Inject JS to perform a countdown overlay then capture the photo
            webcamContainer.getElement().executeJs(
                "var container = this;" +
                "container.style.position = 'relative';" +
                "var overlay = document.createElement('div');" +
                "overlay.style.position = 'absolute';" +
                "overlay.style.width = '100%';" +
                "overlay.style.height = '100%';" +
                "overlay.style.top = '0';" +
                "overlay.style.left = '0';" +
                "overlay.style.display = 'flex';" +
                "overlay.style.justifyContent = 'center';" +
                "overlay.style.alignItems = 'center';" +
                "overlay.style.fontSize = '5rem';" +
                "overlay.style.color = 'white';" +
                "overlay.style.textShadow = '0 0 10px black';" +
                "container.appendChild(overlay);" +
                "var count = 3;" +
                "var countdown = setInterval(function() {" +
                "   if(count > 0) {" +
                "       overlay.innerText = count;" +
                "   } else {" +
                "       clearInterval(countdown);" +
                "       overlay.remove();" +
                "       var video = container.querySelector('video');" +
                "       var canvas = document.createElement('canvas');" +
                "       canvas.width = video.videoWidth || 640;" +
                "       canvas.height = video.videoHeight || 480;" +
                "       canvas.getContext('2d').drawImage(video, 0, 0);" +
                "       var dataUrl = canvas.toDataURL('image/png');" +
                "       $0.$server.receivePhoto(dataUrl);" +
                "   }" +
                "   count--;" +
                "}, 1000);", this);
            Notification.show("Get ready...", 2000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        };
    }

    @ClientCallable
    public void receivePhoto(String dataUrl) {
        try {
            String prefix = "image/png;base64,";
            if (dataUrl != null && dataUrl.contains("base64,")) {
                int idx = dataUrl.indexOf("base64,") + "base64,".length();
                String base64 = dataUrl.substring(idx);
                capturedPhotoBytes = Base64.getDecoder().decode(base64);
                StreamResource resource = new StreamResource("preview.png", () -> new ByteArrayInputStream(capturedPhotoBytes));
                previewImage.setSrc(resource);
                Notification.show("Photo captured. You can now start describing.", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                describeButton.setEnabled(true);
            }
        } catch (Exception ex) {
            Notification.show("Error: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            captureButton.setEnabled(true);
        }
    }
}
