package com.ixxus.alfresco.search.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


import org.alfresco.model.ContentModel;
import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.namespace.QName;
import org.alfresco.service.namespace.RegexQNamePattern;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.runners.MockitoJUnitRunner;
import static org.junit.Assert.*;

import com.ixxus.alfresco.search.SearchTermTest;
import com.ixxus.alfresco.search.util.SearchConstants;

import static org.mockito.Mockito.*;

/**
 * @author dantuffery
 */
@RunWith(MockitoJUnitRunner.class)
public class SearchTermFolderServiceTest implements SearchTermTest
{
	private SearchTermFolderServiceImpl searchTermFolderService;
	private NodeRef dummyNodeRef;
	
	@Mock private ChildAssociationRef mockedChildAssociationRef;
	@Mock private FileFolderService mockedFileFolderService;
	@Mock private FileInfo mockedFileInfo;
	@Mock private NodeService mockedNodeService;
	@Mock private Repository mockedRepository;
	
	
	@Before
	public void setUp()
	{
		//Initalise mocks
		MockitoAnnotations.initMocks(SearchTermFolderServiceTest.class);
		
		searchTermFolderService = new SearchTermFolderServiceImpl();
		searchTermFolderService.setNodeService(mockedNodeService);
		searchTermFolderService.setRepository(mockedRepository);
		searchTermFolderService.setFileFolderService(mockedFileFolderService);
		searchTermFolderService.setSearchTermsFolderPath(SEARCH_TERMS_FOLDER_PATH);
		
		dummyNodeRef = new NodeRef(DUMMY_NODE_REF);
		
		List<ChildAssociationRef>  children = new ArrayList<ChildAssociationRef>();
		children.add(mockedChildAssociationRef);
		
		//common mocks
		when(mockedNodeService.getProperty(dummyNodeRef, ContentModel.PROP_NAME)).thenReturn(FOLDER_NAME);
		when(mockedChildAssociationRef.getChildRef()).thenReturn(dummyNodeRef);
		when(mockedNodeService.getChildAssocs(dummyNodeRef, ContentModel.ASSOC_CONTAINS, 
				RegexQNamePattern.MATCH_ALL)).thenReturn(children);
	}
	
	/**
	 * Test to retrieve the current daily search terms folder, will test the creation
	 * of a new daily folder.
	 */
	@Test
	public void getCurrentSearchTermsFolder() 
	{
		when(mockedNodeService.getChildAssocs(dummyNodeRef, ContentModel.ASSOC_CONTAINS, 
				RegexQNamePattern.MATCH_ALL)).thenReturn(new ArrayList<ChildAssociationRef>());
		when(mockedFileFolderService.create(any(NodeRef.class), anyString(), 
				any(QName.class))).thenReturn(mockedFileInfo);
		when(mockedFileInfo.getNodeRef()).thenReturn(dummyNodeRef);
		
		NodeRef currentSearchTermsFolder = searchTermFolderService.getCurrentSearchTermsFolder(dummyNodeRef);
		assertNotNull(currentSearchTermsFolder);
	}
	
	/**
	 * Test to ensure that an IllegalArgumentExption is thrown if a null object is 
	 * passed as a parameter to the getCurrentSearchTermsFolder method.
	 */
	@Test(expected=IllegalArgumentException.class)
	public void getCurrentSearchTermsFolderWithNullInput() {
		searchTermFolderService.getCurrentSearchTermsFolder(null);
	}
    
	/**
	 * Test that ensures the daily search terms folder is created.
	 */
	@Test
	public void getCurrentFolderNodeRef()
	{
		NodeRef currentFolderNodeRef = searchTermFolderService.getCurrentFolderNodeRef(dummyNodeRef, FOLDER_NAME);
		
		assertEquals(currentFolderNodeRef, dummyNodeRef);
	}
	
	/**
	 * Test to ensure the folder name is in the correct format.
	 */
	@Test
	public void getFolderName()
	{
		Date date = new Date();
		String folderName = searchTermFolderService.getFolderName(date);
		
		assertEquals(folderName, SearchConstants.CURRENT_SEARCH_TERMS_FOLDER_NAME_PREFIX + 
				SearchConstants.sdf.format(date));
	}
	
	/**
	 * Test to get the parent 'Search Terms' folder.
	 */
	@Test
	public void getSearchTermsFolder()
	{
		when(mockedRepository.getCompanyHome()).thenReturn(dummyNodeRef);
		when(mockedNodeService.getChildByName(any(NodeRef.class), any(QName.class), 
				anyString())).thenReturn(dummyNodeRef);
		
		NodeRef searchTermsFolderNodeRef = searchTermFolderService.getSearchTermsFolder();
		
		assertEquals(searchTermsFolderNodeRef, dummyNodeRef);
	}
}
