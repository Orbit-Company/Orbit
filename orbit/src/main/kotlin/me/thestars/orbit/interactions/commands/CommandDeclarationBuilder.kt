package me.thestars.orbit.interactions.commands

import dev.minn.jda.ktx.interactions.commands.Command
import dev.minn.jda.ktx.interactions.commands.Subcommand
import dev.minn.jda.ktx.interactions.commands.SubcommandGroup
import me.thestars.orbit.utils.common.OrbitLocale
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.interactions.IntegrationType
import net.dv8tion.jda.api.interactions.InteractionContextType
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData
import net.dv8tion.jda.api.interactions.DiscordLocale

class CommandDeclarationBuilder(
    val name: String,
    val description: String,
    var aliases: List<String> = emptyList(),
    var integrationType: List<IntegrationType> = listOf(IntegrationType.GUILD_INSTALL),
    var interactionContexts: List<InteractionContextType> = listOf(InteractionContextType.GUILD),
    var baseName: String? = name,
    var executor: UnleashedCommandExecutor? = null
) {
    val subCommands = mutableListOf<CommandDeclarationBuilder>()
    private val subCommandGroups = mutableListOf<CommandGroupBuilder>()
    private val permissions = mutableListOf<Permission>()
    private val commandOptions = mutableListOf<OptionData>()

    companion object {
        private val enUsLocale = OrbitLocale("en-us")
        private val ptBrLocale = OrbitLocale("pt-br")

        private fun getTranslation(locale: OrbitLocale, key: String, fallback: String): String {
            val result = locale[key]
            return if (result.startsWith("!!{") && result.endsWith("}!!")) fallback else result
        }

        private fun getNameLocalizations(path: String, fallback: String): Map<DiscordLocale, String> {
            val ptValue = getTranslation(ptBrLocale, "$path.name", fallback)
            return if (ptValue != fallback) {
                mapOf(DiscordLocale.PORTUGUESE_BRAZILIAN to ptValue)
            } else {
                emptyMap()
            }
        }

        private fun isValidTranslation(value: String): Boolean =
            value.isNotEmpty() && !value.startsWith("!!{")

        private fun getDescription(path: String, fallback: String): Pair<String, Map<DiscordLocale, String>> {
            val enValue = getTranslation(enUsLocale, "$path.description", "")
            val ptValue = getTranslation(ptBrLocale, "$path.description", "")

            val mainDesc = enValue.takeIf { isValidTranslation(it) } ?: fallback

            val locs = buildMap {
                if (isValidTranslation(enValue)) put(DiscordLocale.ENGLISH_US, enValue)
                if (isValidTranslation(ptValue)) put(DiscordLocale.PORTUGUESE_BRAZILIAN, ptValue)
            }

            return mainDesc to locs
        }
    }

    fun subCommand(
        name: String,
        description: String = "placeholderDescription",
        integrationType: List<IntegrationType> = listOf(IntegrationType.GUILD_INSTALL),
        interactionContexts: List<InteractionContextType> = listOf(InteractionContextType.GUILD),
        block: CommandDeclarationBuilder.() -> Unit
    ) {
        val subCommand = CommandDeclarationBuilder(
            name,
            description,
            aliases,
            integrationType,
            interactionContexts
        )
        subCommand.baseName = baseName ?: this.name
        subCommand.block()
        subCommands.add(subCommand)

        if (baseName != null) {
            this.baseName = baseName
        }
    }

    fun subCommandGroup(
        name: String,
        description: String = "placeholderDescription",
        block: CommandGroupBuilder.() -> Unit
    ) {
        val group = CommandGroupBuilder(name, description)
        group.block()
        this.baseName = baseName
        subCommandGroups.add(group)
    }

    fun addPermission(vararg permission: Permission) {
        permission.forEach { permissions.add(it) }
    }

    fun addOptions(
        vararg options: List<OptionData>,
        isSubCommand: Boolean = false,
        baseName: String? = this@CommandDeclarationBuilder.baseName
    ) {
        options.forEach { option ->
            addOption(*option.toTypedArray(), isSubCommand = isSubCommand, baseName = baseName)
        }
    }

    fun opt(type: OptionType, name: String, required: Boolean = false) =
        OptionData(type, name, "No description", required)

    fun addOption(
        vararg option: OptionData,
        isSubCommand: Boolean = false,
        baseName: String? = this@CommandDeclarationBuilder.baseName
    ) {
        option.forEach { op ->
            val optionPath = if (isSubCommand) {
                "commands.command.${baseName}.${name}.options.${op.name}"
            } else {
                "commands.command.$baseName.options.${op.name}"
            }

            val (mainDesc, descLocs) = getDescription(optionPath, "No description")
            op.description = mainDesc

            val nameLocs = getNameLocalizations(optionPath, op.name)

            if (nameLocs.isNotEmpty()) op.setNameLocalizations(nameLocs)
            if (descLocs.isNotEmpty()) op.setDescriptionLocalizations(descLocs)

            commandOptions.add(op)
        }
    }

    fun getSubCommand(name: String): CommandDeclarationBuilder? {
        return subCommands.find { it.name == name || it.aliases.contains(name) }
    }

    fun getSubCommandGroup(name: String): CommandGroupBuilder? {
        return subCommandGroups.find { it.name == name }
    }

    fun build(): SlashCommandData {
        val commandPath = "commands.command.$name"
        val (commandDesc, commandDescLocs) = getDescription(commandPath, description)

        val commandData = Command(name, commandDesc) {
            this.setIntegrationTypes(integrationType[0], *integrationType.drop(1).toTypedArray())
            this.setContexts(interactionContexts[0], *interactionContexts.drop(1).toTypedArray())

            val nameLocs = getNameLocalizations(commandPath, name)

            if (nameLocs.isNotEmpty()) setNameLocalizations(nameLocs)
            if (commandDescLocs.isNotEmpty()) setDescriptionLocalizations(commandDescLocs)

            defaultPermissions = DefaultMemberPermissions.enabledFor(permissions)
            this.addOptions(commandOptions)

            subCommands.forEach { subCmd ->
                val subPath = "commands.command.${baseName}.${subCmd.name}"
                val (subDesc, subDescLocs) = getDescription(subPath, subCmd.description)

                addSubcommands(
                    Subcommand(subCmd.name, subDesc) {
                        val subNameLocs = getNameLocalizations(subPath, subCmd.name)

                        if (subNameLocs.isNotEmpty()) setNameLocalizations(subNameLocs)
                        if (subDescLocs.isNotEmpty()) setDescriptionLocalizations(subDescLocs)

                        this.addOptions(subCmd.commandOptions)
                    }
                )
            }

            subCommandGroups.forEach { group ->
                val groupPath = "commands.command.${baseName}.${group.name}"
                val (groupDesc, groupDescLocs) = getDescription(groupPath, group.description)

                addSubcommandGroups(
                    SubcommandGroup(group.name, groupDesc).apply {
                        val groupNameLocs = getNameLocalizations(groupPath, group.name)

                        if (groupNameLocs.isNotEmpty()) setNameLocalizations(groupNameLocs)
                        if (groupDescLocs.isNotEmpty()) setDescriptionLocalizations(groupDescLocs)

                        group.subCommands.forEach { subCommand ->
                            val subCmdPath = "commands.command.${baseName}.${group.name}.${subCommand.name}"
                            val (subCmdDesc, subCmdDescLocs) = getDescription(subCmdPath, subCommand.description)

                            addSubcommands(
                                Subcommand(subCommand.name, subCmdDesc) {
                                    val scNameLocs = getNameLocalizations(subCmdPath, subCommand.name)

                                    if (scNameLocs.isNotEmpty()) setNameLocalizations(scNameLocs)
                                    if (subCmdDescLocs.isNotEmpty()) setDescriptionLocalizations(subCmdDescLocs)

                                    this.addOptions(subCommand.commandOptions)
                                }
                            )
                        }
                    }
                )
            }
        }

        return commandData
    }
}