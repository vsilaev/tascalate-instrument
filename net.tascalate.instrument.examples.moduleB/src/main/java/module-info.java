import net.tascalate.instrument.api.AllowDynamicClasses;

import net.tascalate.instrument.examples.moduleB.internalX.PackageExtenderX;
import net.tascalate.instrument.examples.moduleB.internalY.PackageExtenderY;

@AllowDynamicClasses({PackageExtenderX.class, PackageExtenderY.class})
module net.tascalate.instrument.examples.moduleB {
	// we can avoid transitive here -- there is no explicit type use in exported API
	requires net.tascalate.instrument.spi;
	
	// Theses 2 exports are necessary for demo app -- to instantiate generated class
	// Not necessary for Java Agent that only define classes
    exports net.tascalate.instrument.examples.moduleB;
	exports net.tascalate.instrument.examples.moduleB.internalX; 
	
	// Mandatory -- need to open for instrumentation (obviously, without export)
	// This is smth. that would be great to avoid, but, unfortunately, is unavoidable
	// Increases the total verbosity a lot!
	opens net.tascalate.instrument.examples.moduleB.internalX to net.tascalate.instrument.spi;
	opens net.tascalate.instrument.examples.moduleB.internalY to net.tascalate.instrument.spi;
	
	// In-place rant. Why it's not possible to do the following:
	/*
	opens 
	    net.tascalate.instrument.examples.moduleB.internalX,
	    net.tascalate.instrument.examples.moduleB.internalY
	to 
	    net.tascalate.instrument.spi;
	*/
	// It's dead simple! Minor change in grammar! 
	// But readability would be improved A LOT!
	// And "export" + "requires" would benefit 
	// from the "list" syntax too!
	
	
}