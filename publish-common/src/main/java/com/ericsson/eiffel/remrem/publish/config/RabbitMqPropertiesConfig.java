/*
    Copyright 2018 Ericsson AB.
    For a full list of individual contributors, please see the commit history.
    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.ericsson.eiffel.remrem.publish.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import com.ericsson.eiffel.remrem.publish.helper.RabbitMqProperties;
import com.ericsson.eiffel.remrem.publish.service.GenerateURLTemplate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ch.qos.logback.classic.Logger;

@Component
public class RabbitMqPropertiesConfig {

    Logger log = (Logger) LoggerFactory.getLogger(RabbitMqPropertiesConfig.class);

    @Autowired
    Environment env;

    @Value("${rabbitmq.instances.jsonlist:{null}}")
    private String rabbitmqInstancesJsonListContent;

    @Value("${jasypt.encryptor.password:{null}}")
    private String jasyptPassword;

    private Map<String, RabbitMqProperties> rabbitMqPropertiesMap = new HashMap<String, RabbitMqProperties>();

    @Autowired
    private GenerateURLTemplate generateURLTemplate;
    private static final String GENERATE_SERVER_URI = "generate.server.uri";
    private static final String GENERATE_SERVER_PATH = "generate.server.contextpath";

    /***
     * This method is used to give RabbitMq properties based on protocol
     * 
     * @return protocol specific RabbitMq properties in map
     */
    public Map<String, RabbitMqProperties> getRabbitMqProperties() {
        Map<String, Object> map = new HashMap<String, Object>();
        readSpringProperties();

        loadGenerateConfigurationBasedOnSystemProperties();
        return rabbitMqPropertiesMap;
    }

    /***
     * Reads Spring Properties and writes RabbitMq properties to RabbitMq
     * instances properties map object.
     */
    private void readSpringProperties() {
        JsonNode rabbitmqInstancesJsonListJsonArray = null;
        final ObjectMapper objMapper = new ObjectMapper();
        try {
            rabbitmqInstancesJsonListJsonArray = objMapper.readTree(rabbitmqInstancesJsonListContent);

            for (int i = 0; i < rabbitmqInstancesJsonListJsonArray.size(); i++) {
                JsonNode rabbitmqInstanceObject = rabbitmqInstancesJsonListJsonArray.get(i);
                String protocol = rabbitmqInstanceObject.get("mp").asText();
                log.info("Configuring RabbitMq instance for Eiffel message protocol: " + protocol);

                RabbitMqProperties rabbitMqProperties = new RabbitMqProperties();
                rabbitMqProperties.setHost(rabbitmqInstanceObject.get("host").asText());
                rabbitMqProperties.setPort(Integer.parseInt(rabbitmqInstanceObject.get("port").asText()));
                rabbitMqProperties.setUsername(rabbitmqInstanceObject.get("username").asText());
                rabbitMqProperties.setPassword(DecryptionUtils.decryptString(rabbitmqInstanceObject.get("password").asText(), jasyptPassword));
                rabbitMqProperties.setTlsVer(rabbitmqInstanceObject.get("tls").asText());
                rabbitMqProperties.setExchangeName(rabbitmqInstanceObject.get("exchangeName").asText());
                rabbitMqProperties.setCreateExchangeIfNotExisting(rabbitmqInstanceObject.get("createExchangeIfNotExisting").asBoolean());
                rabbitMqProperties.setDomainId(rabbitmqInstanceObject.get("domainId").asText());

                rabbitMqPropertiesMap.put(protocol, rabbitMqProperties);
            }
        } catch (Exception e) {
            log.error("Failure when initiating RabbitMq Java Spring properties: " + e.getMessage(), e);
        }
    }

    /***
     * This method is used to load generate server configuration from JAVA_OPTS.
     */
    private void loadGenerateConfigurationBasedOnSystemProperties() {
        if (StringUtils.isBlank(generateURLTemplate.getGenerateServerUri())) {
            generateURLTemplate.setGenerateServerUri(System.getProperty(GENERATE_SERVER_URI));
        }

        if (StringUtils.isBlank(generateURLTemplate.getGenerateServerContextPath())) {
            generateURLTemplate.setGenerateServerPath(System.getProperty(GENERATE_SERVER_PATH));
        }
    }
}
