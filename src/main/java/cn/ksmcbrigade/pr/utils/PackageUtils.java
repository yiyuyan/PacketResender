package cn.ksmcbrigade.pr.utils;

import net.minecraft.client.Minecraft;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.ibm.icu.impl.ClassLoaderUtil.getClassLoader;

public class PackageUtils {
    /**
     * 获取包下所有类
     *
     * @param packageName 包路径
     * @return 类集合
     */
    public static Set<Class<?>> getClasses(String packageName) throws IOException, NoSuchMethodException {
        ClassLoader classLoader = PackageUtils.class.getClassLoader();
        assert classLoader != null;
        String path = packageName.replace('.', '/');
        //System.out.println(path);
        Enumeration<URL> resources = classLoader.getResources(path);
        Set<Class<?>> classes = new LinkedHashSet<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            /*if ("file".equals(resource.getProtocol())) {
                processDirectory(classes, resource, packageName);
            } else if ("jar".equals(resource.getProtocol())) {
                processJarFile(classes, resource, packageName);
            }*/

            System.out.println(resource.getProtocol());
            boolean dev = true;
            for (Method declaredMethod : Minecraft.class.getDeclaredMethods()) {
                if (declaredMethod.getName().equalsIgnoreCase("m_91087_")) {
                    dev = false;
                    break;
                }
            }
            if (dev) {
                processDirectory(classes, resource, packageName);
                //System.out.println("11111111111111111");
            } else {
                //System.err.println("2222222222222222222222");
                processJarFile(classes, resource, packageName);
            }
        }
        return classes;
    }

    /**
     * 递归遍历目录下的所有文件
     *
     * @param classes     类集合
     * @param directory   目录
     * @param packageName 包名
     */
    private static void processDirectory(Set<Class<?>> classes, URL directory, String packageName) throws UnsupportedEncodingException, MalformedURLException {
        String path = URLDecoder.decode(directory.getFile(), StandardCharsets.UTF_8);
        /*System.out.println(directory.getFile());
        System.out.println(new File(directory.getFile()).getAbsolutePath());
        System.out.println(path);*/
        File[] files = new File(path).listFiles();
        /*System.out.println(Arrays.toString(files));*/
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    processDirectory(classes, file.toURI().toURL(), packageName + "." + file.getName());
                } else if (file.getName().endsWith(".class")) {
                    String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                    try {
                        classes.add(Class.forName(className, false, getClassLoader()));
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    /**
     * 解析jar包中的类集合
     *
     * @param classes     类集合
     * @param jarFileUrl  jar包
     * @param packageName 包名
     */
    private static void processJarFile(Set<Class<?>> classes, URL jarFileUrl, String packageName) throws IOException {
        JarFile jarFile = null;
        try {
            URLConnection jarURLConnection = jarFileUrl.openConnection();
            if (jarURLConnection != null) {
                jarFile = new JarFile(URLDecoder.decode(jarFileUrl.getPath().split("%")[0].substring(1), StandardCharsets.UTF_8));
                if (jarFile != null) {
                    Enumeration<JarEntry> jarEntries = jarFile.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        String jarEntryName = jarEntry.getName();
                        if (jarEntryName.startsWith(packageName.replace('.', '/') + '/') && jarEntryName.endsWith(".class")) {
                            String className = jarEntryName.substring(0, jarEntryName.lastIndexOf(".")).replaceAll("/", ".");
                            try {
                                classes.add(Class.forName(className));
                            } catch (ClassNotFoundException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing the Jar file: " + e.getMessage());
        } finally {
            // 在 finally 块中确保 JarFile 被正确关闭
            if (jarFile != null) {
                try {
                    jarFile.close();
                } catch (IOException e) {
                    System.err.println("Error closing the Jar file: " + e.getMessage());
                }
            }
        }
    }

    /**
     * 获取类的方法列表
     *
     * @param clazz 类
     * @return 方法列表
     */
    public static List<Map<String, Object>> getMethodsByClass(Class<?> clazz) {
        List<Map<String, Object>> methodList = new ArrayList<>();
        Arrays.stream(clazz.getDeclaredMethods()).forEach(method -> {
            Map<String, Object> methodNode = new HashMap<>();
            methodNode.put("methodName", method.getName());
            methodNode.put("methodPath", method.toString());
            methodList.add(methodNode);
        });
        return methodList;
    }
}
