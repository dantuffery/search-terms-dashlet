package com.ixxus.alfresco.search.model;

import java.util.Date;

/**
 * @author dtuffery
 *
 */
public class SearchTerm 
{
	private String term;
	private String searchScope;
	private String query;
	private String userName;
	private int numberOfResults;
	private Date searchDate;
	
	public SearchTerm() {}
	
	public SearchTerm(String query, int  numberOfResults, Date searchDate, String userName)
	{
		this.query = query;
		this.numberOfResults = numberOfResults;
		this.searchDate = searchDate;
		this.userName = userName;
	}
	
	public String getTerm() 
	{
		return term;
	}
	
	public void setTerm(String term) 
	{
		this.term = term;
	}
	
	public String getSearchScope() 
	{
		return searchScope;
	}
	
	public void setSearchScope(String searchScope) 
	{
		this.searchScope = searchScope;
	}

	public String getQuery() 
	{
		return query;
	}

	public void setQuery(String query) 
	{
		this.query = query;
	}

	public int getNumberOfResults() 
	{
		return numberOfResults;
	}

	public void setNumberOfResults(int numberOfResults) 
	{
		this.numberOfResults = numberOfResults;
	}

	public Date getSearchDate() 
	{
		return searchDate;
	}

	public void setSearchDate(Date searchDate)
	{
		this.searchDate = searchDate;
	}
	
	public String getUserName() 
	{
		return userName;
	}

	public void setUserName(String userName) 
	{
		this.userName = userName;
	}

	@Override
	public String toString() {
		return "SearchTerm [numberOfResults=" + numberOfResults + ", query="
				+ query + ", searchDate=" + searchDate + ", searchScope="
				+ searchScope + ", term=" + term + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + numberOfResults;
		result = prime * result + ((query == null) ? 0 : query.hashCode());
		result = prime * result
				+ ((searchDate == null) ? 0 : searchDate.hashCode());
		result = prime * result
				+ ((searchScope == null) ? 0 : searchScope.hashCode());
		result = prime * result + ((term == null) ? 0 : term.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SearchTerm other = (SearchTerm) obj;
		if (numberOfResults != other.numberOfResults)
			return false;
		if (query == null) {
			if (other.query != null)
				return false;
		} else if (!query.equals(other.query))
			return false;
		if (searchDate == null) {
			if (other.searchDate != null)
				return false;
		} else if (!searchDate.equals(other.searchDate))
			return false;
		if (searchScope == null) {
			if (other.searchScope != null)
				return false;
		} else if (!searchScope.equals(other.searchScope))
			return false;
		if (term == null) {
			if (other.term != null)
				return false;
		} else if (!term.equals(other.term))
			return false;
		return true;
	}	
}
