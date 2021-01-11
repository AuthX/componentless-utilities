package com.authentic.components;

import com.google.common.base.Strings;
import org.slf4j.Logger;

import java.util.Map;

public interface ComponentlessInfo {
    public Logger getLog();
    public Map<String, String> getMap();

    default boolean getBoolParameter(final String name, final boolean fallback) {
        final String param = getStringParameter(name);
        if (Strings.isNullOrEmpty(param))
            return fallback;
        return param.equals("on") || param.equals("true");
    }

    default Integer getIntParameter(final String name) {
        final String param = getStringParameter(name);
        if (Strings.isNullOrEmpty(param))
            return null;

        try {
            return Integer.parseInt(param);
        } catch (NumberFormatException e) {
            getLog().warn("Error parsing int parameter", e);
            return null;
        }
    }

    default Integer getIntParameter(final String name, final Integer fallback) {
        final Integer result = getIntParameter(name);
        return result != null ? result : fallback;
    }

    default String getStringParameter(final String name, final String fallback) {
        final String result = getStringParameter(name);
        return result != null ? result : fallback;
    }

    default String getStringParameter(final String name) {
        return getMap().get(name);
    }
}
