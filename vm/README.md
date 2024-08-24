



## 安装相关软件

1. 安装VirtualBox:
   - 访问 https://www.virtualbox.org/wiki/Downloads
   - 下载并安装适用于Windows的最新版本

2. 安装Vagrant:
   - 访问 https://www.vagrantup.com/downloads.html
   - 下载并安装适用于Windows的最新版本

3. 安装Git Bash (如果还没有安装):
   - 访问 https://git-scm.com/download/win
   - 下载并安装

## 创建Vagrant配置文件

新建一个 redis 目录，新建一个名为 Vagrantfile 的文件，其中内容为：

```ruby
Vagrant.configure("2") do |config|
  config.vm.box = "generic/rhel7"
  
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "2048"
    vb.cpus = 2
  end

  config.vm.network "private_network", ip: "192.168.56.10"
  config.vm.hostname = "redis-cluster"
  config.vm.synced_folder "./data", "/vagrant"

  config.vm.provision "shell", inline: <<-SHELL
    sudo yum update -y
    sudo yum install -y epel-release
    sudo yum install -y redis
  SHELL
end
```

理解您的资源限制，我们可以修改方案，在单个虚拟机上搭建整个Redis集群。这种方法虽然不能提供真正的高可用性，但对于学习和测试目的来说是足够的。让我们修改计划，在一个RedHat 7虚拟机上搭建Redis集群。

步骤1: 修改Vagrantfile

我们需要更新Vagrantfile以只创建一个虚拟机。

```ruby
Vagrant.configure("2") do |config|
  config.vm.box = "generic/rhel7"
  
  config.vm.provider "virtualbox" do |vb|
    vb.memory = "2048"
    vb.cpus = 2
  end

  config.vm.network "private_network", ip: "192.168.56.10"
  config.vm.hostname = "redis-cluster"

  config.vm.provision "shell", inline: <<-SHELL
    sudo yum update -y
    sudo yum install -y epel-release
    sudo yum install -y redis
    sudo systemctl enable redis
    sudo systemctl start redis
  SHELL
end

```

## 创建和启动虚拟机

1. 在包含Vagrantfile的目录中打开Git Bash。
2. 运行以下命令创建并启动虚拟机：

   ```
   vagrant up
   ```

3. 虚拟机启动后，使用SSH连接到它：
   ```
   vagrant ssh
   ```

## 在虚拟机中配置Redis实例

我们将在同一台机器上运行6个Redis实例：1个主节点，2个从节点，3个哨兵。

1. 创建所需的目录：
 
   ```
   sudo mkdir -p /etc/redis /var/lib/redis/6379 /var/lib/redis/6380 /var/lib/redis/6381
   ```

2. 创建Redis配置文件：

```plaintext
# Master配置 (/etc/redis/redis-6379.conf)
port 6379
daemonize yes
pidfile /var/run/redis_6379.pid
logfile /var/log/redis_6379.log
dir /var/lib/redis/6379

# Slave 1配置 (/etc/redis/redis-6380.conf)
port 6380
daemonize yes
pidfile /var/run/redis_6380.pid
logfile /var/log/redis_6380.log
dir /var/lib/redis/6380
slaveof 127.0.0.1 6379

# Slave 2配置 (/etc/redis/redis-6381.conf)
port 6381
daemonize yes
pidfile /var/run/redis_6381.pid
logfile /var/log/redis_6381.log
dir /var/lib/redis/6381
slaveof 127.0.0.1 6379

```

3. 启动Redis实例：

```bash
sudo redis-server /etc/redis/redis-6379.conf
sudo redis-server /etc/redis/redis-6380.conf
sudo redis-server /etc/redis/redis-6381.conf
```

## 配置Redis Sentinel

1. 创建Sentinel配置文件：

```plaintext
# Sentinel 1配置 (/etc/redis/sentinel-26379.conf)
port 26379
daemonize yes
pidfile /var/run/redis-sentinel-26379.pid
logfile /var/log/redis-sentinel-26379.log
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1

# Sentinel 2配置 (/etc/redis/sentinel-26380.conf)
port 26380
daemonize yes
pidfile /var/run/redis-sentinel-26380.pid
logfile /var/log/redis-sentinel-26380.log
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1

# Sentinel 3配置 (/etc/redis/sentinel-26381.conf)
port 26381
daemonize yes
pidfile /var/run/redis-sentinel-26381.pid
logfile /var/log/redis-sentinel-26381.log
sentinel monitor mymaster 127.0.0.1 6379 2
sentinel down-after-milliseconds mymaster 5000
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1

```

2. 启动Sentinel实例：
   ```
   sudo redis-sentinel /etc/redis/sentinel-26379.conf
   sudo redis-sentinel /etc/redis/sentinel-26380.conf
   sudo redis-sentinel /etc/redis/sentinel-26381.conf
   ```

## 验证集群

1. 检查主从复制状态：
   ```
   redis-cli -p 6379 info replication
   ```

2. 检查Sentinel状态：
   ```
   redis-cli -p 26379 sentinel master mymaster
   ```

这样，我们就在一个虚拟机中搭建了一个包含1个主节点、2个从节点和3个哨兵的Redis集群。虽然这种设置不能提供真正的高可用性（因为所有实例都在同一台机器上），但它对于学习Redis集群的工作原理非常有用。

## Failover 测试

