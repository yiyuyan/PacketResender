package cn.ksmcbrigade.pr.arguments;

import cn.ksmcbrigade.pr.Config;
import cn.ksmcbrigade.pr.PacketResender;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class PacketArgument implements ArgumentType<String> {
    private final StringType type;

    public PacketArgument(final StringType type) {
        this.type = type;
    }

    public static String getString(final CommandContext<?> context, final String name) {
        return context.getArgument(name, String.class);
    }

    public static PacketArgument configPacket(){
        ArrayList<String> packet = new ArrayList<>();
        for (Config.Info packetInfo : PacketResender.config.packetInfos) {
            packet.add(packetInfo.packet());
        }
        return new PacketArgument(new StringType(packet.toArray(new String[0])));
    }

    public static PacketArgument pr_remove(){
        return new PacketArgument(StringType.REMOVE);
    }

    @Override
    public String parse(final StringReader reader) throws CommandSyntaxException {
        if (type == StringType.GREEDY_PHRASE) {
            final String text = reader.getRemaining();
            reader.setCursor(reader.getTotalLength());
            return text;
        } else if (type == StringType.SINGLE_WORD) {
            return reader.readUnquotedString();
        } else {
            return reader.readString();
        }
    }

    @Override
    public String toString() {
        return "string()";
    }

    @Override
    public Collection<String> getExamples() {
        return type.getExamples();
    }

    public static String escapeIfRequired(final String input) {
        for (final char c : input.toCharArray()) {
            if (!StringReader.isAllowedInUnquotedString(c)) {
                return escape(input);
            }
        }
        return input;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if(this.type.equals(StringType.REMOVE)){
            for (Config.Info packetInfo : PacketResender.config.packetInfos) {
                builder.suggest(packetInfo.packet());
            }
        }
        else{
            for (String example : this.type.examples) {
                builder.suggest(example);
            }
        }
        return builder.buildFuture();

    }

    private static String escape(final String input) {
        final StringBuilder result = new StringBuilder("\"");

        for (int i = 0; i < input.length(); i++) {
            final char c = input.charAt(i);
            if (c == '\\' || c == '"') {
                result.append('\\');
            }
            result.append(c);
        }

        result.append("\"");
        return result.toString();
    }

    public static class StringType {
        public static final StringType SINGLE_WORD = new StringType("word", "words_with_underscores");
        public static final StringType QUOTABLE_PHRASE = new StringType("\"quoted phrase\"", "word", "\"\"");
        public static final StringType GREEDY_PHRASE = new StringType("word", "words with spaces", "\"and symbols\"");
        public static final StringType REMOVE = new StringType("command-remove-now");

        private final Collection<String> examples;

        public StringType(final String... examples) {
            this.examples = Arrays.asList(examples);
        }

        public Collection<String> getExamples() {
            return examples;
        }
    }
}
