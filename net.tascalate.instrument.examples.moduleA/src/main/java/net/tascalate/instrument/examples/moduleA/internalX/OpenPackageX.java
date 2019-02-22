package net.tascalate.instrument.examples.moduleA.internalX;

import net.tascalate.instrument.api.AbstractOpenPackage;

public final class OpenPackageX extends AbstractOpenPackage {
	// May be private
	// It's anyway requires to open package for reflection
	// even if this class / constructor is public
	private OpenPackageX() {}
}
