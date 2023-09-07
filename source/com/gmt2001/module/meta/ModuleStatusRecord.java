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

import com.gmt2001.datastore2.record.Record2;
import com.gmt2001.module.Module;

/**
 * A record in {@link ModuleStatusTable} denoting the enabled status of a {@link Module}
 * <p>
 * If a record for the module is not present, then the status of the module is {@link Module#defaultEnabledState()}
 *
 * @author gmt2001
 */
public final class ModuleStatusRecord extends Record2<ModuleStatusRecord, String, Boolean> {
    /**
     * Version of this record implementation
     */
    public static final long serialVersionUID = 1L;

    /**
     * Constructor
     */
    public ModuleStatusRecord() {
        super(ModuleStatusTable.instance(), () -> ModuleStatusTable.instance().MODULE,
            () -> ModuleStatusTable.instance().ENABLED);
    }

    /**
     * Constructor
     *
     * @param name the name of the {@link Module} whose status is stored in this record
     * @param enabled the enabled state of the module
     */
    public ModuleStatusRecord(String name, Boolean enabled) {
        this();
        this.module(name);
        this.enabled(enabled);
        this.resetChangedOnNotNull();
    }

    /**
     * Constructor
     *
     * @param module the {@link Module} whose enabled state is stored in this record
     * @param enabled the enabled state of the module
     */
    public ModuleStatusRecord(Module module, Boolean enabled) {
        this();
        this.module(module);
        this.enabled(enabled);
        this.resetChangedOnNotNull();
    }

    /**
     * Sets the name of the module whose enabled state is stored in this record
     *
     * @param name the name of the module
     */
    public void module(String name) {
        this.value1(name);
    }

    /**
     * Sets the name of the module whose enabled state is stored in this record
     *
     * @param module the module to retrieve the name from
     */
    public void module(Module module) {
        this.value1(module.getClass().getName());
    }

    /**
     * The name of the module whose enabled state is stored in this record
     *
     * @return the name of the module
     */
    public String module() {
        return this.value1();
    }

    /**
     * Sets the enabled state of the module
     *
     * @param enabled the enabled state of the module
     */
    public void enabled(Boolean enabled) {
        this.value2(enabled);
    }

    /**
     * The enabled state of the module
     *
     * @return the enabled state
     */
    public Boolean enabled() {
        return this.value2();
    }

    /**
     * Sets the name and enabled state of the module whose version is stored in this record
     *
     * @param module the module to retrieve the name from
     * @param enabled the enabled state of the module
     * @return {@code this}
     */
    public org.jooq.Record2<String, Boolean> values(Module module, Boolean enabled) {
        this.module(module);
        return this.value2(enabled);
    }
}
