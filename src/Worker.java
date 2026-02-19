import java.io.FileInputStream;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.mail.*;
import jakarta.mail.internet.*;

public class Worker {

    private static final Logger logger = Logger.getLogger(Worker.class.getName());
    private static Properties config;

    static {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            config = new Properties();
            config.load(fis);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to load config.properties", e);
            System.exit(1);
        }
    }

    private static void recordMetric(Connection conn, int jobId, String status, long startTime, int retryAttempt) {
        String sql = "INSERT INTO job_metrics (job_id, status, processing_time_seconds, retry_attempt) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            double duration = (System.currentTimeMillis() - startTime) / 1000.0;
            ps.setInt(1, jobId);
            ps.setString(2, status);
            ps.setDouble(3, duration);
            ps.setInt(4, retryAttempt);
            ps.executeUpdate();
            conn.commit(); 
        } catch (SQLException e) {
            logger.warning("Failed to log metric for job " + jobId + ": " + e.getMessage());
        }
    }

    public static void sendEmail(String to, String subject, String body) throws MessagingException {
        final String username = config.getProperty("email.username");
        final String password = config.getProperty("email.password");
        final String host = config.getProperty("smtp.host");
        final String port = config.getProperty("smtp.port");

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new jakarta.mail.Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(username));
        
        // ⚡ This will throw an AddressException if the email is "wrong" (e.g., no @)
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
        
        message.setSubject(subject);
        message.setText(body);

        Transport.send(message);
        logger.info("Email sent to " + to);
    }

    public static void processJobs(int retryLimit, int processingTimeoutSeconds) {
        try (Connection con = DBConnection.getConnection()) {
            con.setAutoCommit(false);

            String fetchJobs =
                    "SELECT * FROM (" +
                    "  SELECT j.id AS job_id, u.name, u.email, j.retry_count, j.email_sent, j.processing_started_at " +
                    "  FROM jobs j JOIN users u ON j.user_id = u.id " +
                    "  WHERE j.status = 'PENDING' " +
                    "     OR (j.status = 'PROCESSING' AND j.processing_started_at < SYSTIMESTAMP - NUMTODSINTERVAL(?, 'SECOND'))" +
                    ") WHERE ROWNUM <= 1 FOR UPDATE SKIP LOCKED";

            try (PreparedStatement psFetch = con.prepareStatement(fetchJobs)) {
                psFetch.setInt(1, processingTimeoutSeconds);
                ResultSet rs = psFetch.executeQuery();

                if (rs.next()) {
                    int jobId = rs.getInt("job_id");
                    String name = rs.getString("name");
                    String email = rs.getString("email");
                    int retryCount = rs.getInt("retry_count");
                    int emailSent = rs.getInt("email_sent");

                    long startTime = System.currentTimeMillis();

                    logger.info("➡ Processing job " + jobId + " (retry " + retryCount + ")");

                    // 1. CLAIM JOB
                    try (PreparedStatement psProcessing = con.prepareStatement(
                            "UPDATE jobs SET status='PROCESSING', processing_started_at=SYSTIMESTAMP WHERE id=?")) {
                        psProcessing.setInt(1, jobId);
                        psProcessing.executeUpdate();
                        con.commit();
                    }

                    try {
// To Handle Retry Cases

                       /*if (jobId == 20) { // Replace with an ID you have in your DB

    throw new RuntimeException("Simulated SMTP Server Down!");

}*/
                        // 2. SEND EMAIL
                        if (emailSent == 0) {
                            sendEmail(email, "Welcome", "Hi " + name);
                            try (PreparedStatement psEmailSent = con.prepareStatement(
                                    "UPDATE jobs SET email_sent=1 WHERE id=?")) {
                                psEmailSent.setInt(1, jobId);
                                psEmailSent.executeUpdate();
                                con.commit();
                            }
                        }

                        // 3. MARK AS COMPLETED
                        try (PreparedStatement psComp = con.prepareStatement(
                                "UPDATE jobs SET status='COMPLETED', completed_at=SYSTIMESTAMP WHERE id=?")) {
                            psComp.setInt(1, jobId);
                            psComp.executeUpdate();
                            recordMetric(con, jobId, "SUCCESS", startTime, retryCount);
                            con.commit();
                            logger.info("Job " + jobId + " COMPLETED");
                        }

                    } catch (Exception e) {
                        // ⚡ 4. SMART ERROR HANDLING
                        logger.log(Level.WARNING, "Job " + jobId + " failed: " + e.getMessage());

                        // Check if the error is a permanent "Wrong Email" error
                        boolean isWrongEmail = e instanceof AddressException || (e.getMessage() != null && e.getMessage().contains("Address"));

                        if (isWrongEmail || (retryCount + 1 >= retryLimit)) {
                            // If email is wrong OR we reached the limit, mark as FAILED
                            try (PreparedStatement psFail = con.prepareStatement(
                                    "UPDATE jobs SET status='FAILED' WHERE id=?")) {
                                psFail.setInt(1, jobId);
                                psFail.executeUpdate();
                                recordMetric(con, jobId, "FAILED", startTime, retryCount + 1);
                                logger.info("Job " + jobId + " stopped and marked as FAILED");
                            }
                        } else {
                            // Otherwise, increment retry count and put back to PENDING
                            try (PreparedStatement psRetry = con.prepareStatement(
                                    "UPDATE jobs SET retry_count = retry_count + 1, status='PENDING' WHERE id=?")) {
                                psRetry.setInt(1, jobId);
                                psRetry.executeUpdate();
                                recordMetric(con, jobId, "RETRY", startTime, retryCount);
                                logger.info("Job " + jobId + " set back to PENDING for retry");
                            }
                        }
                        con.commit();
                    }
                } else {
                    con.rollback();
                }
            }
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Database error", e);
        }
    }

    public static void main(String[] args) {
        final int RETRY_LIMIT = 3;
        final int PROCESSING_TIMEOUT = 30;

        while (true) {
            try {
                processJobs(RETRY_LIMIT, PROCESSING_TIMEOUT);
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                logger.log(Level.SEVERE, "Worker interrupted", e);
            }
        }
    }
}