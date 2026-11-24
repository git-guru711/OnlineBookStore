package com.example.bookstore.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bookstore.entity.User;
import com.example.bookstore.model.Role;
import com.example.bookstore.support.RepositoryTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

@RepositoryTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void savesUser_andFindsByEmail() {
        User saved = userRepository.saveAndFlush(new User("jane@example.com", "$2a$10$hash", "Jane Doe"));
        entityManager.clear();

        User loaded = userRepository.findByEmail("jane@example.com").orElseThrow();

        assertThat(loaded.getId()).isEqualTo(saved.getId());
        assertThat(loaded.getPasswordHash()).isEqualTo("$2a$10$hash");
        assertThat(loaded.getFullName()).isEqualTo("Jane Doe");
        assertThat(loaded.getRole()).isEqualTo(Role.USER);
        assertThat(loaded.getCreatedAt()).isNotNull();
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void duplicateEmail_isRejected() {
        userRepository.saveAndFlush(new User("dup@example.com", "hash-1", "First"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(new User("dup@example.com", "hash-2", "Second")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
