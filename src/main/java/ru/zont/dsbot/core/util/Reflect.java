package ru.zont.dsbot.core.util;

import java.lang.reflect.Constructor;

public class Reflect {
    public static <T> T commonsNewInstance(Class<T> klass, String errorMsg, Object... constructorParams) {
        T instance;
        try {
            Class<?>[] classes = new Class<?>[constructorParams.length];
            for (int i = 0, constructorParamsLength = constructorParams.length; i < constructorParamsLength; i++)
                classes[i] = constructorParams[i].getClass();
            Constructor<T> constructor = klass.getDeclaredConstructor(classes);
            instance = constructor.newInstance(constructorParams);
        } catch (Exception e) {
            throw new RuntimeException(errorMsg, e);
        }
        return instance;
    }
}
