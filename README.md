# OML Adapter for Ecore

[![Gitpod - Code Now](https://img.shields.io/badge/Gitpod-code%20now-blue.svg?longCache=true)](https://gitpod.io#https://github.com/opencaesar/ecore-adapter)
[![Build Status](https://travis-ci.org/opencaesar/ecore-adapter.svg?branch=master)](https://travis-ci.org/opencaesar/ecore-adapter)
[ ![Download](https://api.bintray.com/packages/opencaesar/ecore-adapter/io.opencaesar.ecore2oml/images/download.svg) ](https://bintray.com/opencaesar/ecore-adapter/io.opencaesar.ecore2oml/_latestVersion)

An [OML](https://github.com/opencaesar/oml) adapter for [Ecore](https://www.eclipse.org/modeling/emf/)

## Clone
```
    git clone https://github.com/opencaesar/ecore-adapter.git
```
      
## Build
Requirements: java 8, node 8.x, 
```
    cd ecore-adapter
    cd io.opencaesar.ecore.adapter/
    ./gradlew clean build
```

## Run

MacOS/Linux:
```
    cd ecore-adapter
    cd io.opencaesar.ecore.adapter/
    ./gradlew io.opencaesar.ecore2oml:run --args="-i path/to/ecore/folder -o path/to/oml/folder"
```
Windows:
```
    cd ecore-adapter
    cd io.opencaesar.ecore.adapter/
    gradlew.bat io.opencaesar.ecore2oml:run --args="-i path/to/ecore/folder -o path/to/oml/folder"
```
