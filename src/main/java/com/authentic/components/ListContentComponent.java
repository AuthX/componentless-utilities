package com.authentic.components;

import com.authentic.util.QueryHelper;
import com.google.common.base.Strings;
import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.component.HstResponse;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import static com.authentic.util.Constants.COMPONENT_PARAMETER_MAP;
import static com.authentic.util.Constants.REQUEST_ATTR_DOCUMENTS;
import static com.authentic.util.QueryHelper.getBeanFromPath;

/**
 * A generic class which serves as the functional part for any component that is
 * intended to assign a list of documents to the request.
 * This has a lot of configuration items, most of which are hidden from normal contributors
 * to allow for creating multiple "virtual" components in the console using one logical Java class.
 */
@ParametersInfo(type=DynamicComponentInfo.class)
public class ListContentComponent extends ComponentlessComponent {
    private static final Logger log = LoggerFactory.getLogger(ListContentComponent.class);
    private static final String NODETYPE_SEPARATOR = ",";

    /**
     * @param request HstRequest
     * @param response
     * Checks for the presence of a document setting in the component param info,
     * if one exists it gets the list of documents and sets them on the request. If not,
     * it checks for the existence of beans set by the URL. If none exists, and the required
     */
    @Override
    public void doBeforeRender(HstRequest request, HstResponse response) {
        super.doBeforeRender(request, response);
        final Info paramInfo = getComponentlessInfo(Info.class, request);
        final HstRequestContext context = request.getRequestContext();
        final HippoBean root = context.getSiteContentBaseBean();

        IterablePagination<HippoBean> pagination;
        final QueryHelper helper = new QueryHelper(request, this, paramInfo);

        HippoBean scope;
        if (!Strings.isNullOrEmpty(paramInfo.getPath())) {
            // Scope by a specified path by getting all the beans in a given path
            scope = getScopeFromSpecifiedPath(paramInfo, root);
        } else {
            scope = getScopeFromContentPath(context, root);
        }

        if (scope != null)
            pagination = getBeansFromQuery(paramInfo, helper, request, context, scope);
        else
            pagination = getBeansFromParamInfo(helper, request, root, context);

        if (pagination != null) {
            request.setAttribute(REQUEST_ATTR_DOCUMENTS, pagination.getItems());
            request.setAttribute(REQUEST_ATTR_PAGEABLE, pagination);
            request.setModel(REQUEST_ATTR_DOCUMENTS, pagination.getItems());
        }
    }

    private IterablePagination<HippoBean> getBeansFromParamInfo(QueryHelper helper, HstRequest request, HippoBean root, HstRequestContext context) {
        final ObjectBeanManager beanManager =  context.getObjectBeanManager();
        final HashMap<String, Object> paramMap;
        Set<Map.Entry<String, Object>> paramSet;
        try {
            paramMap = (HashMap<String, Object>) request.getAttribute(COMPONENT_PARAMETER_MAP);
            paramSet = paramMap.entrySet();
            TreeMap<String, HippoBean> keyBeanHolder = new TreeMap<>();

            paramSet.stream()
                    .filter(e -> e.getKey().matches("document[0-9?]")
                            && isDocumentPath(e.getValue()))
                    .forEach(e -> {
                        HippoBean bean = getBeanFromPath((String) e.getValue(), beanManager, root);
                        if (bean != null) {
                            keyBeanHolder.put(e.getKey(), bean);
                        }
                    });

            List<HippoBean> beans = new ArrayList<>(keyBeanHolder.values());

            return helper.buildPageable(beans);

        } catch (ClassCastException e) {
            log.error("cparam is not a parameter map. This should not happen.", e);
        }

        return null;
    }

    @Nullable
    private HippoBean getScopeFromSpecifiedPath(final Info paramInfo, final HippoBean root) {
        final String specifiedPath = paramInfo.getPath();
        if (Strings.isNullOrEmpty(specifiedPath))
            return null;
        final HippoBean scope;
        if ("/".equals(specifiedPath))
            scope = root;
        else
            scope = root.getBean(specifiedPath);
        return scope;
    }

    @Nullable
    private HippoBean getScopeFromContentPath(final HstRequestContext context, final HippoBean root) {
        final String contentPath = context.getResolvedSiteMapItem().getRelativeContentPath();
        if (Strings.isNullOrEmpty(contentPath))
            return null;

        return root.getBean(contentPath);
    }

    /**
     * This is a generic query to get beans based upon the nodetypes specified in the paraminfo, and a root scope
     */
    @Nullable
    private IterablePagination<HippoBean> getBeansFromQuery(final Info paramInfo, final QueryHelper helper, final HstRequest request, final HstRequestContext context, final HippoBean scope) {
        if (scope == null)
            return null;

        final String nodeTypeString = paramInfo.getNodeTypes();
        String[] nodeTypes;

        if (nodeTypeString.contains(NODETYPE_SEPARATOR)) {
            nodeTypes = nodeTypeString.split(NODETYPE_SEPARATOR);
        } else {
            nodeTypes = new String[1];
            nodeTypes[0] = nodeTypeString;
        }

        try {
            final HstQuery query = context.getQueryManager().createQuery(scope, true, nodeTypes);
            helper.appendParameters(query);
            modifyQuery(query, request);
            final HippoBeanIterator iterator = query.execute().getHippoBeans();
            return helper.buildPageable(iterator);
        } catch (QueryException e) {
            log.debug("Failed to get beans from a content path", e);
            return null;
        }
    }

    private boolean isDocumentPath(Object value) {
        return value instanceof String && !Strings.isNullOrEmpty((String) value);
    }

    protected void modifyQuery(HstQuery query, HstRequest request) {
        // For implementation by child classes
    }

    protected static class Info extends ComponentlessInfoImpl implements QueryHelper.Info {
        public Info(List<DynamicParameter> dynamicComponentParameters, Map<String, String> parameterValues, Map<String, String> localParameters, DynamicComponentInfo proxyParametersInfo) {
            super(dynamicComponentParameters, parameterValues, localParameters, proxyParametersInfo);
        }

        /**
         * Allows a contributor to assign a path in the /content. If this parameter is set,
         * we will query that path for documents and turn them into a list assigned to the
         * request.
         */
        public String getPath() {
            return getStringParameter("path");
        }
    }
}
