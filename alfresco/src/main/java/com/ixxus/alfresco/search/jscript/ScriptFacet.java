package com.ixxus.alfresco.search.jscript;

import java.util.List;

import org.alfresco.repo.jscript.Scopeable;
import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.search.SearchParameters.FieldFacet;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.alfresco.service.namespace.NamespacePrefixResolverProvider;
import org.alfresco.util.Pair;
import org.mozilla.javascript.Scriptable;

/**
 * ScriptFacet class to represent a facet field and its properties.
 * 
 * @author dtuffery
 */

public class ScriptFacet implements Scopeable, NamespacePrefixResolverProvider{

	private static final long serialVersionUID = 1L;
	
    private Scriptable scope;
    private Boolean isFacet = true;
    private FieldFacet facetField;
    private ServiceRegistry services;
    private List<Pair<String, Integer>> facetList;
    private Boolean facetHasAnyHits;

    
    public ScriptFacet(FieldFacet facetField, List<Pair<String, Integer>> facetList, Scriptable scope, ServiceRegistry services)
    {
    	if (facetField == null)
        {
            throw new IllegalArgumentException("Facet name must be supplied.");
        }
        
        if (facetList == null)
        {
            throw new IllegalArgumentException("Facet list must be supplied.");
        }
        
    	this.facetField = facetField;
    	this.facetList = facetList;
    	this.scope = scope;
    }
    
    /**
     * Set the scriptable oject.
     * 
     * @param scope
     */
	@Override
    public void setScope(Scriptable scope)
    {
        this.scope = scope;
    }

	@Override
	public NamespacePrefixResolver getNamespacePrefixResolver() 
	{
		return this.services.getNamespaceService();
	}

	/**
	 * If no facets are found for a field Solr will return a empty list. 
	 * This method checks to see if the facet list is empty. if so, a 
	 * false Boolean value is returned to indicate that the facet should 
	 * not be displayed.
	 *  
	 * @return Boolean
	 */
	public Boolean getFacetHasAnyHits() 
	{
		if(this.facetList != null && facetList.size() > 0 )
		{
			this.facetHasAnyHits = true;
		}
		return facetHasAnyHits;
	}
	
	public void setFacetHasAnyHits(Boolean facetHasAnyHits) 
	{
		this.facetHasAnyHits = facetHasAnyHits;
	}

	public Boolean getIsFacet() 
	{
		return isFacet;
	}

	public void setIsFacet(Boolean isFacet) 
	{
		this.isFacet = isFacet;
	}

	public FieldFacet getFacetField() 
	{
		return facetField;
	}

	public void setFacetField(FieldFacet facetField)
	{
		this.facetField = facetField;
	}

	public List<Pair<String, Integer>> getFacetList() 
	{
		return facetList;
	}

	public void setFacetList(List<Pair<String, Integer>> facetList) 
	{
		this.facetList = facetList;
	}	
}
