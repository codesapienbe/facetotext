package net.codesapien;

import com.vaadin.flow.i18n.I18NProvider;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class I18nProviderImpl implements I18NProvider {
    public static final String BUNDLE_PREFIX = "messages";
    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(Locale.ENGLISH, Locale.of("nl", "BE"), Locale.of("tr"));
    }
    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        ResourceBundle bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale);
        return bundle.getString(key);
    }
}
