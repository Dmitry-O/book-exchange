package com.example.bookexchange.user.specification;

import com.example.bookexchange.user.model.UserType;
import com.example.bookexchange.user.model.User;
import com.example.bookexchange.user.model.UserRole;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Set;

public class UserSpecificationBuilder {
    public static Specification<User> build(String searchText, Set<UserRole> roles, Boolean onlyBannedUsers, Boolean isUserSuperAdmin, UserType userType) {
        return (root, query, cb) -> {
            Predicate predicate = cb.conjunction();

            if (!isUserSuperAdmin) {
                Join<User, UserRole> rolesJoin = root.join("roles");
                predicate = cb.and(predicate, rolesJoin.in(Set.of(UserRole.USER)));
                query.distinct(true);
            } else if (roles != null && !roles.isEmpty()) {
                Join<User, UserRole> rolesJoin = root.join("roles");
                predicate = cb.and(predicate, rolesJoin.in(roles));
                query.distinct(true);
            }

            if (onlyBannedUsers != null && onlyBannedUsers) {
                Predicate bannedPermanentlyEqual = cb.equal(root.get("bannedPermanently"), true);
                Predicate bannedUntilGreaterThan = cb.greaterThan(root.get("bannedUntil"), Instant.now());
                predicate = cb.and(predicate, cb.or(bannedPermanentlyEqual, bannedUntilGreaterThan));
            }

            if (searchText != null && !searchText.isBlank()) {
                String like = "%" + searchText.toLowerCase() + "%";
                Predicate emailLike = cb.like(cb.lower(root.get("email")), like);
                Predicate nicknameLike = cb.like(cb.lower(root.get("nickname")), like);
                predicate = cb.and(predicate, cb.or(emailLike, nicknameLike));
            }

            if (userType != UserType.ALL) {
                predicate = cb.and(predicate, userType == UserType.ACTIVE ? cb.isNull(root.get("deletedAt")) : cb.isNotNull(root.get("deletedAt")));
            }

            return predicate;
        };
    }
}
