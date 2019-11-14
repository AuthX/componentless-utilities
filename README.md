# componentless components 
The Authentic componentless library provides component code and utility code to simplify creating Hippo projects.
   
## Installation
Installing the componentless library plugin into a Hippo CMS project requires only modifying the pom.xml of the site module to include the library in the maven build.

To get the most out of this project, you should also install [Beanless](http://github.com/authx/beanless)

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

In order to configure:
* create a node of type 'hst:containeritemcomponent' in the component catalog
* add required hst:containeritemcomponent fields, such as label, template, xtype, componentClassName (choose from available component classes below)
* add a node of type 'hst:abstractcomponent' to the hst:containeritemcomponent for each parameter desired
* for each hst:abstractcomponent node, add the 'hippostd:relaxed' mixin

On each hst:abstractcomponent node, add the following properties:

* defaultValue - String - optional - doesn't actually do anything, don't bother using this
* description - String - optional - just a useful description
* label - String - The label the content contributor sees
* name - String - The name by which the property will be accessed in the frontend
* hiddenInChannelManager - Boolean - default 'true' - determines if a content contributor can actually see and modify this field
* groupLabel - String - optional - A grouping label under which this field will display
* required - Boolean - default 'false' - Whether the content contributor must enter a value into this field
* type - String - default 'STRING' - Must be one of: [, STRING, VALUE_FROM_LIST, NUMBER, DATE, DOCUMENT, JCR_PATH, COLOR]
    * BOOLEAN - Causes this parameter to display as a checkbox
        * value must be 'on' or 'off'
    * STRING - Causes this parameter to display as a simple text box.
    * VALUE_FROM_LIST - Causes this parameter to display as a dropdown. When using this, you must also add the following properties to the node:
        * dropDownListDisplayValyes - Array of String - The labels for the values in the dropdown
        * dropDownListValues - Array of String - Teh values available in the dropdown
        * See below for example
    * NUMBER - Causes this parameter to display as a number field
    * DATE - Causes this parameter to display as a date field
    * DOCUMENT - Causes this parameter to display as a dropdown list of matching documents. Requires the following additional properties:
        * docType - String - the document types to display in this parameter
    * JCR_PATH - Causes this parameter to display as a document picker. You can add the following additional properties:
        * pickerConfiguration - String - the picker configuration path, usually 'cms-pickers/documents'
        * pickerInitialPath - String - the initial JCR path for the picker to start on
        * pickerSelectableNodeTypes - Array of String - an array of document types that this picker will allow to be selected
    * COLOR - Causes this parameter to display as a color picker
* value - String - optional  - Sets the value for this property, only useful in cases where 'hiddenInChannelManager' is true  

**Usage as non-catalog components**

In order to create a component as a non-catalog component, simply create a node of type hst:containeritemcomponent in the desired location, but instead of specifying hst:abstractcomponent nodes for each parameter you add them to the parameternames and parametervalues array properties. For example:

```
hst:parameternames: ["document", "required"]
hst:parametervalues: ["blog-posts/post", "on"]
```

**Frontend Usage**

Each component will assign a 'cparam' property, and potentially other properties depending upon the class.

Each parameter specified in the console will be available by it's "name" property in the cparam map. For example, if you add a parameter named "heading" then Freemarker will be able to access it by using `${cparam.heading}`

#### Dummy Component
componentClassName: com.authentic.components.DummyComponent

The Dummy Component has no functionality. It is useful in instances where a content contributor can add a component that consists only of a template, with no document or JCR information.

#### Document Content Component
componentClassName: com.authentic.components.DocumentContentComponent

Assigns a document to the request. This is used in two main scenarios: When a document will be selected based on the relativeContentPath set on the sitemap, or when the content contributor should specify a document.

Requires the following parameters to be added:

* _required_ - Must be of type 'BOOLEAN' - If set to 'on' then any time this component is unable to load a document, the user will be loaded to a 404 page
* _document_ - Optional - Must be of type 'JCR_PATH' If this parameter does not exist, the document content path will look for a relativeContentPath document in the sitemap

Assigns the following to the request:

* _document_ - The document 

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