package vn.vnpay.rabbitmqrpc.scan;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vn.vnpay.rabbitmqrpc.annotation.Autowire;
import vn.vnpay.rabbitmqrpc.annotation.Component;
import vn.vnpay.rabbitmqrpc.annotation.ValueInjector;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;

public class ApplicationContext {
    private static final Logger logger = LoggerFactory.getLogger(ApplicationContext.class);

    private Map<Class<?>, Object> beans = new HashMap<>();
    private Map<String, Object> configValues = new HashMap<>();

    public ApplicationContext(String basePackage) throws Exception {
        loadConfigValues();
        List<Class<?>> classes = PackageScanner.getClasses(basePackage);
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class)) {
                Constructor<?> constructor = clazz.getConstructor();
                Object instance = constructor.newInstance();
                ValueInjector.injectValues(instance, configValues);
                beans.put(clazz, instance);
            }
        }
        for (Class<?> clazz : classes) {
            Field[] fields = clazz.getDeclaredFields();
            Object clazzInject = getBean(clazz);
            for (Field field : fields) {
                if (field.isAnnotationPresent(Autowire.class)) {
                    if (field.getType().isInterface()) {
                        List<Class<?>> classList = getClassesImplementingInterface(field.getType(), basePackage);
                        if (classList.size() == 1) {
                            Object bean = getBean(classList.get(0));
                            if (bean != null) {
                                field.setAccessible(true);
                                try {
                                    field.set(clazzInject, bean);
                                    beans.replace(clazz, clazzInject);
                                } catch (IllegalAccessException e) {
                                    logger.error("Could not set bean {} to class {}", bean, clazz, e);
                                }
                            }
                        }
                    } else {
                        Object bean = getBean(field.getType());
                        if (bean != null) {
                            field.setAccessible(true);
                            try {
                                field.set(clazzInject, bean);
                                beans.replace(clazz, clazzInject);
                            } catch (IllegalAccessException e) {
                                logger.error("Could not set bean {} to class {}", bean, clazz, e);
                            }
                        }
                    }
                }
            }
        }
        for (Class<?> clazz : classes) {
            if (clazz.isAnnotationPresent(Component.class)) {
                Object clazzInject = getBean(clazz);
                ValueInjector.injectMethod(clazzInject);
            }
        }
    }

    public <T> T getBean(Class<T> clazz) {
        return clazz.cast(beans.get(clazz));
    }

    public void loadConfigValues() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("src/main/resources/config.properties")) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                String value = properties.getProperty(key);
                configValues.put(key, value);
            }
        } catch (Exception e) {
            logger.error(" Get properties from resource failed with root cause : ", e);
        }
    }

    public List<Class<?>> getClassesImplementingInterface(Class<?> interfaze, String packageName) throws ClassNotFoundException {
        List<Class<?>> implementers = new ArrayList<>();
        List<Class<?>> classes = PackageScanner.getClasses(packageName);
        for (Class<?> clazz : classes) {
            if (interfaze.isAssignableFrom(clazz) && !clazz.isInterface()) {
                implementers.add(clazz);
            }
        }
        return implementers;
    }
}
