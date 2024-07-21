package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import eu.pb4.placeholders.api.parsers.NodeParser;
import java.util.Arrays;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;

public final class DynamicColorNode extends SimpleStylingNode implements DynamicShadowNode.SimpleColoredTransformer {
    private final TextNode color;

    public DynamicColorNode(TextNode[] children, TextNode color) {
        super(children);
        this.color = color;
    }

    @Override
    public boolean isDynamicNoChildren() {
        return this.color.isDynamic();
    }

    @Override
    protected Style style(ParserContext context) {
        var c = TextColor.parseColor(color.toText(context).getString());
        return c.result().map(Style.EMPTY::withColor).orElse(Style.EMPTY);
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new DynamicColorNode(children, this.color);
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children, NodeParser parser) {
        return new DynamicColorNode(children, parser.parseNode(color));
    }

    @Override
    public String toString() {
        return "ColorNode{" +
                "color=" + color +
                ", children=" + Arrays.toString(children) +
                '}';
    }

    @Override
    public int getDefaultShadowColor(Component out, float scale, float alpha, ParserContext context) {
        var color = TextColor.parseColor(this.color.toText(context).getString());

        if (color.result().isPresent()) {
            return DynamicShadowNode.modifiedColor(color.getOrThrow().getValue(), scale, alpha);
        }
        return 0;
    }

    @Override
    public boolean hasShadowColor(ParserContext context) {
        return TextColor.parseColor(this.color.toText(context).getString()).result().isPresent();
    }
}
