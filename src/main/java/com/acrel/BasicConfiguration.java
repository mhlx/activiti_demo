package com.acrel;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
@EnableWebSecurity
public class BasicConfiguration extends WebSecurityConfigurerAdapter {

	@Bean
	public UserDetailsService myUserDetailsService() {

		InMemoryUserDetailsManager inMemoryUserDetailsManager = new InMemoryUserDetailsManager();

		String[][] usersGroupsAndRoles = { { "user", "1", "ROLE_ACTIVITI_USER" },
				{ "dev", "1", "ROLE_ACTIVITI_USER", "GROUP_dev-managers" },
				{ "dev2", "1", "ROLE_ACTIVITI_USER", "GROUP_dev-managers" },
				{ "dev3", "1", "ROLE_ACTIVITI_USER", "GROUP_dev-managers" },
				{ "devm", "1", "ROLE_ACTIVITI_USER", "GROUP_dev-management" },
				{ "finance", "1", "ROLE_ACTIVITI_USER", "GROUP_finance" } };

		for (String[] user : usersGroupsAndRoles) {
			List<String> authoritiesStrings = Arrays.asList(Arrays.copyOfRange(user, 2, user.length));
			inMemoryUserDetailsManager.createUser(new User(user[0], passwordEncoder().encode(user[1]),
					authoritiesStrings.stream().map(s -> new SimpleGrantedAuthority(s)).collect(Collectors.toList())));
		}

		return inMemoryUserDetailsManager;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}