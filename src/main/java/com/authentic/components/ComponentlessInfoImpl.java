package com.authentic.components;

import com.authentic.util.ResourceBundleUtility;
import com.authentic.util.ValueListUtility;
import com.google.common.base.Strings;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.DynamicParameterConfig;
import org.hippoecm.hst.configuration.components.JcrPathParameterConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComponentlessInfoImpl implements ComponentlessInfo, ResourceBundleUtility.Info, ValueListUtility.Info {
    private static final Logger log = LoggerFactory.getLogger(ComponentlessInfoImpl.class);

    private final Map<String, String> map;
    private final Map<String, DynamicParameter> parameterMap = new HashMap<>();

    public ComponentlessInfoImpl(final List<DynamicParameter> dynamicComponentParameters, final Map<String, String> parameterValues, final Map<String, String> localParameters, final DynamicComponentInfo proxyParametersInfo) {
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

        for (DynamicParameter param: dynamicComponentParameters) {

            // do not use BaseHstComponent#getComponentParameter since this does not take 'targeting' neither
            // POST query params into account, see HstParameterInfoProxyFactoryImpl#ParameterInfoInvocationHandler#getParameterValue
            final Object o = proxyParametersInfo.getResidualParameterValues().get(param.getName());
            if (o == null) {
                log.debug("No residual value for '{}' found. If it is non-residual, it means there is a subclass which " +
                        "should set the model for the explicit interface method itself.", param.getName());
                continue;
            }
            if (o instanceof String) {
                log.debug("override parameter '{}' with targeting value '{}'", param.getName(), (String) o);
                map.put(param.getName(), (String) o);
            } else if (o instanceof Boolean) {
                log.debug("override parameter '{}' with targeting value '{}'", param.getName(), ((Boolean)o) ? "on" : "off");
                map.put(param.getName(), ((Boolean)o) ? "on" : "off");
            } else {
                // never expected actually
                log.warn("Unexpected value type for jcr path param '{}'. Type was '{}', but String is expected",
                        param.getName(), o.getClass());
            }
        }
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

    @Override
    public String getValueLists() {
        return getStringParameter("valueLists");
    }

    @Override
    public String getResourceBundles() {
        return getStringParameter("resourceBundles");
    }
}
