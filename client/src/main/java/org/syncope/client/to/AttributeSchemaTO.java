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

import java.util.HashSet;
import java.util.Set;
import org.syncope.types.AttributeType;

public class AttributeSchemaTO extends AbstractBaseTO {

    private String name;
    private AttributeType type;
    private boolean mandatory;
    private boolean multivalue;
    private String conversionPattern;
    private String validatorClass;
    private Set<String> derivedAttributeSchemas;

    public AttributeSchemaTO() {
        derivedAttributeSchemas = new HashSet<String>();
    }

    public String getConversionPattern() {
        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        this.conversionPattern = conversionPattern;
    }

    public boolean addDerivedAttributeSchema(String derivedAttributeSchema) {
        return derivedAttributeSchemas.add(derivedAttributeSchema);
    }

    public boolean removeDerivedAttributeSchema(String derivedAttributeSchema) {
        return derivedAttributeSchemas.remove(derivedAttributeSchema);
    }

    public Set<String> getDerivedAttributeSchemas() {
        return derivedAttributeSchemas;
    }

    public void setDerivedAttributeSchemas(Set<String> derivedAttributeSchemas) {
        this.derivedAttributeSchemas = derivedAttributeSchemas;
    }

    public boolean isMandatory() {
        return mandatory;
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = mandatory;
    }

    public boolean isMultivalue() {
        return multivalue;
    }

    public void setMultivalue(boolean multivalue) {
        this.multivalue = multivalue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AttributeType getType() {
        return type;
    }

    public void setType(AttributeType type) {
        this.type = type;
    }

    public String getValidatorClass() {
        return validatorClass;
    }

    public void setValidatorClass(String validatorClass) {
        this.validatorClass = validatorClass;
    }
}