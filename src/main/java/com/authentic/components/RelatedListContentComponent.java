package com.authentic.components;

import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.configuration.components.DynamicParameter;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.exceptions.FilterException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.ParametersInfo;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * Extends the ListContentComponent to allow comparison queries against a document
 */
@ParametersInfo(type=DynamicComponentInfo.class)
public class RelatedListContentComponent extends ListContentComponent {
    private static final Logger log = LoggerFactory.getLogger(RelatedListContentComponent.class);

    @Override
    protected void modifyQuery(HstQuery query, HstRequest request) {
        final HstRequestContext context = request.getRequestContext();
        final HippoBean contentBean = context.getContentBean();

        if (contentBean == null)
            return;

        final Info paramInfo = getComponentlessInfo(Info.class, request);
        final String relatedField = paramInfo.getRelatedField();
        final Object property = contentBean.getSingleProperty(paramInfo.getField());
        final Filter baseFilter = query.createFilter();

        if (property instanceof String[]) {
            final String[] values = (String[]) property;
            final Filter stringFilters = query.createFilter();

            for (String value : values) {
                Filter valueFilter = query.createFilter();
                addStringFilter(valueFilter, relatedField, value);
                stringFilters.addOrFilter(valueFilter);
            }

            baseFilter.addAndFilter(stringFilters);

        } else if (property instanceof String) {
            addStringFilter(baseFilter, relatedField, (String) property);
        }

        excludeCurrentDocument(query, baseFilter, contentBean);

        if (query.getFilter() != null)
            ((Filter) query.getFilter()).addAndFilter(baseFilter);
        else
            query.setFilter(baseFilter);

        request.setAttribute(REQUEST_ATTR_DOCUMENT, contentBean);
        request.setModel(REQUEST_ATTR_DOCUMENT, contentBean);
    }

    private void excludeCurrentDocument(HstQuery query, Filter baseFilter, HippoBean contentBean) {
        final Filter exclusionFilter = query.createFilter();
        try {
            exclusionFilter.addNotEqualTo("jcr:uuid", contentBean.getIdentifier());
            baseFilter.addAndFilter(exclusionFilter);
        } catch (FilterException e) {
            log.error("Error excluding current document in filter", e);
        }
    }

    private void addStringFilter(Filter filter, String relatedField, String value) {
        try {
            filter.addEqualTo(relatedField, value);
        } catch (FilterException e) {
            log.error("Error adding string filter", e);
        }
    }

    protected static class Info extends ListContentComponent.Info {
        public Info(List<DynamicParameter> dynamicComponentParameters, Map<String, String> parameterValues, Map<String, String> localParameters, DynamicComponentInfo proxyParametersInfo) {
            super(dynamicComponentParameters, parameterValues, localParameters, proxyParametersInfo);
        }

        /**
         * Specify the name of the field in the current document which we are going to compare.
         */
        public String getField() {
            return getStringParameter("field");
        }

        /**
         * Specifies the name of the field in the documents we are comparing against.
         */
        public String getRelatedField() {
            return getStringParameter("relatedField");
        }
    }
}
