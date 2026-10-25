package net.minecraftforge.common.config; import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME) @Target(ElementType.TYPE) public @interface Config { String modid(); String name() default ""; Type type() default Type.INSTANCE;
 enum Type { INSTANCE }
 @Retention(RetentionPolicy.RUNTIME) @Target({ElementType.FIELD, ElementType.TYPE}) @interface Comment { String[] value(); }
 @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface RangeInt { int min() default Integer.MIN_VALUE; int max() default Integer.MAX_VALUE; }
 @Retention(RetentionPolicy.RUNTIME) @Target(ElementType.FIELD) @interface RangeDouble { double min() default Double.MIN_VALUE; double max() default Double.MAX_VALUE; } }
