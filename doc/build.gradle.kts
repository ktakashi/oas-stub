plugins {
    id("org.asciidoctor.jvm.convert") version "4.0.2"
}

tasks.asciidoctor {
    setSourceDir(file("."))
    sources(delegateClosureOf<PatternSet> {
        include(
            "manual.adoc",
            "introduction.adoc",
            "start.adoc",
            "metrics.adoc",
            "records.adoc",
            "plugin.adoc",
            "headers.adoc",
            "options.adoc",
            "delay.adoc",
            "storages.adoc",
            "static.adoc",
            "testing.adoc",
            "forwarding.adoc"
            )
    })
    setOutputDir(file("build/doc"))
}