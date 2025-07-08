package net.codesapien;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.icon.VaadinIcon;

class SendButton extends Button {
    
    private static final String DEFAULT_LABEL = "Send";
    private static final Component DEFAULT_ICON = VaadinIcon.BUTTON.create();
    private static final String DEFAULT_CLASS_NAME = "glass";   
    
    SendButton(String label, ComponentEventListener<ClickEvent<Button>> listener) {
        super(label, listener);
        addClassName(DEFAULT_CLASS_NAME);
    }
    
    SendButton(String label) {
        super(label);
        addClassName(DEFAULT_CLASS_NAME);
    }
    
    SendButton(String label, Component icon, ComponentEventListener<ClickEvent<Button>> listener) {
        super(label, icon, listener);
        addClassName(DEFAULT_CLASS_NAME);
    }
}
