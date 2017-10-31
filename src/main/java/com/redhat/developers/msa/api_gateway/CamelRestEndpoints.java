/**
 * JBoss, Home of Professional Open Source
 * Copyright 2017, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.redhat.developers.msa.api_gateway;

import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.camel.LoggingLevel;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.rest.RestBindingMode;
import org.apache.camel.util.toolbox.AggregationStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.narayana.lra.client.LRAClient;
import io.narayana.lra.client.LRAClientAPI;

@Component
public class CamelRestEndpoints extends RouteBuilder {

    @Value("${service.host}")
    private String serviceHost;

    @Autowired
    private LRAClientAPI lraClient;

    @Override
    public void configure() throws Exception {
        // System.out.println("OK, lra client is ready to go: " + lraClient + " with current lra: " + lraClient.getCurrent());
        // String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
        // URL lraUrlId = lraClient.startLRA(null, CamelRestEndpoints.class.getName() + "#" + methodName, 0L, TimeUnit.SECONDS);
        // String recoveryPath = lraClient.joinLRA(lraUrlId, 0L, getBaseUri(), null);
        // System.out.println("Starting LRA: " + lraUrlId + " when joining with baseUri: " + getBaseUri()
        //    + " on enlistment gets recovery path " + recoveryPath);
        
        /*
         * Common rest configuration
         */

        restConfiguration()
                .host(serviceHost)
                .bindingMode(RestBindingMode.json)
                .contextPath("/api")
                .apiContextPath("/doc")
                    .apiProperty("api.title", "API-Gateway  REST API")
                    .apiProperty("api.description", "Operations that can be invoked in the api-gateway")
                    .apiProperty("api.license.name", "Apache License Version 2.0")
                    .apiProperty("api.license.url", "http://www.apache.org/licenses/LICENSE-2.0.html")
                    .apiProperty("api.version", "1.0.0");


        /*
         * Gateway service
         */

        // full path: /api/gateway
        rest().get("/gateway")
                .description("Invoke all microservices in parallel")
                .outTypeList(String.class)
                .apiDocs(true)
                .responseMessage().code(200).message("OK").endResponseMessage()
                .route()
                    .process(e -> {
                    	String methodName = new Object(){}.getClass().getEnclosingMethod().getName();
                        URL lraUrlId = lraClient.startLRA(null, CamelRestEndpoints.class.getName() + "#" + methodName, 0L, TimeUnit.SECONDS);
                        e.getIn().setHeader(LRAClient.LRA_HTTP_HEADER, lraUrlId.toString());
                        e.setProperty(LRAClient.LRA_HTTP_HEADER, lraUrlId);
                    })
                    .multicast(AggregationStrategies.flexible().accumulateInCollection(LinkedList.class))
                    .parallelProcessing()
                        .to("direct:aloha")
                        .to("direct:hola")
                        .to("direct:ola")
                        .to("direct:bonjour")
                    .end()
                    .transform().body(List.class, list -> list)
                    .setHeader("Access-Control-Allow-Credentials", constant("true"))
                    .setHeader("Access-Control-Allow-Origin", header("Origin"))
                    .process(e -> {
                    	String compatedBody = e.getIn().getBody().toString();
                    	URL lraId = (URL) e.getProperty(LRAClient.LRA_HTTP_HEADER);
                    	if(compatedBody.contains("fallback") || compatedBody.contains("failed")) {
                    		lraClient.cancelLRA(lraId);
                    	} else {
                    		lraClient.closeLRA(lraId);
                    	}
                    });

    }
}
