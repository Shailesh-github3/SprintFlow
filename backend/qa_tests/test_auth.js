const axios = require('axios');

const BASE_URL = 'http://localhost:8080';

async function runTests() {
  let passed = 0, failed = 0;
  
  const assert = (condition, msg, actual, expected) => {
    if (!condition) {
      console.error(`❌ FAILED: ${msg}`);
      if (actual !== undefined || expected !== undefined) {
        console.error(`   Expected: ${JSON.stringify(expected)}`);
        console.error(`   Actual:   ${JSON.stringify(actual)}`);
      }
      failed++;
    } else {
      console.log(`✅ PASSED: ${msg}`);
      passed++;
    }
  };

  console.log("--- Running Auth Tests ---");

  // 1. Register New User
  let email = `testuser_${Date.now()}@example.com`;
  try {
    const res = await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "Test User",
      email: email,
      password: "SecurePass123"
    });
    assert(res.status === 201, `Status code is 201`, res.status, 201);
    assert(res.data.jwt && typeof res.data.jwt === 'string', "Response has JWT token");
    assert(res.data.message === "User created successfully", "Response has success message", res.data.message, "User created successfully");
  } catch (error) {
    assert(false, `Valid registration failed: ${error.message} - ${JSON.stringify(error.response?.data)}`);
  }

  // Duplicate email
  try {
    const res = await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "Test User 2",
      email: email,
      password: "DifferentPass456"
    });
    assert(false, "Duplicate email should have failed");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 500, `Duplicate email status 500`, error.response?.status, 500);
    assert(data.error === "Internal Server Error", "Duplicate email error string", data.error, "Internal Server Error");
    assert(data.message === `User with email ${email} already exists.`, "Duplicate email error message", data.message, `User with email ${email} already exists.`);
    assert(data.path === "/auth/register", "Duplicate email path", data.path, "/auth/register");
  }

  // Invalid email format
  try {
    await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "Test User",
      email: "invalid-email",
      password: "Password123"
    });
    assert(false, "Invalid email format should have failed");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Invalid email status 400", error.response?.status, 400);
    assert(data.error === "Validation failed", "Invalid email error string", data.error, "Validation failed");
    assert(data.path === "/auth/register", "Invalid email path", data.path, "/auth/register");
    assert(data.fields?.email === "Email must be valid", "Invalid email field message", data.fields?.email, "Email must be valid");
  }

  // Password too short
  try {
    await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "Test User",
      email: `test2_${Date.now()}@example.com`,
      password: "short"
    });
    assert(false, "Short password should have failed");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Short password status 400", error.response?.status, 400);
    assert(data.fields?.password === "Password must be at least 8 characters long", "Short password field message", data.fields?.password, "Password must be at least 8 characters long");
  }
  
  // Missing Required Fields
  try {
    await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "",
      email: `test3_${Date.now()}@example.com`,
      password: ""
    });
    assert(false, "Missing fields should have failed");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Missing fields status 400", error.response?.status, 400);
    assert(data.fields?.fullName === "Full name is required", "fullName validation error", data.fields?.fullName, "Full name is required");
    assert(data.fields?.password === "Password is required", "password validation error", data.fields?.password, "Password is required");
  }

  // Login Tests
  try {
    const res = await axios.post(`${BASE_URL}/auth/login`, {
      email: email,
      password: "SecurePass123"
    });
    assert(res.status === 200, "Login successful status 200", res.status, 200);
    assert(res.data.jwt && typeof res.data.jwt === 'string', "Login returns JWT");
    assert(res.data.message === "Login successful", "Login success message", res.data.message, "Login successful");
  } catch (error) {
    assert(false, `Valid login failed: ${error.message} - ${JSON.stringify(error.response?.data)}`);
  }

  // Login - Wrong Password
  try {
    await axios.post(`${BASE_URL}/auth/login`, {
      email: email,
      password: "WrongPassword999"
    });
    assert(false, "Wrong password should have failed");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 500, "Wrong password status 500", error.response?.status, 500);
    assert(data.message === "Invalid email or password.", "Wrong password message", data.message, "Invalid email or password.");
    assert(data.path === "/auth/login", "Wrong password path", data.path, "/auth/login");
  }

  // Login - Invalid Email Format
  try {
    await axios.post(`${BASE_URL}/auth/login`, {
      email: "not-an-email",
      password: "Password123"
    });
    assert(false, "Login invalid email format should have failed");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Login invalid email status 400", error.response?.status, 400);
    assert(data.error === "Validation failed", "Login invalid email error string", data.error, "Validation failed");
    assert(data.path === "/auth/login", "Login invalid email path", data.path, "/auth/login");
    assert(data.fields?.email === "Email must be valid", "Login invalid email field message", data.fields?.email, "Email must be valid");
  }

  console.log(`\nResults: ${passed} passed, ${failed} failed.`);
  if (failed > 0) process.exit(1);
}

runTests();
