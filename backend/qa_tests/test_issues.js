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

  console.log("--- Running Issue Tests ---");

  // Setup: Register User & Create Project
  let email = `issueuser_${Date.now()}@example.com`;
  let token = "";
  let projectId = null;

  try {
    const res = await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "Issue User",
      email: email,
      password: "SecurePass123"
    });
    token = res.data.jwt;
  } catch (error) {
    console.error("Setup failed: could not register user", error.message);
    process.exit(1);
  }

  const authHeaders = { headers: { Authorization: `Bearer ${token}` } };

  try {
    const res = await axios.post(`${BASE_URL}/api/projects`, {
      projectName: "Test Project",
      projectDescription: "Test Description",
      category: "IT"
    }, authHeaders);
    projectId = res.data.projectId;
  } catch (error) {
    console.error("Setup failed: could not create project", error.message);
    process.exit(1);
  }

  // 1. Create Issue
  let issueId = null;
  try {
    const res = await axios.post(`${BASE_URL}/api/issues`, {
      title: "Fix login bug",
      description: "Users unable to login when password contains special characters",
      status: "TODO",
      projectId: projectId,
      priority: "HIGH",
      dueDate: "2026-12-31"
    }, authHeaders);
    
    assert(res.status === 201, "Create Issue status 201", res.status, 201);
    issueId = res.data.issueId;
    assert(res.data.issueTitle === "Fix login bug", "Create Issue title", res.data.issueTitle, "Fix login bug");
    assert(res.data.issueStatus === "TODO", "Create Issue status", res.data.issueStatus, "TODO");
  } catch (error) {
    assert(false, `Create Issue failed: ${error.message} - ${JSON.stringify(error.response?.data)}`);
  }

  // Create Issue - Missing Fields
  try {
    await axios.post(`${BASE_URL}/api/issues`, {
      title: "",
      description: "",
      projectId: null
    }, authHeaders);
    assert(false, "Create Issue missing fields should fail");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Create Issue missing fields status 400", error.response?.status, 400);
    assert(data.fields?.title === "Issue title is required", "Create Issue missing title msg", data.fields?.title, "Issue title is required");
    assert(data.fields?.projectId === "Project ID is required", "Create Issue missing projectId msg", data.fields?.projectId, "Project ID is required");
  }

  // 2. Get Issue By Id
  try {
    const res = await axios.get(`${BASE_URL}/api/issues/${issueId}`, authHeaders);
    assert(res.status === 200, "Get Issue By Id status 200", res.status, 200);
    assert(res.data.issueId === issueId, "Get Issue By Id matches", res.data.issueId, issueId);
  } catch (error) {
    assert(false, `Get Issue By Id failed: ${error.message}`);
  }

  // Get Issue By Id - Not Found
  try {
    await axios.get(`${BASE_URL}/api/issues/99999`, authHeaders);
    assert(false, "Get non-existent Issue should fail");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 404, "Get Issue not found status 404", error.response?.status, 404);
    assert(data.message === "Issue not found with id: 99999", "Get Issue not found message", data.message, "Issue not found with id: 99999");
  }

  // 3. Get Issues By Project Id
  try {
    const res = await axios.get(`${BASE_URL}/api/issues/project/${projectId}`, authHeaders);
    assert(res.status === 200, "Get Issues By Project Id status 200", res.status, 200);
    assert(Array.isArray(res.data), "Get Issues By Project Id returns array");
    assert(res.data.length > 0 && res.data[0].issueId === issueId, "Get Issues By Project returns correct issue");
  } catch (error) {
    assert(false, `Get Issues By Project Id failed: ${error.message}`);
  }

  // 4. Update Issue Status
  try {
    const res = await axios.put(`${BASE_URL}/api/issues/${issueId}/status/DONE`, {}, authHeaders);
    assert(res.status === 200, "Update Issue Status status 200", res.status, 200);
    assert(!res.data || Object.keys(res.data).length === 0, "Update Issue Status returned empty");
  } catch (error) {
    assert(false, `Update Issue Status failed: ${error.message}`);
  }

  // 5. Delete Issue
  try {
    const res = await axios.delete(`${BASE_URL}/api/issues/${issueId}`, authHeaders);
    assert(res.status === 200, "Delete Issue status 200", res.status, 200);
    assert(!res.data || Object.keys(res.data).length === 0, "Delete Issue returned empty");
  } catch (error) {
    assert(false, `Delete Issue failed: ${error.message}`);
  }

  console.log(`\nResults: ${passed} passed, ${failed} failed.`);
  if (failed > 0) process.exit(1);
}

runTests();
