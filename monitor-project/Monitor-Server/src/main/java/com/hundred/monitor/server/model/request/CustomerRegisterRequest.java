package com.hundred.monitor.server.model.request;

import com.hundred.monitor.server.model.entity.Agent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户端注册请求
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerRegisterRequest {

    /**
     * 主机名
     */
    private String hostname;

    /**
     * 客户端IP地址
     */
    private String ip;

    /**
     * 基本运行数据
     */
    private AgentBasicInfo basicInfo;

    /**
     * 基本运行数据
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentBasicInfo {

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
         * GPU信息列表
         */
        private java.util.List<GpuInfo> gpus;

        /**
         * 网络接口列表
         */
        private java.util.List<String> networkInterfaces;

        /**
         * GPU信息
         */
        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class GpuInfo {
            private String name;
            private String vendor;
            private Long vramMb;
            private Integer cores;
        }
    }
}
