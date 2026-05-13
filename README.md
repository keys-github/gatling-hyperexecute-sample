# Gatling Performance Testing on HyperExecute — TestMu AI (Formerly LambdaTest)

Run Gatling load tests on [TestMu AI HyperExecute](https://www.testmuai.com/hyperexecute) with configurable workload models, injection profiles, and automatic HTML report generation.

## What This Repo Contains

- A **Spring Boot** application (`GatlingDemoApplication`) that exposes two endpoints:
  - `GET /greet/{name}` — returns a greeting message
  - `GET /slow` — responds after a random delay (0-3000 ms)
- A **Gatling simulation** (`GreetingSimulation`) that load-tests both endpoints with configurable injection profiles and workload models.
- An **HYE.yaml** configuration to run the tests on HyperExecute.

## Prerequisites

- Java 17+
- Maven 3.6+
- A [TestMu AI](https://www.testmuai.com/) account with HyperExecute access
- HyperExecute CLI binary ([download here](https://www.testmuai.com/support/docs/hyperexecute-cli-run-tests-on-hyperexecute-grid/))
- Environment variables set:
  ```bash
  export LT_USERNAME=<your-lambdatest-username>
  export LT_ACCESS_KEY=<your-lambdatest-access-key>
  ```

---

## Method 1: Using the YAML (CLI)

### 1. Clone the Repository

```bash
git clone <repo-url>
cd gatling-hyperexecute-sample
```

### 2. Understand the HYE.yaml

The `HYE.yaml` file defines the entire test execution pipeline:

```yaml
version: 0.1
runson: linux
autosplit: true
concurrency: 1
scenarioCommandStatusOnly: true

# Starts the Spring Boot app in the background as the test target
background:
  - mvn spring-boot:run -Dspring-boot.run.main-class=dev.simonverhoeven.gatlingdemo.GatlingDemoApplication || true

runtime:
  - language: java
    version: "17"

# Downloads Gatling HTML reports after the run
uploadArtefacts:
  - name: TestReport
    path:
      - target/gatling/**

# Resolves Maven dependencies before running tests
pre:
  - mvn dependency:resolve

# Configurable test parameters
vars:
  INJECT_TYPE: constantUsersPerSec
  WORKLOAD_MODEL: open
  USERS: 10
  DURATION: 30
  RAMP_DURATION: 60
  USERS_START: 5
  USERS_END: 10

testDiscovery:
  type: raw
  mode: static
  command: echo "Test"

# Runs the Gatling simulation with the configured parameters
testRunnerCommand: >
  mvn gatling:test
  -Dgatling.jvmArgs="-DinjectType=${INJECT_TYPE}
  -DworkloadModel=${WORKLOAD_MODEL}
  -Dusers=${USERS}
  -Dduration=${DURATION}
  -DrampDuration=${RAMP_DURATION}
  -DusersStart=${USERS_START}
  -DusersEnd=${USERS_END}"

retryOnFailure: true
maxRetries: 1
jobLabel: [Performance, Gatling]
```

### 3. Customize Test Parameters (Optional)

Edit the `vars` section in `HYE.yaml` to change load test behavior:

| Variable | Default | Description |
|---|---|---|
| `INJECT_TYPE` | `constantUsersPerSec` | Injection profile (see table below) |
| `WORKLOAD_MODEL` | `open` | `open` (arrival rate) or `closed` (concurrent users) |
| `USERS` | `10` | Number of users |
| `DURATION` | `30` | Test duration in seconds |
| `RAMP_DURATION` | `60` | Ramp-up duration in seconds |
| `USERS_START` | `5` | Starting user count (for ramp profiles) |
| `USERS_END` | `10` | Ending user count (for ramp profiles) |

**Supported Injection Types:**

| Inject Type | Workload Model | Behavior |
|---|---|---|
| `constantUsersPerSec` | open | Constant arrival rate of `USERS` users/sec for `DURATION` seconds |
| `rampUsersPerSec` | open | Ramps from `USERS_START` to `USERS_END` users/sec over `DURATION` seconds |
| `stressPeakUsers` | open | Stress test peaking at `USERS` over `DURATION` seconds |
| `soakTest` | closed | Ramps concurrent users from 1 to `USERS` over `DURATION` seconds |
| `capacityTest` | closed | Ramps concurrent users from 1 to `USERS` over `DURATION` seconds |
| `constantUsers` | open | Ramps `USERS` over `RAMP_DURATION` seconds |

### 4. Run via HyperExecute CLI

```bash
./hyperexecute --user {userName} --key {accessKey} --config HYE.yaml
```

To override variables at runtime without editing the YAML:

```bash
./hyperexecute --user {userName} --key {accessKey} --config HYE.yaml --vars "USERS=20,DURATION=60,INJECT_TYPE=soakTest"
```

### 5. View Reports

After the job completes:
- Go to [HyperExecute Dashboard](https://hyperexecute.lambdatest.com/) to see job status
- Download the **TestReport** artefact — it contains the Gatling HTML report under `target/gatling/`

---

## Method 2: Using HyperExecute UI

For UI-based execution, you update the `GreetingSimulation.java` file directly in the project with your desired test configuration, then trigger the run from the HyperExecute dashboard.

### 1. Update GreetingSimulation.java

Edit `src/test/java/example/GreetingSimulation.java` to configure your test:

- **Base URL**: Change `http://localhost:8080` to your target application URL if testing an external service
- **Endpoints**: Modify the HTTP requests to match your API under test
- **Injection profile**: Adjust the `setUp()` method or the default system property values for users, duration, etc.

### 2. Push Changes

Commit and push your updated simulation to the repository:

```bash
git add src/test/java/example/GreetingSimulation.java
git commit -m "Update simulation for target service"
git push
```

### 3. Run from HyperExecute UI

1. Log in to [TestMu AI](https://accounts.lambdatest.com/login)
2. From the left sidebar, go to **HyperExecute**
3. Click **Run New Job**
4. Select your **Git Repository** and branch
5. HyperExecute auto-detects the `HYE.yaml` in the root directory
6. Click **Run**

HyperExecute will:
1. Spin up a Linux VM with Java 17
2. Resolve Maven dependencies
3. Start the Spring Boot application in the background
4. Execute the Gatling simulation
5. Upload the Gatling HTML report as an artefact

### 4. View Results

1. Monitor real-time logs in the **Jobs** tab
2. Once completed, go to the job details page
3. Click on **Artefacts** > **TestReport** to download the Gatling HTML report
4. Open the `index.html` inside the downloaded report to see detailed performance metrics:
   - Response time distribution
   - Requests per second
   - Active users over time
   - Percentile response times

---

## Running Locally (Without HyperExecute)

To run the tests locally for development:

```bash
# Start the Spring Boot app
mvn spring-boot:run &

# Run Gatling tests with default parameters
mvn gatling:test

# Run with custom parameters
mvn gatling:test -Dgatling.jvmArgs="-DinjectType=constantUsersPerSec -DworkloadModel=open -Dusers=5 -Dduration=15"
```

Reports are generated at `target/gatling/greetingsimulation-<timestamp>/index.html`.

---

## Project Structure

```
gatling-hyperexecute-sample/
├── HYE.yaml                          # HyperExecute configuration
├── pom.xml                           # Maven config (Spring Boot + Gatling)
├── src/
│   ├── main/java/.../
│   │   ├── GatlingDemoApplication.java   # Spring Boot app (test target)
│   │   └── MainController.java           # REST endpoints (/greet, /slow)
│   └── test/java/example/
│       └── GreetingSimulation.java       # Gatling load test simulation
└── target/gatling/                   # Generated reports (after test run)
```

## 🚀 LambdaTest is Now TestMu AI

👋 Welcome to TestMu AI, the next evolution of LambdaTest. As of January 2026, [LambdaTest is Now TestMu AI](https://www.testmuai.com/lambdatest-is-now-testmuai/) - we have evolved from a cross-browser testing cloud into a unified, AI-native quality engineering platform designed for the modern DevOps era.

Whether you have been part of the LambdaTest community for years or are just discovering TestMu AI, our mission remains the same: to help you ship faster with high-scale test execution, autonomous testing, and deep quality analytics.

### 🔄 Our Rebrand Journey

In 2017, we introduced LambdaTest with a clear mission: to become the world's most trusted cloud testing platform. We built a scalable, high-performance test cloud that eliminated flakiness, improved developer feedback cycles, and accelerated release velocity for teams worldwide.

As LambdaTest grew, we expanded the platform into Test Intelligence, Visual Regression Testing, Accessibility Testing, API Testing, and Performance Testing, covering the entire testing lifecycle. These capabilities enabled teams to test any stack, on any technology, at enterprise scale.

Over time, we rebuilt the architecture to be AI-native from the ground up. What began as LambdaTest's high-performance testing cloud has now evolved into TestMu AI, an AI-native, multi-agent platform redefining modern quality engineering.

We chose the name TestMu AI to reflect our shift towards intelligent, autonomous testing. While our identity has changed, our core technology and commitment to the testing community stay the same.

👉 Find [LambdaTest's New Home](https://www.testmuai.com/).

### 🔭 Explore TestMu AI

The same infrastructure LambdaTest customers relied on, now delivered through autonomous AI agents.

- [KaneAI](https://www.testmuai.com/kane-ai/)
- [Agent-to-Agent Testing](https://www.testmuai.com/agent-to-agent-testing/)
- [HyperExecute](https://www.testmuai.com/hyperexecute/)
- [Real Device Cloud](https://www.testmuai.com/real-device-cloud/)
- [Pricing](https://www.testmuai.com/pricing/)
- [Documentation](https://www.testmuai.com/support/docs/)