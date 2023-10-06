package org.example.annotation;


import org.example.common.PropertiesFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Map;

public class ValueInjector {

    private ValueInjector() {
    }

    public static void injectValues(Object target) throws IllegalAccessException {
        Field[] fields = target.getClass().getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(ValueKeyMap.class)) {
                ValueKeyMap valueKeyMap = field.getAnnotation(ValueKeyMap.class);
                String key = valueKeyMap.value();
                field.setAccessible(true);
                Map<String, String> valueMap = PropertiesFactory.readKeysFromPropertiesFile(key);
                if (!valueMap.isEmpty()) {
                    field.set(target, valueMap);
                }
            }
            if (field.isAnnotationPresent(CustomValue.class)) {
                CustomValue customValue = field.getAnnotation(CustomValue.class);
                String key = customValue.value();
                field.setAccessible(true);
                String value = PropertiesFactory.getFromProperties(key);
                if (value.matches("\\d+")) {
                    int number = Integer.parseInt(value);
                    field.set(target, number);
                } else if (value.matches("^(true|false)$")) {
                    boolean input = Boolean.parseBoolean(value);
                    field.set(target, input);
                } else {
                    field.set(target, value);
                }
            }
        }
    }



}