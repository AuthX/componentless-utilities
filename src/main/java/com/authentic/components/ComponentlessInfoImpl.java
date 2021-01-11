package com.authentic.components;

import com.authentic.util.ResourceBundleUtility;
import com.authentic.util.ValueListUtility;
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

    public ComponentlessInfoImpl(final List<DynamicParameter> dynamicComponentParameters, final Map<String, String> parameterValues) {
        map = parameterValues;
        dynamicComponentParameters.forEach(dynamicParameter -> parameterMap.put(dynamicParameter.getName(), dynamicParameter));
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
