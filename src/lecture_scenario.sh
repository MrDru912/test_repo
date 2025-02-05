#!/bin/bash

p1_hostname="192.168.56.105"
p1_port=2010
p1_api_port=$((p1_port + 5000))

p2_hostname="192.168.56.106"
p2_port=2020
p2_api_port=$((p2_port + 5000))

p3_hostname="192.168.56.107"
p3_port=2030
p3_api_port=$((p3_port + 5000))

# Define variables for processes
p1="${p1_hostname}:${p1_api_port}"
p2="${p2_hostname}:${p2_api_port}"
p3="${p3_hostname}:${p3_api_port}"

# Define variables for resources
r1="${p1_hostname}:${p1_port}_R"
r2="${p2_hostname}:${p2_port}_R"
r3="${p3_hostname}:${p3_port}_R"

# List of URLs to call
urls=(
    "http://$p2/join/$p1_hostname/$p1_port"
    "http://$p3/join/$p2_hostname/$p2_port"
    "http://$p2/preliminary_request/$r1"
    "http://$p1/preliminary_request/$r1"
    "http://$p2/preliminary_request/$r2"
    "http://$p3/preliminary_request/$r2"
    "http://$p1/preliminary_request/$r3"
    "http://$p3/preliminary_request/$r3"
    "http://$p2/request_resource/$r2"
    "http://$p1/request_resource/$r1"
    "http://$p3/request_resource/$r3"
    "http://$p2/request_resource/$r1"
    "http://$p2/release_resource/$r1"
    "http://$p2/release_resource/$r2"
    "http://$p1/request_resource/$r3"
    "http://$p1/release_resource/$r1"
    "http://$p1/release_resource/$r3"
    "http://$p3/request_resource/$r2"
    "http://$p3/release_resource/$r2"
    "http://$p3/release_resource/$r3"
)

# Loop through URLs and make GET requests
for url in "${urls[@]}"; do
    echo "Calling: $url"
    curl -X GET "$url" -s -o /dev/null -w "Status: %{http_code}\n"
    sleep 1  # Delay of 1 second between requests
done

echo "All requests completed."
