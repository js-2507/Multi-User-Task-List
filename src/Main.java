import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;
import java.net.URLDecoder;

public class Main {
    public static void main(String[] args) throws Exception {
        Database.initialize();

        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", 8000), 0);

        // Navigation Routes
        server.createContext("/login", new LoginHandler());
        server.createContext("/tasks", new TaskHandler());
        server.createContext("/admin", new AdminHandler());

        // Action Routes
        server.createContext("/add-user", new CreateUserHandler());
        server.createContext("/add-task", new AddTaskHandler());
        server.createContext("/clear-tasks", new ClearTasksHandler());
        server.createContext("/complete-task", new CompleteTaskHandler());
        server.createContext("/uncomplete-task", new UncompleteTaskHandler());

        server.createContext("/", t -> {
            t.getResponseHeaders().set("Location", "/login");
            t.sendResponseHeaders(303, -1);
        });

        server.setExecutor(null);
        System.out.println("Chore Server live at port 8000");
        server.start();
    }

    private static final Map<String, Integer> sessions = new HashMap<>();

    public static void addSession(String token, int userId) {
        sessions.put(token, userId);
    }

    public static Integer getUserIdFromExchange(HttpExchange t) {
        String cookieHeader = t.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader != null) {
            for (String cookie : cookieHeader.split(";")) {
                String[] pair = cookie.trim().split("=");
                if (pair.length == 2 && pair[0].equals("sessionToken")) {
                    return sessions.get(pair[1]);
                }
            }
        }
        return null;
    }

    public static String htmlEscape(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;");
    }

    public static String wrapHtml(String title, String content) {
        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
            <style>
                body { font-family: -apple-system, system-ui, sans-serif; background: #f0f2f5; margin: 0; padding: 15px; display: flex; justify-content: center; }
                .card { background: white; width: 100%; max-width: 450px; padding: 20px; border-radius: 15px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); box-sizing: border-box; }
                h1 { color: #1c1e21; text-align: center; margin-bottom: 20px; font-size: 1.5rem; }
                h3 { border-bottom: 2px solid #eee; padding-bottom: 5px; margin-top: 25px; color: #4b4f56; }
                input, select, button { width: 100%; padding: 12px; margin: 8px 0; border-radius: 8px; border: 1px solid #ddd; font-size: 16px; box-sizing: border-box; }
                button { background: #0084ff; color: white; border: none; font-weight: bold; cursor: pointer; transition: background 0.2s; }
                .btn-secondary { background: #e4e6eb; color: #050505; margin-top: 20px; display: block; text-align: center; text-decoration: none; padding: 12px; border-radius: 8px; font-weight: bold; }
                .progress-bg { background: #eee; border-radius: 10px; height: 8px; width: 100%; margin: 5px 0 15px 0; overflow: hidden; }
                .progress-fill { background: #42b72a; height: 100%; transition: width 0.5s; }
                table { width: 100%; border-collapse: collapse; margin: 10px 0; font-size: 0.9rem; }
                th, td { padding: 10px 5px; text-align: left; border-bottom: 1px solid #eee; }
                ul { list-style: none; padding: 0; }
                li { background: #f8f9fa; margin: 10px 0; padding: 15px; border-radius: 10px; display: flex; justify-content: space-between; align-items: center; border-left: 5px solid #0084ff; }
                .done { border-left-color: #ccd0d5; opacity: 0.6; text-decoration: line-through; }
                .undo-btn { width: auto; padding: 5px 10px; font-size: 11px; margin: 0; background: #ffcc00; color: #000; border: 1px solid #d4ac0d; }
                .clear-btn { background: #f02849; font-size: 11px; width: auto; padding: 5px 10px; margin: 0; color: white; border: none; border-radius: 4px;}
            </style>
        </head>
        <body>
            <div class="card"><h1>""" + title + """
                </h1>""" + content + """
        </div>
        </body>
        </html>
        """;
    }

    static class LoginHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            if ("GET".equals(t.getRequestMethod())) {
                String form = """
                <form action="/login" method="POST">
                    <input type="text" name="username" placeholder="Username" required>
                    <input type="password" name="password" placeholder="Password" required>
                    <button type="submit">Login</button>
                </form>
                """;
                sendResponse(t, wrapHtml("Task List: User Login", form));
            } else {
                InputStream is = t.getRequestBody();
                Map<String, String> params = parseFormData(new String(is.readAllBytes(), "UTF-8"));
                try {
                    Integer userId = Database.authenticate(params.get("username"), params.get("password"));
                    if (userId != null) {
                        String token = UUID.randomUUID().toString();
                        Main.addSession(token, userId);
                        String cookieValue = String.format("sessionToken=%s; HttpOnly; Path=/; Max-Age=3600", token);
                        t.getResponseHeaders().add("Set-Cookie", cookieValue);
                        String target = Database.isAdmin(userId) ? "/admin" : "/tasks";
                        t.getResponseHeaders().set("Location", target);
                        t.sendResponseHeaders(303, -1);
                    } else {
                        sendResponse(t, wrapHtml("Error", "Invalid Login. <br><br><a href='/login' class='btn-secondary'>Try again</a>"));
                    }
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }
    }

    static class TaskHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Integer userId = Main.getUserIdFromExchange(t);
            if (userId == null) {
                t.getResponseHeaders().set("Location", "/login");
                t.sendResponseHeaders(303, -1);
                return;
            }
            StringBuilder listHtml = new StringBuilder("<ul>");
            try (ResultSet rs = Database.getTasksForUser(userId)) {
                while (rs.next()) {
                    int taskId = rs.getInt("id");
                    boolean isDone = rs.getInt("is_done") == 1;
                    String desc = htmlEscape(rs.getString("description"));
                    listHtml.append("<li class='").append(isDone ? "done" : "").append("'>");
                    listHtml.append("<span>").append(isDone ? "" : "").append(desc).append("</span>");
                    if (!isDone) {
                        listHtml.append("<form action='/complete-task' method='POST' style='margin:0;'>")
                                .append("<input type='hidden' name='taskId' value='").append(taskId).append("'>")
                                .append("<button type='submit' style='width:auto; padding:8px 12px; margin:0;'>Done</button></form>");
                    }
                    listHtml.append("</li>");
                }
            } catch (SQLException e) { e.printStackTrace(); }
            listHtml.append("</ul><a href='/login' class='btn-secondary' onclick='return confirm(\"Logout?\")'>Logout</a>");
            sendResponse(t, wrapHtml("Your Tasks", listHtml.toString()));
        }
    }

    static class AdminHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Integer currentUserId = Main.getUserIdFromExchange(t);
            try {
                if (currentUserId == null || !Database.isAdmin(currentUserId)) {
                    t.getResponseHeaders().set("Location", (currentUserId == null) ? "/login" : "/tasks");
                    t.sendResponseHeaders(303, -1);
                    return;
                }
            } catch (SQLException e) { e.printStackTrace(); return; }

            StringBuilder content = new StringBuilder();
            content.append("<h3><a href='/login' class='btn-secondary' onclick='return confirm(\"Logout Admin?\")'>Logout</a></h3>");
            content.append("<h3>Task Progress</h3><table><tr><th>User</th><th>Done</th><th>Status</th></tr>");

            try (ResultSet rs = Database.getUserCompletionSummary()) {
                while (rs.next()) {
                    int total = rs.getInt("total_tasks");
                    int done = rs.getInt("completed_tasks");
                    String user = htmlEscape(rs.getString("username"));
                    int percent = (total == 0) ? 0 : (int)((done / (double)total) * 100);
                    String status = (total > 0 && total == done) ? "All Done" : (total == 0 ? "Idle" : "Pending");
                    content.append("<tr><td>").append(user).append("</td><td>").append(done).append("/").append(total).append("</td><td>").append(status).append("</td></tr>")
                            .append("<tr><td colspan='3' style='border-bottom:none; padding:0;'><div class='progress-bg'><div class='progress-fill' style='width:").append(percent).append("%'></div></div></td></tr>");
                }
            } catch (SQLException e) { e.printStackTrace(); }
            content.append("</table>");

            content.append("<h3>Live Feed</h3><div style='overflow-x:auto;'><table><tr><th>User</th><th>Task</th><th>Action</th></tr>");
            try (ResultSet rs = Database.getAllTasksWithIds()) {
                while (rs.next()) {
                    int taskId = rs.getInt("task_id");

                    int uId = rs.getInt("user_id");
                    boolean isDone = rs.getInt("is_done") == 1;
                    content.append("<tr><td>").append(htmlEscape(rs.getString("username"))).append("</td>")
                            .append("<td>").append(htmlEscape(rs.getString("description"))).append(isDone ? " ✅" : "").append("</td><td>");
                    if (isDone) {
                        content.append("<form action='/uncomplete-task' method='POST'><input type='hidden' name='taskId' value='").append(taskId).append("'><button type='submit' class='undo-btn'>Undo</button></form>");
                    } else {
                        content.append("<form action='/clear-tasks' method='POST'><input type='hidden' name='targetUserId' value='").append(uId).append("'><button type='submit' class='clear-btn' onclick='return confirm(\"Wipe tasks?\")'>Delete</button></form>");
                    }
                    content.append("</td></tr>");
                }
            } catch (SQLException e) { e.printStackTrace(); }
            content.append("</table></div>");

            content.append("<h3>Assign Task</h3><form action='/add-task' method='POST'><select name='userId' required><option value='' disabled selected>Select User</option>");
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:chores.db");
                 ResultSet rs = conn.createStatement().executeQuery("SELECT id, username FROM users WHERE is_admin = 0")) {
                while (rs.next()) { content.append("<option value='").append(rs.getInt("id")).append("'>").append(htmlEscape(rs.getString("username"))).append("</option>"); }
            } catch (SQLException e) { e.printStackTrace(); }
            content.append("</select><input type='text' name='description' placeholder='Task' required><button type='submit'>Assign</button></form>");

            content.append("<h3>Add New User</h3><form action='/add-user' method='POST'><input type='text' name='username' placeholder='Username' required><input type='password' name='password' placeholder='Password' required><button type='submit'>Create User</button></form>");
            sendResponse(t, wrapHtml("Admin Dashboard", content.toString()));
        }
    }

    public static Map<String, String> parseFormData(String formData) {
        Map<String, String> map = new HashMap<>();
        try { for (String pair : formData.split("&")) { String[] kv = pair.split("="); if (kv.length > 1) map.put(URLDecoder.decode(kv[0], "UTF-8"), URLDecoder.decode(kv[1], "UTF-8")); }
        } catch (Exception e) { e.printStackTrace(); }
        return map;
    }

    public static void sendResponse(HttpExchange t, String response) throws IOException {
        byte[] bytes = response.getBytes("UTF-8");
        t.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
        t.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = t.getResponseBody()) { os.write(bytes); }
    }

    static class CreateUserHandler implements HttpHandler { public void handle(HttpExchange t) throws IOException { handleAction(t, () -> {
        Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
        Database.createUser(p.get("username"), p.get("password"), 0);
    }, "/admin"); } }

    static class AddTaskHandler implements HttpHandler { public void handle(HttpExchange t) throws IOException { handleAction(t, () -> {
        Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
        Database.addTask(p.get("description"), Integer.parseInt(p.get("userId")));
    }, "/admin"); } }

    static class CompleteTaskHandler implements HttpHandler {
        public void handle(HttpExchange t) throws IOException {
            Integer userId = Main.getUserIdFromExchange(t);
            if (userId == null) { t.getResponseHeaders().set("Location", "/login"); t.sendResponseHeaders(303, -1); return; }
            Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
            try { Database.markTaskDone(Integer.parseInt(p.get("taskId"))); } catch (SQLException e) { e.printStackTrace(); }
            t.getResponseHeaders().set("Location", "/tasks");
            t.sendResponseHeaders(303, -1);
        }
    }

    static class UncompleteTaskHandler implements HttpHandler { public void handle(HttpExchange t) throws IOException { handleAction(t, () -> {
        Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
        Database.unmarkTaskDone(Integer.parseInt(p.get("taskId")));
    }, "/admin"); } }

    static class ClearTasksHandler implements HttpHandler { public void handle(HttpExchange t) throws IOException { handleAction(t, () -> {
        Map<String, String> p = parseFormData(new String(t.getRequestBody().readAllBytes()));
        Database.clearTasksForUser(Integer.parseInt(p.get("targetUserId")));
    }, "/admin"); } }

    private interface DatabaseAction { void run() throws Exception; }
    private static void handleAction(HttpExchange t, DatabaseAction action, String redirect) throws IOException {
        Integer currentUserId = Main.getUserIdFromExchange(t);
        try {
            if (currentUserId == null || !Database.isAdmin(currentUserId)) {
                t.getResponseHeaders().set("Location", "/login");
                t.sendResponseHeaders(303, -1);
                return;
            }
            action.run();
        } catch (Exception e) { e.printStackTrace(); }
        t.getResponseHeaders().set("Location", redirect);
        t.sendResponseHeaders(303, -1);
    }
}