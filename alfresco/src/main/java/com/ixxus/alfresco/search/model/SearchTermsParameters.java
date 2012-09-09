package com.ixxus.alfresco.search.model;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.search.SearchParameters;

/**
 * Extends Alfresco's <code>SearchParameters</code> class, used to 
 * support Solr filter queries.
 * 
 * @author dtuffery
 */

public class SearchTermsParameters extends SearchParameters
{
	private List<String> filterQueries = new ArrayList<String>();

	public List<String> getFilterQueries() {
		return filterQueries;
	}

	public void setFilterQueries(List<String> filterQueries) {
		this.filterQueries = filterQueries;
	}
}
