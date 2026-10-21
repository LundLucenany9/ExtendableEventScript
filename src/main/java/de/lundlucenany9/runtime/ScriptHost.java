package de.lundlucenany9.runtime;

import java.util.List;
import java.util.Map;

public interface ScriptHost {
    Object call(String name, List<Object> args, Map<String, Object> opts);

}
