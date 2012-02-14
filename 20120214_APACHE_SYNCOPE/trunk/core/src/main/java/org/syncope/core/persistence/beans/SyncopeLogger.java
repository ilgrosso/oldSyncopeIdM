/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.persistence.beans;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import org.syncope.types.LoggerLevel;

@Entity
public class SyncopeLogger extends AbstractBaseBean {

    private static final long serialVersionUID = 943012777014416027L;

    @Id
    @Column(name = "logName")
    private String name;

    @Column(name = "logLevel")
    @Enumerated(EnumType.STRING)
    private LoggerLevel level;

    public LoggerLevel getLevel() {
        return level;
    }

    public void setLevel(final LoggerLevel level) {
        this.level = level;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
