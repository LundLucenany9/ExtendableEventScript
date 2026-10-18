package de.lundlucenany9.runtime.simple;

public class StandardMethods implements Library{
    @Override
    public String getName() {
        return "std";
    }

    @EESFunction
    public void log(Object message, @EESOptional(type = EESOptional.Type.BOOLEAN, booleanValue = true) boolean newline) {
        if(newline) message += "\n";
        System.out.print(message);
    }
    @EESFunction
    public void err(Object message, @EESOptional(type = EESOptional.Type.BOOLEAN) boolean throwException) {
        if(throwException) throw new RuntimeException(message.toString());
        System.err.println(message);
    }

    @EESFunction(name = "string")
    public String string_(Object object) {
        return object.toString();
    }

    @EESFunction(name = "int")
    public int int_(Object object) {
        return Integer.parseInt(object.toString());
    }

    @EESFunction(name = "double")
    public double double_(Object object) {
        return Double.parseDouble(object.toString());
    }

    @EESFunction
    public boolean bool(Object object) {
        return Boolean.parseBoolean(object.toString());
    }

}
