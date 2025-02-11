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

p4_hostname="192.168.56.109"
p4_port=2040
p4_api_port=$((p4_port + 5000))

p5_hostname="192.168.56.108"
p5_port=2050
p5_api_port=$((p5_port + 5000))

# Define variables for processes
p1="${p1_hostname}:${p1_api_port}"
p2="${p2_hostname}:${p2_api_port}"
p3="${p3_hostname}:${p3_api_port}"
p4="${p4_hostname}:${p4_api_port}"
p5="${p5_hostname}:${p5_api_port}"

# Define variables for resources
r1="${p1_hostname}:${p1_port}_R"
r2="${p2_hostname}:${p2_port}_R"
r3="${p3_hostname}:${p3_port}_R"
r4="${p4_hostname}:${p4_port}_R"
r5="${p5_hostname}:${p5_port}_R"


# List of URLs to call
urls=(
    "http://$p2/join/$p1_hostname/$p1_port"
    "http://$p3/join/$p2_hostname/$p2_port"
    "http://$p4/join/$p3_hostname/$p3_port"
    "http://$p5/join/$p4_hostname/$p4_port"
)

# Loop through URLs and make GET requests
for url in "${urls[@]}"; do
    echo "Calling: $url"
    curl -X GET "$url" -s -o /dev/null -w "Status: %{http_code}\n"
    sleep 1  # Delay of 1 second between requests
done



curl -X GET "http://192.168.56.106:7020/join/192.168.56.105/2010" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.107:7030/join/192.168.56.106/2020" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.109:7040/join/192.168.56.107/2030" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.108:2050/join/192.168.56.109/2040" -s -o /dev/null -w "Status: %{http_code}\n"

curl -X GET "http://192.168.56.106:7020/setDelay/10000" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X POST "http://192.168.56.106:7020/preliminary_requests" -H "Content-Type: application/json" -d "[\"192.168.56.107:2030_R\", \"192.168.56.108:2050_R\"]" -s -o /dev/null -w "Status: %{http_code}\n" &
curl -X POST "http://192.168.56.109:7040/preliminary_requests" -H "Content-Type: application/json" -d "[\"192.168.56.107:2030_R\", \"192.168.56.108:2050_R\"]" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.106:7020/setDelay/0" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.109:7040/request_resource/192.168.56.107:2030_R" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.109:7040/request_resource/192.168.56.108:2050_R" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.106:7020/request_resource/192.168.56.107:2030_R" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.106:7020/request_resource/192.168.56.108:2050_R" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.106:7020/release_resource/192.168.56.107:2030_R" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.106:7020/release_resource/192.168.56.108:2050_R" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.109:7040/release_resource/192.168.56.107:2030_R" -s -o /dev/null -w "Status: %{http_code}\n"
curl -X GET "http://192.168.56.109:7040/release_resource/192.168.56.108:2050_R" -s -o /dev/null -w "Status: %{http_code}\n"

echo "All requests completed."
