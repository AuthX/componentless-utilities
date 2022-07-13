package com.authentic.util;

import com.authentic.components.ComponentlessInfo;
import com.google.common.base.Strings;
import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.component.HstRequest;
import org.hippoecm.hst.core.parameters.Parameter;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.hippoecm.hst.util.HstResponseUtils;
import org.onehippo.forge.selection.hst.contentbean.ValueList;
import org.onehippo.forge.selection.hst.contentbean.ValueListItem;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ValueListUtility {
    private ValueListUtility() {}

    public static void addValueListsToModel(final HstRequest request, final ValueListUtility.Info info) {
        final String ids = info.getValueLists();
        addValueListsByIdsToModel(request, ids);

    }

    public static void addValueListsByIdsToModel(final HstRequest request, final String ids) {
        if (!Strings.isNullOrEmpty(ids)) {
            final Map<String, Map<String, String>> valueLists = getValueListMaps(Arrays.asList(ids.split(",")));
            valueLists.forEach(request::setModel);
        }

    }
    public static ValueList getValueList(String path) {
        final HstRequestContext hstRequestContext = RequestContextProvider.get();
        final HippoBean siteContentBaseBean = hstRequestContext.getSiteContentBaseBean();
        HippoBean bean = siteContentBaseBean.getBean(path);
        if (bean == null)
            bean = siteContentBaseBean.getBean(String.format("value-lists/%s", path.toLowerCase()));
        if (bean == null)
            bean = siteContentBaseBean.getParentBean().getBean(String.format("administration/value-lists/%s", path));
        if (bean == null)
            bean = siteContentBaseBean.getParentBean().getBean(path);
        if (bean == null) {
            if (hstRequestContext.getResolvedMount() != null &&
                    hstRequestContext.getResolvedMount().getMount() != null &&
                    hstRequestContext.getResolvedMount().getMount().getHstSite() != null) {

                String siteName = hstRequestContext.getResolvedMount().getMount().getHstSite().getName();
                bean = siteContentBaseBean.getBean(String.format("%s/value-lists/%s", siteName, path.toLowerCase()));
            }
        }

        if (bean != null)
            return bean.getBean(".", ValueList.class);
        return null;
    }

    public static Map<String, String> getValueListMap(String path) {
        final ValueList valueList = getValueList(path);
        if (valueList == null)
            return null;

        final List<ValueListItem> items = valueList.getItems();
        Map<String, String> result = new LinkedHashMap<>();
        items.forEach(item -> result.put(item.getKey(), item.getLabel()));

        return result;
    }

    public static Map<String, Map<String, String>> getValueListMaps(List<String> lists) {
        Map<String, Map<String, String>> maps = new HashMap<>();
        lists.forEach(list -> {
            Map<String, String> valueListMap = getValueListMap(list);
            if (valueListMap != null)
                maps.put(list, valueListMap);
        });
        return maps;
    }

    public interface Info {
        @Parameter(name = "valueLists", displayName = "Value Lists")
        String getValueLists();
    }
}
