package de.lundlucenany9.runtime;

public final class StaticObjects {
    private StaticObjects() {}

    public static final Object NULL = new Object(){
        @Override
        public String toString() {
            return "StaticObjects@null";
        }
    };
    public static final Object UNDEFINED = new Object(){
        @Override
        public String toString() {
            return "StaticObjects@undefined";
        }
    };
}
