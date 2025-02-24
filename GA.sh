#!/bin/bash
#SBATCH - job-name=GAforHHCRSP
#SBATCH --time=48:00:00
#SBATCH --mem=72G
#SBATCH --cpus-per-task=56
#SBATCH --output=slurm_logs/output_%j.log
#SBATCH --error=slurm_logs/error_%j.log

module load java/23.0.1

WORKDIR="Z:\MultiThreadedHHCRSP"
LOGDIR="$WORKDIR/logs"

mkdir -p "$LOGDIR"
mkdir -p "slurm_logs"

java -jar GAforHHCRSP.jar > "$LOGDIR/java_output_$(date +%Y%m%d_%H%M%S).log" 2>&1

tar -czf "$LOGDIR/logs_$(date +%Y%m%d_%H%M%S).tar.gz" "$LOGDIR"*.log

echo "Job completed successfully! Logs are stored in $LOGDIR"