package de.lundlucenany9.runtime;

import java.util.List;

public interface Host {
    ScriptHost toScriptHost(List<String> imports);
}
