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
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.SchemaTO;
import org.syncope.core.rest.data.SchemaDataBinder;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.util.AttributableUtil;

@Controller
@RequestMapping("/schema")
public class SchemaController extends AbstractController {

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private SchemaDataBinder schemaDataBinder;

    @PreAuthorize("hasRole('SCHEMA_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/{kind}/create")
    public SchemaTO create(final HttpServletResponse response,
            @RequestBody final SchemaTO schemaTO,
            @PathVariable("kind") final String kind) {

        AbstractSchema schema = getAttributableUtil(kind).newSchema();
        schemaDataBinder.create(schemaTO, schema);
        schema = schemaDAO.save(schema);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return schemaDataBinder.getSchemaTO(schema, getAttributableUtil(kind));
    }

    @PreAuthorize("hasRole('SCHEMA_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/{kind}/delete/{schema}")
    public void delete(@PathVariable("kind") final String kind,
            @PathVariable("schema") final String schemaName)
            throws NotFoundException {

        Class reference = getAttributableUtil(kind).schemaClass();
        AbstractSchema schema = schemaDAO.find(schemaName, reference);
        if (schema == null) {
            LOG.error("Could not find schema '" + schemaName + "'");

            throw new NotFoundException(schemaName);
        }

        schemaDAO.delete(schemaName, getAttributableUtil(kind));
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/list")
    public List<SchemaTO> list(@PathVariable("kind") final String kind) {
        AttributableUtil attributableUtil = getAttributableUtil(kind);
        List<AbstractSchema> schemas = schemaDAO.findAll(
                attributableUtil.schemaClass());

        List<SchemaTO> schemaTOs = new ArrayList<SchemaTO>(schemas.size());
        for (AbstractSchema schema : schemas) {
            schemaTOs.add(schemaDataBinder.getSchemaTO(
                    schema, attributableUtil));
        }

        return schemaTOs;
    }

    @PreAuthorize("hasRole('SCHEMA_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{kind}/read/{schema}")
    public SchemaTO read(@PathVariable("kind") final String kind,
            @PathVariable("schema") final String schemaName)
            throws NotFoundException {

        AttributableUtil attributableUtil = getAttributableUtil(kind);
        AbstractSchema schema = schemaDAO.find(schemaName,
                attributableUtil.schemaClass());
        if (schema == null) {
            LOG.error("Could not find schema '" + schemaName + "'");
            throw new NotFoundException("Schema '" + schemaName + "'");
        }

        return schemaDataBinder.getSchemaTO(schema, attributableUtil);
    }

    @PreAuthorize("hasRole('SCHEMA_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/{kind}/update")
    public SchemaTO update(@RequestBody final SchemaTO schemaTO,
            @PathVariable("kind") final String kind)
            throws NotFoundException {

        AttributableUtil attributableUtil = getAttributableUtil(kind);
        AbstractSchema schema = schemaDAO.find(schemaTO.getName(),
                attributableUtil.schemaClass());
        if (schema == null) {
            LOG.error("Could not find schema '" + schemaTO.getName() + "'");
            throw new NotFoundException("Schema '" + schemaTO.getName() + "'");
        }

        schemaDataBinder.update(schemaTO, schema, attributableUtil);
        schema = schemaDAO.save(schema);

        return schemaDataBinder.getSchemaTO(schema, attributableUtil);
    }
}
