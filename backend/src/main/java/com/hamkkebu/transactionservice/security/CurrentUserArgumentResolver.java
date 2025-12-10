package com.hamkkebu.transactionservice.security;

import com.hamkkebu.boilerplate.common.user.resolver.AbstractCurrentUserArgumentResolver;
import com.hamkkebu.transactionservice.data.entity.User;
import com.hamkkebu.transactionservice.repository.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Transaction Service @CurrentUser 어노테이션 ArgumentResolver
 *
 * <p>AbstractCurrentUserArgumentResolver를 상속받아 공통 로직을 재사용합니다.</p>
 */
@Component("transactionCurrentUserArgumentResolver")
public class CurrentUserArgumentResolver extends AbstractCurrentUserArgumentResolver<User> {

    public CurrentUserArgumentResolver(UserRepository userRepository) {
        super(userRepository);
    }
}
