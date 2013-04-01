package com.ixxus.alfresco.search.service;

import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ixxus.alfresco.search.util.SearchConstants;

/**
 * This service handles the retrieval and creation of folders that are used to contain 
 * the SearchTerm objects which are displayed in the Search Terms Dashlet.
 * 
 * <p>
 * A parent folder 'Search Terms' is created in the Data Dictionary. Each day a child folder
 * is created under the 'Search Terms' folder, the child folder will contain all of the
 * <code>SearchTerm</code> objects that were executed that day. 
 * </p>
 * 
 * @author dantuffery
 */

public class SearchTermFolderServiceImpl implements SearchTermFolderService
{
	private static final Log logger = LogFactory.getLog(SearchTermFolderServiceImpl.class);
	
	private FileFolderService fileFolderService;
	private String searchTermsFolderPath;
	private NodeRef searchTermsFolder;
	private NodeService nodeService;
	private Repository repository;
	
	public void setFileFolderService(FileFolderService fileFolderService) 
	{
		this.fileFolderService = fileFolderService;
	}
	
	public void setSearchTermsFolderPath(String searchTermsFolderPath) 
	{
		this.searchTermsFolderPath = searchTermsFolderPath;
	}
	
	public void setNodeService(NodeService nodeService) 
	{
		this.nodeService = nodeService;
	}

	public void setRepository(Repository repository) 
	{
		this.repository = repository;
	}

	/*
	 * (non-Javadoc)
	 * @see com.ixxus.alfresco.search.service.SearchTermFolderService#getCurrentSearchTermsFolder(org.alfresco.service.cmr.repository.NodeRef)
	 */
	public NodeRef getCurrentSearchTermsFolder(NodeRef searchTermsFolder)
	{
		if (searchTermsFolder == null)
		{
			throw new IllegalArgumentException("searchTermsFolder param must be supplied");
		}
		
		String currentSearchTermsFolderName = getFolderName(new Date());
		
		if (logger.isDebugEnabled())
		{
			logger.debug("Getting current folder " +  currentSearchTermsFolderName);
		}
		
		NodeRef currentSearchTermsFolder = getCurrentFolderNodeRef(searchTermsFolder, currentSearchTermsFolderName);

		//If current search term folder doesn't exist create it.
		if (currentSearchTermsFolder == null)
		{
			if (logger.isDebugEnabled())
			{
				logger.debug("Current folder does not exist, so creating it");
			}
			currentSearchTermsFolder = fileFolderService.create(searchTermsFolder, currentSearchTermsFolderName, 
					ContentModel.TYPE_FOLDER).getNodeRef();
		}
		
		if (logger.isDebugEnabled() && currentSearchTermsFolder != null)
		{
			logger.debug("Current folder node ref is " + currentSearchTermsFolder.toString());
		}
		
		return currentSearchTermsFolder;
	}
	
	
	/**
	 * Uses the <code>searchTermsFolderPath</code> to query the database for 
	 * the Search Terms folder.
	 */
	private void getSearchTermsFolderNodeRef()
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Getting Search Terms folder");
		}
		
		NodeRef companyHomeNodeRef = repository.getCompanyHome();
		
		if (companyHomeNodeRef == null)
		{
			if(logger.isDebugEnabled())
			{
				logger.debug("Company node ref was not found.");
			}
			return;
		}
		
		NodeRef result = null;
		NodeRef dataDictionaryNodeRef = null;
        StringTokenizer t = new StringTokenizer(this.searchTermsFolderPath, "/");
        
        if (t.hasMoreTokens())
        {
            result = companyHomeNodeRef;
            while (t.hasMoreTokens() && result != null)
            {
                String name = t.nextToken();
                if(name.equals("Search Terms"))
                {
                	dataDictionaryNodeRef = result;
                }
                
                try
                {
                	if(logger.isDebugEnabled())
                	{
                		logger.debug("Getting child folder with name: " + name);
                	}
                	
                    result = nodeService.getChildByName(result, ContentModel.ASSOC_CONTAINS, name);
                }
                catch (AccessDeniedException e)
                {
                	logger.error("An error occurred trying whilst trying to retrive a child node", e);
                	result = null;
                }
                finally
                {
                	companyHomeNodeRef = null;
                }
            }
        }
        
        if (result == null && dataDictionaryNodeRef != null)
        {
        	if (logger.isDebugEnabled())
        	{
        		logger.debug("The Search Terms folder does not exist, so creating it.");
        	}
        	
        	FileInfo fileFolderInfo = fileFolderService.create(dataDictionaryNodeRef, 
        			SearchConstants.SEARCH_TERMS_FOLDER_NAME, ContentModel.TYPE_FOLDER);
        	
        	if (fileFolderInfo != null)
        	{
        		result = fileFolderInfo.getNodeRef();
        	}
        }
        setSearchTermsFolder(result);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ixxus.alfresco.search.service.SearchTermFolderService#getCurrentFolderNodeRef(org.alfresco.service.cmr.repository.NodeRef, java.lang.String)
	 */
	public NodeRef getCurrentFolderNodeRef(NodeRef parent, String folderName)
	{
		List<ChildAssociationRef> children = 
				nodeService.getChildAssocs(parent, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
		
		NodeRef currentSearchTermsFolder = null;
		for (ChildAssociationRef child : children)
		{
			if (nodeService.getProperty(child.getChildRef(), ContentModel.PROP_NAME).equals(folderName))
			{
				currentSearchTermsFolder = child.getChildRef();
				break;
			}
		}
		
		return currentSearchTermsFolder;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ixxus.alfresco.search.service.SearchTermFolderService#getFolderName(java.util.Date)
	 */
	public String getFolderName(Date date)
	{
		return SearchConstants.CURRENT_SEARCH_TERMS_FOLDER_NAME_PREFIX + SearchConstants.sdf.format(date);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ixxus.alfresco.search.service.SearchTermFolderService#getSearchTermsFolder()
	 */
	public NodeRef getSearchTermsFolder() 
	{
		if (searchTermsFolder == null)
		{
			getSearchTermsFolderNodeRef();
		}
		return searchTermsFolder;
	}

	public void setSearchTermsFolder(NodeRef searchTermsFolder) 
	{
		this.searchTermsFolder = searchTermsFolder;
	}
}
