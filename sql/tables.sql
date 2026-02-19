
1. CLEANUP (Optional: Run these if you want a fresh start)
 ==========================================
DROP TABLE job_metrics;
DROP TABLE jobs;
DROP TABLE users;
DROP SEQUENCE users_seq;
DROP SEQUENCE jobs_seq;

2. CREATE SEQUENCES
==========================================
CREATE SEQUENCE users_seq START WITH 1 INCREMENT BY 1;
CREATE SEQUENCE jobs_seq START WITH 1 INCREMENT BY 1;


3. CREATE TABLES
==========================================

//USERS TABLE
CREATE TABLE users (
    id NUMBER PRIMARY KEY,
    name VARCHAR2(100) NOT NULL,
    email VARCHAR2(100) NOT NULL UNIQUE
);

//JOBS TABLE
CREATE TABLE jobs (
    id NUMBER PRIMARY KEY,
    user_id NUMBER NOT NULL,
    status VARCHAR2(20) DEFAULT 'PENDING',
    retry_count NUMBER DEFAULT 0,
    email_sent NUMBER(1) DEFAULT 0,( 0 = No, 1 = Yes)
    created_at TIMESTAMP DEFAULT SYSTIMESTAMP,
    processing_started_at TIMESTAMP,
    completed_at TIMESTAMP,
    updated_at TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);

//JOB METRICS TABLE (Dashboard)
CREATE TABLE job_metrics (
    id NUMBER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    job_id NUMBER,
    status VARCHAR2(20),
    processed_at TIMESTAMP DEFAULT SYSTIMESTAMP,
    processing_time_seconds NUMBER(10,3),
    retry_attempt NUMBER
);


 4. CREATE TRIGGERS (The Automation)
==========================================

//Trigger to auto-fill USER ID
CREATE OR REPLACE TRIGGER trg_users_id
BEFORE INSERT ON users
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT users_seq.NEXTVAL INTO :NEW.id FROM dual;
  END IF;
END;
/

//Trigger to auto-fill JOB ID
CREATE OR REPLACE TRIGGER trg_jobs_id
BEFORE INSERT ON jobs
FOR EACH ROW
BEGIN
  IF :NEW.id IS NULL THEN
    SELECT jobs_seq.NEXTVAL INTO :NEW.id FROM dual;
  END IF;
END;
/

//Trigger to auto-update 'updated_at' whenever a job changes
CREATE OR REPLACE TRIGGER trg_jobs_updated_at
BEFORE UPDATE ON jobs
FOR EACH ROW
BEGIN
   :NEW.updated_at := SYSTIMESTAMP;
END;
/