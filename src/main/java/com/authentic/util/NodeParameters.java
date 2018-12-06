package com.authentic.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.HashMap;

import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_NAMES;
import static org.hippoecm.hst.configuration.HstNodeTypes.GENERAL_PROPERTY_PARAMETER_VALUES;

/**
 * A short utility class that parses 'hst:parameternames' and 'hst:parametervalues' into a simple map
 */
public class NodeParameters extends HashMap<String, String> {
    private static final Logger LOG = LoggerFactory.getLogger(NodeParameters.class);

    public NodeParameters(Node node) {
        try {
            if (!node.hasProperty(GENERAL_PROPERTY_PARAMETER_NAMES)
                || !node.hasProperty(GENERAL_PROPERTY_PARAMETER_VALUES))
                return;

            Value[] names = node.getProperty(GENERAL_PROPERTY_PARAMETER_NAMES).getValues();
            Value[] values = node.getProperty(GENERAL_PROPERTY_PARAMETER_VALUES).getValues();

            for (int i = 0; i < names.length; i++) {
                if (values.length > i) {
                    put(names[i].getString(), values[i].getString());
                }
            }

        } catch (RepositoryException e) {
            LOG.error("Repository exception parsing node parameters", e);
        }
    }
}
