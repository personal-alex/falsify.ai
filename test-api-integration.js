// Simple test to verify API integration
const axios = require('axios');

async function testApiIntegration() {
  const baseURL = 'http://localhost:8080/api';
  
  try {
    console.log('Testing API integration...');
    
    // Test 1: Get articles for analysis
    console.log('1. Testing GET /prediction-analysis/articles');
    try {
      const articlesResponse = await axios.get(`${baseURL}/prediction-analysis/articles`, {
        params: { page: 0, size: 5 }
      });
      console.log('✓ Articles endpoint working:', articlesResponse.data.articles?.length || 0, 'articles found');
    } catch (error) {
      console.log('✗ Articles endpoint error:', error.response?.status, error.response?.data?.error || error.message);
    }
    
    // Test 2: Get authors
    console.log('2. Testing GET /prediction-analysis/authors');
    try {
      const authorsResponse = await axios.get(`${baseURL}/prediction-analysis/authors`);
      console.log('✓ Authors endpoint working:', authorsResponse.data?.length || 0, 'authors found');
    } catch (error) {
      console.log('✗ Authors endpoint error:', error.response?.status, error.response?.data?.error || error.message);
    }
    
    // Test 3: Get analysis status
    console.log('3. Testing GET /prediction-analysis/status');
    try {
      const statusResponse = await axios.get(`${baseURL}/prediction-analysis/status`);
      console.log('✓ Status endpoint working:', statusResponse.data?.available ? 'Available' : 'Not available');
    } catch (error) {
      console.log('✗ Status endpoint error:', error.response?.status, error.response?.data?.error || error.message);
    }
    
    console.log('\nAPI integration test completed!');
    
  } catch (error) {
    console.error('General error:', error.message);
  }
}

// Run the test
testApiIntegration();