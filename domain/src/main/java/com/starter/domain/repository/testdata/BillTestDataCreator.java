package com.starter.domain.repository.testdata;

import com.starter.domain.entity.*;
import com.starter.domain.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class BillTestDataCreator implements UserTestData, GroupTestData, BillTestData, BillTagTestData {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final GroupRepository groupRepository;
    private final BillRepository billRepository;
    private final BillTagRepository billTagRepository;

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
            bill.setTags(Set.of(givenBillTagExists(t -> {
            })));
            configure.accept(bill);
        };
        return BillTestData.super.givenBillExists(fullyConfigure);
    }

    @Override
    public BillTag givenBillTagExists(Consumer<BillTag> configure) {
        Consumer<BillTag> fullyConfigure = tag -> {
            tag.setUser(givenUserExists(u -> {
            }));
            configure.accept(tag);
        };
        return BillTagTestData.super.givenBillTagExists(fullyConfigure);
    }

    @Override
    public Repository<BillTag> billTagRepository() {
        return billTagRepository;
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
