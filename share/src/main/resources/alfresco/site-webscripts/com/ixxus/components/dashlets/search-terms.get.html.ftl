<#assign el = args.htmlid>
<#assign jsid = args.htmlid?js_string>

<script type="text/javascript">//<![CDATA[
(function()
{
	new Ixxus.dashlet.SearchTerms("${jsid}").setOptions(
	{
		searchScope: "all",
		showAdminToolbar: "${showAdminToolbar?string?js_string}"
		
	}).setMessages(${messages});
	new Alfresco.widget.DashletResizer("${jsid}", "${instance.object.id}");
})();
	
//]]></script>

<div class="dashlet search-terms">
	<div class="title">${msg("header")}</div>
	<div class="toolbar flat-button">
        <input id="${el}-search-scope" type="button" name="search-scope" value="${msg("filter.all")}" class="align-left"/>
        <select id="${el}-search-scope-menu">
        	<#list filterSearchScopes as filter>
        		<option value="${filter.type?html}">${filter.label}</option>
        	</#list>
        </select>
        <input id="${el}-range" type="button" name="range" value="${msg("filter.28days")}" class="align-left" />
        <select id="${el}-range-menu">
         	<#list filterRanges as filter>
            	<option value="${filter.type?html}">${msg("filter." + filter.label)}</option>
         	</#list>
        </select>
        <input id="${el}-max-items" type="button" name="max-items" value="${msg("filter.onehundred")}" class="align-left"/>
        <select id="${el}-max-items-menu">
         	<#list filtermaxItems as filter>
            	<option value="${filter.type?html}">${msg("filter." + filter.label)}</option>
         	</#list>
        </select>
        <span class="checkbox-filter">
        	<label for="${el}-user-search">${msg("label.mySearches")}</label>
            <input type="checkbox" id="${el}-user-search" name="-" title="${msg("label.mySearches")}" />
         </span>
        <div class="clear"></div>
	</div>
	<#if showAdminToolbar>
		<div class="toolbar flat-button admin-toolbar">
    		<span class="align-right">
    			<label for="${el}-displayCount">${msg("label.displayCount")}</label>
            	<input type="checkbox" id="${el}-display-count" name="-" title="${msg("label.displayCount")}" />
            </span>
            <span class="align-right">
            	<label for="${el}-zero-results" class="zero-results">${msg("label.zeroResults")}</label>
            	<input type="checkbox" id="${el}-zero-results" name="-" title="${msg("label.zero")}" />
        	</span>
    		<div class="clear"></div>
   		</div>
   	</#if>
	<div id="${el}-search-terms" class="body scrollableList" <#if args.height??>style="height: ${args.height}px;"</#if>></div>
</div>

<#-- Empty results list template -->
<div id="${el}-empty" style="display: none">
   <div class="empty"><h3>${msg("empty.title")}</h3><span>${msg("no.search.terms")}</span></div>
</div>