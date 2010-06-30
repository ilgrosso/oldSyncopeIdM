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
package org.syncope.core.persistence.validation;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractAttributeValue;

public abstract class AttributeValidator {

    final protected AbstractSchema schema;
    final protected Class attributeClass;

    public AttributeValidator(AbstractSchema schema)
            throws ClassNotFoundException {

        this.schema = schema;
        this.attributeClass = Class.forName(schema.getType().getClassName());
    }

    public <T extends AbstractAttributeValue> T getValue(Object value,
            T attributeValue) throws ValidationException {

        attributeValue = value instanceof String
                ? parseValue((String) value, attributeValue)
                : parseValue(value, attributeValue);
        doValidate(attributeValue);

        return attributeValue;
    }

    protected <T extends AbstractAttributeValue> T parseValue(String value,
            T attributeValue) throws ParseException {

        Exception exception = null;

        switch (schema.getType()) {

            case String:
                attributeValue.setStringValue(value);
                break;

            case Boolean:
                attributeValue.setBooleanValue(Boolean.parseBoolean(value));
                break;

            case Long:
                try {
                    attributeValue.setLongValue(Long.valueOf(schema.getFormatter(
                            DecimalFormat.class).parse(value).longValue()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Double:
                try {
                    attributeValue.setDoubleValue(Double.valueOf(schema.getFormatter(
                            DecimalFormat.class).parse(value).doubleValue()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;

            case Date:
                try {
                    attributeValue.setDateValue(new Date(schema.getFormatter(
                            SimpleDateFormat.class).parse(value).getTime()));
                } catch (java.text.ParseException pe) {
                    exception = pe;
                }
                break;
        }

        if (exception != null) {
            throw new ParseException(
                    "While trying to parse '" + value + "'", exception);
        }

        return attributeValue;
    }

    protected <T extends AbstractAttributeValue> T parseValue(Object value,
            T attributeValue) throws ParseException {

        switch (schema.getType()) {

            case String:
                attributeValue.setStringValue((String) value);
                break;

            case Boolean:
                attributeValue.setBooleanValue((Boolean) value);
                break;

            case Long:
                attributeValue.setLongValue((Long) value);
                break;

            case Double:
                attributeValue.setDoubleValue((Double) value);
                break;

            case Date:
                attributeValue.setDateValue((Date) value);
                break;
        }

        return attributeValue;
    }

    protected abstract <T extends AbstractAttributeValue> void doValidate(
            T attributeValue) throws ValidationFailedException;
}
