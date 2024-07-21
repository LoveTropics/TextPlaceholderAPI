package eu.pb4.placeholders.impl;

import eu.pb4.placeholders.api.node.KeybindNode;
import eu.pb4.placeholders.api.node.LiteralNode;
import eu.pb4.placeholders.api.node.NbtNode;
import eu.pb4.placeholders.api.node.ScoreNode;
import eu.pb4.placeholders.api.node.SelectorNode;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.node.TranslatedNode;
import eu.pb4.placeholders.api.node.parent.ColorNode;
import eu.pb4.placeholders.api.node.parent.FormattingNode;
import eu.pb4.placeholders.api.node.parent.GradientNode;
import eu.pb4.placeholders.api.node.parent.ParentNode;
import eu.pb4.placeholders.api.node.parent.ParentTextNode;
import eu.pb4.placeholders.api.node.parent.StyledNode;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.network.chat.contents.KeybindContents;
import net.minecraft.network.chat.contents.NbtContents;
import net.minecraft.network.chat.contents.PlainTextContents;
import net.minecraft.network.chat.contents.ScoreContents;
import net.minecraft.network.chat.contents.SelectorContents;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.function.Function;


@ApiStatus.Internal
public class GeneralUtils {
    public static final Logger LOGGER = LoggerFactory.getLogger("Text Placeholder API");
    public static final boolean IS_DEV = FabricLoader.getInstance().isDevelopmentEnvironment();
    public static final TextNode[] CASTER = new TextNode[0];

    public static String durationToString(long x) {
        long seconds = x % 60;
        long minutes = (x / 60) % 60;
        long hours = (x / (60 * 60)) % 24;
        long days = x / (60 * 60 * 24);

        if (days > 0) {
            return String.format("%dd%dh%dm%ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh%dm%ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm%ds", minutes, seconds);
        } else if (seconds > 0) {
            return String.format("%ds", seconds);
        } else {
            return "---";
        }
    }

    public static boolean isEmpty(Component text) {
        return (
                text.getContents() == PlainTextContents.EMPTY
                || (text.getContents() instanceof PlainTextContents.LiteralContents l && l.text().isEmpty())
               ) && text.getSiblings().isEmpty();
    }

    public static MutableComponent toGradient(Component base, GradientNode.GradientProvider posToColor) {
        return recursiveGradient(base, posToColor, 0, getGradientLength(base)).text();
    }

    private static int getGradientLength(Component base) {
        int length = base.getContents() instanceof PlainTextContents.LiteralContents l ? l.text().length() : base.getContents() == PlainTextContents.EMPTY ? 0 : 1;

        for (var text : base.getSiblings()) {
            length += getGradientLength(text);
        }

        return length;
    }

    private static TextLengthPair recursiveGradient(Component base, GradientNode.GradientProvider posToColor, int pos, int totalLength) {
        if (base.getStyle().getColor() == null) {
            MutableComponent out = Component.empty().setStyle(base.getStyle());
            if (base.getContents() instanceof PlainTextContents.LiteralContents literalTextContent) {
                var l = literalTextContent.text().length();
                for (var i = 0; i < l; i++) {
                    var character = literalTextContent.text().charAt(i);
                    int value;
                    if (Character.isHighSurrogate(character) && i + 1 < l) {
                        var next = literalTextContent.text().charAt(++i);
                        if (Character.isLowSurrogate(next)) {
                            value = Character.toCodePoint(character, next);
                        } else {
                            value = character;
                        }
                    } else {
                        value = character;
                    }

                    out.append(Component.literal(Character.toString(value)).setStyle(Style.EMPTY.withColor(posToColor.getColorAt(pos++, totalLength))));

                }
            } else {
                out.append(base.plainCopy().setStyle(Style.EMPTY.withColor(posToColor.getColorAt(pos++, totalLength))));
            }

            for (Component sibling : base.getSiblings()) {
                var pair = recursiveGradient(sibling, posToColor, pos, totalLength);
                pos = pair.length;
                out.append(pair.text);
            }
            return new TextLengthPair(out, pos);
        }
        return new TextLengthPair(base.copy(), pos + base.getString().length());
    }

    public static int rgbToInt(float r, float g, float b) {
        return (((int) (r * 0xff)) & 0xFF) << 16 | (((int) (g * 0xff)) & 0xFF) << 8 | (((int) (b * 0xff) & 0xFF));
    }

    public static Component deepTransform(Component input) {
        var output = cloneText(input);
        removeHoverAndClick(output);
        return output;
    }

    public static Component removeHoverAndClick(Component input) {
        var output = cloneText(input);
        removeHoverAndClick(output);
        return output;
    }

    private static void removeHoverAndClick(MutableComponent input) {
        if (input.getStyle() != null) {
            input.setStyle(input.getStyle().withHoverEvent(null).withClickEvent(null));
        }

        if (input.getContents() instanceof TranslatableContents text) {
            for (int i = 0; i < text.getArgs().length; i++) {
                var arg = text.getArgs()[i];
                if (arg instanceof MutableComponent argText) {
                    removeHoverAndClick(argText);
                }
            }
        }

        for (var sibling : input.getSiblings()) {
            removeHoverAndClick((MutableComponent) sibling);
        }

    }

    public static MutableComponent cloneText(Component input) {
        MutableComponent baseText;
        if (input.getContents() instanceof TranslatableContents translatable) {
            var obj = new ArrayList<>();

            for (var arg : translatable.getArgs()) {
                if (arg instanceof Component argText) {
                    obj.add(cloneText(argText));
                } else {
                    obj.add(arg);
                }
            }

            baseText = Component.translatable(translatable.getKey(), obj.toArray());
        } else {
            baseText = input.plainCopy();
        }

        for (var sibling : input.getSiblings()) {
            baseText.append(cloneText(sibling));
        }

        baseText.setStyle(input.getStyle());
        return baseText;
    }

    public static MutableComponent cloneTransformText(Component input, Function<MutableComponent, MutableComponent> transform) {
        MutableComponent baseText;
        if (input.getContents() instanceof TranslatableContents translatable) {
            var obj = new ArrayList<>();

            for (var arg : translatable.getArgs()) {
                if (arg instanceof Component argText) {
                    obj.add(cloneTransformText(argText, transform));
                } else {
                    obj.add(arg);
                }
            }

            baseText = Component.translatable(translatable.getKey(), obj.toArray());
        } else {
            baseText = input.plainCopy();
        }

        for (var sibling : input.getSiblings()) {
            baseText.append(cloneTransformText(sibling, transform));
        }

        baseText.setStyle(input.getStyle());
        return transform.apply(baseText);
    }

    public static Component getItemText(ItemStack stack, boolean rarity) {
        if (!stack.isEmpty()) {
            MutableComponent mutableText = Component.empty().append(stack.getHoverName());
            if (stack.has(DataComponents.CUSTOM_NAME)) {
                mutableText.withStyle(ChatFormatting.ITALIC);
            }

            if (rarity) {
                mutableText.withStyle(stack.getRarity().color());
            }
            mutableText.withStyle((style) -> {
                return style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, new HoverEvent.ItemStackInfo(stack)));
            });

            return mutableText;
        }

        return Component.empty().append(ItemStack.EMPTY.getHoverName());
    }

    public static ParentNode convertToNodes(Component input) {
        var list = new ArrayList<TextNode>();

        if (input.getContents() instanceof PlainTextContents.LiteralContents content) {
            list.add(new LiteralNode(content.text()));
        } else if (input.getContents() instanceof TranslatableContents content) {
            var args = new ArrayList<>();
            for (var arg : content.getArgs()) {
                if (arg instanceof Component text) {
                    args.add(convertToNodes(text));
                } else if (arg instanceof String s) {
                    args.add(new LiteralNode(s));
                } else {
                    args.add(arg);
                }
            }


            list.add(TranslatedNode.ofFallback(content.getKey(), content.getFallback(), args.toArray()));
        } else if (input.getContents() instanceof ScoreContents content) {
            list.add(new ScoreNode(content.getName(), content.getObjective()));
        } else if (input.getContents() instanceof KeybindContents content) {
            list.add(new KeybindNode(content.getName()));
        } else if (input.getContents() instanceof SelectorContents content) {
            list.add(new SelectorNode(content.getPattern(), content.getSeparator().map(GeneralUtils::convertToNodes)));
        } else if (input.getContents() instanceof NbtContents content) {
            list.add(new NbtNode(content.getNbtPath(), content.isInterpreting(), content.getSeparator().map(GeneralUtils::convertToNodes), content.getDataSource()));
        }


        for (var child : input.getSiblings()) {
            list.add(convertToNodes(child));
        }

        if (input.getStyle() == Style.EMPTY) {
            return new ParentNode(list);
        } else {
            var style = input.getStyle();
            var hoverValue = style.getHoverEvent() != null && style.getHoverEvent().getAction() == HoverEvent.Action.SHOW_TEXT
                    ? convertToNodes(style.getHoverEvent().getValue(HoverEvent.Action.SHOW_TEXT)) : null;

            var clickValue = style.getClickEvent() != null ? new LiteralNode(style.getClickEvent().getValue()) : null;
            var insertion = style.getInsertion() != null ? new LiteralNode(style.getInsertion()) : null;

            return new StyledNode(list.toArray(new TextNode[0]), style, hoverValue, clickValue, insertion);
        }
    }

    public static TextNode removeColors(TextNode node) {
        if (node instanceof ParentTextNode parentNode) {
            var list = new ArrayList<TextNode>();

            for (var child : parentNode.getChildren()) {
                list.add(removeColors(child));
            }

            if (node instanceof ColorNode || node instanceof FormattingNode) {
                return new ParentNode(list.toArray(new TextNode[0]));
            } else if (node instanceof StyledNode styledNode) {
                return new StyledNode(list.toArray(new TextNode[0]), styledNode.rawStyle().withColor((TextColor) null), styledNode.hoverValue(), styledNode.clickValue(), styledNode.insertion());
            }

            return parentNode.copyWith(list.toArray(new TextNode[0]));
        } else {
            return node;
        }
    }

    public record TextLengthPair(MutableComponent text, int length) {
        public static final TextLengthPair EMPTY = new TextLengthPair(null, 0);
    }

    public record Pair<L, R>(L left, R right) {
    }

    public record MutableTransformer(Function<Style, Style> textMutableTextFunction) implements Function<MutableComponent, Component> {
        public static final MutableTransformer CLEAR = new MutableTransformer(x -> Style.EMPTY);

        @Override
        public Component apply(MutableComponent text) {
            return GeneralUtils.cloneTransformText(text, this::transformStyle);
        }

        private MutableComponent transformStyle(MutableComponent mutableText) {
            return mutableText.setStyle(textMutableTextFunction.apply(mutableText.getStyle()));
        }
    }
}
