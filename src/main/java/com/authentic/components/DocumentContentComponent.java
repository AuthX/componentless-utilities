package com.authentic.components;

import com.google.common.base.Strings;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.CommonComponent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.authentic.util.Constants.COMPONENT_PARAMETER_MAP;

/**
 * A generic class which serves as the functional part for any component that is
 * only intended to assign a single document to the request.
 */
@ParametersInfo(type=DocumentContentComponent.Info.class)
public class DocumentContentComponent extends CommonComponent {
    private static final Logger log = LoggerFactory.getLogger(DocumentContentComponent.class);

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

        assignDocumentBeans(request, root);
        if (!Strings.isNullOrEmpty(paramDocumentPath)) {
            bean = root.getBean(paramDocumentPath);
        } else {
            bean = context.getContentBean();
        }

        if (bean == null && paramInfo.isRequired() && !request.getRequestContext().isCmsRequest()) {
            pageNotFound(response);
        }

        Object paramMap = request.getAttribute(COMPONENT_PARAMETER_MAP);
        request.setAttribute(REQUEST_ATTR_DOCUMENT, bean);
        request.setModel(REQUEST_ATTR_DOCUMENT, bean);
        request.setAttribute(REQUEST_ATTR_PARAM_INFO, paramMap);
        request.setModel(REQUEST_ATTR_PARAM_INFO, paramMap);
    }

    @SuppressWarnings("unchecked")
    private void assignDocumentBeans(HstRequest request, HippoBean root) {
        final ObjectBeanManager beanManager = request.getRequestContext().getObjectBeanManager();
        final HashMap<String, Object> paramMap;
        Set<Map.Entry<String, Object>> paramSet;
        try {
            paramMap = (HashMap<String, Object>) request.getAttribute(COMPONENT_PARAMETER_MAP);
            paramSet = paramMap.entrySet();
            paramSet.stream()
                    .filter(e -> !e.getKey().equals(REQUEST_ATTR_DOCUMENT)
                            && e.getKey().endsWith("Document")
                            && isDocumentPath(e.getValue()))
                    .forEach(e -> request.setAttribute(e.getKey(), getBeanFromPath((String) e.getValue(), beanManager, root)));
        } catch (ClassCastException e) {
            log.error("cparam is not a parameter map. This should not happen.", e);
        }
    }

    private Object getBeanFromPath(String value, ObjectBeanManager beanManager, HippoBean root) {
        if (value.startsWith("/content")) { // Beans may start with /content or may not
            try {
                return beanManager.getObject(value);
            } catch (ObjectBeanManagerException e) {
                log.error("Error getting bean from path {}", value, e);
                return null;
            }
        }
        else
            return root.getBean(value);
    }

    private boolean isDocumentPath(Object value) {
        return value instanceof String && !Strings.isNullOrEmpty((String) value);
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
