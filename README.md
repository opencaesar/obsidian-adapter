# OML Adapter for Ecore

[![Build Status](https://travis-ci.org/opencaesar/ecore-adapter.svg?branch=master)](https://travis-ci.org/opencaesar/ecore-adapter)

An [OML](https://opencaesar.github.io/oml-spec) adapter for [Ecore](https://www.eclipse.org/modeling/emf/)

## Clone
```
    git clone https://github.com/opencaesar/ecore-adapter.git
    cd ecore-adapter
```
      
## Build
Requirements: java 8, node 8.x, 
```
    cd ecore-adapter
    ./gradlew build
```

## Run

MacOS/Linux:
```
    cd ecore-adapter
    ./gradlew ecore2oml:run --args="-i path/to/ecore/folder -w path/to/owl.oml -o path/to/oml/folder"
```
Windows:
```
    cd ecore-adapter
    gradlew.bat ecore2oml:run --args="-i path/to/ecore/folder -w path/to/owl.oml -o path/to/oml/folder"
```

## Release

Replace \<version\> by the version, e.g., 1.2
```
  git tag -a v<version> -m "v<version>"
  git push origin v<version>
```