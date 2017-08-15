#!/bin/bash

CONFIGSERVER="10.210.130.46:8090"

function clearENV(){
services=$(curl -s "http://$CONFIGSERVER/naming/admin?action=getservice" | jq .body | sed '{s/\\"/"/g;s/^"//g;s/"$//g}' | jq .services)

service_num=$(echo $services | awk -F '}' '{print NF}')
service_num=$(($service_num-1))
echo "services: $service_num"
for ((i=0; i<$service_num; i++))
do
    service=$(echo $services | jq .[$i].name | awk -F '"' '{print $2}')
    echo "clear service: $service ..."
    clusters=$(curl -s "http://$CONFIGSERVER/naming/admin?action=getcluster" -d "service=$service" | jq .body | sed '{s/\\"/"/g;s/^"//g;s/"$//g}' | jq .clusters)
    cluster_num=$(echo $clusters | awk -F ',' '{print NF}')
    fux=$(echo $clusters | awk -F '"' '{print NF}')
    if [ $fux -eq 1 ]; then
        cluster_num=0
    fi
    echo "service $service has $cluster_num clusters"
    for ((j=0; j<$cluster_num; j++))
    do
        cluster=$(echo $clusters | jq .[$j] | awk -F '"' '{print $2}')
        echo "clear cluster: $cluster ..."
        nodes=$(curl -s "http://$CONFIGSERVER/naming/service?action=lookup" -d "service=$service&cluster=$cluster" | jq .body | sed '{s/\\"/"/g;s/^"//g;s/"$//g}' | jq .nodes)
	workings=$(echo $nodes | jq .working)
	working_num=$(echo $workings | awk -F '}' '{print NF}')
	working_num=$(($working_num-1))
	echo "$service $cluster has $working_num working nodes"
	for ((k=0; k < $working_num; k++))
	do
	    host=$(echo $workings | jq .[$k].host | awk -F '"' '{print $2}')
	    extInfo=$(echo $workings | jq .[$k].extInfo | awk -F '"' '{print $2}')
	    curl -s -d"service=$service&cluster=$cluster&node=$host&extInfo=$extInfo" "http://$CONFIGSERVER/naming/service?action=unregister"
	done

        unreachables=$(echo $nodes | jq .unreachable)
	unreachable_num=$(echo $unreachables | awk -F '}' '{print NF}')
	unreachable_num=$(($unreachable_num-1))
	echo "$service $cluster has $unreachable_num unreachable nodes"
	for ((k=0; k < $unreachable_num; k++))
        do
            host=$(echo $unreachables | jq .[$k].host | awk -F '"' '{print $2}')
            extInfo=$(echo $unreachables | jq .[$k].extInfo | awk -F '"' '{print $2}')
            curl -s -d"service=$service&cluster=$cluster&node=$host&extInfo=$extInfo" "http://$CONFIGSERVER/naming/service?action=unregister"
        done
	echo "delete cluster: $cluster"
	curl -s "http://$CONFIGSERVER/naming/admin?action=deletecluster" -d "service=$service&cluster=$cluster"
    done
    whitelists=$(curl -s "http://$CONFIGSERVER/naming/whitelist?action=get&service=$service" | jq .body | sed '{s/\\"/"/g;s/^"//g;s/"$//g}' | jq .nodes)
    whitelist_num=$(echo $whitelists | awk -F ',' '{print NF}')
    fux=$(echo $whitelists | awk -F '"' '{print NF}')
    if [ $fux -eq 1 ]; then
        whitelist_num=0
    fi
    for ((k=0; k<$whitelist_num; k++))
    do
        node=$(echo $whitelists | jq .[$k] | awk -F '"' '{print $2}')
        echo "$node add whitelist in the $service"
        curl -s -d"service=$service&node=$node" "http://$CONFIGSERVER/naming/whitelist?action=delete"
        echo "delete $node 's whitelist"
    done
    curl -s "http://$CONFIGSERVER/naming/admin?action=deleteservice" -d "service=$service" 
done
}

function getservice(){
    curl -s "http://$CONFIGSERVER/naming/admin?action=getservice" | jq .body | sed '{s/\\"/"/g;s/^"//g;s/"$//g}' | jq .services
}

function getcluster(){
    service="$1"
    curl "http://$CONFIGSERVER/naming/admin?action=getcluster" -d "service=$service" | jq .body | sed '{s/\\"/"/g;s/^"//g;s/"$//g}' | jq .clusters
}

function getRegisterNode(){
    service="$1"
    cluster="$2"
    curl -s "http://$CONFIGSERVER/naming/service?action=lookup" -d "service=$service&cluster=$cluster" | jq .body | sed '{s/\\"/"/g;s/^"//g;s/"$//g}' | jq .nodes
}

function getWhitelist(){
    service="$1"
    curl -s "http://$CONFIGSERVER/naming/whitelist?action=get&service=$service" | jq .body | sed '{s/\\"/"/g;s/^"//g;s/"$//g}' | jq .nodes    
}

usage(){
cat << EOF
***********************************************************
*  Usage: ./clearConfigserver.sh [options...]
*  Options:
*    getService        	
*    getCluster <service> 
*    getRegisterNode <service> <cluster>
*    getWhitelist <service>
*    clear     	
***********************************************************
EOF
exit 1
}

COMMAND="$1"
case $COMMAND in
    getService)
        getservice
        sleep 1
        ;;
    getCluster)
        getcluster
        sleep 1
        ;;
    getRegisterNode)
        getRegisterNode
        sleep 1
        ;;
    getWhitelist)
        getWhitelist
        sleep 1
        ;;
    clear)
        clearENV
        sleep 1
        ;;
    *)
        echo
        echo "Error: you have the wrong command, Please see the usage"
        echo
        usage
        ;;
esac
