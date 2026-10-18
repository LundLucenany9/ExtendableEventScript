package de.lundlucenany9.runtime;

import java.util.List;
import java.util.Map;

public interface Host {
    Object call(String name, List<Object> args, Map<String, Object> opts);
}
