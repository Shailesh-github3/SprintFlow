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

  console.log("--- Running Messages & Comments Tests ---");

  // Setup: Register User, Create Project, Get Chat, Create Issue
  let email = `msguser_${Date.now()}@example.com`;
  let token = "";
  let projectId = null;
  let chatId = null;
  let issueId = null;

  try {
    const res = await axios.post(`${BASE_URL}/auth/register`, {
      fullName: "Message User",
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
      projectDescription: "Test",
      category: "IT"
    }, authHeaders);
    projectId = res.data.projectId;
    chatId = res.data.chat?.chatId;
  } catch (error) {
    console.error("Setup failed: could not create project", error.message);
    process.exit(1);
  }

  try {
    const res = await axios.post(`${BASE_URL}/api/issues`, {
      title: "Test Issue",
      description: "Test",
      status: "TODO",
      projectId: projectId,
      priority: "HIGH",
      dueDate: "2026-12-31"
    }, authHeaders);
    issueId = res.data.issueId;
  } catch (error) {
    console.error("Setup failed: could not create issue", error.message);
    process.exit(1);
  }

  // --- MESSAGES ---

  // 1. Send Message
  let messageId = null;
  try {
    const res = await axios.post(`${BASE_URL}/api/messages/send`, {
      projectId: projectId,
      content: "Hello Team!"
    }, authHeaders);
    assert(res.status === 201, "Send Message status 201", res.status, 201);
    assert(res.data.messageText === "Hello Team!", "Send Message text", res.data.messageText, "Hello Team!");
    messageId = res.data.messageId;
  } catch (error) {
    assert(false, `Send Message failed: ${error.message} - ${JSON.stringify(error.response?.data)}`);
  }

  // Send Message - Validation
  try {
    await axios.post(`${BASE_URL}/api/messages/send`, {
      projectId: projectId,
      content: ""
    }, authHeaders);
    assert(false, "Send Message empty should fail");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Send Message missing fields status 400", error.response?.status, 400);
    assert(data.fields?.content === "Message text is required", "Send Message missing msg", data.fields?.content, "Message text is required");
  }

  // 2. Get Messages by Project Id
  try {
    const res = await axios.get(`${BASE_URL}/api/messages/project/${projectId}`, authHeaders);
    assert(res.status === 200, "Get Messages By Project Id status 200", res.status, 200);
    assert(Array.isArray(res.data), "Get Messages By Project Id returns array");
    assert(res.data.length > 0 && res.data[res.data.length - 1].messageText === "Hello Team!", "Get Messages contains message");
  } catch (error) {
    assert(false, `Get Messages By Chat Id failed: ${error.message}`);
  }


  // --- COMMENTS ---

  // 1. Create Comment
  let commentId = null;
  try {
    const res = await axios.post(`${BASE_URL}/api/comments`, {
      issueId: issueId,
      content: "This is a comment"
    }, authHeaders);
    assert(res.status === 201, "Create Comment status 201", res.status, 201);
    assert(res.data.commentText === "This is a comment", "Create Comment text", res.data.commentText, "This is a comment");
    commentId = res.data.commentId;
  } catch (error) {
    assert(false, `Create Comment failed: ${error.message} - ${JSON.stringify(error.response?.data)}`);
  }

  // Create Comment - Validation
  try {
    await axios.post(`${BASE_URL}/api/comments`, {
      issueId: issueId,
      content: ""
    }, authHeaders);
    assert(false, "Create Comment empty should fail");
  } catch (error) {
    const data = error.response?.data || {};
    assert(error.response?.status === 400, "Create Comment missing fields status 400", error.response?.status, 400);
    assert(data.fields?.content === "Content is required", "Create Comment missing msg", data.fields?.content, "Content is required");
  }

  // 2. Get Comments By Issue Id
  try {
    const res = await axios.get(`${BASE_URL}/api/comments/issue/${issueId}`, authHeaders);
    assert(res.status === 200, "Get Comments By Issue Id status 200", res.status, 200);
    assert(Array.isArray(res.data), "Get Comments By Issue Id returns array");
    assert(res.data.length > 0 && res.data[res.data.length - 1].commentText === "This is a comment", "Get Comments contains comment");
  } catch (error) {
    assert(false, `Get Comments By Issue Id failed: ${error.message}`);
  }

  // 3. Delete Comment
  try {
    const res = await axios.delete(`${BASE_URL}/api/comments/${commentId}`, authHeaders);
    assert(res.status === 200, "Delete Comment status 200", res.status, 200);
    assert(!res.data || Object.keys(res.data).length === 0, "Delete Comment returned empty");
  } catch (error) {
    assert(false, `Delete Comment failed: ${error.message}`);
  }

  console.log(`\nResults: ${passed} passed, ${failed} failed.`);
  if (failed > 0) process.exit(1);
}

runTests();
