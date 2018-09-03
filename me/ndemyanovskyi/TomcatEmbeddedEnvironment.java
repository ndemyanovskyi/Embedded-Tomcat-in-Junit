package me.ndemyanovskyi;

import com.google.common.base.Throwables;
import me.ndemyanovskyi.util.ClassLoaderConfigurer;
import me.ndemyanovskyi.util.TomcatFactory;
import org.apache.catalina.Container;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.springframework.util.Assert;
import org.springframework.web.context.WebApplicationContext;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.springframework.web.context.WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE;

public class TomcatEmbeddedEnvironment {
    
    private final Tomcat tomcat;
    private final Context context;
    private final URL url;
    
    private final AtomicBoolean setUpPerformed = new AtomicBoolean(false);
    private final AtomicBoolean tearDownPerformed = new AtomicBoolean(false);
    
    private WebApplicationContext webApplicationContext;
    
    public TomcatEmbeddedEnvironment(String contextPath) {
        this(contextPath, 0);
    }
    
    public TomcatEmbeddedEnvironment(String contextPath, int port) {
        this.tomcat = new TomcatFactory(contextPath, port).create();
        this.context = findContext(tomcat, contextPath);
        this.url = buildUrl(tomcat, context);
    }
    
    public void setUp() throws LifecycleException {
        Assert.state(setUpPerformed.compareAndSet(false, true), "TomcatEnvironment already set up.");
        
        ClassLoaderConfigurer.configure(getClass().getClassLoader());
        
        tomcat.start();
        if(!context.getState().isAvailable()) {
            throw new LifecycleException("Tomcat context failed to initialize.");
        }
        webApplicationContext = (WebApplicationContext) context.getServletContext().getAttribute(ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE);
    }
    
    public void tearDown() {
        Assert.state(!setUpPerformed.get(), "TomcatEnvironment not already set up.");
        Assert.state(tearDownPerformed.compareAndSet(false, true), "TomcatEnvironment already tear down.");
        
        try {
            tomcat.stop();
            tomcat.destroy();
        } catch (LifecycleException e) {
            throw Throwables.propagate(e);
        }
    }
    
    public WebApplicationContext getWebApplicationContext() {
        return webApplicationContext;
    }
    
    public URL getUrl() {
        return url;
    }
    
    private static Context findContext(Tomcat tomcat, String contextPath) {
        Container child = tomcat.getHost().findChild(contextPath);
        if(!(child instanceof Context)) {
            throw new IllegalArgumentException("Context does not found by path: " + contextPath + ". Found by path: " + child);
        }
        return (Context) child;
    }
    
    private static URL buildUrl(Tomcat tomcat, Context context) {
        try {
            return new URL("http://" + tomcat.getHost().getName() + ":" + tomcat.getConnector().getPort() + context.getPath());
        } catch (MalformedURLException e) {
            throw Throwables.propagate(e);
        }
    }
}
