package com.ixxus.alfresco.search.model;

import org.alfresco.service.namespace.QName;

/** 
 * @author dtuffery
 */
public class SearchModel 
{
	//Namespaces
	public static final String IXXUS_MODEL_1_0_URI = "http://www.ixxus.com/model/ixxus/1.0";
	  
	//Types
    public static final QName TYPE_SEARCH_TERM = QName.createQName(IXXUS_MODEL_1_0_URI, "searchTerm");
    
    //Properties
    public static final QName PROP_TERM = QName.createQName(IXXUS_MODEL_1_0_URI, "term");
    public static final QName PROP_SEARCH_SCOPE = QName.createQName(IXXUS_MODEL_1_0_URI, "searchScope");
    public static final QName PROP_NUMBER_OF_RESULTS = QName.createQName(IXXUS_MODEL_1_0_URI, "numberOfResults");
    public static final QName PROP_SEARCH_DATE = QName.createQName(IXXUS_MODEL_1_0_URI, "searchDate");
    public static final QName PROP_USER_NAME = QName.createQName(IXXUS_MODEL_1_0_URI, "userName");
}
