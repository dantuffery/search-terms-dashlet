package com.ixxus.alfresco.search.service;

import java.util.Date;

import org.alfresco.service.cmr.repository.NodeRef;

/**
 * @author dantuffery
 */

public interface SearchTermFolderService 
{
	/**
	 * A daily folder is created in 'Data Dictionary/Search Terms' to store the search terms. 
	 * This method is used to retrieve the daily search terms folder, or if it does not exist 
	 * the folder will be created.
	 * 
	 * @return NodeRef searchTermsFolder
	 */
	NodeRef getCurrentSearchTermsFolder(NodeRef searchTermsFolder);
	
	/**
	 * Returns the current search terms folder NodeRef.
	 * 
	 * A daily search term folder is created, this method returns today's search
	 * term folder. Does not create a new folder if one doesn't not exist already.
	 * 
	 * @param parent
	 * @param folderName
	 * @return NodeRef
	 */
	NodeRef getCurrentFolderNodeRef(NodeRef parent, String folderName);
	
	/**
	 * Gets the folder name which made up of the prefix 'search-terms-' and
	 * the current date in the format 'yyyy-MM-dd'.
	 * 
	 * @param date
	 * @return String
	 */
	String getFolderName(Date date);
	
	/**
	 * Get the search terms folder.
	 * 
	 * Returns the 'Search Terms' folder in the Data Dictionary. It is the parent folder
	 * for all daily search term folders that are created.
	 */
	NodeRef getSearchTermsFolder();
}
