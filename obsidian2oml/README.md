# Obsidian2Oml

[![Release](https://img.shields.io/github/v/tag/opencaesar/obsidian-adapter?label=release)](https://github.com/opencaesar/obsidian-adapter/releases/latest)

A tool that translates [Obsidian](https://obsidian.md/) files to [OML](https://opencaesar.github.io/oml) descriptions

## Run as CLI

MacOS/Linux

```
    ./gradlew obsidian2oml:run --args="..."
```
Windows

```
    gradlew.bat obsidian2oml:run --args="..."
```

Args

```
```

## Run with Gradle
```
buildscript {
	repositories {
		mavenCentral()
	}
	dependencies {
		classpath 'io.opencaesar.adapters:obsidian2oml-gradle:+'
	}
}
task obsidian2oml(type:io.opencaesar.obsidian2oml.Obsidian2OmlTask) {
}               
```