package eu.pb4.placeholderstest;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.placeholders.api.TextParserUtils;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.LegacyFormattingParser;
import eu.pb4.placeholders.api.parsers.TextParserV1;
import eu.pb4.placeholders.api.parsers.MarkdownLiteParserV1;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;


public class TestMod implements ModInitializer {

    private static int perf(CommandContext<CommandSourceStack> context) {
        long placeholderTimeTotal = 0;
        long contextTimeTotal = 0;
        long tagTimeTotal = 0;
        long textTimeTotal = 0;
        Component output = null;
        var input = context.getArgument("text", String.class);
        ServerPlayer player = context.getSource().getPlayer();

        int iter = 1024 * 20;

        try {
            for (int i = 0; i < iter; i++) {
                var time = System.nanoTime();
                var tags = TextNode.asSingle(
                        LegacyFormattingParser.ALL.parseNodes(
                                TextNode.asSingle(
                                        MarkdownLiteParserV1.ALL.parseNodes(
                                                TextNode.asSingle(
                                                        TextParserV1.DEFAULT.parseNodes(new LiteralNode(input))
                                                )
                                        )
                                )
                        )
                );
                tagTimeTotal += System.nanoTime() - time;
                time = System.nanoTime();

                var placeholders = Placeholders.parseNodes(tags);
                placeholderTimeTotal += System.nanoTime() - time;
                time = System.nanoTime();

                var ctx = ParserContext.of(PlaceholderContext.KEY, PlaceholderContext.of(player));
                contextTimeTotal += System.nanoTime() - time;
                time = System.nanoTime();

                Component text = placeholders.toText(ctx, true);
                textTimeTotal +=  System.nanoTime() - time;
                output = text;
            }
            long total = tagTimeTotal + placeholderTimeTotal + textTimeTotal + contextTimeTotal;

            player.sendSystemMessage(Component.literal(Component.Serializer.toJson(output)), false);
            player.sendSystemMessage(ComponentUtils.updateForEntity(context.getSource(), output, context.getSource().getEntity(), 0), false);
            player.sendSystemMessage(Component.literal(
                    "<FULL> Tag: " + ((tagTimeTotal / 1000) / 1000d) + " ms | " +
                            "Context: " + ((contextTimeTotal / 1000) / 1000d) + " ms | " +
                            "Placeholder: " + ((placeholderTimeTotal / 1000) / 1000d) + " ms | " +
                            "Text: " + ((textTimeTotal / 1000) / 1000d) + " ms | " +
                            "All: " + ((total / 1000) / 1000d) + " ms"
            ), false);

            player.sendSystemMessage(Component.literal(
                    "<SINGLE> Tag: " + ((tagTimeTotal / iter / 1000) / 1000d) + " ms | " +
                            "Context: " + ((contextTimeTotal / iter / 1000) / 1000d) + " ms | " +
                            "Placeholder: " + ((placeholderTimeTotal / iter / 1000) / 1000d) + " ms | " +
                            "Text: " + ((textTimeTotal / iter / 1000) / 1000d) + " ms | " +
                            "All: " + ((total / iter / 1000) / 1000d) + " ms"
            ), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            player.sendSystemMessage(Placeholders.parseText(context.getArgument("text", Component.class), PlaceholderContext.of(player)), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test2(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            Component text = TextParserUtils.formatText(context.getArgument("text", String.class));
            player.sendSystemMessage(Component.literal(Component.Serializer.toJson(text)), false);
            player.sendSystemMessage(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test3(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            var time = System.nanoTime();
            var tags = TextNode.asSingle(
                    LegacyFormattingParser.ALL.parseNodes(
                            TextNode.asSingle(
                                    MarkdownLiteParserV1.ALL.parseNodes(
                                            TextNode.asSingle(
                                                    TextParserV1.DEFAULT.parseNodes(new LiteralNode(context.getArgument("text", String.class)))
                                            )
                                    )
                            )
                    )
            );
            var tagTime = System.nanoTime() - time;
            time = System.nanoTime();

            var placeholders = Placeholders.parseNodes(tags);
            var placeholderTime = System.nanoTime() - time;
            time = System.nanoTime();

            Component text = placeholders.toText(ParserContext.of(PlaceholderContext.KEY, PlaceholderContext.of(player)), true);
            var textTime = System.nanoTime() - time;

            player.sendSystemMessage(Component.literal(Component.Serializer.toJson(text)), false);
            player.sendSystemMessage(ComponentUtils.updateForEntity(context.getSource(), text, context.getSource().getEntity(), 0), false);
            player.sendSystemMessage(Component.literal(
                      "Tag: " + ((tagTime / 1000) / 1000d) + " ms | " +
                            "Placeholder: " + ((placeholderTime / 1000) / 1000d) + " ms | " +
                            "Text: " + ((textTime / 1000) / 1000d) + " ms | " +
                            "All: " + (((tagTime + placeholderTime + textTime) / 1000) / 1000d) + " ms"
                    ), false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test4Text(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            Component text = Placeholders.parseText(
                    Placeholders.parseText(TextParserUtils.formatText(context.getArgument("text", String.class)), PlaceholderContext.of(player)),
                    Placeholders.PREDEFINED_PLACEHOLDER_PATTERN,
                    Map.of("player", player.getName())
            );
            player.sendSystemMessage(Component.literal(Component.Serializer.toJson(text)), false);
            player.sendSystemMessage(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test4nodes(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();
            Component text = Placeholders.parseNodes(
                    Placeholders.parseNodes(TextParserUtils.formatNodes(context.getArgument("text", String.class))),
                    Placeholders.PREDEFINED_PLACEHOLDER_PATTERN,
                    Map.of("player", player.getName())
            ).toText(ParserContext.of(PlaceholderContext.KEY, PlaceholderContext.of(player)), true);
            player.sendSystemMessage(Component.literal(Component.Serializer.toJson(text)), false);
            player.sendSystemMessage(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    private static int test5(CommandContext<CommandSourceStack> context) {
        /*try {
            ServerPlayer player = context.getSource().getPlayer();
            Text text = Placeholders.parseTextCustom(
                    TextParser.parse(context.getArgument("text", String.class)),
                    player,
                    Map.of(new Identifier("player"), (ctx) -> PlaceholderResult.value(Component.literal()("").append(player.getName()).setStyle(Style.EMPTY.withColor(TextColor.parse(ctx.getArgument()))))), Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM);

            player.sendSystemMessage();(Component.literal()(Text.Serializer.toJson(text)), false);
            player.sendSystemMessage();(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return 0;
    }

    private static int test6x(CommandContext<CommandSourceStack> context) {
        /*try {
            ServerPlayer player = context.getSource().getPlayer();
            Text text = Placeholders.parseTextCustom(
                    TextParser.parse(context.getArgument("text", String.class)),
                    player,
                    Map.of(new Identifier("player"), (ctx) -> PlaceholderResult.value(Component.literal()("").append(player.getName()).setStyle(Style.EMPTY.withColor(TextColor.parse(ctx.getArgument()))))), Placeholders.ALT_PLACEHOLDER_PATTERN_CUSTOM);

            player.sendSystemMessage();(Component.literal()(Text.Serializer.toJson(text)), false);

            // Never use it, pls
            player.sendSystemMessage();(Component.literal()(eu.pb4.placeholders.old.util.TextParserUtils.convertToString(text)), false);

            player.sendSystemMessage();(text, false);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        return 0;
    }

    private static int test7(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayer();

            var text = Placeholders.parseText(Component.translatable("death.attack.outOfWorld", player.getDisplayName()), PlaceholderContext.of(player));
            player.sendSystemMessage(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, dedicated) -> {
            dispatcher.register(
                    literal("test").then(argument("text", ComponentArgument.textComponent()).executes(TestMod::test))
            );

            dispatcher.register(
                    literal("test2").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test2))
            );

            dispatcher.register(
                    literal("test3").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test3))
            );

            dispatcher.register(
                    literal("perm").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::perf))
            );
            dispatcher.register(
                    literal("test4text").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test4Text))
            );
            dispatcher.register(
                    literal("test4nodes").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test4nodes))
            );
            dispatcher.register(
                    literal("test5").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test5))
            );

            dispatcher.register(
                    literal("test6ohno").then(argument("text", StringArgumentType.greedyString()).executes(TestMod::test6x))
            );

            dispatcher.register(
                    literal("test7").executes(TestMod::test7)
            );
        });
    }

}
