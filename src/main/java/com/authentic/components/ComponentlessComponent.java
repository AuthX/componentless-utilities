package com.authentic.components;

import com.authentic.util.ResourceBundleUtility;
import com.authentic.util.ValueListUtility;
import com.google.common.base.Strings;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.onehippo.cms7.essentials.components.CommonComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

import static com.authentic.util.Constants.COMPONENT_PARAMETER_MAP;

public class ComponentlessComponent extends CommonComponent {
    private static final Logger log = LoggerFactory.getLogger(ComponentlessComponent.class);

    private List<DynamicParameter> dynamicParameters = null;
    private Map<String, String> parameterValues = null;

    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);
        final DynamicComponentInfo paramInfo = getComponentParametersInfo(request);
        dynamicParameters = paramInfo.getDynamicComponentParameters();
        parameterValues = this.getComponentParameters();
        ComponentlessInfoImpl info = new ComponentlessInfoImpl(dynamicParameters, parameterValues, getComponentLocalParameters(), paramInfo);

        info.getMap().forEach((name, value) -> {
            if (!Strings.isNullOrEmpty(value)) {
                request.setModel(name, value);
                request.setAttribute(name, value);
            }
        });

        request.setAttribute(COMPONENT_PARAMETER_MAP, info.getMap());
        request.setModel(COMPONENT_PARAMETER_MAP, info.getMap());
        ValueListUtility.addValueListsToModel(request, info);
        ResourceBundleUtility.addResourceBundlesToModel(request, info);
    }

    protected <T extends ComponentlessInfo> T getComponentlessInfo(Class<T> clazz, final HstRequest request) {
        try {
            final Constructor<T> construct = clazz.getConstructor(List.class, Map.class, Map.class, DynamicComponentInfo.class);
            final DynamicComponentInfo paramInfo = getComponentParametersInfo(request);
            return construct.newInstance(dynamicParameters, parameterValues, getComponentLocalParameters(), paramInfo);
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            log.error("Unable to create a component info instance", e);
        }
        return null;
    }
}
