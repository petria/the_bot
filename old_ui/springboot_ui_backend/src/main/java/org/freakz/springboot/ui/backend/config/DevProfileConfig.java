package org.freakz.springboot.ui.backend.config;

import java.util.HashSet;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.freakz.springboot.ui.backend.models.ERole;
import org.freakz.springboot.ui.backend.models.Role;
import org.freakz.springboot.ui.backend.models.User;
import org.freakz.springboot.ui.backend.repository.RoleRepository;
import org.freakz.springboot.ui.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
@Slf4j
public class DevProfileConfig implements CommandLineRunner {

    @Autowired
    private PasswordEncoder encoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        log.debug("Start population of MEM DB ");
        populateDevDb();
        log.debug("Population of MEM DB DONE!");
    }

    @Transactional
    void populateDevDb() {
        Role role1 = new Role();
        role1.setName(ERole.ROLE_ADMIN);

        Role role2 = new Role();
        role2.setName(ERole.ROLE_USER);

        Role role3 = new Role();
        role3.setName(ERole.ROLE_MODERATOR);

        role1 = roleRepository.save(role1);
        role2 = roleRepository.save(role2);
        role3 = roleRepository.save(role3);

        log.debug("Roles added!");

        Set<Role> roles = new HashSet<>();
        roles.add(role1);
        roles.add(role2);
        roles.add(role3);

        User user = new User();
        user.setUsername("admin");
        user.setEmail("admin@the_bot.invalid");
        user.setPassword(encoder.encode("admin"));
        user.setRoles(roles);

        user = userRepository.save(user);

        log.debug("Created admin user: {}", user);

        Set<Role> roles2 = new HashSet<>();
        roles2.add(role2);

        User user2 = new User();
        user2.setUsername("user");
        user2.setEmail("user@the_bot.invalid");
        user2.setPassword(encoder.encode("user"));
        user2.setRoles(roles2);

        user2 = userRepository.save(user2);

        log.debug("Created normal user: {}", user2);

    }
}
