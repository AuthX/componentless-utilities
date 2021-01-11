package com.authentic.components;

import org.hippoecm.hst.configuration.components.DynamicComponentInfo;
import org.hippoecm.hst.core.parameters.ParametersInfo;

/**
 * This is essentially just a stub for a component with no backend functionality.
 * It is used to power the Metadata and Video components which do not really have backend functions.
 */
@ParametersInfo(type = DynamicComponentInfo.class)
public class DummyComponent extends ComponentlessComponent {
}
