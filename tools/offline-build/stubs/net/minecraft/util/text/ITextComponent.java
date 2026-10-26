package net.minecraft.util.text; public interface ITextComponent { Style getStyle(); ITextComponent appendSibling(ITextComponent c); ITextComponent appendText(String s); }
