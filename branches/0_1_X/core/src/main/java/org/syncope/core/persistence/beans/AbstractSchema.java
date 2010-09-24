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

import java.lang.reflect.Constructor;
import java.text.DecimalFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.persistence.Basic;
import static javax.persistence.EnumType.STRING;

import javax.persistence.Column;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;
import org.syncope.core.persistence.validation.BasicAttributeValidator;
import org.syncope.core.persistence.validation.AbstractAttributeValidator;
import org.syncope.types.SchemaValueType;

@MappedSuperclass
public abstract class AbstractSchema extends AbstractBaseBean {

    @Id
    private String name;
    @Column(nullable = false)
    @Enumerated(STRING)
    private SchemaValueType type;
    /**
     * Specify if the attribute should be stored on the local repository.
     */
    @Basic
    private Character virtual;
    @Basic
    private Character mandatory;
    @Basic
    private Character multivalue;
    @Basic
    private Character uniquevalue;
    @Basic
    private Character readonly;
    @Column(nullable = true)
    private String conversionPattern;
    @Column(nullable = true)
    private String validatorClass;
    @Transient
    private AbstractAttributeValidator validator;

    public AbstractSchema() {
        type = SchemaValueType.String;
        virtual = getBooleanAsCharacter(false);
        mandatory = getBooleanAsCharacter(false);
        multivalue = getBooleanAsCharacter(false);
        uniquevalue = getBooleanAsCharacter(false);
        readonly = getBooleanAsCharacter(false);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public SchemaValueType getType() {
        return type;
    }

    public void setType(SchemaValueType type) {
        this.type = type;
    }

    public boolean isVirtual() {
        return isBooleanAsCharacter(virtual);
    }

    public void setVirtual(boolean virtual) {
        this.virtual = getBooleanAsCharacter(virtual);
    }

    public boolean isMandatory() {
        return isBooleanAsCharacter(mandatory);
    }

    public void setMandatory(boolean mandatory) {
        this.mandatory = getBooleanAsCharacter(mandatory);
    }

    public boolean isMultivalue() {
        return isBooleanAsCharacter(multivalue);
    }

    public void setMultivalue(boolean multivalue) {
        this.multivalue = getBooleanAsCharacter(multivalue);
    }

    public boolean isUniquevalue() {
        return isBooleanAsCharacter(uniquevalue);
    }

    public void setUniquevalue(boolean uniquevalue) {
        this.uniquevalue = getBooleanAsCharacter(uniquevalue);
    }

    public boolean isReadonly() {
        return isBooleanAsCharacter(readonly);
    }

    public void setReadonly(boolean readonly) {
        this.readonly = getBooleanAsCharacter(readonly);
    }

    public AbstractAttributeValidator getValidator() {
        if (validator != null) {
            return validator;
        }

        if (getValidatorClass() != null && getValidatorClass().length() > 0) {
            try {
                Constructor validatorConstructor =
                        Class.forName(getValidatorClass()).getConstructor(
                        new Class[]{getClass().getSuperclass()});
                validator =
                        (AbstractAttributeValidator) validatorConstructor.newInstance(
                        this);
            } catch (Exception e) {
                LOG.error("Could not instantiate validator of type "
                        + getValidatorClass()
                        + ", reverting to AttributeBasicValidator", e);
            }
        }

        if (validator == null) {
            validator = new BasicAttributeValidator(this);
        }

        return validator;
    }

    public String getValidatorClass() {
        return validatorClass;
    }

    public void setValidatorClass(String validatorClass) {
        this.validatorClass = validatorClass;
    }

    public String getConversionPattern() {
        if (!getType().isConversionPatternNeeded() && LOG.isDebugEnabled()) {
            LOG.debug("Conversion pattern is not needed: " + this
                    + "'s type is " + getType());
        }

        return conversionPattern;
    }

    public void setConversionPattern(String conversionPattern) {
        if (!getType().isConversionPatternNeeded()) {
            LOG.warn("Conversion pattern will be ignored: "
                    + "this attribute type is " + getType());
        }

        this.conversionPattern = conversionPattern;
    }

    public <T extends Format> T getFormatter(Class<T> reference) {
        T result = null;

        switch (getType()) {
            case Long:
                DecimalFormat longFormatter =
                        ((DecimalFormat) getType().getBasicFormatter());
                longFormatter.applyPattern(getConversionPattern());

                result = (T) longFormatter;
                break;

            case Double:
                DecimalFormat doubleFormatter =
                        ((DecimalFormat) getType().getBasicFormatter());
                doubleFormatter.applyPattern(getConversionPattern());

                result = (T) doubleFormatter;
                break;

            case Date:
                SimpleDateFormat dateFormatter =
                        (SimpleDateFormat) getType().getBasicFormatter();
                dateFormatter.applyPattern(getConversionPattern());

                result = (T) dateFormatter;
                break;
        }

        return result;
    }

    public abstract <T extends AbstractAttribute> boolean addAttribute(
            T attribute);

    public abstract <T extends AbstractAttribute> boolean removeAttribute(
            T attribute);

    public abstract List<? extends AbstractAttribute> getAttributes();

    public abstract void setAttributes(
            List<? extends AbstractAttribute> attributes);

    public abstract <T extends AbstractDerivedSchema> boolean addDerivedSchema(
            T derivedSchema);

    public abstract <T extends AbstractDerivedSchema> boolean removeDerivedSchema(
            T derivedSchema);

    public abstract List<? extends AbstractDerivedSchema> getDerivedSchemas();

    public abstract void setDerivedSchemas(
            List<? extends AbstractDerivedSchema> derivedSchemas);
}