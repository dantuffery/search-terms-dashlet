package com.ixxus.alfresco.search.service;

import com.ixxus.alfresco.search.model.SearchTerm;

/**
 * @author dantuffery
 */
public interface SearchTermService 
{
	/**
	 * Adds a search term object to the <code>searchQueries</code> queue.
	 * 
	 * @param searchQuery
	 */
	void addSearchQuery(SearchTerm searchQueries);
}
