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
package org.syncope.core.persistence.validation.attrvalue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.syncope.core.persistence.beans.AbstractSchema;
import org.syncope.core.persistence.beans.AbstractAttrValue;

public class EmailAddressValidator extends AbstractValidator {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[\\w\\-]([\\.\\w])+[\\w]+@([\\w\\-]+\\.)+[A-Z]{2,4}$",
            Pattern.CASE_INSENSITIVE);

    public EmailAddressValidator(final AbstractSchema schema) {
        super(schema);
    }

    @Override
    protected void doValidate(final AbstractAttrValue attributeValue)
            throws InvalidAttrValueException {

        Matcher matcher = EMAIL_PATTERN.matcher(
                (CharSequence) attributeValue.getValue());
        if (!matcher.matches()) {
            throw new InvalidAttrValueException(attributeValue);
        }
    }
}
