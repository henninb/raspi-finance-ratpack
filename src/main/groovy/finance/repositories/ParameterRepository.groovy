package finance.repositories

import com.google.inject.Inject
import finance.domain.Parameter
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_PARAMETER

@Log
class ParameterRepository {
    private final DSLContext dslContext

    @Inject
    ParameterRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    boolean parameterInsert(Parameter parameter) {
        dslContext.newRecord(T_PARAMETER, parameter).store()
        return true
    }

    boolean parameterUpdate(Parameter parameter) {
        dslContext.update(T_PARAMETER)
                .set(T_PARAMETER.PARAMETER_VALUE, parameter.parameterValue)
                .set(T_PARAMETER.ACTIVE_STATUS, parameter.activeStatus)
                .where(T_PARAMETER.PARAMETER_NAME.eq(parameter.parameterName))
                .execute()
        return true
    }

    List<Parameter> parameters() {
        return dslContext.selectFrom(T_PARAMETER)
                .where(T_PARAMETER.ACTIVE_STATUS.eq(true))
                .orderBy(T_PARAMETER.PARAMETER_NAME)
                .fetchInto(Parameter)
    }

    Parameter parameter(String parameterName) {
        return dslContext.selectFrom(T_PARAMETER)
                .where(T_PARAMETER.PARAMETER_NAME.equal(parameterName))
                .fetchOneInto(Parameter)
    }

    boolean parameterDelete(String parameterName) {
        dslContext.delete(T_PARAMETER)
                .where(T_PARAMETER.PARAMETER_NAME.equal(parameterName))
                .execute()
        return true
    }
}
