package finance.repositories

import com.google.inject.Inject
import finance.domain.User
import groovy.util.logging.Slf4j
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource
import java.time.LocalDateTime

import static org.jooq.generated.Tables.T_USER

@Slf4j
class UserRepository {
    private final DSLContext dslContext

    @Inject
    UserRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    User findByUsername(String username) {
        return dslContext.selectFrom(T_USER)
                .where(T_USER.USERNAME.eq(username.toLowerCase()))
                .fetchOneInto(User)
    }

    boolean existsByUsername(String username) {
        return dslContext.fetchCount(T_USER, T_USER.USERNAME.eq(username.toLowerCase())) > 0
    }

    boolean saveUser(User user) {
        LocalDateTime now = LocalDateTime.now()
        dslContext.insertInto(T_USER)
                .set(T_USER.USERNAME, user.username.toLowerCase())
                .set(T_USER.PASSWORD, user.password)
                .set(T_USER.FIRST_NAME, user.firstName ?: "none")
                .set(T_USER.LAST_NAME, user.lastName ?: "none")
                .set(T_USER.ACTIVE_STATUS, true)
                .set(T_USER.DATE_ADDED, now)
                .set(T_USER.DATE_UPDATED, now)
                .execute()
        return true
    }
}
