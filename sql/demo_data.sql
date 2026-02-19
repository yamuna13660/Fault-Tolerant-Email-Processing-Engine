1. Insert Test Users (if your users table also has a trigger/identity)

INSERT INTO users (name, email) VALUES ('John Doe', 'john.doe@example.com');
INSERT INTO users (name, email) VALUES ('Jane Smith', 'jane.smith@example.com');
INSERT INTO users (name, email) VALUES ('Yamuna', 'yamuna.genai@gmail.com');

//INVALID USER (This will trigger the Java Exception and the Retry Logic)
INSERT INTO users (name, email) VALUES ('Broken Email User', 'not-a-real-email@#$');

2. Insert Initial Jobs
Note: We only provide user_id. The ID, Status, and Timestamps are automatic!

INSERT INTO jobs (user_id) VALUES (1);
INSERT INTO jobs (user_id) VALUES (2);
INSERT INTO jobs (user_id) VALUES (3);

