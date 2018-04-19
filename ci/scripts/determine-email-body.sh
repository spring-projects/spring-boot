#!/bin/bash
set -e

pushd git-repo > /dev/null
	PREV_SHA=$( git rev-parse HEAD^1 )
popd > /dev/null
PREV_STATUSES=$( curl https://api.github.com/repos/spring-projects/spring-boot/commits/$PREV_SHA/statuses -H "Authorization: token ${ACCESS_TOKEN}" )
PREV_STATES=$( echo $PREV_STATUSES | jq -r --arg BUILD_JOB_NAME "$BUILD_JOB_NAME" '.[]  | select(.context == $BUILD_JOB_NAME) | .state' )
WAS_PREV_SUCCESSFUL=$( echo "$PREV_STATES" | grep 'success' || true )

if [[ $STATE == "success" ]];then
	echo "Build SUCCESSFUL ${BUILD_PIPELINE_NAME} / ${BUILD_JOB_NAME}" > email-details/subject
	if [[ $WAS_PREV_SUCCESSFUL == "" ]];then
    	echo "Build ${CONCOURSE_URL}/teams/spring-boot/pipelines/${BUILD_PIPELINE_NAME} is successful!" > email-details/body
	elif [[ $WAS_PREV_SUCCESSFUL == "success" ]];then
		touch email-details/body
	fi
elif [[ $STATE == "failure" ]];then
	echo "Build ${CONCOURSE_URL}/teams/spring-boot/pipelines/${BUILD_PIPELINE_NAME} has failed!" > email-details/body
	if [[ $WAS_PREV_SUCCESSFUL == "" ]];then
		echo "Still FAILING ${BUILD_PIPELINE_NAME} / ${BUILD_JOB_NAME}" > email-details/subject
	elif [[ $WAS_PREV_SUCCESSFUL == "success" ]];then
		echo "Build FAILURE ${BUILD_PIPELINE_NAME} / ${BUILD_JOB_NAME}" > email-details/subject
	fi
fi
