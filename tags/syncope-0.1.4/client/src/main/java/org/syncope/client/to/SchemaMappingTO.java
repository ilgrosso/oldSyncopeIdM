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
package org.syncope.client.to;

import org.syncope.client.AbstractBaseBean;
import org.syncope.types.SchemaType;

public class SchemaMappingTO extends AbstractBaseBean {

    private Long id;

    /**
     * Attribute schema to be mapped.
     * Consider that we can associate tha same attribute schema more
     * than once, with different aliases, to different resource attributes.
     */
    private String schemaName;

    /**
     * Schema type to be mapped.
     * Possible values are:
     * * AccountId for an account id;
     * * Password for a password;
     * * UserSchema for a user schema;
     * * RoleSchema for a role schema;
     * * MembershipSchema for a membership schema.
     */
    private SchemaType schemaType;

    /**
     * Target resource's field to be mapped.
     */
    private String field;

    /**
     * Specify if the mapped target resource's field is the key.
     */
    private boolean accountid;

    /**
     * Specify if the mapped target resource's field is the password.
     */
    private boolean password;

    /**
     * Specify if the mapped target resource's field is nullable.
     */
    private boolean nullable;

    public boolean isAccountid() {
        return accountid;
    }

    public void setAccountid(boolean accountid) {
        this.accountid = accountid;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public boolean isNullable() {
        return nullable;
    }

    public void setNullable(boolean nullable) {
        this.nullable = nullable;
    }

    public boolean isPassword() {
        return password;
    }

    public void setPassword(boolean password) {
        this.password = password;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public SchemaType getSchemaType() {
        return schemaType;
    }

    public void setSchemaType(SchemaType schemaType) {
        this.schemaType = schemaType;
    }
}