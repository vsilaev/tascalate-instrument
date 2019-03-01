package net.tascalate.instrument.attach.core;

import java.util.Iterator;
import java.util.NoSuchElementException;

public class SafeAgentLoaderChain implements Iterable<SafeAgentLoader> {

    @Override
    public Iterator<SafeAgentLoader> iterator() {
        return new Seq();
    }

    static class Seq implements Iterator<SafeAgentLoader> {
        int idx = 0;
        
        public boolean hasNext() {
            return idx < 3;
        }
        
        public SafeAgentLoader next() {
            SafeAgentLoader result;
            switch (idx) {
                case 0:
                    // Works only if JNA lib is added
                    result = new SafeNativeAgentLoader();
                    break;
                case 1:
                    result = new LocalAgentLoader();
                    break;
                case 2: 
                    // We ever hardly select this option
                    // because the previous one uses
                    /// external agent as fall-back
                    result = new ExternalAgentLoader();
                    break;
                default:
                    throw new NoSuchElementException();
            }
            idx++;
            return result;
        }
    }
}
