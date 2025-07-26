package finance.repositories

import finance.domain.Category
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import ratpack.exec.Blocking
import ratpack.exec.Operation
import com.google.inject.Inject
import javax.sql.DataSource

import static org.jooq.generated.Tables.T_ACCOUNT
import static org.jooq.generated.Tables.T_CATEGORY

@Log
class CategoryRepository {
    private final DSLContext dslContext

    @Inject
    CategoryRepository(DataSource ds) {
        this.dslContext = DSL.using(ds, SQLDialect.POSTGRES)
    }

    boolean categoryInsert(Category category) {
        dslContext.newRecord(T_CATEGORY, category).store()
        return true
    }

    List<Category> categories() {
        return dslContext.selectFrom(T_CATEGORY).where(T_CATEGORY.ACTIVE_STATUS.eq(true))
                .orderBy(T_CATEGORY.CATEGORY_NAME.asc())
                .fetchInto(Category)
    }

    Category category(String categoryName) {
        return dslContext.selectFrom(T_CATEGORY).where(T_CATEGORY.CATEGORY_NAME.equal(categoryName)).fetchOneInto(Category)
    }

    boolean categoryDelete(String categoryName) {
        dslContext.delete(T_CATEGORY)
                .where(T_CATEGORY.CATEGORY_NAME.equal(categoryName))
                .execute()
        return true
    }
}