import net.tascalate.instrument.api.AllowDynamicClasses;
import net.tascalate.instrument.examples.app.dynamic.OpenPackage;

/**
 * 
 */
/**
 * @author vsilaev
 *
 */
@AllowDynamicClasses(OpenPackage.class)
module net.tascalate.instrument.examples.app {
	requires net.tascalate.instrument.spi;	
	
	opens net.tascalate.instrument.examples.app.dynamic to net.tascalate.instrument.spi;
}