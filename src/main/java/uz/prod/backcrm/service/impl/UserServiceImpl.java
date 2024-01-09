package uz.prod.backcrm.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import uz.prod.backcrm.entity.Role;
import uz.prod.backcrm.entity.User;
import uz.prod.backcrm.exceptions.RestException;
import uz.prod.backcrm.manual.ApiResult;
import uz.prod.backcrm.mapper.UserMapper;
import uz.prod.backcrm.payload.ProfileDTO;
import uz.prod.backcrm.payload.UserDTO;
import uz.prod.backcrm.repository.RoleRepository;
import uz.prod.backcrm.repository.UserRepository;
import uz.prod.backcrm.service.abs.UserService;
import uz.prod.backcrm.utills.CommonUtils;
import uz.prod.backcrm.utills.constants.Message;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final MessageSource messageSource;

    @Override
    public ApiResult<UserDTO> me(){
        return ApiResult.success(userMapper.toDTO(CommonUtils.getUserFromSecurityContext()));
    }

    @Override
    public ApiResult<List<UserDTO>> getAllUsersOrAdmins(boolean areUsers, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (areUsers)
            return ApiResult.success(userMapper.toDTOList(userRepository.findAllUsers(pageable)));
        else
            return ApiResult.success(userMapper.toDTOList(userRepository.findAllAdmins(pageable)));
    }

    @Override
    public ApiResult<List<UserDTO>> getBlockedUsersOrAdmins(boolean areUsers, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (areUsers)
            return ApiResult.success(userMapper.toDTOList(userRepository.findAllByAccountNonLockedIsFalseAndRoleIdNot(pageable, 1L)));
        else
            return ApiResult.success(userMapper.toDTOList(userRepository.findAllByAccountNonLockedIsFalseAndRoleId(pageable, 1L)));
    }

    @Override
    public ApiResult<List<UserDTO>> getActiveUsersOrAdmins(boolean areUsers, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        if (areUsers)
            return ApiResult.success(userMapper.toDTOList(userRepository.findAllByAccountNonLockedIsTrueAndRoleIdNot(pageable, 1L)));
        else
            return ApiResult.success(userMapper.toDTOList(userRepository.findAllByAccountNonLockedIsTrueAndRoleId(pageable, 1L)));
    }

    @Override
    public ApiResult<?> blockUser(UUID id) {
        String message = CommonUtils.createMessage(Message.NOT_FOUND, messageSource, new Object[]{id});
        User me = CommonUtils.getUserFromSecurityContext();
        User user = userRepository.findById(id).orElseThrow(
                () -> RestException.restThrow(message, HttpStatus.BAD_REQUEST));
        if (user.getRole().getId() == 1) {
            assert me != null;
            if (user.getCreatedAt().before(me.getCreatedAt())) {
                throw RestException.restThrow(CommonUtils.createMessage(Message.CANNOT_BLOCK,
                        messageSource, null), HttpStatus.CONFLICT);
            }
        }
        user.setAccountNonLocked(false);
        userRepository.save(user);
        return ApiResult.success(CommonUtils.createMessage(Message.BLOCKED_SUCCESSFULLY, messageSource, new Object[]{id}));
    }

    @Override
    public ApiResult<?> unblockUser(UUID id) {
        String message = CommonUtils.createMessage(Message.NOT_FOUND, messageSource, new Object[]{id});
        User user = userRepository.findById(id).orElseThrow(
                () -> RestException.restThrow(message, HttpStatus.BAD_REQUEST));
        user.setAccountNonLocked(true);
        userRepository.save(user);
        return ApiResult.success(CommonUtils.createMessage(Message.ACTIVATED_SUCCESSFULLY, messageSource, new Object[]{id}));
    }

    @Override
    public ApiResult<?> editProfile(ProfileDTO dto) {
        User user = CommonUtils.getUserFromSecurityContext();
        assert user != null;
        user.setUsername(dto.getUsername());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        userRepository.save(user);
        return ApiResult.success(CommonUtils.createMessage(Message.UPDATED_SUCCESSFULLY, messageSource, new Object[]{dto}));
    }

    @Override
    public ApiResult<?> editByAdmin(UUID id, Long roleId) {
        String message = CommonUtils.createMessage(Message.NOT_FOUND, messageSource, new Object[]{id});
        User user = userRepository.findById(id).orElseThrow(
                () -> RestException.restThrow(message, HttpStatus.BAD_REQUEST));
        User me = CommonUtils.getUserFromSecurityContext();
        if (user.getRole().getId() == 1) {
            assert me != null;
            if (user.getCreatedAt().before(me.getCreatedAt())) {
                throw RestException.restThrow(CommonUtils.createMessage(Message.CANNOT_EDIT,
                        messageSource, null), HttpStatus.CONFLICT);
            }
        }
        Role role = roleRepository.findById(roleId).orElseThrow(
                () -> RestException.restThrow(message, HttpStatus.BAD_REQUEST));
        user.setRole(role);
        userRepository.save(user);
        return ApiResult.success(CommonUtils.createMessage(Message.UPDATED_SUCCESSFULLY, messageSource, new Object[]{id}));
    }

    @Override
    public ApiResult<?> deleteProfile() {
        User user = CommonUtils.getUserFromSecurityContext();
        try {
            assert user != null;
            userRepository.deleteById(user.getId());
            return ApiResult.success(CommonUtils.createMessage(Message.DELETED_SUCCESSFULLY,
                    messageSource, new Object[]{null}));
        } catch (Exception e) {
            throw RestException.restThrow(CommonUtils.createMessage(Message.DELETE_FAILED,
                    messageSource, new Object[]{null}), HttpStatus.CONFLICT);
        }
    }


}
