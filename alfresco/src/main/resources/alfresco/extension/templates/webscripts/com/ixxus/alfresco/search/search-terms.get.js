var FACET_FIELD_TERM = "@{http://www.ixxus.com/model/ixxus/1.0}term.__.u";
var FILTER_QUERY_SEARCH_SCOPE = "@{http://www.ixxus.com/model/ixxus/1.0}searchScope.__.u:"; 
var FILTER_QUERY_USER_NAME = "@{http://www.ixxus.com/model/ixxus/1.0}userName.__.u:"; 
var FILTER_QUERY_NO_OF_RESULTS = "@{http://www.ixxus.com/model/ixxus/1.0}numberOfResults:0";

/**
 * Create Date in the required format
 * 
 * @param filter
 * @param hour
 * @param minutes
 * @param seconds
 * @param milliseconds
 * @returns Date
 */
function getRangeDate(filter, hour, minutes, seconds, milliseconds)
{
	var now = new Date();
	var queryDate = now.setHours(hour, minutes, seconds, milliseconds);
	queryDate = now.setDate(now.getDate() - filter);
	return utils.toISO8601(queryDate).toString();
}

function main()
{
	if (logger.isLoggingEnabled())
	{
		logger.log("Getting search terms...");
	}
	
	var dateRangeQuery, now = new Date(),dateFilter = 1, dateRangeStart, 
		dateRangeEnd, ftsQuery, nodes, searchTerm, searchScope = "all", 
		period, filterQueries = [], facetMinCount = "1",
		maxItems = "20", zeroResults = false;
		
	//add filter queries
	if(typeof url.templateArgs.userSearch !== "undefined" && url.templateArgs.userSearch === "true") {
		filterQueries.push(FILTER_QUERY_USER_NAME + person.properties.userName);
	}
	
	if(typeof url.templateArgs.searchScope !== "undefined" && url.templateArgs.searchScope != "all") {
		filterQueries.push(FILTER_QUERY_SEARCH_SCOPE + url.templateArgs.searchScope);
	}
	
	if(typeof url.templateArgs.zeroResults !== "undefined" && url.templateArgs.zeroResults === "true") {
		filterQueries.push(FILTER_QUERY_NO_OF_RESULTS);
	}
	
	if(logger.isLoggingEnabled()) {
		logger.log("Filter queries : " + filterQueries.toString());
	}
	
	//add date range query
	if(typeof url.templateArgs.dateFilter !== "undefined") {
		dateFilter = url.templateArgs.dateFilter;
	}
	
	dateRangeStart = getRangeDate(dateFilter, 0, 0, 0, 1);
	dateRangeEnd = getRangeDate(0, 23, 59, 59, 999);
	dateRangeQuery = 'ix:searchDate:"' + dateRangeStart + '".."' + dateRangeEnd + '"';
	ftsQuery = '((TYPE:"ix:searchTerm" AND ' + dateRangeQuery + ') AND -TYPE:"cm:thumbnail" AND -TYPE:"cm:failedThumbnail" AND -TYPE:"cm:rating") AND NOT ASPECT:"sys:hidden"';
	
	//get max items
	if(typeof url.templateArgs.maxItems !== "undefined")
	{
		maxItems = url.templateArgs.maxItems;
	}

	if(logger.isLoggingEnabled())
	{
		logger.log("Date range query : " + dateRangeQuery);
		logger.log("Fts query : "  + ftsQuery);
	}
	
	var queryDef = {
	    query: ftsQuery,
	    facetField: FACET_FIELD_TERM,
	    facetMinCount: facetMinCount,
	    facetLimit: maxItems,
	    facetSort: "index",
	    filterQueries: filterQueries,
	    language: "fts-alfresco",
	    onerror: "no-results"
	};
	
	nodes = search.query(queryDef);
	
	for(var i = 0; i < nodes.length; i++)
	{
		if(nodes[i].isFacet)
		{
			var facet = nodes[i];
			if(facet.facetHasAnyHits)
			{
				searchTerm = facet;
		  	}
		}
	}
	
	if(logger.isLoggingEnabled())
	{
		if(searchTerm && searchTerm.facetList)
		{
			logger.log("Found " +  searchTerm.facetList.length + " search terms.");
		}
		else
		{
			logger.log("No search terms found.");
		}
	}
	
	return searchTerm;
}

model.searchTerm = main();