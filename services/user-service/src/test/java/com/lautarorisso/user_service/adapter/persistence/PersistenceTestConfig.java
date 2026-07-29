package com.lautarorisso.user_service.adapter.persistence;

import com.lautarorisso.user_service.adapter.out.persistence.TeamPersistenceAdapter;
import com.lautarorisso.user_service.adapter.out.persistence.UserPersistenceAdapter;
import com.lautarorisso.user_service.adapter.out.persistence.mapper.TeamEntityMapperImpl;
import com.lautarorisso.user_service.adapter.out.persistence.mapper.UserEntityMapperImpl;
import com.lautarorisso.user_service.adapter.out.persistence.repository.TeamJpaRepository;
import com.lautarorisso.user_service.adapter.out.persistence.repository.UserJpaRepository;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableAutoConfiguration
@EntityScan(basePackages = "com.lautarorisso.user_service.adapter.out.persistence.entity")
@EnableJpaRepositories(basePackages = "com.lautarorisso.user_service.adapter.out.persistence.repository")
public class PersistenceTestConfig {

    @Bean
    public UserPersistenceAdapter userPersistenceAdapter(
            UserJpaRepository userJpaRepository,
            UserEntityMapperImpl userEntityMapper) {
        return new UserPersistenceAdapter(userJpaRepository, userEntityMapper);
    }

    @Bean
    public TeamPersistenceAdapter teamPersistenceAdapter(
            TeamJpaRepository teamJpaRepository,
            TeamEntityMapperImpl teamEntityMapper) {
        return new TeamPersistenceAdapter(teamJpaRepository, teamEntityMapper);
    }

    @Bean
    public UserEntityMapperImpl userEntityMapper() {
        return new UserEntityMapperImpl();
    }

    @Bean
    public TeamEntityMapperImpl teamEntityMapper() {
        return new TeamEntityMapperImpl();
    }
}
