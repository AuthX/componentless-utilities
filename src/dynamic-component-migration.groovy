package org.hippoecm.frontend.plugins.cms.admin.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor

import javax.jcr.*
import javax.jcr.query.Query
/**
 * Run against xpath query: //hst:catalog//element(*, hst:containeritemcomponent)
 */

class UpdaterTemplate extends BaseNodeUpdateVisitor {
    ValueFactory valueFactory = null
    Map<String, List<String>> groups = null

    boolean logSkippedNodePaths() {
        return false // don't log skipped node paths
    }

    boolean skipCheckoutNodes() {
        return false // return true for readonly visitors and/or updates unrelated to versioned content
    }

    Node firstNode(final Session session) throws RepositoryException {
        return null // implement when using custom node selection/navigation
    }

    Node nextNode() throws RepositoryException {
        return null // implement when using custom node selection/navigation
    }

    boolean doUpdate(Node oldComponent) {
        def catalogNode = oldComponent.getParent()
        def session = oldComponent.getSession()
        String componentName = oldComponent.getName()

        // Move, don't remove old component just yet
        session.move(oldComponent.getPath(), "${oldComponent.getPath()}-old")
        valueFactory = session.getValueFactory()

        // Reset group list, we're going to add to it as we go along
        groups = new HashMap<>()

        log.debug "Generating ${componentName}"
        // Overwrite old one if we're re-running the script
        if (catalogNode.hasNode(componentName))
            catalogNode.getNode(componentName).remove()

        def newComponent = catalogNode.addNode(componentName, "hst:componentdefinition")
        // Create our new component
        newComponent.setProperty("hst:componentclassname", oldComponent.getProperty("hst:componentclassname").getValue())
        if (oldComponent.hasProperty("hst:iconpath"))
            newComponent.setProperty("hst:iconpath", oldComponent.getProperty("hst:iconpath").getValue())
        if (oldComponent.hasProperty("hst:label"))
            newComponent.setProperty("hst:label", oldComponent.getProperty("hst:label").getValue())
        if (oldComponent.hasProperty("hst:template"))
            newComponent.setProperty("hst:template", oldComponent.getProperty("hst:template").getValue())
        if (oldComponent.hasProperty("hst:xtype"))
            newComponent.setProperty("hst:xtype", oldComponent.getProperty("hst:xtype").getValue())
        if (oldComponent.hasProperty("hiddenInChannelManager"))
            newComponent.setProperty("hst:hiddeninchannelmanager", oldComponent.getProperty("hiddenInChannelManager").getValue())

        // Iterate through children and migrate over the fields
        def nodes = oldComponent.getNodes()
        while (nodes.hasNext()) {
            def fieldNode = nodes.nextNode()
            migrateFieldNode(fieldNode, newComponent)
        }

        addFieldGroups(newComponent)
        migrateComponentInstances(session, oldComponent.getProperty("hst:label").getValue().getString(), oldComponent.getProperty("hst:componentclassname").getString(), newComponent)
        session.removeItem(oldComponent.getPath())
        return false
    }

    void migrateComponentInstances(Session session, String label, String className, Node newComponent) {
        def query = session.getWorkspace().getQueryManager().createQuery(
            "//element(*, hst:containeritemcomponent)[@hst:label = '${label}' and @hst:componentclassname = '${className}']",
            Query.XPATH
        )
        def nodes = query.execute().getNodes()
        def path = newComponent.getPath()
        path = path.substring(path.indexOf("hst:catalog"), path.length()).replace("hst:catalog", "hst:components")
        while (nodes.hasNext()) {
            def node = nodes.nextNode()
            if (node.getPath().contains("/hst:catalog/"))
                continue
            log.debug("Migrating instance ${node.getPath()}")
            node.setProperty("hst:componentdefinition", path)

            // Remove the properties that would otherwise be local overrides of the definition
            node.getProperty("hst:componentclassname").remove()
            if (node.hasProperty("hst:template"))
                node.getProperty("hst:template").remove()

            // Clean up all the unnecessary child nodes
            def childNodes = node.getNodes()
            while (childNodes.hasNext())
                childNodes.nextNode().remove()
        }
    }

    void migrateFieldNode(Node fieldNode, Node newComponent) {
        // Make sure its an abstractcomponent.. Not sure what else it would be but lets be safe
        if (!fieldNode.isNodeType("hst:abstractcomponent"))
            return
        // Make sure we have the properties that we need to migrate this
        try {
            verifyProperty(fieldNode, "name")
            verifyProperty(fieldNode, "label")
        } catch (Exception e) {
            log.error e.getMessage()
        }

        String name = fieldNode.getProperty("name").getString()
        Node newField = newComponent.addNode(name, "hst:dynamicparameter")

        newField.setProperty("hst:displayname", fieldNode.getProperty("label").getValue())
        newField.setProperty("hst:hideinchannelmanager", getPropertyOrDefault(fieldNode, "hiddenInChannelManager", false))
        newField.setProperty("hst:required", getPropertyOrDefault(fieldNode, "required", false))
        newField.setProperty("hst:defaultvalue", getPropertyOrDefault(fieldNode, "defaultValue", ""))
        newField.setProperty("hst:valuetype", getValueType(fieldNode))

        // Add to groups if there is a group label
        if (fieldNode.hasProperty("groupLabel")) {
            addToGroup(name, fieldNode.getProperty("groupLabel").getString())
        }

        addFieldConfig(fieldNode, newField)
    }

    void addToGroup(String name, String group) {
        if (!groups.containsKey(group))
            groups.put(group, new ArrayList<String>())
        groups.get(group).add(name)
    }

    void addFieldGroups(Node componentNode) {
        if (groups.isEmpty())
            return
        String[] fieldGroups = groups.keySet().toArray(String[].class)
        componentNode.setProperty("hst:fieldgroups", fieldGroups)
        for (String groupName : fieldGroups) {
            final String hstGroupName = "hst:fieldgroups.${groupName}"
            def fields = groups.get(groupName)
            String[] fieldNames = fields.toArray(String[].class)
            componentNode.setProperty(hstGroupName, fieldNames)
        }
    }

    static void addFieldConfig(Node fieldNode, Node newField) {
        if (!fieldNode.hasProperty("type"))
            return
        if (fieldNode.getProperty("type").getString() == "JCR_PATH") {
            def fieldConfig = newField.addNode("hst:fieldconfig", "hst:jcrpath")
            if (fieldNode.hasProperty("pickerInitialPath"))
                fieldConfig.setProperty("hst:pickerinitialpath", fieldNode.getProperty("pickerInitialPath").getValue())
            if (fieldNode.hasProperty("pickerSelectableNodeTypes"))
                fieldConfig.setProperty("hst:pickerselectablenodetypes", fieldNode.getProperty("pickerSelectableNodeTypes").getValues())
            if (fieldNode.hasProperty("pickerConfiguration"))
                fieldConfig.setProperty("hst:pickerconfiguration", fieldNode.getProperty("pickerConfiguration").getValue())
            fieldConfig.setProperty("hst:relative", true)
        } else if (fieldNode.getProperty("type").getString() == "VALUE_FROM_LIST") {
            def fieldConfig = newField.addNode("hst:fieldconfig", "hst:dropdown")
            Value[] labels = null
            if (fieldNode.hasProperty("dropDownListDisplayValues")) {
                labels = fieldNode.getProperty("dropDownListDisplayValues").getValues()
            }
            Value[] values = fieldNode.getProperty("dropDownListValues").getValues()
            List<String> options = new ArrayList<>()

            for (def i = 0; i < values.length; i++) {
                String label = labels != null ? labels[i].getString() : values[i].getString()
                options.add("${values[i].getString()}=${label}")
            }

            String optionString = String.join(";", options)
            fieldConfig.setProperty("hst:sourceid", optionString)
            fieldConfig.setProperty("hst:valuelistprovider", "com.authentic.util.DropdownValueListProvider")
        }
    }

    Value getValueType(Node fieldNode) {
        if (fieldNode.hasProperty("type"))
            switch (fieldNode.getProperty("type").getString()) {
                case "BOOLEAN":
                    return valueFactory.createValue("boolean")
                case "NUMBER":
                    return valueFactory.createValue("integer")
                case "DATE":
                    return valueFactory.createValue("datetime")
            }
        return valueFactory.createValue("text")
    }

    Value getPropertyOrDefault(Node fieldNode, String property, boolean defVal) {
        if (fieldNode.hasProperty(property))
            return fieldNode.getProperty(property).getValue()
        if (valueFactory != null)
            return valueFactory.createValue(defVal)
        throw new Exception("This just shouldn't happen tbh")
    }

    Value getPropertyOrDefault(Node fieldNode, String property, String defVal) {
        if (fieldNode.hasProperty(property))
            return fieldNode.getProperty(property).getValue()
        if (valueFactory != null)
            return valueFactory.createValue(defVal)
        throw new Exception("This just shouldn't happen tbh")
    }

    static void verifyProperty(Node node, String propertyName) throws Exception {
        if (!node.hasProperty(propertyName))
            throw new Exception("Unable to migrate ${node.getName()} because it is missing property '${propertyName}'")
    }

    boolean undoUpdate(Node node) {
        throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
    }

}
