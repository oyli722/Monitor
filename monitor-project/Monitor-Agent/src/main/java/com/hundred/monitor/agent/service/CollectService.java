package com.hundred.monitor.agent.service;

import com.hundred.monitor.commonlibrary.model.BasicInfo;
import com.hundred.monitor.commonlibrary.model.Metrics;
import com.hundred.monitor.commonlibrary.model.Metrics.DiskUsageInfo;
import oshi.SystemInfo;
import oshi.hardware.*;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.FormatUtil;
import oshi.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 信息采集服务
 * 使用OSHI采集系统信息
 */
@Service
public class CollectService {

    private static final Logger log = LoggerFactory.getLogger(CollectService.class);

    private static final int SSH_PORT = 22;

    private final SystemInfo systemInfo = new SystemInfo();

    // 网络速率计算需要的上次状态
    private long lastNetworkBytesReceived = 0;
    private long lastNetworkBytesSent = 0;
    private long lastNetworkTimestamp = 0;

    /**
     * 采集基本数据
     * 包含主机硬件信息（不变或极少变）
     */
    public BasicInfo collectBasicInfo() {
        BasicInfo basicInfo = new BasicInfo();

        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            HardwareAbstractionLayer hardware = systemInfo.getHardware();

            // 1. 主机名
            String hostname = os.getNetworkParams().getHostName();
            basicInfo.setHostname(hostname);


            // 2. CPU信息
            CentralProcessor processor = hardware.getProcessor();
            basicInfo.setCpuModel(processor.getProcessorIdentifier().getName());
            basicInfo.setCpuCores(processor.getLogicalProcessorCount());

            // 3. 内存容量
            GlobalMemory memory = hardware.getMemory();
            long totalBytes = memory.getTotal();
            basicInfo.setMemoryGb(totalBytes / 1024 / 1024 / 1024);

            // 4. 磁盘信息
            List<BasicInfo.DiskInfo> disks = new ArrayList<>();
            for (OSFileStore fs : os.getFileSystem().getFileStores()) {
                BasicInfo.DiskInfo disk = new BasicInfo.DiskInfo();
                disk.setMount(fs.getMount());
                disk.setTotalGb(fs.getTotalSpace() / 1024 / 1024 / 1024);
                disks.add(disk);
            }
            basicInfo.setDisks(disks);

            // 5. 网络接口
            List<String> networkInterfaces = new ArrayList<>();
            for (NetworkIF netIF : systemInfo.getHardware().getNetworkIFs()) {
                networkInterfaces.add(netIF.getName());
            }
            basicInfo.setNetworkInterfaces(networkInterfaces);

            // 6. GPU信息
            List<BasicInfo.GpuInfo> gpus = new ArrayList<>();
            for (GraphicsCard gpu : systemInfo.getHardware().getGraphicsCards()) {
                BasicInfo.GpuInfo gpuInfo = new BasicInfo.GpuInfo();
                gpuInfo.setName(gpu.getName());
                gpuInfo.setVendor(gpu.getVendor());

                // 获取显存并转换为MB（getVRam()返回字节数）
                long vramBytes = gpu.getVRam();
                gpuInfo.setVramMb(vramBytes / 1024 / 1024);

                // OSHI不直接提供核心数，设置为null
                gpuInfo.setCores(null);

                gpus.add(gpuInfo);
            }
            basicInfo.setGpus(gpus);

            log.debug("基本数据采集完成");

        } catch (Exception e) {
            log.error("采集基本数据失败", e);
        }

        return basicInfo;
    }
    public double getCpuUsage() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        HardwareAbstractionLayer hardware = systemInfo.getHardware();
        CentralProcessor processor = hardware.getProcessor();

        // 获取正确的tick数组长度
        long[] prevTicks = processor.getSystemCpuLoadTicks();

        // 等待一段时间获取两次采样
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // 再次获取tick数组
        long[] ticks = processor.getSystemCpuLoadTicks();

        // 确保数组长度一致
        if (prevTicks.length == ticks.length) {
            // 计算CPU使用率
            double cpuLoad = processor.getSystemCpuLoadBetweenTicks(prevTicks);
            return cpuLoad;
        }
        return 0;

    }

    /**
     * 采集运行时数据
     * 包含CPU、内存、磁盘、网络等实时指标
     */
    public Metrics collectMetrics() {
        Metrics metrics = new Metrics();

        try {
            OperatingSystem os = systemInfo.getOperatingSystem();
            HardwareAbstractionLayer hardware = systemInfo.getHardware();
            GlobalMemory memory = hardware.getMemory();

            // 1. CPU使用率

            double cpuUsage = getCpuUsage();
            if (cpuUsage < 0) {
                cpuUsage = 0;
            }
            metrics.setCpuPercent(cpuUsage * 100);

            // 2. 内存使用率
            long totalMemory = memory.getTotal();
            long availableMemory = memory.getAvailable();
            double memoryUsage = (double) (totalMemory - availableMemory) / totalMemory * 100;
            metrics.setMemoryPercent(memoryUsage);

            // 3. 磁盘使用率（每块磁盘单独上报）
            List<DiskUsageInfo> diskUsages = new ArrayList<>();
            for (OSFileStore fs : os.getFileSystem().getFileStores()) {
                DiskUsageInfo diskUsage = new DiskUsageInfo();
                diskUsage.setMount(fs.getMount());
                // 通过分盘计算占用子节

                long totalSpace = fs.getTotalSpace();
                long usableSpace = fs.getUsableSpace();
                long usedSpace = totalSpace - usableSpace;

                diskUsage.setTotalGb(totalSpace / 1024 / 1024 / 1024);
                diskUsage.setUsedGb(usedSpace / 1024 / 1024 / 1024);
                diskUsage.setUsedPercent((double) usedSpace / totalSpace * 100);

                diskUsages.add(diskUsage);
            }
            metrics.setDiskUsages(diskUsages);

            // 4. 网络速率（计算过去间隔内的平均速率）
            calculateNetworkRate(metrics);

            // 5. SSH状态
            Metrics.SshStatus sshStatus = checkSshStatus();
            metrics.setSshStatus(sshStatus);

            log.debug("运行时数据采集完成");

        } catch (Exception e) {
            log.error("采集运行时数据失败", e);
        }

        return metrics;
    }

    /**
     * 计算网络速率
     */
    private void calculateNetworkRate(Metrics metrics) {
        long currentTimestamp = System.currentTimeMillis();

        long totalBytesReceived = 0;
        long totalBytesSent = 0;

        for (NetworkIF netIF : systemInfo.getHardware().getNetworkIFs()) {
            netIF.updateAttributes();
            totalBytesReceived += netIF.getBytesRecv();
            totalBytesSent += netIF.getBytesSent();
        }

        if (lastNetworkTimestamp > 0) {
            long timeDelta = currentTimestamp - lastNetworkTimestamp;
            long deltaReceived = totalBytesReceived - lastNetworkBytesReceived;
            long deltaSent = totalBytesSent - lastNetworkBytesSent;

            if (timeDelta > 0) {
                // 转换为 Mbps: (bytes * 8) / 1000 / 1000
                double downloadMbps = (deltaReceived * 8.0 / timeDelta) / 1000.0;
                double uploadMbps = (deltaSent * 8.0 / timeDelta) / 1000.0;

                metrics.setNetworkDownMbps(downloadMbps);
                metrics.setNetworkUpMbps(uploadMbps);
            }
        }

        lastNetworkBytesReceived = totalBytesReceived;
        lastNetworkBytesSent = totalBytesSent;
        lastNetworkTimestamp = currentTimestamp;
    }

    /**
     * 检查SSH服务状态
     */
    private Metrics.SshStatus checkSshStatus() {
        Metrics.SshStatus sshStatus = new Metrics.SshStatus();
        sshStatus.setPort(SSH_PORT);

        // 检查SSH进程是否运行
        boolean sshProcessRunning = isSshProcessRunning();
        sshStatus.setRunning(sshProcessRunning);

        // 检查SSH端口是否监听
        boolean portListening = isPortListening(SSH_PORT);
        sshStatus.setPortListening(portListening);

        return sshStatus;
    }

    /**
     * 检查SSH进程是否运行
     */
    private boolean isSshProcessRunning() {
        OperatingSystem os = systemInfo.getOperatingSystem();
        List<oshi.software.os.OSProcess> processes = os.getProcesses();

        for (oshi.software.os.OSProcess process : processes) {
            String name = process.getName().toLowerCase();
            if (name.contains("sshd") || name.contains("ssh")) {
                return true;
            }
        }
        return false;
    }

    /**
     * 检查端口是否监听
     */
    private boolean isPortListening(int port) {
        try {
            // 尝试连接本地端口
            Socket socket = new Socket(InetAddress.getByName("localhost"), port);
            socket.close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
