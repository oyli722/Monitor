package com.hundred.monitor.server.service.impl;

import com.hundred.monitor.server.model.request.ForgetPasswordRequest;
import com.hundred.monitor.server.model.response.ForgetPasswordResponse;
import org.springframework.beans.factory.annotation.Value;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hundred.monitor.server.manager.TokenManager;
import com.hundred.monitor.server.mapper.UserMapper;
import com.hundred.monitor.server.model.Const;
import com.hundred.monitor.server.model.entity.User;
import com.hundred.monitor.server.model.request.LoginRequest;
import com.hundred.monitor.server.model.request.RegisterRequest;
import com.hundred.monitor.server.model.response.LoginResponse;
import com.hundred.monitor.server.model.response.RegisterResponse;
import com.hundred.monitor.server.security.JwtTokenProvider;
import com.hundred.monitor.server.service.AuthService;
import com.hundred.monitor.server.utils.FlowUtils;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 认证服务实现类
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final TokenManager tokenManager;

    private final UserMapper userMapper;
    //验证邮件发送冷却时间限制，秒为单位
    @Value("${spring.web.verify.mail-limit}")
    int verifyLimit;

    @Resource
    AmqpTemplate rabbitTemplate;

    @Resource
    StringRedisTemplate stringRedisTemplate;


    @Resource
    FlowUtils flow;

    /**
     * 邮箱正则表达式
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        String input = loginRequest.getUsername();
        String password = loginRequest.getPassword();

        // 查询用户（根据邮箱或手机号）
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (EMAIL_PATTERN.matcher(input).matches()) {
            wrapper.eq(User::getEmail, input);
        } else {
            wrapper.eq(User::getUsername, input);
        }
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 验证密码
        if (!passwordEncoder.matches(password, user.getPassword())) {
//            throw new RuntimeException("用户名或密码错误");
        }

        // 生成JWT令牌
        String token = jwtTokenProvider.generateToken(user.getUsername());

        // 将token添加到管理器
        tokenManager.addToken(user.getUsername(), token);

        // 构建返回对象
        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setTokenType("Bearer");
        response.setUsername(user.getUsername());
        response.setExpirationTime(System.currentTimeMillis() + jwtTokenProvider.getJwtExpirationInMs());
        response.setRole(user.getUserRole());
        response.setPermissions(new String[0]);

        log.info("用户 {} 登录成功", user.getUsername());
        return response;
    }

    /**
     * 生成注册验证码存入Redis中，并将邮件发送请求提交到消息队列等待发送
     * @param type 类型
     * @param email 邮件地址
     * @param address 请求IP地址
     * @return 操作结果，null表示正常，否则为错误原因
     */
    @Override
    public String registerEmailVerifyCode(String type, String email, String address){
        synchronized (address.intern()) {
            if(!this.verifyLimit(address))
                return "请求频繁，请稍后再试";
            Random random = new Random();
            int code = random.nextInt(899999) + 100000;
            Map<String, Object> data = Map.of("type",type,"email", email, "code", code);
            rabbitTemplate.convertAndSend(Const.MQ_MAIL, data);
            stringRedisTemplate.opsForValue()
                    .set(Const.VERIFY_EMAIL_DATA + email, String.valueOf(code), 3, TimeUnit.MINUTES);
            return null;
        }
    }

    /**
     * 获取JWT过期时间（毫秒）
     */
    public long getJwtExpirationInMs() {
        return jwtTokenProvider.getJwtExpirationInMs();
    }

    @Override
    public void logout(String token) {
        // TODO: 实现用户登出逻辑
        String username = jwtTokenProvider.getUsernameFromToken(token);
        tokenManager.removeToken(username);

        // TODO: 将token加入黑名单（如需要）
        log.info("用户 {} 登出成功", username);
    }

    @Override
    public LoginResponse refreshToken(String token) {
        // TODO: 实现令牌刷新逻辑
        String username = jwtTokenProvider.getUsernameFromToken(token);

        // TODO: 验证旧令牌
        if (!jwtTokenProvider.validateToken(token)) {
            throw new RuntimeException("令牌无效或已过期");
        }

        // TODO: 生成新令牌
        String newToken = jwtTokenProvider.generateToken(username);
        tokenManager.updateToken(username, newToken);

        // TODO: 构建返回对象
        LoginResponse response = new LoginResponse();
        response.setToken(newToken);
        response.setUsername(username);
        response.setExpirationTime(System.currentTimeMillis() + 86400000L);

        log.info("用户 {} 刷新令牌成功", username);
        return response;
    }

    @Override
    public boolean validateToken(String token) {
        // TODO: 实现令牌验证逻辑
        return jwtTokenProvider.validateToken(token);
    }

    @Override
    public boolean userExists(String username) {
        // 判断是邮箱还是手机号
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (EMAIL_PATTERN.matcher(username).matches()) {
            wrapper.eq(User::getEmail, username);
        } else {
            wrapper.eq(User::getPhoneNumber, username);
        }
        return userMapper.selectOne(wrapper) != null;
    }

    @Override
    public boolean validatePassword(String username, String password) {
        // 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        if (EMAIL_PATTERN.matcher(username).matches()) {
            wrapper.eq(User::getEmail, username);
        } else {
            wrapper.eq(User::getPhoneNumber, username);
        }
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            return false;
        }

        // 使用BCrypt验证密码
        return passwordEncoder.matches(password, user.getPassword());
    }

    /**
     * 用户注册
     * @param registerRequest 注册请求
     * @return 注册结果
     */
    @Override
    public RegisterResponse register(RegisterRequest registerRequest) {
        String username = registerRequest.getUsername();
        String password = registerRequest.getPassword();
        String email = registerRequest.getEmail();
        String code = stringRedisTemplate.opsForValue().get(Const.VERIFY_EMAIL_DATA + email);
        if (code == null || !code.equals(registerRequest.getEmailCode())) {
            return RegisterResponse.error("邮箱验证码错误");
        }
        password = passwordEncoder.encode(password);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, username).or(wrapper1 -> wrapper1.eq(User::getEmail, email));
        if(userMapper.selectList(wrapper) != null && !userMapper.selectList(wrapper).isEmpty()){
            return RegisterResponse.error("用户名或邮箱已存在");
        }
        User build = User.builder().userRole("user").username(username).nickname("NewUser").email(email).password(password).build();
        int insert = userMapper.insert(build);
        if (insert <= 0) {
            return RegisterResponse.error("注册失败");
        }
        return RegisterResponse.success(build, jwtTokenProvider.generateToken(username));
    }

    /**
     * 重置密码
     * @param forgetPasswordRequest 重置密码请求
     * @return 重置密码结果
     */
    @Override
    public ForgetPasswordResponse resetPassword(ForgetPasswordRequest forgetPasswordRequest) {
        String email = forgetPasswordRequest.getEmail();
        String code = stringRedisTemplate.opsForValue().get(Const.VERIFY_EMAIL_DATA + email);
        if (code == null || !code.equals(forgetPasswordRequest.getEmailCode())) {
            return ForgetPasswordResponse.error("邮箱验证码错误");
        }
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return ForgetPasswordResponse.error("邮箱对应的用户不存在");
        }
        log.info("user change password:{}",passwordEncoder.encode(forgetPasswordRequest.getPassword()));
        user.setPassword(passwordEncoder.encode(forgetPasswordRequest.getPassword()));
        int update = userMapper.update(user, wrapper);
        if (update <= 0) {
            return ForgetPasswordResponse.error("重置密码失败");
        }
        return ForgetPasswordResponse.success(user, jwtTokenProvider.generateToken(user.getUsername()));
    }

    /**
     * 针对IP地址进行邮件验证码获取限流
     * @param address 地址
     * @return 是否通过验证
     */
    private boolean verifyLimit(String address) {
        String key = Const.VERIFY_EMAIL_LIMIT + address;
        return flow.limitOnceCheck(key, verifyLimit);
    }
}
