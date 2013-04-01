package com.ixxus.alfresco.search.service;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ExecutionException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.repo.transaction.RetryingTransactionHelper.RetryingTransactionCallback;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.ixxus.alfresco.parsers.SearchQueryParser;
import com.ixxus.alfresco.search.model.SearchModel;
import com.ixxus.alfresco.search.model.SearchTerm;
import com.ixxus.alfresco.search.util.JobType;

/**
 * <code>SearchTermService</code> carries out two main functions: search terms persistence 
 * and search terms deletion.
 * <p>
 * When a search is executed within the repository a <code>SearchTerm</code> object
 * that contains the search term, and properties relating to the search term are added 
 * to the <code>searchQueries</code> queue. A job will execute every minute and persist 
 * the search terms in the queue to the database. Only user search terms are saved by 
 * this service, system searches are identified and ignored.
 * <p>
 * Another job will run every day at 00:01 and retrieve and delete all of the search 
 * terms that were created after the current day - <code>maxNumberOfDays</code>.  
 *  
 * @author dantuffery
 *
 */
public class SearchTermServiceImpl implements SearchTermService
{
	private static final Log logger = LogFactory.getLog(SearchTermServiceImpl.class);
	
	private Queue<SearchTerm> searchQueries = new LinkedList<SearchTerm>();
	private SearchTermFolderService searchTermFolderService;
	private TransactionService transactionService;
	private SearchQueryParser searchQueryParser;
	private ContentService contentService;
	private NodeService nodeService;
	private Integer maxNumberOfDays;
	
	public void setSearchTermFolderService(
			SearchTermFolderService searchTermFolderService) {
		this.searchTermFolderService = searchTermFolderService;
	}

	public void setTransactionService(TransactionService transactionService) 
	{
		this.transactionService = transactionService;
	}
	
	public void setSearchQueryParser(SearchQueryParser searchQueryParser) 
	{
		this.searchQueryParser = searchQueryParser;
	}

	public void setContentService(ContentService contentService) 
	{
		this.contentService = contentService;
	}

	public void setNodeService(NodeService nodeService) 
	{
		this.nodeService = nodeService;
	}

	public void setMaxNumberOfDays(Integer maxNumberOfDays) 
	{
		this.maxNumberOfDays = maxNumberOfDays;
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.ixxus.alfresco.search.service.SearchTermService#addSearchQuery(com.ixxus.alfresco.search.model.SearchTerm)
	 */
	public void addSearchQuery(SearchTerm searchQuery)
	{
		searchQueries.add(searchQuery);
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
		    	if (jobType == JobType.SEARCH_TERM_SAVE)
		    	{
		    		saveSearchTerms();
		    	}
		    	else if (jobType == JobType.SEARCH_TERM_DELETE)
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
	public void saveSearchTerms()
	{
		NodeRef searchTermsFolder = searchTermFolderService.getSearchTermsFolder();
		
		if (searchTermsFolder == null)
		{
			logger.debug("No search term folder found.");
			return;
		}
		
		for (int i = 0; i < this.searchQueries.size(); i++)
		{
			SearchTerm searchQuery = this.searchQueries.poll();
		
			if (logger.isDebugEnabled())
			{
				logger.debug("Saving search term....");
			}
		
			Map<QName, Serializable> properties = searchQueryParser.parseSearchQuery(searchQuery);
		    
			/**
			 * If the properties are null it means that the the search was a system search
			 * (no search term was found in the query string) and is therefore ignored.
			 */
			if (properties == null)
			{
				logger.debug("Properties are null for " + searchQuery.toString());
				continue;
			}
		
			NodeRef currentSearchTermFolder = searchTermFolderService.getCurrentSearchTermsFolder(searchTermsFolder);
		    String fileName = (String) properties.get(ContentModel.PROP_NAME);
		    
			NodeRef searchTermNodeRef = nodeService.createNode(
					currentSearchTermFolder, 
					ContentModel.ASSOC_CONTAINS, 
					QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, QName.createValidLocalName(fileName)), 
					SearchModel.TYPE_SEARCH_TERM, 
					properties).getChildRef(); 
			
			addNodeContent(searchTermNodeRef, fileName);
		 
			if (logger.isDebugEnabled())
			{
				logger.debug("Successfully saved search term.....");
			}
		}
	}
	
	/**
	 * The daily search terms folder that is older than current day - <code>maxNumberOfDays</code>
	 * is retrieved and deleted.
	 */
	public void deleteSearchTerms()
	{
		if (logger.isDebugEnabled())
		{
			logger.debug("Deleting search terms...");
		}
		
		NodeRef searchTermsFolder = searchTermFolderService.getSearchTermsFolder();
		
		if (searchTermsFolder == null)
		{
			return;
		}
		
		Date deleteDate = getDeleteDate();
		String nameOfFolderToDelete = searchTermFolderService.getFolderName(deleteDate);
		
		NodeRef folderToDelete = 
				searchTermFolderService.getCurrentFolderNodeRef(searchTermsFolder, nameOfFolderToDelete);

		
		if (folderToDelete == null)
		{
			if (logger.isDebugEnabled())
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
		
		if (logger.isDebugEnabled())
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
		
		if (logger.isDebugEnabled())
		{
			logger.debug("The delete date is " + deleteDate.getTime());
		}
		return deleteDate.getTime();
	}
	
	/**
	 * Creates content in the repository for the give node.
	 * 
	 * @param nodeRef
	 * @param content
	 */
	private void addNodeContent(NodeRef nodeRef, String content)
	{
		if (logger.isDebugEnabled()) 
		{
			logger.debug("Creating content for nodeRef: " + nodeRef.toString());
		}
		
		ContentWriter writer = this.contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
       
        if (writer != null)
        {
        	writer.setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
        	writer.putContent((String) content);
        }
	}

	/*====== Getters and Setters =======*/

	public Queue<SearchTerm> getSearchQueries() 
	{
		return searchQueries;
	}

	public void setSearchQueries(Queue<SearchTerm> searchQueries) 
	{
		this.searchQueries = searchQueries;
	}
}
