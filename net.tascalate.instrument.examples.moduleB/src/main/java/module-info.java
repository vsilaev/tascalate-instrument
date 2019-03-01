
/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Valery Silaev All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
import net.tascalate.instrument.emitter.api.AllowDynamicClasses;
import net.tascalate.instrument.examples.moduleB.internalX.PackageExtenderX;
import net.tascalate.instrument.examples.moduleB.internalY.PackageExtenderY;

@AllowDynamicClasses({ PackageExtenderX.class, PackageExtenderY.class }) 
module net.tascalate.instrument.examples.moduleB {
    // we can avoid transitive here -- there is no explicit type use in exported API
    requires net.tascalate.instrument.emitter;

    // Theses 2 exports are necessary for demo app -- to instantiate generated class
    // Not necessary for Java Agent that only define classes
    exports net.tascalate.instrument.examples.moduleB;
    exports net.tascalate.instrument.examples.moduleB.internalX;

    // Mandatory -- need to open for instrumentation (obviously, without export)
    // This is smth. that would be great to avoid, but, unfortunately, is
    // unavoidable
    // Increases the total verbosity a lot!
    opens net.tascalate.instrument.examples.moduleB.internalX 
       to net.tascalate.instrument.emitter;
    opens net.tascalate.instrument.examples.moduleB.internalY 
       to net.tascalate.instrument.emitter;

    // In-place rant. Why it's not possible to do the following:
    /*
     * opens net.tascalate.instrument.examples.moduleB.internalX,
     *       net.tascalate.instrument.examples.moduleB.internalY 
     * to
     *     net.tascalate.instrument.emitter;
     */
    // It's dead simple! Minor change in grammar!
    // But readability would be improved A LOT!
    // And "export" + "requires" would benefit
    // from the "list" syntax too!

}