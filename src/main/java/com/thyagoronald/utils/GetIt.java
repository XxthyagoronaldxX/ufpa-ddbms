package com.thyagoronald.utils;

import java.util.HashMap;
import java.util.Map;

public class GetIt {
    private static GetIt instance;
    private final Map<Class<?>, Object> instances;

    private GetIt() {
        this.instances = new HashMap<>();
    }

    public static GetIt getInstance() {
        if (GetIt.instance == null) {
            GetIt.instance = new GetIt();
        }

        return GetIt.instance;
    }

    public <T> GetIt addSingleton(T instance) {
        this.instances.put(instance.getClass(), instance);

        return this;
    }

    public <T> T find(Class<T> instanceClass) {
        Object obj = this.instances.get(instanceClass);
        return instanceClass.cast(obj);
    }
}