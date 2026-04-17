# Security Workflow and File Usage

## 1. The Security Workflow (How it works in easy terms)

The application uses **JWT (JSON Web Token)** security. Think of a JWT like a digital ID badge for a restricted building.

Here is the step-by-step flow:

**Step 1: Registration/Login (Getting the ID Badge)**
* The user sends their username and password to the login API (`/api/auth/login`).
* The system checks the database to see if the username and password are correct.
* If correct, the system generates a long, encrypted text string called a **JWT (Token)**.
* The system sends this token back to the user.

**Step 2: Accessing Secure Areas (Showing the ID Badge)**
* Now, the user wants to fetch a list of Vendors (`/api/vendors`).
* To prove who they are, the user attaches the **JWT Token** to their request (in the HTTP Header as `Authorization: Bearer <token>`).
* Before the request reaches the Vendor API, a security "guard" (a Filter) intercepts it.
* The guard checks if the token is valid, hasn't expired, and hasn't been tampered with.
* If the token is good, the guard looks at the roles inside the token (e.g., "Is this user a Procurement Manager?").
* If the user has the right permissions, the guard lets the request through to the API. If not, it blocks it with a "403 Forbidden" error.

---

## 2. Security Files and Their Usage

Here is a breakdown of all the key security files in the project and what they do:

### `SecurityConfig.java`
*   **What it does:** This is the master configuration file for security. It acts as the rulebook.
*   **Usage:** It defines which URLs are public (like login and registration) and which are private (require authentication). It also disables default, outdated security features (like CSRF and basic auth) so the system can rely entirely on JWTs. It registers the token filter (the "guard") to run before every request.

### `JwtAuthenticationFilter.java`
*   **What it does:** This is the security "guard" at the front door.
*   **Usage:** It runs once for every single incoming HTTP request. It checks the Request Headers for the word "Bearer", extracts the token, and asks the `JwtTokenProvider` to validate it. If valid, it tells Spring Security, "This user is authenticated, let them proceed."

### `JwtTokenProvider.java`
*   **What it does:** The ID Badge maker and validator.
*   **Usage:** It has two main jobs:
    1.  **Generate:** It takes a successfully logged-in User and creates a brand new JWT string containing their username and roles, signed with a secret key.
    2.  **Validate:** It looks at an incoming token, verifies the secret signature, checks the expiration date, and extracts the username so the Filter knows who is trying to access the system.

### `CustomUserDetailsService.java`
*   **What it does:** The bridge between the database and Spring Security.
*   **Usage:** Spring Security needs to know how to find a user. When someone tries to log in, Spring calls this class, which searches your `UserRepository` (database) for the username. It takes your custom `User`, `Role`, and `Permission` entities and translates them into a format Spring Security understands (`UserDetails`).

### `AuthController.java`
*   **What it does:** The public reception desk.
*   **Usage:** It provides the actual REST APIs (`/api/auth/login`, `/api/auth/register`) that frontend applications or Postman use to send credentials and receive the token back.

### `SecurityConstants.java`
*   **What it does:** A file to store fixed security values.
*   **Usage:** It holds constants like the secret key used to sign the tokens (e.g., specific long strings), expiration times (e.g., token lasts for 24 hours), and the header prefix (`Bearer `). Keeping these here prevents hardcoding them all over the place.

### `AuthResponseDTO` & `LoginRequestDTO`
*   **What it does:** The data carriers for login.
*   **Usage:** `LoginRequestDTO` dictates exactly what JSON the user must send to log in (username and password). `AuthResponseDTO` dictates exactly what JSON the server will reply with (the newly generated JWT token).
