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
package com.gskorupa.cricket.services;

import com.gskorupa.cricket.ArgumentParser;
import com.gskorupa.cricket.Event;
import com.gskorupa.cricket.EventHook;
import com.gskorupa.cricket.HttpAdapterHook;
import com.gskorupa.cricket.Kernel;
import com.gskorupa.cricket.RequestObject;
import com.gskorupa.cricket.in.HttpAdapter;
import java.util.logging.Logger;
import com.gskorupa.cricket.in.ParameterMapResult;
import com.gskorupa.cricket.out.LoggerAdapterIface;
import java.util.HashMap;
import java.util.Map;
import com.gskorupa.cricket.in.EchoHttpAdapterIface;

/**
 * EchoService
 *
 * @author greg
 */
public class EchoService extends Kernel {

    // emergency logger
    private static final Logger logger = Logger.getLogger(com.gskorupa.cricket.services.EchoService.class.getName());

    // adapterClasses
    LoggerAdapterIface logAdapter = null;
    EchoHttpAdapterIface httpAdapter = null;

    public EchoService() {
        adapters = new Object[2];
        adapters[0] = logAdapter;
        adapters[1] = httpAdapter;
        adapterClasses = new Class[2];
        adapterClasses[0] = LoggerAdapterIface.class;
        adapterClasses[1] = EchoHttpAdapterIface.class;
    }

    @Override
    public void getAdapters() {
        logAdapter = (LoggerAdapterIface) super.adapters[0];
        httpAdapter = (EchoHttpAdapterIface) super.adapters[1];
    }

    @Override
    public void runOnce() {
        Event e=new Event("DummyService.runOnce()","LOG",Event.LOG_INFO, "executed");
        logEvent(e);
        System.out.println("Hello from DummyService.runOnce()");
    }

    @HttpAdapterHook(handlerClassName = "EchoHttpAdapterIface", requestMethod = "GET")
    public Object doGet(RequestObject request) {
        return sendEcho(request);
    }
    
    @HttpAdapterHook(handlerClassName = "EchoHttpAdapterIface", requestMethod = "POST")
    public Object doPost(RequestObject request) {
        return sendEcho(request);
    }
    
    @HttpAdapterHook(handlerClassName = "EchoHttpAdapterIface", requestMethod = "PUT")
    public Object doPut(RequestObject request) {
        return sendEcho(request);
    }
    
    @HttpAdapterHook(handlerClassName = "EchoHttpAdapterIface", requestMethod = "DELETE")
    public Object doDelete(RequestObject request) {
        return sendEcho(request);
    }
    
    @EventHook(eventCategory = "LOG")
    public void logEvent(Event event){
        logAdapter.log(event);
    }
    
    @EventHook(eventCategory = "*")
    public void processEvent(Event event){
        //does nothing
    }
    
    public Object sendEcho(RequestObject request) {
        ParameterMapResult r = new ParameterMapResult();
        HashMap<String, String> data=new HashMap();
        Map<String, Object> map = request.parameters;
        data.put("request.method",request.method);
        data.put("request.pathExt",request.pathExt);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            //System.out.println(entry.getKey() + "=" + entry.getValue());
            data.put(entry.getKey(), (String)entry.getValue());
        }
        if (data.containsKey("error")){
            r.setCode(HttpAdapter.SC_INTERNAL_SERVER_ERROR);
            data.put("error", "error forced by request");
        } else {
            r.setCode(HttpAdapter.SC_OK);
        }
        r.setData(data);
        return r;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        final EchoService service;

        ArgumentParser arguments = new ArgumentParser(args);
        if (arguments.isProblem()) {
            if (arguments.containsKey("error")) {
                System.out.println(arguments.get("error"));
            }
            System.out.println(new EchoService().getHelp());
            System.exit(-1);
        }

        try {
            if (arguments.containsKey("config")) {
                service = (EchoService) EchoService.getInstance(EchoService.class, arguments.get("config"));
            } else {
                service = (EchoService) EchoService.getInstanceUsingResources(EchoService.class);
            }
            service.getAdapters();

            if (arguments.containsKey("run")) {
                service.start();
            } else {
                System.out.println("Executing runOnce method");
                service.runOnce();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}