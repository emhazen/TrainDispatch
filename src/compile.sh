#!/bin/bash
javac -cp "*:." graph/*.java &&
javac -cp "*:." schedule/*.java &&
javac -cp "*:." main/*.java
