package com.ixxus.alfresco.search.jscript;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.repo.jscript.ScriptNode;
import org.alfresco.repo.jscript.Search;
import org.alfresco.repo.jscript.ValueConverter;
import org.alfresco.repo.search.impl.lucene.SolrJSONResultSet;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.LimitBy;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacetSort;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.ixxus.alfresco.search.model.SearchTermsParameters;

/**
 * Extends Alfresco's Search class.
 * 
 * @author dtuffery
 */

public class TermsSearch extends Search 
{
	private static final Log logger = LogFactory.getLog(TermsSearch.class);
	
	public Scriptable query(Object search)
    {
        Object[] results = null;
        
        if (search instanceof Serializable)
        {
            Serializable obj = new ValueConverter().convertValueForRepo((Serializable)search);
            if (obj instanceof Map)
            {
                Map<Serializable, Serializable> def = (Map<Serializable, Serializable>)obj;
                
                // test for mandatory values
                String query = (String)def.get("query");
                if (query == null || query.length() == 0)
                {
                    throw new AlfrescoRuntimeException("Failed to search: Missing mandatory 'query' value.");
                }
                
                // collect optional values
                String store = (String)def.get("store");
                String language = (String)def.get("language");
                List<Map<Serializable, Serializable>> sort = (List<Map<Serializable, Serializable>>)def.get("sort");
                Map<Serializable, Serializable> page = (Map<Serializable, Serializable>)def.get("page");
                String namespace = (String)def.get("namespace");
                String onerror = (String)def.get("onerror");
                String defaultField = (String)def.get("defaultField");
                
                //========== DT Customisations Start ==========//
                
                String facetField = (String)def.get("facetField");
                String facetSort = (String)def.get("facetSort");
                String facetLimit = (String)def.get("facetLimit");
                String facetMinCount = (String)def.get("facetMinCount");
                List<String> filterQueries = (List<String>)def.get("filterQueries");
                
                //========== DT Customisations END ============//
                
                // extract supplied values
                
                // sorting columns
                SortColumn[] sortColumns = null;
                if (sort != null)
                {
                    sortColumns = new SortColumn[sort.size()];
                    int index = 0;
                    for (Map<Serializable, Serializable> column : sort)
                    {
                        String strCol = (String)column.get("column");
                        if (strCol == null || strCol.length() == 0)
                        {
                            throw new AlfrescoRuntimeException("Failed to search: Missing mandatory 'sort: column' value.");
                        }
                        Boolean boolAsc = (Boolean)column.get("ascending");
                        boolean ascending = (boolAsc != null ? boolAsc.booleanValue() : false);
                        sortColumns[index++] = new SortColumn(strCol, ascending);
                    }
                }
                
                // paging settings
                int maxResults = -1;
                int skipResults = 0;
                if (page != null)
                {
                    if (page.get("maxItems") != null)
                    {
                        Object maxItems = page.get("maxItems");
                        if (maxItems instanceof Number)
                        {
                            maxResults = ((Number)maxItems).intValue();
                        }
                        else if (maxItems instanceof String)
                        {
                            // try and convert to int (which it what it should be!)
                            maxResults = Integer.parseInt((String)maxItems);
                        }
                    }
                    if (page.get("skipCount") != null)
                    {
                        Object skipCount = page.get("skipCount");
                        if (skipCount instanceof Number)
                        {
                            skipResults = ((Number)page.get("skipCount")).intValue();
                        }
                        else if (skipCount instanceof String)
                        {
                            skipResults = Integer.parseInt((String)skipCount);
                        }
                    }
                }
                
                // query templates
                Map<String, String> queryTemplates = null;
                List<Map<Serializable, Serializable>> templates = (List<Map<Serializable, Serializable>>)def.get("templates");
                if (templates != null)
                {
                    queryTemplates = new HashMap<String, String>(templates.size(), 1.0f);
                    
                    for (Map<Serializable, Serializable> template : templates)
                    {
                        String field = (String)template.get("field");
                        if (field == null || field.length() == 0)
                        {
                            throw new AlfrescoRuntimeException("Failed to search: Missing mandatory 'template: field' value.");
                        }
                        String t = (String)template.get("template");
                        if (t == null || t.length() == 0)
                        {
                            throw new AlfrescoRuntimeException("Failed to search: Missing mandatory 'template: template' value.");
                        }
                        queryTemplates.put(field, t);
                    }
                }
                
                //========== DT Customisations Start ==========//
                
                SearchTermsParameters sp = new SearchTermsParameters();
                if(facetField != null)
                {
                	FieldFacet fieldFacet = new FieldFacet(facetField);
                	fieldFacet.setMinCount(new Integer(facetMinCount));
                	fieldFacet.setLimit(new Integer(facetLimit));
                	fieldFacet.setSort(facetSort == "index" ? FieldFacetSort.INDEX : FieldFacetSort.COUNT);
                	sp.addFieldFacet(fieldFacet);
                }
                
                if(filterQueries != null)
                {
                	sp.setFilterQueries(filterQueries);
                }
            
                //========== DT Customisations END ============//
                
                sp.addStore(store != null ? new StoreRef(store) : this.storeRef);
                sp.setLanguage(language != null ? language : SearchService.LANGUAGE_LUCENE);
                sp.setQuery(query);
                if (defaultField != null)
                {
                    sp.setDefaultFieldName(defaultField);
                }
                if (namespace != null)
                {
                    sp.setNamespace(namespace);
                }
                if (maxResults > 0)
                {
                    sp.setLimit(maxResults);
                    sp.setLimitBy(LimitBy.FINAL_SIZE);
                }
                if (skipResults > 0)
                {
                    sp.setSkipCount(skipResults);
                }
                if (sort != null)
                {
                    for (SortColumn sd : sortColumns)
                    {
                        sp.addSort(sd.column, sd.asc);
                    }
                }
                if (queryTemplates != null)
                {
                    for (String field: queryTemplates.keySet())
                    {
                        sp.addQueryTemplate(field, queryTemplates.get(field));
                    }
                }
                
                // error handling opions
                boolean exceptionOnError = true;
                if (onerror != null)
                {
                    if (onerror.equals("exception"))
                    {
                        // default value, do nothing
                    }
                    else if (onerror.equals("no-results"))
                    {
                        exceptionOnError = false;
                    }
                    else
                    {
                        throw new AlfrescoRuntimeException("Failed to search: Unknown value supplied for 'onerror': " + onerror);
                    }
                }
                
                // execute search based on search definition
                results = query(sp, exceptionOnError);
            }
        }
        
        if (results == null)
        {
            results = new Object[0];
        }
        
        return Context.getCurrentContext().newArray(getScope(), results);
    }
	
	
	
	protected Object[] query(SearchParameters sp, boolean exceptionOnError)
	{ 
		Collection<Scopeable> set = null;
        
        // perform the search against the repo
        ResultSet results = null;
        try
        {
        	
            results = this.services.getSearchService().query(sp);
            
            if (results.length() != 0)
            {
                NodeService nodeService = this.services.getNodeService();
                set = new LinkedHashSet<Scopeable>(results.length(), 1.0f);
                for (ResultSetRow row: results)
                {
                    NodeRef nodeRef = row.getNodeRef();
                    if (nodeService.exists(nodeRef))
                    {
                       set.add(new ScriptNode(nodeRef, this.services, getScope()));
                    }
                }
            }
            
            //========== DT Customisations Start ==========//
            
            if(results.length() != 0 && !sp.getFieldFacets().isEmpty())
        	{
        		List<FieldFacet> fieldFacets = sp.getFieldFacets();
        		for(FieldFacet facetField : fieldFacets)
        		{
        			List<Pair<String, Integer>> facets = ((SolrJSONResultSet)results).getFieldFacet(facetField.getField());
        			set.add(new ScriptFacet(facetField, facets, getScope(), this.services));
        		}
        	}
            
            //========== DT Customisations END ============//
            
        }
        catch (Throwable err)
        {
            if (exceptionOnError)
            {
                throw new AlfrescoRuntimeException("Failed to execute search: " + sp.getQuery(), err);
            }
            else if (logger.isDebugEnabled())
            {
                logger.debug("Failed to execute search: " + sp.getQuery(), err);
            }
        }
        finally
        {
            if (results != null)
            {
                results.close();
            }
        }
        
        return set != null ? set.toArray(new Object[(set.size())]) : new Object[0];
	}
}
