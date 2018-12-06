package com.authentic.components;

import com.google.common.base.Strings;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.CommonComponent;

import static com.authentic.util.Constants.COMPONENT_PARAMETER_MAP;

/**
 * A generic class which serves as the functional part for any component that is
 * only intended to assign a single document to the request.
 */
@ParametersInfo(type=DocumentContentComponent.Info.class)
public class DocumentContentComponent extends CommonComponent {
    /**
     * @param request HstRequest
     * @param response
     * Checks for the presence of a document setting in the component param info,
     * if one exists it sets it on the request. If not, it checks for the existence of
     * a bean set by the URL. If none exists, and the required checkbox is checked in the
     * param info, it redirects to a 404 page. Otherwise, it assigns null to the request.
     */
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);
        final Info paramInfo = getComponentParametersInfo(request);
        final String paramDocumentPath = paramInfo.getDocument();
        final HstRequestContext context = request.getRequestContext();
        final HippoBean root = context.getSiteContentBaseBean();
        HippoBean bean;

        if (!Strings.isNullOrEmpty(paramDocumentPath)) {
            bean = root.getBean(paramDocumentPath);
        } else {
            bean = context.getContentBean();
        }

        if (bean == null && paramInfo.isRequired() && !request.getRequestContext().isCmsRequest()) {
            pageNotFound(response);
        }

        request.setAttribute(REQUEST_ATTR_DOCUMENT, bean);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, request.getAttribute(COMPONENT_PARAMETER_MAP));
    }

    interface Info {
        @Parameter(name = "document", displayName = "Document")
        @JcrPath(
                isRelative = true,
                pickerSelectableNodeTypes = {"hippo:document"}
        )
        String getDocument();

        @Parameter(name = "required", displayName = "Required?", hideInChannelManager = true, defaultValue = "false")
        Boolean isRequired();
    }
}
