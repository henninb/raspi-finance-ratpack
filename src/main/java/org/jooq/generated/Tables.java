package org.jooq.generated;

import org.jooq.*;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;

/**
 * Enhanced stub class for JOOQ generated tables to allow compilation without database connection
 * Run ./gradlew generateJooq when database is available to generate actual tables
 */
public class Tables {
    
    public static final TAccount T_ACCOUNT = new TAccount();
    public static final TCategory T_CATEGORY = new TCategory();
    public static final TDescription T_DESCRIPTION = new TDescription();
    public static final TParameter T_PARAMETER = new TParameter();
    public static final TPayment T_PAYMENT = new TPayment();
    public static final TTransaction T_TRANSACTION = new TTransaction();
    public static final TValidationAmount T_VALIDATION_AMOUNT = new TValidationAmount();
    
    public static class TAccount extends TableImpl<org.jooq.Record> {
        public final Field<Long> ACCOUNT_ID = createField(DSL.name("account_id"), SQLDataType.BIGINT);
        public final Field<String> ACCOUNT_NAME_OWNER = createField(DSL.name("account_name_owner"), SQLDataType.VARCHAR);
        
        public TAccount() {
            super(DSL.name("t_account"));
        }
    }
    
    public static class TCategory extends TableImpl<org.jooq.Record> {
        public final Field<String> CATEGORY_NAME = createField(DSL.name("category_name"), SQLDataType.VARCHAR);
        
        public TCategory() {
            super(DSL.name("t_category"));
        }
    }
    
    public static class TDescription extends TableImpl<org.jooq.Record> {
        public final Field<String> DESCRIPTION_NAME = createField(DSL.name("description_name"), SQLDataType.VARCHAR);
        
        public TDescription() {
            super(DSL.name("t_description"));
        }
    }
    
    public static class TParameter extends TableImpl<org.jooq.Record> {
        public final Field<String> PARAMETER_NAME = createField(DSL.name("parameter_name"), SQLDataType.VARCHAR);
        public final Field<String> PARAMETER_VALUE = createField(DSL.name("parameter_value"), SQLDataType.VARCHAR);
        
        public TParameter() {
            super(DSL.name("t_parameter"));
        }
    }
    
    public static class TPayment extends TableImpl<org.jooq.Record> {
        public final Field<Long> PAYMENT_ID = createField(DSL.name("payment_id"), SQLDataType.BIGINT);
        public final Field<String> ACCOUNT_NAME_OWNER = createField(DSL.name("account_name_owner"), SQLDataType.VARCHAR);
        
        public TPayment() {
            super(DSL.name("t_payment"));
        }
    }
    
    public static class TTransaction extends TableImpl<org.jooq.Record> {
        public final Field<String> GUID = createField(DSL.name("guid"), SQLDataType.VARCHAR);
        public final Field<Long> ACCOUNT_ID = createField(DSL.name("account_id"), SQLDataType.BIGINT);
        public final Field<String> ACCOUNT_NAME_OWNER = createField(DSL.name("account_name_owner"), SQLDataType.VARCHAR);
        public final Field<String> TRANSACTION_STATE = createField(DSL.name("transaction_state"), SQLDataType.VARCHAR);
        public final Field<java.sql.Date> TRANSACTION_DATE = createField(DSL.name("transaction_date"), SQLDataType.DATE);
        
        public TTransaction() {
            super(DSL.name("t_transaction"));
        }
    }
    
    public static class TValidationAmount extends TableImpl<org.jooq.Record> {
        public final Field<Long> ACCOUNT_ID = createField(DSL.name("account_id"), SQLDataType.BIGINT);
        
        public TValidationAmount() {
            super(DSL.name("t_validation_amount"));
        }
    }
}