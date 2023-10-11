package vn.vnpay.rabbitmqrpc.scan;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class PackageScanner {
    private PackageScanner() {
    }

    public static List<Class<?>> getClasses(String packageName) throws ClassNotFoundException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        List<Class<?>> classes = new ArrayList<>();
        File dir = new File(classLoader.getResource(path).getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                classes.addAll(getClasses(packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                classes.add(Class.forName(className));
            }
        }
        return classes;
    }
}
