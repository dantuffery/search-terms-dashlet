package com.ixxus.alfresco.search.jscript;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.util.Pair;
import org.junit.Before;
import org.junit.Test;

/**
 * @author dantufery
 */

public class ScriptFacetTest {

	private ScriptFacet scriptFacet;
	private List<Pair<String, Integer>> facetList;
	
	@Before
	public void setUp()
	{
		facetList = new ArrayList<Pair<String, Integer>>();
		facetList.add(new Pair<String, Integer>("alfresco", new Integer(11)));
		facetList.add(new Pair<String, Integer>("test", new Integer(1)));
		scriptFacet = new ScriptFacet(new FieldFacet("@{http://www.ixxus.com/model/ixxus/1.0}term.__.u"), 
				facetList, null, null);
	}
	
	/**
	 * Test to ensure that a 'true' Boolean value is returned if the facetList is populated.
	 */
	@Test
	public void facetHasHits() 
	{
		assertTrue(scriptFacet.getFacetHasAnyHits());
	}
	
	/**
	 * Test to ensure a 'false' Boolean value is returned if the facetList is empty
	 */
	@Test
	public void facetHasNoHits()
	{
		scriptFacet.setFacetList(new ArrayList<Pair<String, Integer>>());
		assertFalse(scriptFacet.getFacetHasAnyHits());
	}

}
