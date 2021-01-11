package com.authentic.util;

import org.hippoecm.hst.core.parameters.ValueListProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DropdownValueListProvider implements ValueListProvider {
    final private Map<String, String> values;

    public DropdownValueListProvider(String valueString) {
        final String[] kvpairs = valueString.split(";");
        values = new HashMap<>();
        for (String kvpair : kvpairs) {
            final String[] split = kvpair.split("=");
            if (split.length > 1) {
                values.put(split[0], split[1]);
            } else {
                values.put(kvpair, kvpair);
            }
        }
    }

    @Override
    public List<String> getValues() {
        return new ArrayList<>(values.keySet());
    }

    @Override
    public String getDisplayValue(String value) {
        return values.get(value);
    }

    @Override
    public String getDisplayValue(String value, Locale locale) {
        return values.get(value);
    }
}
