package finance.repositories

import com.google.inject.Inject
import finance.domain.Parameter
import finance.domain.Payment
import groovy.transform.CompileStatic
import groovy.util.logging.Log
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL

import javax.sql.DataSource

import static org.jooq.generated.Tables.T_PARAMETER
import static org.jooq.generated.Tables.T_PAYMENT

@Log
class ParameterRepository {
    private final DSLContext dslContext

    @Inject
    ParameterRepository(DataSource dataSource) {
        this.dslContext = DSL.using(dataSource, SQLDialect.POSTGRES)
    }

    Parameter parameter(String parameterName) {
        return dslContext.selectFrom(T_PARAMETER).where(T_PARAMETER.PARAMETER_NAME.equal(parameterName)).fetchOneInto(Parameter)
    }
}