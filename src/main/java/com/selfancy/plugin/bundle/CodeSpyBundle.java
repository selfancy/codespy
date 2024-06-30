package com.selfancy.plugin.bundle;

import com.intellij.DynamicBundle;

import java.util.*;

/**
 * {@link CodeSpyBundle}
 * <p>
 * Created by mike on 2024/05/18
 *
 * @see DynamicBundle
 */
public final class CodeSpyBundle extends DynamicBundle {
    public static final String BUNDLE = "CodeSpyBundle";
    private static final CodeSpyBundle INSTANCE = new CodeSpyBundle();
    private static final String OPTIONS_KEY_PREFIX = "decompiler.options";

    private CodeSpyBundle() {
        super(BUNDLE);
    }

    public static String message(String key, Object... params) {
        return INSTANCE.getMessage(key, params);
    }

    public static Map<String, String> options() {
        Map<String, String> options = new TreeMap<>(Comparator.naturalOrder());
        ResourceBundle bundle = INSTANCE.getResourceBundle();
        Enumeration<String> keys = bundle.getKeys();
        while(keys.hasMoreElements()) {
            String fullKey = keys.nextElement();
            if(fullKey.startsWith(OPTIONS_KEY_PREFIX)) {
                String value = bundle.getString(fullKey);
                String key = fullKey.substring(OPTIONS_KEY_PREFIX.length() + 1);
                if (!key.isBlank() && !value.isBlank()) {
                    options.put(key, value);
                }
            }
        }
        return options;
    }
}
