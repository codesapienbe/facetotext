package net.codesapien;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public class Notifier {

    private Notifier() {
        // Private constructor to prevent instantiation
    }

    public static final String NOTIFICATION_CLASS = "glass-notification";

    /**
     * Displays an error notification with the given message.
     *
     * @param message The message to display in the notification.
     */
    public static void showError(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.addClassName(NOTIFICATION_CLASS);
    }

    /**
     * Displays an informational notification with the given message.
     *
     * @param message The message to display in the notification.
     */
    public static void showInfo(String message) {
        Notification notification = Notification.show(message, 3000, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
    }
}

