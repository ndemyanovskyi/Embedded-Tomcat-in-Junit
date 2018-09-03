package me.ndemyanovskyi.util;

import com.google.common.base.Throwables;
import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.deploy.FilterDef;
import org.apache.catalina.deploy.FilterMap;
import org.apache.catalina.startup.ContextConfig;
import org.apache.catalina.startup.Tomcat;
import org.apache.tomcat.util.scan.StandardJarScanner;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;

public class TomcatFactory {

    private static final String TOMCAT_TEMP_DIRECTORY_PREFIX = "tomcat";
    private static final String TOMCAT_DOCBASE_TEMP_DIRECTORY_PREFIX = "tomcat-docbase";
    
    public static final int DEFAULT_SESSION_TIMEOUT = 30;
    public static final String DEFAULT_CHARSET = StandardCharsets.UTF_8.name();
    public static final String DEFAULT_PROTOCOL = "org.apache.coyote.http11.Http11NioProtocol";

    private int port = 0;
    private String contextPath = "";
    
    public TomcatFactory(String contextPath, int port) {
        setPort(port);
        setContextPath(contextPath);
    }
    
    public Tomcat create() {
        Tomcat tomcat = new Tomcat();
        tomcat.setBaseDir(createTempDirectory(TOMCAT_TEMP_DIRECTORY_PREFIX));
        tomcat.getHost().setAutoDeploy(false);
        addConnector(tomcat);
        addContext(tomcat);
        return tomcat;
    }
    
    private void addConnector(Tomcat tomcat) {
        Connector connector = new Connector(DEFAULT_PROTOCOL);
        configureConnector(connector);
        tomcat.getService().addConnector(connector);
        tomcat.setConnector(connector);
    }
    
    private void configureConnector(Connector connector) {
        connector.setPort(resolvePort(getPort()));
        connector.setURIEncoding(DEFAULT_CHARSET);
        connector.setProperty("bindOnInit", "false");
    }
    
    private void addContext(Tomcat tomcat) {
        Context context = new StandardContext();
        Tomcat.initWebappDefaults(context);
        configureContext(context);
        tomcat.getHost().addChild(context);
    }
    
    @SuppressWarnings("deprecation")
    private void configureContext(Context context) {
        context.setName(getContextPath());
        context.setPath(getContextPath());
        context.setDocBase(createTempDirectory(TOMCAT_DOCBASE_TEMP_DIRECTORY_PREFIX));
        context.setXmlValidation(false);
        context.setXmlNamespaceAware(false);
        context.setTldValidation(false);
        context.setTldNamespaceAware(false);
        context.addLifecycleListener(new ContextConfig());
        
        configureJarScanner(context);
    }
    
    private void configureJarScanner(Context context) {
        StandardJarScanner jarScanner = (StandardJarScanner) context.getJarScanner();
        jarScanner.setScanClassPath(true);
        jarScanner.setScanAllFiles(true);
        jarScanner.setScanAllDirectories(true);
    }
    
    private static String createTempDirectory(String prefix) {
        try {
            File tempDir = File.createTempFile(prefix, "");
            if (tempDir.delete() && tempDir.mkdir()) {
                tempDir.deleteOnExit();
                return tempDir.getAbsolutePath();
            } else {
                throw new IOException("Cannot create temp file.");
            }
        } catch (IOException ex) {
            throw Throwables.propagate(ex);
        }
    }
    
    private static int resolvePort(int port) {
        try (ServerSocket socket = new ServerSocket(port)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }
    
    public int getPort() {
        return port;
    }
    
    public String getContextPath() {
        return contextPath;
    }
    
    public void setPort(int port) {
        this.port = port;
    }
    
    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
