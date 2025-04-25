#!/bin/bash

# Install Java and Parallel
sudo apt update
sudo apt install -y parallel openjdk-17-jdk

# Clone your code (replace with your actual repository)
git clone https://github.com/your-username/MultiThreadedHHCRSP.git
cd MultiThreadedHHCRSP
git checkout cloud

# Run all jobs (10 configs × 30 runs)
parallel -j 5 \
  "java -cp lib/*:out/artifacts/GAforHHCRSP_jar/GAforHHCRSP.jar \
  org.example.Main Experiment/configs/GAConfig_44_0_{1}.json \
  > runlogs/GAConfig_44_300_{1}_{2}.log" \
  ::: {1..10} ::: {01..30}

# Auto-shutdown when done (optional)
sudo shutdown -h now
