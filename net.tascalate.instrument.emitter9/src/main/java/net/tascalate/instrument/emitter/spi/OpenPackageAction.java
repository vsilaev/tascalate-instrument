package net.tascalate.instrument.emitter.spi;

abstract class OpenPackageAction {
    abstract boolean run(String packageName, Module packageModule, Module accessorModule);
    
    static final OpenPackageAction NOP = new OpenPackageAction() {
        @Override
        boolean run(String packageName, Module packageModule, Module accessorModule) {
            return false;
        }
    };
}
