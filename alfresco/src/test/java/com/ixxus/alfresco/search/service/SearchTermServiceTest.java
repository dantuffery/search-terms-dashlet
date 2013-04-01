package com.ixxus.alfresco.search.service;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.content.MimetypeMap;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.transaction.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import static org.mockito.Mockito.*;

import com.ixxus.alfresco.parsers.SearchQueryParser;
import com.ixxus.alfresco.parsers.SearchQueryParserImpl;
import com.ixxus.alfresco.search.SearchTermTest;
import com.ixxus.alfresco.search.model.SearchTerm;

/**
 * @author dantuffery
 */

@RunWith(MockitoJUnitRunner.class)
public class SearchTermServiceTest implements SearchTermTest 
{
	
	public static final Integer MAX_NUMBER_OF_DAYS = 29;
	
	private SearchTermServiceImpl searchTermService;
	private SearchQueryParser searchQueryParser;
	private NodeRef dummyNodeRef;
	
	@Mock private SearchTermFolderService mockedSearchTermFolderService;
	@Mock private ChildAssociationRef mockedChildAssociationRef;
	@Mock private TransactionService mockedTransactionService;
	@Mock private SearchQueryParser mockedSearchQueryParser;
	@Mock private ContentService mockedContentService;
	@Mock private ContentWriter mockedContentWriter;
	@Mock private NodeService mockedNodeService;
	
	
	@Before
	public void setUp() 
	{
		//Initialise mocks
		MockitoAnnotations.initMocks(SearchTermServiceTest.class);
	
		searchTermService = new SearchTermServiceImpl();
		searchQueryParser = new SearchQueryParserImpl();
		
		searchTermService.setSearchTermFolderService(mockedSearchTermFolderService);
		searchTermService.setTransactionService(mockedTransactionService);
		searchTermService.setContentService(mockedContentService);
		searchTermService.setSearchQueryParser(searchQueryParser);
		searchTermService.setNodeService(mockedNodeService);
		searchTermService.setMaxNumberOfDays(MAX_NUMBER_OF_DAYS);
		
		dummyNodeRef = new NodeRef(DUMMY_NODE_REF);
		
		//create search term object and add it to the queue
		SearchTerm searchTerm = new SearchTerm(REPO_QUERY, 2, new Date(), "admin");
		searchTermService.addSearchQuery(searchTerm);
		
		//common mocks
		when(mockedSearchTermFolderService.getSearchTermsFolder()).thenReturn(dummyNodeRef);
		when(mockedSearchTermFolderService.getCurrentSearchTermsFolder(dummyNodeRef)).thenReturn(dummyNodeRef);
	}
	
	/**
	 * Test functionality that saves a SearchTerm object..
	 * 
	 * @throws ExecutionException
	 */
	@Test
	public void saveSearchTerm() throws ExecutionException
	{
		when(mockedChildAssociationRef.getChildRef()).thenReturn(dummyNodeRef); 
		when(mockedNodeService.createNode(
				any(NodeRef.class), 
				any(QName.class), 
				any(QName.class),
				any(QName.class), 
				Mockito.<Map<QName, Serializable>> any())).thenReturn(mockedChildAssociationRef);
		
		when(mockedContentService.getWriter(dummyNodeRef, ContentModel.PROP_CONTENT, true)).thenReturn(mockedContentWriter);
		
		searchTermService.saveSearchTerms();
		
		//verify that the call to save the search term node was executed.
		verify(mockedNodeService, times(1)).createNode(
				any(NodeRef.class), 
				any(QName.class), 
				any(QName.class),
				any(QName.class), 
				Mockito.<Map<QName, Serializable>> any()); 
		
		//verify that the template node's content is saved
		verify(mockedContentWriter, times(1)).setMimetype(MimetypeMap.MIMETYPE_TEXT_PLAIN);
		verify(mockedContentWriter, times(1)).putContent((String) anyString());		
	}
	
	/**
	 * Test functionality that deletes daily search terms folder.
	 */
	@Test
	public void deleteSearchTerms()
	{
		when(mockedSearchTermFolderService.getCurrentFolderNodeRef(any(NodeRef.class), anyString())).thenReturn(dummyNodeRef);
		
		searchTermService.deleteSearchTerms();
		
		//verify that the temporary aspect gets applied and the search terms folder is deleted.
		verify(mockedNodeService, times(1)).addAspect(dummyNodeRef, ContentModel.ASPECT_TEMPORARY, null);
    	verify(mockedNodeService, times(1)).deleteNode(dummyNodeRef);	
	}
}
