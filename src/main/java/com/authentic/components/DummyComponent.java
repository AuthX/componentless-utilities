package com.authentic.components;

import com.authentic.util.ResourceBundleUtility;
import com.authentic.util.ValueListUtility;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;

/**
 * This is essentially just a stub for a component with no backend functionality.
 * It is used to power the Metadata and Video components which do not really have backend functions.
 */
@ParametersInfo(type=DummyComponent.Info.class)
public class DummyComponent extends BaseHstComponent {
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);
        Info paramInfo = getComponentParametersInfo(request);
        ValueListUtility.addValueListsToModel(request, paramInfo);
        ResourceBundleUtility.addResourceBundlesToModel(request, paramInfo);
    }

    interface Info extends ResourceBundleUtility.Info, ValueListUtility.Info {
    }
}
