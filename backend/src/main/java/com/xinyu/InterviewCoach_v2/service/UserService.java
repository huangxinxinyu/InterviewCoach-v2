package com.xinyu.InterviewCoach_v2.service;

import com.xinyu.InterviewCoach_v2.dto.CreateUserRequest;
import com.xinyu.InterviewCoach_v2.dto.UserDTO;
import com.xinyu.InterviewCoach_v2.entity.User;
import com.xinyu.InterviewCoach_v2.enums.UserRole;
import com.xinyu.InterviewCoach_v2.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 用户业务逻辑层
 */
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    /**
     * 创建新用户
     * 注意：只能创建普通用户(USER角色)，管理员需要通过数据库直接初始化
     */
    public UserDTO createUser(CreateUserRequest request) {
        // 检查邮箱是否已存在
        if (userMapper.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已存在");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        // 加密密码
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(UserRole.USER); // 强制设置为USER角色

        int result = userMapper.insert(user);
        if (result > 0) {
            return convertToDTO(user);
        } else {
            throw new RuntimeException("创建用户失败");
        }
    }

    /**
     * 根据ID查询用户
     */
    public Optional<UserDTO> getUserById(Long id) {
        return userMapper.findById(id).map(this::convertToDTO);
    }

    /**
     * 根据邮箱查询用户
     */
    public Optional<UserDTO> getUserByEmail(String email) {
        return userMapper.findByEmail(email).map(this::convertToDTO);
    }

    /**
     * 根据邮箱查询用户实体（用于认证）
     */
    public Optional<User> getUserEntityByEmail(String email) {
        return userMapper.findByEmail(email);
    }

    /**
     * 查询所有用户
     */
    public List<UserDTO> getAllUsers() {
        return userMapper.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 根据角色查询用户
     */
    public List<UserDTO> getUsersByRole(UserRole role) {
        return userMapper.findByRole(role).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * 更新用户信息
     * 注意：不允许通过API更改用户角色
     */
    public UserDTO updateUser(Long id, CreateUserRequest request) {
        Optional<User> existingUser = userMapper.findById(id);
        if (existingUser.isEmpty()) {
            throw new RuntimeException("用户不存在");
        }

        // 如果邮箱有变化，检查新邮箱是否已被其他用户使用
        if (!existingUser.get().getEmail().equals(request.getEmail())
                && userMapper.existsByEmail(request.getEmail())) {
            throw new RuntimeException("邮箱已被其他用户使用");
        }

        User user = existingUser.get();
        user.setEmail(request.getEmail());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            // 加密新密码
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        // 不允许更改角色，保持原有角色

        int result = userMapper.update(user);
        if (result > 0) {
            return convertToDTO(user);
        } else {
            throw new RuntimeException("更新用户失败");
        }
    }

    /**
     * 删除用户
     */
    public boolean deleteUser(Long id) {
        if (!userMapper.findById(id).isPresent()) {
            throw new RuntimeException("用户不存在");
        }
        return userMapper.deleteById(id) > 0;
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean emailExists(String email) {
        return userMapper.existsByEmail(email);
    }

    /**
     * 获取用户总数
     */
    public long getUserCount() {
        return userMapper.count();
    }

    /**
     * 用户登录验证（使用加密密码）
     */
    public Optional<User> authenticate(String email, String password) {
        Optional<User> user = userMapper.findByEmail(email);
        if (user.isPresent() && passwordEncoder.matches(password, user.get().getPassword())) {
            return user;
        }
        return Optional.empty();
    }

    /**
     * 将User实体转换为UserDTO
     */
    private UserDTO convertToDTO(User user) {
        return new UserDTO(
                user.getId(),
                user.getEmail(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}