// Import AWS SDK for DynamoDB
const AWS = require('aws-sdk');

// Initialize DynamoDB DocumentClient
const dynamoDB = new AWS.DynamoDB.DocumentClient();

// Lambda entry point
exports.handler = async (event) => {
  console.log('Processing get user request');
  
  try {
    // Extract userId from API Gateway path parameter
    const userId = event.pathParameters.userId;

    // Query DynamoDB for a user by ID
    const result = await dynamoDB.get({
      TableName: process.env.USERS_TABLE, // table name from environment
      Key: { userId }
    }).promise();

    // If no item found, return 404 Not Found
    if (!result.Item) {
      return {
        statusCode: 404,
        headers: {
          'Content-Type': 'application/json',
          'Access-Control-Allow-Origin': '*'
        },
        body: JSON.stringify({ error: 'User not found' })
      };
    }

    // Return found user object
    return {
      statusCode: 200, // HTTP OK
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      body: JSON.stringify(result.Item)
    };
  } catch (error) {
    // Handle unexpected errors
    console.error('Error getting user:', error);
    return {
      statusCode: 500,
      headers: {
        'Content-Type': 'application/json',
        'Access-Control-Allow-Origin': '*'
      },
      body: JSON.stringify({ error: 'Internal server error' })
    };
  }
};
