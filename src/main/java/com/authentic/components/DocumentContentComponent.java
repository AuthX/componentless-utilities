package com.authentic.components;

import com.google.common.base.Strings;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.configuration.components.DynamicParameterConfig;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.authentic.util.QueryHelper.getBeanFromPath;

/**
 * A generic class which serves as the functional part for any component that is
 * only intended to assign a single document to the request.
 */
@ParametersInfo(type = DynamicComponentInfo.class)
public class DocumentContentComponent extends ComponentlessComponent {
    /**
     * @param request HstRequest
     * @param response
     * Checks for the presence of a document setting in the component param info,
     * if one exists it sets it on the request. If not, it checks for the existence of
     * a bean set by the URL. If none exists, and the required checkbox is checked in the
     * param info, it redirects to a 404 page. Otherwise, it assigns null to the request.
     */
    @Override
    public void doBeforeRender(final HstRequest request, final HstResponse response) {
        super.doBeforeRender(request, response);
        final HstRequestContext context = request.getRequestContext();
        final HippoBean root = context.getSiteContentBaseBean();
        final Info componentlessInfo = getComponentlessInfo(Info.class);

        assignDocumentBeans(request, componentlessInfo.getDocumentParams(), root);

        // Check for if a document is required
        if (componentlessInfo.isRequired()) {
            HippoBean bean;
            if (Strings.isNullOrEmpty(componentlessInfo.getDocument())) {
                bean = context.getContentBean();
            } else {
                bean = root.getBean(componentlessInfo.getDocument());
            }
            if (bean == null) {
                pageNotFound(response);
            }
        }
    }

    protected void assignDocumentBeans(final HstRequest request, final Map<String, String> parameterValues, final HippoBean root) {
        final ObjectBeanManager beanManager = request.getRequestContext().getObjectBeanManager();
        parameterValues.keySet()
            .forEach(key -> {
                HippoBean bean = getBeanFromPath(parameterValues.get(key), beanManager, root);
                if (bean != null) {
                    request.setModel(key, bean);
                    request.setAttribute(key, bean);
                } else {
                    // Don't assign empty bean strings
                    request.removeModel(key);
                    request.removeAttribute(key);
                }
            });
    }

    static public class Info extends ComponentlessInfoImpl {
        public Info(List<DynamicParameter> dynamicComponentParameters, Map<String, String> parameterValues, Map<String, String> localParameters) {
            super(dynamicComponentParameters, parameterValues, localParameters);
        }

        public Map<String, String> getDocumentParams() {
            Map<String, String> result = new HashMap<>();

            // For backwards compatibility, we first add anything with "document" in the name
            getMap().forEach((key, value) -> {
                if ((key.startsWith("document") || key.endsWith("Document")) && isDocumentPath(value))
                    result.put(key, value);
            });

            // Check for parameters that are of type JCR path; if we get a value here then override the parameterValue
            getParameters().forEach((name, dynamicParameter) -> {
                final DynamicParameterConfig config = dynamicParameter.getComponentParameterConfig();
                if (config != null && config.getType() == DynamicParameterConfig.Type.JCR_PATH) {
                    if (getMap().containsKey(name))
                        result.put(name, getMap().get(name));
                }
            });

            return result;
        }

        private boolean isDocumentPath(Object value) {
            return value instanceof String && !Strings.isNullOrEmpty((String) value);
        }

        public String getDocument() {
            return getStringParameter("document");
        }

        public boolean isRequired() {
            return getBoolParameter("required", false);
        }
    }
}
