package com.ixxus.alfresco.search.service;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutionException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.model.Repository;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.security.permissions.AccessDeniedException;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ixxus.alfresco.search.model.SearchModel;
import com.ixxus.alfresco.search.model.SearchTerm;
import com.ixxus.alfresco.search.util.JobType;
import com.ixxus.alfresco.search.util.SearchConstants;

/**
 * <code>SearchTermService</code> carries out two main functions: search terms persistence 
 * and search terms deletion.
 * <p>
 * When a search is executed within the repository a <code>SearchTerm</code> object
 * that contains the search term and properties relating to the search term are added 
 * to the <code>searchQueries</code> queue. A job will execute every minute and persist 
 * the search terms in the queue to the database. Only user search terms are saved by 
 * this service, system searches are identified and ignored.
 * <p>
 * Another job will run every day at 00:01 and retrieve and delete all of the search 
 * terms that were created after the current day - <code>maxNumberOfDays</code>.  
 *  
 * @author dtuffery
 *
 */
public class SearchTermServiceImpl implements SearchTermService
{
	private static final Log logger = LogFactory.getLog(SearchTermServiceImpl.class);
	
	private Queue<SearchTerm> searchQueries = new LinkedList<SearchTerm>();
	private TransactionService transactionService;
	private FileFolderService fileFolderService;
	private ContentService contentService;
	private NodeService nodeService;
	private Repository repository;
	private Integer maxNumberOfDays;
	private String searchTermsFolderPath;
	private NodeRef searchTermsFolder;

	public void setTransactionService(TransactionService transactionService) 
	{
		this.transactionService = transactionService;
	}
	
	public void setFileFolderService(FileFolderService fileFolderService) 
	{
		this.fileFolderService = fileFolderService;
	}
	
	public void setContentService(ContentService contentService) 
	{
		this.contentService = contentService;
	}

	public void setNodeService(NodeService nodeService) 
	{
		this.nodeService = nodeService;
	}

	public void setSearchTermsFolderPath(String searchTermsFolderPath) 
	{
		this.searchTermsFolderPath = searchTermsFolderPath;
	}
	
	public void setRepository(Repository repository) 
	{
		this.repository = repository;
	}
	
	public void setMaxNumberOfDays(Integer maxNumberOfDays) 
	{
		this.maxNumberOfDays = maxNumberOfDays;
	}

	/**
	 * Called every minute by the <code>saveJobDetail<code> bean
	 * and executes <code>doExecute<code> passing the save job type 
	 * as a parameter.
	 * 
	 * @throws ExecutionException
	 */
	public void saveJobExecute() throws ExecutionException 
	{
		doExecute(JobType.SEARCH_TERM_SAVE);
	}
	
	/**
	 * Called once a day by the <code>deleteJobDetail<code> bean
	 * and executes <code>doExecute<code> passing the delete job type 
	 * as a parameter.
	 * 
	 * @throws ExecutionException
	 */
	public void deleteJobExecute() throws ExecutionException
	{
		doExecute(JobType.SEARCH_TERM_DELETE);
	}
	
	public void doExecute(final JobType jobType) throws ExecutionException 
	{
		final RetryingTransactionCallback<Object> searchTermCallback = new RetryingTransactionCallback<Object>() {
		    public Object execute() throws Throwable 
		    {
		    	if(jobType == JobType.SEARCH_TERM_SAVE)
		    	{
		    		saveSearchTerms();
		    	}
		    	else if(jobType == JobType.SEARCH_TERM_DELETE)
		    	{
		    		deleteSearchTerms();
		    	}
		    	return null;
		    }
		};
		
		final RunAsWork<Object> searchTermRunAsWork = new RunAsWork<Object>() 
		{
			public Object doWork() throws Exception 
			{
				transactionService.getRetryingTransactionHelper().doInTransaction(searchTermCallback, false);
				return null;
			}
		};
		
		AuthenticationUtil.runAs(searchTermRunAsWork, AuthenticationUtil.SYSTEM_USER_NAME);		
	}
	
	/**
	 * Iterates over the <code>searchQueries</code> queue and process each
	 * <code>SearchTerm<code> object in the queue.
	 */
	private void saveSearchTerms()
	{
		for(int i = 0; i < this.searchQueries.size(); i++)
		{
			SearchTerm searchQuery = this.searchQueries.poll();
		
			if(logger.isDebugEnabled())
			{
				logger.debug("Saving search term....");
			}
		
			Map<QName, Serializable> properties = getSearchTermProperties(searchQuery);
		    
			/**
			 * If the properties are null it means that the the search was a system search
			 * (no search term was found in the query string) and is therefore ignored.
			 */
			if(properties == null)
			{
				continue;
			}
		
			NodeRef currentSearchTermFolder = getCurrentSearchTermsFolder();
		
			if(currentSearchTermFolder == null)
			{
				continue;
			}
  
		    String fileName = (String) properties.get(ContentModel.PROP_NAME);
		    
			NodeRef searchTermNodeRef = nodeService.createNode(
					currentSearchTermFolder, 
					ContentModel.ASSOC_CONTAINS, 
					QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(fileName)), 
					SearchModel.TYPE_SEARCH_TERM, 
					properties).getChildRef(); 
			
			addNodeContent(searchTermNodeRef, fileName);
		 
			if(logger.isDebugEnabled())
			{
				logger.debug("Successfully saved search term.....");
			}
		}
	}
	
	/**
	 * Creates a map of the search term properties from the given 
	 * <code>SearchTerm</code> object.
	 * 
	 * @param searchQuery
	 * @return Map<QName, Serializable>
	 */
	private Map<QName, Serializable> getSearchTermProperties(SearchTerm searchQuery)
	{
		String query = searchQuery.getQuery();
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Getting search term properties from query: " + query);
		}
		
		String searchScope = getSearchScopeFromQueryStr(query);
		String term = getSearchTermFromQueryStr(query, searchScope);
		
		if(term == null)
		{
			if(logger.isDebugEnabled())
			{
				logger.debug("No search term found, aborting");
			}
			return null;
		}
		
		Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
		properties.put(ContentModel.PROP_NAME, "searchterm-" + new Date().getTime());
		properties.put(SearchModel.PROP_TERM, term.trim());
		properties.put(SearchModel.PROP_SEARCH_SCOPE, searchScope);
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
	private String getSearchScopeFromQueryStr(String query)
	{
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
	private String getSearchTermFromQueryStr(String query, String searchScope)
	{
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
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Search term is " + term);
		}
		
		return term;
	}
	 
	/**
	 * Uses the <code>searchTermsFolderPath</code> to query the database for 
	 * the Search Terms folder.
	 */
	private void getSearchTermsFolderNodeRef()
	{
		if(logger.isDebugEnabled())
		{
			logger.debug("Getting Search Terms folder");
		}
		
		NodeRef companyHomeNodeRef = repository.getCompanyHome();
		
		if(companyHomeNodeRef == null)
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
        
        if(result == null && dataDictionaryNodeRef != null)
        {
        	if(logger.isDebugEnabled())
        	{
        		logger.debug("The Search Terms folder does not exist, so creating it.");
        	}
        	FileInfo fileFolderInfo = fileFolderService.create(dataDictionaryNodeRef, 
        			SearchConstants.SEARCH_TERMS_FOLDER_NAME, ContentModel.TYPE_FOLDER);
        	if(fileFolderInfo != null)
        	{
        		result = fileFolderInfo.getNodeRef();
        	}
        }
        setSearchTermsFolder(result);
	}
	
	/**
	 * A daily folder is created in 'Data Dictionary/Search Terms' to store the search terms. 
	 * This method is used to retrieve the daily search terms folder, or if it does not exist 
	 * the folder will be created.
	 * 
	 * @return NodeRef
	 */
	private NodeRef getCurrentSearchTermsFolder()
	{
		String currentSearchTermsFolderName = getFolderName(new Date());
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Getting current folder " +  currentSearchTermsFolderName);
		}
		
		NodeRef searchTermsFolder = getSearchTermsFolder();
		if(searchTermsFolder == null)
		{
			return null;
		}
		
		NodeRef currentSearchTermsFolder = getCurrentFolderNodeRef(searchTermsFolder, currentSearchTermsFolderName);

		//If current search term folder doesn't exist create it.
		if(currentSearchTermsFolder == null)
		{
			if(logger.isDebugEnabled())
			{
				logger.debug("Current folder does not exist, so creating it");
			}
			currentSearchTermsFolder = fileFolderService.create(searchTermsFolder, currentSearchTermsFolderName, 
					ContentModel.TYPE_FOLDER).getNodeRef();
		}
		
		if(logger.isDebugEnabled() && currentSearchTermsFolder != null)
		{
			logger.debug("Current folder node ref is " + currentSearchTermsFolder.toString());
		}
		return currentSearchTermsFolder;
	}
	
	/**
	 * Returns current folder NodeRef
	 * 
	 * @param parent
	 * @param folderName
	 * @return NodeRef
	 */
	public NodeRef getCurrentFolderNodeRef(NodeRef parent, String folderName)
	{
		List<ChildAssociationRef> children = 
				nodeService.getChildAssocs(parent, ContentModel.ASSOC_CONTAINS, RegexQNamePattern.MATCH_ALL);
		
		NodeRef currentSearchTermsFolder = null;
		for(ChildAssociationRef child : children)
		{
			if(nodeService.getProperty(child.getChildRef(), ContentModel.PROP_NAME).equals(folderName))
			{
				currentSearchTermsFolder = child.getChildRef();
				break;
			}
		}
		
		return currentSearchTermsFolder;
	}
	
	/**
	 * The daily search terms folder that is older than current day - <code>maxNumberOfDays</code>
	 * is retrieved and deleted.
	 */
	private void deleteSearchTerms()
	{
		if(logger.isDebugEnabled())
		{
			logger.debug("Deleting search terms...");
		}
		
		NodeRef searchTermsFolder = getSearchTermsFolder();
		
		if(searchTermsFolder == null)
		{
			return;
		}
		
		Date deleteDate = getDeleteDate();
		String nameOfFolderToDelete = getFolderName(deleteDate);
		
		NodeRef folderToDelete = 
				getCurrentFolderNodeRef(searchTermsFolder, nameOfFolderToDelete);

		
		if(folderToDelete == null)
		{
			if(logger.isDebugEnabled())
			{
				logger.debug("No folder called " + nameOfFolderToDelete  + " found, aborting.");
			}
			return;
		}
		
		/**
		 * Applying the temporary aspect will delete the node immediately
		 */
		nodeService.addAspect(folderToDelete, ContentModel.ASPECT_TEMPORARY, null);
		nodeService.deleteNode(folderToDelete);
		
		if(logger.isDebugEnabled())
		{
			logger.debug("Successfully deleted folder  " + nameOfFolderToDelete + "...");
		}
	}
	
	/**
	 * Calculates the deletion date: current day - <code>maxNumberOfDays</code> 
	 * 
	 * @return Date
	 */
	private Date getDeleteDate()
	{
		Calendar deleteDate = Calendar.getInstance();
		deleteDate.add(Calendar.DATE, -this.maxNumberOfDays);
		
		if(logger.isDebugEnabled())
		{
			logger.debug("The delete date is " + deleteDate.getTime());
		}
		return deleteDate.getTime();
	}
	
	/**
	 * Gets the folder name which made up of the prefix 'search-terms-' and
	 * the current date in the format 'yyyy-MM-dd'.
	 * 
	 * @param date
	 * @return String
	 */
	private String getFolderName(Date date)
	{
		return SearchConstants.CURRENT_SEARCH_TERMS_FOLDER_NAME_PREFIX + SearchConstants.sdf.format(date);
	}
	
	/**
	 * Adds a search term object to the <code>searchQueries</code> queue.
	 */
	public void addSearchQuery(SearchTerm searchQuery)
	{
		searchQueries.add(searchQuery);
	}
	
	/**
	 * Creates content in the repository for the give node.
	 * 
	 * @param nodeRef
	 * @param content
	 */
	private void addNodeContent(NodeRef nodeRef, String content)
	{
		if(logger.isDebugEnabled()) 
		{
			logger.debug("Creating content for nodeRef: " + nodeRef.toString());
		}
		
		ContentWriter writer = this.contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
       
        if(writer != null)
        {
        	writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        	writer.putContent((String) content);
        }
	}

	/*====== Getters and Setters =======*/
	
	public NodeRef getSearchTermsFolder() 
	{
		if(searchTermsFolder == null)
		{
			getSearchTermsFolderNodeRef();
		}
		return searchTermsFolder;
	}

	public void setSearchTermsFolder(NodeRef searchTermsFolder) 
	{
		this.searchTermsFolder = searchTermsFolder;
	}

	public Queue<SearchTerm> getSearchQueries() 
	{
		return searchQueries;
	}

	public void setSearchQueries(Queue<SearchTerm> searchQueries) 
	{
		this.searchQueries = searchQueries;
	}
}
