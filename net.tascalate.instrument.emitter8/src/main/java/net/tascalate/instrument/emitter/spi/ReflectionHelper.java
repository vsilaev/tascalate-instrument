/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2019, Valery Silaev
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.tascalate.instrument.emitter.spi;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

class ReflectionHelper {
    private ReflectionHelper() {}
    
    static String getClassName(byte[] bytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        try {
            return getClassName(in);
        } finally {
            in.close();
        }
    }
    
    // https://stackoverflow.com/questions/1649674/resolve-class-name-from-bytecode
    static String getClassName(InputStream is) throws IOException {
        DataInputStream dis = new DataInputStream(is);
        dis.readLong(); // skip header and class version
        int constantPoolSize = (dis.readShort() & 0xFFFF ) - 1;
        int[] classes = new int[constantPoolSize];
        String[] strings = new String[constantPoolSize];
        for (int i = 0; i < constantPoolSize; i++) {
            int t = dis.read();
            switch (t) {
                case 1:
                    strings[i] = dis.readUTF(); 
                    break;
                case 5:
                case 6:
                    dis.readLong(); 
                    i++; 
                    break;
                case 7:
                    classes[i] = dis.readShort() & 0xFFFF;
                    break;
                case 8:
                    dis.readShort();
                    break;
                default:
                    dis.readInt();
            }
        }
        dis.readShort(); // skip access flags
        return strings[classes[(dis.readShort() & 0xFFFF) -1 ] - 1].replace('/', '.');
    }
    
    static String packageNameOf(String className) {
        int lastDot = className.lastIndexOf('.');
        return lastDot > 0 ? className.substring(0, lastDot) : null;
    }
    
    static ClassLoader getBestClassLoader(ClassLoader a, ClassLoader b) {
        if (null == a) {
            if (null == b) {
                throw new IllegalArgumentException("Either of arguments should not be null");
            } else {
                return b;
            }
        } else {
            if (null == b) {
                return a;
            } else if (a == b) {
                return b;
            } else if (a instanceof ClassEmitters.Factory) {
                if (b instanceof ClassEmitters.Factory) {
                    // resolve by hierarchy
                } else {
                    return a;
                }
            } else if (b instanceof ClassEmitters.Factory) {
                return b;
            }
        }
        if (isClassLoaderParent(a,b)) {
            return a;
        } else {
            // Biased to second, it should be taken on account
            // ClassEmitters pass b as an explicit parameter
            return b;
        }
    }
    
    /**
     * Check if <code>maybeParent</code> is a parent (probably inderect) of the <code>classLoader</code>
     * @param classLoader The classloader whose parents are checked, may not be null
     * @param maybeParent Possible parent, may be null for boot class loader
     * @return
     */
    static boolean isClassLoaderParent(ClassLoader classLoader, ClassLoader maybeParent) {
        ClassLoader cl = classLoader;
        do {
            cl = cl.getParent();
            if (maybeParent == cl) {
                // Check includes null == null for bootstrap classloader
                return true;
            }
        } while (cl != null);
        return false;
    }
    
}
