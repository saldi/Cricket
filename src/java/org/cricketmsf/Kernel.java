/*
 * Copyright 2015 Grzegorz Skorupa <g.skorupa at gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cricketmsf;

import com.cedarsoftware.util.io.JsonWriter;
import org.cricketmsf.annotation.EventHook;
import com.sun.net.httpserver.Filter;
import org.cricketmsf.config.AdapterConfiguration;
import org.cricketmsf.config.ConfigSet;
import org.cricketmsf.config.Configuration;
import org.cricketmsf.in.InboundAdapter;
import org.cricketmsf.out.OutboundAdapter;
import java.util.logging.Logger;
import static java.lang.Thread.MIN_PRIORITY;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;
import java.util.logging.Level;
import org.cricketmsf.config.HttpHeader;
import org.cricketmsf.out.DispatcherException;
import org.cricketmsf.out.DispatcherIface;
import org.cricketmsf.out.EventDispatcherAdapter;
import org.cricketmsf.out.log.LoggerAdapterIface;
import org.cricketmsf.out.log.StandardLogger;

/**
 * Microkernel.
 *
 * @author Grzegorz Skorupa
 */
public abstract class Kernel {

    // emergency LOGGER
    private static final Logger LOGGER = Logger.getLogger(org.cricketmsf.Kernel.class.getName());
    // standard logger
    protected static LoggerAdapterIface logger = new StandardLogger().getDefault();

    // singleton
    private static Object instance = null;

    private UUID uuid; //autogenerated when service starts - unpredictable
    private HashMap<String, String> eventHookMethods = new HashMap<>();
    private String id; //identifying service 
    private String name; // name identifying service deployment (various names will have the same id)
    public boolean liftMode = false;

    // adapters
    public HashMap<String, Object> adaptersMap = new HashMap<>();
    
    // event dispatcher
    private DispatcherIface eventDispatcher = null;

    // user defined properties
    public HashMap<String, Object> properties = new HashMap<>();
    public SimpleDateFormat dateFormat = null;

    // http server
    private String host = null;
    private int port = 0;
    private Httpd httpd;
    private boolean httpHandlerLoaded = false;
    private boolean inboundAdaptersLoaded = false;

    private static long eventSeed = System.currentTimeMillis();

    protected ConfigSet configSet = null;

    private Filter securityFilter = new SecurityFilter();
    private ArrayList corsHeaders;

    private long startedAt = 0;
    private boolean started = false;

    public Kernel() {
    }

    public boolean isStarted() {
        return started;
    }

    void setStartedAt(long time) {
        startedAt = time;
    }

    private void addHookMethodNameForEvent(String eventCategory, String hookMethodName) {
        eventHookMethods.put(eventCategory, hookMethodName);
    }

    private void getEventHooks() {
        EventHook ah;
        String eventCategory;
        getLogger().print("REGISTERING EVENT HOOKS");
        // for every method of a Kernel instance (our service class extending Kernel)
        for (Method m : this.getClass().getMethods()) {
            ah = (EventHook) m.getAnnotation(EventHook.class);
            // we search for annotated method
            if (ah != null) {
                eventCategory = ah.eventCategory();
                addHookMethodNameForEvent(eventCategory, m.getName());
                getLogger().print("hook method for event category " + eventCategory + " : " + m.getName());
            }
        }
        getLogger().print("END REGISTERING EVENT HOOKS");
    }

    private String getHookMethodNameForEvent(String eventCategory) {
        String result;
        result = eventHookMethods.get(eventCategory);
        if (null == result) {
            result = eventHookMethods.get("*");
        }
        return result;
    }

    /**
     * Invokes the service method annotated as dedicated to this event category
     *
     * @param event event object that should be processed
     */
    public Object handleEvent(Event event) {
        Object o = null;
        try {
            Method m = getClass()
                    .getMethod(getHookMethodNameForEvent(event.getCategory()), Event.class);
            o = m.invoke(this, event);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return o;
    }
    
    /**
     * Sends event object to the event queue using registered dispatcher adapter. In case the dispatcher adapter is not registered or throws exception,
     * the Kernel.handle(event) method will be called. 
     * 
     * @param event the event object that should be send to the event queue 
     * @return null if dispatcher adapter is registered, otherwise returns result of the Kernel.handle(event) method
     */
    public Object dispatchEvent(Event event){
            try {
                eventDispatcher.dispatch(event);
                return null;
            } catch (NullPointerException | DispatcherException ex) {
                return handleEvent(event);
            }
    }

    /**
     * Invokes the service method annotated as dedicated to this event category
     *
     * @param event event object that should be processed
     */
    public static Object handle(Event event) {
        return Kernel.getInstance().handleEvent(event);
    }

    public HashMap<String, Object> getAdaptersMap() {
        return adaptersMap;
    }

    protected Object getRegistered(String adapterName) {
        return adaptersMap.get(adapterName);
    }

    //protected Object registerAdapter(String adapterName, Object adapter) {
    //    return adaptersMap.put(adapterName, adapter);
    //}

    /**
     * Returns next unique identifier for Event.
     *
     * @return next unique identifier
     */
    public static long getEventId() {
        return eventSeed += 1;
    }

    /**
     * Must be used to set adapter variables after instantiating them according
     * to the configuration in cricket.json file. Look at EchoService example.
     */
    public abstract void getAdapters();

    public static Kernel getInstance() {
        return (Kernel) instance;
    }

    public static Object getInstanceWithProperties(Class c, Configuration config) {
        if (instance != null) {
            return instance;
        }
        try {
            instance = c.newInstance();
            ((Kernel) instance).setUuid(UUID.randomUUID());
            ((Kernel) instance).setId(config.getId());
            ((Kernel) instance).setName((String) config.getProperties().getOrDefault("SRVC_NAME_ENV_VARIABLE", "CRICKET_NAME"));
            ((Kernel) instance).setProperties(config.getProperties());
            ((Kernel) instance).configureTimeFormat(config);
            ((Kernel) instance).loadAdapters(config);
        } catch (Exception e) {
            instance = null;
            LOGGER.log(Level.SEVERE, "{0}:{1}", new Object[]{e.getStackTrace()[0].toString(), e.getStackTrace()[1].toString()});
            e.printStackTrace();
        }
        return instance;
    }

    private void configureTimeFormat(Configuration config) {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z");
        dateFormat.setTimeZone(TimeZone.getTimeZone(config.getProperty("time-zone", "GMT")));
    }

    private void printHeader(String version) {
        getLogger().print("");
        getLogger().print("  __|  \\  | __|  Cricket");
        getLogger().print(" (    |\\/ | _|   Microservices Framework");
        getLogger().print("\\___|_|  _|_|    version " + version);
        getLogger().print("");
        // big text generated using http://patorjk.com/software/taag
    }

    /**
     * Instantiates adapters following configuration in cricket.json
     *
     * @param config Configuration object loaded from cricket.json
     * @throws Exception
     */
    private synchronized void loadAdapters(Configuration config) throws Exception {
        setHttpHandlerLoaded(false);
        setInboundAdaptersLoaded(false);
        getLogger().print("LOADING SERVICE PROPERTIES FOR " + config.getService());
        getLogger().print("\tUUID=" + getUuid().toString());
        getLogger().print("\tenv name=" + getName());
        //setHost(config.getHost());
        setHost(config.getProperty("host", "0.0.0.0"));
        getLogger().print("\thost=" + getHost());
        try {
            //setPort(Integer.parseInt(config.getPort()));
            setPort(Integer.parseInt(config.getProperty("port", "8080")));
        } catch (Exception e) {
            e.printStackTrace();
        }
        getLogger().print("\tport=" + getPort());
        setSecurityFilter(config.getProperty("filter"));
        getLogger().print("\tfilter=" + getSecurityFilter().getClass().getName());
        setCorsHeaders(config.getProperty("cors"));
        getLogger().print("\tCORS=" + getCorsHeaders());
        getLogger().print("\tExtended properties: " + getProperties().toString());
        getLogger().print("LOADING ADAPTERS");
        String adapterName = null;
        AdapterConfiguration ac = null;
        try {
            HashMap<String, AdapterConfiguration> adcm = config.getAdapters();
            for (Map.Entry<String, AdapterConfiguration> adapterEntry : adcm.entrySet()) {
                adapterName = adapterEntry.getKey();
                ac = adapterEntry.getValue();
                getLogger().print("ADAPTER: " + adapterName);
                try {
                    Class c = Class.forName(ac.getClassFullName());
                    adaptersMap.put(adapterName, c.newInstance());
                    if (adaptersMap.get(adapterName) instanceof org.cricketmsf.in.http.HttpAdapter) {
                        setHttpHandlerLoaded(true);
                    } else if (adaptersMap.get(adapterName) instanceof org.cricketmsf.in.InboundAdapter) {
                        setInboundAdaptersLoaded(true);
                    }
                    if (adaptersMap.get(adapterName) instanceof org.cricketmsf.out.EventDispatcherAdapter) {
                        setEventDispatcher(adaptersMap.get(adapterName));
                    }
                    // loading properties
                    java.lang.reflect.Method loadPropsMethod = c.getMethod("loadProperties", HashMap.class, String.class);
                    loadPropsMethod.invoke(adaptersMap.get(adapterName), ac.getProperties(), adapterName);
                } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException | SecurityException | IllegalArgumentException | InvocationTargetException ex) {
                    adaptersMap.put(adapterName, null);
                    getLogger().print("ERROR: " + adapterName + " configuration: " + ex.getClass().getSimpleName());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Adapters initialization error. Configuration for: {0}", adapterName);
            throw new Exception(e);
        }
        getLogger().print("END LOADING ADAPTERS");
        getLogger().print("");
    }
    
    private void setEventDispatcher(Object adapter){
        eventDispatcher = (EventDispatcherAdapter)adapter;
    }

    private void setSecurityFilter(String filterName) {
        try {
            Class c = Class.forName(filterName);
            securityFilter = (Filter) c.newInstance();
        } catch (ClassCastException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
            securityFilter = new SecurityFilter();
        }
    }

    public Filter getSecurityFilter() {
        return securityFilter;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the port
     */
    public int getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * @return the httpd
     */
    private Httpd getHttpd() {
        return httpd;
    }

    /**
     * @param httpd the httpd to set
     */
    private void setHttpd(Httpd httpd) {
        this.httpd = httpd;
    }

    /**
     * @return the httpHandlerLoaded
     */
    private boolean isHttpHandlerLoaded() {
        return httpHandlerLoaded;
    }

    /**
     * @param httpHandlerLoaded the httpHandlerLoaded to set
     */
    private void setHttpHandlerLoaded(boolean httpHandlerLoaded) {
        this.httpHandlerLoaded = httpHandlerLoaded;
    }

    /**
     * This method will be invoked when Kernel is executed without --run option
     */
    public void runOnce() {
        getEventHooks();
        getAdapters();
        setKeystores();
        printHeader(Kernel.getInstance().configSet.getKernelVersion());
    }

    private void setKeystores() {
        String keystore;
        String keystorePass;
        String truststore;
        String truststorePass;

        keystore = (String) getProperties().getOrDefault("keystore", "");
        keystorePass = (String) getProperties().get("keystore-password");
        truststore = (String) getProperties().getOrDefault("keystore", "");
        truststorePass = (String) getProperties().get("keystore-password");

        if (!keystore.isEmpty() && !keystorePass.isEmpty()) {
            System.setProperty("javax.net.ssl.keyStore", keystore);
            System.setProperty("javax.net.ssl.keyStorePassword", keystorePass);
        }
        if (!truststore.isEmpty() && !truststorePass.isEmpty()) {
            System.setProperty("javax.net.ssl.trustStore", truststore);
            System.setProperty("javax.net.ssl.trustStorePassword", truststorePass);
        }
    }

    /**
     * Starts the service instance
     *
     * @throws InterruptedException
     */
    public void start() throws InterruptedException {
        getAdapters();
        getEventHooks();
        setKeystores();
        if (isHttpHandlerLoaded() || isInboundAdaptersLoaded()) {

            Runtime.getRuntime().addShutdownHook(
                    new Thread() {
                public void run() {
                    try {
                        shutdown();
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            });

            getLogger().print("Running initialization tasks");
            runInitTasks();

            getLogger().print("Starting listeners ...");
            // run listeners for inbound adapters
            runListeners();

            getLogger().print("Starting http listener ...");
            setHttpd(new Httpd(this));
            getHttpd().run();

            long startedIn = System.currentTimeMillis() - startedAt;
            printHeader(Kernel.getInstance().configSet.getKernelVersion());
            if (liftMode) {
                getLogger().print("# Service: " + getClass().getName());
            } else {
                getLogger().print("# Service: " + getId());
            }
            getLogger().print("# UUID: " + getUuid());
            getLogger().print("# NAME: " + getName());
            getLogger().print("#");
            if (getHttpd().isSsl()) {
                getLogger().print("# HTTPS listening on port " + getPort());
            } else {
                getLogger().print("# HTTP listening on port " + getPort());
            }
            getLogger().print("#");
            getLogger().print("# Started in " + startedIn + "ms. Press Ctrl-C to stop");
            getLogger().print("");
            runFinalTasks();
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            started = true;
        } else {
            getLogger().print("Couldn't find any http request hook method. Exiting ...");
            System.exit(MIN_PRIORITY);
        }
    }

    /**
     * Could be overriden in a service implementation to run required code at
     * the service start. As the last step of the service starting procedure
     * before HTTP service.
     */
    protected void runInitTasks() {
    }

    /**
     * Could be overriden in a service implementation to run required code at
     * the service start. As the last step of the service starting procedure
     * after HTTP service.
     */
    protected void runFinalTasks() {

    }

    protected void runListeners() {
        for (Map.Entry<String, Object> adapterEntry : getAdaptersMap().entrySet()) {
            if (adapterEntry.getValue() instanceof org.cricketmsf.in.InboundAdapter) {
                if (!(adapterEntry.getValue() instanceof org.cricketmsf.in.http.HttpAdapter)) {
                    (new Thread((InboundAdapter) adapterEntry.getValue())).start();
                    getLogger().print(adapterEntry.getKey() + " (" + adapterEntry.getValue().getClass().getSimpleName() + ")");
                }
            }
        }
    }

    public void shutdown() {

        getLogger().print("Shutting down ...");
        for (Map.Entry<String, Object> adapterEntry : getAdaptersMap().entrySet()) {
            if (adapterEntry.getValue() instanceof org.cricketmsf.in.InboundAdapter) {
                ((InboundAdapter) adapterEntry.getValue()).destroy();
            } else if (adapterEntry.getValue() instanceof org.cricketmsf.out.OutboundAdapter) {
                ((OutboundAdapter) adapterEntry.getValue()).destroy();
            }
        }
        try {
            getHttpd().stop();
        } catch (NullPointerException e) {
        }
        System.out.println("Kernel stopped\r\n");
    }

    /**
     * @return the configSet
     */
    public ConfigSet getConfigSet() {
        return configSet;
    }

    /**
     * @param configSet the configSet to set
     */
    public void setConfigSet(ConfigSet configSet) {
        this.configSet = configSet;
    }

    /**
     * Return service instance unique identifier
     *
     * @return the uuid
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * @param id the id to set
     */
    private void setId(String id) {
        this.id = id;
    }

    /**
     * @return the corsHeaders
     */
    public ArrayList getCorsHeaders() {
        return corsHeaders;
    }

    /**
     * @param corsHeaders the corsHeaders to set
     */
    public void setCorsHeaders(String corsHeaders) {
        this.corsHeaders = new ArrayList<>();
        if (corsHeaders != null) {
            String[] headers = corsHeaders.split("\\|");
            for (String header : headers) {
                try {
                    this.corsHeaders.add(
                            new HttpHeader(
                                    header.substring(0, header.indexOf(":")).trim(),
                                    header.substring(header.indexOf(":") + 1).trim()
                            )
                    );
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * @return the properties
     */
    public HashMap<String, Object> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(HashMap<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * @return the inboundAdaptersLoaded
     */
    public boolean isInboundAdaptersLoaded() {
        return inboundAdaptersLoaded;
    }

    /**
     * @param inboundAdaptersLoaded the inboundAdaptersLoaded to set
     */
    public void setInboundAdaptersLoaded(boolean inboundAdaptersLoaded) {
        this.inboundAdaptersLoaded = inboundAdaptersLoaded;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param variableName system environment variable name which holds the name
     * of this service
     */
    public void setName(String variableName) {
        String tmp = null;
        try {
            tmp = System.getenv(variableName);
        } catch (Exception e) {
        }
        this.name = tmp != null ? tmp : "CricketService";
    }

    /**
     * @return the logger
     */
    public static LoggerAdapterIface getLogger() {
        return logger;
    }

    /**
     * Returns map of the current service properties along with list of statuses
     * reported by all registered (running) adapters
     *
     * @return status map
     */
    public Map reportStatus() {
        HashMap status = new HashMap();

        status.put("name", getName());
        status.put("id", getId());
        status.put("uuid", getUuid().toString());
        status.put("class", getClass().getName());
        status.put("totalMemory", Runtime.getRuntime().totalMemory());
        status.put("maxMemory", Runtime.getRuntime().maxMemory());
        status.put("freeMemory", Runtime.getRuntime().freeMemory());
        status.put("threads", Thread.activeCount());
        ArrayList adapters = new ArrayList();

        adaptersMap.keySet().forEach(key -> {
            try {
                adapters.add(
                        ((Adapter) adaptersMap.get(key)).getStatus(key));
            } catch (Exception e) {
                handle(Event.logFine(this, key + " adapter is not registered"));
            }
        });
        status.put("adapters", adapters);
        return status;
    }

    /**
     * Returns status map formated as JSON
     *
     * @return JSON representation of the statuses map
     */
    public String printStatus() {
        HashMap args = new HashMap();
        args.put(JsonWriter.PRETTY_PRINT, true);
        args.put(JsonWriter.DATE_FORMAT, "dd/MMM/yyyy:kk:mm:ss Z");
        args.put(JsonWriter.TYPE, false);
        return JsonWriter.objectToJson(reportStatus(), args);
    }

}
