package net.tascalate.instrument.api;

/**
 * <p>Marker superclass for "open for extension" (or, actually, "instrumentation")
 * packages.
 * <p>Each package that is opened for extension must:
 * <ol>
 * <li>Define a subclass of this class
 * <li>List this class in {@link AllowDynamicClasses} of own module
 * <li>Additionally, the module should open this package to
 * <code>net.tascalate.instrument.api</code>
 * </ol>
 * 
 * <p>The ritual is necessary to avoid using other classes from the package in
 * {@link AllowDynamicClasses} - this will cause loading of such classes during
 * ClassFileTransformer.transform method invocation and may lead to
 * unpredictable results. For the same reason the {@link AllowDynamicClasses} is
 * declared as an abstract class rather than an interface.
 * 
 * 
 * @author vsilaev
 * @see AllowDynamicClasses
 *
 */
abstract public class AbstractOpenPackage {
}
