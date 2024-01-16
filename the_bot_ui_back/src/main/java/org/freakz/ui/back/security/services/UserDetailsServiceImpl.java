package org.freakz.ui.back.security.services;


import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import org.freakz.common.model.users.GetUsersResponse;
import org.freakz.common.util.FeignUtils;
import org.freakz.ui.back.clients.EngineClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {


    @Autowired
    EngineClient engineClient;

    @Autowired
    ObjectMapper objectMapper;

    @Override
//  @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        Response response = engineClient.handleGetUsers();
        Optional<GetUsersResponse> responseBody = FeignUtils.getResponseBody(response, GetUsersResponse.class, objectMapper);
        if (responseBody.isPresent()) {
            GetUsersResponse getUsersResponse = responseBody.get();
            for (org.freakz.common.model.users.User user : getUsersResponse.getUsers()) {
                if (user.getUsername().equals(username)) {
                    return UserDetailsImpl.build(user);
                }
            }
        }
        throw new UsernameNotFoundException("User Not Found with username: " + username);
    }

}
