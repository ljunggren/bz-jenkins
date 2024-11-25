# Jenkins Plugin: My Awesome Plugin

![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Jenkins Plugin](https://img.shields.io/jenkins/plugin/my-awesome-plugin)

## Overview

My Awesome Plugin is a Jenkins plugin that enhances your CI/CD pipelines by providing custom functionality to automate your workflows seamlessly.

### Key Features
- Feature 1: Describe the first key feature here.
- Feature 2: Describe the second key feature here.
- Feature 3: Describe the third key feature here.

## Prerequisites

Run with Java 17

```
openjdk 17.0.13 2024-10-15
```
## Compile and Build Instructions

You can build this plugin using the following instructions depending on your environment.

### **1. Standard Build with Maven**

Run the following commands in the project directory:
```bash
mvn clean install
```

### **2. Skipping tests**

```
mvn clean install -DskipTests
```

### **2. Detialed build logs**

```
mvn clean install -X
```

### **3. Run Jenkins locally**

```
mvn hpi:run
```