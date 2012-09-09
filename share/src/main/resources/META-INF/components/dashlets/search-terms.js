(function () {
    /**
    * YUI Library aliases
    */
    var Dom = YAHOO.util.Dom,
        Event = YAHOO.util.Event;

    Ixxus.dashlet.SearchTerms = function SearchTerms_constructor(htmlId) {
        Ixxus.dashlet.SearchTerms.superclass.constructor.call(this, "Ixxus.dashlet.SearchTerms", htmlId, ["button", "container"]);
        return this;
    };

    YAHOO.extend(Ixxus.dashlet.SearchTerms, Ixxus.component.Base, {
        options: {
            /**
            * Search scope 
            * 
            * @property searchScope
            * @type string
            * @default "site"
            */
            searchScope: "all",

            /**
             * Flag to indicate whether on not to show admin toolbar
            * 
            * @property showAdminToolbar
            * @type boolean
            * @default false
            */
            showAdminToolbar: false
        },

        /**
         * Date range
         * 
         * @property dateRange
         * @type int
         * @default 0
         */
        dateRange: 28,

        /**
         * User search
         * 
         * @property userSearch
         * @type boolean
         * @default false
         */
        userSearch: false,

        /**
         * Max items
         * 
         * @property maxItems
         * @type string
         * @default 100
         */
        maxItems: "100",

        /**
         * Show terms with zero results only
         * 
         * @property zeroResults
         * @type boolean
         * @default false
         */
        zeroResults: false,

        /**
         * Flag to indicate if the tag count 
         * should be displayed.
         * 
         * @property displayCount
         * @type boolean
         * @default false
         */
        displayCount: false,

        onReady: function SearchTerms_onReady() {
            var me = this, searchScopeMenu, dateRangeMenu, maxItemsMenu,
                userSearchCheckbox, displayCountCheckbox, termsWithZeroResults;

            //search scope filter
            this.widgets.searchScope = new YAHOO.widget.Button(this.id + "-search-scope", {
                type: "split",
                menu: me.id + "-search-scope-menu",
                lazyloadmenu: false
            });

            searchScopeMenu = this.widgets.searchScope.getMenu();
            searchScopeMenu.subscribe("click", function (p_sType, p_aArgs) {
                var menuItem = p_aArgs[1];
                if (menuItem) {
                    me.widgets.searchScope.set("label", menuItem.cfg.getProperty("text"));
                    me.onFilterChanged.call(me, p_aArgs[1], "searchScope");
                }
            });

            //date range filter
            this.widgets.dateRange = new YAHOO.widget.Button(this.id + "-range", {
                type: "split",
                menu: me.id + "-range-menu",
                lazyloadmenu: false
            });

            dateRangeMenu = this.widgets.dateRange.getMenu();
            dateRangeMenu.subscribe("click", function (p_sType, p_aArgs) {
                var menuItem = p_aArgs[1];
                if (menuItem) {
                    me.widgets.dateRange.set("label", menuItem.cfg.getProperty("text"));
                    me.onFilterChanged.call(me, p_aArgs[1], "dateRange");
                }
            });

            //max items filter
            this.widgets.maxItems = new YAHOO.widget.Button(this.id + "-max-items", {
                type: "split",
                menu: me.id + "-max-items-menu",
                lazyloadmenu: false
            });

            maxItemsMenu = this.widgets.maxItems.getMenu();
            maxItemsMenu.subscribe("click", function (p_sType, p_aArgs) {
                var menuItem = p_aArgs[1];
                if (menuItem) {
                    me.widgets.maxItems.set("label", menuItem.cfg.getProperty("text"));
                    me.onFilterChanged.call(me, p_aArgs[1], "maxItems");
                }
            });

            //My searches check box
            userSearchCheckbox = Dom.get(this.id + "-user-search");
            Event.addListener(userSearchCheckbox, "click", this.onCheckboxClicked, "userSearch", this);

            //only display these check boxes if the user is an admin or a site manager.
            if (this.options.showAdminToolbar) {

                //Display count checkbox
                displayCountCheckbox = Dom.get(this.id + "-display-count");
                Event.addListener(displayCountCheckbox, "click", this.onCheckboxClicked, "displayCount", this);

                //Show terms that returned zero results checkbox
                termsWithZeroResults = Dom.get(this.id + "-zero-results");
                Event.addListener(termsWithZeroResults, "click", this.onCheckboxClicked, "zeroResults", this);
            } else {
                this.options.displayCount = false;
            }
            this.loadSearchTerms();
        },

        /**
         * Makes an Ajax call to Alfresco to get the search terms. The results 
         * are passed to the TagCloud.js to generate the search terms tag cloud.
         */
        loadSearchTerms : function SearchTerms_loadSearchTerms() {
            var me = this, fnSearchTermsLoaded;

            /**
             * The response from Alfresco is passed to this method, the TagCloud object is
             * created, and TagCloud parameters are set.
             * 
             * @param p_response
             */
            fnSearchTermsLoaded = function SearchTerms_fnSearchTermsLoaded(p_response) {

                var searchTerms, json = p_response.json, tagCloud, errorMessage;

                if (typeof json.searchTerms !== "undefined") {
                    searchTerms = json.searchTerms;

                    tagCloud = new Ixxus.module.TagCloud(me.id + "-tag-cloud");
                    tagCloud.setOptions({
                        results : searchTerms,
                        tagUri: me._getSearchTermURI(),
                        tagUriSuffix : me._getSearchTermURISuffix(),
                        tagCloudContainerId: me.id + "-search-terms",
                        noTagsFoundMessage: me.msg("label.noSearchTerms"),
                        displayCount: me.displayCount
                    });
                    tagCloud.generateTagCloud();
                } else {
                    errorMessage = '<label class="no-tags-msg">' + me.msg("label.failureMessage") + '</label>';
                    Dom.get(me.id + "-search-terms").innerHTML = errorMessage;
                }
            };

            Alfresco.util.Ajax.request({
                url: Alfresco.constants.PROXY_URI_RELATIVE + this._buildURI(),
                successCallback: {
                    fn: fnSearchTermsLoaded,
                    scope: this
                }
            });
        },

        /**
         * Called when one of the drop down filters has been changed on the UI.
         * 
         * The filterName is passed as a parameter to help determine what property
         * needs to be updated before the request is sent to Alfresco.
         */
        onFilterChanged : function SearchTerms_onFilterChanged(p_oMenuItem, filterName) {

            var filter = p_oMenuItem.value;

            if (filterName === "searchScope") {
                this.options.searchScope = filter;
            } else if (filterName === "dateRange") {
                this.dateRange = filter;
            } else if (filterName === "maxItems") {
                this.maxItems = filter;
            }
            this.loadSearchTerms();
        },

        /**
         * Called when one of the check box filters has been clicked on the UI.
         * 
         * The filterName is passed as a parameter to help determine what property
         * needs to be updated before the request is sent to Alfresco.
         */
        onCheckboxClicked : function SearchTerms_onCheckboxClicked(e, filterName) {

            if (filterName === "userSearch") {
                this.userSearch = !this.userSearch;
            } else if (filterName === "zeroResults") {
                this.zeroResults = !this.zeroResults;
            } else if (filterName === "displayCount") {
                this.displayCount = !this.displayCount;
            }
            this.loadSearchTerms();
        },

        /**
         * Returns the share search URI that is used in the for each search term link.
         *
         * The URI that is return id determined by what the current searchScope is.
         */
        _getSearchTermURI : function SearchTerms__getSearchTermURI() {
            var uri = "";
            if (this.options.searchScope !== "all" && this.options.searchScope !== "repo" && this.options.searchScope !== "allSites") {
                uri = Alfresco.constants.URL_CONTEXT + 'page/site/' + this.options.searchScope + '/search?t=';
            } else {
                uri = Alfresco.constants.URL_CONTEXT + 'page/search?t=';
            }
            return uri;
        },

        /**
         * Returns any values that are to be appended to the end of the share search URL. 
         * All sites and repository searches have additional parameters that are used by 
         * Alfresco to identify what type of search is being executed. 
         */
        _getSearchTermURISuffix : function SearchTerms__getSearchTermURISuffix() {
            var uriSuffix = "";
            if (this.options.searchScope === "allSites") {
                uriSuffix = '&a=true&r=false';
            } else if (this.options.searchScope === "repo") {
                uriSuffix = '&a=true&r=true';
            }
            return uriSuffix;
        },

        /**
         * Apply parameter values to the search terms URL.
         */
        _buildURI : function SearchTerms__buildURI() {
            var uri = YAHOO.lang.substitute("search/search-terms/{searchScope}/{dateRange}/{userSearch}/{maxItems}/{zeroResults}", {
                searchScope: encodeURIComponent(this.options.searchScope),
                dateRange: this.dateRange,
                userSearch: encodeURIComponent(this.userSearch),
                maxItems: this.maxItems,
                zeroResults: encodeURIComponent(this.zeroResults)
            });
            return uri;
        }
    });
})();