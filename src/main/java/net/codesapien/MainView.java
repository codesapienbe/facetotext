package net.codesapien;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Route;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;

import java.io.File;
import java.util.List;

@Route
public class MainView extends VerticalLayout {

    public MainView(@Autowired OllamaChatModel chatModel) {

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout glassLayout = new VerticalLayout();
        glassLayout.addClassName("glass");
        glassLayout.setWidth("400px");

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

        sendButton.addClickListener(event -> {
            sendButton.setEnabled(false); // Prevent double clicks
            responseField.clear();

            try {
                // Load the image resource (adjust path if needed)
                ClassPathResource imageResource = new ClassPathResource(uploadPath.getValue());

                // Build the user message
                UserMessage userMessage = UserMessage.builder()
                        .text(promptField.getValue())
                        .media(List.of(new Media(MimeTypeUtils.IMAGE_PNG, imageResource)))
                        .build();


                // Call the model (Llava)
                ChatResponse response = chatModel.call(
                        new Prompt(userMessage, OllamaOptions.builder().model(OllamaModel.LLAVA).build())
                );

                // Set the response in the UI
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



        glassLayout.add(promptField, uploadPath, responseField, sendButton);
        add(glassLayout);
    }
}
