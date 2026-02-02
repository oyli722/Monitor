package com.hundred.monitor.server.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hundred.monitor.server.model.entity.SshCredential;
import org.apache.ibatis.annotations.Mapper;

/**
 * SSH凭证Mapper
 */
@Mapper
public interface SshCredentialMapper extends BaseMapper<SshCredential> {
}
