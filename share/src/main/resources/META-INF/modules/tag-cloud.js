/**
 * The code used to generate the tag cloud is originally 
 * from Will Abson's site tag dashlet in the share extras project: 
 * 
 * http://share-extras.googlecode.com/svn/trunk/Site%20Tags%20Dashlet/
 * 
 * The code has been moved into its own javascript class to make it more re-usable. 
 */

(function () {
    /**
    * YUI Library aliases
    */
    var Dom = YAHOO.util.Dom,
      Event = YAHOO.util.Event;

    Ixxus.module.TagCloud = function (htmlId) {
        Ixxus.module.TagCloud.superclass.constructor.call(this, "Ixxus.module.TagCloud", htmlId, ["button", "container"]);

        return this;
    };

    YAHOO.extend(Ixxus.module.TagCloud, Ixxus.component.Base, {
        options: {
            /**
             * List of results to be used in tag cloud
             * 
             * @property results
             * @type list
             * @default []
             */
            results: [],

            /**
             * The tag uri link
             * 
             * @property tagUri
             * @type string
             * @default ""
             */
            tagUri: "",

            /**
             * Tag cloud container ID
             * 
             * @property tagCloudContainerId
             * @type string,
             * @default ""
             */
            tagCloudContainerId: "",

            /**
             * No tags found message
             * 
             * @property noTagsFoundMessage
             * @type string
             * @default "No tags found."
             */
            noTagsFoundMessage: "No tags found.",

            /**
             * Flag to indicate if the tag count 
             * should be displayed.
             * 
             * @property displayCount
             * @type boolean
             * @default false
             */
            displayCount: false,

            /**
             * Tag URI suffix
             * 
             * @property tagUriSuffix
             * @type string
             * @default ""
             */
            tagUriSuffix: ""
        },

        /**
         * Minimum tag font size.
         * 
         * @property minFontSize
         * @type number
         * @default 1.0
         */
        minFontSize: 1.0,

        /**
         * Maximum tag font size.
         * 
         * @property maxFontSize
         * @type number
         * @default 3.5
         */
        maxFontSize: 3.0,

        /**
         * Font size units
         * 
         * @property fontSizeUnits
         * @type string
         * @default "em"
         */
        fontSizeUnits: "em",

        /**
         * Set the options for the TagCloud
         */
        setOptions: function TagCloud_setOptions(obj) {
            this.options = YAHOO.lang.merge(this.options, obj);
        },
        
        /**
         * Contains functions to generate the tag cloud from the results list.
         */
        generateTagCloud : function TagCloud_generateTagCloud() {

            var me = this, html, minTagCount, maxTagCount, tags = this.options.results,
                tagsContainer = Dom.get(this.options.tagCloudContainerId), tag, count,
                countLabel, fnMaxTagCount, fnMinTagCount, fnTagWeighting, fnTagFontSize,
                i, ii;

            if (tags.length === 0) {
                html = '<label class="no-tags-msg">' + this.options.noTagsFoundMessage + '</label>';
                tagsContainer.innerHTML = html;
                return;
            }

            fnMaxTagCount = function maxTagCount() {
				var maxCount = 0, count;
				for (i = 0, ii = tags.length; i < ii; i++) {
					count = tags[i].count;
					if (parseInt(count, 10) > parseInt(maxCount, 10)) {
						maxCount = count;
					}
				}
				return maxCount;
			};
			fnMinTagCount = function minTagCount() {
				var minCount = parseInt(1000000, 10), count;
				for (i = 0, ii = tags.length; i < ii; i++) {
					count = parseInt(tags[i].count, 10);
					if (count < minCount) {
						minCount = count;
					}
				}
				return minCount;
			};
			fnTagWeighting = function tagWeighting(count) {
				// should return a number between 0.0 (for smallest) and 1.0 (for largest)
				var weight = (count - minTagCount) / (maxTagCount - minTagCount);
				return weight;
			};
			fnTagFontSize = function tagFontSize(thisTag, count) {
				var size = (me.minFontSize +
						(me.maxFontSize - me.minFontSize) * fnTagWeighting(count)).toFixed(2);
				return size;
			};

            minTagCount = fnMinTagCount();
            maxTagCount = fnMaxTagCount();
            html = "<ul>";

            for (i = 0, ii = tags.length; i < ii; i++) {
                tag = tags[i].label;
                count = tags[i].count;
                countLabel = this.options.displayCount === true ? '(' + count + ')' : "";

                html += '<li class="tag"><a href="' + this.options.tagUri + tag + this.options.tagUriSuffix + '" class="theme-color-1" style="font-size: '
                    + fnTagFontSize(tag, count) + this.fontSizeUnits + '">' + tag + countLabel + '</a></li>\n';
            }

            html += "</ul>";

            tagsContainer.innerHTML = html;
            Alfresco.util.Anim.fadeIn(tagsContainer);
        }
    });
})();