package org.gwmdevelopments.sponge_plugin.crates.command;

import org.gwmdevelopments.sponge_plugin.crates.GWMCrates;
import org.gwmdevelopments.sponge_plugin.crates.command.commands.*;
import org.gwmdevelopments.sponge_plugin.crates.command.commands.buy.BuyCaseCommand;
import org.gwmdevelopments.sponge_plugin.crates.command.commands.buy.BuyDropCommand;
import org.gwmdevelopments.sponge_plugin.crates.command.commands.buy.BuyKeyCommand;
import org.gwmdevelopments.sponge_plugin.crates.command.commands.buy.BuySSOCommand;
import org.gwmdevelopments.sponge_plugin.crates.command.commands.give.*;
import org.gwmdevelopments.sponge_plugin.crates.command.commands.withdraw.WithdrawCaseCommand;
import org.gwmdevelopments.sponge_plugin.crates.command.commands.withdraw.WithdrawKeyCommand;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.args.GenericArguments;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.text.Text;

import java.util.Optional;

public class GWMCratesCommandUtils {

    public static void registerCommands() {
        CommandSpec helpCommand = CommandSpec.builder().
                description(Text.of("Help command")).
                executor(new HelpCommand()).
                build();
        CommandSpec guiCommand = CommandSpec.builder().
                permission("gwm_crates.command.gui").
                description(Text.of("GUI for creating crates")).
                executor(new GUICommand()).
                build();
        CommandSpec importToMySQLCommand = CommandSpec.builder().
                permission("gwm_crates.command.import_to_mysql").
                description(Text.of("Import data to MySQL")).
                executor(new ImportToMySQLCommand()).
                arguments(
                        GenericArguments.flags().flag("a").buildWith(GenericArguments.none())).
                build();
        CommandSpec importFromMySQLCommand = CommandSpec.builder().
                permission("gwm_crates.command.import_from_mysql").
                description(Text.of("Import data from MySQL")).
                executor(new ImportFromMySQLCommand()).
                arguments(
                        GenericArguments.flags().flag("a").buildWith(GenericArguments.none())).
                build();
        CommandSpec reloadCommand = CommandSpec.builder().
                permission("gwm_crates.command.reload").
                description(Text.of("Reload plugin")).
                executor(new ReloadCommand()).
                build();
        CommandSpec saveCommand = CommandSpec.builder().
                permission("gwm_crates.command.save").
                description(Text.of("Save plugin configs")).
                executor(new SaveCommand()).
                build();
        CommandSpec openCommand = CommandSpec.builder().
                description(Text.of("Open a crate")).
                executor(new OpenCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager"))).
                build();
        CommandSpec forceCommand = CommandSpec.builder().
                description(Text.of("Force open a crate")).
                executor(new ForceCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.playerOrSource(Text.of("player"))).
                build();
        CommandSpec previewCommand = CommandSpec.builder().
                description(Text.of("Preview a crate")).
                executor(new PreviewCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.playerOrSource(Text.of("player"))).
                build();
        CommandSpec giveCaseCommand = CommandSpec.builder().
                description(Text.of("Give the case to the player")).
                executor(new GiveCaseCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.playerOrSource(Text.of("player")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1),
                        GenericArguments.optional(GenericArguments.bool(Text.of("force")))
                ).
                build();
        CommandSpec giveKeyCommand = CommandSpec.builder().
                description(Text.of("Give the key to the player")).
                executor(new GiveKeyCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.playerOrSource(Text.of("player")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1),
                        GenericArguments.optional(GenericArguments.bool(Text.of("force")))
                ).
                build();
        CommandSpec giveDropCommand = CommandSpec.builder().
                description(Text.of("Give the drop to the player")).
                executor(new GiveDropCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.string(Text.of("drop")),
                        GenericArguments.playerOrSource(Text.of("player")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1)
                ).
                build();
        CommandSpec giveSSOCommand = CommandSpec.builder().
                description(Text.of("Give the SSO to the player")).
                executor(new GiveSSOCommand()).
                arguments(
                        new SuperObjectCommandElement(Text.of("sso"), Optional.empty(), true),
                        GenericArguments.playerOrSource(Text.of("player")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1),
                        GenericArguments.optional(GenericArguments.bool(Text.of("force")))
                ).
                build();
        CommandSpec giveCommand = CommandSpec.builder().
                child(giveCaseCommand, "case").
                child(giveKeyCommand, "key").
                child(giveDropCommand, "drop").
                child(giveSSOCommand, "savedsuperobject", "sso").
                build();
        CommandSpec giveEveryoneCaseCommand = CommandSpec.builder().
                description(Text.of("Give the case to all online players")).
                executor(new GiveEveryoneCaseCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1),
                        GenericArguments.optional(GenericArguments.bool(Text.of("force")))
                ).
                build();
        CommandSpec giveEveryoneKeyCommand = CommandSpec.builder().
                description(Text.of("Give the key to all online players")).
                executor(new GiveEveryoneKeyCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1),
                        GenericArguments.optional(GenericArguments.bool(Text.of("force")))
                ).
                build();
        CommandSpec giveEveryoneDropCommand = CommandSpec.builder().
                description(Text.of("Give the drop to all online players")).
                executor(new GiveEveryoneDropCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.string(Text.of("drop")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1)
                ).
                build();
        CommandSpec giveEveryoneSSOCommand = CommandSpec.builder().
                description(Text.of("Give the SSO to all online players")).
                executor(new GiveEveryoneSSOCommand()).
                arguments(
                        new SuperObjectCommandElement(Text.of("sso"), Optional.empty(), true),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1),
                        GenericArguments.optional(GenericArguments.bool(Text.of("force")))
                ).
                build();
        CommandSpec giveEveryoneCommand = CommandSpec.builder().
                child(giveEveryoneCaseCommand, "case").
                child(giveEveryoneKeyCommand, "key").
                child(giveEveryoneDropCommand, "drop").
                child(giveEveryoneSSOCommand, "savedsuperobject", "sso").
                build();
        CommandSpec withdrawCaseCommand = CommandSpec.builder().
                description(Text.of("Withdraw the case from the player")).
                executor(new WithdrawCaseCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.playerOrSource(Text.of("player")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1),
                        GenericArguments.optional(GenericArguments.bool(Text.of("force")))
                ).
                build();
        CommandSpec withdrawKeyCommand = CommandSpec.builder().
                description(Text.of("Withdraw the key from the player")).
                executor(new WithdrawKeyCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.playerOrSource(Text.of("player")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1),
                        GenericArguments.optional(GenericArguments.bool(Text.of("force")))
                ).
                build();
        CommandSpec withdrawCommand = CommandSpec.builder().
                child(withdrawCaseCommand, "case").
                child(withdrawKeyCommand, "key").
                build();
        CommandSpec checkCommand = CommandSpec.builder().
                description(Text.of("Check player's amount of cases and keys")).
                executor(new CheckCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.playerOrSource(Text.of("player"))).
                build();
        CommandSpec buyCaseCommand = CommandSpec.builder().
                description(Text.of("Buy the case")).
                executor(new BuyCaseCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1)
                ).
                build();
        CommandSpec buyKeyCommand = CommandSpec.builder().
                description(Text.of("Buy the key")).
                executor(new BuyKeyCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1)
                ).
                build();
        CommandSpec buyDropCommand = CommandSpec.builder().
                description(Text.of("Buy the drop")).
                executor(new BuyDropCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager")),
                        GenericArguments.string(Text.of("drop")),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1)
                ).
                build();
        CommandSpec buySSOCommand = CommandSpec.builder().
                description(Text.of("Buy the SSO")).
                executor(new BuySSOCommand()).
                arguments(
                        new SuperObjectCommandElement(Text.of("sso"), Optional.empty(), true),
                        GenericArguments.optional(GenericArguments.integer(Text.of("amount")), 1)
                ).
                build();
        CommandSpec buyCommand = CommandSpec.builder().
                child(buyCaseCommand, "case").
                child(buyKeyCommand, "key").
                child(buyDropCommand, "drop").
                child(buySSOCommand, "savedsuperobject", "sso").
                build();
        CommandSpec listCommand = CommandSpec.builder().
                permission("gwm_crates.command.list").
                description(Text.of("List all available crates")).
                executor(new ListCommand()).
                build();
        CommandSpec infoCommand = CommandSpec.builder().
                description(Text.of("Info about a crate")).
                executor(new InfoCommand()).
                arguments(
                        GenericArguments.onlyOne(new ManagerCommandElement(Text.of("manager")))).
                build();
        CommandSpec loadCommand = CommandSpec.builder().
                permission("gwm_crates.command.load").
                description(Text.of("Load manager from file")).
                executor(new LoadCommand()).
                arguments(
                        GenericArguments.remainingJoinedStrings(Text.of("path"))).
                build();
        CommandSpec unloadCommand = CommandSpec.builder().
                description(Text.of("Unload a manager")).
                executor(new UnloadCommand()).
                arguments(
                        new ManagerCommandElement(Text.of("manager"))).
                build();
        CommandSpec spec = CommandSpec.builder().
                permission("gwm_crates.command").
                description(Text.of("Main plugin command.")).
                child(helpCommand, "help").
                child(guiCommand, "gui").
                child(importToMySQLCommand, "importtomysql").
                child(importFromMySQLCommand, "importfrommysql").
                child(reloadCommand, "reload").
                child(saveCommand, "save").
                child(openCommand, "open").
                child(forceCommand, "force").
                child(previewCommand, "preview").
                child(giveCommand, "give").
                child(giveEveryoneCommand, "giveeveryone").
                child(withdrawCommand, "withdraw").
                child(checkCommand, "check").
                child(buyCommand, "buy").
                child(listCommand, "list").
                child(infoCommand, "info").
                child(loadCommand, "load").
                child(unloadCommand, "unload").
                build();
        Sponge.getCommandManager().register(GWMCrates.getInstance(), spec,
                "gwmcrates", "gwmcrate", "crates", "crate");
    }
}
