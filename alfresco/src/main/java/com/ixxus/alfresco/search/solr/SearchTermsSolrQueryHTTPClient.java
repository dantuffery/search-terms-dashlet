package com.ixxus.alfresco.search.solr;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.repo.admin.RepositoryState;
import org.alfresco.repo.search.impl.lucene.LuceneQueryParserException;
import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.repo.search.impl.solr.SolrQueryHTTPClient;
import org.alfresco.repo.search.impl.solr.SolrStoreMapping;
import org.alfresco.repo.tenant.TenantService;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacetMethod;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacetSort;
import org.alfresco.service.cmr.search.SearchParameters.SortDefinition;
import org.alfresco.service.cmr.security.AuthorityType;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.util.PropertyCheck;
import org.apache.commons.codec.net.URLCodec;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.ByteArrayRequestEntity;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpClientParams;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.extensions.surf.util.I18NUtil;

import com.ixxus.alfresco.search.model.SearchTerm;
import com.ixxus.alfresco.search.model.SearchTermsParameters;
import com.ixxus.alfresco.search.service.SearchTermService;


/**
 * Extends Alfresco's <code>SolrQueryHTTPClient</code> class. 
 * 
 * @author dtuffery
 */
public class SearchTermsSolrQueryHTTPClient extends SolrQueryHTTPClient implements BeanFactoryAware
{
    static Log s_logger = LogFactory.getLog(SearchTermsSolrQueryHTTPClient.class);

    private NodeService nodeService;
    private PermissionService permissionService;
    private TenantService tenantService;
    private AuthenticationService authenticationService;
    private SearchTermService searchTermService;
    private Map<String, String> languageMappings;
    private List<SolrStoreMapping> storeMappings;
    private HashMap<StoreRef, HttpClient> httpClients = new HashMap<StoreRef, HttpClient>();
    private HashMap<StoreRef, SolrStoreMapping> mappingLookup = new HashMap<StoreRef, SolrStoreMapping>();
	private RepositoryState repositoryState;
    private BeanFactory beanFactory;
    private boolean includeGroupsForRoleAdmin = false;
	
    public SearchTermsSolrQueryHTTPClient(){}

    public void init()
    {
        PropertyCheck.mandatory(this, "NodeService", nodeService);
        PropertyCheck.mandatory(this, "PermissionService", permissionService);
        PropertyCheck.mandatory(this, "TenantService", tenantService);
        PropertyCheck.mandatory(this, "LanguageMappings", languageMappings);
        PropertyCheck.mandatory(this, "StoreMappings", storeMappings);
        PropertyCheck.mandatory(this, "RepositoryState", repositoryState);

        for(SolrStoreMapping mapping : storeMappings)
        {
            mappingLookup.put(mapping.getStoreRef(), mapping);
            
            HttpClientFactory httpClientFactory = (HttpClientFactory)beanFactory.getBean(mapping.getHttpClientFactory());
            HttpClient httpClient = httpClientFactory.getHttpClient();
            HttpClientParams params = httpClient.getParams();
            params.setBooleanParameter(HttpClientParams.PREEMPTIVE_AUTHENTICATION, true);
            httpClient.getState().setCredentials(new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), new UsernamePasswordCredentials("admin", "admin"));
            httpClients.put(mapping.getStoreRef(), httpClient);
        }
    }
    
    /**
     * @param repositoryState the repositoryState to set
     */
    public void setRepositoryState(RepositoryState repositoryState)
    {
        this.repositoryState = repositoryState;
    }
    
    public void setNodeService(NodeService nodeService)
    {
        this.nodeService = nodeService;
    }

    public void setPermissionService(PermissionService permissionService)
    {
        this.permissionService = permissionService;
    }
    
    public void setTenantService(TenantService tenantService)
    {
        this.tenantService = tenantService;
    }
    
    public void setAuthenticationService(AuthenticationService authenticationService) 
    {
		this.authenticationService = authenticationService;
	}

	public void setSearchTermService(SearchTermService searchTermService) 
	{
		this.searchTermService = searchTermService;
	}

    public void setLanguageMappings(Map<String, String> languageMappings)
    {
        this.languageMappings = languageMappings;
    }

    public void setStoreMappings(List storeMappings)
    {
        this.storeMappings = storeMappings;
    }
    
    /**
     * @param includeGroupsForRoleAdmin the includeGroupsForRoleAdmin to set
     */
    public void setIncludeGroupsForRoleAdmin(boolean includeGroupsForRoleAdmin)
    {
        this.includeGroupsForRoleAdmin = includeGroupsForRoleAdmin;
    }

	public ResultSet executeQuery(SearchParameters searchParameters, String language)
    {   
		if(repositoryState.isBootstrapping())
	    {
	        throw new AlfrescoRuntimeException("SOLR queries can not be executed while the repository is bootstrapping");
	    }
		
        try
        {
        	
        	if (searchParameters.getStores().size() == 0)
            {
                throw new AlfrescoRuntimeException("No store for query");
            }
            
            StoreRef store = searchParameters.getStores().get(0);
            
            SolrStoreMapping mapping = mappingLookup.get(store);
            
            if (mapping == null)
            {
                throw new AlfrescoRuntimeException("No solr query support for store " + searchParameters.getStores().get(0).toString());
            }
            
            URLCodec encoder = new URLCodec();
            StringBuilder url = new StringBuilder();
            url.append(mapping.getBaseUrl());
            
            String languageUrlFragment = languageMappings.get(language);
            if (languageUrlFragment == null)
            {
                throw new AlfrescoRuntimeException("No solr query support for language " + language);
            }
            url.append("/").append(languageUrlFragment);

         
            // Send the query in JSON only
            url.append("?wt=").append(encoder.encode("json", "UTF-8"));
            url.append("&fl=").append(encoder.encode("DBID,score", "UTF-8"));
            
            if (searchParameters.getMaxItems() >= 0)
            {
                url.append("&rows=").append(encoder.encode("" + searchParameters.getMaxItems(), "UTF-8"));
            }
            else if(searchParameters.getLimitBy() == LimitBy.FINAL_SIZE)
            {
                url.append("&rows=").append(encoder.encode("" + searchParameters.getLimit(), "UTF-8"));
            }
            else
            {
                url.append("&rows=").append(encoder.encode("" + Integer.MAX_VALUE, "UTF-8"));
            }
            
            url.append("&df=").append(encoder.encode(searchParameters.getDefaultFieldName(), "UTF-8"));
            url.append("&start=").append(encoder.encode("" + searchParameters.getSkipCount(), "UTF-8"));

            Locale locale = I18NUtil.getLocale();
            if (searchParameters.getLocales().size() > 0)
            {
                locale = searchParameters.getLocales().get(0);
            }
            url.append("&locale=");
            url.append(encoder.encode(locale.toString(), "UTF-8"));

            StringBuffer sortBuffer = new StringBuffer();
            for (SortDefinition sortDefinition : searchParameters.getSortDefinitions())
            {
                if (sortBuffer.length() == 0)
                {
                    sortBuffer.append("&sort=");
                }
                else
                {
                    sortBuffer.append(encoder.encode(", ", "UTF-8"));
                }
                sortBuffer.append(encoder.encode(sortDefinition.getField(), "UTF-8")).append(encoder.encode(" ", "UTF-8"));
                if (sortDefinition.isAscending())
                {
                    sortBuffer.append(encoder.encode("asc", "UTF-8"));
                }
                else
                {
                    sortBuffer.append(encoder.encode("desc", "UTF-8"));
                }

            }
            url.append(sortBuffer);

            url.append("&fq=").append(encoder.encode("{!afts}AUTHORITY_FILTER_FROM_JSON", "UTF-8"));
            
            url.append("&fq=").append(encoder.encode("{!afts}TENANT_FILTER_FROM_JSON", "UTF-8"));
            
            //========== DT Customisations Start ==========//
            
            if(searchParameters instanceof SearchTermsParameters && 
            		((SearchTermsParameters)searchParameters).getFilterQueries().size() > 0)
            {
            	List<String> filterQueries = ((SearchTermsParameters)searchParameters).getFilterQueries();
            	for(String filterQuery:  filterQueries)
            	{
            		/**
            		 * Certain values have been escaped to ensure the
            		 * query is accepted by the lucene query parser.
            		 */
            		filterQuery = filterQuery.replace("{", "\\{");
            		filterQuery = filterQuery.replace("http", "http\\");
            		filterQuery = filterQuery.replace("}", "\\}");
            		filterQuery = filterQuery.replace(" ", "*");
            		filterQuery = filterQuery.replace("TO", " TO ");
            		url.append("&fq=").append(encoder.encode("{!lucene}" + filterQuery, "UTF-8"));
            	}
            }
            
            //========== DT Customisations END ============//

            if(searchParameters instanceof SearchTermsParameters && 
            		searchParameters.getFieldFacets().size() > 0)
            {
                url.append("&facet=").append(encoder.encode("true", "UTF-8"));
                for(FieldFacet facet : searchParameters.getFieldFacets())
                {
                    url.append("&facet.field=").append(encoder.encode(facet.getField(), "UTF-8"));
                    if(facet.getEnumMethodCacheMinDF() != 0)
                    {
                        url.append("&").append(encoder.encode("f."+facet.getField()+".facet.enum.cache.minDf", "UTF-8")).append("=").append(encoder.encode(""+facet.getEnumMethodCacheMinDF(), "UTF-8"));
                    }
                    url.append("&").append(encoder.encode("f."+facet.getField()+".facet.limit", "UTF-8")).append("=").append(encoder.encode(""+facet.getLimit(), "UTF-8"));
                    if(facet.getMethod() != null)
                    {
                        url.append("&").append(encoder.encode("f."+facet.getField()+".facet.method", "UTF-8")).append("=").append(encoder.encode(facet.getMethod()==FieldFacetMethod.ENUM ?  "enum" : "fc", "UTF-8"));
                    }
                    if(facet.getMinCount() != 0)
                    {
                        url.append("&").append(encoder.encode("f."+facet.getField()+".facet.mincount", "UTF-8")).append("=").append(encoder.encode(""+facet.getMinCount(), "UTF-8"));
                    }
                    if(facet.getOffset() != 0)
                    {
                        url.append("&").append(encoder.encode("f."+facet.getField()+".facet.offset", "UTF-8")).append("=").append(encoder.encode(""+facet.getOffset(), "UTF-8"));
                    }
                    if(facet.getPrefix() != null)
                    {
                        url.append("&").append(encoder.encode("f."+facet.getField()+".facet.prefix", "UTF-8")).append("=").append(encoder.encode(""+facet.getPrefix(), "UTF-8"));
                    }
                    if(facet.getSort() != null)
                    {
                        url.append("&").append(encoder.encode("f."+facet.getField()+".facet.sort", "UTF-8")).append("=").append(encoder.encode(facet.getSort() == FieldFacetSort.COUNT ? "count" : "index", "UTF-8"));
                    }
                    
                }
            }
            
            // end of field factes
            
            JSONObject body = new JSONObject();
            body.put("query", searchParameters.getQuery());

            
            // Authorities go over as is - and tenant mangling and query building takes place on the SOLR side

            Set<String> allAuthorisations = permissionService.getAuthorisations();
            boolean includeGroups = includeGroupsForRoleAdmin ? true : !allAuthorisations.contains(PermissionService.ADMINISTRATOR_AUTHORITY);
            
            JSONArray authorities = new JSONArray();
            for (String authority : allAuthorisations)
            {
                if(includeGroups)
                {
                    authorities.put(authority);
                }
                else
                {
                    if(AuthorityType.getAuthorityType(authority) != AuthorityType.GROUP)
                    {
                        authorities.put(authority);
                    }
                }
            }
            body.put("authorities", authorities);
            
            JSONArray tenants = new JSONArray();
            tenants.put(tenantService.getCurrentUserDomain());
            body.put("tenants", tenants);

            JSONArray locales = new JSONArray();
            for (Locale currentLocale : searchParameters.getLocales())
            {
                locales.put(DefaultTypeConverter.INSTANCE.convert(String.class, currentLocale));
            }
            if (locales.length() == 0)
            {
                locales.put(I18NUtil.getLocale());
            }
            body.put("locales", locales);

            JSONArray templates = new JSONArray();
            for (String templateName : searchParameters.getQueryTemplates().keySet())
            {
                JSONObject template = new JSONObject();
                template.put("name", templateName);
                template.put("template", searchParameters.getQueryTemplates().get(templateName));
                templates.put(template);
            }
            body.put("templates", templates);

            JSONArray allAttributes = new JSONArray();
            for (String attribute : searchParameters.getAllAttributes())
            {
                allAttributes.put(attribute);
            }
            body.put("allAttributes", allAttributes);

            body.put("defaultFTSOperator", searchParameters.getDefaultFTSOperator());
            body.put("defaultFTSFieldOperator", searchParameters.getDefaultFTSFieldOperator());
            if (searchParameters.getMlAnalaysisMode() != null)
            {
                body.put("mlAnalaysisMode", searchParameters.getMlAnalaysisMode().toString());
            }
            body.put("defaultNamespace", searchParameters.getNamespace());

            JSONArray textAttributes = new JSONArray();
            for (String attribute : searchParameters.getTextAttributes())
            {
                textAttributes.put(attribute);
            }
            body.put("textAttributes", textAttributes);

            PostMethod post = new PostMethod(url.toString());
            post.setRequestEntity(new ByteArrayRequestEntity(body.toString().getBytes("UTF-8"), "application/json"));

            try
            {
            	HttpClient httpClient = httpClients.get(store);
                
                if(httpClient == null)
                {
                    throw new AlfrescoRuntimeException("No http client for store " + searchParameters.getStores().get(0).toString());
                }
                
                httpClient.executeMethod(post);

                if(post.getStatusCode() == HttpStatus.SC_MOVED_PERMANENTLY || post.getStatusCode() == HttpStatus.SC_MOVED_TEMPORARILY)
                {
	    	        Header locationHeader = post.getResponseHeader("location");
	    	        if (locationHeader != null)
	    	        {
	    	            String redirectLocation = locationHeader.getValue();
	    	            post.setURI(new URI(redirectLocation, true));
	    	            httpClient.executeMethod(post);
	    	        }
                }

                if (post.getStatusCode() != HttpServletResponse.SC_OK)
                {
                    throw new LuceneQueryParserException("Request failed " + post.getStatusCode() + " " + url.toString());
                }

                Reader reader = new BufferedReader(new InputStreamReader(post.getResponseBodyAsStream()));
                // TODO - replace with streaming-based solution e.g. SimpleJSON ContentHandler
                JSONObject json = new JSONObject(new JSONTokener(reader));
                SolrJSONResultSet results = new SolrJSONResultSet(json, searchParameters, nodeService);
                if (s_logger.isDebugEnabled())
                {
                    s_logger.debug("Sent :" + url);
                    s_logger.debug("   with: " + body.toString());
                    s_logger.debug("Got: " + results.getNumberFound() + " in " + results.getQueryTime() + " ms");
                }
                
                searchTermService.addSearchQuery(new SearchTerm(searchParameters.getQuery(), 
        				results.length(), new Date(), this.authenticationService.getCurrentUserName()));
                
                return results;
            }
            finally
            {
                post.releaseConnection();
            }
        }
        catch (UnsupportedEncodingException e)
        {
            throw new LuceneQueryParserException("", e);
        }
        catch (HttpException e)
        {
            throw new LuceneQueryParserException("", e);
        }
        catch (IOException e)
        {
            throw new LuceneQueryParserException("", e);
        }
        catch (JSONException e)
        {
            throw new LuceneQueryParserException("", e);
        }
    }
	
	 /* (non-Javadoc)
     * @see org.springframework.beans.factory.BeanFactoryAware#setBeanFactory(org.springframework.beans.factory.BeanFactory)
     */
    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        this.beanFactory = beanFactory;
    }

}
