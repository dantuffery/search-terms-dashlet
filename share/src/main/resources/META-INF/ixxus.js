/**
 * Ixxus Base class that extends the Alfresco Base class.
 * 
 * Ixxus root namespace.
 * 
 * @namespace Ixxus
 */
if (typeof Ixxus === "undefined" || !Ixxus) {
    var Ixxus = {};
}

/**
 * Ixxus top-level component namespace.
 * 
 * @namespace Ixxus
 * @class Ixxus.component
 */
Ixxus.component = Ixxus.component || {};

/**
 * Ixxus top-level dashlet namespace.
 * 
 * @namespace Ixxus
 * @class Ixxus.dashlet
 */
Ixxus.dashlet = Ixxus.dashlet || {};

/**
 * Ixxus top-level module namespace.
 * 
 * @namespace Ixxus
 * @class Ixxus.module
 */
Ixxus.module = Ixxus.module || {};

(function() {
	
	Ixxus.component.Base =  function Ixxus_component_Base(name, id, components) {
		Ixxus.component.Base.superclass.constructor.call(this, name, id, components);
		
        return this;
    };
    
    YAHOO.extend(Ixxus.component.Base, Alfresco.component.Base, {
    	//common functions go here.
    });  
})();