package nl.knaw.huygens.security.server.util;

import java.util.Iterator;

public class ClassNameIterator implements Iterator<String> {
    private final Class[] classes;

    private int index;

    public ClassNameIterator(Class[] classes) {
        this.classes = classes;
        this.index = 0;
    }

    @Override
    public boolean hasNext() {
        return classes.length > index;
    }

    @Override
    public String next() {
        return classes[index++].getName();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}