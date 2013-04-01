package com.ixxus.alfresco.parsers;

import static org.junit.Assert.*;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.junit.Before;
import org.junit.Test;

import com.ixxus.alfresco.search.SearchTermTest;
import com.ixxus.alfresco.search.model.SearchModel;
import com.ixxus.alfresco.search.model.SearchTerm;
import com.ixxus.alfresco.search.util.SearchConstants;

/**
 * @author dantuffery
 */

public class SearchQueryParsersTest implements SearchTermTest 
{
	
	//private static final String REPO_QUERY = "((report  AND (+TYPE:\"cm:content\" +TYPE:\"cm:folder\")) AND -TYPE:\"cm:thumbnail\" AND -TYPE:\"cm:failedThumbnail\" AND -TYPE:\"cm:rating\") AND NOT ASPECT:\"sys:hidden\"";
    private static final String ALL_SITES_QUERY = "((PATH:\"/app:company_home/st:sites/*/*//*\" AND (scrum  AND (+TYPE:\"cm:content\" +TYPE:\"cm:folder\"))) AND -TYPE:\"cm:thumbnail\" AND -TYPE:\"cm:failedThumbnail\" AND -TYPE:\"cm:rating\") AND NOT ASPECT:\"sys:hidden\"";
	private static final String SITE_QUERY = "((PATH:\"/app:company_home/st:sites/cm:test/*//*\" AND (alfresco  AND (+TYPE:\"cm:content\" +TYPE:\"cm:folder\"))) AND -TYPE:\"cm:thumbnail\" AND -TYPE:\"cm:failedThumbnail\" AND -TYPE:\"cm:rating\") AND NOT ASPECT:\"sys:hidden\"";
    private static final String TEST_SITE_NAME = "test";
    private static final String REPO_SEARCH_TERM = "report";
    private static final String ALL_SITES_SEARCH_TERM = "scrum";
    private static final String SITE_SEARCH_TERM = "alfresco";
    private static final Integer NUMBER_OF_RESULTS = 9;
    private static final String USERNAME = "admin";
    
    private SearchQueryParserImpl searchQueryParser;
	private SearchTerm searchTerm;
	
	@Before
	public void setUp() 
	{
		searchQueryParser = new SearchQueryParserImpl();
		
		searchTerm = new SearchTerm();
		searchTerm.setQuery(REPO_QUERY);
		searchTerm.setNumberOfResults(NUMBER_OF_RESULTS);
		searchTerm.setSearchScope(SearchConstants.SEARCH_SCOPE_REPO);
		searchTerm.setTerm(REPO_SEARCH_TERM);
		searchTerm.setSearchDate(new Date());
		searchTerm.setUserName(USERNAME);
	}
	
	/**
	 * Test that ensures the 'repo' search scope is returned when a repository search
	 * query String is passed to the 'getSearchScopeFromQueryStr' method.
	 */
	@Test
	public void getRepoSearchScopeFromQueryStr() 
	{
		String repoSearchScope = searchQueryParser.getSearchScopeFromQueryStr(REPO_QUERY);
		assertEquals(repoSearchScope, SearchConstants.SEARCH_SCOPE_REPO);
	}
	
	/**
	 * Test that ensures the 'allSites' search scope is returned when an 'all sites' search
	 * query String is passed to the 'getSearchScopeFromQueryStr' method.
	 */
	@Test
	public void getAllSitesSearchScopeFromQueryStr() 
	{
		String allSitesSearchScope = searchQueryParser.getSearchScopeFromQueryStr(ALL_SITES_QUERY);
		assertEquals(allSitesSearchScope, SearchConstants.SEARCH_SCOPE_ALL_SITES);
	}
	
	/**
	 * Test to ensure that if null is passed to getSearchScopeFromQueryStr as the query an 
	 * exception is thrown.
	 */
	@Test(expected=IllegalArgumentException.class)  
	public void getSearchScopeFormNullInput() 
	{
		searchQueryParser.getSearchScopeFromQueryStr(null);
	}
	
	/**
	 * Test that ensures the site name is returned when a individual site search
	 * query String is passed to the 'getSearchScopeFromQueryStr' method.
	 */
	@Test
	public void getSiteSearchScopeFromQueryString() 
	{
		String siteSearchScope = searchQueryParser.getSearchScopeFromQueryStr(SITE_QUERY);
		assertEquals(siteSearchScope, TEST_SITE_NAME);
	}
	
	/**
	 * Ensures that that the correct search term is returned when a repository search
	 * query String is passed to the 'getSearchTermFromQueryStr' method.
	 */
	@Test
	public void getSearchTermFromRepoQueryStr()
	{
		String searchTerm = 
				searchQueryParser.getSearchTermFromQueryStr(REPO_QUERY, SearchConstants.SEARCH_SCOPE_REPO);
		assertEquals(searchTerm, REPO_SEARCH_TERM);
	}
	
	/**
	 * Ensures that that correct search term is returned when an 'all sites' search
	 * query String is passed to the 'getSearchTermFromQueryStr' method.
	 */
	@Test
	public void getSearchTermFromAllSitesQueryStr()
	{
		String searchTerm = 
				searchQueryParser.getSearchTermFromQueryStr(ALL_SITES_QUERY, SearchConstants.SEARCH_SCOPE_ALL_SITES);
		assertEquals(searchTerm, ALL_SITES_SEARCH_TERM);
	}
	
	/**
	 * Ensures that that correct search term is returned when an individual site search
	 * query String is passed to the 'getSearchTermFromQueryStr' method.
	 */
	@Test
	public void getSearchTermFromSiteQueryStr() 
	{
		String searchTerm = 
				searchQueryParser.getSearchTermFromQueryStr(SITE_QUERY, "");
		assertEquals(searchTerm, SITE_SEARCH_TERM);
	}	
	
	/**
	 * Test to ensure that if null is passed to getSearchTermFromQueryStr as the query an 
	 * exception is thrown.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void getSearchTermFromNullInput()
	{
		searchQueryParser.getSearchTermFromQueryStr(null, null);
	}
	
	/**
	 * Test to ensure that the search term properties map is populated correctly.
	 */
	@Test
	public void getSearchTermProperties()
	{
		Map<QName, Serializable> properties = 
				searchQueryParser.getSearchTermProperties(searchTerm);
		
		assertNotNull(properties.get(ContentModel.PROP_NAME));
		assertEquals(properties.get(SearchModel.PROP_TERM), REPO_SEARCH_TERM);
		assertEquals(properties.get(SearchModel.PROP_SEARCH_SCOPE), SearchConstants.SEARCH_SCOPE_REPO);
		assertEquals(properties.get(SearchModel.PROP_NUMBER_OF_RESULTS), NUMBER_OF_RESULTS);
		assertNotNull(properties.get(SearchModel.PROP_SEARCH_DATE));
		assertEquals(properties.get(SearchModel.PROP_USER_NAME), USERNAME);
	}
	
	/**
	 * Test to ensure that if the 'term' is null in the SearchTerm object the search terms properties
	 * map is returned as null.
	 */
	@Test
	public void getSearchTermPropertiesWithNullTerm() 
	{
		searchTerm.setTerm(null);
		Map<QName, Serializable> properties = 
				searchQueryParser.getSearchTermProperties(searchTerm);
		assertNull(properties);	
	}
}
