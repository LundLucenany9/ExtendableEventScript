package de.lundlucenany9.runtime;

import java.util.List;

/**
 * Provides the bridge between the runtime and the host environment.
 * <p>
 * Implementations receive the imports requested by a script and create a
 * corresponding {@link ScriptHost} that is used during script execution.
 */

public interface Host {

    /**
     * Creates a script host for the given list of requested imports.
     *
     * @param imports the import names requested by the script
     * @return a {@link ScriptHost} configured for the requested imports
     */
    ScriptHost toScriptHost(List<String> imports);
}
