/*
 * Copyright (C) 2016-2024 phantombot.github.io/PhantomBot
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

/**
 * commandRegister.js
 *
 * Register and keep track of commands.
 *
 * NOTE: You will have to register ANY command you implement!
 * The commandEvent will not get fired to your module if the registry does not know about it!
 */
(function() {
    var commands = {},
        aliases = {},
        _aliasesLock = new Packages.java.util.concurrent.locks.ReentrantLock(),
        _commandsLock = new Packages.java.util.concurrent.locks.ReentrantLock(),
        disablecomBlocked = ['disablecom', 'enablecom', 'pausecommands', 'setcommandrestriction'];

    const RESTRICTION = {
        NONE: -1,
        ONLINE: 1,
        OFFLINE: 2
    }

    /*
     * @function registerChatCommand
     *
     * @param {String} script
     * @param {String} command
     * @param {Number} groupId
     * @param {RESTRICTION} restriction
     */
    function registerChatCommand(script, command, groupId, restriction) {
        command = $.jsString(command).toLowerCase();
        // If groupId is undefined set it to 7 (viewer).
        groupId = (groupId === undefined ? $.PERMISSION.Viewer : groupId);
        restriction = (restriction === undefined ? RESTRICTION.NONE : restriction);
        restriction = (restriction === RESTRICTION.NONE || restriction === RESTRICTION.ONLINE || restriction === RESTRICTION.OFFLINE ? restriction : RESTRICTION.NONE)

        if (commandExists(command)) {
            return;
        }

        // This is for the panel commands.
        if (groupId === $.PERMISSION.Panel) {
            $.inidb.del('permcom', command);
            $.inidb.del('commandRestrictions', command);

            _commandsLock.lock();
            try {
                commands[command] = {
                    groupId: groupId,
                    script: script,
                    restriction: restriction,
                    subcommands: {}
                };
            } finally {
                _commandsLock.unlock();
            }

            return;
        }

        // Handle disabled commands.
        if (!disablecomBlocked.includes(command) && $.inidb.exists('disabledCommands', command)) {
            $.inidb.set('tempDisabledCommandScript', command, script);
            return;
        }

        $.inidb.del('tempDisabledCommandScript', command);

        // Get and set the command permission.
        groupId = $.getSetIniDbNumber('permcom', command, groupId);

        if (!disablecomBlocked.includes(command)) {
            // Get command restriction
            restriction = $.getIniDbNumber('commandRestrictions', command, restriction);
        }

        _commandsLock.lock();
        try {
            commands[command] = {
                groupId: groupId,
                script: script,
                restriction: restriction,
                subcommands: {}
            };
        } finally {
            _commandsLock.unlock();
        }
    }

    /*
     * @function registerChatSubcommand
     *
     * @param {String} command
     * @param {String} subcommand
     * @param {Number} groupId
     * @param {RESTRICTION} restriction
     */
    function registerChatSubcommand(command, subcommand, groupId, restriction) {
        command = $.jsString(command).toLowerCase();
        subcommand = $.jsString(subcommand).toLowerCase();
        // If groupId is undefined set it to 7 (viewer).
        groupId = (groupId === undefined ? $.PERMISSION.Viewer : groupId);
        restriction = (restriction === undefined ? RESTRICTION.NONE : restriction);
        restriction = (restriction === RESTRICTION.NONE || restriction === RESTRICTION.ONLINE || restriction === RESTRICTION.OFFLINE ? restriction : RESTRICTION.NONE)

        if (!commandExists(command) || subCommandExists(command, subcommand)) {
            return;
        }

        // Get and set the command permission.
        groupId = $.getSetIniDbNumber('permcom', (command + ' ' + subcommand), groupId);

        // Get command restriction
        restriction = $.getIniDbNumber('commandRestrictions', (command + ' ' + subcommand), restriction);

        _commandsLock.lock();
        try {
            commands[command].subcommands[subcommand] = {
                groupId: groupId,
                restriction: restriction
            };
        } finally {
            _commandsLock.unlock();
        }
    }

    /*
     * @function registerChatAlias
     *
     * @param {String} alias
     */
    function registerChatAlias(alias, target, script) {
        let full = target !== undefined && target !== null && script !== undefined && script !== null;
        alias = $.jsString(alias).toLowerCase();
        _aliasesLock.lock();
        try {
            if (full) {
                if ($.commandExists(alias) || !$.commandExists(target.split(' ')[0]) || $.inidb.exists('aliases', alias)) {
                    return;
                }
                $.registerChatCommand(script, alias);
                $.inidb.set('aliases', alias, target);
            }

            if (!aliasExists(alias)) {
                aliases[alias] = true;
            }
        } finally {
            _aliasesLock.unlock();
        }
    }

    /*
     * @function unregisterChatAlias
     *
     * @param {String} alias
     */
    function unregisterChatAlias(alias) {
        alias = $.jsString(alias).toLowerCase();
        _aliasesLock.lock();
        try {
            $.unregisterChatCommand(alias);
            $.inidb.del('aliases', alias);
            delete aliases[alias];
        } finally {
            _aliasesLock.unlock();
        }
    }

    /*
     * @function unregisterChatCommand
     *
     * @param {String} command
     */
    function unregisterChatCommand(command) {
        command = $.jsString(command).toLowerCase();
        if (commandExists(command)) {
            _commandsLock.lock();
            try {
                delete commands[command];
            } finally {
                _commandsLock.unlock();
            }

            _aliasesLock.lock();
            try {
                delete aliases[command];
            } finally {
                _aliasesLock.unlock();
            }
        }

        $.inidb.del('permcom', command);
        $.inidb.del('pricecom', command);
        $.inidb.del('cooldown', command);
        $.inidb.del('paycom', command);
        $.inidb.del('disabledCommands', command);
        $.inidb.del('commandRestrictions', command);
    }

    /*
     * @function tempUnRegisterChatCommand
     *
     * @param {String} command
     */
    function tempUnRegisterChatCommand(command) {
        command = $.jsString(command).toLowerCase();
        _commandsLock.lock();
        try {
            $.inidb.set('tempDisabledCommandScript', command, commands[command].script);
        } finally {
            _commandsLock.unlock();
        }

        if (commandExists(command)) {
            _commandsLock.lock();
            try {
                delete commands[command];
            } finally {
                _commandsLock.unlock();
            }

        } else if (aliasExists(command)) {
            _aliasesLock.lock();
            try {
                delete aliases[command];
            } finally {
                _aliasesLock.unlock();
            }
        }
    }

    /*
     * @function unregisterChatSubcommand
     *
     * @param {String} command
     * @param {String} subcommand
     */
    function unregisterChatSubcommand(command, subcommand) {
        command = $.jsString(command).toLowerCase();
        subcommand = $.jsString(subcommand).toLowerCase();

        _commandsLock.lock();
        try {
            if (subCommandExists(command, subcommand)) {
                delete commands[command].subcommands[subcommand];
            }
        } finally {
            _commandsLock.unlock();
        }

        $.inidb.del('permcom', command + ' ' + subcommand);
        $.inidb.del('pricecom', command + ' ' + subcommand);
        $.inidb.del('commandRestrictions', command + ' ' + subcommand);
    }

    /*
     * @function getCommandScript
     *
     * @param  {String} command
     * @return {String}
     */
    function getCommandScript(command) {
        command = $.jsString(command).toLowerCase();
        _commandsLock.lock();
        try {
            if (commands[command] === undefined) {
                return 'Undefined';
            }

            return commands[command].script;
        } finally {
            _commandsLock.unlock();
        }
    }

    /*
     * @function commandExists
     *
     * @param  {String} command
     * @return {Boolean}
     */
    function commandExists(command) {
        command = $.jsString(command).toLowerCase();
        _commandsLock.lock();
        try {
            return (commands[command] !== undefined);
        } finally {
            _commandsLock.unlock();
        }
    }

    /**
     * @function commandRestrictionMet
     * @param {String} command
     * @param {String} subCommand
     * @return {Boolean} true if the command and or subcommand can be run; false otherwise
     */
    function commandRestrictionMet(command, subCommand) {
        command = $.jsString(command).toLowerCase();
        subCommand = subCommand !== null && subCommand !== undefined ? $.jsString(subCommand).toLowerCase() : null;
        if (!commandExists(command)) {
            return false;
        }

        let restriction = null;

        _commandsLock.lock();
        try {
            restriction = commands[command].restriction;
        } finally {
            _commandsLock.unlock();
        }

        if (subCommand !== null && subCommand !== '') {
            if (!subCommandExists(command, subCommand)) {
                return false;
            }

            _commandsLock.lock();
            try {
                let subRestriction = commands[command].subcommands[subCommand].restriction;
                restriction = subRestriction !== null && subRestriction !== undefined ? subRestriction : restriction;
            } finally {
                _commandsLock.unlock();
            }
        }

        if (restriction === undefined || restriction === null) {
            return false; // Restriction is always set to at least RESTRICTION.NONE
        }

        switch (restriction) {
            case RESTRICTION.NONE:
                return true;
            case RESTRICTION.ONLINE:
                return $.isOnline($.channelName);
            case RESTRICTION.OFFLINE:
                return !$.isOnline($.channelName);
            default:
                return false;
        }
    }

    /**
     * @function setCommandRestriction
     * @param {String} command
     * @param {String} subCommand
     * @param {RESTRICTION} restriction
     */
    function setCommandRestriction(command, subCommand, restriction) {
        command = $.jsString(command).toLowerCase();
        subCommand = subCommand !== null && subCommand !== undefined ? $.jsString(subCommand).toLowerCase() : null;

        switch (restriction) {
            case RESTRICTION.NONE:
            case RESTRICTION.ONLINE:
            case RESTRICTION.OFFLINE:
                break;
            default:
                return;
        }

        if (subCommand === null) {
            _commandsLock.lock();
            try {
                if (commandExists(command)) {
                    commands[command].restriction = restriction;
                    $.setIniDbNumber('commandRestrictions', command, restriction);
                }
            } finally {
                _commandsLock.unlock();
            }
        } else {
            _commandsLock.lock();
            try {
                if (subCommandExists(command, subCommand)) {
                    commands[command].subcommands[subCommand].restriction = restriction;
                    $.setIniDbNumber('commandRestrictions', (command + ' ' + subCommand), restriction);
                }
            } finally {
                _commandsLock.unlock();
            }
        }
    }

    /**
     * @function getCommandRestrictionByName
     * @param {String} restrictionName
     * @returns {RESTRICTION} if a restriction with the provided name exists; null otherwise
     */
    function getCommandRestrictionByName(restrictionName) {
        if (restrictionName !== undefined && restrictionName !== null) {
            restrictionName = $.jsString(restrictionName).toLowerCase();
            if ($.equalsIgnoreCase(restrictionName, 'none')) {
                return RESTRICTION.NONE;
            }
            if ($.equalsIgnoreCase(restrictionName, 'online')) {
                return RESTRICTION.ONLINE;
            }
            if ($.equalsIgnoreCase(restrictionName, 'offline')) {
                return RESTRICTION.OFFLINE;
            }
        }

        return null;
    }

    /*
     * @function aliasExists
     *
     * @param {String} command
     */
    function aliasExists(alias) {
        alias = $.jsString(alias).toLowerCase();
        _aliasesLock.lock();
        try {
            return (aliases[alias] !== undefined);
        } finally {
            _aliasesLock.unlock();
        }
    }

    /*
     * @function subCommandExists
     *
     * @param  {String} command
     * @param  {String} subcommand
     * @return {Boolean}
     */
    function subCommandExists(command, subcommand) {
        command = $.jsString(command).toLowerCase();
        subcommand = $.jsString(subcommand).toLowerCase();
        _commandsLock.lock();
        try {
            if (commandExists(command)) {
                return (commands[command].subcommands[subcommand] !== undefined);
            }
        } finally {
            _commandsLock.unlock();
        }

        return false;
    }

    /*
     * @function getCommandGroup
     *
     * @param  {String} command
     * @return {Number}
     */
    function getCommandGroup(command) {
        command = $.jsString(command).toLowerCase();
        var groupid = $.PERMISSION.Viewer;

        _commandsLock.lock();
        try {
            if (commandExists(command)) {
                groupid = commands[command].groupId;
            }
        } finally {
            _commandsLock.unlock();
        }

        return groupid;
    }

    /*
     * @function getCommandGroupName
     *
     * @param  {String} command
     * @return {String}
     */
    function getCommandGroupName(command) {
        command = $.jsString(command).toLowerCase();
        var group = '';
        _commandsLock.lock();
        try {
            if (commandExists(command)) {
                if (commands[command].groupId === $.PERMISSION.Caster) {
                    group = 'Caster';
                } else if (commands[command].groupId === $.PERMISSION.Admin) {
                    group = 'Administrator';
                } else if (commands[command].groupId === $.PERMISSION.Mod) {
                    group = 'Moderator';
                } else if (commands[command].groupId === $.PERMISSION.Sub) {
                    group = 'Subscriber';
                } else if (commands[command].groupId === $.PERMISSION.Donator) {
                    group = 'Donator';
                } else if (commands[command].groupId === $.PERMISSION.VIP) {
                    group = 'VIP';
                } else if (commands[command].groupId === $.PERMISSION.Regular) {
                    group = 'Regular';
                } else if (commands[command].groupId === $.PERMISSION.Viewer) {
                    group = 'Viewer';
                }

                return group;
            }
        } finally {
            _commandsLock.unlock();
        }
        return 'Viewer';
    }

    /*
     * @function getSubcommandGroup
     *
     * @param  {String} command
     * @param  {String} subcommand
     * @return {Number}
     */
    function getSubcommandGroup(command, subcommand) {
        command = $.jsString(command).toLowerCase();
        subcommand = $.jsString(subcommand).toLowerCase();
        _commandsLock.lock();
        try {
            if (commandExists(command)) {
                if (subCommandExists(command, subcommand)) {
                    return commands[command].subcommands[subcommand].groupId;
                }
                return getCommandGroup(command);
            }
        } finally {
            _commandsLock.unlock();
        }

        return $.PERMISSION.Viewer;
    }

    /*
     * @function getSubCommandGroupName
     *
     * @param  {String} command
     * @param  {String} subcommand
     * @return {String}
     */
    function getSubCommandGroupName(command, subcommand) {
        command = $.jsString(command).toLowerCase();
        subcommand = $.jsString(subcommand).toLowerCase();
        var group = '';

        _commandsLock.lock();
        try {
            if (subCommandExists(command, subcommand)) {
                if (commands[command].subcommands[subcommand].groupId === $.PERMISSION.Caster) {
                    group = 'Caster';
                } else if (commands[command].subcommands[subcommand].groupId === $.PERMISSION.Admin) {
                    group = 'Administrator';
                } else if (commands[command].subcommands[subcommand].groupId === $.PERMISSION.Mod) {
                    group = 'Moderator';
                } else if (commands[command].subcommands[subcommand].groupId === $.PERMISSION.Sub) {
                    group = 'Subscriber';
                } else if (commands[command].subcommands[subcommand].groupId === $.PERMISSION.Donator) {
                    group = 'Donator';
                } else if (commands[command].subcommands[subcommand].groupId === $.PERMISSION.VIP) {
                    group = 'VIP';
                } else if (commands[command].subcommands[subcommand].groupId === $.PERMISSION.Regular) {
                    group = 'Regular';
                } else if (commands[command].subcommands[subcommand].groupId === $.PERMISSION.Viewer) {
                    group = 'Viewer';
                }
                return group;
            }
        } finally {
            _commandsLock.unlock();
        }

        return 'Viewer';
    }

    /*
     * @function updateCommandGroup
     *
     * @param {String} command
     * @param {Number} groupId
     */
    function updateCommandGroup(command, groupId) {
        command = $.jsString(command).toLowerCase();
        _commandsLock.lock();
        try {
            if (commandExists(command)) {
                commands[command].groupId = groupId;
            }
        } finally {
            _commandsLock.unlock();
        }
    }

    /*
     * @function updateSubcommandGroup
     *
     * @param {String} command
     * @param {String} subcommand
     * @param {Number} groupId
     */
    function updateSubcommandGroup(command, subcommand, groupId) {
        command = $.jsString(command).toLowerCase();
        subcommand = $.jsString(subcommand).toLowerCase();
        _commandsLock.lock();
        try {
            if (subCommandExists(command, subcommand)) {
                commands[command].subcommands[subcommand].groupId = groupId;
            }
        } finally {
            _commandsLock.unlock();
        }
    }

    /*
     * @function getSubCommandFromArguments
     *
     * @param {String}   command
     * @param {String[]} args
     */
    function getSubCommandFromArguments(command, args) {
        if (command === undefined || command === null) {
            command = '';
        }
        command = $.jsString(command).toLowerCase();
        if (!commandExists(command) || args === undefined || args === null || args[0] === undefined || args[0] === null) {
            return '';
        } else {
            var subCommand = args[0].toLowerCase();

            if (subCommandExists(command, subCommand)) {
                return subCommand;
            }
            return '';
        }
    }

    /**
     * Lists all registered commands
     *
     * @returns {Array} A list of command names
     */
    function listCommands() {
        let commandlist = [];

        for (let x in commands) {
            commandlist.push(x);
        }

        return commandlist;
    }

    /**
     * Lists all registered sub-commands of the command
     *
     * @param {String} command The command to check
     * @returns {Array} A list of sub-command names
     */
    function listSubCommands(command) {
        let commandlist = [];

        if (commandExists(command)) {
            for (let x in commands[command].subcommands) {
                commandlist.push(x);
            }
        }

        return commandlist;
    }

    function getDisablecomBlocked() {
        return disablecomBlocked;
    }

    /*
     * @function priceCom
     *
     * @export $
     * @param {string} username
     * @param {string} command
     * @param {sub} subcommand
     * @param {bool} isMod
     * @returns 1 | 0 - Not a boolean
     */
    function priceCom(username, command, subCommand, isMod) {
        if (!disablecomBlocked.includes(command)) {
            if ((subCommand !== '' && $.inidb.exists('pricecom', command + ' ' + subCommand)) || $.inidb.exists('pricecom', command)) {
                if ((((isMod && $.getIniDbBoolean('settings', 'pricecomMods', false) && !$.isBot(username)) || !isMod)) && $.bot.isModuleEnabled('./systems/pointSystem.js')) {
                    if ($.getUserPoints(username) < getCommandPrice(command, subCommand, '')) {
                        return 1;
                    }
                    return 0;
                }
            }
        }
        return -1;
    }

    /*
     * @function getCommandPrice
     *
     * @export $
     * @param {string} command
     * @param {string} subCommand
     * @param {string} subCommandAction
     * @returns {Number}
     */
    function getCommandPrice(command, subCommand, subCommandAction) {
        command = command.toLowerCase();
        subCommand = subCommand.toLowerCase();
        subCommandAction = subCommandAction.toLowerCase();

        let cost = $.optIniDbNumber('pricecom', command + ' ' + subCommand + ' ' + subCommandAction);
        if (cost.isPresent()) {
            return cost.get();
        }

        cost = $.optIniDbNumber('pricecom', command + ' ' + subCommand);
        if (cost.isPresent()) {
            return cost.get();
        }

        cost = $.optIniDbNumber('pricecom', command);
        if (cost.isPresent()) {
            return cost.get();
        }
        return 0;
    }

    $.bind('command', function (event) {
        let sender = event.getSender(),
                command = event.getCommand(),
                args = event.getArgs(),
                action = args[0];

        /*
         * @commandpath disablecom [command] - Disable a command from being used in chat
         */
        if ($.equalsIgnoreCase(command, 'disablecom')) {
            if (action === undefined) {
                $.say($.whisperPrefix(sender) + $.lang.get('customcommands.disable.usage'));
                return;
            }

            action = $.jsString(action.replace('!', '').toLowerCase());

            if ($.inidb.exists('disabledCommands', action)) {
                $.say($.whisperPrefix(sender) + $.lang.get('customcommands.disable.err'));
                return;
            } else if ((!$.commandExists(action) && action !== '@all') || disablecomBlocked.includes(action)) {
                $.say($.whisperPrefix(sender) + $.lang.get('customcommands.disable.404'));
                return;
            }

            $.say($.whisperPrefix(sender) + $.lang.get('customcommands.disable.success', action));
            if (action === '@all') {
                let commands = $.listCommands();

                $.logCustomCommand({
                    'disable.command': action,
                    'sender': sender
                });

                for (let x in commands) {
                    if (!disablecomBlocked.includes(commands[x]) && customCommands[commands[x]] === undefined) {
                        $.inidb.set('disabledCommands', commands[x], true);
                        $.tempUnRegisterChatCommand(commands[x]);
                    }
                }
            } else {
                $.logCustomCommand({
                    'disable.command': '!' + action,
                    'sender': sender
                });
                $.inidb.set('disabledCommands', action, true);
                $.tempUnRegisterChatCommand(action);
            }
            return;
        }

        /*
         * @commandpath enablecom [command] - Enable a command thats been disabled from being used in chat
         */
        if ($.equalsIgnoreCase(command, 'enablecom')) {
            if (action === undefined) {
                $.say($.whisperPrefix(sender) + $.lang.get('customcommands.enable.usage'));
                return;
            }

            action = $.jsString(action.replace('!', '').toLowerCase());

            if (action !== '@all' && !$.inidb.exists('disabledCommands', action)) {
                $.say($.whisperPrefix(sender) + $.lang.get('customcommands.enable.err'));
                return;
            }

            $.say($.whisperPrefix(sender) + $.lang.get('customcommands.enable.success', action));

            if (action === '@all') {
                let commands = $.listCommands();

                $.logCustomCommand({
                    'enable.command': action,
                    'sender': sender
                });

                for (let x in commands) {
                    if (customCommands[commands[x]] === undefined) {
                        $.inidb.del('disabledCommands', commands[x]);
                        $.registerChatCommand($.getIniDbString('tempDisabledCommandScript', commands[x], './commands/customCommands.js'), commands[x]);
                    }
                }
            } else {
                $.logCustomCommand({
                    'enable.command': '!' + action,
                    'sender': sender
                });
                $.inidb.del('disabledCommands', action);
                $.registerChatCommand($.getIniDbString('tempDisabledCommandScript', action, './commands/customCommands.js'), action);
            }
            return;
        }
    });

    /*
     * @event initReady
     */
    $.bind('initReady', function () {
        $.registerChatCommand('./core/commandRegister.js', 'disablecom', $.PERMISSION.Admin);
        $.registerChatCommand('./core/commandRegister.js', 'enablecom', $.PERMISSION.Admin);
    });

    /** Export functions to API */
    $.registerChatCommand = registerChatCommand;
    $.registerChatSubcommand = registerChatSubcommand;
    $.unregisterChatCommand = unregisterChatCommand;
    $.unregisterChatSubcommand = unregisterChatSubcommand;
    $.commandExists = commandExists;
    $.subCommandExists = subCommandExists;
    $.getCommandGroup = getCommandGroup;
    $.getCommandGroupName = getCommandGroupName;
    $.getSubcommandGroup = getSubcommandGroup;
    $.getSubCommandGroupName = getSubCommandGroupName;
    $.updateCommandGroup = updateCommandGroup;
    $.updateSubcommandGroup = updateSubcommandGroup;
    $.getCommandScript = getCommandScript;
    $.aliasExists = aliasExists;
    $.registerChatAlias = registerChatAlias;
    $.unregisterChatAlias = unregisterChatAlias;
    $.tempUnRegisterChatCommand = tempUnRegisterChatCommand;
    $.getSubCommandFromArguments = getSubCommandFromArguments;
    $.listCommands = listCommands;
    $.listSubCommands = listSubCommands;
    $.commandRestriction = RESTRICTION;
    $.commandRestrictionMet = commandRestrictionMet;
    $.setCommandRestriction = setCommandRestriction;
    $.getCommandRestrictionByName = getCommandRestrictionByName;
    $.priceCom = priceCom;
    $.disablecomBlocked = getDisablecomBlocked;
    $.getCommandPrice = getCommandPrice;

    $.bind('webPanelSocketUpdate', function (event) {
        if ($.equalsIgnoreCase(event.getScript(), './core/commandRegister.js')) {
            var args = event.getArgs(),
                eventName = args[0] + '',
                command = args[1] + '',
                commandLower = command.toLowerCase() + '';
            if (eventName === 'enable') {
                let tempDisabled = $.optIniDbString('tempDisabledCommandScript', commandLower);
                if (tempDisabled.isPresent()) {
                    $.registerChatCommand(tempDisabled.get(), commandLower);
                }
            } else if (eventName === 'disable') {
                if (commandExists(commandLower)) {
                    tempUnRegisterChatCommand(commandLower);
                }
            }
        }
    });
})();
