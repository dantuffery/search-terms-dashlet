package com.ixxus.alfresco.parsers;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ixxus.alfresco.search.model.SearchModel;
import com.ixxus.alfresco.search.model.SearchTerm;

import com.ixxus.alfresco.search.util.SearchConstants;

/**
 * Helper class that parses each search query and derives properties from 
 * the query: searchScope and query term. The properties are added to a Map
 * which is used to persist the information to the database.
 * 
 * @author dantuffery
 */

public class SearchQueryParserImpl implements SearchQueryParser
{
	private static final Log logger = LogFactory.getLog(SearchQueryParser.class);
	
	
	public Map<QName, Serializable> parseSearchQuery(SearchTerm searchQuery) 
	{
		String searchScope = getSearchScopeFromQueryStr(searchQuery.getQuery());
		String term = getSearchTermFromQueryStr(searchQuery.getQuery(), searchScope);
		
		searchQuery.setSearchScope(searchScope);
		searchQuery.setTerm(term);
		
		return getSearchTermProperties(searchQuery);
	}
	
	/**
	 * Creates a map of the search term properties from the given 
	 * <code>SearchTerm</code> object.
	 * 
	 * @param searchQuery
	 * @return Map<QName, Serializable>
	 */
	public Map<QName, Serializable> getSearchTermProperties(SearchTerm searchQuery)
	{
		
		if(searchQuery.getTerm() == null)
		{
			if(logger.isDebugEnabled())
			{
				logger.debug("No search term found, aborting");
			}
			return null;
		}
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Getting search term properties from SearchQuery object: " + searchQuery.toString());
		}
		
		Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
		properties.put(ContentModel.PROP_NAME, "searchterm-" + new Date().getTime());
		properties.put(SearchModel.PROP_TERM, searchQuery.getTerm());
		properties.put(SearchModel.PROP_SEARCH_SCOPE, searchQuery.getSearchScope());
		properties.put(SearchModel.PROP_NUMBER_OF_RESULTS, searchQuery.getNumberOfResults());
		properties.put(SearchModel.PROP_SEARCH_DATE, searchQuery.getSearchDate());
		properties.put(SearchModel.PROP_USER_NAME, searchQuery.getUserName());
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Search Term created " + properties.toString());
		}
		
		return properties;
	}
	
	
	/**
	 * Derives the <code>searchScope</code> by identifying patterns the given query string. 
	 * 
	 * There are three possible search scopes: individual site, all sites and repository scope.
	 * 
	 * @param query
	 * @return String
	 */
	public String getSearchScopeFromQueryStr(String query)
	{
		if (query == null) {
		    throw new IllegalArgumentException("query cannot be null");
		}
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Getting search scope form query string " + query);
		}
		
		String searchScope = null;
		
		if(StringUtils.contains(query, SearchConstants.SITES_QUERY_STR))
		{
			searchScope = StringUtils.substringBetween(query, 
					SearchConstants.SITES_QUERY_STR, SearchConstants.SLASH);
		}
		else if(StringUtils.contains(query, SearchConstants.ALL_SITES_QUERY_STR))
		{
			searchScope = SearchConstants.SEARCH_SCOPE_ALL_SITES;
		}
		else
		{
			searchScope = SearchConstants.SEARCH_SCOPE_REPO;
		}
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Search scope is " + searchScope);
		}
		
		return searchScope;
	}
	
	/**
	 * Derives the search term by the given <code>searchScope</code> and identifying 
	 * patterns in the given query string.
	 * 
	 * @param query
	 * @param searchScope
	 * @return String
	 */
	public String getSearchTermFromQueryStr(String query, String searchScope)
	{
		if (query == null) {
		    throw new IllegalArgumentException("query cannot be null");
		}
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Getting search term form query string " + query);
		}
		
		String term = null;
		
		if(searchScope.equals(SearchConstants.SEARCH_SCOPE_REPO))
		{
			term = StringUtils.substringBetween(query, 
					SearchConstants.DOUBLE_OPEN_BRACKET, SearchConstants.AND_BRACKET_PLUS);
		}
		else
		{
			term = StringUtils.substringBetween(query, 
					SearchConstants.AND_BRACKET, SearchConstants.AND);
		}
		
		if(logger.isDebugEnabled() && term != null)
		{
			logger.debug("Search term is " + term);
		}
		
		if (term != null) {
			term = term.trim();
		}
		
		return term;
	}
}
