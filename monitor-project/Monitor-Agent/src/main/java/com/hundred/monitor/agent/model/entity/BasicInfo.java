package com.hundred.monitor.agent.model.entity;

import lombok.Data;

import java.util.List;

/**
 * 基本数据模型
 * 包含主机硬件信息（不变或极少变）
 */
@Data
public class BasicInfo {

    /**
     * 主机名
     */
    private String hostname;

    /**
     * CPU型号
     */
    private String cpuModel;

    /**
     * CPU核心数
     */
    private Integer cpuCores;

    /**
     * 内存容量（GB）
     */
    private Long memoryGb;

    /**
     * 磁盘信息列表
     */
    private List<DiskInfo> disks;

    /**
     * 网络接口列表
     */
    private List<String> networkInterfaces;

    /**
     * GPU信息列表
     */
    private List<GpuInfo> gpus;

    /**
     * 磁盘信息
     */
    @Data
    public static class DiskInfo {
        private String mount;
        private Long totalGb;
    }

    /**
     * GPU信息
     */
    @Data
    public static class GpuInfo {
        private String name;
        private String vendor;
        private Long vramMb;
        private Integer cores;
    }
}
