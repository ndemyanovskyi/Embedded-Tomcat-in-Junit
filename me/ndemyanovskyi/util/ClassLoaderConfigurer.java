package me.ndemyanovskyi.util;

import com.google.common.base.Throwables;
import org.springframework.util.ReflectionUtils;
import sun.misc.SharedSecrets;
import sun.misc.URLClassPath;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class ClassLoaderConfigurer {
    
    public static void configure(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            URLClassLoader urlClassLoader = (URLClassLoader) classLoader;
            DeclassifiedURLClassPath urlClassPath = DeclassifiedURLClassPath.from(urlClassLoader);
            for (URL url : urlClassPath.getLoaderUrls()) {
                if (!isFileUrl(url) || isFileExistsByUrl(url)) {
                    urlClassPath.addURL(url);
                }
            }
        }
    }
    
    private static boolean isFileUrl(URL url) {
        return url.getProtocol().equals("file");
    }
    
    private static boolean isFileExistsByUrl(URL url) {
        try {
            return new File(url.toURI()).exists();
        } catch (URISyntaxException e) {
            throw Throwables.propagate(e);
        }
    }
    
    private static class DeclassifiedURLClassPath {
        
        private final URLClassPath urlClassPath;
        
        private DeclassifiedURLClassPath(URLClassPath urlClassPath) {
            this.urlClassPath = Objects.requireNonNull(urlClassPath, "urlClassPath");
        }
        
        public static DeclassifiedURLClassPath from(URLClassLoader urlClassLoader) {
            return new DeclassifiedURLClassPath(SharedSecrets.getJavaNetAccess().getURLClassPath(urlClassLoader));
        }
        
        public Set<URL> getLoaderUrls() {
            Set<URL> resourceUrls = new LinkedHashSet<>();
            for (String resource : getLoaderMap().keySet()) {
                try {
                    resourceUrls.add(new URL(resource));
                } catch (MalformedURLException e) {
                    throw Throwables.propagate(e);
                }
            }
            return resourceUrls;
        }
        
        @SuppressWarnings("unchecked")
        public Map<String, ?> getLoaderMap() {
            Field field = ReflectionUtils.findField(urlClassPath.getClass(), "lmap");
            ReflectionUtils.makeAccessible(field);
            return (Map<String, ?>) ReflectionUtils.getField(field, urlClassPath);
        }
        
        public void addURL(URL url) {
            this.urlClassPath.addURL(url);
        }
    }
}
