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

import java.util.Collection;

import org.jooq.UpdatableRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to allow a {@link JSONArray} to be used in {@link JSONArrayConverter}
 *
 * @author gmt2001
 */
public final class AttachableJSONArray extends JSONArray implements AttachableDataType {
    /**
     * The linked record
     */
    private UpdatableRecord<?> record;
    /**
     * The field index
     */
    private int fieldIndex = -1;

    /**
     * Construct an empty JSONArray
     */
    public AttachableJSONArray() {
        super();
    }

    /**
     * Construct a JSONArray from a source JSON text string
     *
     * @param source a string beginning with {@code [} (left bracket) and ending with {@code ]} (right bracket)
     * @throws JSONException if there is a syntax error in the source string
     */
    public AttachableJSONArray(String source) throws JSONException {
        super(source);
    }

    /**
     * Construct a JSONArray from another JSONArray. This is a shallow copy
     *
     * @param array an array
     */
    public AttachableJSONArray(JSONArray array) {
        super(array);
    }

    /**
     * Construct a JSONArray from a Collection
     *
     * @param collection a collection
     */
    public AttachableJSONArray(Collection<?> collection) {
        super(collection);
    }

    @Override
    public void attach(UpdatableRecord<?> record, int fieldIndex) {
        this.record = record;
        this.fieldIndex = fieldIndex;
    }

    @Override
    public void clear() {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        super.clear();
    }

    @Override
    public Object opt(int index) {
        Object o = super.opt(index);

        if (o != null && o instanceof JSONObject jso) {
            AttachableJSONObject ajso = new AttachableJSONObject(jso);
            ajso.attach(this.record, this.fieldIndex);
            o = ajso;
        } else if (o != null && o instanceof JSONArray jsa) {
            AttachableJSONArray ajsa = new AttachableJSONArray(jsa);
            ajsa.attach(this.record, this.fieldIndex);
            o = ajsa;
        }

        return o;
    }

    @Override
    public JSONArray put(Object value) {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        return super.put(value);
    }

    @Override
    public JSONArray put(int index, Object value) throws JSONException {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        return super.put(index, value);
    }

    @Override
    public JSONArray putAll(JSONArray array) {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        return super.putAll(array);
    }

    @Override
    public JSONArray putAll(Collection<?> collection) {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        return super.putAll(collection);
    }

    @Override
    public JSONArray putAll(Iterable<?> iter) {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        return super.putAll(iter);
    }

    @Override
    public JSONArray putAll(Object array) throws JSONException {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        return super.putAll(array);
    }
}
