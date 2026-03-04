import java.sql.*;

public class Database {
    private static Connection conn;

    // 1. Initialize Connection and Tables
    public static void initialize() throws SQLException {
        conn = DriverManager.getConnection("jdbc:sqlite:chores.db");
        Statement state = conn.createStatement();

        state.execute("""
            CREATE TABLE IF NOT EXISTS users (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                is_admin INTEGER DEFAULT 0
            )""");

        state.execute("""
            CREATE TABLE IF NOT EXISTS tasks (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                description TEXT NOT NULL,
                assigned_to_id INTEGER,
                is_done INTEGER DEFAULT 0,
                FOREIGN KEY(assigned_to_id) REFERENCES users(id)
            )""");

        state.execute("INSERT OR IGNORE INTO users (username, password, is_admin) VALUES ('admin', 'admin123$', 1)");
        System.out.println("Database initialized successfully.");
    }

    // 2. Authentication
    public static Integer authenticate(String username, String password) throws SQLException {
        // 1. Use '?' placeholders instead of variables
        String sql = "SELECT id FROM users WHERE username = ? AND password = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // 2. Bind the data to the placeholders
            pstmt.setString(1, username);
            pstmt.setString(2, password);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        }
        return null;
    }

    // 3. Admin View: Enhanced to include Task IDs for the "Undo" button
    public static ResultSet getAllTasksWithIds() throws SQLException {
        String sql = """
            SELECT 
                users.id AS user_id, 
                users.username, 
                tasks.id AS task_id, 
                tasks.description, 
                tasks.is_done 
            FROM users 
            LEFT JOIN tasks ON users.id = tasks.assigned_to_id
            WHERE users.is_admin = 0
            """;
        return conn.createStatement().executeQuery(sql);
    }

    // 4. User View
    public static ResultSet getTasksForUser(int userId) throws SQLException {
        String sql = "SELECT id, description, is_done FROM tasks WHERE assigned_to_id = ?";
        PreparedStatement pstmt = conn.prepareStatement(sql);
        pstmt.setInt(1, userId);
        return pstmt.executeQuery();
    }

    // 5. Add User
    public static void createUser(String name, String pass, int isAdmin) throws SQLException {
        String sql = "INSERT INTO users (username, password, is_admin) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, pass);
            pstmt.setInt(3, isAdmin);
            pstmt.executeUpdate();
        }
    }

    // 6. Assign Task
    public static void addTask(String description, int userId) throws SQLException {
        String sql = "INSERT INTO tasks (description, assigned_to_id) VALUES (?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, description);
            pstmt.setInt(2, userId);
            pstmt.executeUpdate();
        }
    }

    // 7. Mark Done
    public static void markTaskDone(int taskId) throws SQLException {
        String sql = "UPDATE tasks SET is_done = 1 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            pstmt.executeUpdate();
        }
    }

    // 8. NEW: Unmark Done (Undo)
    public static void unmarkTaskDone(int taskId) throws SQLException {
        String sql = "UPDATE tasks SET is_done = 0 WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, taskId);
            pstmt.executeUpdate();
        }
    }

    // 9. Clear Tasks for User
    public static void clearTasksForUser(int userId) throws SQLException {
        String sql = "DELETE FROM tasks WHERE assigned_to_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }

    // 10. Role Check
    public static boolean isAdmin(int userId) throws SQLException {
        String sql = "SELECT is_admin FROM users WHERE id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next() && rs.getInt("is_admin") == 1;
        }
    }
    // Gets a summary of completion progress for every non-admin user
    public static ResultSet getUserCompletionSummary() throws SQLException {
        String sql = """
        SELECT 
            users.username, 
            COUNT(tasks.id) AS total_tasks,
            SUM(CASE WHEN tasks.is_done = 1 THEN 1 ELSE 0 END) AS completed_tasks
        FROM users 
        LEFT JOIN tasks ON users.id = tasks.assigned_to_id
        WHERE users.is_admin = 0
        GROUP BY users.id
        """;
        return conn.createStatement().executeQuery(sql);
    }
}