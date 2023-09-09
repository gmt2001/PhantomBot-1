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
package com.gmt2001.module.meta;

import com.gmt2001.datastore2.datatype.AttachableJSONObject;
import com.gmt2001.datastore2.record.Record2;
import com.gmt2001.module.Module;

/**
 * A record in {@link SettingsTable} holding some settings
 *
 * @author gmt2001
 */
public final class SettingsRecord extends Record2<SettingsRecord, String, AttachableJSONObject> {
    /**
     * Version of this record implementation
     */
    public static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public SettingsRecord() {
        super(SettingsTable.instance(), () -> SettingsTable.instance().MODULE,
            () -> SettingsTable.instance().SETTINGS);
    }

    /**
     * Constructor
     *
     * @param name the name of the {@link Module} whose settings are stored in this record
     * @param settings the settings object
     */
    public SettingsRecord(String name, AttachableJSONObject settings) {
        this();
        this.module(name);
        this.settings(settings);
        this.resetChangedOnNotNull();
    }

    /**
     * Constructor
     *
     * @param module the {@link Module} whose settings are stored in this record
     * @param settings the settings object
     */
    public SettingsRecord(Module module, AttachableJSONObject settings) {
        this();
        this.module(module);
        this.settings(settings);
        this.resetChangedOnNotNull();
    }

    /**
     * Sets the name of the module whose settings are stored in this record
     *
     * @param name the name of the module
     */
    public void module(String name) {
        this.value1(name);
    }

    /**
     * Sets the name of the module whose settings are stored in this record
     *
     * @param module the module to retrieve the name from
     */
    public void module(Module module) {
        this.value1(module.getClass().getName());
    }

    /**
     * The name of the module whose settings are stored in this record
     *
     * @return the name of the module
     */
    public String module() {
        return this.value1();
    }

    /**
     * Sets the settings object
     *
     * @param settings the settings object
     */
    public void settings(AttachableJSONObject settings) {
        this.value2(settings);
    }

    /**
     * The settings object
     *
     * @return the settings object
     */
    public AttachableJSONObject settings() {
        return this.value2();
    }

    /**
     * Sets the name and settings object of the module whose settings are stored in this record
     *
     * @param module the module to retrieve the name from
     * @param settings the settings object
     * @return {@code this}
     */
    public org.jooq.Record2<String, AttachableJSONObject> values(Module module, AttachableJSONObject settings) {
        this.module(module);
        return this.value2(settings);
    }
}
