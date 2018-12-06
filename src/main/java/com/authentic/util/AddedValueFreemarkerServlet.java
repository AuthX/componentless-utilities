package com.authentic.util;

import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.BeansWrapperBuilder;
import freemarker.template.Configuration;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import org.hippoecm.hst.servlet.HstFreemarkerServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

/**
 * The AddedValueFreemarkerServlet just extends the base Bloomreach Freemarker servlet to add a few
 * values that will be useful for templates.
 */
public class AddedValueFreemarkerServlet extends HstFreemarkerServlet {
    private static final Logger log = LoggerFactory.getLogger(AddedValueFreemarkerServlet.class);
    private static final String VAR_QUERYHELPER = "QueryHelper";
    private static final String VAR_VALUELISTUTILITY = "ValueListUtility";

    @Override
    public void init(final ServletConfig config) throws ServletException {
        super.init(config);

        final Configuration conf = super.getConfiguration();
        final BeansWrapper wrapper = new BeansWrapperBuilder(Configuration.VERSION_2_3_24).build();
        final TemplateHashModel staticModels = wrapper.getStaticModels();

        try {
            // Make the ValueListUtility statically available inside Freemarker.
            final TemplateModel valueListUtility = staticModels.get(ValueListUtility.class.getName());
            conf.setSharedVariable(VAR_VALUELISTUTILITY, valueListUtility);
        } catch (TemplateModelException e) {
            log.error("Error adding ValueListUtility", e);
        }

        try {
            // Make the QueryHelper statically available inside Freemarker.
        	final TemplateModel queryHelper = staticModels.get(QueryHelper.class.getName());
            conf.setSharedVariable(VAR_QUERYHELPER, queryHelper);
        } catch (TemplateModelException e) {
            log.error("Error adding QueryHelper", e);
        }
    }
}
