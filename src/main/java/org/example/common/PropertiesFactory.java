package org.example.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class PropertiesFactory {

    private static final Logger logger = LoggerFactory.getLogger(PropertiesFactory.class);

    private PropertiesFactory() {
    }

    public static String getFromProperties(String key) {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("src/main/resources/config.properties")) {
            properties.load(input);
        } catch (Throwable e) {
            logger.error(" Get properties from resource failed with root cause : ", e);
        }
        return properties.getProperty(key);
    }

    public static Map<String, String> readKeysFromPropertiesFile(String prefix) {
        Map<String, String> keyValueMap = new HashMap<>();
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("src/main/resources/config.properties")) {
            properties.load(input);
            for (String key : properties.stringPropertyNames()) {
                if (key.startsWith(prefix)) {
                    String value = properties.getProperty(key);
                    String mapKey = key.replace(prefix, "");
                    keyValueMap.put(mapKey, value);
                }
            }
        } catch (IOException e) {
            logger.error(" Get properties from resource failed with root cause : ", e);
        }
        return keyValueMap; // TODO có thể return khi không đoc đc
    }
}
