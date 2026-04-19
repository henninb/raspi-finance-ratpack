package finance.repositories

import com.google.inject.Inject
import finance.domain.Description
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_DESCRIPTION
import static org.jooq.generated.Tables.T_TRANSACTION

@Log
class DescriptionRepository {
    private final DSLContext dslContext

    @Inject
    DescriptionRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean descriptionInsert(Description description) {
        dslContext.newRecord(T_DESCRIPTION, description).store()
        return true
    }

    boolean descriptionUpdate(Description description) {
        dslContext.update(T_DESCRIPTION)
                .set(T_DESCRIPTION.ACTIVE_STATUS, description.activeStatus)
                .where(T_DESCRIPTION.DESCRIPTION_NAME.eq(description.descriptionName))
                .execute()
        return true
    }

    boolean descriptionsMerge(List<String> sourceNames, String targetName) {
        dslContext.transaction { config ->
            def ctx = DSL.using(config)
            sourceNames.each { sourceName ->
                ctx.update(T_TRANSACTION)
                        .set(T_TRANSACTION.DESCRIPTION, targetName)
                        .where(T_TRANSACTION.DESCRIPTION.eq(sourceName))
                        .execute()
                ctx.delete(T_DESCRIPTION)
                        .where(T_DESCRIPTION.DESCRIPTION_NAME.eq(sourceName))
                        .execute()
            }
        }
        return true
    }

    List<Description> descriptions() {
        return dslContext.selectFrom(T_DESCRIPTION)
                .where(T_DESCRIPTION.ACTIVE_STATUS.eq(true))
                .fetchInto(Description)
    }

    Description description(String descriptionName) {
        return dslContext.selectFrom(T_DESCRIPTION)
                .where(T_DESCRIPTION.DESCRIPTION_NAME.equal(descriptionName))
                .fetchOneInto(Description)
    }

    boolean descriptionDelete(String descriptionName) {
        dslContext.delete(T_DESCRIPTION)
                .where(T_DESCRIPTION.DESCRIPTION_NAME.equal(descriptionName))
                .execute()
        return true
    }
}
