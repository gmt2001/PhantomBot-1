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

import java.util.Map;

import org.jooq.UpdatableRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Class to allow a {@link JSONObject} to be used in {@link JSONObjectConverter}
 *
 * @author gmt2001
 */
public final class AttachableJSONObject extends JSONObject implements AttachableDataType {
    /**
     * The linked record
     */
    private UpdatableRecord<?> record;
    /**
     * The field index
     */
    private int fieldIndex = -1;

    /**
     * Construct an empty JSONObject
     */
    public AttachableJSONObject() {
        super();
    }

    /**
     * Construct a JSONObject from a source JSON text string
     *
     * @param source a string beginning with {@code &#123;} (left brace) and ending with {@code &#125;} (right brace)
     * @throws JSONException if there is a syntax error in the source string or a duplicated key
     */
    public AttachableJSONObject(String source) throws JSONException {
        super(source);
    }

    /**
     * Construct a JSONObject from another JSONObject
     *
     * @param jo a JSONObject
     */
    public AttachableJSONObject(JSONObject jo) {
        super(jo, jo.keySet().toArray(new String[0]));
    }

    /**
     * Construct a JSONObject from a subset of another JSONObject. An array of strings is used to identify the keys that should be copied. Missing keys are ignored
     *
     * @param jo a JSONObject
     * @param names an array of strings
     */
    public AttachableJSONObject(JSONObject jo, String... names) {
        super(jo, names);
    }

    /**
     * Construct a JSONObject from a Map
     *
     * @param m a Map that contains values to initialize the JSONObject with
     * @throws JSONException if a value in the map is a non-finite number
     * @throws NullPointerException if the map contains {@code null} as a key
     */
    public AttachableJSONObject(Map<?, ?> m) {
        super(m);
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
    public Object opt(String key) {
        Object o = super.opt(key);

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
    public JSONObject put(String key, Object value) throws JSONException {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        return super.put(key, value);
    }

    @Override
    public Object remove(String key) {
        if (this.fieldIndex >= 0) {
            this.record.changed(this.fieldIndex, true);
        }

        return super.remove(key);
    }

}
