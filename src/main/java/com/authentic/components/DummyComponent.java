package com.authentic.components;

import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

import static com.authentic.util.Constants.COMPONENT_PARAMETER_MAP;
import static com.authentic.util.Constants.REQUEST_ATTR_PARAM_INFO;

/**
 * This is essentially just a stub for a component with no backend functionality.
 * It is used to power the Metadata and Video components which do not really have backend functions.
 */
public class DummyComponent extends BaseHstComponent {
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);
        getComponentParametersInfo(request);

        Object paramMap = request.getAttribute(COMPONENT_PARAMETER_MAP);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramMap);
        request.setModel(REQUEST_ATTR_PARAM_INFO, paramMap);
    }
}
