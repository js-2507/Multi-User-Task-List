<h1>Multi-User Task Tracker</h1>

A lightweight, secure, and mobile-responsive web application for managing tasks across users. <br>
Built with Java 25 and SQLite.<br>
<img src="screenshots/admin1.png" height=400 width=225><img src="screenshots/admin2.png" height=400 width=225>
<br><img src="screenshots/user.png" height=270 width=225><img src="screenshots/login.png" height=330 width=225>
<br>Screenshots from a mobile phone browser above:<br>
<h2>Features</h2>
<ul><li>Role-based access control: A separate interface for Admins and Users with their respective controls<br>
<li>Live progress tracking: Admins can see Users task progression 
<li>Mobile-friendly design: Looks good and easy to use on mobile phones as well as computers
<li>Persistent storage: SQLite database remains safe and intact after updates or changes
</ul>
<h2>Security Implementations</h2>
<ul>
<li>Session Management: Implemented secure UUID-based session tokens with HttpOnly cookies to prevent Session Hijacking and IDOR attacks.

<li>SQL Injection Prevention: 100% coverage using PreparedStatements for all database interactions.

<li>XSS Protection: Automatic HTML entity escaping for all user-generated content (usernames and task descriptions).

<li>Access Control: Server-side verification of administrative privileges for all sensitive actions (adding users, clearing feeds).
</ul>

<h2>Tech Stack</h2>
<ul>
<li>Language: Java (using the built-in com.sun.net.httpserver)

<li>Database: SQLite 3

<li>Frontend: Vanilla HTML5 and CSS3 
</ul>
<!--<h2>Installation and Setup</h2>
<ol>
<li>Prerequisites: Java 25 installed on server
<li>Clone the repo: <br>`git clone https://github.com/your-username/chore-tracker.git
cd chore-tracker`
<li>Add Dependencies: Download the <a href="https://github.com/xerial/sqlite-jdbc">sqlite-jdbc</a> JAR file and add it to your classpath.</li>
<li>To deploy, do this in the intellij project (or project directory) terminal on your local machine<br>
# 1. Compile<br>
`javac -cp "lib/*" src/*.java -d bin/`<br>
# 2. Create a simple JAR<br>
`jar cfe chore-app.jar Main -C bin .`<br>
</li>
<li>Then send the .jar file and the lib folder to your server (in your folder for the service)<br>
you can use FileZilla or WinSCP for an easy GUI transfer or use the terminal
<li>To start the .jar file, enter `java -cp "chore-app.jar:lib/*" Main`</li>
<li>(Optional) To make it a systemd service (for best availability in case of power outage/restarting)<br>
On your server, go to `/etc/systemd/system` and create a `chore.service` using `sudo nano`
add this: <br><br>
`[Unit]`
`Description=Household Chore Tracker Service`<br>
# Wait for the network to be ready before starting`<br>
`After=network.target`<br>

`[Service]`<br>
`# The user that will run the app (usually your username, replace user)`<br>
`User=user`<br>
`# The folder where your .jar and chores.db are located (replace filepath)`<br>
`WorkingDirectory=/home/user/filepath`<br>
`# The command to start your app`<br>
`ExecStart=/usr/bin/java -cp "chore-app.jar:lib/*" Main`<br>
`# Restart the app automatically if it crashes`<br>
`Restart=always`<br>
`# Optional: standard output logs`<br>
`StandardOutput=syslog`<br>
`StandardError=syslog`<br>
`SyslogIdentifier=chore-app`<br>

`[Install]`<br>
`# This tells Ubuntu to start the service during a normal boot`<br>
`WantedBy=multi-user.target`<br><br>

Then do `sudo systemctl daemon-reload` and
`sudo systemctl enable chore.service` and `sudo systemctl start chore.service`
</li>
</ol>--><p>

---

# ---Installation and Setup---

### Prerequisites
* **Local Machine:** Java Development Kit (JDK 25) and Git installed.
* **Server:** Java Runtime Environment (JRE) or JDK installed.

---

### 1. Clone the Repository
Clone the project files to your local development machine and navigate into the project root:

`git clone [https://github.com/js-2507/Multi-User-Task-List.git](https://github.com/js-2507/Multi-User-Task-List.git)
cd Multi-User-Task-List`
## 2. Verify External Dependencies
Ensure that your project's local dependency directory contains the necessary .jar archive files for the database driver and security features:
<br>`sqlite-jdbc-*.jar`
## Build the Executable JAR Locally

Run the following commands inside your local project directory or IntelliJ IDEA built-in terminal to compile the source code and pack it into a portable package:
<br>`# Step 1: Compile all Java source files into a 'bin' distribution directory
javac -cp "lib/*" src/*.java -d bin/`

`# Step 2: Create an executable JAR archive targeting the Main entrypoint
jar cfe chore-app.jar Main -C bin .`

## 4. Transfer Files to Your Server

Deploy the generated application artifact along with its required dependency directory to your target production server environment using an SFTP client (such as FileZilla or WinSCP):

## 5. Executing the Application on the Server
   Option A: Running in the Foreground (For Testing)

This locks your terminal window to the process. Closing your terminal or disconnecting from SSH will stop the application:
<br>`java -cp "chore-app.jar:lib/*" Main`

## Option B: Running in the Background (Persistent)

To run the service silently in the background so it keeps executing even after you close your terminal or log out of your SSH session, use the nohup command:
<br>`nohup java -cp "chore-app.jar:lib/*" Main > server.log 2>&1 &`

# Production Deployment (systemd Service)

To ensure the chore management service remains highly available, runs silently in the background, and automatically recovers from server reboots or system power outages, configure it as an official Ubuntu system service.
<br><br>1. Create a new service tracking definition file:
<br>`sudo nano /etc/systemd/system/chore.service`
<p>2. Populate the configuration layout below (make sure to replace user and the companion working paths with your actual server account environment configurations):
<br><p>

<pre>
[Unit]
Description=Household Chore Tracker Service
After=network.target

[Service]
User=user
WorkingDirectory=/home/user/chore-app
ExecStart=/usr/bin/java -cp "chore-app.jar:lib/*" Main
Restart=always
StandardOutput=syslog
StandardError=syslog
SyslogIdentifier=chore-app

[Install]
WantedBy=multi-user.target
</pre>

<p>
3. Reload the tracking manager engine, flag the chore tracking script to start during the initial boot sequence, and engage the service right away:
<br>

<pre>
sudo systemctl daemon-reload
sudo systemctl enable chore.service
sudo systemctl start chore.service
</pre>

<p>
4. Confirm that your deployment profile launched successfully without active system blockages:
<br>

`sudo systemctl status chore.service`
<h2>How to Use</h2>
Service runs on port 8000, you can set up a reverse proxy and make a DNS record to attach a custom URL to make it easier to fnd<br>
for web admin and use, <br>
The admin can add users (and their usernames/passwords), add tasks for users, undo task progress, clear tasks
for users, and see progress of all users tasks.<br>
Default admin username is 'admin' and password is 'admin123$' (can be changed in Database.java file, line 28).
Users can see only their tasks through login, and can mark tasks as complete.
<h2>License</h2>
Any contributions and improvements are appreciated! 