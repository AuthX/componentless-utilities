package com.authentic.util;

import com.authentic.components.ComponentlessInfo;
import com.google.common.base.Strings;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.resourcebundle.ResourceBundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class ResourceBundleUtility {
    private static final Logger log = LoggerFactory.getLogger(ResourceBundleUtility.class);

    private ResourceBundleUtility() {}

    public static void addResourceBundlesToModel(final HstRequest request, final Info info) {
        final String ids = info.getResourceBundles();

        if (!Strings.isNullOrEmpty(ids)) {
            final Map<String, Map<String, String>> resourceBundles = getResourceBundles(ids, request.getLocale());
            resourceBundles.forEach(request::setModel);
        }
    }

    public static Map<String, Map<String, String>> getResourceBundles(final String ids, final Locale locale) {
        Map<String, Map<String, String>> bundles = new HashMap<>();
        Arrays.stream(ids.split(",")).forEach(id -> {
            Map<String, String> bundle = getResourceBundle(id, locale);
            if (bundle != null)
                bundles.put(id, bundle);
        });
        return bundles;
    }

    public static Map<String, String> getResourceBundle(final String id, final Locale locale) {
        try {
            ResourceBundle bundle = ResourceBundleUtils.getBundle(id, locale, false);
            Map<String, String> bundleValues = new HashMap<>();
            bundle.keySet().forEach(key -> bundleValues.put(key, bundle.getString(key)));
            return bundleValues;
        } catch (MissingResourceException e) {
            log.warn("Attempt to get missing resource bundle {}", id, e);
        }
        return null;
    }

    public interface Info {
        @Parameter(name = "resourceBundles", displayName = "resourceBundles")
        String getResourceBundles();
    }
}
