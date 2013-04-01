package com.ixxus.alfresco.parsers;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.service.namespace.QName;

import com.ixxus.alfresco.search.model.SearchTerm;

/**
 * @author dantuffery
 */

public interface SearchQueryParser 
{
	/**
	 * From the given Search term object parse the search query and return
	 * a Map of properties. 
	 * 
	 * @param SearchTerm
	 */
	Map<QName, Serializable> parseSearchQuery(SearchTerm searchQuery);
}