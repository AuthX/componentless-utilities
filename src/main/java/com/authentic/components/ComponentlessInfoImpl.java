package com.authentic.components;

import com.authentic.util.ResourceBundleUtility;
import com.authentic.util.ValueListUtility;
import com.google.common.base.Strings;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentlessInfoImpl implements ComponentlessInfo, ResourceBundleUtility.Info, ValueListUtility.Info {
    private static final Logger log = LoggerFactory.getLogger(ComponentlessInfoImpl.class);

    private final Map<String, String> map;
    private final Map<String, DynamicParameter> parameterMap = new HashMap<>();

    public ComponentlessInfoImpl(final List<DynamicParameter> dynamicComponentParameters, final Map<String, String> parameterValues, final Map<String, String> localParameters) {
        map = parameterValues;
        dynamicComponentParameters.forEach(dynamicParameter -> parameterMap.put(dynamicParameter.getName(), dynamicParameter));

        // Sometimes values are inherited when they shouldn't be, lets work around that
        map.forEach((name, value) -> {
            final DynamicParameter parameter = parameterMap.get(name);
            if (localParameters.containsKey(name))
                map.put(name, localParameters.get(name));
            else if (parameter != null && parameter.isHideInChannelManager() && !Strings.isNullOrEmpty(parameter.getDefaultValue()))
                map.put(name, parameter.getDefaultValue());
        });
    }

    @Override
    public Logger getLog() {
        return log;
    }

    @Override
    public Map<String, String> getMap() {
        return map;
    }

    public Map<String, DynamicParameter> getParameters() {
        return parameterMap;
    }
}
