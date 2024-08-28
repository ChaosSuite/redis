#!/bin/bash

# 设置 Redis 和 Sentinel 的端口
HOST=192.168.56.10
REDIS_MASTER_PORT=6379
REDIS_SLAVE1_PORT=6380
REDIS_SLAVE2_PORT=6381
SENTINEL_PORT=26379

# 获取当前 master 的主机名和端口
get_master_info() {
    local master_info=$(redis-cli -h $HOST -p $SENTINEL_PORT sentinel get-master-addr-by-name mymaster)
    local hostname=$(echo "$master_info" | head -n 1)
    local port=$(echo "$master_info" | tail -n 1)
    echo "${hostname}:${port}"
}

# 等待新的 master 被选举
wait_for_new_master() {
    local start_time=$(date +%s)
    local timeout=20  # 5 秒超时
    local old_master=$1

    while true; do
        local current_time=$(date +%s)
        local elapsed=$((current_time - start_time))

        if [ $elapsed -ge $timeout ]; then
            echo "Timeout: No new master elected within $timeout seconds."
            exit 1
        fi

        local new_master=$(get_master_info)
        if [ "$new_master" != "$old_master" ]; then
            echo "New master elected: $new_master"
            echo "Time taken: $elapsed seconds"
            break
        fi

        sleep 1
    done
}

# 主测试函数
run_test() {
    echo "Starting failover test..."

    # 获取当前 master
    local old_master=$(get_master_info)
    echo "Current master: $old_master"

    # 从 old_master 中提取端口
    local old_master_port=$(echo $old_master | cut -d':' -f2)

    # 模拟 master 故障
    echo "Simulating master failure..."
    redis-cli -h $HOST -p $old_master_port DEBUG SLEEP 10 &

    # 等待新的 master 被选举
    wait_for_new_master $old_master

    echo "Failover test completed."
}

# 运行测试
run_test