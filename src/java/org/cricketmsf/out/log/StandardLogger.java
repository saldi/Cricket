/*
 * Copyright 2016 Grzegorz Skorupa <g.skorupa at gmail.com>.
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
package org.cricketmsf.out.log;

import org.cricketmsf.Adapter;
import org.cricketmsf.Event;
import org.cricketmsf.out.OutboundAdapter;
import java.util.HashMap;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author greg
 */
public class StandardLogger extends OutboundAdapter implements Adapter, LoggerAdapterIface {

    //Logger logger;
    Level level = null;
    private String name;

    public static final Logger logger = Logger.getGlobal();

    @Override
    public void loadProperties(HashMap<String, String> properties) {
        setName(properties.get("name"));
        System.out.println("logger name: " + getName());
        setLoggingLevel(properties.get("level"));

        Handler systemOut = new ConsoleHandler();
        systemOut.setLevel(level);
        systemOut.setFormatter(new StandardLoggerFormatter());
        logger.addHandler(systemOut);
        logger.setLevel(level);
        // Prevent logs from processed by default Console handler.
        logger.setUseParentHandlers(false);

        System.out.println("logging level: " + logger.getLevel().getName());
    }

    public void log(Event event) {
        String level = event.getType();
        switch (level) {
            case "LOG_INFO":
            case "INFO":
                logger.log(Level.INFO, event.toLogString());
                break;
            case "LOG_FINEST":
            case "FINEST":
                logger.log(Level.FINEST, event.toLogString());
                break;
            case "LOG_FINER":
            case "FINER":
                logger.log(Level.FINER, event.toLogString());
                break;
            case "LOG_FINE":
            case "FINE":
                logger.log(Level.FINE, event.toLogString());
                break;
            case "LOG_WARNING":
            case "WARNING":
                logger.log(Level.WARNING, event.toLogString());
                break;
            case "LOG_SEVERE":
            case "SEVERE":
                logger.log(Level.SEVERE, event.toLogString());
                break;
            default:
                logger.log(Level.FINEST, event.toLogString());
                break;
        }
    }

    private void setLoggingLevel(String level) {
        try {
            this.level = Level.parse(level);
        } catch (Exception e) {
            this.level = Level.ALL;
        }
    }

    private Level getLoggingLevel() {
        if (level == null) {
            level = Level.ALL;
        }
        return level;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        if (null != name && !name.isEmpty()) {
            this.name = name;
        } else {
            this.name = this.getClass().getName();
        }
    }
}