/*
 * Copyright (C) 2016-2023 phantombot.github.io/PhantomBot
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.gmt2001.datastore2.datatype;

import org.jooq.Converter;
import org.jooq.DataType;

import com.gmt2001.datastore2.Datastore2;

/**
 * Provides a {@link Converter} and {@link DataType} for storing an {@link AttachableJSONArray} in the database
 *
 * @author gmt2001
 */
public final class JSONArrayConverter implements Converter<String, AttachableJSONArray> {
    /**
     * A data type storing an {@link AttachableJSONArray}
     */
    public static final DataType<AttachableJSONArray> JSONARRAY = Datastore2.instance().longTextDataType().asConvertedDataType(new JSONArrayConverter());

    /**
     * Constructor
     */
    private JSONArrayConverter(){
    }

    @Override
    public AttachableJSONArray from(String databaseObject) {
        return new AttachableJSONArray(databaseObject);
    }

    @Override
    public String to(AttachableJSONArray userObject) {
        return userObject.toString();
    }

    @Override
    public Class<String> fromType() {
        return String.class;
    }

    @Override
    public Class<AttachableJSONArray> toType() {
        return AttachableJSONArray.class;
    }
}
