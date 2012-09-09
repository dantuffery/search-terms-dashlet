package com.ixxus.alfresco.search.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

/**
 * Constants for the search terms classes.
 * 
 * @author dtuffery
 */
public interface SearchConstants 
{
	public static final String SEARCH_SCOPE_ALL_SITES = "allSites";
	public static final String SEARCH_SCOPE_REPO = "repo";
	public static final String SEARCH_TERMS_FOLDER_NAME = "Search Terms";
	public static final String CURRENT_SEARCH_TERMS_FOLDER_NAME_PREFIX = "search-terms-";
	                                              
	public static final String SITES_QUERY_STR = "st:sites/cm:";
	public static final String ALL_SITES_QUERY_STR = "st:sites/*";
	public static final String AND_BRACKET = "AND (";
	public static final String DOUBLE_OPEN_BRACKET = "((";
	public static final String AND_BRACKET_PLUS = "AND (+";
	public static final String AND = "AND";
	public static final String SLASH = "/";
	
	public static final DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
}
