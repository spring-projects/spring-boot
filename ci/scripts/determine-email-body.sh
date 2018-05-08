#!/bin/bash
set -e

pushd git-repo > /dev/null
	PREV_SHA=$( git rev-parse HEAD^1 )
popd > /dev/null

function getPreviousSha() {
	pushd git-repo > /dev/null
		PREV_SHA=$( git rev-parse "$1"^1 )
	popd > /dev/null
	echo "$PREV_SHA"
}

function getPreviousStates() {
	PREV_STATUSES=$( curl https://api.github.com/repos/spring-projects/spring-boot/commits/"$1"/statuses -H "Authorization: token ${ACCESS_TOKEN}" )
	PREV_STATES=$( echo "$PREV_STATUSES" | jq -r --arg BUILD_JOB_NAME "$BUILD_JOB_NAME" '.[]  | select(.context == $BUILD_JOB_NAME) | .state' )
	echo "$PREV_STATES"
}

PREV_STATES=$( getPreviousStates "$PREV_SHA" )
WAS_PREV_SUCCESSFUL=$( echo "$PREV_STATES" | grep 'success' || true )
IS_PREV_FAILURE=$( echo "$PREV_STATES" | grep 'failure' || true )

while [[ $WAS_PREV_SUCCESSFUL == "" ]] && [[ $IS_PREV_FAILURE == "" ]]; do
	PREV_SHA=$( getPreviousSha "$PREV_SHA" )
	PREV_STATES=$(getPreviousStates "$PREV_SHA")
	WAS_PREV_SUCCESSFUL=$( echo "$PREV_STATES" | grep 'success' || true )
	IS_PREV_FAILURE=$( echo "$PREV_STATES" | grep 'failure' || true )
done

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
