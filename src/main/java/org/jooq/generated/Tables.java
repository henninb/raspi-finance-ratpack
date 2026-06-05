package org.jooq.generated;

import org.jooq.Field;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Stub JOOQ tables for compilation without a database connection.
 * Run ./gradlew generateJooq when the database is available to replace these with generated classes.
 */
public class Tables {

    public static final TAccount T_ACCOUNT = new TAccount();
    public static final TCategory T_CATEGORY = new TCategory();
    public static final TDescription T_DESCRIPTION = new TDescription();
    public static final TFamilyMember T_FAMILY_MEMBER = new TFamilyMember();
    public static final TMedicalExpense T_MEDICAL_EXPENSE = new TMedicalExpense();
    public static final TMedicalProvider T_MEDICAL_PROVIDER = new TMedicalProvider();
    public static final TParameter T_PARAMETER = new TParameter();
    public static final TPayment T_PAYMENT = new TPayment();
    public static final TPendingTransaction T_PENDING_TRANSACTION = new TPendingTransaction();
    public static final TTransaction T_TRANSACTION = new TTransaction();
    public static final TTransfer T_TRANSFER = new TTransfer();
    public static final TUser T_USER = new TUser();
    public static final TValidationAmount T_VALIDATION_AMOUNT = new TValidationAmount();

    public static class TAccount extends TableImpl<org.jooq.Record> {
        public final Field<Long> ACCOUNT_ID = createField(DSL.name("account_id"), SQLDataType.BIGINT);
        public final Field<String> ACCOUNT_NAME_OWNER = createField(DSL.name("account_name_owner"), SQLDataType.VARCHAR);
        public final Field<String> ACCOUNT_TYPE = createField(DSL.name("account_type"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);
        public final Field<String> MONIKER = createField(DSL.name("moniker"), SQLDataType.VARCHAR);
        public final Field<Boolean> PAYMENT_REQUIRED = createField(DSL.name("payment_required"), SQLDataType.BOOLEAN);

        public TAccount() { super(DSL.name("t_account")); }
    }

    public static class TCategory extends TableImpl<org.jooq.Record> {
        public final Field<String> CATEGORY_NAME = createField(DSL.name("category_name"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);

        public TCategory() { super(DSL.name("t_category")); }
    }

    public static class TDescription extends TableImpl<org.jooq.Record> {
        public final Field<String> DESCRIPTION_NAME = createField(DSL.name("description_name"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);

        public TDescription() { super(DSL.name("t_description")); }
    }

    public static class TFamilyMember extends TableImpl<org.jooq.Record> {
        public final Field<Long> FAMILY_MEMBER_ID = createField(DSL.name("family_member_id"), SQLDataType.BIGINT);
        public final Field<String> OWNER = createField(DSL.name("owner"), SQLDataType.VARCHAR);
        public final Field<String> MEMBER_NAME = createField(DSL.name("member_name"), SQLDataType.VARCHAR);
        public final Field<String> RELATIONSHIP = createField(DSL.name("relationship"), SQLDataType.VARCHAR);
        public final Field<LocalDate> DATE_OF_BIRTH = createField(DSL.name("date_of_birth"), SQLDataType.LOCALDATE);
        public final Field<String> INSURANCE_MEMBER_ID = createField(DSL.name("insurance_member_id"), SQLDataType.VARCHAR);
        public final Field<String> SSN_LAST_FOUR = createField(DSL.name("ssn_last_four"), SQLDataType.VARCHAR);
        public final Field<String> MEDICAL_RECORD_NUMBER = createField(DSL.name("medical_record_number"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);
        public final Field<LocalDateTime> DATE_ADDED = createField(DSL.name("date_added"), SQLDataType.LOCALDATETIME);
        public final Field<LocalDateTime> DATE_UPDATED = createField(DSL.name("date_updated"), SQLDataType.LOCALDATETIME);

        public TFamilyMember() { super(DSL.name("t_family_member")); }
    }

    public static class TMedicalExpense extends TableImpl<org.jooq.Record> {
        public final Field<Long> MEDICAL_EXPENSE_ID = createField(DSL.name("medical_expense_id"), SQLDataType.BIGINT);
        public final Field<Long> TRANSACTION_ID = createField(DSL.name("transaction_id"), SQLDataType.BIGINT);
        public final Field<Long> PROVIDER_ID = createField(DSL.name("provider_id"), SQLDataType.BIGINT);
        public final Field<Long> FAMILY_MEMBER_ID = createField(DSL.name("family_member_id"), SQLDataType.BIGINT);
        public final Field<LocalDate> SERVICE_DATE = createField(DSL.name("service_date"), SQLDataType.LOCALDATE);
        public final Field<String> SERVICE_DESCRIPTION = createField(DSL.name("service_description"), SQLDataType.VARCHAR);
        public final Field<String> PROCEDURE_CODE = createField(DSL.name("procedure_code"), SQLDataType.VARCHAR);
        public final Field<String> DIAGNOSIS_CODE = createField(DSL.name("diagnosis_code"), SQLDataType.VARCHAR);
        public final Field<BigDecimal> BILLED_AMOUNT = createField(DSL.name("billed_amount"), SQLDataType.DECIMAL);
        public final Field<BigDecimal> INSURANCE_DISCOUNT = createField(DSL.name("insurance_discount"), SQLDataType.DECIMAL);
        public final Field<BigDecimal> INSURANCE_PAID = createField(DSL.name("insurance_paid"), SQLDataType.DECIMAL);
        public final Field<BigDecimal> PATIENT_RESPONSIBILITY = createField(DSL.name("patient_responsibility"), SQLDataType.DECIMAL);
        public final Field<LocalDate> PAID_DATE = createField(DSL.name("paid_date"), SQLDataType.LOCALDATE);
        public final Field<Boolean> IS_OUT_OF_NETWORK = createField(DSL.name("is_out_of_network"), SQLDataType.BOOLEAN);
        public final Field<String> CLAIM_NUMBER = createField(DSL.name("claim_number"), SQLDataType.VARCHAR);
        public final Field<String> CLAIM_STATUS = createField(DSL.name("claim_status"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);
        public final Field<BigDecimal> PAID_AMOUNT = createField(DSL.name("paid_amount"), SQLDataType.DECIMAL);
        public final Field<String> OWNER = createField(DSL.name("owner"), SQLDataType.VARCHAR);
        public final Field<LocalDateTime> DATE_ADDED = createField(DSL.name("date_added"), SQLDataType.LOCALDATETIME);
        public final Field<LocalDateTime> DATE_UPDATED = createField(DSL.name("date_updated"), SQLDataType.LOCALDATETIME);

        public TMedicalExpense() { super(DSL.name("t_medical_expense")); }
    }

    public static class TMedicalProvider extends TableImpl<org.jooq.Record> {
        public final Field<Long> PROVIDER_ID = createField(DSL.name("provider_id"), SQLDataType.BIGINT);
        public final Field<String> PROVIDER_NAME = createField(DSL.name("provider_name"), SQLDataType.VARCHAR);
        public final Field<String> PROVIDER_TYPE = createField(DSL.name("provider_type"), SQLDataType.VARCHAR);
        public final Field<String> SPECIALTY = createField(DSL.name("specialty"), SQLDataType.VARCHAR);
        public final Field<String> NPI = createField(DSL.name("npi"), SQLDataType.VARCHAR);
        public final Field<String> TAX_ID = createField(DSL.name("tax_id"), SQLDataType.VARCHAR);
        public final Field<String> BILLING_NAME = createField(DSL.name("billing_name"), SQLDataType.VARCHAR);
        public final Field<String> ADDRESS_LINE = createField(DSL.name("address_line"), SQLDataType.VARCHAR);
        public final Field<String> CITY = createField(DSL.name("city"), SQLDataType.VARCHAR);
        public final Field<String> STATE = createField(DSL.name("state"), SQLDataType.VARCHAR);
        public final Field<String> ZIP_CODE = createField(DSL.name("zip_code"), SQLDataType.VARCHAR);
        public final Field<String> COUNTRY = createField(DSL.name("country"), SQLDataType.VARCHAR);
        public final Field<String> PHONE = createField(DSL.name("phone"), SQLDataType.VARCHAR);
        public final Field<String> FAX = createField(DSL.name("fax"), SQLDataType.VARCHAR);
        public final Field<String> EMAIL = createField(DSL.name("email"), SQLDataType.VARCHAR);
        public final Field<String> WEBSITE = createField(DSL.name("website"), SQLDataType.VARCHAR);
        public final Field<String> NETWORK_STATUS = createField(DSL.name("network_status"), SQLDataType.VARCHAR);
        public final Field<String> NOTES = createField(DSL.name("notes"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);
        public final Field<LocalDateTime> DATE_ADDED = createField(DSL.name("date_added"), SQLDataType.LOCALDATETIME);
        public final Field<LocalDateTime> DATE_UPDATED = createField(DSL.name("date_updated"), SQLDataType.LOCALDATETIME);

        public TMedicalProvider() { super(DSL.name("t_medical_provider")); }
    }

    public static class TParameter extends TableImpl<org.jooq.Record> {
        public final Field<String> PARAMETER_NAME = createField(DSL.name("parameter_name"), SQLDataType.VARCHAR);
        public final Field<String> PARAMETER_VALUE = createField(DSL.name("parameter_value"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);

        public TParameter() { super(DSL.name("t_parameter")); }
    }

    public static class TPayment extends TableImpl<org.jooq.Record> {
        public final Field<Long> PAYMENT_ID = createField(DSL.name("payment_id"), SQLDataType.BIGINT);
        public final Field<String> ACCOUNT_NAME_OWNER = createField(DSL.name("account_name_owner"), SQLDataType.VARCHAR);
        public final Field<BigDecimal> AMOUNT = createField(DSL.name("amount"), SQLDataType.DECIMAL);
        public final Field<java.sql.Date> TRANSACTION_DATE = createField(DSL.name("transaction_date"), SQLDataType.DATE);

        public TPayment() { super(DSL.name("t_payment")); }
    }

    public static class TPendingTransaction extends TableImpl<org.jooq.Record> {
        public final Field<Long> PENDING_TRANSACTION_ID = createField(DSL.name("pending_transaction_id"), SQLDataType.BIGINT);
        public final Field<BigDecimal> AMOUNT = createField(DSL.name("amount"), SQLDataType.DECIMAL);
        public final Field<String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.VARCHAR);
        public final Field<String> REVIEW_STATUS = createField(DSL.name("review_status"), SQLDataType.VARCHAR);
        public final Field<java.sql.Date> TRANSACTION_DATE = createField(DSL.name("transaction_date"), SQLDataType.DATE);

        public TPendingTransaction() { super(DSL.name("t_pending_transaction")); }
    }

    public static class TTransaction extends TableImpl<org.jooq.Record> {
        public final Field<Long> TRANSACTION_ID = createField(DSL.name("transaction_id"), SQLDataType.BIGINT);
        public final Field<String> GUID = createField(DSL.name("guid"), SQLDataType.VARCHAR);
        public final Field<Long> ACCOUNT_ID = createField(DSL.name("account_id"), SQLDataType.BIGINT);
        public final Field<String> ACCOUNT_NAME_OWNER = createField(DSL.name("account_name_owner"), SQLDataType.VARCHAR);
        public final Field<String> ACCOUNT_TYPE = createField(DSL.name("account_type"), SQLDataType.VARCHAR);
        public final Field<String> TRANSACTION_STATE = createField(DSL.name("transaction_state"), SQLDataType.VARCHAR);
        public final Field<java.sql.Date> TRANSACTION_DATE = createField(DSL.name("transaction_date"), SQLDataType.DATE);
        public final Field<BigDecimal> AMOUNT = createField(DSL.name("amount"), SQLDataType.DECIMAL);
        public final Field<String> CATEGORY = createField(DSL.name("category"), SQLDataType.VARCHAR);
        public final Field<String> DESCRIPTION = createField(DSL.name("description"), SQLDataType.VARCHAR);
        public final Field<String> NOTES = createField(DSL.name("notes"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);
        public final Field<String> REOCCURRING_TYPE = createField(DSL.name("reoccurring_type"), SQLDataType.VARCHAR);
        public final Field<String> TRANSACTION_TYPE = createField(DSL.name("transaction_type"), SQLDataType.VARCHAR);
        public final Field<java.sql.Timestamp> DATE_UPDATED = createField(DSL.name("date_updated"), SQLDataType.TIMESTAMP);
        public final Field<java.sql.Timestamp> DATE_ADDED = createField(DSL.name("date_added"), SQLDataType.TIMESTAMP);

        public TTransaction() { super(DSL.name("t_transaction")); }
    }

    public static class TTransfer extends TableImpl<org.jooq.Record> {
        public final Field<Long> TRANSFER_ID = createField(DSL.name("transfer_id"), SQLDataType.BIGINT);
        public final Field<String> SOURCE_ACCOUNT = createField(DSL.name("source_account"), SQLDataType.VARCHAR);
        public final Field<String> DESTINATION_ACCOUNT = createField(DSL.name("destination_account"), SQLDataType.VARCHAR);
        public final Field<BigDecimal> AMOUNT = createField(DSL.name("amount"), SQLDataType.DECIMAL);
        public final Field<java.sql.Date> TRANSACTION_DATE = createField(DSL.name("transaction_date"), SQLDataType.DATE);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);

        public TTransfer() { super(DSL.name("t_transfer")); }
    }

    public static class TUser extends TableImpl<org.jooq.Record> {
        public final Field<Long> USER_ID = createField(DSL.name("user_id"), SQLDataType.BIGINT);
        public final Field<String> USERNAME = createField(DSL.name("username"), SQLDataType.VARCHAR);
        public final Field<String> PASSWORD = createField(DSL.name("password"), SQLDataType.VARCHAR);
        public final Field<String> FIRST_NAME = createField(DSL.name("first_name"), SQLDataType.VARCHAR);
        public final Field<String> LAST_NAME = createField(DSL.name("last_name"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);
        public final Field<LocalDateTime> DATE_ADDED = createField(DSL.name("date_added"), SQLDataType.LOCALDATETIME);
        public final Field<LocalDateTime> DATE_UPDATED = createField(DSL.name("date_updated"), SQLDataType.LOCALDATETIME);

        public TUser() { super(DSL.name("t_user")); }
    }

    public static class TValidationAmount extends TableImpl<org.jooq.Record> {
        public final Field<Long> VALIDATION_ID = createField(DSL.name("validation_id"), SQLDataType.BIGINT);
        public final Field<Long> ACCOUNT_ID = createField(DSL.name("account_id"), SQLDataType.BIGINT);
        public final Field<BigDecimal> AMOUNT = createField(DSL.name("amount"), SQLDataType.DECIMAL);
        public final Field<String> TRANSACTION_STATE = createField(DSL.name("transaction_state"), SQLDataType.VARCHAR);
        public final Field<Boolean> ACTIVE_STATUS = createField(DSL.name("active_status"), SQLDataType.BOOLEAN);

        public TValidationAmount() { super(DSL.name("t_validation_amount")); }
    }
}
