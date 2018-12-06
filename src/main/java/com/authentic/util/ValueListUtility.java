package com.authentic.util;

import org.hippoecm.hst.container.RequestContextProvider;
import org.hippoecm.hst.content.beans.standard.HippoBean;
import org.hippoecm.hst.core.request.HstRequestContext;
import org.onehippo.forge.selection.hst.contentbean.ValueList;

public class ValueListUtility {
    private ValueListUtility() {}

    public static ValueList getValueList(String name) {
        HstRequestContext hstRequestContext = RequestContextProvider.get();
        HippoBean siteContentBaseBean = hstRequestContext.getSiteContentBaseBean();
        HippoBean bean = siteContentBaseBean.getParentBean().getBean("administration/value-lists");
        return bean.getBean(String.format("%s/%s", name, name), ValueList.class);
    }
}
