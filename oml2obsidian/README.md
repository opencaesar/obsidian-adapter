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
--input-ontology-iri | -iri http://... [Required]
--output-vault-path | -o path/to/output/obsidian/vault [Required]
--output-classes-path | -cls relative/path/to/vault/classes [Required]
--output-templates-path | -tmp relative/path/to/vault/templates [Required]
--debug | -d [optional]
--help | -h [optional]
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
    inputOntologyIri = 'http://...' [Required]
    outputVaultPath = file('path/to/output/obsidian/vault') [Required]
    outputClassesPath = 'metadata/classes' [Required]
    outputTemplatesPath = 'metadata/templates' [Required]
}
