package finance.repositories

import com.google.inject.Inject
import finance.domain.Description
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_DESCRIPTION

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

    List<Description> descriptions() {
        return dslContext.selectFrom(T_DESCRIPTION).where(T_DESCRIPTION.ACTIVE_STATUS.endsWith(true)).fetchInto(Description)
    }

    Description description(String descriptionName) {
        return dslContext.selectFrom(T_DESCRIPTION).where(T_DESCRIPTION.DESCRIPTION_NAME.equal(descriptionName)).fetchOneInto(Description)
    }
}