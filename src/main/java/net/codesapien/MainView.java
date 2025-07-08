package net.codesapien;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@Route
public class MainView extends HorizontalLayout {

    private static final String TRANSLATIONS_BUNDLE = "translations.messages";

    private final Image previewImage = new Image();

    public MainView(@Autowired OllamaChatModel chatModel) {
        setSizeFull();

        // Create form fields
        PromptField promptField = new PromptField(
                getTranslation("prompt.label"),
                getTranslation("prompt.placeholder")
        );

        PromptField uploadPath = new PromptField(
                getTranslation("upload.label"),
                getTranslation("upload.placeholder")
        );
        uploadPath.setValue("uploads" + File.separator + "files");
        uploadPath.setHelperText(getTranslation("upload.helper"));

        PromptField responseField = new PromptField(
                getTranslation("response.label"),
                getTranslation("response.placeholder")
        );
        responseField.setReadOnly(true);

        Button sendButton = new SendButton(getTranslation("send.label"));
        sendButton.setEnabled(false);

        promptField.addValueChangeListener(event -> {
            sendButton.setEnabled(!event.getValue().trim().isEmpty());
        });

        // Initialize preview image using default properties
        previewImage.setAlt(getTranslation("preview.image.alt"));

        sendButton.addClickListener(event -> {
            sendButton.setEnabled(false); // Prevent double clicks
            responseField.clear();

            try {
                ClassPathResource imageResource = new ClassPathResource(uploadPath.getValue());
                StreamResource resource = new StreamResource("preview.png", () -> {
                    try {
                        return imageResource.getInputStream();
                    } catch (Exception ex) {
                        Notification.show(getTranslation("image.load.error") + ": " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return null;
                    }
                });
                previewImage.setSrc(resource);

                UserMessage userMessage = UserMessage.builder()
                        .text(promptField.getValue())
                        .media(List.of(new Media(MimeTypeUtils.IMAGE_PNG, imageResource)))
                        .build();

                ChatResponse response = chatModel.call(
                        new Prompt(userMessage, OllamaOptions.builder().model(OllamaModel.LLAVA).build())
                );

                String resultText = response.getResult().getOutput().getText();
                responseField.setValue(resultText);

                Notification.show(getTranslation("response.success"), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show(getTranslation("response.error") + ": " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            } finally {
                sendButton.setEnabled(true);
            }
        });

        // Create left column (form column)
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.add(promptField, uploadPath, responseField, sendButton);
        formLayout.setFlexGrow(1, promptField, uploadPath, sendButton);
        formLayout.setFlexGrow(4, responseField);

        // Create right column (image preview column)
        VerticalLayout imageLayout = new VerticalLayout(previewImage);

        // Add columns to the layout using default styling
        add(formLayout, imageLayout);
        setFlexGrow(1, formLayout);
        setFlexGrow(1, imageLayout);
    }

    private String getTranslation(String key) {
        ResourceBundle bundle;
        try {
            Locale locale = getLocale();
            bundle = ResourceBundle.getBundle(TRANSLATIONS_BUNDLE, locale);
            if (!bundle.containsKey(key)) {
                bundle = ResourceBundle.getBundle(TRANSLATIONS_BUNDLE, Locale.ENGLISH);
            }
        } catch (Exception e) {
            Notification.show("Error loading translations: " + e.getMessage(), 5000, Notification.Position.TOP_CENTER);
            bundle = ResourceBundle.getBundle(TRANSLATIONS_BUNDLE, Locale.ENGLISH);
        }
        return bundle.getString(key);
    }
}