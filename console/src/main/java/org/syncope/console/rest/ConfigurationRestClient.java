/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.syncope.console.rest;

import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConfigurationTO;
import org.syncope.client.to.LoggerTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.types.LoggerLevel;

@Component
public class ConfigurationRestClient extends AbstractBaseRestClient {

    /**
     * Get all stored configurations.
     *
     * @return ConfigurationTOs
     */
    public List<ConfigurationTO> getAllConfigurations() {
        return Arrays.asList(
                restTemplate.getForObject(baseURL
                + "configuration/list.json", ConfigurationTO[].class));
    }

    public ConfigurationTO readConfiguration(String key) {

        return restTemplate.getForObject(
                baseURL + "configuration/read/{key}.json",
                ConfigurationTO.class, key);
    }

    /**
     * Create a new configuration.
     *
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean createConfiguration(ConfigurationTO configurationTO) {

        ConfigurationTO newConfigurationTO =
                restTemplate.postForObject(baseURL
                + "configuration/create",
                configurationTO, ConfigurationTO.class);

        return configurationTO.equals(newConfigurationTO);
    }

    /**
     * Update an existent configuration.
     *
     * @param configurationTO
     * @return true if the operation ends succesfully, false otherwise
     */
    public boolean updateConfiguration(ConfigurationTO configurationTO) {
        ConfigurationTO newConfigurationTO = null;

        try {
            newConfigurationTO = restTemplate.postForObject(baseURL
                    + "configuration/update", configurationTO,
                    ConfigurationTO.class);
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While updating a configuration", e);
            return false;
        }
        return configurationTO.equals(newConfigurationTO);
    }

    /**
     * Deelete a configuration by key.
     */
    public void deleteConfiguration(final String key) {
        restTemplate.delete(baseURL
                + "configuration/delete/{key}.json", key);
    }

    /**
     * Get all loggers.
     *
     * @return LoggerTOs
     */
    public List<LoggerTO> getLoggers() {
        return Arrays.asList(restTemplate.getForObject(
                baseURL + "logger/list", LoggerTO[].class));
    }

    public boolean setLoggerLevel(final String name, final LoggerLevel level) {
        boolean result;
        try {
            restTemplate.postForObject(
                    baseURL + "logger/set/{name}/{level}",
                    null, LoggerTO.class, name, level);
            result = true;
        } catch (SyncopeClientCompositeErrorException e) {
            LOG.error("While setting a logger's level", e);
            result = false;
        }

        return result;
    }
}
