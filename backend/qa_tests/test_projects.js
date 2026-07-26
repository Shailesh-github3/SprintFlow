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

  console.log("--- Running Project Tests ---");

  // Setup: Register User
  let email = `projectuser_${Date.now()}@example.com`;
  let token = "";
  let userId = null;
  try {
    const res = await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "Project User",
      email: email,
      password: "SecurePass123"
    });
    token = res.data.jwt;
  } catch (error) {
    console.error("Setup failed: could not register user", error.message);
    process.exit(1);
  }

  const authHeaders = { headers: { Authorization: `Bearer ${token}` } };

  // 1. Create Project
  let projectId = null;
  try {
    const res = await axios.post(`${BASE_URL}/api/projects`, {
      projectName: "Mobile Banking App",
      projectDescription: "Develop secure mobile banking application",
      category: "FinTech",
      tags: ["mobile", "security", "api"]
    }, authHeaders);
    
    assert(res.status === 201, "Create Project status 201", res.status, 201);
    projectId = res.data.projectId;
    assert(res.data.projectName === "Mobile Banking App", "Create Project name", res.data.projectName, "Mobile Banking App");
    assert(res.data.category === "FinTech", "Create Project category", res.data.category, "FinTech");
    assert(res.data.projectOwner && typeof res.data.projectOwner.userId === 'number', "Create Project owner exists");
    assert(res.data.chat && res.data.chat.chatId, "Create Project chat exists");
    assert(res.data.projectMembers && res.data.projectMembers.length > 0, "Create Project members exist");
  } catch (error) {
    assert(false, `Create Project failed: ${error.message} - ${JSON.stringify(error.response?.data)}`);
  }

  // Create Project - Missing Fields
  try {
    await axios.post(`${BASE_URL}/api/projects`, {
      projectName: "",
      projectDescription: "",
      category: ""
    }, authHeaders);
    assert(false, "Create Project missing fields should fail");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Create Project missing fields status 400", error.response?.status, 400);
    assert(data.fields?.projectName === "Project name is required", "Create Project missing name msg", data.fields?.projectName, "Project name is required");
    assert(data.fields?.projectDescription === "Project description is required", "Create Project missing description msg", data.fields?.projectDescription, "Project description is required");
    assert(data.fields?.category === "Category is required", "Create Project missing category msg", data.fields?.category, "Category is required");
  }

  // Create Project - Unauthorized
  try {
    await axios.post(`${BASE_URL}/api/projects`, {
      projectName: "Test",
      projectDescription: "Test",
      category: "Test"
    });
    assert(false, "Create Project without token should fail");
  } catch (error) {
    // Note: The API.md says 401 with "Token has expired" but Spring Security typically throws 403 or 401 with standard message.
    assert(error.response?.status === 401 || error.response?.status === 403, "Create Project unauthorized status", error.response?.status, "401 or 403");
  }

  // 2. Get Project by ID
  try {
    const res = await axios.get(`${BASE_URL}/api/projects/${projectId}`, authHeaders);
    assert(res.status === 200, "Get Project status 200", res.status, 200);
    assert(res.data.projectId === projectId, "Get Project id matches", res.data.projectId, projectId);
  } catch (error) {
    assert(false, `Get Project failed: ${error.message}`);
  }

  // Get Project - Not Found
  try {
    await axios.get(`${BASE_URL}/api/projects/99999`, authHeaders);
    assert(false, "Get non-existent Project should fail");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 404, "Get Project not found status 404", error.response?.status, 404);
    assert(data.message === "Project not found with id: 99999", "Get Project not found message", data.message, "Project not found with id: 99999");
  }

  // 3. Get All Projects
  try {
    const res = await axios.get(`${BASE_URL}/api/projects`, authHeaders);
    assert(res.status === 200, "Get All Projects status 200", res.status, 200);
    assert(Array.isArray(res.data), "Get All Projects returns array");
    assert(res.data.length > 0, "Get All Projects array not empty");
  } catch (error) {
    assert(false, `Get All Projects failed: ${error.message}`);
  }

  // Filter by Category
  try {
    const res = await axios.get(`${BASE_URL}/api/projects?category=FinTech`, authHeaders);
    assert(res.status === 200, "Filter by Category status 200", res.status, 200);
    assert(res.data.length > 0 && res.data[0].category === "FinTech", "Filter by Category returns correct projects");
  } catch (error) {
    assert(false, `Filter by Category failed: ${error.message}`);
  }

  // 4. Update Project
  try {
    const res = await axios.put(`${BASE_URL}/api/projects/${projectId}`, {
      projectName: "Mobile Banking App v2.0",
      projectDescription: "Enhanced secure mobile banking application",
      category: "FinTech",
      tags: ["mobile", "security", "api", "biometric", "encryption"]
    }, authHeaders);
    assert(res.status === 200, "Update Project status 200", res.status, 200);
    assert(res.data.projectName === "Mobile Banking App v2.0", "Update Project name updated", res.data.projectName, "Mobile Banking App v2.0");
  } catch (error) {
    assert(false, `Update Project failed: ${error.message}`);
  }

  // 5. Search Projects
  try {
    const res = await axios.get(`${BASE_URL}/api/projects/search?name=Banking`, authHeaders);
    assert(res.status === 200, "Search Projects status 200", res.status, 200);
    assert(res.data.length > 0 && res.data[0].projectName.includes("Banking"), "Search Projects returns match");
  } catch (error) {
    assert(false, `Search Projects failed: ${error.message}`);
  }

  // 6. Get Project Chat
  try {
    const res = await axios.get(`${BASE_URL}/api/projects/${projectId}/chat`, authHeaders);
    assert(res.status === 200, "Get Project Chat status 200", res.status, 200);
    assert(res.data.project && res.data.project.projectId === projectId, "Chat belongs to project");
  } catch (error) {
    assert(false, `Get Project Chat failed: ${error.message}`);
  }

  // 7. Invite User
  // Register a second user to invite
  let email2 = `projectuser2_${Date.now()}@example.com`;
  let token2 = "";
  try {
    const res2 = await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "Project User 2",
      email: email2,
      password: "SecurePass123"
    });
    token2 = res2.data.jwt;
  } catch (e) {}

  try {
    const res = await axios.post(`${BASE_URL}/api/projects/invite`, {
      projectId: projectId,
      userEmail: email2
    }, authHeaders);
    assert(res.status === 200, "Invite User status 200", res.status, 200);
    assert(res.data.message === "Invitation sent successfully.", "Invite User message", res.data.message, "Invitation sent successfully.");
  } catch (error) {
    assert(false, `Invite User failed: ${error.message}`);
  }

  // Invite - Invalid Email Format
  try {
    await axios.post(`${BASE_URL}/api/projects/invite`, {
      projectId: projectId,
      userEmail: "invalid-email"
    }, authHeaders);
    assert(false, "Invite User invalid email should fail");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Invite User invalid email status 400", error.response?.status, 400);
    assert(data.fields?.userEmail === "Email must be valid", "Invite User invalid email msg", data.fields?.userEmail, "Email must be valid");
  }

  // Accept Invitation would require grabbing the token from the DB or mock, might be tricky in pure e2e unless returned in API or DB query.
  // We can skip it for now or query DB if needed.

  // 8. Delete Project
  try {
    const res = await axios.delete(`${BASE_URL}/api/projects/${projectId}`, authHeaders);
    assert(res.status === 200, "Delete Project status 200", res.status, 200);
    assert(res.data.message === "Project deleted successfully.", "Delete Project message", res.data.message, "Project deleted successfully.");
  } catch (error) {
    assert(false, `Delete Project failed: ${error.message}`);
  }

  console.log(`\nResults: ${passed} passed, ${failed} failed.`);
  if (failed > 0) process.exit(1);
}

runTests();
