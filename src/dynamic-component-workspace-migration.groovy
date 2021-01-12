package org.hippoecm.frontend.plugins.cms.admin.updater

import org.onehippo.repository.update.BaseNodeUpdateVisitor

import javax.jcr.*
import javax.jcr.query.Query
/**
 * Run against xpath query: //hst:workspace//element(*, hst:containeritemcomponent)
 */

class UpdaterTemplate extends BaseNodeUpdateVisitor {
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

    boolean doUpdate(Node component) {
        def session = component.getSession()
        def label = component.getProperty("hst:label").getString()
        def className = component.getProperty("hst:componentclassname").getString()
        def query = session.getWorkspace().getQueryManager().createQuery(
                "//element(*, hst:componentdefinition)[@hst:label = '${label}' and @hst:componentclassname = '${className}']",
                Query.XPATH
        )
        def nodes = query.execute().getNodes()
        if (!nodes.hasNext()) {
            log.debug "Component ${component.getPath()} does not appear to be a dynamic component"
            return false
        }
        def catalogNode = nodes.nextNode()
        def path = catalogNode.getPath()
        path = path.substring(path.indexOf("hst:catalog"), path.length()).replace("hst:catalog", "hst:components")
        log.debug("Migrating instance ${component.getPath()}")

        component.setProperty("hst:componentdefinition", path)

        // Remove the properties that would otherwise be local overrides of the definition
        component.getProperty("hst:componentclassname").remove()
        if (component.hasProperty("hst:template"))
            component.getProperty("hst:template").remove()

        // Clean up all the unnecessary child nodes
        def childNodes = component.getNodes()
        while (childNodes.hasNext())
            childNodes.nextNode().remove()

        return false
    }

    boolean undoUpdate(Node node) {
        throw new UnsupportedOperationException('Updater does not implement undoUpdate method')
    }

}
