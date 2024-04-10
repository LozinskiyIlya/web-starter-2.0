package com.starter.domain.repository.testdata;

import com.starter.domain.entity.Bill;
import com.starter.domain.entity.Group;
import com.starter.domain.entity.Role;
import com.starter.domain.entity.User;
import com.starter.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class GroupTestDataCreator implements UserTestData, GroupTestData, BillTestData {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;

    @Override
    public Group givenGroupExists(Consumer<Group> configure) {
        Consumer<Group> fullyConfigure = group -> {
            group.setUser(givenUserExists(u -> {
            }));
            configure.accept(group);
        };
        return GroupTestData.super.givenGroupExists(fullyConfigure);
    }

    @Override
    public Bill givenBillExists(Consumer<Bill> configure) {
        Consumer<Bill> fullyConfigure = bill -> {
            bill.setGroup(givenGroupExists(g -> {
            }));
            configure.accept(bill);
        };
        return BillTestData.super.givenBillExists(fullyConfigure);
    }

    @Override
    public Repository<Bill> billRepository() {
        return billRepository;
    }

    @Override
    public Repository<Group> groupRepository() {
        return groupRepository;
    }

    @Override
    public Repository<User> userRepository() {
        return userRepository;
    }

    @Override
    public Repository<Role> roleRepository() {
        return roleRepository;
    }
}
