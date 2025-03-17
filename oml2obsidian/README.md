# OML to Obsidian

[![Release](https://img.shields.io/github/v/tag/opencaesar/obsidian-adapter?label=release)](https://github.com/opencaesar/obsidian-adapter/releases/latest)

A tool to translate [OML](https://opencaesar.github.io/oml) vocabularies to [Obsidian](https://obsidian.md/) files

## Run as CLI

MacOS/Linux:
```
./gradlew oml2obsidian:run --args="..."
```
Windows:
```
gradlew.bat oml2obsidian:run --args="..."
```
Args:
```
--input-catalog-path | -i path/to/input/oml/catalog.xml [Required]
--root-ontology-iri | -r http://... [Optional]
--output-catalog-path | -o path/to/output/owl/catalog.xml [Required]
--output-file-extension | -f [optional, options: owl (default), rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
--disjoint-unions | -u [Optional]
--annotations-on-axioms | -a [Optional]
--generateRules | -rl [Optional]
--oml-annotations | -an suppress [Optional, options=generate (default), suppress]
```

## Run as Gradle Task
```
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath 'io.opencaesar.adapters:oml2obsidian-gradle:+'
    }
}
task oml2obsidian(type:io.opencaesar.oml2obsidian.Oml2ObsidianTask) {
    inputCatalogPath = file('path/to/input/oml/catalog.xml') [Required]
    rootOntologyIri = 'http://...' [Optional]
    outputCatalogPath = file('path/to/output/owl/catalog.xml') [Required]
    outputFileExtension = 'owl' [Optional, default=owl, options: owl, rdf, xml, rj, ttl, n3, nt, trig, nq, trix, jsonld, fss]
    disjointUnions = true [Optional, false by default]
    annotationsOnAxioms = true [Optional, false by default]
    generateRules = true [Optional, false by default] // generates Jena rules files
    omlAnnotations = 'suppress' [Optional,options='generate' (default), 'suppress'] 
}
