#!/bin/bash

cd ~/MultiThreadedHHCRSP/
java -cp lib/jackson-databind-2.18.2.jar:out/artifacts/GAforHHCRSP_jar/GAforHHCRSP.jar:lib/jackson-annotations-2.18.2.jar:lib/jackson-core-2.18.2.jar org.example.Main Experiment/configs/GAConfig_44_6_2.json

