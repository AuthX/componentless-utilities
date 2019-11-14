package com.authentic.util;

import org.apache.commons.lang.StringUtils;
import org.hippoecm.hst.component.support.bean.BaseHstComponent;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.ObjectBeanManagerException;
import org.hippoecm.hst.content.beans.manager.ObjectBeanManager;
import org.hippoecm.hst.content.beans.query.HstQuery;
import org.hippoecm.hst.content.beans.query.HstQueryResult;
import org.hippoecm.hst.content.beans.query.exceptions.QueryException;
import org.hippoecm.hst.content.beans.query.filter.Filter;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.content.beans.standard.HippoBeanIterator;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.JcrPath;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.SearchInputParsingUtils;
import org.onehippo.cms7.essentials.components.paging.IterablePagination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.onehippo.cms7.essentials.components.utils.SiteUtils.getAnyIntParameter;
import static org.onehippo.cms7.essentials.components.utils.SiteUtils.getAnyParameter;

public class QueryHelper {
    private static final Logger log = LoggerFactory.getLogger(QueryHelper.class);

    private static final String ORDER_BY_ASCENDING = "asc";
    private static final String ORDER_BY_DESCENDING = "desc";
    // Alright, so, we replace segments that don't matter with a truthy statement. This is the fastest
    // I could find. Why not use true? Because in xpath: "true or true" is false. Read the info below for more.
    private static final String DEFAULT_REPLACEMENT = "(hippo:paths != '')";
    private static final String EXPRESSION_MATCH = "\\([^()]*?(?<fullParam>#\\{(?<param>[^}]+)})[^()]*?\\)";
    private static final String FUNCTION_MATCH = String.format("[A-Za-z:]+\\(%s\\)", DEFAULT_REPLACEMENT);
    private static final Pattern PATTERN = Pattern.compile(EXPRESSION_MATCH);
    private static final Pattern FUNCTION_MATCH_PATTERN = Pattern.compile(FUNCTION_MATCH);
    private String jcrExpr;
    private String order;
    private String orderField;
    private Integer pageSize;
    private Integer pageNumber;
    private HippoBean scope = null;
    private HstQuery query = null;
    private Filter filter = null;
    private String[] nodeTypes = null;

    private QueryHelper() {}

    public QueryHelper(HstRequest request, BaseHstComponent component, QueryHelper.Info info) {
        pageSize = parsePageSize(request, component, info);

        if (info.getPageNumberParam() != null)
            pageNumber = getAnyIntParameter(request, info.getPageNumberParam(), 1, component);
        if (info.getPageNumberParam() == null || pageNumber <= 0)
            pageNumber = 1;

        if (info.getOrderFieldParam() != null)
            orderField = getAnyParameter(info.getOrderFieldParam(), request, component);
        if (StringUtils.isEmpty(orderField) && StringUtils.isNotEmpty(info.getOrderField()))
            orderField = info.getOrderField();

        order = parseOrder(request, component, info);

        if (StringUtils.isNotEmpty(info.getFilterParams()))
            jcrExpr = parseFilterParams(info.getFilterParams(), component, request);
    }
    
    public static QueryHelper query() {
        return new QueryHelper();
    }

    public static HippoBean getBeanFromPath(String value, ObjectBeanManager beanManager, HippoBean root) {
        if (value.startsWith("/content")) { // Beans may start with /content or may not
            try {
                return (HippoBean) beanManager.getObject(value);
            } catch (ObjectBeanManagerException e) {
                log.error("Error getting bean from path {}", value, e);
            } catch (ClassCastException e) {
                log.error("While attempting to get a bean from path {}, got something else", value, e);
            }

            return null;
        }
        else
            return root.getBean(value);
    }

    private String parseOrder(HstRequest request, BaseHstComponent component, Info info) {
        String result = null;
        if (info.getOrderParam() != null)
            result = getAnyParameter(info.getOrderParam(), request, component);
        // Ensure we got asc or desc
        if (result != null && (!result.equalsIgnoreCase(ORDER_BY_ASCENDING) && !result.equalsIgnoreCase(ORDER_BY_DESCENDING)))
            result = null;
        if (StringUtils.isEmpty(result) && StringUtils.isNotEmpty(info.getOrder()))
            result = info.getOrder();

        return result;
    }

    private Integer parsePageSize(HstRequest request, BaseHstComponent component, Info info) {
        Integer result = 0;

        if (info.getPageSizeParam() != null)
            result = getAnyIntParameter(request, info.getPageSizeParam(), info.getPageSize(), component);
        else if (info.getPageSize() != null && info.getPageSize() != 0)
            result = info.getPageSize();
        if (result == null || result < 0)
            result = 0;

        return result;
    }

    // Add the parameters in this helper to a query
    public void appendParameters(HstQuery query) {
        if (StringUtils.isNotEmpty(getOrderField()) && StringUtils.isNotEmpty(getOrder())) {
            // We have an order field and an order, so we're going to order our results
            if (getOrder().equalsIgnoreCase(QueryHelper.ORDER_BY_ASCENDING))
                query.addOrderByAscendingCaseInsensitive(getOrderField());
            else if (getOrder().equalsIgnoreCase(QueryHelper.ORDER_BY_DESCENDING))
                query.addOrderByDescendingCaseInsensitive(getOrderField());
        }

        if (StringUtils.isNotEmpty(getJcrExpr())) {
            try {
                getFilter().addJCRExpression(getJcrExpr());
            } catch (QueryException e) {
                log.error("Error adding JCR expression to filter", e);
            }
        }

        if (filter != null)
            query.setFilter(filter);
    }

    public IterablePagination<HippoBean> buildPageable(List<HippoBean> children) {
        if (pageSize == 0)
            pageSize = children.size();
        if (pageNumber == null)
            pageNumber = 1;
        return new IterablePagination<>(children, pageNumber, pageSize);
    }

    public IterablePagination<HippoBean> buildPageable(HippoBeanIterator iterator) {
        if (pageSize == 0)
            pageSize = Math.toIntExact(iterator.getSize());
        if (pageNumber == null)
            pageNumber = 1;
        return new IterablePagination<>(iterator, pageSize, pageNumber);
    }

    public QueryHelper limit(Integer pageSize) {
        this.pageSize = pageSize;
        return this;
    }

    public QueryHelper expression(String expr) {
        this.jcrExpr = expr;
        return this;
    }

    public QueryHelper order(String field, String order) {
        this.orderField = field;
        this.order = order;
        return this;
    }

    public QueryHelper pageNumber(Integer pageNumber) {
        this.pageNumber = pageNumber;
        return this;
    }

    public QueryHelper types(String ...nodeTypes) {
        this.nodeTypes = nodeTypes;
        return this;
    }

    public QueryHelper scope(HippoBean scope) {
        this.scope = scope;
        return this;
    }

    public HstQuery getQuery() throws QueryException {
        if (query == null) {
            final HstRequestContext context = RequestContextProvider.get();
            query = context.getQueryManager().createQuery(context.getSiteContentBaseBean());
        }
        return query;
    }

    public Filter getFilter() throws QueryException {
        if (filter == null) {
            filter = getQuery().createFilter();
        }
        return filter;
    }

    public QueryHelper containsOne(String key, String search) throws QueryException {
        return containsOne(key, Arrays.asList(search.split(" ")));
    }

    public QueryHelper containsOne(String key, List<String> values) throws QueryException {
        Filter orFilter = getQuery().createFilter();

        for (String value : values) {
            Filter containsFilter = getQuery().createFilter();
            containsFilter.addContains(key, value);
            orFilter.addOrFilter(containsFilter);
        }

        getFilter().addAndFilter(orFilter);

        return this;
    }

    public QueryHelper equalsOne(String key, String search) throws QueryException {
        return equalsOne(key, Arrays.asList(search.split(" ")));
    }

    public QueryHelper equalsOne(String key, List<String> values) throws QueryException {
        Filter orFilter = getQuery().createFilter();

        for (String value : values) {
            Filter equalsFilter = getQuery().createFilter();
            equalsFilter.addEqualTo(key, value);
            orFilter.addOrFilter(equalsFilter);
        }

        getFilter().addAndFilter(orFilter);

        return this;
    }

    public QueryHelper filterNot(String key, String value) throws QueryException {
        Filter notFilter = getQuery().createFilter();
        notFilter.addNotEqualTo(key, value);
        getFilter().addAndFilter(notFilter);
        return this;
    }

    public QueryHelper equals(String key, String value) throws QueryException {
        Filter filter = getQuery().createFilter();
        filter.addEqualTo(key, value);
        getFilter().addAndFilter(filter);
        return this;
    }

    public IterablePagination<HippoBean> execute(HstRequestContext context) {
        if (scope == null)
            scope = context.getSiteContentBaseBean();
        try {
            HstQuery hstQuery;
            if (nodeTypes != null)
                hstQuery = context.getQueryManager().createQuery(scope, nodeTypes);
            else
                hstQuery = context.getQueryManager().createQuery(scope);
            appendParameters(hstQuery);
            HstQueryResult execute = hstQuery.execute();
            return buildPageable(execute.getHippoBeans());
        } catch (QueryException e) {
            log.error("Query error: ", e.getMessage(), e);
        }

        // Just send back an empty pageable
        return buildPageable(new ArrayList<>());
    }

    String getJcrExpr() {
        return jcrExpr;
    }

    String getOrder() {
        return order;
    }

    String getOrderField() {
        return orderField;
    }

    Integer getPageNumber() {
        return pageNumber;
    }

    Integer getPageSize() {
        return pageSize;
    }

    private String getMatchingValueForParam(HstRequest request, BaseHstComponent component, String param) {
        // Make sure this is a valid match
        if (StringUtils.isEmpty(param))
            return null;

        String defVal = null;
        String paramSubString = param;
        if (param.contains("|")) {
            defVal = param.substring(param.indexOf('|')+1);
            paramSubString = param.substring(0, param.indexOf('|'));
        }

        String val = getAnyParameter(paramSubString, request, component);
        // Make sure we have a parameter that matches, or else get the default
        if (StringUtils.isEmpty(val) && defVal == null)
            return null;
        else if (StringUtils.isEmpty(val))
            val = defVal;

        return SearchInputParsingUtils.parse(val, false);
    }

    private String parseFilterParams(String paramString, BaseHstComponent component, HstRequest request) {
        String result = parseFilterParamsParameters(paramString, component, request);
        return parseFilterParamsFunctions(result);
    }

    private String parseFilterParamsFunctions(String paramString) {
        Matcher matcher = FUNCTION_MATCH_PATTERN.matcher(paramString);
        if (!matcher.find())
            return paramString;

        matcher.reset();
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(result, DEFAULT_REPLACEMENT);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private String parseFilterParamsParameters(String paramString, BaseHstComponent component, HstRequest request) {
        Matcher matcher = PATTERN.matcher(paramString);

        // If there are no optional parameters, let's not bother with all this
        if (!matcher.find())
            return paramString;

        matcher.reset();
        StringBuffer result = new StringBuffer();

        // Find all of the matches of our expression, and then replace them
        while (matcher.find()) {
            String param = matcher.group("param");
            String fullReplacement = matcher.group("fullParam");
            String val = getMatchingValueForParam(request, component, param);

            if (val == null) {
                // Replaces the entire parentheses group with the default, as documented above
                matcher.appendReplacement(result, DEFAULT_REPLACEMENT);
            } else {
                // Replace just the ${} matched token with the value
                String replacement = matcher.group(0).replace(fullReplacement, val);
                matcher.appendReplacement(result, replacement);
            }
        }
        // Wrap up the replacement
        matcher.appendTail(result);

        return result.toString();
    }

    public interface Info {
        /**
         * A default page size or limit to the number of results in the list. The default, 0,
         * is unlimited. This value would be overridden if the pageSizeParam below is set AND
         * it has been added to the request.
         */
        @Parameter(name = "pageSize", displayName = "Page Size", defaultValue = "0", hideInChannelManager = true)
        Integer getPageSize();

        /**
         * The parameter name to look for in the request in order to change the current page size.
         * If this is null or empty, the user cannot change the page size from the default above.
         * For example, if this parameter is "size" then we would check for "?size=X" in the request,
         * and if it exists then we would change the pageSize parameter above to X.
         */
        @Parameter(name = "pageSizeParam", displayName = "Page Size Parameter", hideInChannelManager = true)
        String getPageSizeParam();

        /**
         * Whether to order by asc or desc. Only those values will be accepted. If this value does NOT equal
         * "asc" or "desc" then it will be discarded and not ordered.
         */
        @Parameter(name = "order", displayName = "Order", hideInChannelManager = true)
        String getOrder();

        /**
         * A parameter name to use in the request for 'order' allowing the user to specify the order to sort by.
         * For example, if this is set to "order" then we will look for "?order=X" in the request. The value will
         * need to be asc or desc, or else will be discarded and no sorting will take place.
         */
        @Parameter(name = "orderParam", displayName = "Order Parameter", hideInChannelManager = true)
        String getOrderParam();

        /**
         * The name of the field on which to apply ordering to sort.
         */
        @Parameter(name = "orderField", displayName = "Order Field", hideInChannelManager = true)
        String getOrderField();

        /**
         * This allows us to set a request parameter that the user can use to change which field is being
         * sorted/ordered. For example, if this is set to "orderBy" then we will look for "?orderBy=X" in
         * the request. If X is set to a field name that does not exist, this will be discarded and no sorting
         * will take place.
         */
        @Parameter(name = "orderFieldParam", displayName = "Order Field Parameter", hideInChannelManager = true)
        String getOrderFieldParam();

        /**
         * The name of the request parameter to use for pagination. If this is empty, the user will not be able to
         * change what page they are looking at. For example, if the value of this is "page" (the default)
         * then we will look for "?page=X" on the request.
         */
        @Parameter(name = "pageNumberParam", displayName = "Page Number Parameter", defaultValue = "page", hideInChannelManager = true)
        String getPageNumberParam();

        /**
         * A comma-separated list of document types that we are looking for. If this is not set, we will create
         * a list of ALL document types. If this is set to, for example, "brxp:Article,brxp:BlogArticle" we will
         * create a list of ONLY BlogArticle and Article doctypes.
         */
        @Parameter(name = "nodeTypes", displayName = "Supported Document Types", defaultValue = "brxp:basedocument")
        String getNodeTypes();

        /**
         * This allows you to craft a JCR statement for querying, and add in optional user parameters.
         * Here's an example value:
         * ((brxp:classifiable = "Blog") or (brxp:classifiable = "Finance")) and (jcr:contains(brxp:title, "#{title})")
         * This would query for a document where the classifiable field is Blog or Finance AND
         * where the title looks like the user-supplied query parameter "title."
         * Note that "title" will be expected in a query parameter in the URL like: ?title=X
         * IF there is no title query parameter, or it is empty, then the nearest parentheses group will be replaced
         * with a statement that is always true, negating it. For example:
         * (jcr:contains(brxp:title, "#{title}")) and (brxp:classifiable = "#{category}" or brxp:classifiable = "Finance")
         * The resulting query if "category" is empty but "title" is not would be:
         * (jcr:contains(brxp:title, "title")) and (true)
         * This allows for optional parameter configurations, where we can specify what to do when a param is empty.
         * Suppose you want to have an optional parameter with a default, it would look like:
         * (jcr:contains(brxp:title, "#{title|Financial}"))
         * In this scenario, if the title query paramter doesn't exist, then it uses "Financial" as the default.
         * Finally, you may query ALL of a document in a full text search using this format:
         * (jcr:contains(., "${search}"))
         * In this example, we will look for the "search" query parameter, and do a full text search on it throughout
         * the entirety of the document.
         * A few other notes:
         * - We're expecting that functions are all wrapped in their own parentheses
         *      (that is, (jcr:contains(., "#{test}")) is good, but NOT jcr:contains(., "#{test}")
         *      This is a limitation in the parsing I'm doing to save parsing time.
         * - The truthy statement we are using is above. Note that we do NOT use "true" because of a quirk of xpath:
         *      (true and true) is evaluted as true, but (true or true) is evaluated as false
         *      The result of this leads to an edge case: (jcr:title = "#{title}") or (jcr:subject = "#{title}")
         *      In this example, if the title parameter is excluded, we end up with "true or true" which
         *      is always false, and thus returns no results.
         * - The ${} syntax favored normally for such endeavors is replaced with #{} here. This is because the
         *      Bloomreach parser will actually evaluate these values for URL segment matches using the ${} syntax.
         *      In other words, sitemap parameters like ${1} can be used here as well to reference sitemap item URL
         *      segments. This is probably a very advanced case.
         * - Note that user-supplied parameters are all filtered through the SearchInputParsingUtils to sanitize them.
         */
        @Parameter(name = "filterParams", displayName = "Filter Parameters", hideInChannelManager = true)
        String getFilterParams();
    }
}
