# componentless components 
The Authentic componentless library provides component code and utility code to simplify creating Hippo projects.

Starting with V4, intended for use with v14.3 or greater of Bloomreach, this library is for use with Dynamic Components rather than the Authentic Componentless library.

## Installation
Installing the componentless library plugin into a Hippo CMS project requires only modifying the pom.xml of the site module to include the library in the maven build.

To use the frontend utilities, you must also modify your project's web.xml. Simply look for `org.hippoecm.hst.servlet.HstFreemarkerServlet` in web.xml with `com.authentic.util.AddedValueFreemarkerServlet`

### site/pom.xml
Add a dependency to the site/pom.xml to compile in the library classes:

------
                <dependency>
                        <groupId>com.authentic</groupId>
                        <artifactId>authentic-componentless-utilities</artifactId>
                        <version>*</version>
                </dependency>

## Usage

### Componentless Components
There are four available component classes that can achieve various effects through configuration in the CMS console.

**Component Setup**

Create dybamic components in the catalog as described in the [Bloomreach documentation](https://documentation.bloomreach.com/14/library/concepts/component-development/dynamic-components.html).

**Usage as non-catalog components**

In order to create a component as a non-catalog component, simply create a node of type hst:containeritemcomponent in the desired location, but instead of specifying hst:abstractcomponent nodes for each parameter you add them to the parameternames and parametervalues array properties. For example:

```
hst:parameternames: ["document", "required"]
hst:parametervalues: ["blog-posts/post", "on"]
```

**Frontend Usage**

Each component will assign a 'componentParameterMap' property, and potentially other properties depending upon the class.

Each parameter's raw value be available by it's "name" property in the cparam map. For example, if you add a parameter named "heading" then Freemarker will be able to access it by using `${cparam.heading}`

Each parameter will also be accessible, parsed by the specified component, directly. For example, `${document}`

#### Resource bundles and value lists

Every component made with one of the below classes can automatically include resource bundles or value lists. This is primarily useful for SPA++ or EditConnect, as a simple method of retrieving these values.

In the component, simply add a string parameter named 'resourceBundles' or a string parameter named 'valueLists' These parameters should be a comma-separated list of identifiers. Each of those identifiers will be processed and the resulting value list or resource bundle added to the request models.

#### Dummy Component
componentClassName: com.authentic.components.DummyComponent

The Dummy Component has no functionality. It is useful in instances where a content contributor can add a component that consists only of a template, with no document or JCR information.

#### Document Content Component
componentClassName: com.authentic.components.DocumentContentComponent

Assigns one or more documents to the request. This is used in two main scenarios: When a document will be selected based on the relativeContentPath set on the sitemap, or when the content contributor should specify a document.

Requires the following parameters to be added:

* _required_ - Must be of type 'BOOLEAN' - If set to 'on' then any time this component is unable to load a document as specified in the next parameter, the user will be loaded to a 404 page
* _document_ - Optional - Must be a valid hst:jcrpath parameter as specified by the Bloomreach dynamic components documentation. 

Assigns the following to the request:

* _document_ - The document
* \* - Any parameter which is a hst:jcrpath will also be parsed as a document.

#### List Content Component
componentClassName: com.authentic.components.ListContentComponent

Assigns a list of documents to the request. This can be used to populate based off of a JCR query, a relative content path query, or by allowing the content contributor to specify more than one document.

Utilizes the following configuration parameters:

* _path_ - Optional - A relative content path in which to search for documents,in the same format as the sitemap relativeContentPath
* _pageSize_ - Optional - A default page size or limit to the number of results in the list. The default, 0, is unlimited. This value would be overridden if the pageSizeParam below is set AND it has been added to the request.
* _pageSizeParam_ - Optional - The parameter name to look for in the request in order to change the current page size. If this is null or empty, the user cannot change the page size from the default above. For example, if this parameter is "size" then we would check for "?size=X" in the request, and if it exists then we would change the pageSize parameter above to X.
* _order_ - Optional - Either 'asc' or 'desc' - Whether to order by asc or desc. Only those values will be accepted. If this value does NOT equal "asc" or "desc" then it will be discarded and not ordered.
* _orderParam_ - Optional - A parameter name to use in the request for 'order' allowing the user to specify the order to sort by. For example, if this is set to "order" then we will look for "?order=X" in the request. The value will need to be asc or desc, or else will be discarded and no sorting will take place.
* _orderField_ - Optional - The name of the field on the documents on which to apply ordering.
* _orderFieldParam_ - Optional - This allows us to set a request parameter that the user can use to change which field is being sorted/ordered. For example, if this is set to "orderBy" then we will look for "?orderBy=X" in the request. If X is set to a field name that does not exist, this will be discarded and no sorting will take place.
* _pageNumberParam_ - Optional - The name of the request parameter to use for pagination. If this is empty, the user will not be able to change what page they are looking at. For example, if the value of this is "page" (the default) then we will look for "?page=X" on the request.
* _nodeTypes_ - Optional - A comma-separated list of document types that we are looking for. If this is not set, we will create a list of ALL document types. If this is set to, for example, "brxp:Article,brxp:BlogArticle" we will create a list of ONLY BlogArticle and Article doctypes.
* _filterParams_ - Optional - A JCR query to use.
* _documentX_ - Optional - If no path is specified, the component will look for numbered document parameters and build them into an array of documents if they exist.

**filterParams**

The filter params is a JCR query which will apply sanitized user input directly to a query.

Here's an example value:

```
((brxp:classifiable = "Blog") or (brxp:classifiable = "Finance")) and (jcr:contains(brxp:title, "#{title})")
```

This would query for a document where the classifiable field is Blog or Finance AND
where the title looks like the user-supplied query parameter "title."
Note that "title" will be expected in a query parameter in the URL like: ?title=X

IF there is no title query parameter, or it is empty, then the nearest parentheses group will be replaced
with a statement that is always true, negating it. For example:

```
(jcr:contains(brxp:title, "#{title}")) and (brxp:classifiable = "#{category}" or brxp:classifiable = "Finance")
```

The resulting query if "category" is empty but "title" is not would be:

```
(jcr:contains(brxp:title, "title")) and (true)
```

This allows for optional parameter configurations, where we can specify what to do when a param is empty.
Suppose you want to have an optional parameter with a default, it would look like:

```
(jcr:contains(brxp:title, "#{title|Financial}"))
```

In this scenario, if the title query paramter doesn't exist, then it uses "Financial" as the default.

Finally, you may query ALL of a document in a full text search using this format:
```
(jcr:contains(., "${search}"))
```
In this example, we will look for the "search" query parameter, and do a full text search on it throughout
the entirety of the document.

We're expecting that functions are all wrapped in their own parentheses (that is, (jcr:contains(., "#{test}")) is good, but NOT jcr:contains(., "#{test}") This is a limitation in the parsing I'm doing to save parsing time.

The truthy statement we are using is above. Note that we do NOT use "true" because of a quirk of xpath: (true and true) is evaluted as true, but (true or true) is evaluated as false The result of this leads to an edge case: (jcr:title = "#{title}") or (jcr:subject = "#{title}") In this example, if the title parameter is excluded, we end up with "true or true" which is always false, and thus returns no results.

The ${} syntax favored normally for such endeavors is replaced with #{} here. This is because the Bloomreach parser will actually evaluate these values for URL segment matches using the ${} syntax. In other words, sitemap parameters like ${1} can be used here as well to reference sitemap item URL segments. This is probably a very advanced case.

Note that user-supplied parameters are all filtered through the SearchInputParsingUtils to sanitize them.

#### Related List Content Component
componentClassName: com.authentic.components.RelatedListContentComponent

Creates a list of documents, based on values from another document. The document loaded for comparison must come from the sitemap's relativeContentPath. This is useful for, for example, showing a list of "related blog posts."

Uses ALL of the same parameters of the List Content Component, as well as the following:

* _field_ - String - Specify the name of the field in the current document which we are going to compare.
* _relatedField_ - String - Specifies the name of the field in the documents we are comparing against.

### Frontend Utilities
This library comes packaged with a few useful utilities that are automatically made availabe to all Freemarker templates:

#### QueryHelper
The QueryHelper is a fluent interface for building Hippo queries directly from a template.

```
<#assign articles=QueryHelper.query().limit(5).types("brxp:BlogPost", "brxp:Article").equals("brxp:Author", document.author).execute(hstRequestContext) />
<#list articles as article>
    ...
</#list> 
```

#### ValueListUtility
Just gets a value list.

```
<#assign someList=ValueListUtility.getValueList("some-folder/some-list") />
<#list someList.items as item>
    ${key} = ${label}
</#list>
```

## Migration

### Migration from v3 to v4

With the upgrade from version 3 of this utility to version 4, we are moving from using Componentless as the underlying technology to using Bloomreach Dynamic Components. As such, you MUST be on version 14.3 or higher of Bloomreach.

Included in this repository is `dynamic-component-migration.groovy` which is a groovy script to handle the migration for you. This script can be run in the CMS as an updater script. There are a few caveats:

**Non-Catalog Components**

IF you have components which are NOT in the catalog, but MUST be editable by users.. Dynamic Components must exist in the catalog in order for them to be editable in channel manager. The easy method for handling this:

- Find an instance of the component and export it as YAML
- Import that instance into the catalog.
- Add the hippostd:relaxed mixin to the catalog component.
- Add the 'hiddenInChannelManager' parameter as a boolean and set to true so that it will not appear in channel manager.

EVERY component that must be editable in the CMS must exist in the catalog. Doing it this way will allow the migration script to turn it automatically into a hidden dynamic component.

**Look out for standard components**

This migration script assumes that all components are dynamic. As such, non-dynamic components will be turned into dynamic components. Keep a backup of your catalog to add them back easily.