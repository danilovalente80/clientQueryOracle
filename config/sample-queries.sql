-- Sample SQL Queries for Oracle Query Client
-- These are examples to demonstrate the application functionality

-- ============================================
-- INSERT Examples
-- ============================================

-- Insert a single record
INSERT INTO employees (employee_id, first_name, last_name, email, hire_date, job_id, salary)
VALUES (1001, 'John', 'Doe', 'john.doe@example.com', SYSDATE, 'IT_PROG', 5000);

-- Insert with sequence
INSERT INTO audit_log (log_id, action, action_date, user_name)
VALUES (audit_seq.NEXTVAL, 'User Login', SYSDATE, USER);

-- Insert multiple records (Oracle 21c multi-row insert)
INSERT ALL
  INTO temp_data (id, description) VALUES (1, 'Record 1')
  INTO temp_data (id, description) VALUES (2, 'Record 2')
  INTO temp_data (id, description) VALUES (3, 'Record 3')
SELECT * FROM DUAL;

-- ============================================
-- UPDATE Examples
-- ============================================

-- Update with WHERE clause
UPDATE employees
SET salary = salary * 1.10
WHERE department_id = 60 AND hire_date < ADD_MONTHS(SYSDATE, -12);

-- Update with subquery
UPDATE employees e
SET salary = (SELECT AVG(salary) FROM employees WHERE department_id = e.department_id)
WHERE employee_id = 1001;

-- Update multiple columns
UPDATE employees
SET salary = 6000,
    commission_pct = 0.10,
    last_updated = SYSDATE
WHERE employee_id = 1001;

-- Update with CASE
UPDATE employees
SET salary = CASE
    WHEN department_id = 10 THEN salary * 1.15
    WHEN department_id = 20 THEN salary * 1.10
    ELSE salary * 1.05
END
WHERE hire_date < ADD_MONTHS(SYSDATE, -24);

-- ============================================
-- DELETE Examples
-- ============================================

-- Delete with simple WHERE
DELETE FROM temp_data
WHERE created_date < SYSDATE - 30;

-- Delete with subquery
DELETE FROM employees
WHERE department_id IN (SELECT department_id FROM departments WHERE location_id = 1700);

-- Delete old audit records
DELETE FROM audit_log
WHERE action_date < ADD_MONTHS(SYSDATE, -6);

-- Delete with EXISTS
DELETE FROM orders o
WHERE NOT EXISTS (
    SELECT 1 FROM customers c WHERE c.customer_id = o.customer_id
);

-- ============================================
-- Complex Examples with Joins and Subqueries
-- ============================================

-- Update based on join condition
UPDATE employees e
SET salary = salary * 1.10
WHERE EXISTS (
    SELECT 1
    FROM departments d
    WHERE d.department_id = e.department_id
    AND d.location_id = 1700
);

-- Delete with complex conditions
DELETE FROM order_items oi
WHERE oi.order_id IN (
    SELECT o.order_id
    FROM orders o
    WHERE o.order_date < ADD_MONTHS(SYSDATE, -12)
    AND o.order_status = 'COMPLETED'
);

-- ============================================
-- Testing Different Aliases
-- ============================================

-- To test different database aliases, you can use the same query
-- and just change the alias in the frontend:

-- For entr_asp database:
UPDATE entr_asp_specific_table
SET status = 'PROCESSED'
WHERE process_date = TRUNC(SYSDATE);

-- For sesamo database:
INSERT INTO sesamo_audit (action, timestamp)
VALUES ('Data Import', SYSTIMESTAMP);

-- For test/development databases:
DELETE FROM test_data WHERE 1=1;  -- Clears all test data

-- ============================================
-- Queries to Test Transaction Management
-- ============================================

-- Test 1: Update then check affected rows before commit
UPDATE employees SET salary = salary + 100 WHERE department_id = 60;
-- Check affected rows in UI
-- Then ROLLBACK to undo changes

-- Test 2: Multiple operations in same transaction
INSERT INTO audit_log (log_id, action, action_date) VALUES (1, 'Test', SYSDATE);
UPDATE employees SET last_updated = SYSDATE WHERE employee_id = 1001;
DELETE FROM temp_data WHERE id = 999;
-- All will be committed or rolled back together

-- ============================================
-- Safety Examples (Good Practices)
-- ============================================

-- Always use WHERE clause to prevent accidental mass updates
UPDATE employees SET salary = 10000 WHERE employee_id = 1001;  -- GOOD
-- UPDATE employees SET salary = 10000;  -- BAD (affects all rows!)

-- Use ROWNUM or FETCH FIRST for limited deletes
DELETE FROM audit_log
WHERE log_id IN (
    SELECT log_id FROM audit_log
    WHERE action_date < SYSDATE - 365
    FETCH FIRST 1000 ROWS ONLY
);

-- ============================================
-- Error Testing Examples
-- ============================================

-- Test constraint violation (should show error)
INSERT INTO employees (employee_id) VALUES (NULL);  -- Will fail if employee_id is NOT NULL

-- Test foreign key violation
INSERT INTO employees (employee_id, department_id) VALUES (9999, 99999);  -- Will fail if dept doesn't exist

-- Test invalid column name (should show clear error)
UPDATE employees SET invalid_column = 'test' WHERE employee_id = 1;

-- ============================================
-- NOTES FOR USERS
-- ============================================

/*
1. Always test queries on development/test databases first
2. Always review the "affected rows" count before committing
3. For critical updates, first run a SELECT with the same WHERE clause
4. Keep your queries focused - update/delete small batches at a time
5. Document the reason for each query execution
6. Be especially careful with DELETE and UPDATE without WHERE clauses
7. Use transactions wisely - commit frequently for large operations
8. Remember: ROLLBACK undoes all changes since the last COMMIT

TRANSACTION FLOW:
1. Execute Query → See affected rows
2. Review the impact
3. Decision:
   - COMMIT → Changes become permanent
   - ROLLBACK → Changes are discarded

SECURITY REMINDER:
- Only authorized personnel should use this tool
- All operations are logged
- Never share database credentials
- Follow your organization's change management procedures
*/
