<#escape x as jsonUtils.encodeJSONString(x)>
{
    "searchTerms":
    [
        <#if searchTerm.facetList??>
        	<#list searchTerm.facetList as facet>
			{
				"label":"${facet.first}",
				"count":"${facet.second}"
			}<#if facet_has_next>,</#if>
			</#list>
        </#if>
    ]		
}
</#escape>