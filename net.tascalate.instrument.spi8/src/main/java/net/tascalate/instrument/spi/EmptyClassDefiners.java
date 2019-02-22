package net.tascalate.instrument.spi;

import java.util.Optional;

class EmptyClassDefiners implements ClassDefiners {
	private EmptyClassDefiners() {}
	
	@Override
	public Optional<ClassDefiner> lookup(String packageName) {
		return Optional.empty();
	}
	
	static final ClassDefiners INSTANCE = new EmptyClassDefiners();
}
