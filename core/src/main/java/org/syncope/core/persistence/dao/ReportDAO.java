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
package org.syncope.core.persistence.dao;

import java.util.List;
import org.syncope.core.persistence.beans.Report;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;

public interface ReportDAO extends DAO {

    Report find(Long id);

    List<Report> findAll();

    List<Report> findAll(int page, int itemsPerPage);

    int count();

    Report save(Report report)
            throws InvalidEntityException;

    void delete(Long id);

    void delete(Report report);
}
