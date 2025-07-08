package net.codesapien;

import com.vaadin.flow.component.textfield.TextField;

public class PromptField extends TextField {
    public PromptField(String label, String placeholder) {
        setLabel(label);
        setPlaceholder(placeholder);
        setWidthFull();
    }
}
