package eu.pb4.placeholders.api.node.parent;

import eu.pb4.placeholders.api.ParserContext;
import eu.pb4.placeholders.api.node.TextNode;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;

public final class ObfuscatedNode extends ParentNode {
    private final boolean value;

    public ObfuscatedNode(TextNode[] nodes, boolean value) {
        super(nodes);
        this.value = value;
    }

    @Override
    protected Component applyFormatting(MutableComponent out, ParserContext context) {
        return out.setStyle(out.getStyle().withObfuscated(this.value));
    }

    @Override
    public ParentTextNode copyWith(TextNode[] children) {
        return new ObfuscatedNode(children, this.value);
    }

    @Override
    public String toString() {
        return "ObfuscatedNode{" +
                "value=" + value +
                ", children=" + Arrays.toString(children) +
                '}';
    }
}