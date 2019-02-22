import net.tascalate.instrument.api.AllowDynamicClasses;
import net.tascalate.instrument.examples.moduleA.internalX.OpenPackageX;
import net.tascalate.instrument.examples.moduleA.internalY.OpenPackageY;
/**
 * 
 */
/**
 * @author vsilaev
 *
 */
// Mandatory -- explicit list of "open" packages
@AllowDynamicClasses({OpenPackageX.class, OpenPackageY.class})
module net.tascalate.instrument.examples.moduleA {
	// we can avoid transitive here -- there is no explicit type use in exported API
	requires net.tascalate.instrument.spi;
	
    // Theses 2 exports are necessary for demo app -- to instantiate generated class
    // Not necessary for Java Agent that only define classes
    exports net.tascalate.instrument.examples.moduleA;
    exports net.tascalate.instrument.examples.moduleA.internalY; 
	
	// Mandatory -- need to open for instrumentation (obviously, without export)
	// This is smth. that would be great to avoid, but, unfortunately, is unavoidable
	// Increases the total verbosity a lot!
	opens net.tascalate.instrument.examples.moduleA.internalX to net.tascalate.instrument.spi;
	opens net.tascalate.instrument.examples.moduleA.internalY to net.tascalate.instrument.spi;
}