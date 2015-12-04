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
package com.gskorupa.cricket;

import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class Httpd {

    private Service service;
    public HttpServer server=null;

    public Httpd(Service service) {
        this.service = service;
        try {
            server = HttpServer.create(new InetSocketAddress(service.getPort()), 0);
        } catch (IOException e) {
            e.printStackTrace();
        }
        HttpContext context;
        for (int i = 0; i < service.fields.length; i++) {
            if (service.fields[i] instanceof com.sun.net.httpserver.HttpHandler) {
                System.out.println("creating context: "+((Adapter) service.fields[i]).getContext());
                context=server.createContext(((Adapter) service.fields[i]).getContext(), (com.sun.net.httpserver.HttpHandler) service.fields[i]);
                context.getFilters().add(new ParameterFilter());
            }
        }
    }

    public void run() {
        //Create a default executor
        server.setExecutor(null);
        server.start();
    }

}