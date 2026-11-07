package de.lundlucenany9.ees.runtime.simple;

import java.lang.reflect.Method;

record EESMethod(String libraryName, String methodName, Method method) {
}
