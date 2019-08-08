package com.helpers.classrelationship.analysis

import java.util.concurrent.ConcurrentHashMap

class JarRegistry implements RegistryInterface {

    static final String UNRESOLVED_JAR = "unresolved.jar"

    Map<String, JarDto> registry = new ConcurrentHashMap<>()

    JarRegistry() {
        add(UNRESOLVED_JAR, AppRegistry.UNRESOLVED_APP)
    }

    JarDto add(String jarPath, String appName) {
        String name = jarPathToName(jarPath)
        registry.compute(name, {key, value ->
            if (!value) {
                return new JarDto([
                    appNames: [appName],
                    name: name,
                    paths: [jarPath]])
            } else {
                value.appNames.add(appName)
                value.paths.add(jarPath)
                return value
            }
        })
    }

    JarDto getUnresolved() {
        return registry.get(UNRESOLVED_JAR)
    }

    JarDto getByPath(String jarPath) {
        return registry.get(jarPathToName(jarPath))
    }

    static String jarPathToName(String jarPath) {
        return new File(jarPath).getName()
    }

    static class JarDto {

        Set<String> appNames
        String name
        Set<String> paths
        long entityId
    }
}