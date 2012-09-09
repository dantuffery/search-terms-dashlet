/**
 * Capitalise First letter of display Label.
 * 
 * @param label
 * @returns String
 */
function capitaliseFirstLetter(label) {
    return label.charAt(0).toUpperCase() + label.slice(1);
}

/**
 * Get the filters that are defined in search-terms.get.config.xml.
 * If the sites parameter is not empty also add sites to the filter 
 * map, the sites are used for search scope filter.
 */
function getFilters(filterType, sites) {
   var myConfig = new XML(config.script),
       filters = [];

   for each (var xmlFilter in myConfig[filterType].filter) {
      filters.push({
         type: xmlFilter.@type.toString(),
         label: xmlFilter.@label.toString()
      });
   }
   
   if(sites) {
	   for(var i = 0, ii = sites.length; i < ii; i++) {
		   var site = sites[i];
		   filters.push({
			   type: site.shortName,
			   label: capitaliseFirstLetter(site.shortName) + " Site Search Scope"
		   });
	   }
   }

   return filters;
}

/**
 * Add the user's sites to the search scope filter
 */
var sites = [];
var result = remote.call("/api/people/" + encodeURIComponent(user.name) + "/sites");

if (result.status == 200) {
   sites = eval('(' + result + ')');
}

var showAdminToolbar = true;

if(!user.isAdmin)
{
	//see if user is the site manager
	showAdminToolbar = false;
	for(var i = 0, ii = sites.length; i < ii; i++)
	{
		var site = sites[i];
	    var json = remote.call("/api/sites/" + site.shortName + "/memberships/" + encodeURIComponent(user.name));

	    if (json.status == 200) {
	    	var obj = eval('(' + json + ')');
	        if (obj) {
	        	showAdminToolbar = (obj.role == "SiteManager");
	        	if(showAdminToolbar) {
	        		break;
	        	}
	        }
	    }
	}
}

model.showAdminToolbar = showAdminToolbar;
model.filtermaxItems = getFilters("filter-max-items");
model.filterRanges = getFilters("filter-range");
model.filterSearchScopes = getFilters("filter-search-scope", sites)

