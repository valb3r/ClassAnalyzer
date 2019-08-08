package com.helpers.classrelationship.analysis

import java.util.concurrent.ConcurrentHashMap

class AppRegistry implements RegistryInterface {

    static final String UNRESOLVED_APP = "Unresolved"

    Map<String, AppDto> registry = new ConcurrentHashMap<>()

    AppRegistry() {
        add(UNRESOLVED_APP)
    }

    AppDto add(String appName) {
        return registry.computeIfAbsent(appName, {new AppDto([
                name: appName
        ])})
    }

    AppDto get(String name) {
        return registry.get(name)
    }

    AppDto getUnresolved() {
        return registry.get(UNRESOLVED_APP)
    }

    static class AppDto {

        String name
        long entityId
    }
}