package finance.repositories

import com.google.inject.Inject
import finance.domain.Category
import groovy.util.logging.Slf4j
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_CATEGORY
import static org.jooq.generated.Tables.T_TRANSACTION

@Slf4j
class CategoryRepository {
    private final DSLContext dslContext

    @Inject
    CategoryRepository(DataSource ds) {
        this.dslContext = DSL.using(ds, SQLDialect.POSTGRES)
    }

    boolean categoryInsert(Category category) {
        dslContext.insertInto(T_CATEGORY)
                .set(T_CATEGORY.OWNER, category.owner ?: "")
                .set(T_CATEGORY.CATEGORY_NAME, category.categoryName)
                .set(T_CATEGORY.ACTIVE_STATUS, category.activeStatus)
                .execute()
        return true
    }

    boolean categoryUpdate(Category category) {
        dslContext.update(T_CATEGORY)
                .set(T_CATEGORY.ACTIVE_STATUS, category.activeStatus)
                .where(T_CATEGORY.CATEGORY_NAME.eq(category.categoryName))
                .execute()
        return true
    }

    boolean categoryMerge(String oldCategoryName, String newCategoryName) {
        dslContext.transaction { config ->
            def ctx = DSL.using(config)
            ctx.update(T_TRANSACTION)
                    .set(T_TRANSACTION.CATEGORY, newCategoryName)
                    .where(T_TRANSACTION.CATEGORY.eq(oldCategoryName))
                    .execute()
            ctx.delete(T_CATEGORY)
                    .where(T_CATEGORY.CATEGORY_NAME.eq(oldCategoryName))
                    .execute()
        }
        return true
    }

    List<Category> categories() {
        return dslContext.selectFrom(T_CATEGORY)
                .where(T_CATEGORY.ACTIVE_STATUS.eq(true))
                .orderBy(T_CATEGORY.CATEGORY_NAME.asc())
                .fetchInto(Category)
    }

    Category category(String categoryName) {
        return dslContext.selectFrom(T_CATEGORY)
                .where(T_CATEGORY.CATEGORY_NAME.equal(categoryName))
                .limit(1)
                .fetchOneInto(Category)
    }

    Category category(String owner, String categoryName) {
        return dslContext.selectFrom(T_CATEGORY)
                .where(T_CATEGORY.OWNER.equal(owner).and(T_CATEGORY.CATEGORY_NAME.equal(categoryName)))
                .fetchOneInto(Category)
    }

    boolean categoryDelete(String categoryName) {
        dslContext.delete(T_CATEGORY)
                .where(T_CATEGORY.CATEGORY_NAME.equal(categoryName))
                .execute()
        return true
    }
}
