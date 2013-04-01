package com.ixxus.alfresco.search;

/**
 * dantuffery
 */

public interface SearchTermTest {
	
	static final String REPO_QUERY = "((report  AND (+TYPE:\"cm:content\" +TYPE:\"cm:folder\")) AND -TYPE:\"cm:thumbnail\" AND -TYPE:\"cm:failedThumbnail\" AND -TYPE:\"cm:rating\") AND NOT ASPECT:\"sys:hidden\"";
	static final String DUMMY_NODE_REF = "workspace://SpacesStore/990be519-edf9-4c8d-94d3-ca21166c685e";
	static final String REPO_SEARCH_TERM = "report";
	static final String FOLDER_NAME = "folderName"; 
	static final String SEARCH_TERMS_FOLDER_PATH = "Data Dictionary/Search Terms";
	static final String SEARCH_TERMS_FOLDER_NAME = "Search Terms";

}
