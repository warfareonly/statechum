#!/bin/sh
#$ -l mem=20G -l rmem=14g

EXPERIMENT=statechum.analysis.learning.experiments.PairSelection.MarkovLearnerExperimentWithStatisticalAnalysis

if [ -z ${SGE_TASK_ID+x} ] || [ "${SGE_TASK_ID}" == "undefined" ];then
	if [ -z ${STATECHUM_COUNT+x} ];then
		# thanks to http://stackoverflow.com/questions/3601515/how-to-check-if-a-variable-is-set-in-bash
		sh ./runexperiment.sh -Xmx13000m ${EXPERIMENT} COLLECT_RESULTS
	else
		sh ./runexperiment.sh -Xmx13000m ${EXPERIMENT} COUNT_TASKS
	fi
else
# if task id is not "undefined", it means we are running an array task
	sh ./runexperiment.sh -Xmx13000m ${EXPERIMENT} RUN_TASK $SGE_TASK_ID
fi

