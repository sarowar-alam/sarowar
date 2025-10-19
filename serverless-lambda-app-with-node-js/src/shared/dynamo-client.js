// Import AWS SDK to interact with AWS services
const AWS = require('aws-sdk');

// Create a function that initializes and configures a DynamoDB DocumentClient
const createDynamoClient = () => {
  return new AWS.DynamoDB.DocumentClient({
    // Retry up to 3 times in case of transient failures
    maxRetries: 3,
    // Set a timeout of 5 seconds for each request
    httpOptions: {
      timeout: 5000
    }
  });
};

// Export the client creator function for reuse in other modules
module.exports = { createDynamoClient };
