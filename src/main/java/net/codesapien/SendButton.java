package net.codesapien;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

class SendButton extends Button {
    
    private static final String DEFAULT_LABEL = "Send";
    SendButton(String label, ComponentEventListener<ClickEvent<Button>> listener) {
        super(label, listener);
    }
    
    SendButton(String label) {
        super(label);
    }
    
    SendButton(String label, Component icon, ComponentEventListener<ClickEvent<Button>> listener) {
        super(label, icon, listener);
    }
}
