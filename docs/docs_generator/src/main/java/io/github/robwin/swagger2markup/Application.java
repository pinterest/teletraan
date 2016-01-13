package io.github.robwin.swagger2markup;

import io.airlift.airline.*;
import io.github.robwin.markup.builder.MarkupLanguage;

import java.io.IOException;

public class Application {
    public static void main(String[] args) {
        Cli.CliBuilder<Runnable> builder = Cli.<Runnable>builder("swagger2markup")
                .withDescription("Swagger2Markup converts a Swagger JSON or YAML file into several AsciiDoc or GitHub Flavored Markdown documents which can be combined with hand-written documentation.")
                .withDefaultCommand(Help.class)
                .withCommands(Help.class, Generate.class);

        Cli<Runnable> parser = builder.build();
        parser.parse(args).run();
    }

    public static class BaseCommand implements Runnable {
        @Option(type = OptionType.GLOBAL, name = "-v", description = "Verbose mode")
        public boolean verbose;

        public void run() {

        }
    }

    @Command(name = "generate", description = "Generate")
    public static class Generate extends BaseCommand {
        public static final String ASCIIDOC = "ASCIIDOC";
        public static final String MARKDOWN = "MARKDOWN";
        public static final String AS_IS = "AS_IS";
        public static final String NATURAL = "NATURAL";
        public static final String TAGS = "TAGS";

        @Option(name = "-i", required = true, description = "Input file")
        public String inputFile;

        @Option(name = "-o", required = true, description = "Output path")
        public String outputPath;

        @Option(name = "-l", required = true, allowedValues = {ASCIIDOC, MARKDOWN}, description = "Markup language")
        public String language;

        @Option(name = "-g", allowedValues = {AS_IS, TAGS}, description = "Specifies if the paths should be grouped by tags or stay as-is")
        public String pathsGroupedBy;

        @Option(name = "-n", allowedValues = {AS_IS, NATURAL}, description = "Specifies if the definitions should be ordered by natural ordering or stay as-is")
        public String definitionsOrderedBy;

        @Option(name = "-d", description = "Include hand-written descriptions into the Paths and Definitions document")
        public String descriptionsPath;

        @Option(name = "-e", description = "Include examples into the Paths document")
        public String examplesPath;

        @Option(name = "-s", description = "Include (JSON, XML) schemas into the Definitions document")
        public String schemasPath;

        @Option(name = "-p", description = "In addition to the definitions file, also create separate definition files for each model definition.")
        public boolean separateDefinitions;

        @Override
        public void run() {
            try {
                final Swagger2MarkupConverter.Builder builder = Swagger2MarkupConverter
                        .from(inputFile)
                        .withMarkupLanguage(MarkupLanguage.valueOf(language.toUpperCase()));
                if (pathsGroupedBy != null) {
                    builder.withPathsGroupedBy(GroupBy.valueOf(pathsGroupedBy.toUpperCase()));
                }
                if (definitionsOrderedBy != null) {
                    builder.withDefinitionsOrderedBy(OrderBy.valueOf(definitionsOrderedBy.toUpperCase()));
                }
                if (examplesPath != null) {
                    builder.withExamples(examplesPath);
                }
                if (descriptionsPath != null) {
                    builder.withDescriptions(descriptionsPath);
                }
                if (schemasPath != null) {
                    builder.withSchemas(schemasPath);
                }
                if (separateDefinitions) {
                    builder.withSeparatedDefinitions();
                }
                builder.build().intoFolder(outputPath);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
